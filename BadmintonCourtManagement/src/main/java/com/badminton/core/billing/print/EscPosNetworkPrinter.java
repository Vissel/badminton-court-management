package com.badminton.core.billing.print;

import com.badminton.core.billing.print.ReceiptDocument.Line;
import com.badminton.core.billing.print.ReceiptDocument.Style;
import com.badminton.exception.BusinessException;
import com.badminton.exception.enums.ErrorCodeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Network ESC/POS printer channel: renders the receipt as a raster bitmap and
 * pushes it to printer_ip:port (default 9100) over a raw socket. Rasterising
 * via Java2D keeps Vietnamese diacritics intact without printer codepage
 * configuration.
 */
@Slf4j
@Component
public class EscPosNetworkPrinter {

    private static final int CONNECT_TIMEOUT_MS = 3000;
    private static final int WRITE_TIMEOUT_MS = 5000;
    private static final int MARGIN_PX = 12;

    private static final Font BASE_FONT = loadFont("/fonts/DejaVuSans.ttf");
    private static final Font BOLD_FONT = loadFont("/fonts/DejaVuSans-Bold.ttf");

    // Standard ESC/POS raster widths at ~203 dpi.
    public static int widthForPaperMm(int paperWidthMm) {
        return paperWidthMm <= 60 ? 384 : 576;
    }

    private static Font loadFont(String path) {
        try {
            return Font.createFont(Font.TRUETYPE_FONT, new ClassPathResource(path).getInputStream());
        } catch (Exception e) {
            log.warn("Bundled font {} unavailable, falling back to SansSerif: {}", path, e.getMessage());
            return new Font(Font.SANS_SERIF, Font.PLAIN, 1);
        }
    }

    /**
     * Render + push the document to the printer. Throws BusinessException on
     * any connectivity/IO failure — the caller decides whether the bill keeps
     * its state (it must never roll the bill back).
     */
    public void print(ReceiptDocument doc, String ip, int port, int paperWidthMm)
            throws BusinessException {
        BufferedImage image = render(doc, widthForPaperMm(paperWidthMm));
        byte[] payload = toEscPos(image);
        send(ip, port, payload);
    }

    /** Connectivity probe: opens the socket and pushes an init + test line. */
    public void testConnection(String ip, int port) throws BusinessException {
        ReceiptDocument probe = new ReceiptDocument();
        probe.add(Line.builder().left("Printer OK").style(Style.CENTER_BOLD).build());
        BufferedImage image = render(probe, 384);
        send(ip, port, toEscPos(image));
    }

    // ---------- raster rendering ----------

    BufferedImage render(ReceiptDocument doc, int widthPx) {
        Font normal = BASE_FONT.deriveFont(Font.PLAIN, 22f);
        Font bold = BOLD_FONT.deriveFont(Font.BOLD, 22f);
        Font title = BOLD_FONT.deriveFont(Font.BOLD, 28f);
        Font small = BASE_FONT.deriveFont(Font.PLAIN, 18f);

        // First pass: measure the required height.
        BufferedImage scratch = new BufferedImage(widthPx, 10, BufferedImage.TYPE_BYTE_BINARY);
        Graphics2D g0 = scratch.createGraphics();
        int height = MARGIN_PX;
        for (Line line : doc.getLines()) {
            height += lineHeight(line, g0, normal, bold, title, small);
        }
        height += MARGIN_PX * 2;
        g0.dispose();

        BufferedImage image = new BufferedImage(widthPx, height, BufferedImage.TYPE_BYTE_BINARY);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, widthPx, height);
        g.setColor(Color.BLACK);

        int y = MARGIN_PX;
        for (Line line : doc.getLines()) {
            if (line.getStyle() == Style.SEPARATOR) {
                int mid = y + 4;
                for (int x = MARGIN_PX; x < widthPx - MARGIN_PX; x += 8) {
                    g.fillRect(x, mid, 4, 1);
                }
                y += 10;
                continue;
            }
            Font font = switch (line.getStyle()) {
                case BOLD, CENTER_BOLD -> bold;
                case TITLE -> title;
                case SMALL -> small;
                default -> normal;
            };
            g.setFont(font);
            FontMetrics fm = g.getFontMetrics();
            int ascent = fm.getAscent();
            switch (line.getStyle()) {
                case CENTER, CENTER_BOLD, TITLE -> {
                    int textWidth = fm.stringWidth(nullToEmpty(line.getLeft()));
                    g.drawString(nullToEmpty(line.getLeft()),
                            Math.max(0, (widthPx - textWidth) / 2), y + ascent);
                }
                default -> {
                    g.drawString(nullToEmpty(line.getLeft()), MARGIN_PX, y + ascent);
                    if (line.getRight() != null) {
                        int rw = fm.stringWidth(line.getRight());
                        g.drawString(line.getRight(), widthPx - MARGIN_PX - rw, y + ascent);
                    }
                }
            }
            y += fm.getHeight() + 2;
            if (line.getSub() != null) {
                g.setFont(small);
                g.drawString(line.getSub(), MARGIN_PX * 2, y + g.getFontMetrics().getAscent());
                y += g.getFontMetrics().getHeight() + 2;
            }
        }
        g.dispose();
        return image;
    }

    private int lineHeight(Line line, Graphics2D g, Font normal, Font bold, Font title, Font small) {
        if (line.getStyle() == Style.SEPARATOR) {
            return 10;
        }
        Font font = switch (line.getStyle()) {
            case BOLD, CENTER_BOLD -> bold;
            case TITLE -> title;
            case SMALL -> small;
            default -> normal;
        };
        int h = g.getFontMetrics(font).getHeight() + 2;
        if (line.getSub() != null) {
            h += g.getFontMetrics(small).getHeight() + 2;
        }
        return h;
    }

    private String nullToEmpty(String s) {
        return s != null ? s : "";
    }

    // ---------- ESC/POS raster encoding ----------

    /**
     * GS v 0 — raster bit image, mode 0. Bits are MSB-first per byte; a set
     * bit prints black.
     */
    byte[] toEscPos(BufferedImage image) throws BusinessException {
        int width = image.getWidth();
        int height = image.getHeight();
        int widthBytes = (width + 7) / 8;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.writeBytes(new byte[] { 0x1B, 0x40 }); // ESC @ init
        out.writeBytes(new byte[] { 0x1D, 0x76, 0x30, 0x00 }); // GS v 0 mode 0
        out.write(widthBytes & 0xFF);
        out.write((widthBytes >> 8) & 0xFF);
        out.write(height & 0xFF);
        out.write((height >> 8) & 0xFF);
        for (int y = 0; y < height; y++) {
            for (int xb = 0; xb < widthBytes; xb++) {
                int b = 0;
                for (int bit = 0; bit < 8; bit++) {
                    int x = xb * 8 + bit;
                    if (x < width && (image.getRGB(x, y) & 0xFFFFFF) == 0) {
                        b |= (0x80 >> bit);
                    }
                }
                out.write(b);
            }
        }
        out.writeBytes(new byte[] { 0x1B, 0x64, 0x03 }); // ESC d 3 — feed 3 lines
        out.writeBytes(new byte[] { 0x1D, 0x56, 0x41, 0x03 }); // GS V — partial cut
        return out.toByteArray();
    }

    private void send(String ip, int port, byte[] payload) throws BusinessException {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(ip, port), CONNECT_TIMEOUT_MS);
            socket.setSoTimeout(WRITE_TIMEOUT_MS);
            OutputStream out = socket.getOutputStream();
            out.write(payload);
            out.flush();
        } catch (IOException e) {
            log.error("ESC/POS print to {}:{} failed: {}", ip, port, e.getMessage());
            throw new BusinessException(ErrorCodeEnum.INTERNAL_SERVER_ERROR,
                    "Không kết nối được máy in " + ip + ":" + port + " — " + e.getMessage());
        }
    }
}

package com.badminton.core.billing.print;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;
import javax.imageio.ImageIO;

/**
 * Renders the buyer retrieval link of an e-invoice as a QR bitmap. Used by the
 * PDF exporter, the ESC/POS raster printer and the QR endpoint that feeds the
 * browser receipt. Instantiated directly by the renderers — it holds no state
 * and must not become a bean.
 */
@Slf4j
public class QrCodeRenderer {

    private static final int QUIET_ZONE = 1;

    /**
     * PNG form of {@link #render(String, int)} for HTTP responses.
     *
     * @return the encoded image, or null when the payload cannot be encoded.
     */
    public byte[] renderPng(String payload, int sizePx) {
        BufferedImage image = render(payload, sizePx);
        if (image == null) {
            return null;
        }
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (IOException e) {
            log.warn("Cannot encode QR as PNG: {}", e.getMessage());
            return null;
        }
    }

    /**
     * @return a 1-bit-per-pixel black/white image, or null when the payload is
     *         blank or encoding fails (a receipt without a QR must still print).
     */
    public BufferedImage render(String payload, int sizePx) {
        if (StringUtils.isBlank(payload) || sizePx <= 0) {
            return null;
        }
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.MARGIN, QUIET_ZONE);
        try {
            BitMatrix matrix = new QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, sizePx, sizePx, hints);
            BufferedImage image = new BufferedImage(matrix.getWidth(), matrix.getHeight(),
                    BufferedImage.TYPE_BYTE_BINARY);
            for (int y = 0; y < matrix.getHeight(); y++) {
                for (int x = 0; x < matrix.getWidth(); x++) {
                    image.setRGB(x, y, matrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
                }
            }
            return image;
        } catch (WriterException | IllegalArgumentException e) {
            log.warn("Cannot render QR for payload of length {}: {}", payload.length(), e.getMessage());
            return null;
        }
    }
}

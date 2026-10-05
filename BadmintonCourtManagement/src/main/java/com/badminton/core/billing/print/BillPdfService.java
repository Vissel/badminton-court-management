package com.badminton.core.billing.print;

import com.badminton.core.billing.print.ReceiptDocument.Line;
import com.badminton.core.billing.print.ReceiptDocument.Style;
import com.badminton.entity.BillConfig;
import com.badminton.entity.Invoice;
import com.badminton.exception.BusinessException;
import com.badminton.exception.enums.ErrorCodeEnum;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Renders a bill to an 80mm-wide PDF (thermal-receipt proportions) using the
 * bundled DejaVu fonts — required for Vietnamese diacritics (the PDF Base-14
 * fonts only cover Latin-1).
 */
@Component
public class BillPdfService {

    private static final float MM = 2.83465f;          // 1 mm in PDF points
    private static final float MARGIN = 4f * MM;
    private static final float LINE_GAP = 1.2f;

    public byte[] render(ReceiptDocument doc, int paperWidthMm) throws BusinessException {
        float pageWidth = paperWidthMm * MM;
        try (PDDocument pdf = new PDDocument()) {
            PDType0Font regular = PDType0Font.load(pdf,
                    new ClassPathResource("/fonts/DejaVuSans.ttf").getInputStream());
            PDType0Font bold = PDType0Font.load(pdf,
                    new ClassPathResource("/fonts/DejaVuSans-Bold.ttf").getInputStream());

            // First pass: measure height to size the page.
            float height = MARGIN;
            for (Line line : doc.getLines()) {
                height += lineHeight(line) + (line.getSub() != null ? subHeight() : 0);
            }
            height += MARGIN;

            PDPage page = new PDPage(new PDRectangle(pageWidth, height));
            pdf.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(pdf, page)) {
                float y = height - MARGIN;
                for (Line line : doc.getLines()) {
                    y = drawLine(cs, doc, line, pageWidth, y, regular, bold);
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            pdf.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new BusinessException(ErrorCodeEnum.INTERNAL_SERVER_ERROR,
                    "Cannot render bill PDF: " + e.getMessage());
        }
    }

    private float lineHeight(Line line) {
        if (line.getStyle() == Style.SEPARATOR) {
            return 3.5f * MM;
        }
        return fontSize(line.getStyle()) * LINE_GAP + 1.5f;
    }

    private float subHeight() {
        return 6.5f * LINE_GAP + 1f;
    }

    private float fontSize(Style style) {
        return switch (style) {
            case TITLE -> 11f;
            case BOLD, CENTER_BOLD -> 8.5f;
            case SMALL -> 7f;
            default -> 8f;
        };
    }

    private PDType0Font font(Style style, PDType0Font regular, PDType0Font bold) {
        return switch (style) {
            case TITLE, BOLD, CENTER_BOLD -> bold;
            default -> regular;
        };
    }

    private float drawLine(PDPageContentStream cs, ReceiptDocument doc, Line line,
                           float pageWidth, float y,
                           PDType0Font regular, PDType0Font bold) throws IOException {
        if (line.getStyle() == Style.SEPARATOR) {
            float mid = y - 1.5f * MM;
            cs.setLineWidth(0.4f);
            cs.moveTo(MARGIN, mid);
            cs.lineTo(pageWidth - MARGIN, mid);
            cs.setLineDashPattern(new float[]{2f, 2f}, 0);
            cs.stroke();
            cs.setLineDashPattern(new float[]{}, 0);
            return y - 3.5f * MM;
        }
        PDType0Font font = font(line.getStyle(), regular, bold);
        float size = fontSize(line.getStyle());
        float baseline = y - size;
        switch (line.getStyle()) {
            case CENTER, CENTER_BOLD, TITLE -> {
                String text = safe(line.getLeft());
                float w = font.getStringWidth(text) / 1000f * size;
                write(cs, font, size, Math.max(MARGIN, (pageWidth - w) / 2f), baseline, text);
            }
            default -> {
                write(cs, font, size, MARGIN, baseline, safe(line.getLeft()));
                if (line.getRight() != null) {
                    float w = font.getStringWidth(line.getRight()) / 1000f * size;
                    write(cs, font, size, pageWidth - MARGIN - w, baseline, line.getRight());
                }
            }
        }
        y -= fontSize(line.getStyle()) * LINE_GAP + 1.5f;
        if (line.getSub() != null) {
            write(cs, regular, 6.5f, MARGIN * 2, y - 6.5f, line.getSub());
            y -= subHeight();
        }
        return y;
    }

    private void write(PDPageContentStream cs, PDType0Font font, float size,
                       float x, float y, String text) throws IOException {
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(text);
        cs.endText();
    }

    private String safe(String s) {
        return s != null ? s : "";
    }
}

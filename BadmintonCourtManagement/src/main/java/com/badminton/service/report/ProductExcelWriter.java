package com.badminton.service.report;

import com.badminton.enums.ProductSheet;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.List;

/**
 * Writes the product catalog workbook used by export and template endpoints.
 * Two sheets - {@code ShuttleBall} and {@code Service} - each with a
 * {@code Tên | Giá} header row.
 */
@Slf4j
@Component
public class ProductExcelWriter {

    public static final String HEADER_NAME = "Tên";
    public static final String HEADER_COST = "Giá";

    private static final int MAX_CELL_LENGTH = 32767;
    private static final String[] FORMULA_PREFIXES = {"=", "+", "-", "@"};

    /**
     * A single product line: display name + cost.
     */
    public record ProductLine(String name, float cost) {
    }

    /**
     * Writes the two product sheets. Pass empty lists for a headers-only template.
     */
    public void write(List<ProductLine> shuttleBalls, List<ProductLine> services, OutputStream outputStream) {
        try (Workbook workbook = new XSSFWorkbook()) {
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle moneyStyle = createMoneyStyle(workbook);

            writeSheet(workbook, ProductSheet.SHUTTLE_BALL.getSheetName(), shuttleBalls, headerStyle, moneyStyle);
            writeSheet(workbook, ProductSheet.SERVICE.getSheetName(), services, headerStyle, moneyStyle);

            workbook.write(outputStream);
            outputStream.flush();
        } catch (IOException e) {
            log.error("Failed to write product workbook", e);
            throw new UncheckedIOException("Failed to write product workbook", e);
        }
    }

    private void writeSheet(Workbook workbook, String sheetName, List<ProductLine> lines,
                            CellStyle headerStyle, CellStyle moneyStyle) {
        Sheet sheet = workbook.createSheet(sheetName);

        Row headerRow = sheet.createRow(0);
        Cell nameHeader = headerRow.createCell(0);
        nameHeader.setCellValue(HEADER_NAME);
        nameHeader.setCellStyle(headerStyle);
        Cell costHeader = headerRow.createCell(1);
        costHeader.setCellValue(HEADER_COST);
        costHeader.setCellStyle(headerStyle);

        int rowIdx = 1;
        for (ProductLine line : lines) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(escapeCellText(line.name()));
            Cell costCell = row.createCell(1);
            costCell.setCellValue(line.cost());
            costCell.setCellStyle(moneyStyle);
        }

        sheet.setColumnWidth(0, 40 * 256);
        sheet.setColumnWidth(1, 15 * 256);
        sheet.createFreezePane(0, 1);
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private CellStyle createMoneyStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setDataFormat(workbook.createDataFormat().getFormat("#,##0"));
        style.setAlignment(HorizontalAlignment.RIGHT);
        return style;
    }

    private String escapeCellText(String text) {
        if (text == null) {
            return "";
        }
        String trimmed = text.length() > MAX_CELL_LENGTH ? text.substring(0, MAX_CELL_LENGTH) : text;
        for (String prefix : FORMULA_PREFIXES) {
            if (trimmed.startsWith(prefix)) {
                return "'" + trimmed;
            }
        }
        return trimmed;
    }
}

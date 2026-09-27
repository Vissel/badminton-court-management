package com.badminton.service.report;

import com.badminton.response.inventory.InventoryItemResponse;
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
import java.math.BigDecimal;
import java.util.List;

/**
 * Writes the inventory report workbook: one {@code Inventory} sheet with
 * stock on hand, weighted average cost, retail price and margin per item.
 */
@Slf4j
@Component
public class InventoryExcelWriter {

    public static final String SHEET_NAME = "Inventory";
    public static final String[] HEADERS = {
            "Tên", "Loại", "Đơn vị", "Tồn kho", "Quy đổi", "Giá nhập TB", "Giá bán", "Lãi/sp"};

    public void write(List<InventoryItemResponse> items, OutputStream outputStream) {
        try (Workbook workbook = new XSSFWorkbook()) {
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle moneyStyle = createMoneyStyle(workbook);

            Sheet sheet = workbook.createSheet(SHEET_NAME);
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (InventoryItemResponse item : items) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(item.getItemName());
                row.createCell(1).setCellValue(item.getItemType());
                row.createCell(2).setCellValue(item.getUnit() != null ? item.getUnit() : "");
                row.createCell(3).setCellValue(item.getStockOnHand());
                String breakdown = item.getPackageBreakdown();
                row.createCell(4).setCellValue(breakdown != null ? breakdown : "");
                writeMoney(row.createCell(5), item.getAvgCost(), moneyStyle);
                writeMoney(row.createCell(6), item.getRetailPrice(), moneyStyle);
                writeMoney(row.createCell(7), item.getMargin(), moneyStyle);
            }

            sheet.setColumnWidth(0, 40 * 256);
            for (int i = 1; i < HEADERS.length; i++) {
                sheet.setColumnWidth(i, 15 * 256);
            }
            sheet.createFreezePane(0, 1);

            workbook.write(outputStream);
            outputStream.flush();
        } catch (IOException e) {
            log.error("Failed to write inventory workbook", e);
            throw new UncheckedIOException("Failed to write inventory workbook", e);
        }
    }

    private void writeMoney(Cell cell, BigDecimal value, CellStyle moneyStyle) {
        if (value != null) {
            cell.setCellValue(value.doubleValue());
        } else {
            cell.setCellValue("");
        }
        cell.setCellStyle(moneyStyle);
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
}

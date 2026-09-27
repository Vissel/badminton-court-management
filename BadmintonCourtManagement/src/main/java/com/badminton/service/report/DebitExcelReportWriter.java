package com.badminton.service.report;

import com.badminton.enums.DebitReportMode;
import com.badminton.enums.DebitStatus;
import com.badminton.model.report.DebitReportData;
import com.badminton.model.report.DebtCurrentReportRow;
import com.badminton.model.report.DebtHistoryReportRow;
import com.badminton.model.report.DebtPlayerSummaryRow;
import com.badminton.requestmodel.debit.DebitReportExportRequest;
import com.badminton.util.MoneyUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Component
public class DebitExcelReportWriter {

    private static final int ROW_WINDOW = 200;
    private static final int MAX_CELL_LENGTH = 32767;
    private static final String[] FORMULA_PREFIXES = {"=", "+", "-", "@"};

    private static final DateTimeFormatter FILE_TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final DateTimeFormatter DISPLAY_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DISPLAY_DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private static final String SHEET_SUMMARY = "Tổng hợp";
    private static final String SHEET_DETAIL = "Chi tiết";

    private static final String ASCII_FALLBACK_FILENAME = "debt-report.xlsx";
    private static final String CURRENT_PREFIX = "cong-no-hien-tai";
    private static final String HISTORY_PREFIX = "lich-su-no";

    private static final Map<String, String> STATUS_LABELS = Map.of(
            "PENDING", "Chưa thanh toán",
            "PARTIALLY_PAID", "Thanh toán một phần",
            "PAID", "Đã thanh toán"
    );

    public void write(DebitReportData data,
                      DebitReportExportRequest request,
                      ZoneId zoneId,
                      OutputStream outputStream) {
        SXSSFWorkbook workbook = null;
        try {
            workbook = new SXSSFWorkbook(ROW_WINDOW);
            workbook.setCompressTempFiles(true);

            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle moneyStyle = createMoneyStyle(workbook);
            CellStyle textStyle = createTextStyle(workbook);

            writeSummarySheet(workbook, data, request, zoneId, headerStyle, moneyStyle, textStyle);
            writeDetailSheet(workbook, data, request, zoneId, headerStyle, moneyStyle, textStyle);

            workbook.write(outputStream);
            outputStream.flush();
        } catch (IOException e) {
            log.error("Failed to write debit report workbook", e);
            throw new UncheckedIOException("Failed to write debit report workbook", e);
        } finally {
            if (workbook != null) {
                try {
                    workbook.dispose();
                    workbook.close();
                } catch (IOException e) {
                    log.error("Failed to close SXSSF workbook", e);
                }
            }
        }
    }

    public String buildContentDisposition(DebitReportMode mode, Instant exportTime, ZoneId zoneId) {
        String utf8Filename = buildUtf8Filename(mode, exportTime, zoneId);
        String encoded = URLEncoder.encode(utf8Filename, StandardCharsets.UTF_8)
                .replace("+", "%20");
        return "attachment; filename=\"" + ASCII_FALLBACK_FILENAME + "\"; filename*=UTF-8''" + encoded;
    }

    private String buildUtf8Filename(DebitReportMode mode, Instant exportTime, ZoneId zoneId) {
        String prefix = mode == DebitReportMode.CURRENT ? CURRENT_PREFIX : HISTORY_PREFIX;
        String timestamp = exportTime.atZone(zoneId).format(FILE_TIMESTAMP_FORMATTER);
        return prefix + "_" + timestamp + ".xlsx";
    }

    private void writeSummarySheet(SXSSFWorkbook workbook,
                                   DebitReportData data,
                                   DebitReportExportRequest request,
                                   ZoneId zoneId,
                                   CellStyle headerStyle,
                                   CellStyle moneyStyle,
                                   CellStyle textStyle) {
        Sheet sheet = workbook.createSheet(SHEET_SUMMARY);
        sheet.setDefaultColumnStyle(0, textStyle);

        int rowIdx = 0;
        rowIdx = writeMetadata(sheet, rowIdx, data.getMode(), request, zoneId);

        String[] headers;
        if (data.getMode() == DebitReportMode.CURRENT) {
            headers = new String[]{"STT", "Người chơi", "Tổng nợ còn lại", "Số khoản", "Tiền tệ"};
        } else {
            headers = new String[]{"STT", "Người chơi", "Tổng tiền nợ", "Đã trả", "Còn lại",
                    "Số khoản đã trả", "Tổng số khoản", "Tiền tệ"};
        }

        Row headerRow = sheet.createRow(rowIdx++);
        for (int col = 0; col < headers.length; col++) {
            Cell cell = headerRow.createCell(col);
            cell.setCellValue(headers[col]);
            cell.setCellStyle(headerStyle);
        }

        BigDecimal totalMoney1 = BigDecimal.ZERO;
        BigDecimal totalMoney2 = BigDecimal.ZERO;
        BigDecimal totalMoney3 = BigDecimal.ZERO;
        long totalCount1 = 0;
        long totalCount2 = 0;

        List<DebtPlayerSummaryRow> summaries = data.getSummaries();
        int stt = 1;
        for (DebtPlayerSummaryRow summary : summaries) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(stt++);
            row.createCell(1).setCellValue(escapeCellText(summary.getPlayerName()));
            String currency = MoneyUtils.CURRENCY_VN;

            if (data.getMode() == DebitReportMode.CURRENT) {
                BigDecimal total = summary.getTotalRemainingDebt() != null ? summary.getTotalRemainingDebt() : BigDecimal.ZERO;
                long count = summary.getNumDebits() != null ? summary.getNumDebits() : 0;
                setMoneyCell(row, 2, total, moneyStyle);
                row.createCell(3).setCellValue(count);
                row.createCell(4).setCellValue(currency);
                totalMoney1 = totalMoney1.add(total);
                totalCount1 += count;
            } else {
                BigDecimal totalDebt = summary.getTotalDebt() != null ? summary.getTotalDebt() : BigDecimal.ZERO;
                BigDecimal totalPaid = summary.getTotalPaid() != null ? summary.getTotalPaid() : BigDecimal.ZERO;
                BigDecimal totalRemaining = summary.getTotalRemaining() != null ? summary.getTotalRemaining() : BigDecimal.ZERO;
                long paidCount = summary.getPaidDebits() != null ? summary.getPaidDebits() : 0;
                long totalCount = summary.getTotalDebits() != null ? summary.getTotalDebits() : 0;

                setMoneyCell(row, 2, totalDebt, moneyStyle);
                setMoneyCell(row, 3, totalPaid, moneyStyle);
                setMoneyCell(row, 4, totalRemaining, moneyStyle);
                row.createCell(5).setCellValue(paidCount);
                row.createCell(6).setCellValue(totalCount);
                row.createCell(7).setCellValue(currency);

                totalMoney1 = totalMoney1.add(totalDebt);
                totalMoney2 = totalMoney2.add(totalPaid);
                totalMoney3 = totalMoney3.add(totalRemaining);
                totalCount1 += paidCount;
                totalCount2 += totalCount;
            }
        }

        Row totalRow = sheet.createRow(rowIdx++);
        totalRow.createCell(0).setCellValue("");
        Cell totalLabel = totalRow.createCell(1);
        totalLabel.setCellValue("Tổng cộng");
        totalLabel.setCellStyle(headerStyle);
        if (data.getMode() == DebitReportMode.CURRENT) {
            setMoneyCell(totalRow, 2, totalMoney1, moneyStyle);
            totalRow.createCell(3).setCellValue(totalCount1);
            totalRow.createCell(4).setCellValue("");
        } else {
            setMoneyCell(totalRow, 2, totalMoney1, moneyStyle);
            setMoneyCell(totalRow, 3, totalMoney2, moneyStyle);
            setMoneyCell(totalRow, 4, totalMoney3, moneyStyle);
            totalRow.createCell(5).setCellValue(totalCount1);
            totalRow.createCell(6).setCellValue(totalCount2);
            totalRow.createCell(7).setCellValue("");
        }

        setSummaryColumnWidths(sheet, data.getMode());
        sheet.createFreezePane(0, headerRow.getRowNum() + 1);
        sheet.setAutoFilter(new CellRangeAddress(headerRow.getRowNum(), headerRow.getRowNum(), 0, headers.length - 1));
    }

    private void writeDetailSheet(SXSSFWorkbook workbook,
                                  DebitReportData data,
                                  DebitReportExportRequest request,
                                  ZoneId zoneId,
                                  CellStyle headerStyle,
                                  CellStyle moneyStyle,
                                  CellStyle textStyle) {
        Sheet sheet = workbook.createSheet(SHEET_DETAIL);
        sheet.setDefaultColumnStyle(0, textStyle);

        int rowIdx = 0;
        rowIdx = writeMetadata(sheet, rowIdx, data.getMode(), request, zoneId);

        String[] headers;
        if (data.getMode() == DebitReportMode.CURRENT) {
            headers = new String[]{"STT", "Người chơi", "Ngày ghi nợ", "Số tiền còn lại", "Tiền tệ", "Ghi chú"};
        } else {
            headers = new String[]{"STT", "Người chơi", "Ngày ghi nợ", "Số tiền nợ", "Đã trả", "Còn lại",
                    "Ngày trả gần nhất", "Trạng thái", "Tiền tệ", "Ghi chú"};
        }

        Row headerRow = sheet.createRow(rowIdx++);
        for (int col = 0; col < headers.length; col++) {
            Cell cell = headerRow.createCell(col);
            cell.setCellValue(headers[col]);
            cell.setCellStyle(headerStyle);
        }

        int stt = 1;
        if (data.getMode() == DebitReportMode.CURRENT) {
            for (DebtCurrentReportRow row : data.getCurrentDetails()) {
                Row excelRow = sheet.createRow(rowIdx++);
                excelRow.createCell(0).setCellValue(stt++);
                excelRow.createCell(1).setCellValue(escapeCellText(row.getPlayerName()));
                excelRow.createCell(2).setCellValue(formatDateTime(row.getDebtDate(), zoneId));
                setMoneyCell(excelRow, 3, row.getRemainingAmount(), moneyStyle);
                excelRow.createCell(4).setCellValue(defaultCurrency(row.getCurrency()));
                excelRow.createCell(5).setCellValue(escapeCellText(row.getNote()));
            }
        } else {
            for (DebtHistoryReportRow row : data.getHistoryDetails()) {
                Row excelRow = sheet.createRow(rowIdx++);
                excelRow.createCell(0).setCellValue(stt++);
                excelRow.createCell(1).setCellValue(escapeCellText(row.getPlayerName()));
                excelRow.createCell(2).setCellValue(formatDateTime(row.getDebtDate(), zoneId));
                setMoneyCell(excelRow, 3, row.getDebtAmount(), moneyStyle);
                setMoneyCell(excelRow, 4, row.getPaidAmount(), moneyStyle);
                setMoneyCell(excelRow, 5, row.getRemainingAmount(), moneyStyle);
                excelRow.createCell(6).setCellValue(formatDateTime(row.getLastPaymentDate(), zoneId));
                excelRow.createCell(7).setCellValue(translateStatus(row.getStatus()));
                excelRow.createCell(8).setCellValue(defaultCurrency(row.getCurrency()));
                excelRow.createCell(9).setCellValue(escapeCellText(row.getNote()));
            }
        }

        setDetailColumnWidths(sheet, data.getMode());
        sheet.createFreezePane(0, headerRow.getRowNum() + 1);
        sheet.setAutoFilter(new CellRangeAddress(headerRow.getRowNum(), headerRow.getRowNum(), 0, headers.length - 1));
    }

    private int writeMetadata(Sheet sheet, int startRow, DebitReportMode mode,
                              DebitReportExportRequest request, ZoneId zoneId) {
        int row = startRow;
        Instant now = Instant.now();

        Row typeRow = sheet.createRow(row++);
        typeRow.createCell(0).setCellValue("Loại báo cáo:");
        typeRow.createCell(1).setCellValue(mode == DebitReportMode.CURRENT ? "Nợ hiện tại" : "Lịch sử nợ");

        Row scopeRow = sheet.createRow(row++);
        scopeRow.createCell(0).setCellValue("Phạm vi:");
        scopeRow.createCell(1).setCellValue(request.getScope());

        Row playerRow = sheet.createRow(row++);
        playerRow.createCell(0).setCellValue("Người chơi / Bộ lọc:");
        playerRow.createCell(1).setCellValue(escapeCellText(resolvePlayerLabel(request)));

        Row dateRow = sheet.createRow(row++);
        dateRow.createCell(0).setCellValue("Khoảng thời gian:");
        dateRow.createCell(1).setCellValue(request.getFrom() + " - " + request.getTo());

        Row exportRow = sheet.createRow(row++);
        exportRow.createCell(0).setCellValue("Xuất lúc:");
        exportRow.createCell(1).setCellValue(formatDateTime(now, zoneId) + " (" + zoneId.getId() + ")");

        sheet.setColumnWidth(0, 22 * 256);
        sheet.setColumnWidth(1, 50 * 256);
        return row;
    }

    private String resolvePlayerLabel(DebitReportExportRequest request) {
        if (request.getPlayerName() != null && !request.getPlayerName().isBlank()) {
            return request.getPlayerName();
        }
        if (request.getPlayerNameFilter() != null && !request.getPlayerNameFilter().isBlank()) {
            return request.getPlayerNameFilter();
        }
        return "Tất cả";
    }

    private void setSummaryColumnWidths(Sheet sheet, DebitReportMode mode) {
        sheet.setColumnWidth(0, 6 * 256);
        sheet.setColumnWidth(1, 30 * 256);
        sheet.setColumnWidth(2, 20 * 256);
        if (mode == DebitReportMode.CURRENT) {
            sheet.setColumnWidth(3, 12 * 256);
            sheet.setColumnWidth(4, 10 * 256);
        } else {
            sheet.setColumnWidth(3, 20 * 256);
            sheet.setColumnWidth(4, 20 * 256);
            sheet.setColumnWidth(5, 18 * 256);
            sheet.setColumnWidth(6, 18 * 256);
            sheet.setColumnWidth(7, 10 * 256);
        }
    }

    private void setDetailColumnWidths(Sheet sheet, DebitReportMode mode) {
        sheet.setColumnWidth(0, 6 * 256);
        sheet.setColumnWidth(1, 30 * 256);
        sheet.setColumnWidth(2, 20 * 256);
        if (mode == DebitReportMode.CURRENT) {
            sheet.setColumnWidth(3, 20 * 256);
            sheet.setColumnWidth(4, 10 * 256);
            sheet.setColumnWidth(5, 35 * 256);
        } else {
            sheet.setColumnWidth(3, 18 * 256);
            sheet.setColumnWidth(4, 18 * 256);
            sheet.setColumnWidth(5, 18 * 256);
            sheet.setColumnWidth(6, 22 * 256);
            sheet.setColumnWidth(7, 22 * 256);
            sheet.setColumnWidth(8, 10 * 256);
            sheet.setColumnWidth(9, 35 * 256);
        }
    }

    private CellStyle createHeaderStyle(SXSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createMoneyStyle(SXSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setDataFormat(workbook.createDataFormat().getFormat("#,##0"));
        style.setAlignment(HorizontalAlignment.RIGHT);
        return style;
    }

    private CellStyle createTextStyle(SXSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setVerticalAlignment(VerticalAlignment.TOP);
        return style;
    }

    private void setMoneyCell(Row row, int col, BigDecimal amount, CellStyle moneyStyle) {
        Cell cell = row.createCell(col);
        if (amount != null) {
            cell.setCellValue(amount.doubleValue());
        } else {
            cell.setCellValue(0d);
        }
        cell.setCellStyle(moneyStyle);
    }

    private String formatDateTime(Instant instant, ZoneId zoneId) {
        if (instant == null) {
            return "";
        }
        return instant.atZone(zoneId).format(DISPLAY_DATETIME_FORMATTER);
    }

    private String defaultCurrency(String currency) {
        return currency != null && !currency.isBlank() ? currency : MoneyUtils.CURRENCY_VN;
    }

    private String translateStatus(DebitStatus status) {
        if (status == null) {
            return "";
        }
        return STATUS_LABELS.getOrDefault(status.name(), status.name());
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

package com.badminton.service.report;

import com.badminton.enums.DebitReportMode;
import com.badminton.model.report.DebitReportData;
import com.badminton.model.report.DebtCurrentReportRow;
import com.badminton.model.report.DebtHistoryReportRow;
import com.badminton.model.report.DebtPlayerSummaryRow;
import com.badminton.requestmodel.debit.DebitReportExportRequest;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DebitExcelReportWriterTest {

    private final DebitExcelReportWriter writer = new DebitExcelReportWriter();

    @Test
    void currentWorkbookIsValidZipAndContainsExpectedSheets() throws IOException {
        DebitReportExportRequest request = createRequest("CURRENT", "ALL_PLAYERS");
        DebitReportData data = DebitReportData.builder()
                .mode(DebitReportMode.CURRENT)
                .summaries(List.of(new DebtPlayerSummaryRow(1, "Nguyễn Văn An",
                        new BigDecimal("150000.00"), 2L, null, null, null, null, null)))
                .currentDetails(List.of(new DebtCurrentReportRow(1, "Nguyễn Văn An",
                        Instant.parse("2026-09-01T00:00:00Z"), new BigDecimal("100000.00"), "VND", "=cmd|'))))")))
                .build();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        writer.write(data, request, ZoneId.of("Asia/Ho_Chi_Minh"), baos);

        byte[] bytes = baos.toByteArray();
        assertTrue(bytes.length > 0);
        assertTrue(isZip(bytes));

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            assertEquals(2, workbook.getNumberOfSheets());
            assertEquals("Tổng hợp", workbook.getSheetName(0));
            assertEquals("Chi tiết", workbook.getSheetName(1));

            Sheet detail = workbook.getSheetAt(1);
            Row headerRow = detail.getRow(5);
            assertNotNull(headerRow);
            assertEquals("Số tiền còn lại", headerRow.getCell(3).getStringCellValue());

            Row dataRow = detail.getRow(6);
            assertNotNull(dataRow);
            assertEquals("Nguyễn Văn An", dataRow.getCell(1).getStringCellValue());
            assertEquals(100000.0, dataRow.getCell(3).getNumericCellValue(), 0.001);

            Cell noteCell = dataRow.getCell(5);
            assertTrue(noteCell.getStringCellValue().startsWith("'"));
        }
    }

    @Test
    void historyWorkbookEscapesFormulaAndTranslatesStatus() throws IOException {
        DebitReportExportRequest request = createRequest("HISTORY", "ALL_PLAYERS");
        DebitReportData data = DebitReportData.builder()
                .mode(DebitReportMode.HISTORY)
                .summaries(List.of(new DebtPlayerSummaryRow(1, "Nguyễn Văn An",
                        null, null,
                        new BigDecimal("200000.00"), new BigDecimal("150000.00"),
                        new BigDecimal("50000.00"), 1L, 2L)))
                .historyDetails(List.of(new DebtHistoryReportRow(1, "Nguyễn Văn An",
                        Instant.parse("2026-09-01T00:00:00Z"),
                        new BigDecimal("200000.00"), new BigDecimal("50000.00"),
                        Instant.parse("2026-09-02T00:00:00Z"),
                        com.badminton.enums.DebitStatus.PARTIALLY_PAID,
                        "VND", "@sum")))
                .build();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        writer.write(data, request, ZoneId.of("Asia/Ho_Chi_Minh"), baos);

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(baos.toByteArray()))) {
            Sheet detail = workbook.getSheetAt(1);
            Row dataRow = detail.getRow(6);
            assertEquals("Thanh toán một phần", dataRow.getCell(7).getStringCellValue());
            assertEquals("'@sum", dataRow.getCell(9).getStringCellValue());
        }
    }

    private static DebitReportExportRequest createRequest(String mode, String scope) {
        DebitReportExportRequest request = new DebitReportExportRequest();
        request.setMode(mode);
        request.setScope(scope);
        request.setPlayerName(null);
        request.setPlayerNameFilter(null);
        request.setFrom("2000-01-01");
        request.setTo("9999-12-31");
        request.setSortField("PLAYER_NAME");
        request.setSortDirection("ASC");
        request.setTimeZone("Asia/Ho_Chi_Minh");
        return request;
    }

    private static boolean isZip(byte[] bytes) {
        return bytes.length >= 4
                && bytes[0] == 'P'
                && bytes[1] == 'K'
                && bytes[2] == 0x03
                && bytes[3] == 0x04;
    }
}

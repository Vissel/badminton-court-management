package com.badminton.core.billing.print;

import com.badminton.entity.Invoice;
import com.badminton.enums.InvoiceStatus;
import com.badminton.enums.InvoiceType;
import com.badminton.exception.BusinessException;
import com.badminton.exception.enums.ErrorCodeEnum;
import com.badminton.repository.InvoiceRepository;
import com.badminton.requestmodel.billing.BillListRequest;
import com.badminton.util.TimeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Excel export of the filtered bill list — same filters as
 * {@code /api/v1/bills/list}. Streams with SXSSF for large ranges.
 */
@Component
public class BillExcelExportService {

    private static final int EXPORT_PAGE_SIZE = 500;
    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneOffset.UTC);

    private static final String[] HEADERS = {
            "Số HĐ", "Thời gian", "Khách hàng", "Đơn vị", "MST", "Loại",
            "Thanh toán", "Giá chưa VAT", "VAT %", "Tiền VAT",
            "Tổng cộng", "Thực thu", "Trạng thái", "HĐĐT", "Thu ngân", "Ghi chú huỷ"
    };

    @Autowired
    private InvoiceRepository invoiceRepository;

    public void exportToStream(BillListRequest request, OutputStream outputStream)
            throws BusinessException {
        Instant from = StringUtils.isNotBlank(request.getFromDate())
                ? TimeUtils.convertToInstant(request.getFromDate())
                : null;
        Instant to = StringUtils.isNotBlank(request.getToDate())
                ? TimeUtils.convertToInstant(request.getToDate())
                : null;
        InvoiceStatus status = parse(InvoiceStatus.class, request.getStatus());
        InvoiceType type = parse(InvoiceType.class, request.getInvoiceType());
        String keyword = StringUtils.isNotBlank(request.getKeyword()) ? request.getKeyword().trim() : null;

        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100)) {
            SXSSFSheet sheet = workbook.createSheet("Hoá đơn");
            // SXSSF can only auto-size columns tracked before rows stream out.
            for (int c = 0; c < HEADERS.length; c++) {
                sheet.trackColumnForAutoSizing(c);
            }
            CellStyle headerStyle = headerStyle(workbook);
            Row header = sheet.createRow(0);
            for (int c = 0; c < HEADERS.length; c++) {
                header.createCell(c).setCellValue(HEADERS[c]);
                header.getCell(c).setCellStyle(headerStyle);
            }

            int rowNum = 1;
            int page = 0;
            Page<Invoice> result;
            do {
                result = invoiceRepository.search(from, to, status, type,
                        request.getSessionId(), keyword,
                        PageRequest.of(page, EXPORT_PAGE_SIZE));
                for (Invoice invoice : result.getContent()) {
                    Row row = sheet.createRow(rowNum++);
                    writeRow(row, invoice);
                }
                page++;
            } while (result.hasNext());

            for (int c = 0; c < HEADERS.length; c++) {
                sheet.autoSizeColumn(c);
                sheet.untrackColumnForAutoSizing(c);
            }

            workbook.write(outputStream);
            workbook.dispose();
        } catch (IOException e) {
            throw new BusinessException(ErrorCodeEnum.INTERNAL_SERVER_ERROR,
                    "Cannot export bills: " + e.getMessage());
        }
    }

    private void writeRow(Row row, Invoice invoice) {
        int c = 0;
        row.createCell(c++).setCellValue(invoice.getBillNo());
        row.createCell(c++).setCellValue(invoice.getIssuedAt() != null ? TS_FMT.format(invoice.getIssuedAt()) : "");
        row.createCell(c++).setCellValue(invoice.getBuyerName() != null
                ? invoice.getBuyerName()
                : invoice.getPlayer() != null ? invoice.getPlayer().getPlayerName() : "");
        row.createCell(c++).setCellValue(nullToEmpty(invoice.getBuyerCompany()));
        row.createCell(c++).setCellValue(nullToEmpty(invoice.getBuyerTaxCode()));
        row.createCell(c++).setCellValue(invoice.getInvoiceType() != null ? invoice.getInvoiceType().name() : "");
        row.createCell(c++).setCellValue(invoice.getPayType() != null ? invoice.getPayType().name() : "");
        row.createCell(c++).setCellValue(doubleVal(invoice.getSubtotal()));
        row.createCell(c++).setCellValue(doubleVal(invoice.getVatRate()));
        row.createCell(c++).setCellValue(doubleVal(invoice.getVatAmount()));
        row.createCell(c++).setCellValue(doubleVal(invoice.getTotal()));
        row.createCell(c++).setCellValue(doubleVal(invoice.getCollectAmount()));
        row.createCell(c++).setCellValue(invoice.getStatus() != null ? invoice.getStatus().name() : "");
        row.createCell(c++).setCellValue(nullToEmpty(invoice.getEinvoiceNo()));
        row.createCell(c++).setCellValue(nullToEmpty(invoice.getIssuedBy()));
        row.createCell(c++).setCellValue(nullToEmpty(invoice.getVoidReason()));
    }

    private double doubleVal(java.math.BigDecimal v) {
        return v != null ? v.doubleValue() : 0d;
    }

    private String nullToEmpty(String s) {
        return s != null ? s : "";
    }

    private CellStyle headerStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private <E extends Enum<E>> E parse(Class<E> type, String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        try {
            return Enum.valueOf(type, value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}

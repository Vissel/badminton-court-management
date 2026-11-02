package com.badminton.controller;

import com.badminton.core.billing.CoreBillingService;
import com.badminton.core.billing.einvoice.EInvoiceService;
import com.badminton.core.billing.print.BillExcelExportService;
import com.badminton.core.billing.print.QrCodeRenderer;
import com.badminton.exception.BusinessException;
import com.badminton.requestmodel.billing.AdjustBillRequest;
import com.badminton.requestmodel.billing.BillConfigRequest;
import com.badminton.requestmodel.billing.BillListRequest;
import com.badminton.requestmodel.billing.ReplaceBillRequest;
import com.badminton.requestmodel.billing.SendEInvoiceRequest;
import com.badminton.requestmodel.billing.ServiceVatRequest;
import com.badminton.requestmodel.billing.TaxReportRequest;
import com.badminton.requestmodel.billing.VoidBillRequest;
import com.badminton.response.PageResponse;
import com.badminton.response.billing.BillConfigResponse;
import com.badminton.response.billing.BillLogResponse;
import com.badminton.response.billing.BillResponse;
import com.badminton.response.billing.ReceiptResponse;
import com.badminton.response.billing.ServiceVatResponse;
import com.badminton.response.billing.TaxReportResponse;
import com.badminton.response.result.Result;
import com.badminton.service.BillingService;
import com.badminton.util.ResponseConvertor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Bill (invoice) management: list/detail/void/print/export + venue billing
 * config. Void, config writes and printer tests are restricted to ROOT and
 * ADMINISTRATOR — the FE only hides the controls, which is bypassable.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/bills")
public class BillingController {

    private static final String XLSX_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    /** QR side length for the browser receipt — 320 px scans from a 72mm page. */
    private static final int QR_PIXELS = 320;

    @Autowired
    private BillingService billingService;
    @Autowired
    private CoreBillingService coreBillingService;
    @Autowired
    private BillExcelExportService billExcelExportService;
    @Autowired
    private EInvoiceService eInvoiceService;

    /**
     * Stateless renderer — instantiated directly so the print package is not
     * pulled into every application context that scans controllers.
     */
    private final QrCodeRenderer qrCodeRenderer = new QrCodeRenderer();

    @PostMapping("/list")
    public ResponseEntity<Result<PageResponse<BillResponse>>> listBills(@RequestBody BillListRequest request) {
        return ResponseConvertor.convert(billingService.listBills(request));
    }

    /**
     * Excel export of the same filtered list as /list.
     */
    @PostMapping(value = "/export", produces = XLSX_CONTENT_TYPE)
    public ResponseEntity<StreamingResponseBody> exportBills(@RequestBody BillListRequest request) {
        StreamingResponseBody stream = outputStream -> {
            try {
                billExcelExportService.exportToStream(request, outputStream);
            } catch (BusinessException e) {
                throw new IOException(e.getMessage(), e);
            }
        };
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=bills.xlsx")
                .header(HttpHeaders.CACHE_CONTROL, CacheControl.noStore().getHeaderValue())
                .contentType(MediaType.parseMediaType(XLSX_CONTENT_TYPE))
                .body(stream);
    }

    @GetMapping("/{billId}")
    public ResponseEntity<Result<BillResponse>> getBill(@PathVariable Long billId) {
        return ResponseConvertor.convert(billingService.getBill(billId));
    }

    @GetMapping("/{billId}/receipt")
    public ResponseEntity<Result<ReceiptResponse>> getReceipt(@PathVariable Long billId) {
        return ResponseConvertor.convert(billingService.getReceipt(billId));
    }

    /**
     * 80mm PDF of the bill — same layout as the ESC/POS raster print.
     */
    @GetMapping(value = "/{billId}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<StreamingResponseBody> exportPdf(@PathVariable Long billId) {
        BillResponse bill;
        try {
            bill = billingService.getBill(billId).getData();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
        String billNo = bill != null && bill.getBillNo() != null ? bill.getBillNo() : billId.toString();
        StreamingResponseBody stream = outputStream -> {
            try {
                outputStream.write(coreBillingService.renderPdf(billId));
            } catch (BusinessException e) {
                throw new IOException(e.getMessage(), e);
            }
        };
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + billNo + ".pdf")
                .header(HttpHeaders.CACHE_CONTROL, CacheControl.noStore().getHeaderValue())
                .contentType(MediaType.APPLICATION_PDF)
                .body(stream);
    }

    /**
     * Print through the given channel — {@code ?channel=NETWORK} pushes the
     * receipt to the configured ESC/POS printer; default reads the configured
     * printer mode; BROWSER just counts the print (the FE ran window.print()).
     */
    @PostMapping("/{billId}/print")
    public ResponseEntity<Result<BillResponse>> printBill(@PathVariable Long billId,
            @RequestParam(required = false) String channel) {
        return ResponseConvertor.convert(billingService.printBill(billId, channel));
    }

    /** Probe the configured network printer (admin only). */
    @PostMapping("/printer/test")
    public ResponseEntity<Result<Void>> testPrinter() {
        if (!hasAdminRole()) {
            return forbidden();
        }
        return ResponseConvertor.convert(billingService.testPrinter());
    }

    @PostMapping("/{billId}/void")
    public ResponseEntity<Result<BillResponse>> voidBill(@PathVariable Long billId,
            @RequestBody VoidBillRequest request) {
        if (!hasAdminRole()) {
            return forbidden();
        }
        return ResponseConvertor.convert(billingService.voidBill(billId, request));
    }

    /**
     * G-1: adjustment invoice for an issued bill (admin only — a correction is
     * a legal act). The new bill is linked to the original and numbered
     * gaplessly.
     */
    @PostMapping("/{billId}/adjust")
    public ResponseEntity<Result<BillResponse>> adjustBill(@PathVariable Long billId,
            @RequestBody AdjustBillRequest request) {
        if (!hasAdminRole()) {
            return forbidden();
        }
        return ResponseConvertor.convert(billingService.adjustBill(billId, request));
    }

    /** G-1: replacement invoice — voids the original once issued. */
    @PostMapping("/{billId}/replace")
    public ResponseEntity<Result<BillResponse>> replaceBill(@PathVariable Long billId,
            @RequestBody ReplaceBillRequest request) {
        if (!hasAdminRole()) {
            return forbidden();
        }
        return ResponseConvertor.convert(billingService.replaceBill(billId, request));
    }

    /**
     * G-5: internal-control history (issue/void/print/e-invoice/config) — admin
     * only.
     */
    @GetMapping("/{billId}/logs")
    public ResponseEntity<Result<List<BillLogResponse>>> getBillLogs(@PathVariable Long billId) {
        if (!hasAdminRole()) {
            return forbidden();
        }
        return ResponseConvertor.convert(billingService.getBillLogs(billId));
    }

    /** G-6: revenue + output VAT summary for a period. */
    @PostMapping("/tax-report")
    public ResponseEntity<Result<TaxReportResponse>> taxReport(
            @RequestBody(required = false) TaxReportRequest request) {
        if (!hasAdminRole()) {
            return forbidden();
        }
        return ResponseConvertor.convert(billingService.taxReport(request));
    }

    /** Excel version of the tax report (same period/grouping). */
    @PostMapping(value = "/tax-report/export", produces = XLSX_CONTENT_TYPE)
    public ResponseEntity<StreamingResponseBody> exportTaxReport(
            @RequestBody(required = false) TaxReportRequest request) {
        if (!hasAdminRole()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        TaxReportResponse report = billingService.taxReport(request).getData();
        StreamingResponseBody stream = outputStream -> {
            try {
                billExcelExportService.exportTaxReportToStream(report, outputStream);
            } catch (BusinessException e) {
                throw new IOException(e.getMessage(), e);
            }
        };
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=tax-report.xlsx")
                .header(HttpHeaders.CACHE_CONTROL, CacheControl.noStore().getHeaderValue())
                .contentType(MediaType.parseMediaType(XLSX_CONTENT_TYPE))
                .body(stream);
    }

    /** G-8: VAT override per service — read for Setup, write is admin only. */
    @GetMapping("/service-vat")
    public ResponseEntity<Result<List<ServiceVatResponse>>> listServiceVat() {
        return ResponseConvertor.convert(billingService.listServiceVat());
    }

    @PutMapping("/service-vat/{serviceId}")
    public ResponseEntity<Result<ServiceVatResponse>> updateServiceVat(@PathVariable Long serviceId,
            @RequestBody ServiceVatRequest request) {
        if (!hasAdminRole()) {
            return forbidden();
        }
        return ResponseConvertor.convert(billingService.updateServiceVat(serviceId, request));
    }

    /**
     * Publish (or retry) the bill to the e-invoice provider — admin only:
     * issuing an e-invoice is a legal act.
     */
    @PostMapping("/{billId}/einvoice")
    public ResponseEntity<Result<BillResponse>> issueEInvoice(@PathVariable Long billId) {
        if (!hasAdminRole()) {
            return forbidden();
        }
        Result<BillResponse> result = new Result<>();
        try {
            result.setData(coreBillingService.getBill(eInvoiceService.issue(billId).getInvoiceId()));
            result.setSuccess(true);
        } catch (Exception e) {
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            result.setErrorCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        return ResponseConvertor.convert(result);
    }

    /** Refresh einvoice_* fields from the provider's status endpoint. */
    @GetMapping("/{billId}/einvoice/status")
    public ResponseEntity<Result<BillResponse>> eInvoiceStatus(@PathVariable Long billId) {
        Result<BillResponse> result = new Result<>();
        try {
            result.setData(coreBillingService.getBill(eInvoiceService.refreshStatus(billId).getInvoiceId()));
            result.setSuccess(true);
        } catch (Exception e) {
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            result.setErrorCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        return ResponseConvertor.convert(result);
    }

    /** Provider-side PDF of the issued e-invoice. */
    @GetMapping(value = "/{billId}/einvoice/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<StreamingResponseBody> downloadEInvoicePdf(@PathVariable Long billId) {
        StreamingResponseBody stream = outputStream -> {
            try {
                outputStream.write(eInvoiceService.downloadPdf(billId));
            } catch (BusinessException e) {
                throw new IOException(e.getMessage(), e);
            }
        };
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=einvoice-" + billId + ".pdf")
                .header(HttpHeaders.CACHE_CONTROL, CacheControl.noStore().getHeaderValue())
                .contentType(MediaType.APPLICATION_PDF)
                .body(stream);
    }

    /** G-3: archived provider XML — kept for the NĐ 254/2026 retention period. */
    @GetMapping(value = "/{billId}/einvoice/xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<StreamingResponseBody> downloadEInvoiceXml(@PathVariable Long billId) {
        StreamingResponseBody stream = outputStream -> {
            try {
                outputStream.write(eInvoiceService.downloadXml(billId).getBytes(StandardCharsets.UTF_8));
            } catch (BusinessException e) {
                throw new IOException(e.getMessage(), e);
            }
        };
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=einvoice-" + billId + ".xml")
                .header(HttpHeaders.CACHE_CONTROL, CacheControl.noStore().getHeaderValue())
                .contentType(MediaType.APPLICATION_XML)
                .body(stream);
    }

    /**
     * G-11: buyer retrieval QR of the bill (Điều 11) as a PNG. The browser
     * receipt pulls it through the API client so the JWT header still applies;
     * the PDF and ESC/POS outputs draw the same payload server-side.
     */
    @GetMapping(value = "/{billId}/qr", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> billQr(@PathVariable Long billId) {
        String payload;
        try {
            BillResponse bill = billingService.getBill(billId).getData();
            payload = bill != null ? bill.getEinvoiceLookupUrl() : null;
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
        byte[] png = qrCodeRenderer.renderPng(payload, QR_PIXELS);
        if (png == null) {
            // No lookup base configured yet — the receipt then prints without a QR.
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, CacheControl.noStore().getHeaderValue())
                .contentType(MediaType.IMAGE_PNG)
                .body(png);
    }

    /**
     * G-11: (re)send the issued e-invoice to the buyer by email. Empty body
     * falls back to the buyer email stored on the bill.
     */
    @PostMapping("/{billId}/einvoice/send")
    public ResponseEntity<Result<BillResponse>> sendEInvoice(@PathVariable Long billId,
            @RequestBody(required = false) SendEInvoiceRequest request) {
        Result<BillResponse> result = new Result<>();
        try {
            result.setData(coreBillingService.getBill(eInvoiceService.sendMail(billId, request).getInvoiceId()));
            result.setSuccess(true);
        } catch (Exception e) {
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            result.setErrorCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        return ResponseConvertor.convert(result);
    }

    @GetMapping("/config")
    public ResponseEntity<Result<BillConfigResponse>> getConfig() {
        return ResponseConvertor.convert(billingService.getConfig());
    }

    @PutMapping("/config")
    public ResponseEntity<Result<BillConfigResponse>> updateConfig(@RequestBody BillConfigRequest request) {
        if (!hasAdminRole()) {
            return forbidden();
        }
        return ResponseConvertor.convert(billingService.updateConfig(request));
    }

    private boolean hasAdminRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ROOT")
                        || authority.getAuthority().equals("ROLE_ADMINISTRATOR"));
    }

    private <T> ResponseEntity<Result<T>> forbidden() {
        Result<T> result = new Result<>();
        result.setSuccess(false);
        result.setErrorCode(HttpStatus.FORBIDDEN.value());
        result.setErrorMessage("Bạn không có quyền thực hiện thao tác này.");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(result);
    }
}

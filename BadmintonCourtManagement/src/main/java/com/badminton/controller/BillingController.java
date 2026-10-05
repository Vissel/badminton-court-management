package com.badminton.controller;

import com.badminton.core.billing.CoreBillingService;
import com.badminton.core.billing.einvoice.EInvoiceService;
import com.badminton.core.billing.print.BillExcelExportService;
import com.badminton.exception.BusinessException;
import com.badminton.requestmodel.billing.BillConfigRequest;
import com.badminton.requestmodel.billing.BillListRequest;
import com.badminton.requestmodel.billing.VoidBillRequest;
import com.badminton.response.PageResponse;
import com.badminton.response.billing.BillConfigResponse;
import com.badminton.response.billing.BillResponse;
import com.badminton.response.billing.ReceiptResponse;
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

    @Autowired
    private BillingService billingService;
    @Autowired
    private CoreBillingService coreBillingService;
    @Autowired
    private BillExcelExportService billExcelExportService;
    @Autowired
    private EInvoiceService eInvoiceService;

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
        result.setErrorMessage("Forbidden");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(result);
    }
}

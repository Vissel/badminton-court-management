package com.badminton.service;

import com.badminton.requestmodel.billing.AdjustBillRequest;
import com.badminton.requestmodel.billing.BillConfigRequest;
import com.badminton.requestmodel.billing.BillListRequest;
import com.badminton.requestmodel.billing.ReplaceBillRequest;
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

import java.util.List;

public interface BillingService {

    Result<PageResponse<BillResponse>> listBills(BillListRequest request);

    Result<BillResponse> getBill(Long billId);

    /** Bill + seller profile merged for the print view. */
    Result<ReceiptResponse> getReceipt(Long billId);

    Result<BillResponse> voidBill(Long billId, VoidBillRequest request);

    /**
     * Print through the given channel ("NETWORK" | "BROWSER", null = use the
     * configured mode) and count the print event.
     */
    Result<BillResponse> printBill(Long billId, String channel);

    /** Connectivity probe for the configured network printer. */
    Result<Void> testPrinter();

    Result<BillConfigResponse> getConfig();

    Result<BillConfigResponse> updateConfig(BillConfigRequest request);

    /** G-1: raise an adjustment invoice linked to the original bill. */
    Result<BillResponse> adjustBill(Long billId, AdjustBillRequest request);

    /** G-1: raise a replacement invoice and void the original. */
    Result<BillResponse> replaceBill(Long billId, ReplaceBillRequest request);

    /** G-5: internal-control history of one bill. */
    Result<List<BillLogResponse>> getBillLogs(Long billId);

    /** G-6: period revenue + output VAT summary. */
    Result<TaxReportResponse> taxReport(TaxReportRequest request);

    /** G-8: VAT overrides per service (Setup page). */
    Result<List<ServiceVatResponse>> listServiceVat();

    Result<ServiceVatResponse> updateServiceVat(Long serviceId, ServiceVatRequest request);
}

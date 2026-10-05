package com.badminton.service;

import com.badminton.requestmodel.billing.BillConfigRequest;
import com.badminton.requestmodel.billing.BillListRequest;
import com.badminton.requestmodel.billing.VoidBillRequest;
import com.badminton.response.PageResponse;
import com.badminton.response.billing.BillConfigResponse;
import com.badminton.response.billing.BillResponse;
import com.badminton.response.billing.ReceiptResponse;
import com.badminton.response.result.Result;

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
}

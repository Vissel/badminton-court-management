package com.badminton.response.billing;

import lombok.Data;

/**
 * Payload for the print view: bill lines merged with the seller profile so
 * the FE can render the receipt without a second config fetch.
 */
@Data
public class ReceiptResponse {
    private BillResponse bill;
    private BillConfigResponse seller;
    /** BROWSER or NETWORK — FE uses BROWSER view, NETWORK is backend-driven. */
    private String printerMode;
    private Integer paperWidth;
    private boolean autoPrint;
}

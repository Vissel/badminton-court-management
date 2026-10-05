package com.badminton.response.billing;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * Bill detail — also used as the list row shape (items may be omitted there).
 */
@Data
public class BillResponse {
    private Long billId;
    private String billNo;
    private String invoiceType;
    private Integer sessionId;
    private String playerName;
    private String buyerName;
    private String buyerCompany;
    private String buyerTaxCode;
    private String buyerAddress;
    private String buyerEmail;
    private BigDecimal subtotal;
    private BigDecimal vatRate;
    private BigDecimal vatAmount;
    private BigDecimal total;
    private BigDecimal collectAmount;
    private String currency;
    private String payType;
    private String status;
    private String issuedBy;
    private String issuedAt;
    private String voidedBy;
    private String voidedAt;
    private String voidReason;
    private String einvoiceStatus;
    private String einvoiceNo;
    private String einvoicePdfUrl;
    /** Last provider submission error — shown on FAILED bills for ops review. */
    private String einvoiceError;
    private int printCount;
    private String note;
    private List<BillItemResponse> items;
}

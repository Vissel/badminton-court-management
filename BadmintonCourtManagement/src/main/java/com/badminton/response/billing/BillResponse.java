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
    private BigDecimal discountAmount;
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
    /** Buyer retrieval link encoded in the receipt QR (Điều 11). */
    private String einvoiceLookupUrl;
    /** True once the provider XML is archived and downloadable. */
    private boolean einvoiceXmlAvailable;
    /** Original bill this correction points at (null on forward bills). */
    private Long relatedBillId;
    private String relatedBillNo;
    private String correctionType;
    private String adjustReason;
    /** True when at least one correction bill already references this bill. */
    private boolean corrected;
    private int printCount;
    private String note;
    private List<BillItemResponse> items;
}

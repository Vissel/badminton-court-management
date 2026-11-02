package com.badminton.model.dto;

import com.badminton.constant.PayType;
import com.badminton.model.billing.BuyerInfo;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class PaymentDTO {
    private String playerName;
    private List<ServiceDTO> services;
    private String totalPay;
    private PayType payType;
    private CreateDebitDTO debit;
    private AllocateDebitPaymentRequest payDebits;
    /**
     * Optional buyer tax info for the bill (business customers).
     */
    private BuyerInfo buyer;
    /**
     * Optional per-bill VAT % override — only honoured for ADMINISTRATOR/ROOT.
     */
    private BigDecimal vatRate;
    /**
     * FE-generated idempotency key (one per checkout attempt). A retry with the
     * same key returns the bill created by the first attempt instead of
     * issuing a duplicate.
     */
    private String clientTransactionId;
    /**
     * Optional invoice-level discount (gross, price-inclusive). Recorded as a
     * negative DISCOUNT line so the printed total matches what was collected.
     */
    private BigDecimal discountAmount;
}

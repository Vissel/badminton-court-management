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
}

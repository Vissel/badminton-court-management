package com.badminton.model.dto;

import com.badminton.constant.PayType;
import com.badminton.model.billing.BuyerInfo;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Builder
@Data
public class AllocateDebitPaymentRequest {
    private String playerName;
    private BigDecimal payAmount;
    private PayType payMethod;
    private String note;
    private List<DebitPayDTO> listDebitPay;
    /**
     * TRUE only for standalone debt settlement (/api/v1/debit/pay). Checkout
     * folds settled debts into its own bill via "Trả nợ" lines instead.
     */
    private Boolean issueBill;
    /** Optional buyer tax info for the bill. */
    private BuyerInfo buyer;
    /** Optional per-bill VAT % override — ADMINISTRATOR/ROOT only. */
    private BigDecimal vatRate;
}

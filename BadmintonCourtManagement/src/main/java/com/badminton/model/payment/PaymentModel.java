package com.badminton.model.payment;

import com.badminton.constant.PayType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
public class PaymentModel {
    private String payFor;
    private PayType payType;
    private BigDecimal payAmount;
    private Instant payTime;
    private String services;
    private BigDecimal debitAmount;
    private BigDecimal paidDebts;
    private BigDecimal remainingDebts;
    private Integer numPaidDebts;
    private Integer numRemainingDebts;
    private String payDebitsMessage;
}

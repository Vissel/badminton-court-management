package com.badminton.model.payment;

import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
public class PaymentModel {
    private String payFor;
    private String payType;
    private BigDecimal payAmount;
    private Instant payTime;
    private String services;
    private BigDecimal debitAmount;
}

package com.badminton.model.debit;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Builder
@Data
public class PayDebitModel {
    private String playerName;
    private BigDecimal paymentAmount;
    private String paymentMethod;
    private DebitModel paidDebit;
    private Instant paymentDate;
    private String status;
}

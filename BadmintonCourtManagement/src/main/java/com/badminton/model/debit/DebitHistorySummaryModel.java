package com.badminton.model.debit;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class DebitHistorySummaryModel {
    private String playerName;
    private BigDecimal totalDebitAmount;
    private BigDecimal totalPaidAmount;
    private BigDecimal totalRemainingAmount;
    private int numDebits;
    private int numPaidDebits;
    private int numUnpaidDebits;
    private String currency;
}

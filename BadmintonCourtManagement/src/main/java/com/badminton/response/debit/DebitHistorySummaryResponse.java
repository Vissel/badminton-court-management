package com.badminton.response.debit;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DebitHistorySummaryResponse {
    private String playerName;
    private float totalDebitAmount;
    private float totalPaidAmount;
    private float totalRemainingAmount;
    private int numDebits;
    private int numPaidDebits;
    private int numUnpaidDebits;
    private String currency;
}

package com.badminton.model.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DebtCurrentReportRow {
    private Integer debitId;
    private String playerName;
    private Instant debtDate;
    private BigDecimal remainingAmount;
    private String currency;
    private String note;
}

package com.badminton.model.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DebtPlayerSummaryRow {
    private Integer playerId;
    private String playerName;

    // Current debt mode
    private BigDecimal totalRemainingDebt;
    private Long numDebits;

    // History mode
    private BigDecimal totalDebt;
    private BigDecimal totalPaid;
    private BigDecimal totalRemaining;
    private Long paidDebits;
    private Long totalDebits;
}

package com.badminton.model.report;

import com.badminton.enums.DebitStatus;
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
public class DebtHistoryReportRow {
    private Integer debitId;
    private String playerName;
    private Instant debtDate;
    private BigDecimal debtAmount;
    private BigDecimal remainingAmount;
    private Instant lastPaymentDate;
    private DebitStatus status;
    private String currency;
    private String note;

    public BigDecimal getPaidAmount() {
        if (debtAmount == null) {
            return BigDecimal.ZERO;
        }
        return debtAmount.subtract(remainingAmount != null ? remainingAmount : BigDecimal.ZERO);
    }
}

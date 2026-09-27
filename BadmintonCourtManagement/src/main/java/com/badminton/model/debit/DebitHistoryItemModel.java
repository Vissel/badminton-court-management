package com.badminton.model.debit;

import com.badminton.enums.DebitStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class DebitHistoryItemModel {
    private int debitId;
    /** when the debt was created */
    private Instant debtDateTime;
    /** latest payment date applied to this debit, null when never paid */
    private Instant paidDateTime;
    private BigDecimal debtAmount;
    private BigDecimal remainingAmount;
    private BigDecimal paidAmount;
    private DebitStatus status;
    private String currency;
    private String note;
}

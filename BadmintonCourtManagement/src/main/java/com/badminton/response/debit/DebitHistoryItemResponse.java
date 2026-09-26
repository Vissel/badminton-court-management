package com.badminton.response.debit;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DebitHistoryItemResponse {
    private String playerName;
    private float debtAmount;
    private String debtDateTime;
    private float paidAmount;
    private String paidDateTime;
    private float remainingAmount;
    private String currency;
    private String status;
    private String note;
}

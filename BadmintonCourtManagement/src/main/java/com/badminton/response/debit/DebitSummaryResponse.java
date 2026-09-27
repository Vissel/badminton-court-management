package com.badminton.response.debit;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DebitSummaryResponse {
    private String playerName;
    private MoneyResponse totalDebts;
    private int numberDebit;
}

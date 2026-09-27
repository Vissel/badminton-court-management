package com.badminton.response.debit;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.sql.Timestamp;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DebitResponse {
    private BigDecimal debtAmount;
    private String currency;
    private Timestamp createdDate;
    private String note;
    private String playerName;
}

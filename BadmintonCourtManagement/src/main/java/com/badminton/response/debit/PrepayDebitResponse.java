package com.badminton.response.debit;

import lombok.Data;

import java.util.List;

@Data
public class PrepayDebitResponse {
    private String playerName;
    private float paymentAmount;
    private List<PrepayDebit> prepayDebits;
}

package com.badminton.response.debit;

import lombok.Data;

@Data
public class PayDebitResponse {
    private String playerName;
    private float paymentAmount;
    private String paymentMethod;
    private float remainingDebts;
    private String paymentDate;
    private String status;
}

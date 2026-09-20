package com.badminton.response.debit;

import lombok.Data;

@Data
public class PayDebitResponse {
    private String playerName;
    private float paymentAmount;
    private String paymentMethod;
    private float paidDebts;
    private float remainingDebts;
    private int numPaidDebts;
    private int numRemainingDebts;
    private String paymentDate;
    private String status;
    private String message;
}

package com.badminton.response.debit;

import lombok.Data;
import com.badminton.constant.PayType;

@Data
public class PayDebitResponse {
    private String playerName;
    private float paymentAmount;
    private PayType paymentMethod;
    private float paidDebts;
    private float remainingDebts;
    private int numPaidDebts;
    private int numRemainingDebts;
    private String paymentDate;
    private String status;
    private String message;
}

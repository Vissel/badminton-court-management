package com.badminton.model.dto;

import com.badminton.constant.PayType;
import com.badminton.enums.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Builder
@Data
public class AllocateDebitPaymentResponse {
    private String playerName;
    private BigDecimal paymentAmount;
    private PayType paymentMethod;
    private BigDecimal paidDebts;
    private BigDecimal remainingDebts;
    private int numPaidDebts;
    private int numRemainingDebts;
    private Instant paymentDate;
    private PaymentStatus status;
    private String message;
    private int errorCode;
}

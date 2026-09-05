package com.badminton.requestmodel.debit;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class PayDebitRequest {
    @NotBlank(message = "Player name must not be blank")
    private String playerName;

    @Positive(message = "Payment amount must be positive")
    private float paymentAmount;

    @NotBlank(message = "Payment method must not be blank")
    private String paymentMethod;
}

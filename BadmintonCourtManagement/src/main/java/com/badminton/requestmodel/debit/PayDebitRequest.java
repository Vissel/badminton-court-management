package com.badminton.requestmodel.debit;

import com.badminton.constant.PayType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.util.List;

@Data
public class PayDebitRequest {
    @NotBlank(message = "Player name must not be blank")
    private String playerName;

    @Positive(message = "Payment amount must be positive")
    private float totalPayAmount;

    @NotNull(message = "Payment method must not be null")
    private PayType paymentMethod;

    private String note;
    
    private List<DebitPayRequest> listDebitPay;

}

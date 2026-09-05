package com.badminton.requestmodel.debit;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class DebitRequest {
    @NotNull
    @Positive
    private float debitAmount;

    private String currency;

    private String note;

    @NotNull
    private String playerName;

    @NotNull
    private String createdTime;
}

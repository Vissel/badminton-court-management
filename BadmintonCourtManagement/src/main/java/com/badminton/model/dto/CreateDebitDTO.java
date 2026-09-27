package com.badminton.model.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
public class CreateDebitDTO {
    private BigDecimal debitAmount;

    private String currency;

    private String note;

    private String playerName;

    private Instant createdTime;
}

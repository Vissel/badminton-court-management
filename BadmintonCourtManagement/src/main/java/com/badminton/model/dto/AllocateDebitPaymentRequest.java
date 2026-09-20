package com.badminton.model.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Builder
@Data
public class AllocateDebitPaymentRequest {
    private String playerName;
    private BigDecimal payAmount;
    private String payMethod;
    private String note;
}


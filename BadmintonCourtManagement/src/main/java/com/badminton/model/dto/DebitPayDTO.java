package com.badminton.model.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Builder
@Data
public class DebitPayDTO {
    private String dateTime;
    private BigDecimal payAmount;
}

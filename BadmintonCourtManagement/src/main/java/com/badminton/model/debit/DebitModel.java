package com.badminton.model.debit;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class DebitModel {
    private Instant dateTime;
    private BigDecimal money;
    private String note;
}

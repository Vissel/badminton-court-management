package com.badminton.model.debit;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.sql.Timestamp;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DebitSummaryModel {
    private BigDecimal totalDebts;
    private String currency;
    private Integer numDebts;
    private Timestamp lastUpdate;
}

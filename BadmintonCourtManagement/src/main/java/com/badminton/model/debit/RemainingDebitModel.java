package com.badminton.model.debit;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class RemainingDebitModel {
    private String playerName;
    private BigDecimal totalDebts;
    private int numberDebit;
    private List<DebitModel> debitModels;
}

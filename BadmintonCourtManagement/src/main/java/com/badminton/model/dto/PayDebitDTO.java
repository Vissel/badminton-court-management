package com.badminton.model.dto;

import com.badminton.constant.PayType;
import com.badminton.model.debit.DebitModel;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Builder
@Data
public class PayDebitDTO {
    private String playerName;
    private BigDecimal payAmount;
    private PayType payMethod;
    private String note;
    private DebitModel payForDebit;
}

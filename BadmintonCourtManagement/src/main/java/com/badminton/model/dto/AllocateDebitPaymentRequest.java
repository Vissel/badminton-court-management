package com.badminton.model.dto;

import com.badminton.constant.PayType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Builder
@Data
public class AllocateDebitPaymentRequest {
    private String playerName;
    private BigDecimal payAmount;
    private PayType payMethod;
    private String note;
    private List<DebitPayDTO> listDebitPay;
}

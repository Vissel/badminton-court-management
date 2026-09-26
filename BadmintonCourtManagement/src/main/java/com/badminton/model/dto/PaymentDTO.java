package com.badminton.model.dto;

import com.badminton.constant.PayType;
import lombok.Data;

import java.util.List;

@Data
public class PaymentDTO {
    private String playerName;
    private List<ServiceDTO> services;
    private String totalPay;
    private PayType payType;
    private CreateDebitDTO debit;
    private AllocateDebitPaymentRequest payDebits;
}

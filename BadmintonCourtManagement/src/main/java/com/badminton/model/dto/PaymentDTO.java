package com.badminton.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class PaymentDTO {
    private String playerName;
    private List<ServiceDTO> services;
    private String totalPay;
    private String payType;
    private CreateDebitDTO debit;
}

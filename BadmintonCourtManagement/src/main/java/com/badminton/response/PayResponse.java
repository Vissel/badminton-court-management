package com.badminton.response;

import com.badminton.constant.PayType;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Pay for debit
 */
@Data
@AllArgsConstructor
public class PayResponse {
    private String playerName;
    private String services;
    private PayType payType;
    private Float payAmount;
    private String payTime;
    private Float debitAmount;
    private Float paidDebts;
    private Float remainingDebts;
    private Integer numPaidDebts;
    private Integer numRemainingDebts;
    private String payDebitsMessage;
    private Long billId;
    private String billNo;
}

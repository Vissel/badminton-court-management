package com.badminton.model.player;

import com.badminton.constant.PayType;
import com.badminton.core.payment.PayStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AvailablePlayerPayModel {
    private String name;
    private PayType payType;
    private BigDecimal payAmount;
    private Instant payTime;
    private String services;
    private PayStatus payStatus;
}

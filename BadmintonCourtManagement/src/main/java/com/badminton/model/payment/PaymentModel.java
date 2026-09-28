package com.badminton.model.payment;

import com.badminton.constant.PayType;
import com.badminton.model.dto.ServiceDTO;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
public class PaymentModel {
    private BigDecimal amount;

    private String currency = "VND";

    private Instant paymentDate;

    private String note;

    private PayType payType;

    private String payFor;

}

package com.badminton.model.report;

import com.badminton.constant.PayType;
import com.badminton.model.payment.PaymentModel;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
public class PayDebitModel {
   private PaymentModel paymentModel;

    private boolean isAddedToRpt;
}

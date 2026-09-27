package com.badminton.requestmodel.debit;

import lombok.Data;

@Data
public class DebitPayRequest {
    private String dateTime;
    private float payAmount;
}

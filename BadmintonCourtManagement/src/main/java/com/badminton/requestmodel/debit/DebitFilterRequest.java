package com.badminton.requestmodel.debit;

import lombok.Data;

@Data
public class DebitFilterRequest {
    private String from;
    private String to;
    private float amountFrom;
    private float amountTo;
}

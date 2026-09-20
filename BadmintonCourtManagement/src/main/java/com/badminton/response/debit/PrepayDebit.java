package com.badminton.response.debit;

import lombok.Data;

@Data
public class PrepayDebit {
    private RemainingDebitsResponse payDebit;
    private String payStatus;
}

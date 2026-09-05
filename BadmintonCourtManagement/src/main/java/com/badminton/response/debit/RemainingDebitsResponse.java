package com.badminton.response.debit;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RemainingDebitsResponse {
    private String dateTime;
    private MoneyResponse money;
    private String note;
}

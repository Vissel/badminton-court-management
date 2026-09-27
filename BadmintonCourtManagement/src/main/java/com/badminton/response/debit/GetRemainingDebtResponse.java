package com.badminton.response.debit;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class GetRemainingDebtResponse {
    private String playerName;
    private DebitSummaryResponse debitSummary;
    private List<RemainingDebitsResponse> remainingDebits;
}

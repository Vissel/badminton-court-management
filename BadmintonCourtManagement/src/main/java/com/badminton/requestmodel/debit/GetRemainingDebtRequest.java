package com.badminton.requestmodel.debit;

import com.badminton.requestmodel.Pagination;
import lombok.Data;

import java.util.List;

@Data
public class GetRemainingDebtRequest {
    private Pagination pagination;
    private DebitFilterRequest filter;
    private List<String> playerNames;
}

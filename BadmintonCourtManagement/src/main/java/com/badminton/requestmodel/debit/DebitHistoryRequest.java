package com.badminton.requestmodel.debit;

import com.badminton.requestmodel.Pagination;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DebitHistoryRequest {
    @NotBlank
    private String playerName;
    private Pagination pagination;
    private DebitFilterRequest filter;
}

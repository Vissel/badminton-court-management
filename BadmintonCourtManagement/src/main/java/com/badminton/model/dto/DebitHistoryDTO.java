package com.badminton.model.dto;

import com.badminton.requestmodel.Pagination;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class DebitHistoryDTO {
    private String playerName;
    private Pagination pagination;
    private Instant from;
    private Instant to;
    private Float amountFrom;
    private Float amountTo;
}

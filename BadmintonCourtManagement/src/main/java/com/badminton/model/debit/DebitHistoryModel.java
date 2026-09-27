package com.badminton.model.debit;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DebitHistoryModel {
    private String playerName;
    private List<DebitHistoryItemModel> items;
    private long total;
    private int totalPage;
}

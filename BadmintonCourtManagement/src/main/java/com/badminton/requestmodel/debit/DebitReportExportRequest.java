package com.badminton.requestmodel.debit;

import lombok.Data;

@Data
public class DebitReportExportRequest {
    private String mode;
    private String scope;
    private String playerName;
    private String playerNameFilter;
    private String from;
    private String to;
    private Float amountFrom;
    private Float amountTo;
    private String sortField;
    private String sortDirection;
    private String timeZone;
}

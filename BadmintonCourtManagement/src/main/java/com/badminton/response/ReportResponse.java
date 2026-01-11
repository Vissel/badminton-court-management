package com.badminton.response;

import lombok.Data;

import java.time.Instant;

@Data
public class ReportResponse {
    private int no;
    private int sessionId;
    private DateResponse date;
    private String during;
    private float grossRevenue;

    public void setDate(Instant date) {
        this.date = new DateResponse(date);
    }
}

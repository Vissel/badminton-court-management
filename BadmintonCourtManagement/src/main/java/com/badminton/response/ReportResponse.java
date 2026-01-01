package com.badminton.response;

import lombok.Data;

@Data
public class ReportResponse {
    private int no;
    private int sessionId;
    private String date;
    private String during;
    private float grossRevenue;
}

package com.badminton.response;

import com.badminton.util.MoneyUtils;
import lombok.Data;

import java.time.Instant;

@Data
public class ReportResponse {
    private int no;
    private int sessionId;
    private DateResponse date;
    private String during;
    private float grossRevenue;
    private String grossRevenueFormat;
    private String currency = MoneyUtils.CURRENCY_VN;

    public void setDate(Instant date) {
        this.date = new DateResponse(date);
    }
}

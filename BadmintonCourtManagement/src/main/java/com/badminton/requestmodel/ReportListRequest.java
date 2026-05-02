package com.badminton.requestmodel;

import lombok.Data;

@Data
public class ReportListRequest {
    private String yearMonth;
    private String date;
    private String fromToDate;
    private Pagination pagination;

}

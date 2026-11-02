package com.badminton.requestmodel.billing;

import lombok.Data;

/**
 * Period revenue/VAT summary. Dates accept ISO-8601 or yyyy-MM-dd;
 * {@code toDate} is exclusive (the FE sends end-of-day).
 */
@Data
public class TaxReportRequest {
    private String fromDate;
    private String toDate;
    /** DAY | MONTH | QUARTER (default MONTH). */
    private String groupBy;
}

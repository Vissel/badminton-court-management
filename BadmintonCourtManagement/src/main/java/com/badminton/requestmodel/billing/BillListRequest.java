package com.badminton.requestmodel.billing;

import com.badminton.requestmodel.Pagination;
import lombok.Data;

/**
 * Filters for the bill list. Dates are ISO-8601 or yyyy-MM-dd (see
 * {@link com.badminton.util.TimeUtils#convertToInstant}); {@code toDate} is
 * exclusive — the FE sends end-of-day.
 */
@Data
public class BillListRequest {
    private String fromDate;
    private String toDate;
    private String status;
    private String invoiceType;
    private Integer sessionId;
    /** Matches bill no, player name, buyer name/company/tax code. */
    private String keyword;
    private Pagination pagination;
}

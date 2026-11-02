package com.badminton.response.billing;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Period revenue + VAT summary (bảng kê bán ra). Amounts are price-inclusive
 * gross; net and VAT come from the snapshot stored on each bill.
 */
@Data
public class TaxReportResponse {
    private String fromDate;
    private String toDate;
    /** DAY | MONTH | QUARTER */
    private String groupBy;
    private long billCount;
    private BigDecimal gross = BigDecimal.ZERO;
    private BigDecimal net = BigDecimal.ZERO;
    private BigDecimal vat = BigDecimal.ZERO;
    private List<Row> rows = new ArrayList<>();

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Row {
        /** yyyy-MM-dd | yyyy-MM | yyyy-Qn */
        private String period;
        private long billCount;
        private BigDecimal gross;
        private BigDecimal net;
        private BigDecimal vat;
    }
}

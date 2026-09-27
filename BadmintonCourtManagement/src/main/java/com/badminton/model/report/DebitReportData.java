package com.badminton.model.report;

import com.badminton.enums.DebitReportMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DebitReportData {
    private DebitReportMode mode;
    private List<DebtPlayerSummaryRow> summaries;
    private List<DebtCurrentReportRow> currentDetails;
    private List<DebtHistoryReportRow> historyDetails;
}

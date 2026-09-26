package com.badminton.repository;

import com.badminton.model.report.DebtCurrentReportRow;
import com.badminton.model.report.DebtHistoryReportRow;
import com.badminton.model.report.DebtPlayerSummaryRow;

import java.time.Instant;
import java.util.List;

public interface DebitReportRepositoryCustom {

    List<DebtPlayerSummaryRow> findCurrentSummary(Integer playerId,
                                                  String playerNameFilter,
                                                  Instant from,
                                                  Instant to,
                                                  String sortClause);

    List<DebtCurrentReportRow> findCurrentDetails(Integer playerId,
                                                  String playerNameFilter,
                                                  Instant from,
                                                  Instant to,
                                                  String sortClause,
                                                  int limit,
                                                  int offset);

    List<DebtPlayerSummaryRow> findHistorySummary(Integer playerId,
                                                  String playerNameFilter,
                                                  Instant from,
                                                  Instant to,
                                                  String sortClause);

    List<DebtHistoryReportRow> findHistoryDetails(Integer playerId,
                                                  String playerNameFilter,
                                                  Instant from,
                                                  Instant to,
                                                  String sortClause,
                                                  int limit,
                                                  int offset);
}

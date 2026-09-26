package com.badminton.core.debit;

import com.badminton.entity.Player;
import com.badminton.enums.DebitReportMode;
import com.badminton.enums.DebitReportScope;
import com.badminton.enums.DebitReportSortDirection;
import com.badminton.enums.DebitReportSortField;
import com.badminton.model.report.DebitReportData;
import com.badminton.model.report.DebtCurrentReportRow;
import com.badminton.model.report.DebtHistoryReportRow;
import com.badminton.model.report.DebtPlayerSummaryRow;
import com.badminton.repository.DebitReportRepository;
import com.badminton.repository.PaymentDebitRepository;
import com.badminton.repository.UserRepository;
import com.badminton.requestmodel.debit.DebitReportExportRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CoreDebitReportService {

    private static final int DETAIL_CHUNK_SIZE = 1000;
    private static final Instant MYSQL_TIMESTAMP_MIN = Instant.parse("1970-01-01T00:00:01Z");
    private static final Instant MYSQL_TIMESTAMP_MAX = Instant.parse("2038-01-19T03:14:06Z");

    @Autowired
    private DebitReportRepository debitReportRepository;

    @Autowired
    private PaymentDebitRepository paymentDebitRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public DebitReportData loadReportData(DebitReportExportRequest request,
                                          DebitReportMode mode,
                                          DebitReportScope scope,
                                          DebitReportSortField sortField,
                                          DebitReportSortDirection sortDirection,
                                          ZoneId zoneId) {
        Integer playerId = resolvePlayerId(scope, request.getPlayerName());
        String playerNameFilter = resolvePlayerNameFilter(scope, request.getPlayerNameFilter());
        Instant from = parseDateBound(request.getFrom(), zoneId, true);
        Instant to = parseDateBound(request.getTo(), zoneId, false);

        if (from.isAfter(to)) {
            throw new IllegalArgumentException("'from' date must be before or equal to 'to' date");
        }

        String summarySortClause = buildSummarySortClause(mode, sortField, sortDirection);
        String detailSortClause = buildDetailSortClause(mode, sortField, sortDirection);

        List<DebtPlayerSummaryRow> summaries;
        List<DebtCurrentReportRow> currentDetails = new ArrayList<>();
        List<DebtHistoryReportRow> historyDetails = new ArrayList<>();

        if (mode == DebitReportMode.CURRENT) {
            summaries = debitReportRepository.findCurrentSummary(playerId, playerNameFilter, from, to, summarySortClause);
            fetchCurrentDetailsInChunks(playerId, playerNameFilter, from, to, detailSortClause, currentDetails);
        } else {
            summaries = debitReportRepository.findHistorySummary(playerId, playerNameFilter, from, to, summarySortClause);
            fetchHistoryDetailsInChunks(playerId, playerNameFilter, from, to, detailSortClause, historyDetails);
        }

        return DebitReportData.builder()
                .mode(mode)
                .summaries(summaries)
                .currentDetails(currentDetails)
                .historyDetails(historyDetails)
                .build();
    }

    private Integer resolvePlayerId(DebitReportScope scope, String playerName) {
        if (scope == DebitReportScope.PLAYER) {
            if (playerName == null || playerName.isBlank()) {
                throw new IllegalArgumentException("playerName is required when scope is PLAYER");
            }
            Player player = userRepository.findByPlayerName(playerName)
                    .orElseThrow(() -> new IllegalArgumentException("Player not found: " + playerName));
            return player.getPlayerId();
        }
        return null;
    }

    private String resolvePlayerNameFilter(DebitReportScope scope, String playerNameFilter) {
        if (scope == DebitReportScope.PLAYER || playerNameFilter == null || playerNameFilter.isBlank()) {
            return null;
        }
        return playerNameFilter.trim();
    }

    private Instant parseDateBound(String dateString, ZoneId zoneId, boolean startOfDay) {
        if (dateString == null || dateString.isBlank()) {
            return startOfDay ? MYSQL_TIMESTAMP_MIN : MYSQL_TIMESTAMP_MAX;
        }
        try {
            LocalDate date = LocalDate.parse(dateString);
            Instant instant = startOfDay
                    ? date.atStartOfDay(zoneId).toInstant()
                    : date.atTime(LocalTime.MAX).atZone(zoneId).toInstant();
            return clampToMysqlRange(instant);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date format: " + dateString);
        }
    }

    private static Instant clampToMysqlRange(Instant instant) {
        if (instant.isBefore(MYSQL_TIMESTAMP_MIN)) {
            return MYSQL_TIMESTAMP_MIN;
        }
        if (instant.isAfter(MYSQL_TIMESTAMP_MAX)) {
            return MYSQL_TIMESTAMP_MAX;
        }
        return instant;
    }

    private void fetchCurrentDetailsInChunks(Integer playerId,
                                            String playerNameFilter,
                                            Instant from,
                                            Instant to,
                                            String sortClause,
                                            List<DebtCurrentReportRow> accumulator) {
        int offset = 0;
        while (true) {
            List<DebtCurrentReportRow> chunk = debitReportRepository.findCurrentDetails(
                    playerId, playerNameFilter, from, to, sortClause, DETAIL_CHUNK_SIZE, offset);
            if (chunk == null || chunk.isEmpty()) {
                break;
            }
            accumulator.addAll(chunk);
            if (chunk.size() < DETAIL_CHUNK_SIZE) {
                break;
            }
            offset += DETAIL_CHUNK_SIZE;
        }
    }

    private void fetchHistoryDetailsInChunks(Integer playerId,
                                            String playerNameFilter,
                                            Instant from,
                                            Instant to,
                                            String sortClause,
                                            List<DebtHistoryReportRow> accumulator) {
        int offset = 0;
        while (true) {
            List<DebtHistoryReportRow> chunk = debitReportRepository.findHistoryDetails(
                    playerId, playerNameFilter, from, to, sortClause, DETAIL_CHUNK_SIZE, offset);
            if (chunk == null || chunk.isEmpty()) {
                break;
            }
            fillLastPaymentDates(chunk);
            accumulator.addAll(chunk);
            if (chunk.size() < DETAIL_CHUNK_SIZE) {
                break;
            }
            offset += DETAIL_CHUNK_SIZE;
        }
    }

    private void fillLastPaymentDates(List<DebtHistoryReportRow> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        List<Integer> debitIds = rows.stream()
                .map(DebtHistoryReportRow::getDebitId)
                .toList();
        Map<Integer, Instant> lastPaymentDates = paymentDebitRepository.findLastPaymentDateByDebitIds(debitIds)
                .stream()
                .collect(Collectors.toMap(
                        row -> (Integer) row[0],
                        row -> (Instant) row[1]));
        for (DebtHistoryReportRow row : rows) {
            row.setLastPaymentDate(lastPaymentDates.get(row.getDebitId()));
        }
    }

    private String buildSummarySortClause(DebitReportMode mode,
                                          DebitReportSortField sortField,
                                          DebitReportSortDirection direction) {
        String primary;
        if (sortField == DebitReportSortField.PLAYER_NAME) {
            primary = "p.playerName";
        } else if (mode == DebitReportMode.CURRENT) {
            primary = "SUM(d.remainingAmount)";
        } else {
            primary = "SUM(d.debtAmount)";
        }
        String dir = direction.name();
        return primary + " " + dir + ", p.playerId ASC";
    }

    private String buildDetailSortClause(DebitReportMode mode,
                                           DebitReportSortField sortField,
                                           DebitReportSortDirection direction) {
        String primary;
        if (sortField == DebitReportSortField.PLAYER_NAME) {
            primary = "p.playerName";
        } else if (mode == DebitReportMode.CURRENT) {
            primary = "d.remainingAmount";
        } else {
            primary = "d.debtAmount";
        }
        String dir = direction.name();
        return primary + " " + dir + ", d.debitId ASC";
    }
}

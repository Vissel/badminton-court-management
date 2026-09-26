package com.badminton.repository.impl;

import com.badminton.enums.DebitStatus;
import com.badminton.model.report.DebtCurrentReportRow;
import com.badminton.model.report.DebtHistoryReportRow;
import com.badminton.model.report.DebtPlayerSummaryRow;
import com.badminton.repository.DebitReportRepositoryCustom;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

public class DebitReportRepositoryImpl implements DebitReportRepositoryCustom {

    private static final int NUMERIC_SCALE = 2;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<DebtPlayerSummaryRow> findCurrentSummary(Integer playerId,
                                                         String playerNameFilter,
                                                         Instant from,
                                                         Instant to,
                                                         String sortClause) {
        StringBuilder jpql = new StringBuilder(
                "SELECT p.playerId, p.playerName, COALESCE(SUM(d.remainingAmount), 0), COUNT(d.debitId) " +
                "FROM Debit d JOIN d.player p " +
                "WHERE d.remainingAmount > 0 " +
                "AND d.createdDate >= :from AND d.createdDate <= :to ");
        appendScopeAndFilter(jpql, playerId, playerNameFilter);
        jpql.append("GROUP BY p.playerId, p.playerName ");
        jpql.append("ORDER BY ").append(sortClause);

        Query query = entityManager.createQuery(jpql.toString(), Object[].class)
                .setParameter("from", from)
                .setParameter("to", to);
        setScopeAndFilterParams(query, playerId, playerNameFilter);

        List<Object[]> rows = query.getResultList();
        return rows.stream()
                .map(this::toCurrentSummaryRow)
                .collect(Collectors.toList());
    }

    @Override
    public List<DebtCurrentReportRow> findCurrentDetails(Integer playerId,
                                                         String playerNameFilter,
                                                         Instant from,
                                                         Instant to,
                                                         String sortClause,
                                                         int limit,
                                                         int offset) {
        StringBuilder jpql = new StringBuilder(
                "SELECT d.debitId, p.playerName, d.createdDate, d.remainingAmount, d.currency, d.note " +
                "FROM Debit d JOIN d.player p " +
                "WHERE d.remainingAmount > 0 " +
                "AND d.createdDate >= :from AND d.createdDate <= :to ");
        appendScopeAndFilter(jpql, playerId, playerNameFilter);
        jpql.append("ORDER BY ").append(sortClause);

        Query query = entityManager.createQuery(jpql.toString(), Object[].class)
                .setParameter("from", from)
                .setParameter("to", to)
                .setFirstResult(offset)
                .setMaxResults(limit);
        setScopeAndFilterParams(query, playerId, playerNameFilter);

        List<Object[]> rows = query.getResultList();
        return rows.stream()
                .map(this::toCurrentDetailRow)
                .collect(Collectors.toList());
    }

    @Override
    public List<DebtPlayerSummaryRow> findHistorySummary(Integer playerId,
                                                         String playerNameFilter,
                                                         Instant from,
                                                         Instant to,
                                                         String sortClause) {
        StringBuilder jpql = new StringBuilder(
                "SELECT p.playerId, p.playerName, " +
                "COALESCE(SUM(d.debtAmount), 0), " +
                "COALESCE(SUM(d.debtAmount - d.remainingAmount), 0), " +
                "COALESCE(SUM(d.remainingAmount), 0), " +
                "SUM(CASE WHEN d.remainingAmount = 0 THEN 1 ELSE 0 END), " +
                "COUNT(d.debitId) " +
                "FROM Debit d JOIN d.player p " +
                "WHERE d.createdDate >= :from AND d.createdDate <= :to ");
        appendScopeAndFilter(jpql, playerId, playerNameFilter);
        jpql.append("GROUP BY p.playerId, p.playerName ");
        jpql.append("ORDER BY ").append(sortClause);

        Query query = entityManager.createQuery(jpql.toString(), Object[].class)
                .setParameter("from", from)
                .setParameter("to", to);
        setScopeAndFilterParams(query, playerId, playerNameFilter);

        List<Object[]> rows = query.getResultList();
        return rows.stream()
                .map(this::toHistorySummaryRow)
                .collect(Collectors.toList());
    }

    @Override
    public List<DebtHistoryReportRow> findHistoryDetails(Integer playerId,
                                                         String playerNameFilter,
                                                         Instant from,
                                                         Instant to,
                                                         String sortClause,
                                                         int limit,
                                                         int offset) {
        StringBuilder jpql = new StringBuilder(
                "SELECT d.debitId, p.playerName, d.createdDate, d.debtAmount, d.remainingAmount, " +
                "d.status, d.currency, d.note " +
                "FROM Debit d JOIN d.player p " +
                "WHERE d.createdDate >= :from AND d.createdDate <= :to ");
        appendScopeAndFilter(jpql, playerId, playerNameFilter);
        jpql.append("ORDER BY ").append(sortClause);

        Query query = entityManager.createQuery(jpql.toString(), Object[].class)
                .setParameter("from", from)
                .setParameter("to", to)
                .setFirstResult(offset)
                .setMaxResults(limit);
        setScopeAndFilterParams(query, playerId, playerNameFilter);

        List<Object[]> rows = query.getResultList();
        return rows.stream()
                .map(this::toHistoryDetailRow)
                .collect(Collectors.toList());
    }

    private void appendScopeAndFilter(StringBuilder jpql, Integer playerId, String playerNameFilter) {
        if (playerId != null) {
            jpql.append("AND p.playerId = :playerId ");
        }
        if (playerNameFilter != null && !playerNameFilter.isBlank()) {
            jpql.append("AND LOWER(p.playerName) LIKE LOWER(:playerNameFilter) ");
        }
    }

    private void setScopeAndFilterParams(Query query, Integer playerId, String playerNameFilter) {
        if (playerId != null) {
            query.setParameter("playerId", playerId);
        }
        if (playerNameFilter != null && !playerNameFilter.isBlank()) {
            query.setParameter("playerNameFilter", "%" + playerNameFilter + "%");
        }
    }

    private DebtPlayerSummaryRow toCurrentSummaryRow(Object[] row) {
        return new DebtPlayerSummaryRow(
                (Integer) row[0],
                (String) row[1],
                toBigDecimal(row[2], NUMERIC_SCALE),
                toLong(row[3]),
                null, null, null, null, null);
    }

    private DebtPlayerSummaryRow toHistorySummaryRow(Object[] row) {
        return new DebtPlayerSummaryRow(
                (Integer) row[0],
                (String) row[1],
                null, null,
                toBigDecimal(row[2], NUMERIC_SCALE),
                toBigDecimal(row[3], NUMERIC_SCALE),
                toBigDecimal(row[4], NUMERIC_SCALE),
                toLong(row[5]),
                toLong(row[6]));
    }

    private DebtCurrentReportRow toCurrentDetailRow(Object[] row) {
        return new DebtCurrentReportRow(
                (Integer) row[0],
                (String) row[1],
                toInstant(row[2]),
                toBigDecimal(row[3], NUMERIC_SCALE),
                (String) row[4],
                (String) row[5]);
    }

    private DebtHistoryReportRow toHistoryDetailRow(Object[] row) {
        return new DebtHistoryReportRow(
                (Integer) row[0],
                (String) row[1],
                toInstant(row[2]),
                toBigDecimal(row[3], NUMERIC_SCALE),
                toBigDecimal(row[4], NUMERIC_SCALE),
                null,
                toDebitStatus(row[5]),
                (String) row[6],
                (String) row[7]);
    }

    private static BigDecimal toBigDecimal(Object value, int scale) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(scale, RoundingMode.HALF_UP);
        }
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal.setScale(scale, RoundingMode.HALF_UP);
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue()).setScale(scale, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO.setScale(scale, RoundingMode.HALF_UP);
    }

    private static Long toLong(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return 0L;
    }

    private static Instant toInstant(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Instant instant) {
            return instant;
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toInstant();
        }
        if (value instanceof java.util.Date date) {
            return date.toInstant();
        }
        return null;
    }

    private static DebitStatus toDebitStatus(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof DebitStatus status) {
            return status;
        }
        try {
            return DebitStatus.valueOf(value.toString());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}

package com.badminton.util;

import com.badminton.repository.filter.SessionParam;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class TimeUtils {
    /**
     * Converts a relative time string to a UTC Instant.
     * "today" -> Start of current day (00:00:00 UTC)
     * "yesterday" -> Start of previous day (00:00:00 UTC)
     */
    public static Instant convertToInstant(String timeString) throws IllegalArgumentException {
        if (timeString == null || timeString.isBlank()) {
            throw new IllegalArgumentException("Invalid time format: " + timeString +
                    ". Use 'today', 'yesterday', YYYY-MM-DD, or ISO-8601 timestamp.");
        }

        String input = timeString.trim();
        String lowerInput = input.toLowerCase();

        // 1. Handle Shortcuts
        if ("today".equals(lowerInput)) {
            return LocalDate.now(ZoneOffset.UTC).atStartOfDay(ZoneOffset.UTC).toInstant();
        }
        if ("yesterday".equals(lowerInput)) {
            return LocalDate.now(ZoneOffset.UTC).minusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        }

        // 2. Try parsing as a full Instant (ISO-8601: 2026-01-01T10:00:00Z)
        try {
            return Instant.parse(input);
        } catch (DateTimeParseException e) {
            // If it's not a full timestamp, move to the next check
        }

        // 3. Try parsing as a simple Date (2026-01-01)
        try {
            return LocalDate.parse(input)
                    .atStartOfDay(ZoneOffset.UTC)
                    .toInstant();
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid time format: " + input +
                    ". Use 'today', 'yesterday', YYYY-MM-DD, or ISO-8601 timestamp.");
        }
    }

    public static SessionParam convertYearMonthToInstant(String yearMonthParam) {
        YearMonth yearMonth = VietnameseMonthConverter.convertToYearMonth(yearMonthParam);

        ZoneId zoneId = ZoneId.systemDefault(); // or ZoneOffset.UTC (recommended)

        Instant start = yearMonth
                .atDay(1)
                .atStartOfDay(zoneId)
                .toInstant();

        Instant end = yearMonth
                .plusMonths(1)
                .atDay(1)
                .atStartOfDay(zoneId)
                .toInstant();
        SessionParam param = new SessionParam();
        param.setFrom(start);
        param.setTo(end);
        return param;
    }

    // Define the formatter as a constant for performance
    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("H:mm").withZone(ZoneOffset.UTC);

    public static String convertInstantsToString(Instant from, Instant to) {
        if (from == null || to == null) {
            return "N/A"; // Or return "" based on your preference
        }

        String startTime = TIME_FORMATTER.format(from);
        String endTime = TIME_FORMATTER.format(to);

        return String.format("%s - %s", startTime, endTime);
    }
}

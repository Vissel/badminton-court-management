package com.badminton.util;

import com.badminton.constant.CommonConstant;
import com.badminton.repository.filter.SessionParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Calendar;
import java.util.Locale;

@Slf4j
public class TimeUtils {
    /**
     * Converts a relative time string to a UTC Instant.
     * "today" -> Start of current day (00:00:00 UTC)
     * "yesterday" -> Start of previous day (00:00:00 UTC)
     */
    public static Instant convertToInstant(String timeString) throws IllegalArgumentException {
        if (timeString == null || timeString.isBlank()) {
            throw new IllegalArgumentException("Invalid time format: " + timeString + ". Use 'today', 'yesterday', YYYY-MM-DD, or ISO-8601 timestamp.");
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
            return LocalDate.parse(input).atStartOfDay(ZoneOffset.UTC).toInstant();
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid time format: " + input + ". Use 'today', 'yesterday', YYYY-MM-DD, or ISO-8601 timestamp.");
        }
    }

    public static SessionParam convertYearMonthToInstant(String yearMonthParam) {
        SessionParam param = new SessionParam();
        if (StringUtils.isBlank(yearMonthParam)) {
            return param;
        }
        String normalized = yearMonthParam.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");

        String[] parts = normalized.split("\\s+");
        int year = Integer.parseInt(parts[0]);
        Month month = Month.valueOf(parts[1].toUpperCase(Locale.ENGLISH));
        YearMonth yearMonth = YearMonth.of(year, month);
//                VietnameseMonthConverter.convertToYearMonth(yearMonthParam);

        ZoneId zoneId = ZoneId.systemDefault(); // or ZoneOffset.UTC (recommended)

        Instant start = yearMonth.atDay(1).atStartOfDay(zoneId).toInstant();

        Instant end = yearMonth.plusMonths(1).atDay(1).atStartOfDay(zoneId).toInstant();
        param.setFrom(start);
        param.setTo(end);
        return param;
    }

    // Define the formatter as a constant for performance
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("H:mm").withZone(ZoneOffset.UTC);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC);
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd H:mm:ss").withZone(ZoneOffset.UTC);

    public static String convertInstantsToString(Instant from, Instant to) {
        String startTime = convertInstantToTimeStr(from);
        String endTime = convertInstantToTimeStr(to);

        return String.format("%s - %s", startTime, endTime);
    }

    public static String convertInstantToTimeStr(Instant time) {
        if (time == null) {
            return CommonConstant.EMPTY; // Or return "" based on your preference
        }
        return TIME_FORMATTER.format(time);
    }

    /**
     * @param instant Instant to format (UTC)
     * @param locale  Locale.ENGLISH or new Locale("vi")
     */
    public static String toDateDisplay(Instant instant, Locale locale) {
        if (instant == null) {
            return CommonConstant.EMPTY;
        }

        LocalDate date = instant.atZone(ZoneOffset.UTC).toLocalDate();
        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        if (date.equals(today)) {
            return isVietnamese(locale) ? "Hôm nay" : "Today";
        }

        if (date.equals(today.minusDays(1))) {
            return isVietnamese(locale) ? "Hôm qua" : "Yesterday";
        }

        return DATE_FORMATTER.format(instant);
    }

    private static boolean isVietnamese(Locale locale) {
        return locale != null && "vi".equalsIgnoreCase(locale.getLanguage());
    }

    public static String convertInstantToDateStr(Instant date) {
        if (date == null) {
            return CommonConstant.EMPTY; // Or return "" based on your preference
        }
        return DATE_FORMATTER.format(date);
    }

    public static Locale newVNLocal() {
        return new Locale("vi", "VN");
    }

    public static String toDateTimeDisplay(Instant dateTime) {
        if (dateTime == null) {
            return CommonConstant.EMPTY; // Or return "" based on your preference
        }
        return DATETIME_FORMATTER.format(dateTime);
    }

    // 1. Convert to ZonedDateTime (Preserves the timezone info)
    public static ZonedDateTime toSystemZoned(Instant instant) {
        return instant.atZone(ZoneId.systemDefault());
    }

    // 2. Convert to LocalDateTime (Standard "wall-clock" time)
    public static LocalDateTime toLocalDateTime(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }

    /**
     * Requirement: Format for the Session Header
     * Example: "2026, tháng 1 ngày 6 thứ ba"
     */
    public static String toVNDateFormat(Instant instant) {
        if (instant == null) return "";

        DateTimeFormatter formatter = DateTimeFormatter
                .ofPattern("yyyy, 'tháng' M 'ngày' d EEEE", newVNLocal());

        return toSystemZoned(instant).format(formatter);
    }

    /**
     * get UTC +7 instant time
     *
     * @return
     */
    public static Instant getUTCPlus7Instant() {
        // Create a Calendar instance
        Calendar calendar = Calendar.getInstance();
        // Plus 7 hours
        calendar.add(Calendar.HOUR, +7);
        log.info("Convert to DB time zone value: {}", calendar.toInstant().toString());
        return calendar.toInstant();
    }
}

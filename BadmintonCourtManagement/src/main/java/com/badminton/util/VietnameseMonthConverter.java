package com.badminton.util;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class VietnameseMonthConverter {

    private static final Map<String, String> VN_TO_EN_MONTH = new HashMap<>();

    static {
        VN_TO_EN_MONTH.put("tháng 1", "January");
        VN_TO_EN_MONTH.put("tháng 2", "February");
        VN_TO_EN_MONTH.put("tháng 3", "March");
        VN_TO_EN_MONTH.put("tháng 4", "April");
        VN_TO_EN_MONTH.put("tháng 5", "May");
        VN_TO_EN_MONTH.put("tháng 6", "June");
        VN_TO_EN_MONTH.put("tháng 7", "July");
        VN_TO_EN_MONTH.put("tháng 8", "August");
        VN_TO_EN_MONTH.put("tháng 9", "September");
        VN_TO_EN_MONTH.put("tháng 10", "October");
        VN_TO_EN_MONTH.put("tháng 11", "November");
        VN_TO_EN_MONTH.put("tháng 12", "December");
    }

    private VietnameseMonthConverter() {
    }

    public static YearMonth convertToYearMonth(String monthParam) {

        String normalized = monthParam
                .trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ");

        // Case 1: numeric month (Tháng 1, Tháng 01)
        if (normalized.matches("\\d{4}\\s+tháng\\s+\\d{1,2}")) {
            String[] parts = normalized.split("\\s+");
            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[2]);
            return YearMonth.of(year, month);
        }

        // Case 2: Vietnamese text month
        for (Map.Entry<String, String> entry : VN_TO_EN_MONTH.entrySet()) {
            if (normalized.contains(entry.getKey())) {
                String english = normalized.replace(entry.getKey(), entry.getValue());
                return YearMonth.parse(
                        english,
                        DateTimeFormatter.ofPattern("yyyy MMMM", Locale.ENGLISH)
                );
            }
        }

        throw new IllegalArgumentException(
                "Invalid month format. Expected: '2025 Tháng Một' or '2025 Tháng 1'"
        );
    }
}

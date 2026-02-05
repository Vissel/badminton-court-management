package com.badminton.service.impl;

import com.badminton.repository.SessionRepository;
import com.badminton.response.MonthYearResponse;
import com.badminton.service.DateTimeService;
import com.badminton.util.TimeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Month;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class DateTimeServiceImpl implements DateTimeService {
    @Autowired
    SessionRepository sessionRepository;

    @Override
    public List<MonthYearResponse> getMonthYearList() {

        List<MonthYearResponse> result = new ArrayList<>();

        // ✅ Top item: "Tat ca"
        result.add(new MonthYearResponse("", "Tất cả"));

        List<Object[]> rows =
                sessionRepository.findDistinctYearMonthFromSessions();

        for (Object[] row : rows) {
            int year = ((Number) row[0]).intValue();
            int month = ((Number) row[1]).intValue();

            // ✅ English value (for converter)
            String monthEn = Month.of(month)
                    .getDisplayName(TextStyle.FULL, Locale.ENGLISH);
            String value = year + " " + monthEn;

            // ✅ Vietnamese label (for UI)
            String monthVi = Month.of(month)
                    .getDisplayName(TextStyle.FULL, TimeUtils.newVNLocal());
            String label = monthVi + " " + year;

            result.add(new MonthYearResponse(value, label));
        }

        return result;
    }

    @Override
    public Boolean inTheSameDay() {
        return null;
    }
}

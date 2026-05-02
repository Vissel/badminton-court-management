package com.badminton.service;

import com.badminton.response.MonthYearResponse;

import java.util.List;

public interface DateTimeService {
    List<MonthYearResponse> getMonthYearList();

    Boolean inTheSameDay();
}

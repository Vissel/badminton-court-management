package com.badminton.response;

import com.badminton.util.TimeUtils;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.Locale;

@Getter
@Setter
public class DateResponse {
    private Instant instant;
    private String dateString;
    private String viDateString;

    public DateResponse(Instant date) {
        this.instant = date;
        this.dateString = TimeUtils.toDateDisplay(date, Locale.ENGLISH);
        this.viDateString = TimeUtils.toDateDisplay(date, TimeUtils.newVNLocal());
    }
}
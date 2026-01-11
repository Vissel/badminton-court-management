package com.badminton.response.result;

import com.badminton.util.TimeUtils;
import lombok.Data;

import java.time.Instant;

@Data
public class SessionResult {
    private int id;
    private Instant fromTime;
    private Instant toTime;
    private String message;

    public void setFromTime(Instant fromTime) {
        this.fromTime = fromTime;
        setMessage("Session start from:" + TimeUtils.toDateTimeDisplay(fromTime));
    }

    public void setToTime(Instant toTime) {
        this.toTime = toTime;
        setMessage("Session start from:" + TimeUtils.toDateTimeDisplay(fromTime) + " to:" + TimeUtils.toDateTimeDisplay(toTime));
    }

}

package com.badminton.response.result;

import lombok.Data;

import java.time.Instant;

@Data
public class SessionResult {
    private int id;
    private Instant fromTime;
    private Instant toTime;

}

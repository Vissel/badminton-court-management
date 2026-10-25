package com.badminton.model.session;

import lombok.Data;

import java.time.Instant;

@Data
public class SessionModel {
    private Instant fromTime;
    private Instant toTime;
    private String message;
}

package com.badminton.model;

import lombok.Data;

import java.time.Instant;

@Data
public class SessionModel {
    private Instant fromTime;

    private Instant toTime;

    private boolean isActive;
}

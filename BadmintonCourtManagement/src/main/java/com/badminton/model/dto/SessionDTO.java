package com.badminton.model.dto;

import lombok.Data;

import java.time.Instant;

@Data
public class SessionDTO {
    private Integer sessionId;
    private Instant fromTime;
    private Instant toTime;
    private boolean isActive;
}

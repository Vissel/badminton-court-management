package com.badminton.response.result;

import lombok.Data;

@Data
public class SessionResult {
    private String fromTime;
    private String toTime;
    private String message;
    private int retry;

}

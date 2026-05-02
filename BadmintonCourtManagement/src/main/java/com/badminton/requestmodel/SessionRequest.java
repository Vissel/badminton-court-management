package com.badminton.requestmodel;

import lombok.Data;

@Data
public class SessionRequest {
    private boolean scheduler;

    public SessionRequest(boolean scheduler) {
        this.scheduler = scheduler;
    }
}

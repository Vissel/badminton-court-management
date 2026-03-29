package com.badminton.model;

import lombok.Data;

@Data
public class CacheObject {
    private Object value;
    private long expiryTime;

    public CacheObject(Object value, long ttlMillis) {
        this.value = value;
        this.expiryTime = System.currentTimeMillis() + ttlMillis;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expiryTime;
    }
}

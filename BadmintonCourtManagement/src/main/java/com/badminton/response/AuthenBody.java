package com.badminton.response;

import lombok.Data;

@Data
public class AuthenBody {
    private String username;
    private String token;
    private boolean valid;
    private long expiresInSeconds;

    public AuthenBody(String username, String token, boolean valid, long expiresInSeconds) {
        this.username = username;
        this.token = token;
        this.valid = valid;
        this.expiresInSeconds = expiresInSeconds;
    }

    public AuthenBody() {
    }

    ;
}
package com.badminton.response;

import com.google.gson.Gson;
import lombok.Data;

@Data
public class AuthenResponse {
    private int status;
    private String message;
    private AuthenBody body;
    private String username;
    private String token;
    private boolean valid;
    private long expiresInSeconds;

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }

}

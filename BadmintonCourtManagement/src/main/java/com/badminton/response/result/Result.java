package com.badminton.response.result;

import lombok.Data;

@Data
public class Result<T> {
    /**
     * success result
     */
    private boolean success;

    /**
     * data
     */
    private T data;

    /**
     * error message
     */
    private String errorMessage;
}

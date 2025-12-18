package com.badminton.util;

import com.badminton.response.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;

@Slf4j
public class ResponseConvertor {


    public static final <T> ResponseEntity<Result<T>> convert(Result<T> response) {
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(response.getErrorCode()).body(response);
    }
}

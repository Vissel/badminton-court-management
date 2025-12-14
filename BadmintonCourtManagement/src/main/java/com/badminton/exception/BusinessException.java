package com.badminton.exception;

import com.badminton.exception.enums.BizCode;
import lombok.Data;

@Data
public class BusinessException extends Exception {
    private BizCode bizCode;
    private String errorMessage;

    public BusinessException(BizCode bizCode, String errorMessage) {
        super(errorMessage);
        this.bizCode = bizCode;
        this.errorMessage = errorMessage;
    }
}

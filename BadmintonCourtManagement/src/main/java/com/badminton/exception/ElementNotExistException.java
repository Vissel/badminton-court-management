package com.badminton.exception;

import com.badminton.exception.enums.ErrorCodeEnum;

public class ElementNotExistException extends BusinessException {

    public ElementNotExistException(ErrorCodeEnum errorCodeEnum, String errorMessage) {
        super(errorCodeEnum, errorMessage);
        // TODO Auto-generated constructor stub
    }

}

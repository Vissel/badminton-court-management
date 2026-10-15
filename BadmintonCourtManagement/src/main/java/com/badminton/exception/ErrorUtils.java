package com.badminton.exception;

import com.badminton.exception.enums.ErrorCodeEnum;

public class ErrorUtils {
    public static String buildErrorMessage(ErrorCodeEnum errorCodeEnum) {
        return errorCodeEnum.getCode().concat("-").concat(errorCodeEnum.getName()).concat("-").concat(errorCodeEnum.getDescription());
    }
}

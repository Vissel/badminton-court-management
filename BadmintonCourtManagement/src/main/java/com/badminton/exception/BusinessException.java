package com.badminton.exception;

import com.badminton.exception.enums.ErrorCodeEnum;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

@Data
public class BusinessException extends Exception {
    private ErrorCodeEnum errorCodeEnum;
    private String errorMessage;
    private Object data;

    public BusinessException(ErrorCodeEnum errorCodeEnum, String errorMessage) {
        super(errorMessage);
        this.errorCodeEnum = errorCodeEnum;
        this.errorMessage = StringUtils.isNotEmpty(errorMessage) ? errorMessage : errorCodeEnum.getDescription();
    }

    public BusinessException(ErrorCodeEnum errorCodeEnum, String errorMessage, Object data) {
        super(errorMessage);
        this.errorCodeEnum = errorCodeEnum;
        this.errorMessage = StringUtils.isNotEmpty(errorMessage) ? errorMessage : errorCodeEnum.getDescription();
        this.data = data;
    }

    public BusinessException(ErrorCodeEnum errorCodeEnum) {
        super(errorCodeEnum.getDescription());
        this.errorCodeEnum = errorCodeEnum;
        this.errorMessage = errorCodeEnum.getDescription();
    }
}

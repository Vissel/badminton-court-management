package com.badminton.service;

import com.badminton.exception.BusinessException;
import com.badminton.exception.ErrorMess;
import com.badminton.exception.enums.ErrorCodeEnum;
import com.badminton.response.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ServiceTemple {
    public <R, T> Result<T> execute(ProcessCallback<R, T> callback) {
        Result<T> result = new Result<>();
        String errorMess = null;
        result.setSuccess(false);
        try {
            callback.preProcess(callback.getRequest());
            T data = callback.process();
            result.setSuccess(true);
            result.setData(data);
        } catch (IllegalArgumentException e) {
            log.error(ErrorMess.REQUEST_VAL, e.getMessage());
            result.setErrorCode(HttpStatus.BAD_REQUEST.value());
            errorMess = e.getMessage();
        } catch (BusinessException e) {
            log.error(ErrorMess.BIZ_VAL, e.getMessage());
            result.setErrorCode(HttpStatus.CONFLICT.value());
            errorMess = e.getMessage();
        } catch (Throwable e) {
            log.error(ErrorMess.INTERNAL_SERVER_ERROR, e.getMessage());
            result.setErrorCode(Integer.valueOf(ErrorCodeEnum.INTERNAL_SERVER_ERROR.getCode()));
            errorMess = "Server error.";
        } finally {
            result.setErrorMessage(errorMess);
        }
        return result;
    }

    private <R> void preProcess(R request) {
    }
}

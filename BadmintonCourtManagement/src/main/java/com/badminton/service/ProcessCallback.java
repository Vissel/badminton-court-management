package com.badminton.service;

import com.badminton.exception.BusinessException;

public interface ProcessCallback<R, T> {

    R getRequest();

    void preProcess(R request);

    T process() throws BusinessException;
}

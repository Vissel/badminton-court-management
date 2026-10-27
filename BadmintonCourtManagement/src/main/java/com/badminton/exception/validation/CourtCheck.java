package com.badminton.exception.validation;

import com.badminton.entity.Court;
import com.badminton.exception.BusinessException;
import com.badminton.exception.enums.ErrorCodeEnum;

import java.util.Optional;

public class CourtCheck {
    public static void isCourtPresent(Optional<Court> optCourt) throws BusinessException {
        if (optCourt == null || optCourt.isEmpty()) {
            throw new BusinessException(ErrorCodeEnum.COURT_NOT_FOUND, "Court is not found.");
        }
    }
}

package com.badminton.exception.validation;

import com.badminton.exception.BusinessException;
import com.badminton.exception.enums.ErrorCodeEnum;

public class DebitCheck {
    public static void isCreated(Boolean creationDebt) throws BusinessException {
        if (creationDebt == null || !creationDebt) {
            throw new BusinessException(ErrorCodeEnum.DEBTS_CREATION_FAILURE);
        }
    }
}

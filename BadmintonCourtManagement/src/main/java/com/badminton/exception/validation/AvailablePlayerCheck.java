package com.badminton.exception.validation;

import com.badminton.entity.AvailablePlayer;
import com.badminton.exception.BusinessException;
import com.badminton.exception.enums.ErrorCodeEnum;

import java.util.Optional;

public class AvailablePlayerCheck {
    public static void isAvailablePlayerPresent(Optional<AvailablePlayer> optAvailablePlayer) throws BusinessException {
        if (!optAvailablePlayer.isPresent()) {
            throw new BusinessException(ErrorCodeEnum.PLAYER_NOT_FOUND, "Available player is not found.");
        }
    }
}

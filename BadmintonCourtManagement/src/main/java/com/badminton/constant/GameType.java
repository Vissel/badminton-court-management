package com.badminton.constant;

import lombok.Getter;

@Getter
public enum GameType {
    /**
     * there are winners and losers will pay
     */
    SHARE,

    /**
     * there are winners but there are negotiate money together.
     * that means winners can have expense.
     */
    NEGO
}

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
    NEGO,
    /**
     * RENT_BY_TIME 
     * this is a type of game that a player rent a court by time.
     */
    RENT
}

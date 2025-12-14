package com.badminton.exception.enums;

import lombok.Getter;

@Getter
public enum BizCode {
    GAME_NOT_FOUND("1", "GAME_NOT_FOUND", "Game is not found"),
    WINNER_NOT_FOUND("2", "WINNER_NOT_FOUND", "No winner finding");

    private String code;
    private String name;
    private String description;

    BizCode(String code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }
}

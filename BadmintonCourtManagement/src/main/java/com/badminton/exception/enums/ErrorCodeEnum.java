package com.badminton.exception.enums;

import lombok.Getter;

@Getter
public enum ErrorCodeEnum {
    GAME_NOT_FOUND("100", "GAME_NOT_FOUND", "Game is not found"),
    WINNER_NOT_FOUND("101", "WINNER_NOT_FOUND", "No winner finding"),
    SHUTTLE_BALL_NOT_FOUND("102", "SHUTTLE_BALL_NOT_FOUND", "No shuttle ball finding"),
    PLAYER_NOT_FOUND("103", "PLAYER_NOT_FOUND", "Player is not found"),
    AVAILABLE_PLAYER_NOT_FOUND("104", "AVAILABLE_PLAYER_NOT_FOUND", "Available player“““ is not found"),
    INCORRECT_TYPE("411", "INCORRECT_TYPE", "Provided data type is incorrect."),
    FLOW_ERROR("412", "FLOW_ERROR", "Consequence flow has error."),
    INTERNAL_SERVER_ERROR("500", "INTERNAL_SERVER_ERROR", "Server error."),
    EXPORTING_ERROR("413", "EXPORTING_ERROR", "Error while exporting."),
    SESSION_ERROR("510", "SESSION_ERROR", "Session error"),
    DEBIT_NOT_FOUND("511", "DEBIT_NOT_FOUND", "Debit is not found"),
    INVALID_PAYMENT_AMOUNT("512", "INVALID_PAYMENT_AMOUNT", "Invalid payment amount"),
    CURRENT_SESSION_NOT_FOUND("513", "CURRENT_SESSION_NOT_FOUND", "Current session not found"),
    DEBTS_NOT_FOUND("514", "DEBTS_NOT_FOUND", "There no debts found");

    private String code;
    private String name;
    private String description;

    ErrorCodeEnum(String code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }
}

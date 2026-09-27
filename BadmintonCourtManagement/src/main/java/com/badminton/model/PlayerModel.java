package com.badminton.model;

import lombok.Data;

import java.sql.Timestamp;

@Data
public class PlayerModel {
    private String playerName;

    private String password;

    private Timestamp createdDate;
}

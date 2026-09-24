package com.badminton.response.player;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlayerResponse {
    private int playerId;
    private String playerName;
    private Timestamp createdDate;
}

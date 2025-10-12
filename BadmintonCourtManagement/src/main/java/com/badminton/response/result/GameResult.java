package com.badminton.response.result;

import lombok.Data;

import java.sql.Timestamp;
import java.util.Map;

@Data
public class GameResult {
    private int gameId;

    private CourtResult courtResult;

    private Map<ShuttleBallResult, Integer> ballResultMap;

    private TeamResult teamOneResult;

    private TeamResult teamTwoResult;

    private Timestamp createdDate;

    private Timestamp endedDate;

    /**
     * Follow GameState
     */
    private String state;
}

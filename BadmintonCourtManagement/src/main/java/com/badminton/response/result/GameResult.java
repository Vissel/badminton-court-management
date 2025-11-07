package com.badminton.response.result;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
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

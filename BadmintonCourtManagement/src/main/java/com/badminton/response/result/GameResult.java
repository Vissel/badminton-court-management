package com.badminton.response.result;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GameResult {
    private int gameId;

    private CourtResult courtResult;

    private Map<ShuttleBallResponse, Integer> ballResultMap;

    private TeamResult teamOneResult;

    private TeamResult teamTwoResult;

    private Instant createdDate;

    private Instant endedDate;

    /**
     * Follow GameState
     */
    private String state;
}

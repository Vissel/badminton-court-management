package com.badminton.response.result;

import lombok.Data;

import java.util.List;

@Data
public class GameManagementResult {
    private List<GameResult> gameResultList;

    private List<SessionResult> sessionResultList;
}

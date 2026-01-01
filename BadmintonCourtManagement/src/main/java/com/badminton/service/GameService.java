package com.badminton.service;

import com.badminton.requestmodel.GameDTO;
import com.badminton.response.result.GameResult;
import com.badminton.response.result.Result;

public interface GameService {
    GameResult getGameResult(int courtId);

    Result<Boolean> handleFinishGame(GameDTO gameRequest);

    Result<Boolean> terminateGame(GameDTO gameRequest);
}

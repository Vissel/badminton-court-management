package com.badminton.service;

import com.badminton.entity.Game;
import com.badminton.requestmodel.GameDTO;
import com.badminton.requestmodel.rent.EndRentGameRequest;
import com.badminton.response.result.GameResult;
import com.badminton.response.result.Result;

import java.util.List;

public interface GameService {
    GameResult getGameResult(int courtId);

    Result<Boolean> handleChangeGameState(GameDTO gameRequest);

    Result<Boolean> handleFinishGame(GameDTO gameRequest);

    Result<Boolean> terminateGame(GameDTO gameRequest);

    List<Game> findAllInprogress();

    Boolean saveAll(List<Game> gameList);

    Boolean checkGameExistById(int id);

    /**
     * States of the game rows that are still open on the court (endedDate is null). A rent is
     * refused unless one of them is "Not start", so this reports why the court is not rentable.
     */
    List<String> findOpenGameStates(int courtId);

    /**
     * Move the active RENT game of the court to FINISH or CANCEL.
     */
    Result<Boolean> endRentGame(EndRentGameRequest request);


}

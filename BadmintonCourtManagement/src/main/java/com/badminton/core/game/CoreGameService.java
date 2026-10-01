package com.badminton.core.game;

import com.badminton.model.game.GameModel;
import org.springframework.stereotype.Service;

@Service
public class CoreGameService {

    public Boolean finishGame(GameModel gameModel) {
        return Boolean.TRUE;
    }

    public Boolean cancelGame(GameModel gameModel) {
        return Boolean.TRUE;
    }
}

package com.badminton.model.game;

import com.badminton.constant.GameState;
import com.badminton.constant.GameType;
import lombok.Data;

import java.util.Map;

@Data
public class GameModel {
    private CourtModel courtModel;
    private GameType gameType;
    private GameState gameState;
    private Map<ShuttleBallModel, Integer> shuttleBallModelMap;
}

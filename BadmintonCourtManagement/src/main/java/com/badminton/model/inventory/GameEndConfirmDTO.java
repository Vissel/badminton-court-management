package com.badminton.model.inventory;

import com.badminton.model.game.ShuttleBallModel;
import lombok.Data;

import java.util.Map;

@Data
public class GameEndConfirmDTO {
    private int gameId;
    private Map<ShuttleBallModel, Integer> shuttleBallModelMap;
    private boolean isFinish = true;
}

package com.badminton.model.inventory;

import com.badminton.model.game.ShuttleBallModel;
import lombok.Data;

import java.util.List;

@Data
public class ConsumedBallsInGame {
    private List<ShuttleBallModel> shuttleBallModelList;
    private int inGameId;
}

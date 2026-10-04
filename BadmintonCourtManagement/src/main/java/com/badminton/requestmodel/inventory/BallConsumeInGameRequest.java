package com.badminton.requestmodel.inventory;

import com.badminton.model.dto.ShuttleBallDTO;
import lombok.Data;

import java.util.List;

@Data
public class BallConsumeInGameRequest {
    private List<ShuttleBallDTO> shuttleBallDTOList;
    private int inGame;
}

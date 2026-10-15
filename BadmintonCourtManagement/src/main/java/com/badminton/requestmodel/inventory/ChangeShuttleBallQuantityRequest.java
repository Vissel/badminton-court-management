package com.badminton.requestmodel.inventory;

import com.badminton.model.dto.ShuttleBallDTO;
import lombok.Data;

@Data
public class ChangeShuttleBallQuantityRequest {
    private int gameId;
    private int oldQuantity;
    private int newQuantity;
    private ShuttleBallDTO shuttleBallDTO;
}

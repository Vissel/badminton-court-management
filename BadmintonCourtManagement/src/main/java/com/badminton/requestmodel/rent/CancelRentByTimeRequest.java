package com.badminton.requestmodel.rent;

import com.badminton.model.dto.ShuttleBallDTO;
import lombok.Data;

import java.util.List;

@Data
public class CancelRentByTimeRequest extends RentRequest {
    private int rentId;
    private String endedTime;

    @Override
    public List<ShuttleBallDTO> getShuttleBalls() {
        return List.of();
    }
}

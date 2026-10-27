package com.badminton.requestmodel.rent;

import com.badminton.model.dto.ShuttleBallDTO;
import lombok.Data;

import java.util.List;

@Data
public class EndRentByTimeRequest extends RentRequest {
    private int rentId;
    private List<ShuttleBallDTO> shuttleBalls;
    private String endedTime;
    /**
     * rental duration in hours; <= 0 means "keep the stored value"
     */
    private float numTime;
    /**
     * fee actually charged; <= 0 means "charge the system fee"
     */
    private float actualFee;
}

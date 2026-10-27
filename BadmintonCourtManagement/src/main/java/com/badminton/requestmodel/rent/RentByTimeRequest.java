package com.badminton.requestmodel.rent;

import com.badminton.model.dto.ShuttleBallDTO;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class RentByTimeRequest extends RentRequest {

    private String courtArea;
    private float numTime;
    private List<ShuttleBallDTO> shuttleBalls;
    /**
     * ISO-8601 string (optional, used for updates only)
     */
    private String startTime;
    /**
     * ISO-8601 string (optional, used for updates only)
     */
    private String endTime;
    private float costPerHour;

    
}

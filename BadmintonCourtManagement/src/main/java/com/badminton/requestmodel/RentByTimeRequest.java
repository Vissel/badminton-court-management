package com.badminton.requestmodel;

import com.badminton.model.dto.ShuttleBallDTO;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class RentByTimeRequest {
    private String courtId;
    private String playerName;
    private float numTime;
    private List<ShuttleBallDTO> shuttleBalls;
    /** ISO-8601 string (optional, used for updates only) */
    private String startTime;
    /** ISO-8601 string (optional, used for updates only) */
    private String endTime;
}

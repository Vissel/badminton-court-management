package com.badminton.requestmodel;

import com.badminton.model.dto.ShuttleBallDTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class RentByTimeRequest {
    @NotNull
    @NotBlank
    private String courtId;
    @NotNull
    @NotBlank
    private String playerName;
    private String courtArea;
    private float numTime;
    private List<ShuttleBallDTO> shuttleBalls;
    /** ISO-8601 string (optional, used for updates only) */
    private String startTime;
    /** ISO-8601 string (optional, used for updates only) */
    private String endTime;
    private float costPerHour;
    
}

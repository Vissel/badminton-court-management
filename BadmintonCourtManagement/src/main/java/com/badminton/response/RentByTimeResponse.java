package com.badminton.response;

import com.badminton.model.dto.RentShuttleDTO;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
public class RentByTimeResponse {
    private int id;
    private int courtId;
    private String courtName;
    private String playerName;
    private Instant startTime;
    private Instant endTime;
    private float numTime;
    private float fee;
    private List<RentShuttleDTO> shuttleBalls;
    private String state;
    private long remainingMinutes;
}

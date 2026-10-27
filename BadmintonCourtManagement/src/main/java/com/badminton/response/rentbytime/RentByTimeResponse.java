package com.badminton.response.rentbytime;

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
    /** null when the rental was billed at its system fee */
    private Float actualFee;
    /** set only when actualFee differs from the system fee */
    private String note;
    private List<RentShuttleDTO> shuttleBalls;
    private String state;
    private int remainingMinutes;
    private long remainingSeconds;
}

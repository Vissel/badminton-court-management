package com.badminton.response;

import lombok.Data;

@Data
public class CourtRentResponse {
    private int courtId;
    private String courtName;
    private String playerName;
    private String rentState;
}

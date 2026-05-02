package com.badminton.response.result;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CourtResult {
    private int courtId;

    private String courtName;
}

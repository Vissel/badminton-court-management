package com.badminton.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RentShuttleDTO {
    private int shuttleId;
    private String shuttleName;
    private float cost;
    private int number;
}

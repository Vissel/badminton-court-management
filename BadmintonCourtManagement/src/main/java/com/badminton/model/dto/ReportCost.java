package com.badminton.model.dto;

import lombok.Data;

@Data
public class ReportCost {
    private TeamDTO team1;
    private TeamDTO team2;
    private String start;
    private String end;
    private String courtName;
}

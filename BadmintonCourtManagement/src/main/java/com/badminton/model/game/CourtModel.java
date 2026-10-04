package com.badminton.model.game;


import lombok.Data;

@Data
public class CourtModel {
    private String courtId;
    private String name;
    private CourtAreaModel areaA;
    private CourtAreaModel areaB;
    private CourtAreaModel areaC;
    private CourtAreaModel areaD;
}

package com.badminton.model.game;

import lombok.Data;

@Data
public class CourtAreaModel {
    private AreaEnum area;
    private String availablePlayer;
    private GameResultEnum gameResult;
    private float expense;
}


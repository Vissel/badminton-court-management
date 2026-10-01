package com.badminton.model.game;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ShuttleBallModel {
    private String name;
    private BigDecimal cost;
    private int quantity;
}

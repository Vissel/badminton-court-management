package com.badminton.model;

import lombok.Data;

import java.time.Instant;

@Data
public class AvailablePlayerModel {
    private PlayerModel player;

    private SessionModel session;

    private Instant leaveTime;

    private String services;

    private Float payAmount;

    private String payType;

    private Float advancePayment;
}

package com.badminton.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PayResponse {
    private String playerName;
    private String services;
    private String payType;
    private Float payAmount;
    private String payTime;
}

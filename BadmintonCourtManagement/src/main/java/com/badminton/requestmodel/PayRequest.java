package com.badminton.requestmodel;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class PayRequest {
    private String playerName;
    private List<ServiceRequest> serviceRequests;
    private String totalExpense;
    @NotBlank
    private String payType;
}

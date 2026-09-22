package com.badminton.requestmodel;

import com.badminton.requestmodel.debit.DebitRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class PayRequest {
    private String playerName;
    private List<ServiceRequest> serviceRequests;
    private String totalExpense;
    @NotBlank
    private String payType;
    @NotNull
    private DebitRequest debitRequest;
}

package com.badminton.requestmodel;

import com.badminton.constant.PayType;
import com.badminton.requestmodel.debit.DebitRequest;
import com.badminton.requestmodel.debit.PayDebitRequest;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class PayRequest {
    @NotBlank
    private String playerName;
    private List<ServiceRequest> serviceRequests;
    private String totalExpense;
    private PayType payType;
    private DebitRequest debitRequest;
    private PayDebitRequest payDebits;
}

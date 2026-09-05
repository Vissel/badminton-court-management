package com.badminton.service;

import com.badminton.entity.DebitSummary;
import com.badminton.requestmodel.debit.DebitRequest;
import com.badminton.requestmodel.debit.GetRemainingDebtRequest;
import com.badminton.requestmodel.debit.PayDebitRequest;
import com.badminton.response.debit.DebitResponse;
import com.badminton.response.debit.DebitSummaryResponse;
import com.badminton.response.debit.GetRemainingDebtResponse;
import com.badminton.response.debit.PayDebitResponse;
import com.badminton.response.result.Result;

import java.util.List;

public interface DebitService {

    Result<Boolean> createDebit(DebitRequest debitRequest);

    Result<DebitResponse> getDebitById(Integer debitId);

    Result<GetRemainingDebtResponse> getRemainingDebts(GetRemainingDebtRequest getRemainingDebtRequest);

    Result<List<DebitResponse>> getAllDebits();

    Result<DebitSummary> payForDebit(Integer debitId, java.math.BigDecimal paymentAmount);

    Result<PayDebitResponse> payForPlayerDebits(PayDebitRequest payDebitRequest);

    Result<DebitSummaryResponse> getDebitSummary(String playerName);
}

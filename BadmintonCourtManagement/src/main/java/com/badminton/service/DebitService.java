package com.badminton.service;

import com.badminton.requestmodel.debit.DebitHistoryRequest;
import com.badminton.requestmodel.debit.DebitRequest;
import com.badminton.requestmodel.debit.GetRemainingDebtRequest;
import com.badminton.requestmodel.debit.PayDebitRequest;
import com.badminton.response.PageResponse;
import com.badminton.response.debit.*;
import com.badminton.response.result.Result;

import java.util.List;

public interface DebitService {

    Result<Boolean> createDebit(DebitRequest debitRequest);

    Result<DebitResponse> getDebitById(Integer debitId);

    Result<GetRemainingDebtResponse> getRemainingDebts(GetRemainingDebtRequest getRemainingDebtRequest);

    Result<List<DebitResponse>> getAllDebits();

    Result<PrepayDebitResponse> prepayDebitsForPlayer(PayDebitRequest payDebitRequest);

    Result<PayDebitResponse> payDebitsForPlayer(PayDebitRequest payDebitRequest);

    Result<DebitSummaryResponse> getDebitSummary(String playerName);

    Result<PageResponse<DebitHistoryItemResponse>> getDebitHistory(DebitHistoryRequest request);

    Result<DebitHistorySummaryResponse> getDebitHistorySummary(String playerName);
}

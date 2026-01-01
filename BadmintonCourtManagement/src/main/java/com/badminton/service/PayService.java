package com.badminton.service;

import com.badminton.requestmodel.PayRequest;
import com.badminton.response.PayResponse;
import com.badminton.response.result.Result;

public interface PayService {

    Result<PayResponse> payToPlayer(PayRequest payRequest);

}

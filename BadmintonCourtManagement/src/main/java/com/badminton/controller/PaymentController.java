package com.badminton.controller;

import com.badminton.requestmodel.PayRequest;
import com.badminton.response.PayResponse;
import com.badminton.response.result.Result;
import com.badminton.service.PayService;
import com.badminton.util.ResponseConvertor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/pay")
public class PaymentController {

    @Autowired
    PayService payService;

    @PostMapping("/payToPlayer")
    public ResponseEntity<Result<PayResponse>> payToPlayer(@RequestBody PayRequest payRequest) {
        return ResponseConvertor.convert(payService.payToPlayer(payRequest));
    }
}

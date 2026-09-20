package com.badminton.controller;

import com.badminton.requestmodel.debit.DebitRequest;
import com.badminton.requestmodel.debit.GetRemainingDebtRequest;
import com.badminton.requestmodel.debit.PayDebitRequest;
import com.badminton.response.debit.*;
import com.badminton.response.result.Result;
import com.badminton.service.DebitService;
import com.badminton.util.ResponseConvertor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/debit")
public class DebitController {

    @Autowired
    private DebitService debitService;

    /**
     * Create a new debit.
     *
     * @param debitRequest
     * @return
     */
    @PostMapping("/create")
    public ResponseEntity<Boolean> createDebit(@RequestBody DebitRequest debitRequest) {
        return ResponseConvertor.convertToResponseEntity(debitService.createDebit(debitRequest));
    }

    @GetMapping("/{debitId}")
    public ResponseEntity<Result<DebitResponse>> getDebitById(@PathVariable Integer debitId) {
        return ResponseConvertor.convert(debitService.getDebitById(debitId));
    }

    /**
     * List remaining debts.
     *
     * @param getRemainingDebtRequest
     * @return
     */
    @PostMapping("/listRemainingDebts")
    public ResponseEntity<GetRemainingDebtResponse> listRemainingDebts(@RequestBody GetRemainingDebtRequest getRemainingDebtRequest) {
        return ResponseConvertor.convertToResponseEntity(debitService.getRemainingDebts(getRemainingDebtRequest));
    }

    @GetMapping("/all")
    public ResponseEntity<Result<List<DebitResponse>>> getAllDebits() {
        return ResponseConvertor.convert(debitService.getAllDebits());
    }

    /**
     * pre pay
     *
     * @param payDebitRequest
     * @return
     */
    @PostMapping("/prePay")
    public ResponseEntity<PrepayDebitResponse> prePayForPlayerDebits(
            @RequestBody PayDebitRequest payDebitRequest) {
        return ResponseConvertor.convertToResponseEntity(debitService.prepayDebitsForPlayer(payDebitRequest));
    }

    /**
     * Pay for player debits.
     *
     * @param payDebitRequest
     * @return
     */
    @PostMapping("/pay")
    public ResponseEntity<PayDebitResponse> payForPlayerDebits(
            @RequestBody PayDebitRequest payDebitRequest) {
        return ResponseConvertor.convertToResponseEntity(debitService.payDebitsForPlayer(payDebitRequest));
    }

    /**
     * Get the debit summary for a specific player.
     *
     * @param playerName
     * @return
     */
    @GetMapping("/summary")
    public ResponseEntity<DebitSummaryResponse> getDebitSummary(@RequestParam String playerName) {
        return ResponseConvertor.convertToResponseEntity(debitService.getDebitSummary(playerName));
    }
}

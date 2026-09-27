package com.badminton.controller;

import com.badminton.requestmodel.debit.DebitHistoryRequest;
import com.badminton.requestmodel.debit.DebitReportExportRequest;
import com.badminton.requestmodel.debit.DebitRequest;
import com.badminton.requestmodel.debit.GetRemainingDebtRequest;
import com.badminton.requestmodel.debit.PayDebitRequest;
import com.badminton.response.PageResponse;
import com.badminton.response.debit.*;
import com.badminton.response.result.Result;
import com.badminton.service.DebitReportService;
import com.badminton.service.DebitService;
import com.badminton.service.report.DebitExcelReportWriter;
import com.badminton.util.ResponseConvertor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/debit")
public class DebitController {

    @Autowired
    private DebitService debitService;

    @Autowired
    private DebitReportService debitReportService;

    private static final String XLSX_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    /**
     * Create a new debit.
     *
     * @param debitRequest
     * @return
     */
    @PostMapping("/create")
    public ResponseEntity<Result<Boolean>> createDebit(@RequestBody DebitRequest debitRequest) {
        return ResponseConvertor.convert(debitService.createDebit(debitRequest));
    }

    /**
     * List remaining debts.
     *
     * @param getRemainingDebtRequest
     * @return
     */
    @PostMapping("/listRemainingDebts")
    public ResponseEntity<Result<GetRemainingDebtResponse>> listRemainingDebts(@RequestBody GetRemainingDebtRequest getRemainingDebtRequest) {
        return ResponseConvertor.convert(debitService.getRemainingDebts(getRemainingDebtRequest));
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
    public ResponseEntity<Result<PrepayDebitResponse>> prePayForPlayerDebits(
            @RequestBody PayDebitRequest payDebitRequest) {
        return ResponseConvertor.convert(debitService.prepayDebitsForPlayer(payDebitRequest));
    }

    /**
     * Pay for player debits.
     *
     * @param payDebitRequest
     * @return
     */
    @PostMapping("/pay")
    public ResponseEntity<Result<PayDebitResponse>> payForPlayerDebits(
            @RequestBody PayDebitRequest payDebitRequest) {
        return ResponseConvertor.convert(debitService.payDebitsForPlayer(payDebitRequest));
    }

    /**
     * Get the debit summary for a specific player.
     *
     * @param playerName
     * @return
     */
    @GetMapping("/summary")
    public ResponseEntity<Result<DebitSummaryResponse>> getDebitSummary(@RequestParam String playerName) {
        return ResponseConvertor.convert(debitService.getDebitSummary(playerName));
    }

    /**
     * Full debit history of a player (paid + remaining), newest first.
     * Pagination defaults: page 1, size 10.
     *
     * @param debitHistoryRequest
     * @return
     */
    @PostMapping("/history")
    public ResponseEntity<Result<PageResponse<DebitHistoryItemResponse>>> getDebitHistory(
            @RequestBody DebitHistoryRequest debitHistoryRequest) {
        return ResponseConvertor.convert(debitService.getDebitHistory(debitHistoryRequest));
    }

    /**
     * Aggregate summary over the player's full debit history (paid + remaining).
     *
     * @param playerName
     * @return
     */
    @GetMapping("/summaryHistory")
    public ResponseEntity<Result<DebitHistorySummaryResponse>> getDebitHistorySummary(@RequestParam String playerName) {
        return ResponseConvertor.convert(debitService.getDebitHistorySummary(playerName));
    }

    /**
     * Export debit report as XLSX.
     */
    @PostMapping(
            value = "/report/export",
            produces = XLSX_CONTENT_TYPE
    )
    public ResponseEntity<StreamingResponseBody> exportDebitReport(
            @RequestBody DebitReportExportRequest request) {
        String contentDisposition = debitReportService.buildContentDisposition(request);
        StreamingResponseBody stream = outputStream -> debitReportService.exportToStream(request, outputStream);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                .header(HttpHeaders.CACHE_CONTROL, CacheControl.noStore().getHeaderValue())
                .contentType(MediaType.parseMediaType(XLSX_CONTENT_TYPE))
                .body(stream);
    }

}

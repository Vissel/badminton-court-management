package com.badminton.service.impl;

import com.badminton.core.debit.CoreDebitService;
import com.badminton.enums.PaymentStatus;
import com.badminton.exception.BusinessException;
import com.badminton.exception.enums.ErrorCodeEnum;
import com.badminton.model.debit.DebitHistoryItemModel;
import com.badminton.model.debit.DebitHistoryModel;
import com.badminton.model.debit.DebitHistorySummaryModel;
import com.badminton.model.debit.RemainingDebitModel;
import com.badminton.model.dto.AllocateDebitPaymentRequest;
import com.badminton.model.dto.AllocateDebitPaymentResponse;
import com.badminton.model.dto.CreateDebitDTO;
import com.badminton.model.dto.DebitHistoryDTO;
import com.badminton.model.dto.DebitPayDTO;
import com.badminton.model.dto.RemainingDebitDTO;
import com.badminton.requestmodel.Pagination;
import com.badminton.requestmodel.debit.DebitHistoryRequest;
import com.badminton.requestmodel.debit.DebitRequest;
import com.badminton.requestmodel.debit.GetRemainingDebtRequest;
import com.badminton.requestmodel.debit.PayDebitRequest;
import com.badminton.response.PageResponse;
import com.badminton.response.debit.*;
import com.badminton.response.result.Result;
import com.badminton.service.DebitService;
import com.badminton.service.ProcessCallback;
import com.badminton.service.ServiceTemplate;
import com.badminton.util.MoneyUtils;
import com.badminton.util.TimeUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DebitServiceImpl implements DebitService {

    @Autowired
    private ServiceTemplate serviceTemplate;

    @Autowired
    private CoreDebitService coreDebitService;

    @Transactional
    @Override
    public Result<Boolean> createDebit(DebitRequest debitRequest) {
        return serviceTemplate.execute(new ProcessCallback<DebitRequest, Boolean>() {
            @Override
            public DebitRequest getRequest() {
                return debitRequest;
            }

            @Override
            public void preProcess(DebitRequest request) {
                Assert.notNull(request, "Request must not be null");
                Assert.isTrue(request.getDebitAmount() > 0, "Debt amount must be positive");
            }

            @Override
            public Boolean process() throws BusinessException {
                log.info("Creating new debit.");
                return coreDebitService.createDebit(convertToCreateDebitDTO(getRequest()));
            }
        });
    }

    @Override
    public Result<DebitResponse> getDebitById(Integer debitId) {
        return serviceTemplate.execute(new ProcessCallback<Void, DebitResponse>() {
            @Override
            public Void getRequest() {
                return null;
            }

            @Override
            public void preProcess(Void request) {
                Assert.notNull(debitId, "Debit ID must not be null");
            }

            @Override
            public DebitResponse process() throws BusinessException {
                DebitResponse response = coreDebitService.getDebitById(debitId);
                if (response == null) {
                    throw new BusinessException(ErrorCodeEnum.DEBIT_NOT_FOUND, "Debit not found with ID: " + debitId);
                }
                return response;
            }
        });
    }

    @Override
    public Result<GetRemainingDebtResponse> getRemainingDebts(GetRemainingDebtRequest getRemainingDebtRequest) {
        return serviceTemplate.execute(new ProcessCallback<GetRemainingDebtRequest, GetRemainingDebtResponse>() {
            @Override
            public GetRemainingDebtRequest getRequest() {
                return getRemainingDebtRequest;
            }

            @Override
            public void preProcess(GetRemainingDebtRequest request) {
                Assert.notEmpty(request.getPlayerNames(), "Player names must not be null or empty");
                Assert.isTrue(StringUtils.isNotBlank(request.getPlayerNames().get(0)), "Player name must not be blank");
            }

            @Override
            public GetRemainingDebtResponse process() throws BusinessException {
                log.info("Getting remaining debts for player: {}", getRequest().getPlayerNames().get(0));
                RemainingDebitModel remainingDebit = coreDebitService
                        .getRemainingDebtsBySinglePlayer(convertToDebitDTO(getRequest()));
                return convertToGetRemainingDebtResponse(remainingDebit);
            }
        });
    }

    @Override
    public Result<List<DebitResponse>> getAllDebits() {
        return serviceTemplate.execute(new ProcessCallback<Void, List<DebitResponse>>() {
            @Override
            public Void getRequest() {
                return null;
            }

            @Override
            public void preProcess(Void request) {
            }

            @Override
            public List<DebitResponse> process() throws BusinessException {
                return coreDebitService.getAllDebits();
            }
        });
    }

    @Override
    public Result<PrepayDebitResponse> prepayDebitsForPlayer(PayDebitRequest payDebitRequest) {
        return serviceTemplate.execute(new ProcessCallback<PayDebitRequest, PrepayDebitResponse>() {
            @Override
            public PayDebitRequest getRequest() {
                return payDebitRequest;
            }

            @Override
            public void preProcess(PayDebitRequest request) {
                Assert.notNull(request.getPlayerName(), "Player name must not be null");
                Assert.notNull(request.getTotalPayAmount(), "Payment amount must not be null");
                Assert.isTrue(request.getTotalPayAmount() > 0, "Payment amount must be positive");
            }

            @Override
            public PrepayDebitResponse process() throws BusinessException {
                AllocateDebitPaymentRequest request = convertToAllocateDebitPaymentRequest(getRequest());
                return coreDebitService.prepayDebitsForPlayer(request);
            }
        });
    }

    @Override
    public Result<PayDebitResponse> payDebitsForPlayer(PayDebitRequest payDebitRequest) {
        return serviceTemplate.execute(new ProcessCallback<PayDebitRequest, PayDebitResponse>() {
            @Override
            public PayDebitRequest getRequest() {
                return payDebitRequest;
            }

            @Override
            public void preProcess(PayDebitRequest request) {
                Assert.notNull(request.getPlayerName(), "Player name must not be null");
                Assert.notNull(request.getTotalPayAmount(), "Payment amount must not be null");
                Assert.isTrue(request.getTotalPayAmount() > 0, "Payment amount must be positive");
                Assert.notEmpty(request.getListDebitPay(), "Debit pay list must not be null or empty");
                request.getListDebitPay().forEach(debitPay -> {
                    Assert.notNull(debitPay, "Debit pay element must not be null");
                    Assert.isTrue(StringUtils.isNotBlank(debitPay.getDateTime()),
                            "Debit pay date time must not be blank");
                    Assert.isTrue(debitPay.getPayAmount() > 0, "Debit pay amount must be positive");
                });
                BigDecimal listTotalAmount = request.getListDebitPay().stream()
                        .map(debitPay -> BigDecimal.valueOf(debitPay.getPayAmount()))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                Assert.isTrue(listTotalAmount.compareTo(BigDecimal.valueOf(request.getTotalPayAmount())) == 0,
                        "Total pay amount must equal the sum of pay amounts in the debit pay list");
            }

            @Override
            public PayDebitResponse process() throws BusinessException {

                AllocateDebitPaymentResponse response = coreDebitService
                        .allocateDebitPayment(convertToAllocateDebitPaymentRequest(getRequest()));
                if (response == null) {
                    throw new BusinessException(ErrorCodeEnum.INTERNAL_SERVER_ERROR,
                            "Debit allocation returned empty response");
                }

                PayDebitResponse payDebitResponse = convertToPayDebitResponse(response);

                if (PaymentStatus.FAIL.equals(response.getStatus())) {
                    log.error("Pay debits failed for player [{}]: {}", getRequest().getPlayerName(),
                            response.getMessage());
                    throw new BusinessException(
                            resolveErrorCode(response.getErrorCode()),
                            response.getMessage(),
                            payDebitResponse);
                }

                log.info("Pay debits result for player [{}]: {}", getRequest().getPlayerName(), response.getMessage());
                return payDebitResponse;
            }
        });
    }

    @Override
    public Result<DebitSummaryResponse> getDebitSummary(String playerName) {
        return serviceTemplate.execute(new ProcessCallback<String, DebitSummaryResponse>() {
            @Override
            public String getRequest() {
                return playerName;
            }

            @Override
            public void preProcess(String request) {
                Assert.notNull(request, "Player name must not be null");
            }

            @Override
            public DebitSummaryResponse process() throws BusinessException {
                return coreDebitService.getDebitSummary(playerName);
            }
        });
    }

    @Override
    public Result<DebitHistorySummaryResponse> getDebitHistorySummary(String playerName) {
        return serviceTemplate.execute(new ProcessCallback<String, DebitHistorySummaryResponse>() {
            @Override
            public String getRequest() {
                return playerName;
            }

            @Override
            public void preProcess(String request) {
                Assert.isTrue(StringUtils.isNotBlank(request), "Player name must not be blank");
            }

            @Override
            public DebitHistorySummaryResponse process() throws BusinessException {
                return convertToDebitHistorySummaryResponse(coreDebitService.getDebitHistorySummary(playerName));
            }
        });
    }

    private DebitHistorySummaryResponse convertToDebitHistorySummaryResponse(DebitHistorySummaryModel model) {
        return new DebitHistorySummaryResponse(
                model.getPlayerName(),
                model.getTotalDebitAmount() != null ? model.getTotalDebitAmount().floatValue() : 0f,
                model.getTotalPaidAmount() != null ? model.getTotalPaidAmount().floatValue() : 0f,
                model.getTotalRemainingAmount() != null ? model.getTotalRemainingAmount().floatValue() : 0f,
                model.getNumDebits(),
                model.getNumPaidDebits(),
                model.getNumUnpaidDebits(),
                model.getCurrency());
    }

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;

    @Override
    public Result<PageResponse<DebitHistoryItemResponse>> getDebitHistory(DebitHistoryRequest request) {
        return serviceTemplate
                .execute(new ProcessCallback<DebitHistoryRequest, PageResponse<DebitHistoryItemResponse>>() {
                    @Override
                    public DebitHistoryRequest getRequest() {
                        return request;
                    }

                    @Override
                    public void preProcess(DebitHistoryRequest request) {
                        Assert.notNull(request, "Request must not be null");
                        Assert.isTrue(StringUtils.isNotBlank(request.getPlayerName()), "Player name must not be blank");
                    }

                    @Override
                    public PageResponse<DebitHistoryItemResponse> process() throws BusinessException {
                        log.info("Getting debit history for player: {}", getRequest().getPlayerName());
                        DebitHistoryDTO dto = convertToDebitHistoryDTO(getRequest());
                        DebitHistoryModel model = coreDebitService.getDebitHistory(dto);

                        PageResponse<DebitHistoryItemResponse> pageResponse = new PageResponse<>();
                        pageResponse.setList(model.getItems() != null
                                ? model.getItems().stream()
                                        .map(item -> convertToDebitHistoryItemResponse(item, model.getPlayerName()))
                                        .collect(Collectors.toList())
                                : List.of());
                        pageResponse.setTotal(model.getTotal());
                        Pagination pagination = dto.getPagination();
                        pagination.setTotalPage(model.getTotalPage());
                        pageResponse.setPagination(pagination);
                        return pageResponse;
                    }
                });
    }

    private DebitHistoryDTO convertToDebitHistoryDTO(DebitHistoryRequest request) {
        Pagination pagination = request.getPagination();
        if (pagination == null || pagination.getCurrent() <= 0 || pagination.getPageSize() <= 0) {
            pagination = new Pagination(DEFAULT_PAGE, DEFAULT_PAGE_SIZE, 0);
        }
        Instant from = request.getFilter() != null && StringUtils.isNotBlank(request.getFilter().getFrom())
                ? TimeUtils.convertToInstant(request.getFilter().getFrom())
                : null;
        Instant to = request.getFilter() != null && StringUtils.isNotBlank(request.getFilter().getTo())
                ? TimeUtils.convertToInstant(request.getFilter().getTo())
                : null;
        Float amountFrom = request.getFilter() != null && request.getFilter().getAmountFrom() > 0
                ? request.getFilter().getAmountFrom()
                : null;
        Float amountTo = request.getFilter() != null && request.getFilter().getAmountTo() > 0
                ? request.getFilter().getAmountTo()
                : null;
        return DebitHistoryDTO.builder()
                .playerName(request.getPlayerName())
                .pagination(pagination)
                .from(from)
                .to(to)
                .amountFrom(amountFrom)
                .amountTo(amountTo)
                .build();
    }

    private DebitHistoryItemResponse convertToDebitHistoryItemResponse(DebitHistoryItemModel item, String playerName) {
        return new DebitHistoryItemResponse(
                playerName,
                item.getDebtAmount() != null ? item.getDebtAmount().floatValue() : 0f,
                item.getDebtDateTime() != null ? TimeUtils.toDateTimeDisplay(item.getDebtDateTime()) : null,
                item.getPaidAmount() != null ? item.getPaidAmount().floatValue() : 0f,
                item.getPaidDateTime() != null ? TimeUtils.toDateTimeDisplay(item.getPaidDateTime()) : null,
                item.getRemainingAmount() != null ? item.getRemainingAmount().floatValue() : 0f,
                item.getCurrency() != null ? item.getCurrency() : MoneyUtils.CURRENCY_VN,
                item.getStatus() != null ? item.getStatus().name() : null,
                item.getNote());
    }

    private CreateDebitDTO convertToCreateDebitDTO(DebitRequest request) {
        CreateDebitDTO dto = new CreateDebitDTO();
        dto.setDebitAmount(BigDecimal.valueOf(request.getDebitAmount()));
        dto.setCurrency(request.getCurrency());
        dto.setNote(request.getNote());
        dto.setPlayerName(request.getPlayerName());
        dto.setCreatedTime(TimeUtils.convertToInstant(request.getCreatedTime()));
        return dto;
    }

    private AllocateDebitPaymentRequest convertToAllocateDebitPaymentRequest(PayDebitRequest request) {
        return AllocateDebitPaymentRequest.builder()
                .playerName(request.getPlayerName())
                .payAmount(BigDecimal.valueOf(request.getTotalPayAmount()))
                .payMethod(request.getPaymentMethod())
                .note(request.getNote())
                .listDebitPay(request.getListDebitPay() != null
                        ? request.getListDebitPay().stream()
                                .map(debitPay -> DebitPayDTO.builder()
                                        .dateTime(debitPay.getDateTime())
                                        .payAmount(BigDecimal.valueOf(debitPay.getPayAmount()))
                                        .build())
                                .collect(Collectors.toList())
                        : null)
                .build();
    }

    private PayDebitResponse convertToPayDebitResponse(AllocateDebitPaymentResponse model) {
        PayDebitResponse response = new PayDebitResponse();
        response.setPlayerName(model.getPlayerName());
        response.setPaymentAmount(model.getPaymentAmount() != null ? model.getPaymentAmount().floatValue() : 0f);
        response.setPaymentMethod(model.getPaymentMethod());
        response.setPaidDebts(model.getPaidDebts() != null ? model.getPaidDebts().floatValue() : 0f);
        response.setRemainingDebts(model.getRemainingDebts() != null ? model.getRemainingDebts().floatValue() : 0f);
        response.setNumPaidDebts(model.getNumPaidDebts());
        response.setNumRemainingDebts(model.getNumRemainingDebts());
        response.setPaymentDate(TimeUtils.toDateTimeDisplay(model.getPaymentDate()));
        response.setStatus(model.getStatus() != null ? model.getStatus().name() : null);
        response.setMessage(model.getMessage());
        return response;
    }

    private ErrorCodeEnum resolveErrorCode(int errorCode) {
        return Arrays.stream(ErrorCodeEnum.values())
                .filter(code -> Integer.parseInt(code.getCode()) == errorCode)
                .findFirst()
                .orElse(ErrorCodeEnum.INTERNAL_SERVER_ERROR);
    }

    private RemainingDebitDTO convertToDebitDTO(GetRemainingDebtRequest request) {
        Pagination pagination = request.getPagination();
        if (pagination == null) {
            pagination = new Pagination(1, 10, 0);
        }

        return RemainingDebitDTO.builder()
                .pagination(pagination)
                .playerName(request.getPlayerNames().get(0))
                .from(TimeUtils.convertToInstant(request.getFilter().getFrom()))
                .to(TimeUtils.convertToInstant(request.getFilter().getTo()))
                .amountFrom(null)
                .amountTo(null)
                .build();
    }

    private GetRemainingDebtResponse convertToGetRemainingDebtResponse(RemainingDebitModel remainingDebit) {
        MoneyResponse moneyResponse = new MoneyResponse(
                remainingDebit.getTotalDebts() != null ? remainingDebit.getTotalDebts().floatValue() : 0f,
                MoneyUtils.CURRENCY_VN);

        DebitSummaryResponse debitSummary = new DebitSummaryResponse(
                remainingDebit.getPlayerName(),
                moneyResponse,
                remainingDebit.getNumberDebit());

        List<RemainingDebitsResponse> remainingDebits = remainingDebit.getDebitModels() != null
                ? remainingDebit.getDebitModels().stream()
                        .map(debitModel -> new RemainingDebitsResponse(
                                debitModel.getDateTime() != null ? debitModel.getDateTime().toString() : null,
                                new MoneyResponse(
                                        debitModel.getMoney() != null ? debitModel.getMoney().floatValue() : 0f,
                                        MoneyUtils.CURRENCY_VN),
                                debitModel.getNote()))
                        .collect(Collectors.toList())
                : null;

        return GetRemainingDebtResponse.builder()
                .playerName(remainingDebit.getPlayerName())
                .debitSummary(debitSummary)
                .remainingDebits(remainingDebits)
                .build();
    }

}

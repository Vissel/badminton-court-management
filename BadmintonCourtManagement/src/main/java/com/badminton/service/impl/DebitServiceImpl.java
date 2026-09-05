package com.badminton.service.impl;

import com.badminton.core.debit.CoreDebitService;
import com.badminton.entity.DebitSummary;
import com.badminton.exception.BusinessException;
import com.badminton.exception.enums.ErrorCodeEnum;
import com.badminton.model.debit.RemainingDebitModel;
import com.badminton.model.dto.DebitDTO;
import com.badminton.requestmodel.Pagination;
import com.badminton.requestmodel.debit.DebitRequest;
import com.badminton.requestmodel.debit.GetRemainingDebtRequest;
import com.badminton.requestmodel.debit.PayDebitRequest;
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
                return coreDebitService.createDebit(getRequest());
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
                RemainingDebitModel remainingDebit = coreDebitService.getRemainingDebtsBySinglePlayer(convertToDebitDTO(getRequest()));
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

    @Transactional
    @Override
    public Result<DebitSummary> payForDebit(Integer debitId, BigDecimal paymentAmount) {
        return serviceTemplate.execute(new ProcessCallback<Void, DebitSummary>() {
            @Override
            public Void getRequest() {
                return null;
            }

            @Override
            public void preProcess(Void request) {
                Assert.notNull(debitId, "Debit ID must not be null");
                Assert.notNull(paymentAmount, "Payment amount must not be null");
                Assert.isTrue(paymentAmount.compareTo(BigDecimal.ZERO) > 0, "Payment amount must be positive");
            }

            @Override
            public DebitSummary process() throws BusinessException {
                DebitSummary response = coreDebitService.payForDebit(debitId, paymentAmount);
                if (response == null) {
                    throw new BusinessException(ErrorCodeEnum.DEBIT_NOT_FOUND, "Debit not found or invalid payment amount");
                }
                return response;
            }
        });
    }

    @Transactional
    @Override
    public Result<PayDebitResponse> payForPlayerDebits(PayDebitRequest payDebitRequest) {
        return serviceTemplate.execute(new ProcessCallback<PayDebitRequest, PayDebitResponse>() {
            @Override
            public PayDebitRequest getRequest() {
                return payDebitRequest;
            }

            @Override
            public void preProcess(PayDebitRequest request) {
                Assert.notNull(request.getPlayerName(), "Player name must not be null");
                Assert.notNull(request.getPaymentAmount(), "Payment amount must not be null");
            }

            @Override
            public PayDebitResponse process() throws BusinessException {
                DebitSummary response = coreDebitService.payForPlayerDebits(playerId, paymentAmount);
                if (response == null) {
                    throw new BusinessException(ErrorCodeEnum.PLAYER_NOT_FOUND, "Player not found or no debits found or invalid payment amount");
                }
                return response;
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

    private DebitDTO convertToDebitDTO(GetRemainingDebtRequest request) {
        Pagination pagination = request.getPagination();
        if (pagination == null) {
            pagination = new Pagination(1, 10, 0);
        }

        return DebitDTO.builder()
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
                MoneyUtils.CURRENCY_VN
        );

        DebitSummaryResponse debitSummary = new DebitSummaryResponse(
                remainingDebit.getPlayerName(),
                moneyResponse,
                remainingDebit.getNumberDebit()
        );

        List<RemainingDebitsResponse> remainingDebits = remainingDebit.getDebitModels() != null
                ? remainingDebit.getDebitModels().stream()
                .map(debitModel -> new RemainingDebitsResponse(
                        debitModel.getDateTime() != null ? debitModel.getDateTime().toString() : null,
                        new MoneyResponse(
                                debitModel.getMoney() != null ? debitModel.getMoney().floatValue() : 0f,
                                MoneyUtils.CURRENCY_VN
                        ),
                        debitModel.getNote()
                ))
                .collect(Collectors.toList())
                : null;

        return GetRemainingDebtResponse.builder()
                .playerName(remainingDebit.getPlayerName())
                .debitSummary(debitSummary)
                .remainingDebits(remainingDebits)
                .build();
    }

}

package com.badminton.service.impl;

import com.badminton.entity.AvailablePlayer;
import com.badminton.exception.BusinessException;
import com.badminton.exception.enums.ErrorCodeEnum;
import com.badminton.model.dto.ServiceDTO;
import com.badminton.repository.AvailablePlayerRepository;
import com.badminton.requestmodel.PayRequest;
import com.badminton.requestmodel.debit.DebitRequest;
import com.badminton.response.PayResponse;
import com.badminton.response.result.Result;
import com.badminton.service.*;
import com.badminton.util.ServiceConverter;
import com.badminton.util.ServiceUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.util.Assert;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PayServiceImpl implements PayService {
    @Autowired
    ServiceTemplate serviceTemple;
    @Autowired
    AvailablePlayerRepository availablePlayerRepository;
    @Autowired
    SessionServiceImpl sessionService;
    @Autowired
    DebitService debitService;

    @Transactional
    @Override
    public Result<PayResponse> payToPlayer(PayRequest payRequest) {
        return serviceTemple.execute(new ProcessCallback<PayRequest, PayResponse>() {
            @Override
            public PayRequest getRequest() {
                return payRequest;
            }

            @Override
            public void preProcess(PayRequest request) {
                Assert.notNull(request, "Request must not be null");
                Assert.isTrue(StringUtils.isNotBlank(request.getPlayerName()), "playerName must not be blank");
                Assert.notNull(request.getServiceRequests(), "Service must not be null");
                Assert.isTrue(StringUtils.isNotBlank(request.getPayType()), "Pay type  must not be blank");
                Assert.notNull(request.getDebitRequest(), "Debit request must not be null");
                Assert.isTrue(StringUtils.isNotBlank(request.getDebitRequest().getCreatedTime()), "Debit created time must not be blank");
            }

            @Override
            public PayResponse process() throws BusinessException {
                // create debit for debts contribution
                DebitRequest debitRequest = payRequest.getDebitRequest();
                debitRequest.setPlayerName(payRequest.getPlayerName());
                Result<Boolean> debitResult = debitService.createDebit(debitRequest);
                if (debitResult == null || !debitResult.isSuccess()) {
                    TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                    throw new BusinessException(
                            resolveErrorCode(debitResult != null ? debitResult.getErrorCode() : 0),
                            debitResult != null ? debitResult.getErrorMessage() : "Debit creation failed");
                }

                Optional<AvailablePlayer> optPlayer = availablePlayerRepository.findAvailablePlayerInSessionByNameAndLeaveTimeNull(
                        sessionService.findListCurrentSession().getFirst(), payRequest.getPlayerName());
                if (!optPlayer.isPresent()) {
                    throw new BusinessException(ErrorCodeEnum.PLAYER_NOT_FOUND, "Available player is not found.");
                }
                AvailablePlayer availablePlayer = optPlayer.get();
                List<ServiceDTO> dtos = payRequest.getServiceRequests().stream()
                        .map(req -> ServiceConverter.convertRequestToDTO(req))
                        .collect(Collectors.toList());
                availablePlayer.setServices(
                        ServiceUtil.buildJsonArrayStr(dtos));
                availablePlayer.setLeaveTime(sessionService.getUTCPlus7Instant());
                availablePlayer.setPayType(payRequest.getPayType());
                availablePlayer.setPayAmount(Float.valueOf(payRequest.getTotalExpense()));
                return convertToPayResult(availablePlayerRepository.save(availablePlayer), debitRequest.getDebitAmount());
            }
        });
    }


    private PayResponse convertToPayResult(AvailablePlayer availablePlayer, float debitAmount) {
        return new PayResponse(availablePlayer.getPlayer().getPlayerName(), availablePlayer.getCurrentServices(),
                availablePlayer.getPayType(), availablePlayer.getPayAmount(), availablePlayer.getLeaveTime().toString(),
                debitAmount);
    }

    private ErrorCodeEnum resolveErrorCode(int errorCode) {
        return Arrays.stream(ErrorCodeEnum.values())
                .filter(code -> Integer.parseInt(code.getCode()) == errorCode)
                .findFirst()
                .orElse(ErrorCodeEnum.INTERNAL_SERVER_ERROR);
    }
}

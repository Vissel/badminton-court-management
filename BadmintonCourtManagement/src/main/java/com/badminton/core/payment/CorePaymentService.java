package com.badminton.core.payment;

import com.badminton.constant.ServiceConstants;
import com.badminton.core.debit.CoreDebitService;
import com.badminton.entity.AvailablePlayer;
import com.badminton.enums.PaymentStatus;
import com.badminton.exception.BusinessException;
import com.badminton.exception.enums.ErrorCodeEnum;
import com.badminton.exception.validation.AvailablePlayerCheck;
import com.badminton.exception.validation.DebitCheck;
import com.badminton.model.dto.AllocateDebitPaymentResponse;
import com.badminton.model.dto.PaymentDTO;
import com.badminton.model.dto.ServiceDTO;
import com.badminton.model.payment.PaymentModel;
import com.badminton.repository.AvailablePlayerRepository;
import com.badminton.service.SessionServiceImpl;
import com.badminton.util.ServiceUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
public class CorePaymentService {
    @Autowired
    CoreDebitService coreDebitService;

    @Autowired
    AvailablePlayerRepository availablePlayerRepository;

    @Autowired
    SessionServiceImpl sessionService;

    @Transactional
    public PaymentModel payForPlayerAndCreateDebt(PaymentDTO paymentDTO) throws BusinessException {
        PaymentModel paymentModel = null;
        try {
            // create new debit
            if (paymentDTO.getDebit() != null) {
                DebitCheck.isCreated(coreDebitService.createDebit(paymentDTO.getDebit()));
            }

            // pay the current debits
            AllocateDebitPaymentResponse payDebitsResponse = null;
            if (paymentDTO.getPayDebits() != null) {
                payDebitsResponse = coreDebitService.allocateDebitPayment(paymentDTO.getPayDebits());
                if (payDebitsResponse == null || PaymentStatus.FAIL.equals(payDebitsResponse.getStatus())) {
                    throw new BusinessException(
                            payDebitsResponse != null ? resolveErrorCode(payDebitsResponse.getErrorCode()) : ErrorCodeEnum.INTERNAL_SERVER_ERROR,
                            payDebitsResponse != null ? payDebitsResponse.getMessage() : "Debit allocation returned empty response");
                }
            }

            // save the player with leaveTime, service
            // TODO
            Optional<AvailablePlayer> optPlayer = availablePlayerRepository.findAvailablePlayerInSessionByNameAndLeaveTimeNull(
                    sessionService.findListCurrentSession().getFirst(), paymentDTO.getPlayerName());
            AvailablePlayerCheck.isAvailablePlayerPresent(optPlayer);

            AvailablePlayer availablePlayer = optPlayer.get();
            List<ServiceDTO> services = paymentDTO.getServices() != null
                    ? new ArrayList<>(paymentDTO.getServices())
                    : new ArrayList<>();
            if (paymentDTO.getDebit() != null) {
                services.add(new ServiceDTO(ServiceConstants.CREATE_DEBIT_VN, paymentDTO.getDebit().getDebitAmount().floatValue()));
            }
            if (payDebitsResponse != null) {
                services.add(new ServiceDTO(ServiceConstants.PAY_DEBIT_VN, payDebitsResponse.getPaidDebts().floatValue()));
            }
            availablePlayer.setServices(ServiceUtil.buildJsonArrayStr(services));
            availablePlayer.setLeaveTime(sessionService.getUTCPlus7Instant());
            availablePlayer.setPayType(paymentDTO.getPayType());
            availablePlayer.setPayAmount(Float.valueOf(paymentDTO.getTotalPay()));

            AvailablePlayer savedPlayer = availablePlayerRepository.save(availablePlayer);

            paymentModel = new PaymentModel();
            paymentModel.setPayFor(savedPlayer.getPlayer().getPlayerName());
            paymentModel.setPayType(savedPlayer.getPayType());
            paymentModel.setPayAmount(BigDecimal.valueOf(savedPlayer.getPayAmount()));
            paymentModel.setPayTime(savedPlayer.getLeaveTime());
            paymentModel.setServices(savedPlayer.getCurrentServices());
            paymentModel.setDebitAmount(paymentDTO.getDebit() != null ? paymentDTO.getDebit().getDebitAmount() : null);
            if (payDebitsResponse != null) {
                paymentModel.setPaidDebts(payDebitsResponse.getPaidDebts());
                paymentModel.setRemainingDebts(payDebitsResponse.getRemainingDebts());
                paymentModel.setNumPaidDebts(payDebitsResponse.getNumPaidDebts());
                paymentModel.setNumRemainingDebts(payDebitsResponse.getNumRemainingDebts());
                paymentModel.setPayDebitsMessage(payDebitsResponse.getMessage());
            }
            return paymentModel;
        } catch (BusinessException e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            throw e;
        }
    }

    @Transactional
    public Boolean cancelPayment(String playerName) throws BusinessException {
        if (sessionService.findListCurrentSession().isEmpty()) {
            throw new BusinessException(ErrorCodeEnum.CURRENT_SESSION_NOT_FOUND);
        }
        Optional<AvailablePlayer> optPlayer = availablePlayerRepository.findForUpdateAvailablePlayerInSessionByName(
                sessionService.findListCurrentSession().getFirst(), playerName);
        AvailablePlayerCheck.isAvailablePlayerPresent(optPlayer);

        AvailablePlayer availablePlayer = optPlayer.get();
        availablePlayer.setLeaveTime(sessionService.getUTCPlus7Instant());
        availablePlayer.setIsCanceled(Boolean.TRUE);
        availablePlayerRepository.save(availablePlayer);
        return Boolean.TRUE;
    }

    private ErrorCodeEnum resolveErrorCode(int errorCode) {
        return Arrays.stream(ErrorCodeEnum.values())
                .filter(code -> Integer.parseInt(code.getCode()) == errorCode)
                .findFirst()
                .orElse(ErrorCodeEnum.INTERNAL_SERVER_ERROR);
    }
}

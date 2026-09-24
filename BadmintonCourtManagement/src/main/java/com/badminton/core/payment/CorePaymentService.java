package com.badminton.core.payment;

import com.badminton.core.debit.CoreDebitService;
import com.badminton.entity.AvailablePlayer;
import com.badminton.exception.BusinessException;
import com.badminton.exception.validation.AvailablePlayerCheck;
import com.badminton.exception.validation.DebitCheck;
import com.badminton.model.dto.PaymentDTO;
import com.badminton.model.payment.PaymentModel;
import com.badminton.repository.AvailablePlayerRepository;
import com.badminton.service.SessionServiceImpl;
import com.badminton.util.ServiceUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.math.BigDecimal;
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
            if (paymentDTO.getDebit() != null) {
                DebitCheck.isCreated(coreDebitService.createDebit(paymentDTO.getDebit()));
            }

            Optional<AvailablePlayer> optPlayer = availablePlayerRepository.findAvailablePlayerInSessionByNameAndLeaveTimeNull(
                    sessionService.findListCurrentSession().getFirst(), paymentDTO.getPlayerName());
            AvailablePlayerCheck.isAvailablePlayerPresent(optPlayer);

            AvailablePlayer availablePlayer = optPlayer.get();
            availablePlayer.setServices(ServiceUtil.buildJsonArrayStr(paymentDTO.getServices()));
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
            return paymentModel;
        } catch (BusinessException e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            throw e;
        }
    }
}

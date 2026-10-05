package com.badminton.core.payment;

import com.badminton.constant.ServiceConstants;
import com.badminton.core.billing.CoreBillingService;
import com.badminton.core.debit.CoreDebitService;
import com.badminton.core.player.CoreAvailablePlayerService;
import com.badminton.core.session.CoreSession;
import com.badminton.entity.Invoice;
import com.badminton.entity.AvailablePlayer;
import com.badminton.entity.Payment;
import com.badminton.enums.PaymentStatus;
import com.badminton.exception.BusinessException;
import com.badminton.exception.ErrorUtils;
import com.badminton.exception.enums.ErrorCodeEnum;
import com.badminton.exception.validation.DebitCheck;
import com.badminton.model.dto.*;
import com.badminton.model.payment.PaymentDebitModel;
import com.badminton.model.payment.PaymentModel;
import com.badminton.model.player.AvailablePlayerPayModel;
import com.badminton.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CorePaymentService {
    @Autowired
    CoreDebitService coreDebitService;
    @Autowired
    CoreSession coreSession;
    @Autowired
    CoreAvailablePlayerService coreAvailablePlayerService;

    @Autowired
    PaymentRepository paymentRepository;

    @Autowired
    CoreBillingService coreBillingService;

    @Transactional
    public PaymentDebitModel payForPlayerAndCreateDebt(PaymentDTO paymentDTO) throws BusinessException {
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
                            payDebitsResponse != null ? resolveErrorCode(payDebitsResponse.getErrorCode())
                                    : ErrorCodeEnum.INTERNAL_SERVER_ERROR,
                            payDebitsResponse != null ? payDebitsResponse.getMessage()
                                    : "Debit allocation returned empty response");
                }
            }

            // save the player with leaveTime, service
            // TODO
            Optional<AvailablePlayer> optPlayer = availablePlayerRepository
                    .findAvailablePlayerInSessionByNameAndLeaveTimeNull(
                            sessionService.findListCurrentSession().getFirst(), paymentDTO.getPlayerName());
            AvailablePlayerCheck.isAvailablePlayerPresent(optPlayer);
            AvailablePlayerPayModel savedPlayer = coreAvailablePlayerService.updatePaidAvailablePlayer(toAvaPlayerDTO(paymentDTO, payDebitsResponse));

            AvailablePlayer availablePlayer = optPlayer.get();
            List<ServiceDTO> services = paymentDTO.getServices() != null
                    ? new ArrayList<>(paymentDTO.getServices())
                    : new ArrayList<>();
            if (paymentDTO.getDebit() != null) {
                services.add(new ServiceDTO(ServiceConstants.CREATE_DEBIT_VN,
                        paymentDTO.getDebit().getDebitAmount().floatValue()));
            }
            if (payDebitsResponse != null) {
                services.add(
                        new ServiceDTO(ServiceConstants.PAY_DEBIT_VN, payDebitsResponse.getPaidDebts().floatValue()));
            }
            availablePlayer.setServices(ServiceUtil.buildJsonArrayStr(services));
            availablePlayer.setLeaveTime(sessionService.getUTCPlus7Instant());
            availablePlayer.setPayType(paymentDTO.getPayType());
            availablePlayer.setPayAmount(Float.valueOf(paymentDTO.getTotalPay()));
            return convertToModel(savedPlayer, payDebitsResponse, paymentDTO.getDebit());
        } catch (BusinessException e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            throw e;
        }
    }

            AvailablePlayer savedPlayer = availablePlayerRepository.save(availablePlayer);

            // Issue the bill inside the payment tx — a failure rolls the payment back.
            // The final services list (incl. appended Ghi nợ / Trả nợ lines) is
            // what the bill itemizes.
            paymentDTO.setServices(services);
            Invoice bill = coreBillingService.issueCheckoutBill(paymentDTO, savedPlayer);

            paymentDebitModel = new PaymentDebitModel();
            paymentDebitModel.setPayFor(savedPlayer.getPlayer().getPlayerName());
            paymentDebitModel.setPayType(savedPlayer.getPayType());
            paymentDebitModel.setPayAmount(BigDecimal.valueOf(savedPlayer.getPayAmount()));
            paymentDebitModel.setPayTime(savedPlayer.getLeaveTime());
            paymentDebitModel.setServices(savedPlayer.getCurrentServices());
            paymentDebitModel
                    .setDebitAmount(paymentDTO.getDebit() != null ? paymentDTO.getDebit().getDebitAmount() : null);
            if (payDebitsResponse != null) {
                paymentDebitModel.setPaidDebts(payDebitsResponse.getPaidDebts());
                paymentDebitModel.setRemainingDebts(payDebitsResponse.getRemainingDebts());
                paymentDebitModel.setNumPaidDebts(payDebitsResponse.getNumPaidDebts());
                paymentDebitModel.setNumRemainingDebts(payDebitsResponse.getNumRemainingDebts());
                paymentDebitModel.setPayDebitsMessage(payDebitsResponse.getMessage());
            }
            paymentDebitModel.setBillId(bill.getInvoiceId());
            paymentDebitModel.setBillNo(bill.getBillNo());
            return paymentDebitModel;
        } catch (BusinessException e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            throw e;
    private PaymentDebitModel convertToModel(AvailablePlayerPayModel savedPlayer, AllocateDebitPaymentResponse payDebitsResponse, CreateDebitDTO debit) {
        PaymentDebitModel paymentDebitModel = new PaymentDebitModel();
        paymentDebitModel.setPayFor(savedPlayer.getName());
        paymentDebitModel.setPayType(savedPlayer.getPayType());
        paymentDebitModel.setPayAmount(savedPlayer.getPayAmount());
        paymentDebitModel.setPayTime(savedPlayer.getPayTime());
        paymentDebitModel.setServices(savedPlayer.getServices());
        paymentDebitModel.setDebitAmount(debit != null ? debit.getDebitAmount() : null);
        if (payDebitsResponse != null) {
            paymentDebitModel.setPaidDebts(payDebitsResponse.getPaidDebts());
            paymentDebitModel.setRemainingDebts(payDebitsResponse.getRemainingDebts());
            paymentDebitModel.setNumPaidDebts(payDebitsResponse.getNumPaidDebts());
            paymentDebitModel.setNumRemainingDebts(payDebitsResponse.getNumRemainingDebts());
            paymentDebitModel.setPayDebitsMessage(payDebitsResponse.getMessage());
        }
        return paymentDebitModel;
    }

    private AvailablePlayerPayDTO toAvaPlayerDTO(PaymentDTO paymentDTO, AllocateDebitPaymentResponse payDebitsResponse) {
        List<ServiceDTO> services = paymentDTO.getServices() != null
                ? new ArrayList<>(paymentDTO.getServices())
                : new ArrayList<>();
        if (paymentDTO.getDebit() != null) {
            services.add(new ServiceDTO(ServiceConstants.CREATE_DEBIT_VN, paymentDTO.getDebit().getDebitAmount().floatValue()));
        }
        if (payDebitsResponse != null) {
            services.add(new ServiceDTO(ServiceConstants.PAY_DEBIT_VN, payDebitsResponse.getPaidDebts().floatValue()));
        }
        return AvailablePlayerPayDTO.builder()
                .name(paymentDTO.getPlayerName())
                .listService(services)
                .payType(paymentDTO.getPayType())
                .payAmount(Float.valueOf(paymentDTO.getTotalPay()))
                .build();
    }

    /**
     * Query payment records whose paymentDate falls within [dateStart, dayEnd]
     * and map them to lightweight {@link PaymentModel}s for reporting.
     */
    public List<PaymentModel> findPaymentsBetween(Instant dateStart, Instant dayEnd) {
        return paymentRepository.findByPaymentDateBetween(dateStart, dayEnd).stream()
                .map(this::toPaymentModel)
                .collect(Collectors.toList());
    }

    private PaymentModel toPaymentModel(Payment payment) {
        PaymentModel model = new PaymentModel();
        model.setAmount(payment.getAmount());
        model.setCurrency(payment.getCurrency());
        model.setPaymentDate(payment.getPaymentDate());
        model.setNote(payment.getNote());
        model.setPayType(payment.getPayType());
        model.setPayFor(payment.getPlayer() != null ? payment.getPlayer().getPlayerName() : null);
        return model;
    }

    @Transactional
    public Boolean cancelPayment(String playerName) {
        try {
            coreAvailablePlayerService.updateUnPaidAvailablePlayer(AvailablePlayerPayDTO.builder().name(playerName).build());
        } catch (BusinessException e) {
            throw new RuntimeException(ErrorUtils.buildErrorMessage(e.getErrorCodeEnum()));
        }
        return Boolean.TRUE;
    }

    private ErrorCodeEnum resolveErrorCode(int errorCode) {
        return Arrays.stream(ErrorCodeEnum.values())
                .filter(code -> Integer.parseInt(code.getCode()) == errorCode)
                .findFirst()
                .orElse(ErrorCodeEnum.INTERNAL_SERVER_ERROR);
    }
}

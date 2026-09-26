package com.badminton.core.debit;

import com.badminton.entity.Debit;
import com.badminton.entity.Payment;
import com.badminton.entity.PaymentDebit;
import com.badminton.enums.DebitStatus;
import com.badminton.exception.BusinessException;
import com.badminton.exception.enums.ErrorCodeEnum;
import com.badminton.model.debit.DebitModel;
import com.badminton.model.debit.PayDebitModel;
import com.badminton.model.dto.PayDebitDTO;
import com.badminton.repository.DebitRepository;
import com.badminton.repository.PaymentDebitRepository;
import com.badminton.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

@Service
public class CorePayDebitService {
    @Autowired
    PaymentRepository paymentRepository;
    @Autowired
    PaymentDebitRepository paymentDebitRepository;
    @Autowired
    DebitRepository debitRepository;

    /**
     * Perform a single payment linked to a single debit.
     * Creates a Payment record, a PaymentDebit link, and updates the debit's
     * remaining amount and status.
     *
     * @param payDebit the payment instruction for one debit
     * @return PayDebitModel when payment is applied
     */
    @Transactional(rollbackFor = Exception.class)
    public PayDebitModel payForDebit(PayDebitDTO payDebit) throws BusinessException {
        if (payDebit == null || payDebit.getPayForDebit() == null
                || payDebit.getPayForDebit().getDebitId() == null) {
            throw new BusinessException(ErrorCodeEnum.INVALID_PAYMENT_AMOUNT, "Invalid debit payment request");
        }

        Optional<Debit> debitOpt = debitRepository.findByIdForUpdate(payDebit.getPayForDebit().getDebitId());
        if (!debitOpt.isPresent()) {
            throw new BusinessException(ErrorCodeEnum.DEBIT_NOT_FOUND, "Debit not found with ID: " + payDebit.getPayForDebit().getDebitId());
        }

        Debit debit = debitOpt.get();
        BigDecimal payAmount = payDebit.getPayAmount();
        if (payAmount == null || payAmount.compareTo(BigDecimal.ZERO) <= 0
                || payAmount.compareTo(debit.getRemainingAmount()) > 0) {
            throw new BusinessException(ErrorCodeEnum.INVALID_PAYMENT_AMOUNT,
                    "Payment amount is invalid for debit ID: " + debit.getDebitId());
        }

        Payment payment = new Payment(payAmount, payDebit.getNote(), debit.getPlayer(), payDebit.getPayMethod());
        paymentRepository.save(payment);

        PaymentDebit paymentDebit = new PaymentDebit(payment, debit, payAmount);
        paymentDebitRepository.save(paymentDebit);

        BigDecimal newRemainingAmount = debit.getRemainingAmount().subtract(payAmount);
        debit.setRemainingAmount(newRemainingAmount);
        debit.setStatus(newRemainingAmount.compareTo(BigDecimal.ZERO) == 0
                ? DebitStatus.PAID
                : DebitStatus.PARTIALLY_PAID);
        debitRepository.save(debit);

        return PayDebitModel.builder()
                .playerName(debit.getPlayer().getPlayerName())
                .paymentAmount(payAmount)
                .paymentMethod(payDebit.getPayMethod())
                .paidDebit(DebitModel.builder()
                        .debitId(debit.getDebitId())
                        .dateTime(debit.getCreatedDate())
                        .money(payAmount)
                        .note(debit.getNote())
                        .build())
                .paymentDate(Instant.now())
                .status(debit.getStatus().name())
                .build();
    }
}

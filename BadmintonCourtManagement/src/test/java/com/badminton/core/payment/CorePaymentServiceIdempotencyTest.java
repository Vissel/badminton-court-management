package com.badminton.core.payment;

import com.badminton.constant.PayType;
import com.badminton.core.billing.CoreBillingService;
import com.badminton.core.debit.CoreDebitService;
import com.badminton.core.player.CoreAvailablePlayerService;
import com.badminton.entity.Invoice;
import com.badminton.model.dto.PaymentDTO;
import com.badminton.model.payment.PaymentDebitModel;
import com.badminton.repository.PaymentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * G-7: a retried checkout must return the bill already issued for the client
 * transaction id — never re-create the debit or the debt allocation.
 */
@ExtendWith(MockitoExtension.class)
class CorePaymentServiceIdempotencyTest {

    @Mock
    private CoreBillingService coreBillingService;
    @Mock
    private CoreDebitService coreDebitService;
    @Mock
    private CoreAvailablePlayerService coreAvailablePlayerService;
    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private CorePaymentService corePaymentService;

    @Test
    @DisplayName("A repeated clientTransactionId replays the settled bill without touching money")
    void replayReturnsSettledBillWithoutCharging() throws Exception {
        Invoice settled = new Invoice();
        settled.setInvoiceId(42L);
        settled.setBillNo("HD000042");
        settled.setPayType(PayType.CASH);
        settled.setTotal(new BigDecimal("75000"));
        settled.setIssuedAt(Instant.parse("2026-11-01T10:00:00Z"));

        when(coreBillingService.findByClientTxnId("txn-1")).thenReturn(settled);

        PaymentDTO dto = new PaymentDTO();
        dto.setPlayerName("Nguyen Van A");
        dto.setTotalPay("75000");
        dto.setClientTransactionId("txn-1");

        PaymentDebitModel model = corePaymentService.payForPlayerAndCreateDebt(dto);

        assertEquals(42L, model.getBillId());
        assertEquals("HD000042", model.getBillNo());
        assertEquals(0, model.getPayAmount().compareTo(new BigDecimal("75000")));
        assertEquals("Nguyen Van A", model.getPayFor());
        verifyNoInteractions(coreDebitService, coreAvailablePlayerService, paymentRepository);
    }
}

package com.badminton.repository;

import com.badminton.entity.Debit;
import com.badminton.entity.Payment;
import com.badminton.entity.PaymentDebit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface PaymentDebitRepository extends JpaRepository<PaymentDebit, Long> {
    List<PaymentDebit> findByPayment(Payment payment);
    List<PaymentDebit> findByDebit(Debit debit);

    /**
     * Latest payment date per debit: rows of [debitId, MAX(payment.paymentDate)].
     */
    @Query("SELECT pd.debit.debitId, MAX(pd.payment.paymentDate) FROM PaymentDebit pd " +
            "WHERE pd.debit.debitId IN :debitIds GROUP BY pd.debit.debitId")
    List<Object[]> findLastPaymentDateByDebitIds(@Param("debitIds") Collection<Integer> debitIds);
}

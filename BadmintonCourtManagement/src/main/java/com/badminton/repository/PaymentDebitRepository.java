package com.badminton.repository;

import com.badminton.entity.Debit;
import com.badminton.entity.Payment;
import com.badminton.entity.PaymentDebit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentDebitRepository extends JpaRepository<PaymentDebit, Long> {
    List<PaymentDebit> findByPayment(Payment payment);
    List<PaymentDebit> findByDebit(Debit debit);
}

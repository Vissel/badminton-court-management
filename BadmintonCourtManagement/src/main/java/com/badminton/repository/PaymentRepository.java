package com.badminton.repository;

import com.badminton.entity.Payment;
import com.badminton.entity.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByPlayerOrderByPaymentDateDesc(Player player);
}

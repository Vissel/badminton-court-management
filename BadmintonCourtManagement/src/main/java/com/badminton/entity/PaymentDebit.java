package com.badminton.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payment_debit")
@Getter
@Setter
@NoArgsConstructor
public class PaymentDebit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long paymentDebitId;

    @ManyToOne
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @ManyToOne
    @JoinColumn(name = "debit_id", nullable = false)
    private Debit debit;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amountApplied;

    @Column(updatable = false, insertable = false)
    private Instant createdDate;

    public PaymentDebit(Payment payment, Debit debit, BigDecimal amountApplied) {
        this.payment = payment;
        this.debit = debit;
        this.amountApplied = amountApplied;
    }
}

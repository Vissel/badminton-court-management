package com.badminton.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payment")
@Getter
@Setter
@NoArgsConstructor
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long paymentId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(length = 10)
    private String currency = "VND";

    @Column(updatable = false, insertable = false)
    private Instant paymentDate;

    @Column(length = 250)
    private String note;

    @ManyToOne
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    public Payment(BigDecimal amount, Player player) {
        this.amount = amount;
        this.player = player;
    }

    public Payment(BigDecimal amount, String note, Player player) {
        this.amount = amount;
        this.note = note;
        this.player = player;
    }
}

package com.badminton.entity;

import com.badminton.enums.DebitStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "debit")
@Getter
@Setter
@NoArgsConstructor
public class Debit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int debitId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal debtAmount;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal remainingAmount;

    @Column(length = 10)
    private String currency = "VND";

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private DebitStatus status = DebitStatus.PENDING;

    @Column(updatable = false, insertable = false)
    private Instant createdDate;

    @Column(length = 250)
    private String note;

    @ManyToOne
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @ManyToOne
    @JoinColumn(name = "session_id", nullable = false)
    private Session session;

    public Debit(BigDecimal debtAmount, Player player, Session session) {
        this.debtAmount = debtAmount;
        this.player = player;
        this.session = session;
    }

    public Debit(BigDecimal debtAmount, String note, Player player, Session session) {
        this.debtAmount = debtAmount;
        this.note = note;
        this.player = player;
        this.session = session;
    }
}

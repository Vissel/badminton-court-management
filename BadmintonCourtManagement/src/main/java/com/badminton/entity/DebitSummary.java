package com.badminton.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "debit_summary")
@Getter
@Setter
@NoArgsConstructor
public class DebitSummary {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long debtSumId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalDebts;

    @Column(length = 10)
    private String currency = "VND";

    @Column(nullable = false)
    private Integer numDebts;

    @ManyToOne
    @JoinColumn(name = "player_id")
    private Player player;

    @Column(name = "is_active", columnDefinition = "tinyint(1) default 1")
    private Boolean isActive = true;

    @Column(name = "last_update", updatable = false)
    @UpdateTimestamp
    private Instant lastUpdate;

    public DebitSummary(BigDecimal totalDebts, Integer numDebts) {
        this.totalDebts = totalDebts;
        this.numDebts = numDebts;
    }

    public DebitSummary(BigDecimal totalDebts, String currency, Integer numDebts) {
        this.totalDebts = totalDebts;
        this.currency = currency;
        this.numDebts = numDebts;
    }
}

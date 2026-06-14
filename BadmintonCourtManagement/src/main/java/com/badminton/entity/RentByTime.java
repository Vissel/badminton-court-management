package com.badminton.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "rent_by_time")
@Getter
@Setter
@NoArgsConstructor
public class RentByTime {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne
    @JoinColumn(name = "ava_id", nullable = false)
    private AvailablePlayer availablePlayer;

    @ManyToOne
    @JoinColumn(name = "court_id", nullable = false)
    private Court court;

    @Column(name = "start_time")
    private Instant startTime;

    @Column(name = "end_time")
    private Instant endTime;

    @Column(name = "num_time", precision = 4, scale = 2)
    private BigDecimal numTime;

    @Column(name = "shuttles")
    private String shuttles;

    @Column(name = "state")
    private String state;

    public RentByTime(AvailablePlayer availablePlayer, Court court, Instant startTime, Instant endTime,
            BigDecimal numTime,
            String shuttles, String state) {
        this.availablePlayer = availablePlayer;
        this.court = court;
        this.startTime = startTime;
        this.endTime = endTime;
        this.numTime = numTime;
        this.shuttles = shuttles;
        this.state = state;
    }
}

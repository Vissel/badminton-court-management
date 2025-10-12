package com.badminton.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
public class GameShuttleMap {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne
    @JoinColumn(name = "game_id")
    private Game game;

    @OneToOne(cascade = CascadeType.DETACH)
    @JoinColumn(name = "shuttle_id", nullable = false)
    private ShuttleBall shuttleBall;

    private int shuttleNumber;
}

package com.badminton.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity()
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

    public GameShuttleMap(Game game, ShuttleBall shuttleBall, int shuttleNumber) {
        this.game = game;
        this.shuttleBall = shuttleBall;
        this.shuttleNumber = shuttleNumber;
    }
}

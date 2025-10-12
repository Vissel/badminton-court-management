package com.badminton.entity;

import com.badminton.constant.GameState;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.Timestamp;
import java.util.List;

@Entity
@Table(name = "game")
@Getter
@Setter
@NoArgsConstructor
public class Game {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int gameId;

    @ManyToOne(cascade = CascadeType.DETACH)
    @JoinColumn(name = "court_id", nullable = false)
    private Court court;

    @ManyToOne(cascade = CascadeType.DETACH)
    @JoinColumn(name = "shuttle_id", nullable = false)
    private ShuttleBall shuttleBall;

    private int shuttleNumber;

    @OneToMany(mappedBy = "game")
    @JoinColumn(name = "game_shuttle_id")
    private List<GameShuttleMap> shuttleMap;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "team_id1", nullable = true)
    private Team teamOne;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "team_id2", nullable = true)
    private Team teamTwo;

    @Column(updatable = false, insertable = false)
    private Timestamp createdDate;

    @Column(nullable = true)
    private Timestamp endedDate;

    /**
     * Follow GameState
     */
    private String state;

    public Game(Court court, ShuttleBall shuttleBall) {
        this.court = court;
        this.shuttleBall = shuttleBall;
        // default quantity is 1
        this.shuttleNumber = 1;
        // default Not start
        this.state = GameState.NOT_START.getValue();
    }
}
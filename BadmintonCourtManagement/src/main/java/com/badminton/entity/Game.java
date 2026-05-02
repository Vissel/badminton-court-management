package com.badminton.entity;

import com.badminton.constant.GameState;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
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

//    @ManyToOne(cascade = CascadeType.DETACH)
//    @JoinColumn(name = "shuttle_id", nullable = false)
//    private ShuttleBall shuttleBall;
//
//    private int shuttleNumber;

    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GameShuttleMap> shuttleMap;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "team_id1", nullable = true)
    private Team teamOne;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "team_id2", nullable = true)
    private Team teamTwo;

    @Column(updatable = false, insertable = false)
    private Instant createdDate;

    @Column(nullable = true)
    private Instant endedDate;

    /**
     * Follow GameState
     */
    private String state;

    /**
     * game type: com.badminton.constant.GameType SHARE or NEGO
     */
    private String gtype;

    /**
     * constructor init a game
     *
     * @param court
     * @param shuttleBall
     */
    public Game(Court court, ShuttleBall shuttleBall) {
        this.court = court;
//        this.shuttleBall = shuttleBall;
//        this.shuttleNumber = 1;

        // default Not start
        this.state = GameState.NOT_START.getValue();

        // Add game shuttle map with default quantity is 1
        shuttleMap = new ArrayList<>();
        GameShuttleMap gameBallMap = new GameShuttleMap(this, shuttleBall, 1);
        shuttleMap.add(gameBallMap);
    }
}
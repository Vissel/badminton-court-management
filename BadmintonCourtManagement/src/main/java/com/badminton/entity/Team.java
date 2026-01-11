package com.badminton.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "team")
@Getter
@Setter
@NoArgsConstructor
public class Team {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int teamId;

    @ManyToOne
    @JoinColumn(name = "player_id1")
    private AvailablePlayer playerOne;

    @Column(name = "expense_1")
    private float expenseOne;

    @ManyToOne
    @JoinColumn(name = "player_id2")
    private AvailablePlayer playerTwo;

    @Column(name = "expense_2")
    private float expenseTwo;

    @Column(name = "is_status")
    private boolean win;

    @OneToOne
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

}

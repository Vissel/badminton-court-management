package com.badminton.model.dto;

import com.badminton.entity.Team;
import lombok.Data;

@Data
public class TeamDTO {
    private boolean win;
    private PlayerDTO player1;
    private PlayerDTO player2;

    public TeamDTO(Team team) {
        this.win = team.isWin();
        this.player1 = new PlayerDTO(team.getPlayerOne());
        this.player2 = new PlayerDTO(team.getPlayerTwo());
    }
}

package com.badminton.requestmodel;

import com.badminton.entity.Game;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GameDTO extends ResponseDTO {
	private String playerName;
	private CourtDTO court;
	private ShuttleBallDTO shuttleBall;
	private int ballQuantity;
	private String gameState;

	public GameDTO(Game game) {
		this.shuttleBall = new ShuttleBallDTO(game.getShuttleBall());
		this.ballQuantity = game.getShuttleNumber();
		this.gameState = game.getState();
		this.court = new CourtDTO(game.getCourt(), game.getTeamOne(), game.getTeamTwo());
	}
}

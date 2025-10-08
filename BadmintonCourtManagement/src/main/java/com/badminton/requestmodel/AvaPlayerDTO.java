package com.badminton.requestmodel;

import java.time.Instant;

import com.badminton.entity.AvailablePlayer;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AvaPlayerDTO extends ResponseDTO {
	private int playerId;
	private String playerName;
	private Instant from;
	private Instant to;

	public AvaPlayerDTO(AvailablePlayer avaPlayerEntity) {
		this.playerId = avaPlayerEntity.getPlayer().getPlayerId();
		this.playerName = avaPlayerEntity.getPlayer().getPlayerName();
		this.from = avaPlayerEntity.getSession().getFromTime();
		this.to = avaPlayerEntity.getSession().getToTime();
	}
}

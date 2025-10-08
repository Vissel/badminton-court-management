package com.badminton.requestmodel;

import java.util.ArrayList;
import java.util.List;

import com.badminton.constant.GameState;
import com.badminton.entity.Court;
import com.badminton.entity.Team;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CourtDTO extends ResponseDTO {
	private String courtId;
	private String courtName;
	private List<CourtAreaDTO> courtAreas;

	public CourtDTO(Court court) {
		this.courtId = String.valueOf(court.getCourtId());
		this.courtName = court.getCourtName();
	}

	public CourtDTO(Court court, Team teamOne, Team teamTwo) {
		this(court);
		this.courtAreas = new ArrayList<CourtAreaDTO>();
		courtAreas.addAll(findArea(teamOne, GameState.Player.PLAYER_A, GameState.Player.PLAYER_B));
		courtAreas.addAll(findArea(teamTwo, GameState.Player.PLAYER_C, GameState.Player.PLAYER_D));

	}

	private List<CourtAreaDTO> findArea(Team team, String firstPlayer, String secondPlayer) {
		List<CourtAreaDTO> areas = new ArrayList<>();
		if (team != null) {
			if (team.getPlayerOne() != null) {
				areas.add(new CourtAreaDTO(firstPlayer, team.getPlayerOne()));
			}
			if (team.getPlayerTwo() != null) {
				areas.add(new CourtAreaDTO(secondPlayer, team.getPlayerTwo()));
			}
		}
		return areas;
	}
}

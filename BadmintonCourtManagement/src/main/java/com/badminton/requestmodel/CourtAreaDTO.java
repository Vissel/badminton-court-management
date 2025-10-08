package com.badminton.requestmodel;

import com.badminton.entity.AvailablePlayer;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CourtAreaDTO extends CourtDTO {
	private String area;
	private AvaPlayerDTO playerInArea;

	public CourtAreaDTO(String area, AvailablePlayer player) {
		this.area = area;
		this.playerInArea = new AvaPlayerDTO(player);
	}
}

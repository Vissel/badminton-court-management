package com.badminton.requestmodel;

import com.badminton.entity.ShuttleBall;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ShuttleBallDTO extends ResponseDTO {
	private String shuttleName;
	private float shuttleCost;

	public ShuttleBallDTO(ShuttleBall ballEntity) {
		this.shuttleName = ballEntity.getShuttleName();
		this.shuttleCost = ballEntity.getCost();
	}
}

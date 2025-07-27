package com.badminton.requestmodel;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.badminton.constant.ApiConstant;
import com.badminton.entity.Service;
import com.badminton.entity.ShuttleBall;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SetUpServiceDTO {
	private int totalCourt;
	private float costInPerson;
	private List<ShuttleBallDTO> shuttleBalls;
	private List<ServiceDTO> services;

	public void convertToDTO(int totalCourt2, List<ShuttleBall> listShuttleBall, List<Service> listService) {
		this.totalCourt = totalCourt2;
		this.shuttleBalls = listShuttleBall.stream()
				.map(ball -> new ShuttleBallDTO(ball.getShuttleName(), ball.getCost())).collect(Collectors.toList());
		this.services = listService.stream().map(ser -> {
			if (ser.getSerName().equals(ApiConstant.COST_IN_PERNSON)) {
				setCostInPerson(ser.getCost());
				return null;
			}
			return new ServiceDTO(ser.getSerName(), ser.getCost());

		}).filter(Objects::nonNull).collect(Collectors.toList());
	}
}

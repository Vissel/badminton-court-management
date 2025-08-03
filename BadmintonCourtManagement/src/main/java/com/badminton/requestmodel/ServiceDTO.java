package com.badminton.requestmodel;

import com.badminton.entity.Service;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ServiceDTO extends ResponseDTO {
	private String serviceName;
	private float cost;

	public ServiceDTO(Service serviceEntity) {
		this.serviceName = serviceEntity.getSerName();
		this.cost = serviceEntity.getCost();
	}
}

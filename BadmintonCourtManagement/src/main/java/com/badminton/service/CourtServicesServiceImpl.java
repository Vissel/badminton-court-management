package com.badminton.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;

import com.badminton.entity.Service;
import com.badminton.repository.ServiceRepositoty;
import com.badminton.requestmodel.ServiceDTO;

@org.springframework.stereotype.Service
public class CourtServicesServiceImpl {

	@Autowired
	private ServiceRepositoty serviceRepo;

	public List<ServiceDTO> getActiveServices() {
		List<Service> activeServices = serviceRepo.findAllByIsActive(true);
		return activeServices.stream().map(s -> new ServiceDTO(s)).collect(Collectors.toList());
	}
}

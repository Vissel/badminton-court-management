package com.badminton.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.badminton.entity.ShuttleBall;
import com.badminton.repository.ShuttleBallRepositoty;
import com.badminton.requestmodel.ShuttleBallDTO;

@Service
public class ShuttleBallServiceImpl {

	@Autowired
	private ShuttleBallRepositoty ballRepo;

	public List<ShuttleBallDTO> getActiveShuttleBalls() {
		List<ShuttleBall> activeBalls = ballRepo.findAllByIsActive(true);
		return activeBalls.stream().map(b -> new ShuttleBallDTO(b)).collect(Collectors.toList());
	}
}

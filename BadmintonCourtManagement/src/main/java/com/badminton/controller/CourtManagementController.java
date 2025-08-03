package com.badminton.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.badminton.requestmodel.ServiceDTO;
import com.badminton.requestmodel.ShuttleBallDTO;
import com.badminton.service.CourtServicesServiceImpl;
import com.badminton.service.ShuttleBallServiceImpl;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/court-mana")
public class CourtManagementController {

	@Autowired
	private ShuttleBallServiceImpl ballService;
	@Autowired
	private CourtServicesServiceImpl courtService;

	@GetMapping(value = "/getShuttleBalls")
	public ResponseEntity<List<ShuttleBallDTO>> getShuttleBalls() {
		List<ShuttleBallDTO> res = ballService.getActiveShuttleBalls();
		log.info("Size active shuttle balls is:{}", res.size());
		// Error cases are not handled
		return ResponseEntity.ok().body(res);
	}

	@GetMapping(value = "/getServices")
	public ResponseEntity<List<ServiceDTO>> getServices() {
		List<ServiceDTO> res = courtService.getActiveServices();
		log.info("Size active services is:{}", res.size());
		// Error cases are not handled
		return ResponseEntity.ok().body(res);
	}
}

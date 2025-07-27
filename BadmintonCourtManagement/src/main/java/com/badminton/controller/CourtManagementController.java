package com.badminton.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.badminton.requestmodel.ServiceDTO;
import com.badminton.requestmodel.SetUpServiceDTO;
import com.badminton.requestmodel.ShuttleBallDTO;
import com.badminton.service.AdminService;

@RestController
@RequestMapping("/api")
public class CourtManagementController {

	@Autowired
	AdminService adminService;

	@GetMapping(value = "/getSetupServices")
	public ResponseEntity<SetUpServiceDTO> getSetupService() {
		SetUpServiceDTO setupServiceDTO = adminService.getSetUpService();
		return ResponseEntity.ok().body(setupServiceDTO);

	}

	@PostMapping(value = "/addSetupService")
	public ResponseEntity<?> addSetupService(@RequestBody SetUpServiceDTO setUpServiceDTO) {
		if (adminService.setUpService(setUpServiceDTO)) {
			return ResponseEntity.ok().body("");

		}
		return ResponseEntity.badRequest().body("");
	}

	@PutMapping(value = "/deleteService")
	public ResponseEntity<String> deleteService(@RequestBody ServiceDTO serviceDTO) {
		if (adminService.deleteService(serviceDTO)) {
			return ResponseEntity.ok().body("");

		}
		return ResponseEntity.badRequest().body("");
	}

	@PutMapping(value = "/deleteShuttleBall")
	public ResponseEntity<String> deleteShuttleBall(@RequestBody ShuttleBallDTO shuttleBallDTO) {
		if (adminService.deleteShuttleBall(shuttleBallDTO)) {
			return ResponseEntity.ok().body("");

		}
		return ResponseEntity.badRequest().body("");
	}
}

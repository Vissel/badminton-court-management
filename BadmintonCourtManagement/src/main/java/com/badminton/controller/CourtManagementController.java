package com.badminton.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.badminton.requestmodel.SetUpServiceDTO;
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
}

package com.badminton.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.badminton.service.SessionServiceImpl;

@RestController
@RequestMapping("/session")
public class SessionController {

	@Autowired
	SessionServiceImpl sessionService;

	@GetMapping(value = "/checkAvailable")
	public ResponseEntity<Boolean> checkAvailableSession() {
		return ResponseEntity.ok(sessionService.checkAvailableSession());
	}

	@PostMapping(value = "/createNewSession")
	public ResponseEntity<Boolean> createNewSession() {
		return ResponseEntity.ok(sessionService.createNewSessionInDay());
	}

	@PostMapping(value = "/deleteSession")
	public ResponseEntity<Boolean> deleteSession() {
		return ResponseEntity.ok(sessionService.deactiveSessions());
	}

}

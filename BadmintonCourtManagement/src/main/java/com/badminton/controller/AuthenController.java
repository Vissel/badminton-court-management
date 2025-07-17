package com.badminton.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthenController {

	@GetMapping("/index")
	public ResponseEntity<String> index() {
		return ResponseEntity.ok("index");
	}

	@GetMapping("/indexDB")
	public ResponseEntity<String> indexDB() {
		return ResponseEntity.ok("indexDB");
	}

	@GetMapping("/csrf")
	public CsrfToken csrf(CsrfToken token) {
		return token; // returns token in JSON and also sets it in cookie
	}
}

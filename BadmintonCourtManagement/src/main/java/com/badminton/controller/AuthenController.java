package com.badminton.controller;

import com.badminton.entity.AppUser;
import com.badminton.repository.AppUserRepository;
import com.badminton.requestmodel.AuthenDTO;
import com.badminton.requestmodel.LoginDTO;
import com.badminton.requestmodel.RefreshTokenRequest;
import com.badminton.service.JwtService;
import com.badminton.util.RsaKeyService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

@RestController
public class AuthenController {
	private final AuthenticationManager authenticationManager;
	private final RsaKeyService rsaKeyService;
	private final AppUserRepository appUserRepository;
	private final JwtService jwtService;

	public AuthenController(AuthenticationManager authenticationManager, RsaKeyService rsaKeyService,
			AppUserRepository appUserRepository, JwtService jwtService) {
		this.authenticationManager = authenticationManager;
		this.rsaKeyService = rsaKeyService;
		this.appUserRepository = appUserRepository;
		this.jwtService = jwtService;
	}

	@GetMapping("/index")
	public ResponseEntity<String> index() {
		return ResponseEntity.ok("index");
	}

	@GetMapping("/public-key")
	public ResponseEntity<String> publicKey() {
		return ResponseEntity.ok(rsaKeyService.getPublicKeyPem());
	}

	@PostMapping("/login")
	public ResponseEntity<AuthenDTO> login(@RequestBody LoginDTO loginDTO) {
		try {
			String password = rsaKeyService.decrypt(loginDTO.getInputPassword());
			authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
					loginDTO.getInputUsername(), password));
			AppUser user = appUserRepository.findByUsername(loginDTO.getInputUsername()).orElseThrow();
			return ResponseEntity.ok(jwtService.createTokenPair(user));
		} catch (AuthenticationException | IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
	}

	@PostMapping("/auth/refresh")
	public ResponseEntity<AuthenDTO> refresh(@RequestBody RefreshTokenRequest request) {
		try {
			return ResponseEntity.ok(jwtService.rotate(request.getRefreshToken()));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
	}

	@PostMapping("/logout")
	public ResponseEntity<Void> logout(@RequestBody(required = false) RefreshTokenRequest request) {
		if (request != null)
			jwtService.revoke(request.getRefreshToken());
		return ResponseEntity.noContent().build();
	}
}

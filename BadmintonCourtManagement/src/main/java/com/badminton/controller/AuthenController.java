package com.badminton.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.badminton.constant.ApiConstant;
import com.badminton.requestmodel.AuthenDTO;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@RestController
public class AuthenController {

	@Value(value = "${session.timeout}")
	private long MAX_TOKEN_AGE_MILLIS; // 30 minutes

//	@Autowired
//	HttpSessionCsrfTokenRepository sessionCsrfToken;

	@GetMapping("/index")
	public ResponseEntity<String> index() {
		return ResponseEntity.ok("index");
	}

	@GetMapping("/indexDB")
	public ResponseEntity<String> indexDB() {
		return ResponseEntity.ok("indexDB");
	}

	@GetMapping("/csrf")
	public ResponseEntity<?> checkToken(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		if (session == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("No session");
		}

		CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
//				sessionCsrfToken.loadToken(request);
		if (csrfToken == null) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Missing CSRF token");
		}

		Long createdAt = (Long) session.getAttribute(ApiConstant.CSRF_TOKEN_CREATED_AT);
		if (createdAt == null) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body("No token creation time");
		}

		long age = System.currentTimeMillis() - createdAt;
		if (age > MAX_TOKEN_AGE_MILLIS) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("CSRF token expired");
		}
		AuthenDTO authDTO = new AuthenDTO();
		authDTO.setCsrfToken(csrfToken.getToken());
		authDTO.setValid(true);
		authDTO.setExpiresInSeconds((MAX_TOKEN_AGE_MILLIS - age) / 1000);

		return ResponseEntity.ok(authDTO);
	}

	@Autowired
	private AuthenticationManager authenticationManager;

	@PostMapping("/login")
	public ResponseEntity<AuthenDTO> login(@RequestParam String username, @RequestParam String password,
			HttpServletRequest request, HttpServletResponse response) {
		try {
			Authentication authentication = authenticationManager
					.authenticate(new UsernamePasswordAuthenticationToken(username, password));

			SecurityContextHolder.getContext().setAuthentication(authentication);

			HttpSession session = request.getSession(true);
			session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
					SecurityContextHolder.getContext());

			// invalidate old session
//			request.getSession().invalidate();
			// Create session and store CSRF token
			session.setMaxInactiveInterval(30 * 60); // 30 minutes
			session.setAttribute(ApiConstant.CSRF_TOKEN_CREATED_AT, System.currentTimeMillis());
			CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
//					AuthenUtil.getCsrfToken(request, response, sessionCsrfToken);

			AuthenDTO dto = new AuthenDTO();
			dto.setMessage("Login successful");
			dto.setUsername(username);
			dto.setCsrfToken(token.getToken());

			return ResponseEntity.ok(dto);

		} catch (AuthenticationException e) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
	}
}

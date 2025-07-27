package com.badminton.util;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class AuthenUtil {

	public static CsrfToken getCsrfToken(HttpServletRequest request, HttpServletResponse response,
			HttpSessionCsrfTokenRepository sessionCsrfToken) {
		CsrfToken token = sessionCsrfToken.loadToken(request);
		if (token == null) {
			token = sessionCsrfToken.generateToken(request);
			sessionCsrfToken.saveToken(token, request, response);
		}
		return token;
	}
}

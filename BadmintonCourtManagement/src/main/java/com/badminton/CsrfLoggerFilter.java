package com.badminton;

import java.io.IOException;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class CsrfLoggerFilter extends OncePerRequestFilter {

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String csrfHeader = request.getHeader("X-XSRF-TOKEN");
		CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
		log.info("[CsrfLoggerFilter] Session ID: " + request.getSession(false));
		log.info("[CsrfLoggerFilter] Incoming CSRF Header: " + csrfHeader);
		log.info("[CsrfLoggerFilter] Expected CSRF Token: " + (token != null ? token.getToken() : "null"));
		filterChain.doFilter(request, response);
	}

}

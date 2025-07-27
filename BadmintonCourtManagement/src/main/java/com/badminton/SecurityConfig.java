package com.badminton;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

	@Autowired
	private CustomUserDetailsService userService;

	@Value(value = "${server.servlet.context-path}")
	private String context;

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, UrlBasedCorsConfigurationSource corsConfigurationSource)
			throws Exception {
		http
				// Disable CSRF for simpler development (be cautious in production)
				.csrf(csrf ->

				csrf.ignoringRequestMatchers("/login", "/logout")
						.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())

				)
				// Configure CORS
				.cors(cors -> cors.configurationSource(corsConfigurationSource))
				// Authorize all requests (adjust as per your security requirements)
				.authorizeHttpRequests(authorize -> authorize.requestMatchers("/login", "/logout", "/index", "/error",
						"/admin/internal/**", "/public/**", "/csrf").permitAll().anyRequest().authenticated())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))

//				.formLogin(form -> form.loginProcessingUrl("/login").usernameParameter("username")
//						.passwordParameter("password").permitAll().successHandler(authenticationSuccessHandler())
//						.failureHandler(authenticationFailureHandler()))
				.logout(logout -> logout.logoutUrl("/logout")
//						.logoutSuccessHandler((req, res, auth) -> res.setStatus(HttpServletResponse.SC_OK)))

						.logoutSuccessHandler((req, res, auth) -> {
							// Invalidate CSRF cookie
//							ResponseCookie csrfCookie = ResponseCookie.from("XSRF-TOKEN", "").path("/").maxAge(0)
//									.build();
//							res.setHeader(HttpHeaders.SET_COOKIE, csrfCookie.toString());
							res.setStatus(HttpServletResponse.SC_OK);
							res.getWriter().write("Logged out");

						})

//				.sessionManagement(session -> session.maximumSessions(1).maxSessionsPreventsLogin(false)
//						.expiredUrl("/login?expired"));

				)
//				.exceptionHandling(ex -> ex.authenticationEntryPoint((req, res, excep) -> {
//					res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
//					res.getWriter().write("Unauthorized");
//				}))
		;

		return http.build();
	}

	@Bean
	AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
		return config.getAuthenticationManager();
	}

	@Bean
	AuthenticationProvider authenticationProvider() {
		DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider(userService);
		authenticationProvider.setPasswordEncoder(passwordEncoder());
		return authenticationProvider;
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	UrlBasedCorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();
		// Allow all origins (use specific origins in production)
		configuration.setAllowedOriginPatterns(Arrays.asList("http://localhost:3000", "http://localhost:8080"));
		// Allow all HTTP methods
		configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE"));
		// Allow all headers
		configuration.setAllowedHeaders(Arrays.asList("*"));
		// Allow credentials (e.g., cookies, authorization headers)
		configuration.setAllowCredentials(true);
//		configuration.setExposedHeaders(List.of("csrfToken"));// set token
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		// Apply this CORS configuration to all paths
		source.registerCorsConfiguration("/**", configuration);

		return source;
	}

	// Custom success handler to return JSON instead of redirecting
//	@Bean
//	AuthenticationSuccessHandler authenticationSuccessHandler() {
//		return new AuthenticationSuccessHandler() {
//			@Override
//			public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
//					Authentication authentication) throws IOException {
//				response.setStatus(HttpServletResponse.SC_OK); // HTTP 200 OK
//				// Return CSRF token after login
////				CsrfToken csrf = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
////				request.getHeader(ApiConstant.HEADER_X_XCSRF_TOKEN)
//				AuthenDTO authResponse = new AuthenDTO("Login successful", authentication.getName(),
//						request.getHeader(ApiConstant.HEADER_X_XCSRF_TOKEN));
//				response.setContentType("application/json");
//				response.getWriter().write(authResponse.toString());
//
//				response.getWriter().flush();
//				// If you use session, Spring Security will automatically set the JSESSIONID
//				// cookie
//			}
//		};
//	}

	// Custom failure handler to return JSON instead of redirecting
//	@Bean
//	AuthenticationFailureHandler authenticationFailureHandler() {
//		return new AuthenticationFailureHandler() {
//			@Override
//			public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
//					AuthenticationException exception) throws IOException {
//				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // HTTP 401 Unauthorized
//				response.setContentType("application/json");
//				AuthenDTO authResponse = new AuthenDTO("Authentication failed:" + exception.getMessage(),
//						CommonConstant.EMPTY, CommonConstant.EMPTY);
//				response.getWriter().write(authResponse.toString());
//				response.getWriter().flush();
//			}
//		};
//	}

//	@Bean
	HttpSessionCsrfTokenRepository csrfRepository() {
//		CookieCsrfTokenRepository repo = CookieCsrfTokenRepository.withHttpOnlyFalse();
//		repo.setCookiePath(context);
		HttpSessionCsrfTokenRepository repo = new HttpSessionCsrfTokenRepository();
		repo.setSessionAttributeName("_csrf");

		return repo;
	}
}

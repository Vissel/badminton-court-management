package com.badminton.config;

import com.badminton.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Component
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    private static final String ROOT = "ROOT";
    private static final String ADMIN = "ADMINISTRATOR";
    private static final String COORDINATOR = "COORDINATOR";

    private final CustomUserDetailsService userService;

    public SecurityConfig(CustomUserDetailsService userService) {
        this.userService = userService;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, UrlBasedCorsConfigurationSource cors,
            JwtAuthenticationConverter jwtAuthenticationConverter)
            throws Exception {
        http.csrf(csrf -> csrf.disable())
                .cors(configurer -> configurer.configurationSource(cors))
                // Logout belongs to AuthenController so the refresh token is actually
                // revoked. Spring Security's default LogoutFilter would swallow
                // POST /logout and answer 302 -> /login?logout, and the browser then
                // GETs the POST-only /login endpoint, which renders a 405 error page.
                .logout(logout -> logout.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/login", "/auth/refresh", "/logout", "/index", "/error",
                                "/public/**", "/public-key", "/api/v1/health")
                        .permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/admin/internal/**").hasRole(ROOT)
                        .requestMatchers("/api/inventory/checkStock", "/api/v1/debit/summary",
                                "/api/v1/debit/listRemainingDebts", "/api/v1/debit/prePay")
                        .hasAnyRole(ROOT, ADMIN, COORDINATOR)
                        .requestMatchers("/court-mana/**", "/session/**", "/gameResult/**", "/api/v1/pay/**")
                        .hasAnyRole(ROOT, ADMIN, COORDINATOR)
                        .requestMatchers("/api/v1/users/**").hasAnyRole(ROOT, ADMIN)
                        .requestMatchers("/api/products/**", "/api/inventory/**", "/api/v1/manager/**",
                                "/api/v1/debit/**", "/api/v1/player/**", "/api/**")
                        .hasAnyRole(ROOT, ADMIN)
                        .anyRequest().hasAnyRole(ROOT, ADMIN))
                .oauth2ResourceServer(
                        oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)));
        return http.build();
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        // The "roles" claim carries PascalCase names (Root, Administrator, ...) for
        // the API/UI, while the security rules use canonical ROLE_ROOT /
        // ROLE_ADMINISTRATOR authorities (same as CustomUserDetailsService), so
        // normalize to upper case here.
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            List<String> roles = jwt.getClaimAsStringList("roles");
            if (roles == null) {
                return List.of();
            }
            Collection<GrantedAuthority> authorities = roles.stream()
                    .filter(Objects::nonNull)
                    .map(role -> (GrantedAuthority) new SimpleGrantedAuthority(
                            "ROLE_" + role.trim().toUpperCase(Locale.ROOT)))
                    .toList();
            return authorities;
        });
        return converter;
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UrlBasedCorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins:http://localhost:3000,http://localhost:8080}") String origins) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.stream(origins.split(",")).map(String::trim).toList());
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(List.of(HttpHeaders.CONTENT_DISPOSITION));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}

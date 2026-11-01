package com.badminton.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The "roles" claim is PascalCase for the API/UI, but the security rules expect
 * the
 * canonical ROLE_ROOT / ROLE_ADMINISTRATOR authorities. This guards the
 * mapping.
 */
class SecurityConfigJwtAuthoritiesTest {

    private final JwtAuthenticationConverter converter = new SecurityConfig(null).jwtAuthenticationConverter();

    private Jwt jwt(List<String> roles) {
        Jwt.Builder builder = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("root1")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300));
        if (roles != null) {
            builder.claim("roles", roles);
        }
        return builder.build();
    }

    @Test
    void mapsPascalCaseClaimValuesToCanonicalAuthorities() {
        AbstractAuthenticationToken authentication = converter.convert(jwt(List.of("Root", "Administrator")));

        List<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        assertEquals(2, authorities.size());
        assertTrue(authorities.contains("ROLE_ROOT"));
        assertTrue(authorities.contains("ROLE_ADMINISTRATOR"));
    }

    @Test
    void keepsLegacyCasingClaimWorking() {
        AbstractAuthenticationToken camel = converter.convert(jwt(List.of("coordinator")));
        assertEquals("ROLE_COORDINATOR", camel.getAuthorities().iterator().next().getAuthority());

        AbstractAuthenticationToken upper = converter.convert(jwt(List.of("COORDINATOR")));
        assertEquals("ROLE_COORDINATOR", upper.getAuthorities().iterator().next().getAuthority());
    }

    @Test
    void missingRolesClaimYieldsNoAuthorities() {
        AbstractAuthenticationToken authentication = converter.convert(jwt(null));

        assertTrue(authentication.getAuthorities().isEmpty());
    }
}

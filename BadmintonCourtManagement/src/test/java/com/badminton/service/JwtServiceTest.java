package com.badminton.service;

import com.badminton.entity.AppUser;
import com.badminton.entity.RefreshToken;
import com.badminton.entity.Role;
import com.badminton.enums.RoleName;
import com.badminton.repository.RefreshTokenRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class JwtServiceTest {
    private final JwtEncoder encoder = mock(JwtEncoder.class);
    private final RefreshTokenRepository repository = mock(RefreshTokenRepository.class);
    private final JwtService service = new JwtService(encoder, repository, Duration.ofMinutes(30), Duration.ofHours(12));

    @Test
    void createTokenPairIncludesUserRolesAndPersistsHashedRefreshToken() {
        when(encoder.encode(any(JwtEncoderParameters.class))).thenReturn(jwt());
        AppUser user = user(RoleName.COORDINATOR);

        var response = service.createTokenPair(user);

        assertEquals("access-token", response.getAccessToken());
        assertEquals("coordinator", response.getUsername());
        assertEquals(java.util.List.of("COORDINATOR"), response.getRoles());
        assertNotNull(response.getRefreshToken());
        verify(repository).save(argThat(token -> token.getTokenHash().length() == 64
                && !token.getTokenHash().equals(response.getRefreshToken())));
    }

    @Test
    void rotateRevokesUsedTokenAndCreatesANewPair() {
        when(encoder.encode(any(JwtEncoderParameters.class))).thenReturn(jwt());
        RefreshToken stored = new RefreshToken(user(RoleName.ADMINISTRATOR), "hash", Instant.now().plusSeconds(60));
        when(repository.findByTokenHash(anyString())).thenReturn(Optional.of(stored));

        var response = service.rotate("old-refresh-token");

        assertTrue(stored.isRevoked());
        assertNotNull(response.getRefreshToken());
        verify(repository).save(any(RefreshToken.class));
    }

    @Test
    void reusedRefreshTokenRevokesAllTokensForUser() {
        RefreshToken stored = new RefreshToken(user(RoleName.ROOT), "hash", Instant.now().plusSeconds(60));
        stored.setRevoked(true);
        when(repository.findByTokenHash(anyString())).thenReturn(Optional.of(stored));

        assertThrows(IllegalArgumentException.class, () -> service.rotate("reused-token"));
        verify(repository).deleteByUser(stored.getUser());
    }

    private AppUser user(RoleName roleName) {
        Role role = new Role();
        role.setRoleName(roleName);
        return new AppUser(roleName.name().toLowerCase(), "password", role);
    }

    private Jwt jwt() {
        Instant now = Instant.now();
        return new Jwt("access-token", now, now.plusSeconds(1800),
                java.util.Map.of("alg", "RS256"), java.util.Map.of("sub", "user"));
    }
}

package com.badminton.service;

import com.badminton.entity.AppUser;
import com.badminton.entity.RefreshToken;
import com.badminton.enums.RoleName;
import com.badminton.repository.RefreshTokenRepository;
import com.badminton.requestmodel.AuthenDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class JwtService {
    private final JwtEncoder jwtEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();
    private final Duration accessTtl;
    private final Duration refreshTtl;

    /** Vietnamese display label for each PascalCase role key. */
    private static final Map<String, String> VN_ROLE_LABELS = buildVnRoleLabels();

    private static Map<String, String> buildVnRoleLabels() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("Root", "Quản trị gốc");
        m.put("Administrator", "Quản trị viên");
        m.put("Coordinator", "Điều phối viên");
        m.put("Player", "Người chơi");
        return m;
    }

    private static String toPascalCase(RoleName role) {
        if (role == null)
            return null;
        String name = role.name().toLowerCase();
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    public JwtService(JwtEncoder jwtEncoder, RefreshTokenRepository refreshTokenRepository,
            @Value("${jwt.access-token-ttl:PT30M}") Duration accessTtl,
            @Value("${jwt.refresh-token-ttl:PT12H}") Duration refreshTtl) {
        this.jwtEncoder = jwtEncoder;
        this.refreshTokenRepository = refreshTokenRepository;
        this.accessTtl = accessTtl;
        this.refreshTtl = refreshTtl;
    }

    @Transactional
    public AuthenDTO createTokenPair(AppUser user) {
        String refreshToken = newRefreshToken();
        refreshTokenRepository.save(new RefreshToken(user, hash(refreshToken), Instant.now().plus(refreshTtl)));
        return response(user, refreshToken);
    }

    @Transactional(noRollbackFor = IllegalArgumentException.class)
    public AuthenDTO rotate(String rawRefreshToken) {
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash(rawRefreshToken))
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));
        if (stored.isRevoked()) {
            refreshTokenRepository.deleteByUser(stored.getUser());
            throw new IllegalArgumentException("Refresh token reuse detected");
        }
        if (stored.getExpiresAt().isBefore(Instant.now()) || !stored.getUser().isActive()
                || stored.getUser().isDeleted()) {
            stored.setRevoked(true);
            throw new IllegalArgumentException("Refresh token expired");
        }
        stored.setRevoked(true);
        return createTokenPair(stored.getUser());
    }

    @Transactional
    public void revoke(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank())
            return;
        refreshTokenRepository.findByTokenHash(hash(rawRefreshToken)).ifPresent(token -> token.setRevoked(true));
    }

    @Transactional
    public void revokeAllForUser(AppUser user) {
        refreshTokenRepository.deleteByUser(user);
    }

    private AuthenDTO response(AppUser user, String refreshToken) {
        List<String> roles = user.getRoles().stream()
                .map(role -> toPascalCase(role.getRoleName()))
                .sorted()
                .toList();
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("badminton-court-management")
                .issuedAt(now)
                .expiresAt(now.plus(accessTtl))
                .subject(user.getUsername())
                .claim("roles", roles)
                .build();
        String accessToken = jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
        AuthenDTO response = new AuthenDTO();
        response.setMessage("Login successful");
        response.setUsername(user.getUsername());
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setRoles(roles);
        response.setVnRoleMapping(VN_ROLE_LABELS);
        response.setExpiresInSeconds(accessTtl.toSeconds());
        response.setValid(true);
        return response;
    }

    private String newRefreshToken() {
        byte[] bytes = new byte[48];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String token) {
        if (token == null || token.isBlank())
            throw new IllegalArgumentException("Refresh token is required");
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}

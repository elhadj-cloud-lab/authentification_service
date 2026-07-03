package com.bestech.authentification_service.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenServiceTest {

    private static final String SECRET = "test-secret";

    private InMemoryTokenBlacklistService blacklistService;
    private JwtTokenService jwtTokenService;

    @BeforeEach
    void setUp() {
        blacklistService = new InMemoryTokenBlacklistService();
        ReflectionTestUtils.setField(blacklistService, "accessExpiration", 900_000L);

        jwtTokenService = new JwtTokenService(blacklistService);
        ReflectionTestUtils.setField(jwtTokenService, "secret", SECRET);
        ReflectionTestUtils.setField(jwtTokenService, "expiration", 900_000L);
    }

    @Test
    void createAccessToken_producesValidSignedJwt() {
        String token = jwtTokenService.createAccessToken("alice", List.of("USER"));

        DecodedJWT decoded = JWT.require(Algorithm.HMAC256(SECRET)).build().verify(token);

        assertThat(decoded.getSubject()).isEqualTo("alice");
        assertThat(decoded.getId()).isNotBlank();
        assertThat(decoded.getExpiresAt()).isNotNull();
    }

    @Test
    void createAccessToken_embedsRolesClaim() {
        String token = jwtTokenService.createAccessToken("admin", List.of("ADMIN", "USER"));

        DecodedJWT decoded = JWT.require(Algorithm.HMAC256(SECRET)).build().verify(token);

        assertThat(decoded.getClaim("roles").asList(String.class))
                .containsExactlyInAnyOrder("ADMIN", "USER");
    }

    @Test
    void createAccessToken_tracksJtiInBlacklistService() {
        String token = jwtTokenService.createAccessToken("bob", List.of("USER"));

        DecodedJWT decoded = JWT.decode(token);
        String jti = decoded.getId();

        // JTI is tracked but not blacklisted yet
        assertThat(blacklistService.isBlacklisted(jti)).isFalse();

        // Revoking all tokens for the user should blacklist the JTI
        blacklistService.revokeAllUserTokens("bob");
        assertThat(blacklistService.isBlacklisted(jti)).isTrue();
    }

    @Test
    void createAccessToken_eachCallProducesUniqueJti() {
        String token1 = jwtTokenService.createAccessToken("carol", List.of("USER"));
        String token2 = jwtTokenService.createAccessToken("carol", List.of("USER"));

        assertThat(JWT.decode(token1).getId()).isNotEqualTo(JWT.decode(token2).getId());
    }
}

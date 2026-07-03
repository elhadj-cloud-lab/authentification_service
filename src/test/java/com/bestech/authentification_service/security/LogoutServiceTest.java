package com.bestech.authentification_service.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.bestech.authentification_service.service.refreshtoken.RefreshTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LogoutServiceTest {

    private static final String SECRET = "test-secret";

    @Mock
    private RefreshTokenService refreshTokenService;

    private InMemoryTokenBlacklistService blacklistService;
    private LogoutService logoutService;

    @BeforeEach
    void setUp() {
        blacklistService = new InMemoryTokenBlacklistService();
        ReflectionTestUtils.setField(blacklistService, "accessExpiration", 900_000L);
        logoutService = new LogoutService(blacklistService, refreshTokenService);
    }

    private String buildToken(String username, String jti) {
        return JWT.create()
                .withJWTId(jti)
                .withSubject(username)
                .withExpiresAt(new Date(System.currentTimeMillis() + 900_000))
                .sign(Algorithm.HMAC256(SECRET));
    }

    @Test
    void logout_blacklistsAccessTokenJti() {
        String jti = "jti-logout-test";
        blacklistService.trackUserToken("alice", jti);
        String accessToken = buildToken("alice", jti);

        logoutService.logout(accessToken, null);

        assertThat(blacklistService.isBlacklisted(jti)).isTrue();
    }

    @Test
    void logout_revokesRefreshTokenWhenProvided() {
        String jti = "jti-with-refresh";
        blacklistService.trackUserToken("alice", jti);

        logoutService.logout(buildToken("alice", jti), "refresh-abc");

        verify(refreshTokenService).revokeByTokenValue("refresh-abc");
    }

    @Test
    void logout_nullRefreshToken_doesNotCallRevoke() {
        String jti = "jti-no-refresh";
        blacklistService.trackUserToken("alice", jti);

        logoutService.logout(buildToken("alice", jti), null);

        verifyNoInteractions(refreshTokenService);
    }

    @Test
    void logout_blankRefreshToken_doesNotCallRevoke() {
        String jti = "jti-blank";
        blacklistService.trackUserToken("alice", jti);

        logoutService.logout(buildToken("alice", jti), "   ");

        verifyNoInteractions(refreshTokenService);
    }

    @Test
    void logout_nullAccessToken_doesNotThrow() {
        logoutService.logout(null, null);
        verifyNoInteractions(refreshTokenService);
    }

    @Test
    void logout_malformedAccessToken_doesNotThrow() {
        logoutService.logout("not.a.jwt.at.all", null);
    }

    @Test
    void revokeAllSessions_blacklistsAllUserJtisAndRevokesRefreshTokens() {
        blacklistService.trackUserToken("bob", "jti-b1");
        blacklistService.trackUserToken("bob", "jti-b2");

        logoutService.revokeAllSessions("bob");

        assertThat(blacklistService.isBlacklisted("jti-b1")).isTrue();
        assertThat(blacklistService.isBlacklisted("jti-b2")).isTrue();
        verify(refreshTokenService).revokeAllForUser("bob");
    }
}

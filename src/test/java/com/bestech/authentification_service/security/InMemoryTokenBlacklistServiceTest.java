package com.bestech.authentification_service.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryTokenBlacklistServiceTest {

    private InMemoryTokenBlacklistService service;

    @BeforeEach
    void setUp() {
        service = new InMemoryTokenBlacklistService();
        ReflectionTestUtils.setField(service, "accessExpiration", 900_000L);
    }

    @Test
    void blacklistJti_thenIsBlacklisted_returnsTrue() {
        Date future = new Date(System.currentTimeMillis() + 60_000);
        service.blacklistJti("jti-1", future);
        assertThat(service.isBlacklisted("jti-1")).isTrue();
    }

    @Test
    void isBlacklisted_expiredEntry_returnsFalseAndCleans() {
        Date past = new Date(System.currentTimeMillis() - 1000);
        service.blacklistJti("jti-old", past);
        assertThat(service.isBlacklisted("jti-old")).isFalse();
    }

    @Test
    void isBlacklisted_unknownJti_returnsFalse() {
        assertThat(service.isBlacklisted("unknown")).isFalse();
    }

    @Test
    void trackAndRevokeAllUserTokens_blacklistsAllJtis() {
        service.trackUserToken("alice", "jti-a1");
        service.trackUserToken("alice", "jti-a2");

        service.revokeAllUserTokens("alice");

        assertThat(service.isBlacklisted("jti-a1")).isTrue();
        assertThat(service.isBlacklisted("jti-a2")).isTrue();
    }

    @Test
    void untrackUserToken_removesJtiBeforeRevoke() {
        service.trackUserToken("bob", "jti-b1");
        service.untrackUserToken("bob", "jti-b1");

        service.revokeAllUserTokens("bob");

        assertThat(service.isBlacklisted("jti-b1")).isFalse();
    }

    @Test
    void revokeAllUserTokens_unknownUser_doesNotThrow() {
        // Should silently complete without error
        service.revokeAllUserTokens("nobody");
    }

    @Test
    void trackUserToken_multipleUsers_areScopedIndependently() {
        service.trackUserToken("alice", "a1");
        service.trackUserToken("bob", "b1");

        service.revokeAllUserTokens("alice");

        assertThat(service.isBlacklisted("a1")).isTrue();
        assertThat(service.isBlacklisted("b1")).isFalse();
    }
}

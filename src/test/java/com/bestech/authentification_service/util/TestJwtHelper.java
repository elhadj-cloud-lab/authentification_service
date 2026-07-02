package com.bestech.authentification_service.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;

import java.util.Arrays;
import java.util.Date;
import java.util.UUID;

/**
 * Utilitaire de génération de JWT signés avec le secret de test.
 * À utiliser uniquement dans les tests d'intégration.
 */
public final class TestJwtHelper {

    public static final String TEST_SECRET = "test-secret";
    private static final long EXPIRATION_MS = 900_000L;

    private TestJwtHelper() {}

    public static String generateToken(String username, String... roles) {
        return JWT.create()
                .withJWTId(UUID.randomUUID().toString())
                .withSubject(username)
                .withArrayClaim("roles", roles)
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + EXPIRATION_MS))
                .sign(Algorithm.HMAC256(TEST_SECRET));
    }

    public static String adminToken() {
        return generateToken("admin", "ADMIN");
    }

    public static String userToken() {
        return generateToken("testuser", "USER");
    }

    public static String expiredToken() {
        return JWT.create()
                .withJWTId(UUID.randomUUID().toString())
                .withSubject("expired")
                .withArrayClaim("roles", new String[]{"USER"})
                .withExpiresAt(new Date(System.currentTimeMillis() - 1000))
                .sign(Algorithm.HMAC256(TEST_SECRET));
    }
}

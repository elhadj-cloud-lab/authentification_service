package com.bestech.authentification_service.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JwtTokenService {

    private final TokenBlacklistService tokenBlacklistService;

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    public String createAccessToken(String username, List<String> roles) {
        String jti = UUID.randomUUID().toString();
        Date expiresAt = new Date(System.currentTimeMillis() + expiration);

        String token = JWT.create()
                .withJWTId(jti)
                .withIssuedAt(new Date())
                .withSubject(username)
                .withArrayClaim("roles", roles.toArray(new String[0]))
                .withExpiresAt(expiresAt)
                .sign(Algorithm.HMAC256(secret));

        tokenBlacklistService.trackUserToken(username, jti);
        return token;
    }
}

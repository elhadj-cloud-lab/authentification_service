package com.bestech.authentification_service.security;

import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.JWT;
import com.bestech.authentification_service.service.refreshtoken.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LogoutService {

    private final TokenBlacklistService tokenBlacklistService;
    private final RefreshTokenService refreshTokenService;

    public void logout(String accessToken, String refreshToken) {
        blacklistAccessToken(accessToken);

        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshTokenService.revokeByTokenValue(refreshToken);
        }
    }

    public void revokeAllSessions(String username) {
        tokenBlacklistService.revokeAllUserTokens(username);
        refreshTokenService.revokeAllForUser(username);
    }

    private void blacklistAccessToken(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            return;
        }

        try {
            DecodedJWT decoded = JWT.decode(accessToken);
            String jti = decoded.getId();
            String username = decoded.getSubject();

            if (jti != null && decoded.getExpiresAt() != null) {
                tokenBlacklistService.blacklistJti(jti, decoded.getExpiresAt());
                if (username != null) {
                    tokenBlacklistService.untrackUserToken(username, jti);
                }
            }
        } catch (JWTDecodeException ignored) {
            // token malformé : rien à blacklister
        }
    }
}

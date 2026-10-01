package com.bestech.authentification_service.security;

import java.util.Date;

public interface TokenBlacklistService {

    void trackUserToken(String username, String jti);

    void untrackUserToken(String username, String jti);

    void blacklistJti(String jti, Date expiresAt);

    boolean isBlacklisted(String jti);

    void revokeAllUserTokens(String username);
}

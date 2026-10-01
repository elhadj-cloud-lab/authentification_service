package com.bestech.authentification_service.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Profile("test")
public class InMemoryTokenBlacklistService implements TokenBlacklistService {

    private final Map<String, Long> blacklist = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> userTokens = new ConcurrentHashMap<>();

    @Value("${jwt.expiration}")
    private Long accessExpiration;

    @Override
    public void trackUserToken(String username, String jti) {
        userTokens.computeIfAbsent(username, key -> ConcurrentHashMap.newKeySet()).add(jti);
    }

    @Override
    public void untrackUserToken(String username, String jti) {
        Set<String> tokens = userTokens.get(username);
        if (tokens != null) {
            tokens.remove(jti);
        }
    }

    @Override
    public void blacklistJti(String jti, Date expiresAt) {
        blacklist.put(jti, expiresAt.getTime());
    }

    @Override
    public boolean isBlacklisted(String jti) {
        Long expiresAt = blacklist.get(jti);
        if (expiresAt == null) {
            return false;
        }
        if (expiresAt < System.currentTimeMillis()) {
            blacklist.remove(jti);
            return false;
        }
        return true;
    }

    @Override
    public void revokeAllUserTokens(String username) {
        Set<String> jtis = userTokens.remove(username);
        if (jtis == null) {
            return;
        }
        long expiresAt = System.currentTimeMillis() + accessExpiration;
        for (String jti : jtis) {
            blacklist.put(jti, expiresAt);
        }
    }
}

package com.bestech.authentification_service.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Date;
import java.util.Set;

@Service
@Profile("!test")
@RequiredArgsConstructor
public class RedisTokenBlacklistService implements TokenBlacklistService {

    private static final String BLACKLIST_PREFIX = "blacklist:jti:";
    private static final String USER_JTI_PREFIX = "user:jti:";
    private static final String REVOKED_VALUE = "revoked";

    private final StringRedisTemplate redisTemplate;

    @Value("${jwt.expiration}")
    private Long accessExpiration;

    @Value("${jwt.refresh-expiration}")
    private Long refreshExpiration;

    @Override
    public void trackUserToken(String username, String jti) {
        String key = USER_JTI_PREFIX + username;
        redisTemplate.opsForSet().add(key, jti);
        redisTemplate.expire(key, Duration.ofMillis(refreshExpiration));
    }

    @Override
    public void untrackUserToken(String username, String jti) {
        redisTemplate.opsForSet().remove(USER_JTI_PREFIX + username, jti);
    }

    @Override
    public void blacklistJti(String jti, Date expiresAt) {
        long ttlSeconds = (expiresAt.getTime() - System.currentTimeMillis()) / 1000;
        if (ttlSeconds > 0) {
            redisTemplate.opsForValue().set(
                    BLACKLIST_PREFIX + jti,
                    REVOKED_VALUE,
                    Duration.ofSeconds(ttlSeconds)
            );
        }
    }

    @Override
    public boolean isBlacklisted(String jti) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + jti));
    }

    @Override
    public void revokeAllUserTokens(String username) {
        String key = USER_JTI_PREFIX + username;
        Set<String> jtis = redisTemplate.opsForSet().members(key);
        if (jtis != null) {
            Duration ttl = Duration.ofMillis(accessExpiration);
            for (String jti : jtis) {
                redisTemplate.opsForValue().set(BLACKLIST_PREFIX + jti, REVOKED_VALUE, ttl);
            }
        }
        redisTemplate.delete(key);
    }
}

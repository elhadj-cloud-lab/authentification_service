package com.bestech.authentification_service.service.refreshtoken;

import com.bestech.authentification_service.model.MyUser;
import com.bestech.authentification_service.model.Role;
import com.bestech.authentification_service.repository.UserRepository;
import com.bestech.authentification_service.security.JwtTokenService;
import com.bestech.authentification_service.service.exceptions.ExpiredTokenException;
import com.bestech.authentification_service.service.exceptions.InvalidTokenException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final JwtTokenService jwtTokenService;

    @Value("${jwt.refresh-expiration}")
    private Long refreshExpiration;

    @Transactional
    public RefreshToken createRefreshToken(String username) {
        MyUser user = userRepository.findByUsername(username);
        if (user == null) {
            throw new InvalidTokenException("Utilisateur introuvable");
        }

        refreshTokenRepository.deleteByUser(user);

        Date expiry = new Date(System.currentTimeMillis() + refreshExpiration);
        RefreshToken refreshToken = new RefreshToken(UUID.randomUUID().toString(), user, expiry);
        return refreshTokenRepository.save(refreshToken);
    }

    @Transactional
    public TokenPair refresh(String tokenValue) {
        RefreshToken token = refreshTokenRepository.findByToken(tokenValue);

        if (token == null) {
            throw new InvalidTokenException("Refresh token invalide");
        }

        if (token.getExpirationTime().before(new Date())) {
            refreshTokenRepository.delete(token);
            throw new ExpiredTokenException("Refresh token expiré");
        }

        MyUser user = token.getUser();
        if (!user.isEnabled()) {
            throw new InvalidTokenException("Utilisateur désactivé");
        }

        refreshTokenRepository.delete(token);

        List<String> roles = user.getRoles().stream().map(Role::getRole).toList();
        String accessToken = jwtTokenService.createAccessToken(user.getUsername(), roles);
        RefreshToken newRefreshToken = createRefreshToken(user.getUsername());

        return new TokenPair(accessToken, newRefreshToken.getToken());
    }

    public record TokenPair(String accessToken, String refreshToken) {}

    @Transactional
    public void revokeByTokenValue(String tokenValue) {
        RefreshToken token = refreshTokenRepository.findByToken(tokenValue);
        if (token != null) {
            refreshTokenRepository.delete(token);
        }
    }

    @Transactional
    public void revokeAllForUser(String username) {
        MyUser user = userRepository.findByUsername(username);
        if (user != null) {
            refreshTokenRepository.deleteByUser(user);
        }
    }
}

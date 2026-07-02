package com.bestech.authentification_service.service;

import com.bestech.authentification_service.model.MyUser;
import com.bestech.authentification_service.model.Role;
import com.bestech.authentification_service.repository.UserRepository;
import com.bestech.authentification_service.security.JwtTokenService;
import com.bestech.authentification_service.service.exceptions.ExpiredTokenException;
import com.bestech.authentification_service.service.exceptions.InvalidTokenException;
import com.bestech.authentification_service.service.refreshtoken.RefreshToken;
import com.bestech.authentification_service.service.refreshtoken.RefreshTokenRepository;
import com.bestech.authentification_service.service.refreshtoken.RefreshTokenService;
import com.bestech.authentification_service.service.refreshtoken.RefreshTokenService.TokenPair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock UserRepository userRepository;
    @Mock JwtTokenService jwtTokenService;

    @InjectMocks RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(refreshTokenService, "refreshExpiration", 604_800_000L);
    }

    private MyUser buildActiveUser(String username) {
        MyUser user = new MyUser();
        user.setUsername(username);
        user.setEnabled(true);
        user.setRoles(List.of());
        return user;
    }

    @Test
    void createRefreshToken_deletesOldTokenAndSavesNew() {
        MyUser user = buildActiveUser("alice");
        when(userRepository.findByUsername("alice")).thenReturn(user);
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RefreshToken result = refreshTokenService.createRefreshToken("alice");

        assertThat(result.getToken()).isNotBlank();
        assertThat(result.getUser()).isSameAs(user);
        verify(refreshTokenRepository).deleteByUser(user);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void createRefreshToken_throwsInvalidTokenException_whenUserNotFound() {
        when(userRepository.findByUsername("ghost")).thenReturn(null);

        assertThatThrownBy(() -> refreshTokenService.createRefreshToken("ghost"))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void refresh_returnsNewTokenPair_whenTokenValid() {
        MyUser user = buildActiveUser("alice");
        RefreshToken token = new RefreshToken("valid-tok", user, new Date(System.currentTimeMillis() + 60_000));

        when(refreshTokenRepository.findByToken("valid-tok")).thenReturn(token);
        when(jwtTokenService.createAccessToken(any(), any())).thenReturn("new-access");
        when(userRepository.findByUsername("alice")).thenReturn(user);
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TokenPair pair = refreshTokenService.refresh("valid-tok");

        assertThat(pair.accessToken()).isEqualTo("new-access");
        assertThat(pair.refreshToken()).isNotBlank();
        verify(refreshTokenRepository).delete(token);
    }

    @Test
    void refresh_throwsInvalidTokenException_whenTokenNotFound() {
        when(refreshTokenRepository.findByToken("missing")).thenReturn(null);

        assertThatThrownBy(() -> refreshTokenService.refresh("missing"))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void refresh_throwsExpiredTokenException_andDeletesToken_whenExpired() {
        MyUser user = buildActiveUser("alice");
        RefreshToken expired = new RefreshToken("exp-tok", user, new Date(System.currentTimeMillis() - 1000));
        when(refreshTokenRepository.findByToken("exp-tok")).thenReturn(expired);

        assertThatThrownBy(() -> refreshTokenService.refresh("exp-tok"))
                .isInstanceOf(ExpiredTokenException.class);

        verify(refreshTokenRepository).delete(expired);
    }

    @Test
    void refresh_throwsInvalidTokenException_whenUserDisabled() {
        MyUser user = buildActiveUser("alice");
        user.setEnabled(false);
        RefreshToken token = new RefreshToken("valid-tok", user, new Date(System.currentTimeMillis() + 60_000));
        when(refreshTokenRepository.findByToken("valid-tok")).thenReturn(token);

        assertThatThrownBy(() -> refreshTokenService.refresh("valid-tok"))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void revokeByTokenValue_deletesTokenWhenFound() {
        RefreshToken token = new RefreshToken("tok", new MyUser(), new Date());
        when(refreshTokenRepository.findByToken("tok")).thenReturn(token);

        refreshTokenService.revokeByTokenValue("tok");

        verify(refreshTokenRepository).delete(token);
    }

    @Test
    void revokeByTokenValue_doesNothingWhenTokenNotFound() {
        when(refreshTokenRepository.findByToken("unknown")).thenReturn(null);

        refreshTokenService.revokeByTokenValue("unknown");

        verify(refreshTokenRepository, never()).delete(any());
    }

    @Test
    void revokeAllForUser_delegatesToRepository_whenUserFound() {
        MyUser user = buildActiveUser("bob");
        when(userRepository.findByUsername("bob")).thenReturn(user);

        refreshTokenService.revokeAllForUser("bob");

        verify(refreshTokenRepository).deleteByUser(user);
    }

    @Test
    void revokeAllForUser_doesNothing_whenUserNotFound() {
        when(userRepository.findByUsername("ghost")).thenReturn(null);

        refreshTokenService.revokeAllForUser("ghost");

        verify(refreshTokenRepository, never()).deleteByUser(any());
    }
}

package com.bestech.authentification_service.security;

import com.bestech.authentification_service.service.LoginEventService;
import com.bestech.authentification_service.service.refreshtoken.RefreshToken;
import com.bestech.authentification_service.service.refreshtoken.RefreshTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JWTAuthenticationFilterTest {

    @Mock AuthenticationManager authenticationManager;
    @Mock JwtTokenService jwtTokenService;
    @Mock RefreshTokenService refreshTokenService;
    @Mock LoginEventService loginEventService;

    JWTAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JWTAuthenticationFilter(
                authenticationManager, jwtTokenService, refreshTokenService, loginEventService,
                new ObjectMapper());
    }

    @Test
    void attemptAuthentication_parsesCredentialsAndCallsAuthManager() throws Exception {
        String body = "{\"username\":\"alice\",\"password\":\"secret\"}";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContent(body.getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();

        UsernamePasswordAuthenticationToken expectedToken =
                new UsernamePasswordAuthenticationToken("alice", "secret");
        when(authenticationManager.authenticate(any())).thenReturn(expectedToken);

        Authentication result = filter.attemptAuthentication(request, response);

        assertThat(result).isNotNull();
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void attemptAuthentication_withEmptyBody_stillCallsAuthManager() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContent("{}".getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(authenticationManager.authenticate(any())).thenReturn(
                new UsernamePasswordAuthenticationToken(null, null));

        filter.attemptAuthentication(request, response);

        verify(authenticationManager).authenticate(any());
    }

    @Test
    void successfulAuthentication_setsAccessTokenAndRefreshTokenHeaders() throws Exception {
        User springUser = new User("alice", "encoded",
                List.of(new SimpleGrantedAuthority("USER")));
        Authentication auth = new UsernamePasswordAuthenticationToken(
                springUser, null, springUser.getAuthorities());

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken("refresh-xyz");

        when(jwtTokenService.createAccessToken(eq("alice"), any())).thenReturn("Bearer token.jwt");
        when(refreshTokenService.createRefreshToken("alice")).thenReturn(refreshToken);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("User-Agent", "TestAgent/1.0");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.successfulAuthentication(request, response, chain, auth);

        assertThat(response.getHeader("Authorization")).isEqualTo("Bearer token.jwt");
        assertThat(response.getHeader("Refresh-Token")).isEqualTo("refresh-xyz");
        verify(loginEventService).recordSuccess(eq("alice"), any(), eq("TestAgent/1.0"));
    }

    @Test
    void successfulAuthentication_extractsClientIpFromXForwardedFor() throws Exception {
        User springUser = new User("alice", "encoded", List.of());
        Authentication auth = new UsernamePasswordAuthenticationToken(
                springUser, null, springUser.getAuthorities());

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken("rt");

        when(jwtTokenService.createAccessToken(any(), any())).thenReturn("Bearer t");
        when(refreshTokenService.createRefreshToken(any())).thenReturn(refreshToken);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "192.168.1.100, 10.0.0.1");

        filter.successfulAuthentication(request, new MockHttpServletResponse(), new MockFilterChain(), auth);

        verify(loginEventService).recordSuccess(eq("alice"), eq("192.168.1.100"), any());
    }

    @Test
    void unsuccessfulAuthentication_withBadCredentials_recordsFailureAndDelegates() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        BadCredentialsException ex = new BadCredentialsException("Bad creds");

        filter.unsuccessfulAuthentication(request, response, ex);

        verify(loginEventService).recordFailure(isNull(), any(), any(), eq("INVALID_CREDENTIALS"));
    }

    @Test
    void unsuccessfulAuthentication_withDisabledAccount_returns403WithJson() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setAttribute("attempted_username", "blocked");
        DisabledException ex = new DisabledException("User disabled");

        filter.unsuccessfulAuthentication(request, response, ex);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType()).isEqualTo("application/json");
        assertThat(response.getContentAsString()).contains("disabled");
        verify(loginEventService).recordFailure(eq("blocked"), any(), any(), eq("DISABLED"));
    }
}

package com.bestech.authentification_service.security;

import com.bestech.authentification_service.service.LoginEventService;
import com.bestech.authentification_service.service.refreshtoken.RefreshToken;
import com.bestech.authentification_service.service.refreshtoken.RefreshTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class JWTAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    private static final String ATTR_USERNAME = "attempted_username";

    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;
    private final RefreshTokenService refreshTokenService;
    private final LoginEventService loginEventService;
    private final ObjectMapper objectMapper;

    public JWTAuthenticationFilter(AuthenticationManager authenticationManager,
                                   JwtTokenService jwtTokenService,
                                   RefreshTokenService refreshTokenService,
                                   LoginEventService loginEventService,
                                   ObjectMapper objectMapper) {
        super();
        this.authenticationManager = authenticationManager;
        this.jwtTokenService = jwtTokenService;
        this.refreshTokenService = refreshTokenService;
        this.loginEventService = loginEventService;
        this.objectMapper = objectMapper;
        setFilterProcessesUrl("/login");
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException {

        String username = null;
        String password = null;

        try {
            @SuppressWarnings("unchecked")
            java.util.Map<String, String> credentials = objectMapper.readValue(request.getInputStream(), java.util.Map.class);
            username = credentials.get("username");
            password = credentials.get("password");
        } catch (IOException e) {
            log.warn("Impossible de lire les credentials depuis le corps de la requête : {}", e.getMessage());
        }

        if (username != null) {
            request.setAttribute(ATTR_USERNAME, username);
        }

        return authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password));
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                            FilterChain chain, Authentication authResult)
            throws IOException, ServletException {

        org.springframework.security.core.userdetails.User springUser =
                (org.springframework.security.core.userdetails.User) authResult.getPrincipal();

        List<String> roles = new ArrayList<>();
        springUser.getAuthorities().forEach(au -> roles.add(au.getAuthority()));

        String accessToken = jwtTokenService.createAccessToken(springUser.getUsername(), roles);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(springUser.getUsername());

        loginEventService.recordSuccess(
                springUser.getUsername(),
                extractClientIp(request),
                request.getHeader("User-Agent"));

        response.addHeader("Authorization", accessToken);
        response.addHeader("Refresh-Token", refreshToken.getToken());
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                              AuthenticationException failed)
            throws IOException, ServletException {

        String username = (String) request.getAttribute(ATTR_USERNAME);
        String reason = failed instanceof DisabledException ? "DISABLED" : "INVALID_CREDENTIALS";

        loginEventService.recordFailure(
                username,
                extractClientIp(request),
                request.getHeader("User-Agent"),
                reason);

        if (failed instanceof DisabledException) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            Map<String, Object> data = new HashMap<>();
            data.put("errorCause", "disabled");
            data.put("message", "L'utilisateur est désactivé !");
            response.getWriter().println(objectMapper.writeValueAsString(data));
            response.getWriter().flush();
        } else {
            super.unsuccessfulAuthentication(request, response, failed);
        }
    }

    private String extractClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

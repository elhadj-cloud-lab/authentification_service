package com.bestech.authentification_service.security;

import com.bestech.authentification_service.model.MyUser;
import com.bestech.authentification_service.service.refreshtoken.RefreshToken;
import com.bestech.authentification_service.service.refreshtoken.RefreshTokenService;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

public class JWTAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;
    private final RefreshTokenService refreshTokenService;

    public JWTAuthenticationFilter(AuthenticationManager authenticationManager,
                                   JwtTokenService jwtTokenService,
                                   RefreshTokenService refreshTokenService) {
        super();
        this.authenticationManager = authenticationManager;
        this.jwtTokenService = jwtTokenService;
        this.refreshTokenService = refreshTokenService;
        setFilterProcessesUrl("/login");
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException {

        MyUser user = null;
        try {
            user = new ObjectMapper().readValue(request.getInputStream(), MyUser.class);
        } catch (JsonParseException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return authenticationManager
                .authenticate(new UsernamePasswordAuthenticationToken(user.getUsername(), user.getPassword()));
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain,
                                            Authentication authResult) throws IOException, ServletException {

        org.springframework.security.core.userdetails.User springUser =
                (org.springframework.security.core.userdetails.User) authResult.getPrincipal();

        List<String> roles = new ArrayList<>();
        springUser.getAuthorities().forEach(au -> roles.add(au.getAuthority()));

        String accessToken = jwtTokenService.createAccessToken(springUser.getUsername(), roles);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(springUser.getUsername());

        response.addHeader("Authorization", accessToken);
        response.addHeader("Refresh-Token", refreshToken.getToken());
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request,
                                              HttpServletResponse response, AuthenticationException failed)
            throws IOException, ServletException {
        if (failed instanceof DisabledException) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            Map<String, Object> data = new HashMap<>();
            data.put("errorCause", "disabled");
            data.put("message", "L'utilisateur est désactivé !");
            ObjectMapper objectMapper = new ObjectMapper();
            String json = objectMapper.writeValueAsString(data);
            PrintWriter writer = response.getWriter();
            writer.println(json);
            writer.flush();
        } else {
            super.unsuccessfulAuthentication(request, response, failed);
        }
    }
}

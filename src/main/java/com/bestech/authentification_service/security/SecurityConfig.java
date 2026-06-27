package com.bestech.authentification_service.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import com.bestech.authentification_service.service.refreshtoken.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@EnableWebSecurity
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenService jwtTokenService;
    private final RefreshTokenService refreshTokenService;
    private final JWTAuthorizationFilter jwtAuthorizationFilter;

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, AuthenticationConfiguration config) throws Exception {

        AuthenticationManager authManager = config.getAuthenticationManager();

        http.sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .csrf(AbstractHttpConfigurer::disable)

                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");

                            Map<String, Object> error = new HashMap<>();
                            error.put("timestamp", LocalDateTime.now().toString());
                            error.put("status", HttpServletResponse.SC_UNAUTHORIZED);
                            error.put("error", "Unauthorized");
                            error.put("message", "Authentication required");
                            error.put("path", request.getRequestURI());

                            new ObjectMapper().writeValue(response.getOutputStream(), error);
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json");

                            Map<String, Object> error = new HashMap<>();
                            error.put("timestamp", LocalDateTime.now().toString());
                            error.put("status", HttpServletResponse.SC_FORBIDDEN);
                            error.put("error", "Forbidden");
                            error.put("message", "Access Denied");
                            error.put("path", request.getRequestURI());

                            new ObjectMapper().writeValue(response.getOutputStream(), error);
                        })
                )
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(request -> {
                    CorsConfiguration corsConfig = new CorsConfiguration();
                    corsConfig.setAllowedOrigins(Collections.singletonList("http://localhost:4200"));
                    corsConfig.setAllowedMethods(Collections.singletonList("*"));
                    corsConfig.setAllowedHeaders(Collections.singletonList("*"));
                    corsConfig.setExposedHeaders(List.of("Authorization", "Refresh-Token"));
                    return corsConfig;
                }))

                .authorizeHttpRequests(requests -> requests
                        .requestMatchers("/login", "/refresh", "/register/**", "/verifyEmail/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/admin/revoke/**").hasAuthority("ADMIN")
                        .requestMatchers("/actuator/health", "/actuator/info", "/actuator/**").permitAll()
                        //.requestMatchers("/users/actuator/health", "/users/actuator/info").permitAll()
                        .requestMatchers("/all").hasAuthority("ADMIN")
                        .anyRequest().authenticated()
                )

                .addFilterBefore(new JWTAuthenticationFilter(authManager, jwtTokenService, refreshTokenService),
                        UsernamePasswordAuthenticationFilter.class)

                .addFilterBefore(jwtAuthorizationFilter,
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}

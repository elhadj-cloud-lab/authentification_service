package com.bestech.authentification_service.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@EnableWebSecurity
@Configuration
public class SecurityConfig {

    @Autowired
    AuthenticationManager  authenticationManager ;

    @Bean
    public SecurityFilterChain filterChain (HttpSecurity http) throws Exception
    {
        http.sessionManagement( session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .csrf(AbstractHttpConfigurer::disable)
                .exceptionHandling(ex -> ex
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
                .cors(cors -> cors.configurationSource( request -> {
                    CorsConfiguration corsConfig  = new CorsConfiguration();
                    corsConfig .setAllowedOrigins(Collections.singletonList("http://localhost:4200"));
                    corsConfig .setAllowedMethods(Collections.singletonList("*"));
                    corsConfig .setAllowedHeaders(Collections.singletonList("*"));
                    corsConfig .setExposedHeaders(Collections.singletonList("Authorization"));
                        return corsConfig ;
                }))

                .authorizeHttpRequests( requests -> requests
                        .requestMatchers("/login","/register/**","/verifyEmail/**").permitAll()
                        .requestMatchers("/all").hasAuthority("ADMIN")
                        .anyRequest().authenticated() )

                .addFilterBefore(new JWTAuthenticationFilter(authenticationManager),
                        UsernamePasswordAuthenticationFilter.class)

                .addFilterBefore(new JWTAuthorizationFilter(),UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}

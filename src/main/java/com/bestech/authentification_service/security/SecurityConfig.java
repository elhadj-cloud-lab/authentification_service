package com.bestech.authentification_service.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

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

                .csrf( csrf -> csrf.disable())

                .authorizeHttpRequests( requests -> requests
                        .requestMatchers("/login").permitAll()
                        .anyRequest().authenticated() )

                .addFilterBefore(new JWTAuthenticationFilter(authenticationManager),
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}

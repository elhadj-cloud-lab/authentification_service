package com.bestech.authentification_service.controller;

import com.bestech.authentification_service.util.TestJwtHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    void refresh_withMissingToken_returns4xx() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("refreshToken", "invalid-token"));

        mockMvc.perform(post("/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void logout_withValidAccessToken_returns200() throws Exception {
        String accessToken = TestJwtHelper.adminToken();
        String body = objectMapper.writeValueAsString(Map.of("refreshToken", ""));

        mockMvc.perform(post("/logout")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    @Test
    void logout_withoutToken_returns401() throws Exception {
        // logout requires authentication (not in permitAll list)
        String body = objectMapper.writeValueAsString(Map.of("refreshToken", ""));
        mockMvc.perform(post("/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void revokeUserSessions_withoutAdminToken_returns401or403() throws Exception {
        mockMvc.perform(post("/admin/revoke/someuser"))
                .andExpect(result ->
                        org.assertj.core.api.Assertions.assertThat(
                                result.getResponse().getStatus()
                        ).isIn(401, 403));
    }

    @Test
    void revokeUserSessions_withAdminToken_returns200() throws Exception {
        mockMvc.perform(post("/admin/revoke/admin")
                        .header("Authorization", "Bearer " + TestJwtHelper.adminToken()))
                .andExpect(status().isOk());
    }

    @Test
    void login_withInvalidCredentials_returns401() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "username", "nobody",
                "password", "wrong"
        ));

        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void login_withValidAdminCredentials_returnsTokenHeaders() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "username", "admin",
                "password", "admin1234"
        ));

        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String auth = result.getResponse().getHeader("Authorization");
                    org.assertj.core.api.Assertions.assertThat(auth).isNotBlank();
                });
    }
}

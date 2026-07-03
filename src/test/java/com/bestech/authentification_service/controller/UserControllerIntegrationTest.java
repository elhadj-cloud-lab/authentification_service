package com.bestech.authentification_service.controller;

import com.bestech.authentification_service.util.EmailSender;
import com.bestech.authentification_service.util.TestJwtHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    // Empêche l'envoi de vrais emails pendant les tests
    @MockBean EmailSender emailSender;

    private String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@test.com";
    }

    @Test
    void register_withValidRequest_returns200() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "username", "testuser-" + UUID.randomUUID(),
                "password", "pass123",
                "email", uniqueEmail()
        ));

        mockMvc.perform(post("/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    @Test
    void register_withDuplicateEmail_returns400() throws Exception {
        String email = uniqueEmail();
        String body1 = objectMapper.writeValueAsString(Map.of(
                "username", "user1-" + UUID.randomUUID(),
                "password", "pass",
                "email", email
        ));
        String body2 = objectMapper.writeValueAsString(Map.of(
                "username", "user2-" + UUID.randomUUID(),
                "password", "pass",
                "email", email
        ));

        mockMvc.perform(post("/register").contentType(MediaType.APPLICATION_JSON).content(body1));
        mockMvc.perform(post("/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body2))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("USER_EMAIL_ALREADY_EXISTS"));
    }

    @Test
    void verifyEmail_withInvalidCode_returns404() throws Exception {
        mockMvc.perform(get("/verifyEmail/000000"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("INVALID_TOKEN"));
    }

    @Test
    void findAllUsers_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/all"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void findAllUsers_withAdminToken_returns200() throws Exception {
        mockMvc.perform(get("/all")
                        .header("Authorization", "Bearer " + TestJwtHelper.adminToken()))
                .andExpect(status().isOk());
    }

    @Test
    void findAllUsers_withUserToken_returns403() throws Exception {
        mockMvc.perform(get("/all")
                        .header("Authorization", "Bearer " + TestJwtHelper.userToken()))
                .andExpect(status().isForbidden());
    }
}

package com.bestech.authentification_service.service.exceptions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.WebRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @Mock WebRequest webRequest;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        when(webRequest.getDescription(false)).thenReturn("uri=/test");
    }

    @Test
    void handleEmailAlreadyExists_returnsBadRequest() {
        ResponseEntity<ErrorDetails> response = handler.handleEmailAlreadyExistsException(
                new EmailAlreadyExistsException("Email déjà existant"), webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getErrorCode()).isEqualTo("USER_EMAIL_ALREADY_EXISTS");
        assertThat(response.getBody().getMessage()).isEqualTo("Email déjà existant");
        assertThat(response.getBody().getTimestamp()).isNotNull();
        assertThat(response.getBody().getPath()).isEqualTo("uri=/test");
    }

    @Test
    void handleExpiredToken_returnsBadRequest() {
        ResponseEntity<ErrorDetails> response = handler.handleExpiredTokenException(
                new ExpiredTokenException("Token expiré"), webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getErrorCode()).isEqualTo("EXPIRED_TOKEN");
        assertThat(response.getBody().getMessage()).isEqualTo("Token expiré");
    }

    @Test
    void handleInvalidToken_returnsNotFound() {
        ResponseEntity<ErrorDetails> response = handler.handleInvalidTokenException(
                new InvalidTokenException("Token invalide"), webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getErrorCode()).isEqualTo("INVALID_TOKEN");
        assertThat(response.getBody().getMessage()).isEqualTo("Token invalide");
    }
}

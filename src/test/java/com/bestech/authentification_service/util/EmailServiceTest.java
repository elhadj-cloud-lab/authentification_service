package com.bestech.authentification_service.util;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock JavaMailSender mailSender;

    @InjectMocks EmailService emailService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "fromEmail", "test@example.com");
    }

    @Test
    void sendEmail_createsAndSendsMimeMessage() {
        // MimeMessageHelper needs a real MimeMessage to set content (requires a Session)
        MimeMessage realMimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(realMimeMessage);

        emailService.sendEmail("user@test.com", "<h1>Your code</h1>");

        verify(mailSender).createMimeMessage();
        verify(mailSender).send(realMimeMessage);
    }

    @Test
    void sendEmail_throwsIllegalStateException_onMailSenderError() {
        when(mailSender.createMimeMessage()).thenThrow(new RuntimeException("SMTP unavailable"));

        assertThatThrownBy(() -> emailService.sendEmail("user@test.com", "body"))
                .isInstanceOf(RuntimeException.class);
    }
}

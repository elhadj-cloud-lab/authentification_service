package com.bestech.authentification_service.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "login_events", indexes = {
        @Index(name = "idx_login_events_username", columnList = "username"),
        @Index(name = "idx_login_events_event_time", columnList = "event_time")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(nullable = false)
    private boolean success;

    @Column(name = "failure_reason", length = 100)
    private String failureReason;

    @Column(name = "event_time", nullable = false)
    private LocalDateTime eventTime;
}

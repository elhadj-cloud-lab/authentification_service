package com.bestech.authentification_service.dto;

public record RecentEventDto(
        Long id,
        String username,
        String ipAddress,
        boolean success,
        String failureReason,
        String eventTime
) {
}

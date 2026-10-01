package com.bestech.authentification_service.dto;

public record DailyStatsDto(String day, long total, long successes, long failures) {
}

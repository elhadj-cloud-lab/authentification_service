package com.bestech.authentification_service.dto;

public record HourlyStatsDto(int hour, long total, long successes, long failures) {
}

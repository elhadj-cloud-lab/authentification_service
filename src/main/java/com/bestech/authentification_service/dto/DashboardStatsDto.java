package com.bestech.authentification_service.dto;

import java.util.List;

public record DashboardStatsDto(
        long totalLoginsToday,
        long successfulLoginsToday,
        long failedLoginsToday,
        double successRateToday,
        long totalUsers,
        List<HourlyStatsDto> hourlyStats,
        List<DailyStatsDto> dailyStats,
        List<RecentEventDto> recentEvents
) {
}

package com.bestech.authentification_service.service;

import com.bestech.authentification_service.dto.DailyStatsDto;
import com.bestech.authentification_service.dto.DashboardStatsDto;
import com.bestech.authentification_service.dto.HourlyStatsDto;
import com.bestech.authentification_service.dto.RecentEventDto;
import com.bestech.authentification_service.model.LoginEvent;
import com.bestech.authentification_service.repository.LoginEventRepository;
import com.bestech.authentification_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LoginEventService {

    private final LoginEventRepository loginEventRepository;
    private final UserRepository userRepository;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public void recordSuccess(String username, String ipAddress, String userAgent) {
        loginEventRepository.save(LoginEvent.builder()
                .username(username)
                .ipAddress(ipAddress)
                .userAgent(truncate(userAgent, 500))
                .success(true)
                .eventTime(LocalDateTime.now())
                .build());
    }

    public void recordFailure(String username, String ipAddress, String userAgent, String reason) {
        loginEventRepository.save(LoginEvent.builder()
                .username(username != null ? username : "unknown")
                .ipAddress(ipAddress)
                .userAgent(truncate(userAgent, 500))
                .success(false)
                .failureReason(reason)
                .eventTime(LocalDateTime.now())
                .build());
    }

    public DashboardStatsDto getDashboardStats() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime sevenDaysAgo = LocalDate.now().minusDays(6).atStartOfDay();

        long total = loginEventRepository.countByEventTimeAfter(startOfDay);
        long successful = loginEventRepository.countBySuccessAndEventTimeAfter(true, startOfDay);
        long failed = loginEventRepository.countBySuccessAndEventTimeAfter(false, startOfDay);
        double rate = total > 0 ? Math.round((successful * 100.0 / total) * 10.0) / 10.0 : 0.0;
        long totalUsers = userRepository.count();

        List<HourlyStatsDto> hourly = mapHourlyStats(loginEventRepository.getHourlyStats(startOfDay));
        List<DailyStatsDto> daily = mapDailyStats(loginEventRepository.getDailyStats(sevenDaysAgo));
        List<RecentEventDto> recent = mapRecentEvents(loginEventRepository.findTop20ByOrderByEventTimeDesc());

        return new DashboardStatsDto(total, successful, failed, rate, totalUsers, hourly, daily, recent);
    }

    private List<HourlyStatsDto> mapHourlyStats(List<Object[]> rows) {
        List<HourlyStatsDto> result = new ArrayList<>();
        for (Object[] row : rows) {
            result.add(new HourlyStatsDto(
                    ((Number) row[0]).intValue(),
                    ((Number) row[1]).longValue(),
                    ((Number) row[2]).longValue(),
                    ((Number) row[3]).longValue()
            ));
        }
        return result;
    }

    private List<DailyStatsDto> mapDailyStats(List<Object[]> rows) {
        List<DailyStatsDto> result = new ArrayList<>();
        for (Object[] row : rows) {
            result.add(new DailyStatsDto(
                    (String) row[0],
                    ((Number) row[1]).longValue(),
                    ((Number) row[2]).longValue(),
                    ((Number) row[3]).longValue()
            ));
        }
        return result;
    }

    private List<RecentEventDto> mapRecentEvents(List<LoginEvent> events) {
        return events.stream()
                .map(e -> new RecentEventDto(
                        e.getId(),
                        e.getUsername(),
                        e.getIpAddress(),
                        e.isSuccess(),
                        e.getFailureReason(),
                        e.getEventTime().format(FORMATTER)
                ))
                .toList();
    }

    private String truncate(String value, int maxLength) {
        if (value == null) return null;
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }
}

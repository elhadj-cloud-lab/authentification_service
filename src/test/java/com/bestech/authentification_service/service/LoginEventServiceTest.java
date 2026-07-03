package com.bestech.authentification_service.service;

import com.bestech.authentification_service.dto.DashboardStatsDto;
import com.bestech.authentification_service.model.LoginEvent;
import com.bestech.authentification_service.repository.LoginEventRepository;
import com.bestech.authentification_service.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginEventServiceTest {

    @Mock LoginEventRepository loginEventRepository;
    @Mock UserRepository userRepository;

    @InjectMocks LoginEventService loginEventService;

    @Test
    void recordSuccess_savesEventWithSuccessTrue() {
        loginEventService.recordSuccess("alice", "127.0.0.1", "TestBrowser/1.0");

        ArgumentCaptor<LoginEvent> captor = ArgumentCaptor.forClass(LoginEvent.class);
        verify(loginEventRepository).save(captor.capture());

        LoginEvent saved = captor.getValue();
        assertThat(saved.isSuccess()).isTrue();
        assertThat(saved.getUsername()).isEqualTo("alice");
        assertThat(saved.getIpAddress()).isEqualTo("127.0.0.1");
    }

    @Test
    void recordFailure_savesEventWithSuccessFalse() {
        loginEventService.recordFailure("bob", "10.0.0.1", "Bot/2.0", "INVALID_CREDENTIALS");

        ArgumentCaptor<LoginEvent> captor = ArgumentCaptor.forClass(LoginEvent.class);
        verify(loginEventRepository).save(captor.capture());

        LoginEvent saved = captor.getValue();
        assertThat(saved.isSuccess()).isFalse();
        assertThat(saved.getFailureReason()).isEqualTo("INVALID_CREDENTIALS");
    }

    @Test
    void recordFailure_withNullUsername_storesUnknown() {
        loginEventService.recordFailure(null, "1.2.3.4", "agent", "DISABLED");

        ArgumentCaptor<LoginEvent> captor = ArgumentCaptor.forClass(LoginEvent.class);
        verify(loginEventRepository).save(captor.capture());
        assertThat(captor.getValue().getUsername()).isEqualTo("unknown");
    }

    @Test
    void recordSuccess_truncatesUserAgentOver500Chars() {
        String longAgent = "X".repeat(600);
        loginEventService.recordSuccess("alice", "127.0.0.1", longAgent);

        ArgumentCaptor<LoginEvent> captor = ArgumentCaptor.forClass(LoginEvent.class);
        verify(loginEventRepository).save(captor.capture());
        assertThat(captor.getValue().getUserAgent()).hasSize(500);
    }

    @Test
    void recordSuccess_nullUserAgent_isStoredAsNull() {
        loginEventService.recordSuccess("alice", "127.0.0.1", null);

        ArgumentCaptor<LoginEvent> captor = ArgumentCaptor.forClass(LoginEvent.class);
        verify(loginEventRepository).save(captor.capture());
        assertThat(captor.getValue().getUserAgent()).isNull();
    }

    @Test
    void getDashboardStats_aggregatesDataCorrectly() {
        when(loginEventRepository.countByEventTimeAfter(any(LocalDateTime.class))).thenReturn(20L);
        when(loginEventRepository.countBySuccessAndEventTimeAfter(eq(true), any())).thenReturn(15L);
        when(loginEventRepository.countBySuccessAndEventTimeAfter(eq(false), any())).thenReturn(5L);
        when(userRepository.count()).thenReturn(42L);
        when(loginEventRepository.getHourlyStats(any())).thenReturn(List.of());
        when(loginEventRepository.getDailyStats(any())).thenReturn(List.of());
        when(loginEventRepository.findTop20ByOrderByEventTimeDesc()).thenReturn(List.of());

        DashboardStatsDto stats = loginEventService.getDashboardStats();

        assertThat(stats.totalLoginsToday()).isEqualTo(20L);
        assertThat(stats.successfulLoginsToday()).isEqualTo(15L);
        assertThat(stats.failedLoginsToday()).isEqualTo(5L);
        assertThat(stats.successRateToday()).isEqualTo(75.0);
        assertThat(stats.totalUsers()).isEqualTo(42L);
        assertThat(stats.hourlyStats()).isEmpty();
        assertThat(stats.dailyStats()).isEmpty();
        assertThat(stats.recentEvents()).isEmpty();
    }

    @Test
    void getDashboardStats_successRateIsZeroWhenNoLogins() {
        when(loginEventRepository.countByEventTimeAfter(any())).thenReturn(0L);
        when(loginEventRepository.countBySuccessAndEventTimeAfter(eq(true), any())).thenReturn(0L);
        when(loginEventRepository.countBySuccessAndEventTimeAfter(eq(false), any())).thenReturn(0L);
        when(userRepository.count()).thenReturn(0L);
        when(loginEventRepository.getHourlyStats(any())).thenReturn(List.of());
        when(loginEventRepository.getDailyStats(any())).thenReturn(List.of());
        when(loginEventRepository.findTop20ByOrderByEventTimeDesc()).thenReturn(List.of());

        DashboardStatsDto stats = loginEventService.getDashboardStats();

        assertThat(stats.successRateToday()).isEqualTo(0.0);
    }

    @Test
    void getDashboardStats_mapsRecentEvents() {
        LoginEvent event = LoginEvent.builder()
                .username("alice")
                .ipAddress("127.0.0.1")
                .success(true)
                .eventTime(LocalDateTime.now())
                .build();

        when(loginEventRepository.countByEventTimeAfter(any())).thenReturn(1L);
        when(loginEventRepository.countBySuccessAndEventTimeAfter(eq(true), any())).thenReturn(1L);
        when(loginEventRepository.countBySuccessAndEventTimeAfter(eq(false), any())).thenReturn(0L);
        when(userRepository.count()).thenReturn(1L);
        when(loginEventRepository.getHourlyStats(any())).thenReturn(List.of());
        when(loginEventRepository.getDailyStats(any())).thenReturn(List.of());
        when(loginEventRepository.findTop20ByOrderByEventTimeDesc()).thenReturn(List.of(event));

        DashboardStatsDto stats = loginEventService.getDashboardStats();

        assertThat(stats.recentEvents()).hasSize(1);
        assertThat(stats.recentEvents().get(0).username()).isEqualTo("alice");
        assertThat(stats.recentEvents().get(0).success()).isTrue();
    }

    @Test
    void getDashboardStats_mapsHourlyStats_withNonEmptyData() {
        // hour=14, total=10, success=8, failed=2
        List<Object[]> hourlyRows = new ArrayList<>();
        hourlyRows.add(new Object[]{14, 10L, 8L, 2L});

        when(loginEventRepository.countByEventTimeAfter(any())).thenReturn(10L);
        when(loginEventRepository.countBySuccessAndEventTimeAfter(eq(true), any())).thenReturn(8L);
        when(loginEventRepository.countBySuccessAndEventTimeAfter(eq(false), any())).thenReturn(2L);
        when(userRepository.count()).thenReturn(5L);
        when(loginEventRepository.getHourlyStats(any())).thenReturn(hourlyRows);
        when(loginEventRepository.getDailyStats(any())).thenReturn(List.of());
        when(loginEventRepository.findTop20ByOrderByEventTimeDesc()).thenReturn(List.of());

        DashboardStatsDto stats = loginEventService.getDashboardStats();

        assertThat(stats.hourlyStats()).hasSize(1);
        assertThat(stats.hourlyStats().get(0).hour()).isEqualTo(14);
        assertThat(stats.hourlyStats().get(0).total()).isEqualTo(10L);
        assertThat(stats.hourlyStats().get(0).successes()).isEqualTo(8L);
        assertThat(stats.hourlyStats().get(0).failures()).isEqualTo(2L);
    }

    @Test
    void getDashboardStats_mapsDailyStats_withNonEmptyData() {
        List<Object[]> dailyRows = new ArrayList<>();
        dailyRows.add(new Object[]{"2026-07-01", 20L, 15L, 5L});

        when(loginEventRepository.countByEventTimeAfter(any())).thenReturn(20L);
        when(loginEventRepository.countBySuccessAndEventTimeAfter(eq(true), any())).thenReturn(15L);
        when(loginEventRepository.countBySuccessAndEventTimeAfter(eq(false), any())).thenReturn(5L);
        when(userRepository.count()).thenReturn(10L);
        when(loginEventRepository.getHourlyStats(any())).thenReturn(List.of());
        when(loginEventRepository.getDailyStats(any())).thenReturn(dailyRows);
        when(loginEventRepository.findTop20ByOrderByEventTimeDesc()).thenReturn(List.of());

        DashboardStatsDto stats = loginEventService.getDashboardStats();

        assertThat(stats.dailyStats()).hasSize(1);
        assertThat(stats.dailyStats().get(0).day()).isEqualTo("2026-07-01");
        assertThat(stats.dailyStats().get(0).total()).isEqualTo(20L);
        assertThat(stats.dailyStats().get(0).successes()).isEqualTo(15L);
        assertThat(stats.dailyStats().get(0).failures()).isEqualTo(5L);
    }

    @Test
    void recordSuccess_withShortUserAgent_isStoredAsIs() {
        String agent = "Chrome/121";
        loginEventService.recordSuccess("alice", "127.0.0.1", agent);

        ArgumentCaptor<LoginEvent> captor = ArgumentCaptor.forClass(LoginEvent.class);
        verify(loginEventRepository).save(captor.capture());
        assertThat(captor.getValue().getUserAgent()).isEqualTo(agent);
    }
}

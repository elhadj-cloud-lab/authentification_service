package com.bestech.authentification_service.repository;

import com.bestech.authentification_service.model.LoginEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LoginEventRepository extends JpaRepository<LoginEvent, Long> {

    long countByEventTimeAfter(LocalDateTime since);

    long countBySuccessAndEventTimeAfter(boolean success, LocalDateTime since);

    List<LoginEvent> findTop20ByOrderByEventTimeDesc();

    @Query(value = """
            SELECT EXTRACT(HOUR FROM event_time)::int AS hour,
                   COUNT(*)                            AS total,
                   COUNT(*) FILTER (WHERE success = true)  AS successes,
                   COUNT(*) FILTER (WHERE success = false) AS failures
            FROM login_events
            WHERE event_time >= :since
            GROUP BY EXTRACT(HOUR FROM event_time)
            ORDER BY hour
            """, nativeQuery = true)
    List<Object[]> getHourlyStats(@Param("since") LocalDateTime since);

    @Query(value = """
            SELECT DATE(event_time)::text              AS day,
                   COUNT(*)                            AS total,
                   COUNT(*) FILTER (WHERE success = true)  AS successes,
                   COUNT(*) FILTER (WHERE success = false) AS failures
            FROM login_events
            WHERE event_time >= :since
            GROUP BY DATE(event_time)
            ORDER BY day
            """, nativeQuery = true)
    List<Object[]> getDailyStats(@Param("since") LocalDateTime since);
}

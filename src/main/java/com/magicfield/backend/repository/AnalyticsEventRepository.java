package com.magicfield.backend.repository;

import com.magicfield.backend.entity.AnalyticsEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AnalyticsEventRepository extends JpaRepository<AnalyticsEvent, UUID> {

    Optional<AnalyticsEvent> findTopByClientIdOrderByCreatedAtDesc(UUID clientId);

    @Query("SELECT COUNT(e) FROM AnalyticsEvent e WHERE e.createdAt >= :since")
    long countPageViewsSince(@Param("since") LocalDateTime since);

    @Query("SELECT COUNT(DISTINCT e.sessionId) FROM AnalyticsEvent e WHERE e.createdAt >= :since")
    long countSessionsSince(@Param("since") LocalDateTime since);

    // sessionId + cantidad de pageviews de esa sesión (para calcular bounce rate en Java)
    @Query(value = "SELECT session_id, COUNT(*) FROM analytics_events WHERE created_at >= :since GROUP BY session_id",
           nativeQuery = true)
    List<Object[]> findSessionPageviewCounts(@Param("since") LocalDateTime since);

    @Query(value = "SELECT path, COUNT(*) as cnt FROM analytics_events WHERE created_at >= :since " +
                   "GROUP BY path ORDER BY cnt DESC LIMIT 10", nativeQuery = true)
    List<Object[]> findTopPaths(@Param("since") LocalDateTime since);

    @Query(value = "SELECT COALESCE(NULLIF(referrer, ''), 'Directo') as ref, COUNT(*) as cnt " +
                   "FROM analytics_events WHERE created_at >= :since GROUP BY ref ORDER BY cnt DESC LIMIT 10",
           nativeQuery = true)
    List<Object[]> findTopReferrers(@Param("since") LocalDateTime since);

    @Query(value = "SELECT COALESCE(NULLIF(country, ''), 'Desconocido') as c, COUNT(*) as cnt " +
                   "FROM analytics_events WHERE created_at >= :since GROUP BY c ORDER BY cnt DESC LIMIT 10",
           nativeQuery = true)
    List<Object[]> findTopCountries(@Param("since") LocalDateTime since);

    @Query(value = "SELECT device_type, COUNT(*) as cnt FROM analytics_events WHERE created_at >= :since " +
                   "GROUP BY device_type ORDER BY cnt DESC", nativeQuery = true)
    List<Object[]> findDeviceBreakdown(@Param("since") LocalDateTime since);

    @Query(value = "SELECT browser, COUNT(*) as cnt FROM analytics_events WHERE created_at >= :since " +
                   "GROUP BY browser ORDER BY cnt DESC LIMIT 5", nativeQuery = true)
    List<Object[]> findBrowserBreakdown(@Param("since") LocalDateTime since);

    @Query(value = "SELECT os, COUNT(*) as cnt FROM analytics_events WHERE created_at >= :since " +
                   "GROUP BY os ORDER BY cnt DESC LIMIT 5", nativeQuery = true)
    List<Object[]> findOsBreakdown(@Param("since") LocalDateTime since);

    // Housekeeping: no acumular eventos para siempre
    void deleteByCreatedAtBefore(LocalDateTime cutoff);
}

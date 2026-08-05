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

    // Páginas del front (todo lo que NO sea el detalle de un producto): sin límite, se muestran todas.
    @Query(value = "SELECT path, COUNT(*) as cnt FROM analytics_events " +
                   "WHERE created_at >= :since AND path !~ '^/products/[0-9a-fA-F-]{36}$' " +
                   "GROUP BY path ORDER BY cnt DESC", nativeQuery = true)
    List<Object[]> findTopFrontPages(@Param("since") LocalDateTime since);

    // Detalle de producto: solo el top 10 (se resuelve nombre + imagen en el service).
    @Query(value = "SELECT path, COUNT(*) as cnt FROM analytics_events " +
                   "WHERE created_at >= :since AND path ~ '^/products/[0-9a-fA-F-]{36}$' " +
                   "GROUP BY path ORDER BY cnt DESC LIMIT 10", nativeQuery = true)
    List<Object[]> findTopProductPaths(@Param("since") LocalDateTime since);

    // Un solo row por sesión (el primero, por created_at/id): así fuentes de tráfico, país,
    // dispositivo, navegador y SO reflejan el acceso inicial y no cada redirección interna.
    @Query(value = "SELECT referrer, country, device_type, browser, os FROM (" +
                   "  SELECT referrer, country, device_type, browser, os, " +
                   "         ROW_NUMBER() OVER (PARTITION BY session_id ORDER BY created_at ASC, id ASC) AS rn " +
                   "  FROM analytics_events WHERE created_at >= :since" +
                   ") first_events WHERE rn = 1", nativeQuery = true)
    List<Object[]> findFirstEventOfEachSessionSince(@Param("since") LocalDateTime since);

    // Housekeeping: no acumular eventos para siempre
    void deleteByCreatedAtBefore(LocalDateTime cutoff);
}

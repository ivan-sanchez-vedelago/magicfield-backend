package com.magicfield.backend.service;

import com.magicfield.backend.dto.AnalyticsEventRequest;
import com.magicfield.backend.dto.SiteAnalyticsDTO;
import com.magicfield.backend.entity.AnalyticsEvent;
import com.magicfield.backend.repository.AnalyticsEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Analítica web propia (reemplaza a Umami): el frontend manda un evento por
 * pageview real (desde su middleware, server-side, sin ningún script en el
 * navegador del usuario) y acá lo guardamos + agregamos para el dashboard.
 */
@Service
public class AnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsService.class);

    // Si el último evento del mismo visitante fue hace más de esto, es una sesión nueva.
    private static final long SESSION_GAP_MINUTES = 30;

    // Cuánto tiempo conservamos eventos crudos antes de purgarlos.
    private static final long RETENTION_DAYS = 400;

    private final AnalyticsEventRepository repository;

    public AnalyticsService(AnalyticsEventRepository repository) {
        this.repository = repository;
    }

    public void recordEvent(AnalyticsEventRequest request) {
        AnalyticsEvent event = new AnalyticsEvent();
        event.setClientId(request.getClientId());
        event.setPath(request.getPath());
        event.setReferrer(request.getReferrer());
        event.setCountry(normalizeCountry(request.getCountry()));
        event.setDeviceType(detectDeviceType(request.getUserAgent()));
        event.setBrowser(detectBrowser(request.getUserAgent()));
        event.setOs(detectOs(request.getUserAgent()));
        event.setSessionId(resolveSessionId(request.getClientId(), event.getCreatedAt()));
        repository.save(event);
    }

    private UUID resolveSessionId(UUID clientId, LocalDateTime now) {
        return repository.findTopByClientIdOrderByCreatedAtDesc(clientId)
                .filter(last -> last.getCreatedAt().plusMinutes(SESSION_GAP_MINUTES).isAfter(now))
                .map(AnalyticsEvent::getSessionId)
                .orElseGet(UUID::randomUUID);
    }

    public SiteAnalyticsDTO getAnalytics(String period) {
        LocalDateTime since = LocalDateTime.now().minusSeconds(periodToSeconds(period));

        SiteAnalyticsDTO dto = new SiteAnalyticsDTO();
        dto.setAvailable(true);
        dto.setPageViews(repository.countPageViewsSince(since));

        List<Object[]> sessionCounts = repository.findSessionPageviewCounts(since);
        long totalSessions = sessionCounts.size();
        long bounces = sessionCounts.stream().filter(row -> ((Number) row[1]).longValue() == 1).count();
        dto.setSessions(totalSessions);
        dto.setBounceRate(totalSessions > 0 ? (double) bounces / totalSessions * 100 : 0);

        dto.setTopPages(toMetricItems(repository.findTopPaths(since)));
        dto.setReferrers(toMetricItems(repository.findTopReferrers(since)));
        dto.setCountries(toMetricItems(repository.findTopCountries(since)));
        dto.setDevices(toMetricItems(repository.findDeviceBreakdown(since)));
        dto.setBrowsers(toMetricItems(repository.findBrowserBreakdown(since)));
        dto.setOperatingSystems(toMetricItems(repository.findOsBreakdown(since)));

        return dto;
    }

    private List<SiteAnalyticsDTO.MetricItem> toMetricItems(List<Object[]> rows) {
        List<SiteAnalyticsDTO.MetricItem> items = new ArrayList<>();
        for (Object[] row : rows) {
            String x = String.valueOf(row[0]);
            int y = ((Number) row[1]).intValue();
            items.add(new SiteAnalyticsDTO.MetricItem(x, y));
        }
        return items;
    }

    private long periodToSeconds(String period) {
        return switch (period == null ? "" : period) {
            case "1day"    -> 86_400L;
            case "30days"  -> 2_592_000L;
            case "365days" -> 31_536_000L;
            default        -> 604_800L; // 7days
        };
    }

    private String normalizeCountry(String country) {
        if (country == null || country.isBlank() || country.length() != 2) return null;
        return country.toUpperCase(Locale.ROOT);
    }

    // --- Parsing simple de User-Agent (sin dependencias externas) ---

    private static final Pattern TABLET_PATTERN = Pattern.compile("iPad|Tablet|(?i)android(?!.*mobile)");
    private static final Pattern MOBILE_PATTERN = Pattern.compile("Mobi|iPhone|iPod|Android");

    private String detectDeviceType(String ua) {
        if (ua == null || ua.isBlank()) return "Desconocido";
        if (TABLET_PATTERN.matcher(ua).find()) return "Tablet";
        if (MOBILE_PATTERN.matcher(ua).find()) return "Mobile";
        return "Desktop";
    }

    private String detectBrowser(String ua) {
        if (ua == null || ua.isBlank()) return "Desconocido";
        if (ua.contains("Edg/")) return "Edge";
        if (ua.contains("OPR/") || ua.contains("Opera")) return "Opera";
        if (ua.contains("SamsungBrowser")) return "Samsung Internet";
        if (ua.contains("FBAN") || ua.contains("FBAV") || ua.contains("Instagram")) return "In-app Browser";
        if (ua.contains("CriOS")) return "Chrome (iOS)";
        if (ua.contains("Chrome/") && !ua.contains("Chromium")) return "Chrome";
        if (ua.contains("Firefox/")) return "Firefox";
        if (ua.contains("Safari/") && ua.contains("Version/")) return "Safari";
        return "Otro";
    }

    private String detectOs(String ua) {
        if (ua == null || ua.isBlank()) return "Desconocido";
        if (ua.contains("Android")) return "Android";
        if (ua.contains("iPhone") || ua.contains("iPad") || ua.contains("iPod")) return "iOS";
        if (ua.contains("Windows")) return "Windows";
        if (ua.contains("Mac OS X") || ua.contains("Macintosh")) return "macOS";
        if (ua.contains("Linux")) return "Linux";
        return "Otro";
    }

    // Purga eventos viejos para no crecer indefinidamente (todos los días a las 4 AM).
    @Scheduled(cron = "0 0 4 * * *")
    public void purgeOldEvents() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(RETENTION_DAYS);
        try {
            repository.deleteByCreatedAtBefore(cutoff);
        } catch (Exception e) {
            log.error("[Analytics] Error al purgar eventos viejos", e);
        }
    }
}

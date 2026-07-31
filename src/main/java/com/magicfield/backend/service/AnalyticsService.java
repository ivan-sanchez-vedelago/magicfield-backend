package com.magicfield.backend.service;

import com.magicfield.backend.dto.AnalyticsEventRequest;
import com.magicfield.backend.dto.SiteAnalyticsDTO;
import com.magicfield.backend.entity.AnalyticsEvent;
import com.magicfield.backend.repository.AnalyticsEventRepository;
import com.magicfield.backend.repository.CategoryRepository;
import com.magicfield.backend.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public AnalyticsService(AnalyticsEventRepository repository,
                             ProductRepository productRepository,
                             CategoryRepository categoryRepository) {
        this.repository = repository;
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
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

        dto.setTopPages(toHumanizedTopPages(repository.findTopPaths(since)));
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

    // --- "Páginas más visitadas": traduce URLs crudas a descripciones en lenguaje natural ---
    // Solo se resuelve sobre el top 10 ya agrupado (no en cada evento), así no le pega a la
    // base de productos/categorías en cada pageview, solo cuando se abre el dashboard.

    private static final Map<String, String> STATIC_PAGE_LABELS = Map.ofEntries(
            Map.entry("/", "Inicio"),
            Map.entry("/cart", "Carrito"),
            Map.entry("/checkout", "Checkout"),
            Map.entry("/checkout/success", "Compra exitosa"),
            Map.entry("/auth", "Autenticación"),
            Map.entry("/auth/login", "Login"),
            Map.entry("/auth/register", "Registro"),
            Map.entry("/perfil", "Perfil")
    );

    private static final Pattern PRODUCT_DETAIL_PATTERN =
            Pattern.compile("^/products/([0-9a-fA-F-]{36})$");

    private List<SiteAnalyticsDTO.MetricItem> toHumanizedTopPages(List<Object[]> rows) {
        List<SiteAnalyticsDTO.MetricItem> items = new ArrayList<>();
        for (Object[] row : rows) {
            String rawPath = String.valueOf(row[0]);
            int count = ((Number) row[1]).intValue();
            items.add(new SiteAnalyticsDTO.MetricItem(humanizePath(rawPath), count));
        }
        return items;
    }

    private String humanizePath(String rawPath) {
        String path = rawPath;
        String query = null;
        int qIdx = rawPath.indexOf('?');
        if (qIdx >= 0) {
            path = rawPath.substring(0, qIdx);
            query = rawPath.substring(qIdx + 1);
        }

        Matcher productMatch = PRODUCT_DETAIL_PATTERN.matcher(path);
        if (productMatch.matches()) {
            try {
                UUID productId = UUID.fromString(productMatch.group(1));
                return productRepository.findById(productId)
                        .map(p -> "Producto: " + p.getName())
                        .orElse("Producto (eliminado)");
            } catch (IllegalArgumentException e) {
                return rawPath;
            }
        }

        if ("/products".equals(path)) {
            Map<String, String> params = parseQuery(query);
            String category = params.get("category");
            if (category != null && !category.isBlank()) {
                String categoryName = categoryRepository.findByShortName(category)
                        .map(c -> c.getName())
                        .orElse(category);
                return "Productos: " + categoryName;
            }
            if (params.containsKey("search")) {
                return "Búsqueda de productos";
            }
            return "Catálogo de productos";
        }

        return STATIC_PAGE_LABELS.getOrDefault(path, path);
    }

    private Map<String, String> parseQuery(String query) {
        Map<String, String> params = new LinkedHashMap<>();
        if (query == null || query.isBlank()) return params;
        for (String pair : query.split("&")) {
            if (pair.isBlank()) continue;
            int eq = pair.indexOf('=');
            String key = eq >= 0 ? pair.substring(0, eq) : pair;
            String value = eq >= 0 ? pair.substring(eq + 1) : "";
            params.put(decode(key), decode(value));
        }
        return params;
    }

    private String decode(String value) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            return value;
        }
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

package com.magicfield.backend.service;

import com.magicfield.backend.dto.UmamiAnalyticsDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class UmamiAnalyticsService {

    private final RestTemplate restTemplate;

    @Value("${umami.api.url:https://api.umami.is/v1}")
    private String apiUrl;

    @Value("${umami.api.key:}")
    private String apiKey;

    @Value("${umami.website.id:}")
    private String websiteId;

    private final Map<String, UmamiAnalyticsDTO> cache = new HashMap<>();
    private final Map<String, LocalDateTime> cacheTime = new HashMap<>();
    private static final long CACHE_MINUTES = 5;

    public UmamiAnalyticsService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public UmamiAnalyticsDTO getAnalytics(String period) {
        if (apiKey == null || apiKey.isBlank() || websiteId == null || websiteId.isBlank()) {
            return emptyAnalytics();
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lastFetch = cacheTime.get(period);
        if (lastFetch != null && lastFetch.isAfter(now.minusMinutes(CACHE_MINUTES))) {
            return cache.get(period);
        }
        UmamiAnalyticsDTO result = fetchAnalytics(period);
        cache.put(period, result);
        cacheTime.put(period, now);
        return result;
    }

    private UmamiAnalyticsDTO fetchAnalytics(String period) {
        try {
            long endAt = System.currentTimeMillis();
            long startAt = endAt - periodToMillis(period);
            UmamiAnalyticsDTO dto = new UmamiAnalyticsDTO();
            fetchStats(dto, startAt, endAt);
            dto.setTopPages(fetchMetrics("url", startAt, endAt));
            dto.setReferrers(fetchMetrics("referrer", startAt, endAt));
            dto.setCountries(fetchMetrics("country", startAt, endAt));
            dto.setBrowsers(fetchMetrics("browser", startAt, endAt));
            dto.setDevices(fetchMetrics("device", startAt, endAt));
            dto.setOperatingSystems(fetchMetrics("os", startAt, endAt));
            return dto;
        } catch (Exception e) {
            e.printStackTrace();
            return emptyAnalytics();
        }
    }

    private void fetchStats(UmamiAnalyticsDTO dto, long startAt, long endAt) {
        try {
            String url = UriComponentsBuilder
                .fromHttpUrl(apiUrl + "/websites/" + websiteId + "/stats")
                .queryParam("startAt", startAt)
                .queryParam("endAt", endAt)
                .toUriString();
            HttpHeaders headers = new HttpHeaders();
            headers.set("x-umami-api-key", apiKey);
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            if (response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                dto.setPageViews(extractLong(body, "pageviews"));
                dto.setSessions(extractLong(body, "sessions"));
                long bounces = extractLong(body, "bounces");
                long sessions = dto.getSessions();
                dto.setBounceRate(sessions > 0 ? (double) bounces / sessions * 100 : 0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private List<UmamiAnalyticsDTO.MetricItem> fetchMetrics(String type, long startAt, long endAt) {
        try {
            String url = UriComponentsBuilder
                .fromHttpUrl(apiUrl + "/websites/" + websiteId + "/metrics")
                .queryParam("type", type)
                .queryParam("startAt", startAt)
                .queryParam("endAt", endAt)
                .toUriString();
            HttpHeaders headers = new HttpHeaders();
            headers.set("x-umami-api-key", apiKey);
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                url, HttpMethod.GET, entity,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );
            if (response.getBody() != null) {
                List<UmamiAnalyticsDTO.MetricItem> items = new ArrayList<>();
                for (Map<String, Object> item : response.getBody()) {
                    String x = item.get("x") != null ? String.valueOf(item.get("x")) : "Desconocido";
                    int y = item.get("y") != null ? ((Number) item.get("y")).intValue() : 0;
                    items.add(new UmamiAnalyticsDTO.MetricItem(x, y));
                }
                return items;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
    }

    private long periodToMillis(String period) {
        return switch (period) {
            case "1day"    -> 86_400_000L;
            case "30days"  -> 2_592_000_000L;
            case "365days" -> 31_536_000_000L;
            default        -> 604_800_000L; // 7days
        };
    }

    private long extractLong(Map<String, Object> body, String key) {
        Object val = body.get(key);
        if (val instanceof Map) {
            Object inner = ((Map<?, ?>) val).get("value");
            if (inner instanceof Number) return ((Number) inner).longValue();
        } else if (val instanceof Number) {
            return ((Number) val).longValue();
        }
        return 0L;
    }

    private UmamiAnalyticsDTO emptyAnalytics() {
        UmamiAnalyticsDTO dto = new UmamiAnalyticsDTO();
        dto.setPageViews(0);
        dto.setSessions(0);
        dto.setBounceRate(0);
        dto.setTopPages(Collections.emptyList());
        dto.setReferrers(Collections.emptyList());
        dto.setCountries(Collections.emptyList());
        dto.setBrowsers(Collections.emptyList());
        dto.setDevices(Collections.emptyList());
        dto.setOperatingSystems(Collections.emptyList());
        return dto;
    }
}

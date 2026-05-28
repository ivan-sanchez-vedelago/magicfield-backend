package com.magicfield.backend.service;

import com.magicfield.backend.dto.VercelAnalyticsDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class VercelAnalyticsService {

    private final RestTemplate restTemplate;

    @Value("${vercel.api.token:}")
    private String vercelApiToken;

    @Value("${vercel.project.id:}")
    private String vercelProjectId;

    private Map<String, VercelAnalyticsDTO> cachedData = new HashMap<>();
    private Map<String, LocalDateTime> lastUpdate = new HashMap<>();

    private static final long CACHE_DURATION_MINUTES = 5;
    private static final String BASE_URL = "https://api.vercel.com/v1/analytics";

    public VercelAnalyticsService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public VercelAnalyticsDTO getAnalytics(String period) {
        if (!isValidPeriod(period)) {
            throw new IllegalArgumentException("Período inválido. Use: 1day, 7days, 30days, 365days");
        }

        if (isCacheValid(period)) {
            return cachedData.get(period);
        }

        return fetchAnalytics(period);
    }

    private boolean isValidPeriod(String period) {
        return period.matches("^(1day|7days|30days|365days)$");
    }

    private boolean isCacheValid(String period) {
        if (!cachedData.containsKey(period) || !lastUpdate.containsKey(period)) {
            return false;
        }

        LocalDateTime lastUpdateTime = lastUpdate.get(period);
        return lastUpdateTime.isAfter(LocalDateTime.now().minusMinutes(CACHE_DURATION_MINUTES));
    }

    private VercelAnalyticsDTO fetchAnalytics(String period) {
        try {
            if (vercelApiToken == null || vercelApiToken.isEmpty()) {
                System.out.println("Vercel API token not configured");
                return createEmptyAnalytics();
            }

            String timePeriod = convertPeriodToTimeRange(period);

            VercelAnalyticsDTO analytics = new VercelAnalyticsDTO();

            // Get general stats
            getGeneralStats(analytics, timePeriod);

            // Get page views
            getPageViews(analytics, timePeriod);

            // Get referrers
            getReferrers(analytics, timePeriod);

            // Get countries
            getCountries(analytics, timePeriod);

            // Get devices
            getDevices(analytics, timePeriod);

            // Get browsers
            getBrowsers(analytics, timePeriod);

            // Get OS
            getOperatingSystems(analytics, timePeriod);

            cachedData.put(period, analytics);
            lastUpdate.put(period, LocalDateTime.now());

            return analytics;
        } catch (Exception e) {
            e.printStackTrace();
            // Return cached data if available, otherwise empty
            if (cachedData.containsKey(period)) {
                return cachedData.get(period);
            }
            return createEmptyAnalytics();
        }
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + vercelApiToken);
        headers.set("Content-Type", "application/json");
        return headers;
    }

    private void getGeneralStats(VercelAnalyticsDTO analytics, String timePeriod) {
        try {
            String url = String.format("%s/timeseries?projectId=%s&limit=1&granularity=1d&%s",
                    BASE_URL, vercelProjectId, timePeriod);

            HttpEntity<String> entity = new HttpEntity<>(createHeaders());
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

            if (response.getBody() != null && response.getBody().containsKey("data")) {
                List<Map> data = (List<Map>) response.getBody().get("data");
                if (!data.isEmpty()) {
                    Map latestData = data.get(0);

                    Object visitors = latestData.get("visitors");
                    Object pageViews = latestData.get("pageviews");

                    if (visitors != null) {
                        analytics.setTotalVisitors(((Number) visitors).longValue());
                    }
                    if (pageViews != null) {
                        analytics.setPageViews(((Number) pageViews).longValue());
                    }

                    Object bounceRate = latestData.get("bounceRate");
                    if (bounceRate != null) {
                        analytics.setBounceRate(Double.parseDouble(bounceRate.toString()));
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Error fetching general stats: " + e.getMessage());
        }
    }

    private void getPageViews(VercelAnalyticsDTO analytics, String timePeriod) {
        try {
            String url = String.format("%s/top-paths?projectId=%s&limit=10&%s",
                    BASE_URL, vercelProjectId, timePeriod);

            HttpEntity<String> entity = new HttpEntity<>(createHeaders());
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

            if (response.getBody() != null && response.getBody().containsKey("data")) {
                List<Map> data = (List<Map>) response.getBody().get("data");

                List<VercelAnalyticsDTO.PageViewDto> pages = data.stream()
                        .map(item -> new VercelAnalyticsDTO.PageViewDto(
                                (String) item.get("path"),
                                ((Number) item.getOrDefault("visitors", 0)).longValue()
                        ))
                        .collect(Collectors.toList());

                analytics.setTopPages(pages);
            } else {
                analytics.setTopPages(Collections.emptyList());
            }
        } catch (Exception e) {
            System.out.println("Error fetching page views: " + e.getMessage());
            analytics.setTopPages(Collections.emptyList());
        }
    }

    private void getReferrers(VercelAnalyticsDTO analytics, String timePeriod) {
        try {
            String url = String.format("%s/top-referrers?projectId=%s&limit=10&%s",
                    BASE_URL, vercelProjectId, timePeriod);

            HttpEntity<String> entity = new HttpEntity<>(createHeaders());
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

            if (response.getBody() != null && response.getBody().containsKey("data")) {
                List<Map> data = (List<Map>) response.getBody().get("data");

                List<VercelAnalyticsDTO.ReferrerDto> referrers = data.stream()
                        .map(item -> new VercelAnalyticsDTO.ReferrerDto(
                                (String) item.get("referrer"),
                                ((Number) item.getOrDefault("count", 0)).longValue()
                        ))
                        .collect(Collectors.toList());

                analytics.setTopReferrers(referrers);
            } else {
                analytics.setTopReferrers(Collections.emptyList());
            }
        } catch (Exception e) {
            System.out.println("Error fetching referrers: " + e.getMessage());
            analytics.setTopReferrers(Collections.emptyList());
        }
    }

    private void getCountries(VercelAnalyticsDTO analytics, String timePeriod) {
        try {
            String url = String.format("%s/top-countries?projectId=%s&limit=10&%s",
                    BASE_URL, vercelProjectId, timePeriod);

            HttpEntity<String> entity = new HttpEntity<>(createHeaders());
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

            if (response.getBody() != null && response.getBody().containsKey("data")) {
                List<Map> data = (List<Map>) response.getBody().get("data");

                List<VercelAnalyticsDTO.CountryDto> countries = data.stream()
                        .map(item -> new VercelAnalyticsDTO.CountryDto(
                                (String) item.get("country"),
                                (String) item.get("countryCode"),
                                ((Number) item.getOrDefault("visitors", 0)).longValue()
                        ))
                        .collect(Collectors.toList());

                analytics.setTopCountries(countries);
            } else {
                analytics.setTopCountries(Collections.emptyList());
            }
        } catch (Exception e) {
            System.out.println("Error fetching countries: " + e.getMessage());
            analytics.setTopCountries(Collections.emptyList());
        }
    }

    private void getDevices(VercelAnalyticsDTO analytics, String timePeriod) {
        try {
            String url = String.format("%s/devices?projectId=%s&%s",
                    BASE_URL, vercelProjectId, timePeriod);

            HttpEntity<String> entity = new HttpEntity<>(createHeaders());
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

            if (response.getBody() != null && response.getBody().containsKey("data")) {
                Map deviceData = (Map) response.getBody().get("data");

                List<VercelAnalyticsDTO.DeviceDto> devices = deviceData.entrySet().stream()
                        .map(entry -> new VercelAnalyticsDTO.DeviceDto(
                                entry.getKey().toString(),
                                ((Number) entry.getValue()).longValue()
                        ))
                        .sorted(Comparator.comparingLong(VercelAnalyticsDTO.DeviceDto::getCount).reversed())
                        .collect(Collectors.toList());

                analytics.setDevices(devices);
            } else {
                analytics.setDevices(Collections.emptyList());
            }
        } catch (Exception e) {
            System.out.println("Error fetching devices: " + e.getMessage());
            analytics.setDevices(Collections.emptyList());
        }
    }

    private void getBrowsers(VercelAnalyticsDTO analytics, String timePeriod) {
        try {
            String url = String.format("%s/browsers?projectId=%s&%s",
                    BASE_URL, vercelProjectId, timePeriod);

            HttpEntity<String> entity = new HttpEntity<>(createHeaders());
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

            if (response.getBody() != null && response.getBody().containsKey("data")) {
                Map browserData = (Map) response.getBody().get("data");

                List<VercelAnalyticsDTO.BrowserDto> browsers = browserData.entrySet().stream()
                        .map(entry -> new VercelAnalyticsDTO.BrowserDto(
                                entry.getKey().toString(),
                                ((Number) entry.getValue()).longValue()
                        ))
                        .sorted(Comparator.comparingLong(VercelAnalyticsDTO.BrowserDto::getCount).reversed())
                        .collect(Collectors.toList());

                analytics.setBrowsers(browsers);
            } else {
                analytics.setBrowsers(Collections.emptyList());
            }
        } catch (Exception e) {
            System.out.println("Error fetching browsers: " + e.getMessage());
            analytics.setBrowsers(Collections.emptyList());
        }
    }

    private void getOperatingSystems(VercelAnalyticsDTO analytics, String timePeriod) {
        try {
            String url = String.format("%s/os?projectId=%s&%s",
                    BASE_URL, vercelProjectId, timePeriod);

            HttpEntity<String> entity = new HttpEntity<>(createHeaders());
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

            if (response.getBody() != null && response.getBody().containsKey("data")) {
                Map osData = (Map) response.getBody().get("data");

                List<VercelAnalyticsDTO.OSDto> systems = osData.entrySet().stream()
                        .map(entry -> new VercelAnalyticsDTO.OSDto(
                                entry.getKey().toString(),
                                ((Number) entry.getValue()).longValue()
                        ))
                        .sorted(Comparator.comparingLong(VercelAnalyticsDTO.OSDto::getCount).reversed())
                        .collect(Collectors.toList());

                analytics.setOperatingSystems(systems);
            } else {
                analytics.setOperatingSystems(Collections.emptyList());
            }
        } catch (Exception e) {
            System.out.println("Error fetching OS: " + e.getMessage());
            analytics.setOperatingSystems(Collections.emptyList());
        }
    }

    private String convertPeriodToTimeRange(String period) {
        long now = System.currentTimeMillis() / 1000;
        switch (period) {
            case "1day":
                return "since=" + (now - 86400);
            case "7days":
                return "since=" + (now - (7 * 86400));
            case "30days":
                return "since=" + (now - (30 * 86400));
            case "365days":
                return "since=" + (now - (365 * 86400));
            default:
                return "since=" + (now - (7 * 86400));
        }
    }

    private VercelAnalyticsDTO createEmptyAnalytics() {
        VercelAnalyticsDTO analytics = new VercelAnalyticsDTO();
        analytics.setTotalVisitors(0);
        analytics.setPageViews(0);
        analytics.setBounceRate(0);
        analytics.setTopPages(Collections.emptyList());
        analytics.setTopReferrers(Collections.emptyList());
        analytics.setTopCountries(Collections.emptyList());
        analytics.setDevices(Collections.emptyList());
        analytics.setBrowsers(Collections.emptyList());
        analytics.setOperatingSystems(Collections.emptyList());
        return analytics;
    }

    @Scheduled(fixedRate = 1000 * 60 * 5) // cada 5 minutos
    public void refreshCache() {
        cachedData.clear();
        lastUpdate.clear();
    }
}

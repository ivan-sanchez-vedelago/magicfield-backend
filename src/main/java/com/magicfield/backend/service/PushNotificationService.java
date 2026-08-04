package com.magicfield.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.magicfield.backend.entity.PushDeviceToken;
import com.magicfield.backend.repository.PushDeviceTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class PushNotificationService {

    private static final Logger log = LoggerFactory.getLogger(PushNotificationService.class);
    private static final String EXPO_PUSH_URL = "https://exp.host/--/api/v2/push/send";

    private final PushDeviceTokenRepository pushDeviceTokenRepository;
    private final HttpClient client = HttpClient.newHttpClient();
    private static final ObjectMapper mapper = new ObjectMapper();

    public PushNotificationService(PushDeviceTokenRepository pushDeviceTokenRepository) {
        this.pushDeviceTokenRepository = pushDeviceTokenRepository;
    }

    public void registerToken(String token, String platform) {
        PushDeviceToken deviceToken = pushDeviceTokenRepository.findByToken(token)
                .orElse(new PushDeviceToken(token, platform));
        deviceToken.setPlatform(platform);
        deviceToken.setLastSeenAt(LocalDateTime.now());
        pushDeviceTokenRepository.save(deviceToken);
        log.info("[PushNotificationService] Token registrado (platform={}): {}", platform, token);
    }

    public void notifyNewOrder(String title, String body) {
        List<PushDeviceToken> tokens = pushDeviceTokenRepository.findAll();
        log.info("[PushNotificationService] notifyNewOrder: {} token(s) registrados", tokens.size());
        if (tokens.isEmpty()) {
            return;
        }

        List<Map<String, Object>> messages = tokens.stream()
                .map(t -> Map.<String, Object>of(
                        "to", t.getToken(),
                        "title", title,
                        "body", body,
                        "sound", "default",
                        "priority", "high"
                ))
                .toList();

        try {
            String json = mapper.writeValueAsString(messages);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(EXPO_PUSH_URL))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            log.info("[PushNotificationService] Respuesta Expo ({} tokens): {}", tokens.size(), response.body());

            if (response.statusCode() >= 400) {
                log.error("[PushNotificationService] Error enviando push: {}", response.body());
                return;
            }

            processTickets(tokens, response.body());
        } catch (Exception e) {
            log.error("[PushNotificationService] Error enviando push: {}", e.getMessage());
        }
    }

    /** Loguea el resultado por token y da de baja los que Expo reporta como no registrados. */
    private void processTickets(List<PushDeviceToken> tokens, String responseBody) {
        try {
            JsonNode data = mapper.readTree(responseBody).get("data");
            if (data == null || !data.isArray() || data.size() != tokens.size()) return;

            for (int i = 0; i < tokens.size(); i++) {
                JsonNode ticket = data.get(i);
                String status = ticket.path("status").asText();
                if ("error".equals(status)) {
                    String errorCode = ticket.path("details").path("error").asText();
                    log.error("[PushNotificationService] Ticket con error para token={}: {} ({})",
                            tokens.get(i).getToken(), errorCode, ticket.path("message").asText());
                    if ("DeviceNotRegistered".equals(errorCode)) {
                        pushDeviceTokenRepository.delete(tokens.get(i));
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[PushNotificationService] No se pudo parsear respuesta de Expo: {}", e.getMessage());
        }
    }
}

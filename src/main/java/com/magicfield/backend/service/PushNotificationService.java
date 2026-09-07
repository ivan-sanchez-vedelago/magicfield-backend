package com.magicfield.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.magicfield.backend.entity.PushDeviceToken;
import com.magicfield.backend.repository.PushDeviceTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

    /** Nombre + precio unitario de un ítem del pedido, usado para armar el resumen de productos. */
    public record OrderItemSummary(String productName, BigDecimal unitPrice) {
    }

    public void notifyNewOrder(String title, String body) {
        notifyNewOrder(title, body, null, List.of());
    }

    public void notifyNewOrder(String title, String body, UUID orderId, List<OrderItemSummary> items) {
        List<PushDeviceToken> tokens = pushDeviceTokenRepository.findAll();
        log.info("[PushNotificationService] notifyNewOrder: {} token(s) registrados", tokens.size());
        if (tokens.isEmpty()) {
            return;
        }

        // Expo (exp.host) no ofrece una API soportada de "resumen colapsado / detalle al
        // expandir" para push remoto sin eyectar a un módulo nativo (ej. Notifee) -- por eso
        // toda la info (nombres de producto + primeros 3 por precio desc) va directo en el
        // body, en vez de depender de un gesto de expandir la notificación.
        String fullBody = body + productsSummaryLine(items);

        List<Map<String, Object>> messages = tokens.stream()
                .map(t -> {
                    Map<String, Object> message = new LinkedHashMap<>();
                    message.put("to", t.getToken());
                    message.put("title", title);
                    message.put("body", fullBody);
                    message.put("sound", "default");
                    message.put("priority", "high");
                    if (orderId != null) {
                        message.put("data", Map.of("orderId", orderId.toString()));
                    }
                    return message;
                })
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

    /**
     * "\nProductos: A, B, C..." con los primeros 3 nombres ordenados por precio unitario
     * descendente, y "..." al final si hay más de 3 ítems en el pedido. Vacío si no hay ítems.
     */
    private static String productsSummaryLine(List<OrderItemSummary> items) {
        if (items == null || items.isEmpty()) return "";
        List<String> topNames = items.stream()
                .sorted(Comparator.comparing(OrderItemSummary::unitPrice).reversed())
                .limit(3)
                .map(OrderItemSummary::productName)
                .toList();
        String suffix = items.size() > 3 ? "..." : "";
        return "\nProductos: " + String.join(", ", topNames) + suffix;
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

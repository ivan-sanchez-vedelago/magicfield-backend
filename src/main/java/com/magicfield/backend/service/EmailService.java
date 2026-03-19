package com.magicfield.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

@Service
public class EmailService {

    @Value("${resend.api-key}")
    private String apiKey;

    @Value("${app.email-from}")
    private String fromEmail;

    private final HttpClient client = HttpClient.newHttpClient();
    private static final ObjectMapper mapper = new ObjectMapper();

    public void send(String to, String subject, String text) {
        try {
            if (apiKey == null || apiKey.isBlank()) {
                throw new IllegalStateException("RESEND_API_KEY no está configurada");
            }

            Map<String, Object> body = Map.of(
                "from", fromEmail,
                "to", new String[]{to},
                "subject", subject,
                "text", text
            );
            String json = mapper.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.resend.com/emails"))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = client.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            System.out.println("RESEND STATUS: " + response.statusCode());
            System.out.println("RESEND BODY: " + response.body());

            if (response.statusCode() >= 400) {
                throw new RuntimeException("Error enviando email: " + response.body());
            }

        } catch (Exception e) {
            throw new RuntimeException("Error enviando email", e);
        }
    }
}
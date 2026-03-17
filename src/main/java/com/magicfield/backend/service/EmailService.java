package com.magicfield.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
public class EmailService {

    @Value("${resend.api-key}")
    private String apiKey;

    private final HttpClient client = HttpClient.newHttpClient();

    public void send(String to, String subject, String text) {
        try {

            if (apiKey == null || apiKey.isBlank()) {
                throw new IllegalStateException("RESEND_API_KEY no está configurada");
            }

            String json = """
            {
              "from": "Magic Field <onboarding@resend.dev>",
              "to": ["%s"],
              "subject": "%s",
              "text": "%s"
            }
            """.formatted(
                    to,
                    subject,
                    text.replace("\"", "\\\"")
            );

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

            if (response.statusCode() >= 400) {
                throw new RuntimeException("Error enviando email: " + response.body());
            }

        } catch (Exception e) {
            throw new RuntimeException("Error enviando email", e);
        }
    }
}
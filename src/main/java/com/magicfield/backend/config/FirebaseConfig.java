package com.magicfield.backend.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;

@Configuration
public class FirebaseConfig {

    @Value("${FIREBASE_SERVICE_ACCOUNT_JSON:}")
    private String firebaseJson;

    @Value("${firebase.bucket}")
    private String bucketName;

    @PostConstruct
    public void init() {
        if (FirebaseApp.getApps().isEmpty()) {
            try {

                if (firebaseJson == null || firebaseJson.isBlank()) {
                    throw new RuntimeException("Firebase JSON not configured");
                }

                InputStream serviceAccount =
                        new ByteArrayInputStream(firebaseJson.getBytes(StandardCharsets.UTF_8));

                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .setStorageBucket(bucketName)
                        .build();

                FirebaseApp.initializeApp(options);

            } catch (Exception e) {
                throw new RuntimeException("Firebase init failed", e);
            }
        }
    }
}

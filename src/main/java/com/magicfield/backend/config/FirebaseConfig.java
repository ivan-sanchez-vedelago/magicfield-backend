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

    private static final Logger logger = LoggerFactory.getLogger(FirebaseConfig.class);

    @Value("${firebase.service.account.path:}")
    private String serviceAccountPath;

    @Value("${firebase.bucket}")
    private String bucketName;

    @PostConstruct
    public void init() {
        if (FirebaseApp.getApps().isEmpty()) {
            try {
                if (serviceAccountPath == null || serviceAccountPath.isBlank()) {
                    // TODO: Provide a service account JSON via environment variable `FIREBASE_SERVICE_ACCOUNT_PATH`.
                    logger.warn("No Firebase service account configured. Firebase token verification will fail until configured.");
                    return;
                }

                FileInputStream serviceAccount = new FileInputStream(serviceAccountPath);
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .setStorageBucket(bucketName)
                        .build();
                FirebaseApp.initializeApp(options);
                logger.info("Firebase initialized from {}", serviceAccountPath);
            } catch (IOException e) {
                logger.error("Failed to initialize Firebase", e);
            }
        }
    }
}

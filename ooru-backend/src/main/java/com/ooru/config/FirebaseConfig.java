package com.ooru.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Initializes Firebase on startup IF a real service account file is present at the configured
 * path. If it isn't (the common case until you've done the Firebase Console setup yourself),
 * this logs a warning and moves on rather than crashing the whole app — NotificationService
 * checks FirebaseApp.getApps() before trying to send anything, so bookings still work fine
 * without notifications configured.
 */
@Component
public class FirebaseConfig {

    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

    @Value("${ooru.firebase.service-account-path}")
    private String serviceAccountPath;

    @PostConstruct
    public void init() {
        Path path = Path.of(serviceAccountPath);
        if (!Files.exists(path)) {
            log.warn("No Firebase service account found at '{}' — push notifications are disabled. " +
                     "See README for how to set this up.", serviceAccountPath);
            return;
        }
        try (FileInputStream serviceAccount = new FileInputStream(path.toFile())) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                log.info("Firebase initialized — push notifications are active.");
            }
        } catch (IOException e) {
            log.error("Found a Firebase service account file but couldn't load it — push notifications are disabled.", e);
        }
    }
}

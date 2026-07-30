package com.magicfield.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "analytics_events", indexes = {
    @Index(name = "idx_analytics_created_at", columnList = "created_at"),
    @Index(name = "idx_analytics_client_id_created_at", columnList = "client_id, created_at")
})
public class AnalyticsEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(nullable = false, columnDefinition = "text")
    private String path;

    @Column(columnDefinition = "text")
    private String referrer;

    @Column(length = 2)
    private String country;

    @Column(name = "device_type", nullable = false)
    private String deviceType;

    @Column(nullable = false)
    private String browser;

    @Column(nullable = false)
    private String os;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public AnalyticsEvent() {
        this.createdAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getClientId() { return clientId; }
    public void setClientId(UUID clientId) { this.clientId = clientId; }

    public UUID getSessionId() { return sessionId; }
    public void setSessionId(UUID sessionId) { this.sessionId = sessionId; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public String getReferrer() { return referrer; }
    public void setReferrer(String referrer) { this.referrer = referrer; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getDeviceType() { return deviceType; }
    public void setDeviceType(String deviceType) { this.deviceType = deviceType; }

    public String getBrowser() { return browser; }
    public void setBrowser(String browser) { this.browser = browser; }

    public String getOs() { return os; }
    public void setOs(String os) { this.os = os; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

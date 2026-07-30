package com.magicfield.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class AnalyticsEventRequest {

    @NotNull(message = "clientId es obligatorio")
    private UUID clientId;

    @NotBlank(message = "path es obligatorio")
    private String path;

    private String referrer;
    private String country;
    private String userAgent;

    public UUID getClientId() { return clientId; }
    public void setClientId(UUID clientId) { this.clientId = clientId; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public String getReferrer() { return referrer; }
    public void setReferrer(String referrer) { this.referrer = referrer; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
}

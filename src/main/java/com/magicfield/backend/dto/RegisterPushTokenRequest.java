package com.magicfield.backend.dto;

import jakarta.validation.constraints.NotBlank;

public class RegisterPushTokenRequest {

    @NotBlank
    private String token;

    private String platform;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }
}

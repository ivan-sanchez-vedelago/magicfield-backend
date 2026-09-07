package com.magicfield.backend.dto;

import jakarta.validation.constraints.NotBlank;

public class BannerRequest {

    @NotBlank(message = "El título es obligatorio")
    private String title;

    private String subtitle;

    private boolean active = true;

    public BannerRequest() {}

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSubtitle() { return subtitle; }
    public void setSubtitle(String subtitle) { this.subtitle = subtitle; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}

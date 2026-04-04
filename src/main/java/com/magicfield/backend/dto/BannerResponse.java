package com.magicfield.backend.dto;

import com.magicfield.backend.entity.Banner;
import java.time.LocalDateTime;

public class BannerResponse {

    private Long id;
    private String title;
    private String subtitle;
    private String imageUrl;
    private boolean active;
    private int sortOrder;
    private LocalDateTime createdAt;

    public BannerResponse() {}

    public static BannerResponse fromEntity(Banner banner) {
        BannerResponse dto = new BannerResponse();
        dto.id = banner.getId();
        dto.title = banner.getTitle();
        dto.subtitle = banner.getSubtitle();
        dto.imageUrl = banner.getImageUrl();
        dto.active = banner.isActive();
        dto.sortOrder = banner.getSortOrder();
        dto.createdAt = banner.getCreatedAt();
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSubtitle() { return subtitle; }
    public void setSubtitle(String subtitle) { this.subtitle = subtitle; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

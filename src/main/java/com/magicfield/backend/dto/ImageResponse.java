package com.magicfield.backend.dto;

import com.magicfield.backend.entity.Image;
import java.util.UUID;

public class ImageResponse {

    private Long id;
    private UUID productId;
    private String url;
    private Boolean isMain;

    public ImageResponse() {
    }

    public ImageResponse(Long id, UUID productId, String url, Boolean isMain) {
        this.id = id;
        this.productId = productId;
        this.url = url;
        this.isMain = isMain;
    }

    public static ImageResponse fromEntity(Image image) {
        return new ImageResponse(
                image.getId(),
                image.getProduct().getId(),
                image.getUrl(),
                image.isPrimaryImage()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UUID getProductId() {
        return productId;
    }

    public void setProductId(UUID productId) {
        this.productId = productId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Boolean getIsMain() {
        return isMain;
    }

    public void setIsMain(Boolean isMain) {
        this.isMain = isMain;
    }
}

package com.magicfield.backend.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class ProductResponse {

    private UUID id;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer stock;
    private String type;
    private String scryfallId;
    private Boolean isFoil;
    private String set;
    private String collectorNumber;
    private String condition;
    private String language;
    private Long categoryId;

    // URLs públicas (Firebase, S3, CDN, etc.)
    private List<String> imageUrls;

    public ProductResponse() {
    }

    public ProductResponse(
            UUID id,
            String name,
            String description,
            BigDecimal price,
            Integer stock,
            String type,
            String scryfallId,
            Boolean isFoil,
            String set,
            String collectorNumber,
            String condition,
            String language,
            Long categoryId,
            List<String> imageUrls
    ) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.stock = stock;
        this.type = type;
        this.scryfallId = scryfallId;
        this.isFoil = isFoil;
        this.set = set;
        this.collectorNumber = collectorNumber;
        this.condition = condition;
        this.language = language;
        this.categoryId = categoryId;
        this.imageUrls = imageUrls;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public Integer getStock() {
        return stock;
    }

    public String getType() {
        return type;
    }

    public String getScryfallId() {
        return scryfallId;
    }

    public Boolean getIsFoil() {
        return isFoil;
    }

    public String getSet() {
        return set;
    }

    public String getCollectorNumber() {
        return collectorNumber;
    }

    public String getCondition() {
        return condition;
    }

    public String getLanguage() {
        return language;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public List<String> getImageUrls() {
        return imageUrls;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setScryfallId(String scryfallId) {
        this.scryfallId = scryfallId;
    }

    public void setIsFoil(Boolean isFoil) {
        this.isFoil = isFoil;
    }

    public void setSet(String set) {
        this.set = set;
    }

    public void setCollectorNumber(String collectorNumber) {
        this.collectorNumber = collectorNumber;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls;
    }
}

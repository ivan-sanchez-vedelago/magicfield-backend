package com.magicfield.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class ProductResponse {

    private UUID id;
    private String name;
    // Nombre + tags de variante concatenados para mostrar (ej. "Lightning Bolt (Borderless)
    // (Extended Art)"). `name` se mantiene puro a propósito -- ver variantTags -- porque viaja
    // de vuelta tal cual en el request de edición y no debe llevar el sufijo pegado.
    private String displayName;
    // Labels legibles de la variante de arte/marco (ej. ["Borderless", "Extended Art"]),
    // vacío si la carta no tiene ninguna. Ver Product.variantTags y ScryfallService.VARIANT_TAG_LABELS.
    private List<String> variantTags;
    private String description;
    private BigDecimal price;
    private Integer stock;
    private String type;
    private String scryfallId;
    private Long finishId;
    private String finishShortName;
    private String finishName;
    private String set;
    private String collectorNumber;
    private Long conditionId;
    private String conditionName;
    private Long languageId;
    private String languageName;
    private Long categoryId;
    private LocalDateTime createdAt;

    // URLs públicas (Firebase, S3, CDN, etc.)
    private List<String> imageUrls;

    // Solo se llena en la respuesta de create(): true si en vez de crear una fila nueva
    // se sumó el stock pedido a un producto existente con la misma variante.
    private boolean merged;

    // Solo se llena en el catálogo público agrupado: cantidad de variantes (condición/idioma)
    // distintas que existen en stock para esta carta+finish (singles) o nombre+set (sellados).
    private Integer variantCount;

    public ProductResponse() {
    }

    public ProductResponse(
            UUID id,
            String name,
            String displayName,
            List<String> variantTags,
            String description,
            BigDecimal price,
            Integer stock,
            String type,
            String scryfallId,
            Long finishId,
            String finishShortName,
            String finishName,
            String set,
            String collectorNumber,
            Long conditionId,
            String conditionName,
            Long languageId,
            String languageName,
            Long categoryId,
            LocalDateTime createdAt,
            List<String> imageUrls
    ) {
        this.id = id;
        this.name = name;
        this.displayName = displayName;
        this.variantTags = variantTags;
        this.description = description;
        this.price = price;
        this.stock = stock;
        this.type = type;
        this.scryfallId = scryfallId;
        this.finishId = finishId;
        this.finishShortName = finishShortName;
        this.finishName = finishName;
        this.set = set;
        this.collectorNumber = collectorNumber;
        this.conditionId = conditionId;
        this.conditionName = conditionName;
        this.languageId = languageId;
        this.languageName = languageName;
        this.categoryId = categoryId;
        this.createdAt = createdAt;
        this.imageUrls = imageUrls;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<String> getVariantTags() {
        return variantTags;
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

    public Long getFinishId() {
        return finishId;
    }

    public String getFinishShortName() {
        return finishShortName;
    }

    public String getFinishName() {
        return finishName;
    }

    public String getSet() {
        return set;
    }

    public String getCollectorNumber() {
        return collectorNumber;
    }

    public Long getConditionId() {
        return conditionId;
    }

    public String getConditionName() {
        return conditionName;
    }

    public Long getLanguageId() {
        return languageId;
    }

    public String getLanguageName() {
        return languageName;
    }

    public boolean isMerged() {
        return merged;
    }

    public void setMerged(boolean merged) {
        this.merged = merged;
    }

    public Integer getVariantCount() {
        return variantCount;
    }

    public void setVariantCount(Integer variantCount) {
        this.variantCount = variantCount;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
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

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setVariantTags(List<String> variantTags) {
        this.variantTags = variantTags;
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

    public void setFinishId(Long finishId) {
        this.finishId = finishId;
    }

    public void setFinishShortName(String finishShortName) {
        this.finishShortName = finishShortName;
    }

    public void setFinishName(String finishName) {
        this.finishName = finishName;
    }

    public void setSet(String set) {
        this.set = set;
    }

    public void setCollectorNumber(String collectorNumber) {
        this.collectorNumber = collectorNumber;
    }

    public void setConditionId(Long conditionId) {
        this.conditionId = conditionId;
    }

    public void setConditionName(String conditionName) {
        this.conditionName = conditionName;
    }

    public void setLanguageId(Long languageId) {
        this.languageId = languageId;
    }

    public void setLanguageName(String languageName) {
        this.languageName = languageName;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls;
    }
}

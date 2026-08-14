package com.magicfield.backend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "products", indexes = {
    @Index(name = "idx_product_stock", columnList = "stock"),
    @Index(name = "idx_product_category_id", columnList = "category_id"),
    @Index(name = "idx_product_created_at", columnList = "created_at"),
    // Compuesto en vez de uno solo por scryfall_id: findByScryfallIdAndFinishId (variantes,
    // catálogo, listNewest) filtra siempre por los dos juntos, y por la regla del prefijo
    // izquierdo este índice también sirve a cualquier query que filtre solo por scryfall_id.
    @Index(name = "idx_product_scryfall_finish", columnList = "scryfall_id, finish_id")
})
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(nullable = false)
    private Integer stock;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime lastPriceUpdate;

    @Column
    private String scryfallId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "finish_id")
    private CardFinish finish;

    @Column
    private String set;

    @Column
    private String collectorNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "condition_id")
    private CardCondition condition;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "language_id")
    private CardLanguage language;

    // Códigos de variante de arte/marco (BORDERLESS, EXTENDED_ART, etc. -- ver
    // ScryfallService.VARIANT_TAG_LABELS), separados por coma. Calculado una sola vez al crear
    // el single: el frame de una impresión puntual de Scryfall es inmutable, nunca hace falta
    // recalcularlo como sí pasa con el precio. Null = todavía no calculado (fila vieja, pendiente
    // de backfill) o la carta no tiene ninguna variante de este tipo.
    @Column
    private String variantTags;

    @OneToMany(
        mappedBy = "product",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    private List<Image> images;

    public Product() {
        this.createdAt = LocalDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastPriceUpdate() {
        return lastPriceUpdate;
    }

    public void setLastPriceUpdate(LocalDateTime lastPriceUpdate) {
        this.lastPriceUpdate = lastPriceUpdate;
    }

    public String getScryfallId() {
        return scryfallId;
    }

    public void setScryfallId(String scryfallId) {
        this.scryfallId = scryfallId;
    }

    public CardFinish getFinish() {
        return finish;
    }

    public void setFinish(CardFinish finish) {
        this.finish = finish;
    }

    public String getSet() {
        return set;
    }

    public void setSet(String set) {
        this.set = set;
    }

    public String getCollectorNumber() {
        return collectorNumber;
    }

    public void setCollectorNumber(String collectorNumber) {
        this.collectorNumber = collectorNumber;
    }

    public CardCondition getCondition() {
        return condition;
    }

    public void setCondition(CardCondition condition) {
        this.condition = condition;
    }

    public CardLanguage getLanguage() {
        return language;
    }

    public void setLanguage(CardLanguage language) {
        this.language = language;
    }

    public List<Image> getImages() {
        return images;
    }

    public void setImages(List<Image> images) {
        this.images = images;
    }

    public String getVariantTags() {
        return variantTags;
    }

    public void setVariantTags(String variantTags) {
        this.variantTags = variantTags;
    }

    public void setVariantTags(List<String> tags) {
        this.variantTags = (tags == null || tags.isEmpty()) ? null : String.join(",", tags);
    }

    public List<String> getVariantTagsList() {
        if (variantTags == null || variantTags.isBlank()) return List.of();
        return Arrays.asList(variantTags.split(","));
    }
}
package com.magicfield.backend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "card_condition")
public class CardCondition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "short_name", nullable = false, unique = true)
    private String shortName;

    @Column(name = "long_name", nullable = false)
    private String longName;

    @Column(name = "price_multiplier", nullable = false)
    private BigDecimal priceMultiplier;

    // "SIN" o "PSL": qué categoría raíz puede usar esta condición (NM/LP/... solo para singles,
    // NEW/USD solo para sellados). Nullable a propósito, aunque hoy toda fila nueva la va a
    // traer completa: con ddl-auto:update, agregar una columna NOT NULL a una tabla que ya
    // tiene filas puede fallar el arranque completo si Postgres no puede backfillear un default
    // (ya nos pasó una vez con un índice sobre una columna mal declarada) -- se backfillea vía
    // migración de datos después del primer deploy, no vía constraint de esquema.
    @Column(name = "applicable_type")
    private String applicableType;

    public CardCondition() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public String getLongName() {
        return longName;
    }

    public void setLongName(String longName) {
        this.longName = longName;
    }

    public BigDecimal getPriceMultiplier() {
        return priceMultiplier;
    }

    public void setPriceMultiplier(BigDecimal priceMultiplier) {
        this.priceMultiplier = priceMultiplier;
    }

    public String getApplicableType() {
        return applicableType;
    }

    public void setApplicableType(String applicableType) {
        this.applicableType = applicableType;
    }
}

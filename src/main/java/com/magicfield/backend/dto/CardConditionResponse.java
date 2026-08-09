package com.magicfield.backend.dto;

import java.math.BigDecimal;

public class CardConditionResponse {

    private Long id;
    private String shortName;
    private String longName;
    private BigDecimal priceMultiplier;

    public CardConditionResponse() {
    }

    public CardConditionResponse(Long id, String shortName, String longName, BigDecimal priceMultiplier) {
        this.id = id;
        this.shortName = shortName;
        this.longName = longName;
        this.priceMultiplier = priceMultiplier;
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
}

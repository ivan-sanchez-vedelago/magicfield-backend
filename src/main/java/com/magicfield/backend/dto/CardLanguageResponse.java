package com.magicfield.backend.dto;

public class CardLanguageResponse {

    private Long id;
    private String shortName;
    private String longName;

    public CardLanguageResponse() {
    }

    public CardLanguageResponse(Long id, String shortName, String longName) {
        this.id = id;
        this.shortName = shortName;
        this.longName = longName;
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
}

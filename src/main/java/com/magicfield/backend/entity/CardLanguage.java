package com.magicfield.backend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "card_language")
public class CardLanguage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "short_name", nullable = false, unique = true)
    private String shortName;

    @Column(name = "long_name", nullable = false)
    private String longName;

    public CardLanguage() {
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

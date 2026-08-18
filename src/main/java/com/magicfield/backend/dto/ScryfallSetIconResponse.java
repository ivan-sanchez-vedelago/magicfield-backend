package com.magicfield.backend.dto;

public class ScryfallSetIconResponse {

    private String svg;

    public ScryfallSetIconResponse() {
    }

    public ScryfallSetIconResponse(String svg) {
        this.svg = svg;
    }

    public String getSvg() {
        return svg;
    }

    public void setSvg(String svg) {
        this.svg = svg;
    }
}

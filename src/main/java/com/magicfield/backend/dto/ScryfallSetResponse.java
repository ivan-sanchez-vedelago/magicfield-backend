package com.magicfield.backend.dto;

public class ScryfallSetResponse {

    private String code;
    private String name;
    private String iconSvgUri;

    public ScryfallSetResponse() {
    }

    public ScryfallSetResponse(String code, String name, String iconSvgUri) {
        this.code = code;
        this.name = name;
        this.iconSvgUri = iconSvgUri;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIconSvgUri() {
        return iconSvgUri;
    }

    public void setIconSvgUri(String iconSvgUri) {
        this.iconSvgUri = iconSvgUri;
    }
}

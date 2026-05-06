package com.magicfield.backend.dto;

public class CategoryResponse {

    private Long id;
    private String name;
    private String shortName;
    private Long parentId;

    public CategoryResponse() {
    }

    public CategoryResponse(Long id, String name, String shortName, Long parentId) {
        this.id = id;
        this.name = name;
        this.shortName = shortName;
        this.parentId = parentId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }
}

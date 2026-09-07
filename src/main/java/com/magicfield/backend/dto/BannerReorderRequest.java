package com.magicfield.backend.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class BannerReorderRequest {

    @NotEmpty(message = "La lista de ids es obligatoria")
    private List<Long> orderedIds;

    public BannerReorderRequest() {}

    public List<Long> getOrderedIds() { return orderedIds; }
    public void setOrderedIds(List<Long> orderedIds) { this.orderedIds = orderedIds; }
}

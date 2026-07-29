package com.magicfield.backend.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class AvailabilityCheckRequest {

    @NotNull(message = "Los items son obligatorios")
    @NotEmpty(message = "Debe incluir al menos un item")
    private List<CheckoutItemRequest> items;

    public List<CheckoutItemRequest> getItems() { return items; }
    public void setItems(List<CheckoutItemRequest> items) { this.items = items; }
}

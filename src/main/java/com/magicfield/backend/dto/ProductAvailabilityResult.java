package com.magicfield.backend.dto;

import java.util.UUID;

public class ProductAvailabilityResult {

    private UUID productId;
    private boolean exists;
    private String name;
    private int availableStock;
    private int requestedQuantity;
    private boolean sufficient;

    public ProductAvailabilityResult() {
    }

    public ProductAvailabilityResult(UUID productId, boolean exists, String name,
                                      int availableStock, int requestedQuantity, boolean sufficient) {
        this.productId = productId;
        this.exists = exists;
        this.name = name;
        this.availableStock = availableStock;
        this.requestedQuantity = requestedQuantity;
        this.sufficient = sufficient;
    }

    public UUID getProductId() { return productId; }
    public void setProductId(UUID productId) { this.productId = productId; }

    public boolean isExists() { return exists; }
    public void setExists(boolean exists) { this.exists = exists; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getAvailableStock() { return availableStock; }
    public void setAvailableStock(int availableStock) { this.availableStock = availableStock; }

    public int getRequestedQuantity() { return requestedQuantity; }
    public void setRequestedQuantity(int requestedQuantity) { this.requestedQuantity = requestedQuantity; }

    public boolean isSufficient() { return sufficient; }
    public void setSufficient(boolean sufficient) { this.sufficient = sufficient; }
}

package com.magicfield.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "product_creation_requests")
public class ProductCreationRequest {

    @Id
    @Column(name = "idempotency_key", length = 100, nullable = false, updatable = false)
    private String idempotencyKey;

    @Column(name = "product_id")
    private UUID productId;

    protected ProductCreationRequest() {
    }
}

package com.magicfield.backend.controller;

import com.magicfield.backend.dto.*;
import com.magicfield.backend.service.ProductTypeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/products")
public class ProductTypeController {

    private final ProductTypeService productTypeService;

    public ProductTypeController(ProductTypeService productTypeService) {
        this.productTypeService = productTypeService;
    }

    /**
     * Create a single card product
     */
    @PostMapping("/single")
    public ResponseEntity<ProductResponseWithDetails> createSingleCard(
            @Valid @RequestBody CreateSingleCardRequest request
    ) {
        ProductResponseWithDetails created = productTypeService.createSingleCard(request);
        return ResponseEntity
                .created(URI.create("/api/products/" + created.getId()))
                .body(created);
    }

    /**
     * Create a sealed product
     */
    @PostMapping("/sealed")
    public ResponseEntity<ProductResponseWithDetails> createSealedProduct(
            @Valid @RequestBody CreateSealedProductRequest request
    ) {
        ProductResponseWithDetails created = productTypeService.createSealedProduct(request);
        return ResponseEntity
                .created(URI.create("/api/products/" + created.getId()))
                .body(created);
    }

    /**
     * Create an other product
     */
    @PostMapping("/other")
    public ResponseEntity<ProductResponseWithDetails> createOtherProduct(
            @Valid @RequestBody CreateOtherProductRequest request
    ) {
        ProductResponseWithDetails created = productTypeService.createOtherProduct(request);
        return ResponseEntity
                .created(URI.create("/api/products/" + created.getId()))
                .body(created);
    }

    /**
     * Get product with details by ID
     */
    @GetMapping("/{id}/details")
    public ProductResponseWithDetails getProductDetails(@PathVariable UUID id) {
        return productTypeService.getProductWithDetails(id);
    }

    /**
     * Get all single cards
     */
    @GetMapping("/singles")
    public List<ProductResponseWithDetails> getAllSingleCards() {
        return productTypeService.getAllSingleCards();
    }

    /**
     * Get all sealed products
     */
    @GetMapping("/sealed")
    public List<ProductResponseWithDetails> getAllSealedProducts() {
        return productTypeService.getAllSealedProducts();
    }
}

package com.magicfield.backend.controller;

import com.magicfield.backend.dto.PagedProductResponse;
import com.magicfield.backend.dto.ProductRequest;
import com.magicfield.backend.dto.ProductResponse;
import com.magicfield.backend.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public List<ProductResponse> list() {
        return productService.listAll();
    }

    @GetMapping("/paged")
    public PagedProductResponse listPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "") String categories
    ) {
        int clampedSize = Math.min(Math.max(size, 1), 30);
        List<String> categoryList = categories.isBlank()
                ? List.of()
                : Arrays.asList(categories.split(","));
        return productService.listPaged(search, categoryList, page, clampedSize);
    }

    @GetMapping("/{id}")
    public ProductResponse getById(@PathVariable UUID id) {
        return productService.getById(id);
    }

    @PostMapping
    public ResponseEntity<ProductResponse> create(
            @Valid @RequestBody ProductRequest request
    ) {
        ProductResponse created = productService.create(request);
        return ResponseEntity
                .created(URI.create("/api/products/" + created.getId()))
                .body(created);
    }

    @PutMapping("/{id}")
    public ProductResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody ProductRequest request
    ) {
        return productService.update(id, request);
    }

    @PatchMapping("/{id}/stock")
    public ProductResponse updateStock(
            @PathVariable UUID id,
            @RequestParam int stock
    ) {
        return productService.updateStock(id, stock);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id
    ) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/decrease-stock")
    public ResponseEntity<Void> decreaseStock(
            @PathVariable UUID id,
            @RequestParam int quantity
    ) {
        productService.decreaseStock(id, quantity);
        return ResponseEntity.noContent().build();
    }
}
package com.magicfield.backend.service;

import com.magicfield.backend.dto.PagedProductResponse;
import com.magicfield.backend.dto.ProductRequest;
import com.magicfield.backend.dto.ProductResponse;

import java.util.List;
import java.util.UUID;

public interface ProductService {
    List<ProductResponse> listAll();

    PagedProductResponse listPaged(String search, List<String> categories, int page, int size);

    ProductResponse getById(UUID id);

    ProductResponse create(ProductRequest request);

    ProductResponse update(UUID id, ProductRequest request);

    ProductResponse updateStock(UUID id, int stock);

    void delete(UUID id);

    void decreaseStock(UUID productId, int quantity);
}

package com.magicfield.backend.service;

import com.magicfield.backend.dto.ProductRequest;
import com.magicfield.backend.dto.ProductResponse;

import java.util.List;

public interface ProductService {
    List<ProductResponse> listAll();

    ProductResponse getById(Long id);

    ProductResponse create(ProductRequest request);

    ProductResponse update(Long id, ProductRequest request);

    void delete(Long id);

    void decreaseStock(Long productId, int quantity);
}

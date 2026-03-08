package com.magicfield.backend.repository;

import com.magicfield.backend.entity.Product;
import com.magicfield.backend.entity.ProductType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {
    List<Product> findByType(ProductType type);
}

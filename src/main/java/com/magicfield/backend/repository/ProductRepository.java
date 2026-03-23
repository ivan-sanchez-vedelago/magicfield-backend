package com.magicfield.backend.repository;

import com.magicfield.backend.entity.Product;
import com.magicfield.backend.entity.ProductType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {
    List<Product> findByType(ProductType type);

    @Query("""
        SELECT p FROM Product p
        WHERE p.type = 'SINGLE'
        AND (
            p.lastPriceUpdate IS NULL OR
            p.lastPriceUpdate < :limitDate
        )
    """)
    List<Product> findSinglesNeedingUpdate(LocalDateTime limitDate);
}

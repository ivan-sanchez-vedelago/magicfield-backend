package com.magicfield.backend.repository;

import com.magicfield.backend.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    @Query("""
        SELECT p FROM Product p
        WHERE p.category.shortName = 'SIN'
        AND (
            p.lastPriceUpdate IS NULL OR
            p.lastPriceUpdate < :limitDate
        )
    """)
    List<Product> findSinglesNeedingUpdate(LocalDateTime limitDate);

    @Query(value = """
        SELECT p FROM Product p
        LEFT JOIN FETCH p.category c
        WHERE p.stock > 0
        AND (:search = '' OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%'))
             OR (p.description IS NOT NULL AND LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%'))))
        AND (:allCategories = true OR (c IS NOT NULL AND c.shortName IN :categories))
    """,
    countQuery = """
        SELECT COUNT(p) FROM Product p
        LEFT JOIN p.category c
        WHERE p.stock > 0
        AND (:search = '' OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%'))
             OR (p.description IS NOT NULL AND LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%'))))
        AND (:allCategories = true OR (c IS NOT NULL AND c.shortName IN :categories))
    """)
    Page<Product> findPaged(
        @Param("search") String search,
        @Param("categories") List<String> categories,
        @Param("allCategories") boolean allCategories,
        Pageable pageable
    );
}

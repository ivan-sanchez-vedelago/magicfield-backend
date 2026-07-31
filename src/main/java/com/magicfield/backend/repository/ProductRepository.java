package com.magicfield.backend.repository;

import com.magicfield.backend.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    Optional<Product> findByScryfallIdAndIsFoil(String scryfallId, Boolean isFoil);

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

    // Productos agotados (stock = 0) que ya no tienen ventas PENDING asociadas:
    // candidatos a restaurar desde el admin.
    // Excluye singles (SIN): sus datos/precio vienen de Scryfall y no tiene sentido restaurarlos manualmente.
    @Query(value = """
        SELECT p FROM Product p
        LEFT JOIN FETCH p.category c
        WHERE p.stock = 0
        AND (c IS NULL OR c.shortName <> 'SIN')
        AND NOT EXISTS (
            SELECT 1 FROM SalesAudit sa
            WHERE sa.productId = p.id AND sa.status = 'PENDING'
        )
        AND (:search = '' OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%'))
             OR (p.description IS NOT NULL AND LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%'))))
    """,
    countQuery = """
        SELECT COUNT(p) FROM Product p
        LEFT JOIN p.category c
        WHERE p.stock = 0
        AND (c IS NULL OR c.shortName <> 'SIN')
        AND NOT EXISTS (
            SELECT 1 FROM SalesAudit sa
            WHERE sa.productId = p.id AND sa.status = 'PENDING'
        )
        AND (:search = '' OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%'))
             OR (p.description IS NOT NULL AND LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%'))))
    """)
    Page<Product> findRestorablePaged(
        @Param("search") String search,
        Pageable pageable
    );
}

package com.magicfield.backend.repository;

import com.magicfield.backend.entity.ProductCreationRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ProductCreationRequestRepository extends JpaRepository<ProductCreationRequest, String> {

    @Modifying
    @Query(value = """
            INSERT INTO product_creation_requests (idempotency_key)
            VALUES (:idempotencyKey)
            ON CONFLICT (idempotency_key) DO NOTHING
            """, nativeQuery = true)
    int claimRequest(@Param("idempotencyKey") String idempotencyKey);

    @Modifying
    @Query(value = """
            UPDATE product_creation_requests
            SET product_id = :productId
            WHERE idempotency_key = :idempotencyKey
            """, nativeQuery = true)
    int setProductId(
            @Param("idempotencyKey") String idempotencyKey,
            @Param("productId") UUID productId
    );

    @Query(value = """
            SELECT product_id
            FROM product_creation_requests
            WHERE idempotency_key = :idempotencyKey
            """, nativeQuery = true)
    Optional<UUID> findProductIdByIdempotencyKey(@Param("idempotencyKey") String idempotencyKey);
}

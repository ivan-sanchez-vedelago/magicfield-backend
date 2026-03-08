package com.magicfield.backend.repository;

import com.magicfield.backend.entity.SealedProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface SealedProductRepository extends JpaRepository<SealedProduct, Long> {
    Optional<SealedProduct> findByProductId(UUID productId);
}

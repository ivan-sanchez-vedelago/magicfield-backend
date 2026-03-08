package com.magicfield.backend.repository;

import com.magicfield.backend.entity.OtherProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface OtherProductRepository extends JpaRepository<OtherProduct, Long> {
    Optional<OtherProduct> findByProductId(UUID productId);
}

package com.magicfield.backend.repository;

import com.magicfield.backend.entity.SingleCard;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface SingleCardRepository extends JpaRepository<SingleCard, Long> {
    Optional<SingleCard> findByProductId(UUID productId);
}

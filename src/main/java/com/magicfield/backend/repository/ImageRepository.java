package com.magicfield.backend.repository;

import com.magicfield.backend.entity.Image;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ImageRepository extends JpaRepository<Image, Long> {

    List<Image> findByProductId(UUID productId);
    List<Image> findByProductIdOrderByIdAsc(UUID productId);

    void deleteByProductId(UUID productId);
}

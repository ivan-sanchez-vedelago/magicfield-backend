package com.magicfield.backend.repository;

import com.magicfield.backend.entity.Image;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ImageRepository extends JpaRepository<Image, Long> {

    List<Image> findByProductId(Long productId);
    List<Image> findByProductIdOrderByIdAsc(Long productId);

    void deleteByProductId(Long productId);
}

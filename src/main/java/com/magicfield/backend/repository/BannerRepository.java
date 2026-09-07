package com.magicfield.backend.repository;

import com.magicfield.backend.entity.Banner;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface BannerRepository extends JpaRepository<Banner, Long> {

    List<Banner> findByActiveTrueOrderBySortOrderAsc();

    List<Banner> findAllByOrderBySortOrderAsc();

    Optional<Banner> findFirstByOrderBySortOrderDesc();
}

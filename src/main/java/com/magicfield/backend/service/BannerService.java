package com.magicfield.backend.service;

import com.magicfield.backend.dto.BannerRequest;
import com.magicfield.backend.dto.BannerResponse;
import com.magicfield.backend.entity.Banner;
import com.magicfield.backend.repository.BannerRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BannerService {

    private final BannerRepository bannerRepository;
    private final ImageStorageService imageStorageService;

    public BannerService(BannerRepository bannerRepository, ImageStorageService imageStorageService) {
        this.bannerRepository = bannerRepository;
        this.imageStorageService = imageStorageService;
    }

    public List<BannerResponse> getActiveBanners() {
        return bannerRepository.findByActiveTrueOrderBySortOrderAsc()
                .stream().map(BannerResponse::fromEntity).collect(Collectors.toList());
    }

    public List<BannerResponse> getAllBanners() {
        return bannerRepository.findAllByOrderBySortOrderAsc()
                .stream().map(BannerResponse::fromEntity).collect(Collectors.toList());
    }

    @Transactional
    public BannerResponse create(BannerRequest request) {
        Banner banner = new Banner();
        banner.setTitle(request.getTitle());
        banner.setSubtitle(request.getSubtitle());
        banner.setActive(request.isActive());
        banner.setSortOrder(request.getSortOrder());
        return BannerResponse.fromEntity(bannerRepository.save(banner));
    }

    @Transactional
    public BannerResponse update(Long id, BannerRequest request) {
        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Banner no encontrado"));
        banner.setTitle(request.getTitle());
        banner.setSubtitle(request.getSubtitle());
        banner.setActive(request.isActive());
        banner.setSortOrder(request.getSortOrder());
        return BannerResponse.fromEntity(bannerRepository.save(banner));
    }

    @Transactional
    public BannerResponse uploadImage(Long id, MultipartFile file) throws IOException {
        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Banner no encontrado"));

        // Delete old image if exists
        if (banner.getImageUrl() != null) {
            try {
                imageStorageService.deleteByUrl(banner.getImageUrl());
            } catch (Exception e) {
                System.err.println("Error deleting old banner image: " + e.getMessage());
            }
        }

        String url = imageStorageService.uploadBanner(id, file);
        banner.setImageUrl(url);
        return BannerResponse.fromEntity(bannerRepository.save(banner));
    }

    @Transactional
    public void delete(Long id) {
        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Banner no encontrado"));

        if (banner.getImageUrl() != null) {
            try {
                imageStorageService.deleteByUrl(banner.getImageUrl());
            } catch (Exception e) {
                System.err.println("Error deleting banner image: " + e.getMessage());
            }
        }

        bannerRepository.deleteById(id);
    }
}

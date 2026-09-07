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
import java.util.Map;
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
        // El orden ya no lo elige el admin a mano -- todo banner nuevo entra al final de la
        // lista. Reordenar es un paso aparte (ver reorder() más abajo).
        int nextSortOrder = bannerRepository.findFirstByOrderBySortOrderDesc()
                .map(b -> b.getSortOrder() + 1)
                .orElse(0);
        banner.setSortOrder(nextSortOrder);
        return BannerResponse.fromEntity(bannerRepository.save(banner));
    }

    @Transactional
    public BannerResponse update(Long id, BannerRequest request) {
        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Banner no encontrado"));
        banner.setTitle(request.getTitle());
        banner.setSubtitle(request.getSubtitle());
        banner.setActive(request.isActive());
        // sortOrder no se toca acá -- editar título/subtítulo/activo no debería mover la
        // posición del banner, eso solo cambia vía reorder().
        return BannerResponse.fromEntity(bannerRepository.save(banner));
    }

    // Reordenamiento explícito (confirmar el modo "editar orden" del admin): orderedIds ya
    // viene en el orden final deseado, se le asigna sortOrder = índice a cada uno. Se valida
    // el tamaño contra el total real para no perder banners si el cliente manda una lista
    // vieja/incompleta (ej. alguien borró un banner desde otra sesión mientras reordenaba).
    @Transactional
    public List<BannerResponse> reorder(List<Long> orderedIds) {
        long total = bannerRepository.count();
        if (orderedIds == null || orderedIds.size() != total) {
            throw new IllegalArgumentException(
                    "La lista de orden no coincide con la cantidad de banners existentes");
        }

        Map<Long, Banner> byId = bannerRepository.findAllById(orderedIds).stream()
                .collect(Collectors.toMap(Banner::getId, b -> b));
        if (byId.size() != orderedIds.size()) {
            throw new IllegalArgumentException("La lista de orden incluye banners inexistentes");
        }

        for (int i = 0; i < orderedIds.size(); i++) {
            Banner banner = byId.get(orderedIds.get(i));
            banner.setSortOrder(i);
            bannerRepository.save(banner);
        }

        return getAllBanners();
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

        // Reacomoda el sortOrder de los que quedan para que no haya huecos en la numeración
        // (ej. si se borró el del medio en 0,1,2,3 -> queda 0,1,3 sin este paso).
        List<Banner> remaining = bannerRepository.findAllByOrderBySortOrderAsc();
        for (int i = 0; i < remaining.size(); i++) {
            Banner remainingBanner = remaining.get(i);
            if (remainingBanner.getSortOrder() != i) {
                remainingBanner.setSortOrder(i);
                bannerRepository.save(remainingBanner);
            }
        }
    }
}

package com.magicfield.backend.controller;

import com.magicfield.backend.dto.BannerRequest;
import com.magicfield.backend.dto.BannerResponse;
import com.magicfield.backend.service.BannerService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/banners")
public class BannerController {

    private final BannerService bannerService;

    public BannerController(BannerService bannerService) {
        this.bannerService = bannerService;
    }

    /** Public: returns only active banners ordered by sortOrder */
    @GetMapping
    public List<BannerResponse> getActiveBanners() {
        return bannerService.getActiveBanners();
    }

    /** Admin: returns all banners including inactive */
    @GetMapping("/all")
    public List<BannerResponse> getAllBanners() {
        return bannerService.getAllBanners();
    }

    @PostMapping
    public ResponseEntity<BannerResponse> create(@Valid @RequestBody BannerRequest request) {
        return ResponseEntity.status(201).body(bannerService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BannerResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody BannerRequest request) {
        return ResponseEntity.ok(bannerService.update(id, request));
    }

    @PostMapping("/{id}/image")
    public ResponseEntity<BannerResponse> uploadImage(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(bannerService.uploadImage(id, file));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        bannerService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

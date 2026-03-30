package com.magicfield.backend.controller;

import com.magicfield.backend.dto.ImageResponse;
import com.magicfield.backend.entity.Image;
import com.magicfield.backend.service.ImageService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class ImageController {

    private final ImageService imageService;

    public ImageController(ImageService imageService) {
        this.imageService = imageService;
    }

    @GetMapping("/products/{productId}/images")
    public List<ImageResponse> getProductImages(@PathVariable UUID productId) {
        List<Image> images = imageService.getByProductId(productId);
        return images.stream()
                .map(ImageResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @PostMapping("/products/{productId}/images")
    public ResponseEntity<String> uploadImage(
            @PathVariable UUID productId,
            @RequestParam("file") MultipartFile file,
            Authentication authentication
    ) throws IOException {
        imageService.upload(productId, file);
        return ResponseEntity.status(201).build();
    }

    @DeleteMapping("/images/{id}")
    public ResponseEntity<Void> deleteImage(
            @PathVariable Long id
    ) {
        imageService.deleteImage(id);
        return ResponseEntity.noContent().build();
    }
}

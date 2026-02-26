package com.magicfield.backend.controller;

import com.magicfield.backend.service.ImageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

@RestController
@RequestMapping("/api/products")
public class ImageController {

    private final ImageService imageService;

    public ImageController(ImageService imageService) {
        this.imageService = imageService;
    }

    @PostMapping("/{productId}/images")
    public ResponseEntity<String> uploadImage(
            @PathVariable Long productId,
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        imageService.upload(productId, file);
        return ResponseEntity.status(201).build();
    }
}

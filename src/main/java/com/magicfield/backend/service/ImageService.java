package com.magicfield.backend.service;

import com.magicfield.backend.entity.Image;
import com.magicfield.backend.entity.Product;
import com.magicfield.backend.entity.ProductType;
import com.magicfield.backend.repository.ImageRepository;
import com.magicfield.backend.repository.ProductRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
public class ImageService {

    private final ImageRepository imageRepository;
    private final ProductRepository productRepository;
    private final ImageStorageService imageStorageService;

    public ImageService(
            ImageRepository imageRepository,
            ProductRepository productRepository,
            ImageStorageService imageStorageService
    ) {
        this.imageRepository = imageRepository;
        this.productRepository = productRepository;
        this.imageStorageService = imageStorageService;
    }

    public List<Image> getByProductId(UUID productId) {
        return imageRepository.findByProductId(productId);
    }

    @Transactional
    public void deleteImage(Long imageId) {
        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> new RuntimeException("Imagen no encontrada"));

        try {
            imageStorageService.deleteByUrl(image.getUrl());
        } catch (Exception e) {
            System.err.println("Error deleting image from storage: " + e.getMessage());
        }

        imageRepository.deleteById(imageId);
    }

    @Transactional
    public Image upload(UUID productId, MultipartFile file) throws IOException {

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        if (product.getType() == ProductType.SINGLE) {
            throw new IllegalArgumentException("Los productos SINGLE no aceptan imágenes: se obtienen automáticamente desde Scryfall");
        }

        String url = imageStorageService.upload(productId, file);

        Image image = new Image();
        image.setProduct(product);
        image.setUrl(url);
        image.setPrimaryImage(false);

        return imageRepository.save(image);
    }
}

package com.magicfield.backend.service;

import com.magicfield.backend.entity.Image;
import com.magicfield.backend.entity.Product;
import com.magicfield.backend.repository.ImageRepository;
import com.magicfield.backend.repository.ProductRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

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

    @Transactional
    public Image upload(Long productId, MultipartFile file) throws IOException {

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        String url = imageStorageService.upload(productId, file);

        Image image = new Image();
        image.setProduct(product);
        image.setUrl(url);
        image.setPrimaryImage(false);

        return imageRepository.save(image);
    }
}

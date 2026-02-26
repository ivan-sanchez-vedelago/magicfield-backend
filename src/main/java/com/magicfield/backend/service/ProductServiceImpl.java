package com.magicfield.backend.service;

import com.magicfield.backend.dto.ProductRequest;
import com.magicfield.backend.dto.ProductResponse;
import com.magicfield.backend.entity.Image;
import com.magicfield.backend.entity.Product;
import com.magicfield.backend.exception.ProductNotFoundException;
import com.magicfield.backend.service.ImageStorageService;
import com.magicfield.backend.repository.ImageRepository;
import com.magicfield.backend.repository.ProductRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ImageStorageService imageStorageService;
    private final ImageRepository imageRepository;

    public ProductServiceImpl(ProductRepository productRepository,
                              ImageStorageService imageStorageService,
                              ImageRepository imageRepository) {
        this.productRepository = productRepository;
        this.imageStorageService = imageStorageService;
        this.imageRepository = imageRepository;
    }

    @Override
    public List<ProductResponse> listAll() {
        return productRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ProductResponse getById(Long id) {
        Product p = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        return toResponse(p);
    }

    @Override
    public ProductResponse create(ProductRequest request) {
        Product p = new Product();
        p.setName(request.getName());
        p.setDescription(request.getDescription());
        p.setPrice(request.getPrice());
        p.setStock(request.getStock());

        Product saved = productRepository.save(p);
        return toResponse(saved);
    }

    @Override
    public ProductResponse update(Long id, ProductRequest request) {
        Product p = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        p.setName(request.getName());
        p.setDescription(request.getDescription());
        p.setPrice(request.getPrice());
        p.setStock(request.getStock());

        Product saved = productRepository.save(p);
        return toResponse(saved);
    }

    @Override
    public void delete(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ProductNotFoundException(id);
        }
        productRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void decreaseStock(Long productId, int quantity) {

        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() ->
                        new RuntimeException("Product not found with id " + productId)
                );

        int currentStock = product.getStock();

        if (currentStock < quantity) {
            throw new IllegalStateException("Insufficient stock");
        }

        int newStock = currentStock - quantity;

        if (newStock > 0) {
            product.setStock(newStock);
            productRepository.save(product);
            return;
        }

        // 🧨 stock llega a 0 → borrar todo
        List<Image> images = imageRepository.findByProductId(productId);

        imageRepository.deleteByProductId(productId);
        productRepository.delete(product);

        // 🔥 Firebase cleanup (fuera del control transaccional real)
        images.forEach(image -> {
            try {
                imageStorageService.deleteByUrl(image.getUrl());
            } catch (Exception e) {
                System.err.println(
                        "Failed to delete image from Firebase: " + image.getUrl()
                );
            }
        });
    }

    private ProductResponse toResponse(Product p) {
        List<String> imageUrls = imageRepository
                .findByProductIdOrderByIdAsc(p.getId())
                .stream()
                .map(Image::getUrl)
                .collect(Collectors.toList());

        return new ProductResponse(
                p.getId(),
                p.getName(),
                p.getDescription(),
                p.getPrice(),
                p.getStock(),
                imageUrls
        );
    }
}
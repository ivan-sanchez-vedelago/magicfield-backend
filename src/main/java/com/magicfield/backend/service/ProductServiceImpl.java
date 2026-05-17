package com.magicfield.backend.service;

import com.magicfield.backend.dto.ProductRequest;
import com.magicfield.backend.dto.ProductResponse;
import com.magicfield.backend.dto.PagedProductResponse;
import com.magicfield.backend.entity.Category;
import com.magicfield.backend.entity.Image;
import com.magicfield.backend.entity.Product;
import com.magicfield.backend.exception.ProductNotFoundException;
import com.magicfield.backend.service.ImageStorageService;
import com.magicfield.backend.repository.CategoryRepository;
import com.magicfield.backend.repository.ImageRepository;
import com.magicfield.backend.repository.ProductRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ImageStorageService imageStorageService;
    private final ImageRepository imageRepository;
    private final ScryfallService scryfallService;
    private final DollarService dollarService;

    public ProductServiceImpl(ProductRepository productRepository,
                              CategoryRepository categoryRepository,
                              ImageStorageService imageStorageService,
                              ImageRepository imageRepository,
                              ScryfallService scryfallService,
                              DollarService dollarService) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.imageStorageService = imageStorageService;
        this.imageRepository = imageRepository;
        this.scryfallService = scryfallService;
        this.dollarService = dollarService;
    }

    @Override
    public List<ProductResponse> listAll() {
        return productRepository.findAll()
                .stream()
                .filter(p -> p.getStock() > 0)
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public PagedProductResponse listPaged(String search, List<String> categories, int page, int size) {
        boolean allCategories = categories == null || categories.isEmpty();
        List<String> cats = allCategories ? List.of("") : categories;
        String normalizedSearch = (search == null) ? "" : search.trim();

        Page<Product> productPage = productRepository.findPaged(
                normalizedSearch,
                cats,
                allCategories,
                PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"))
        );

        // Bulk-load images for non-SIN products to eliminate N+1 queries
        List<UUID> nonSinIds = productPage.getContent().stream()
                .filter(p -> p.getCategory() == null || !"SIN".equals(p.getCategory().getShortName()))
                .map(Product::getId)
                .collect(Collectors.toList());

        Map<UUID, List<String>> imagesByProduct = new HashMap<>();
        if (!nonSinIds.isEmpty()) {
            imageRepository.findByProductIdsOrdered(nonSinIds).stream()
                    .collect(Collectors.groupingBy(
                            img -> img.getProduct().getId(),
                            Collectors.mapping(Image::getUrl, Collectors.toList())
                    ))
                    .forEach(imagesByProduct::put);
        }

        List<ProductResponse> content = productPage.getContent().stream()
                .map(p -> toResponseWithImages(p, imagesByProduct))
                .collect(Collectors.toList());

        return new PagedProductResponse(
                content,
                productPage.getTotalElements(),
                productPage.getTotalPages(),
                productPage.getNumber()
        );
    }

    private ProductResponse toResponseWithImages(Product p, Map<UUID, List<String>> imagesByProduct) {
        List<String> imageUrls;
        if (p.getCategory() != null && "SIN".equals(p.getCategory().getShortName()) && p.getScryfallId() != null) {
            imageUrls = scryfallService.getImageUrls(p.getScryfallId());
        } else {
            imageUrls = imagesByProduct.getOrDefault(p.getId(), List.of());
        }
        return new ProductResponse(
                p.getId(),
                p.getName(),
                p.getDescription(),
                p.getPrice(),
                p.getStock(),
                p.getCategory() != null ? p.getCategory().getShortName() : null,
                p.getScryfallId(),
                p.getIsFoil(),
                p.getSet(),
                p.getCollectorNumber(),
                p.getCondition(),
                p.getLanguage(),
                p.getCategory() != null ? p.getCategory().getId() : null,
                imageUrls
        );
    }

    @Override
    public ProductResponse getById(UUID id) {
        Product p = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        if (p.getStock() == 0) {
            throw new ProductNotFoundException(id);
        }
        return toResponse(p);
    }

    @Override
    public ProductResponse create(ProductRequest request) {
        try {
            System.out.println("Creando producto con tipo: " + request.getType());
            Product p = new Product();
            p.setName(request.getName());
            p.setDescription(request.getDescription());
            p.setStock(request.getStock());
            Category category = categoryRepository.findByShortName(request.getType().toUpperCase())
                    .orElseThrow(() -> new IllegalArgumentException("Tipo de producto inválido: " + request.getType()));
            p.setCategory(category);

            if ("SIN".equals(category.getShortName())) {
                BigDecimal usd = scryfallService.getPrice(
                    request.getScryfallId(),
                    request.getIsFoil()
                );
                BigDecimal ars = convertUsdToArs(usd);

                p.setPrice(ars);
                p.setLastPriceUpdate(LocalDateTime.now());
                p.setScryfallId(request.getScryfallId());
                p.setIsFoil(request.getIsFoil());
                p.setSet(request.getSet());
                p.setCollectorNumber(request.getCollectorNumber());
                p.setCondition(request.getCondition());
                p.setLanguage(request.getLanguage());
            } else {
                p.setPrice(request.getPrice());
            }

            Product saved = productRepository.save(p);
            return toResponse(saved);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Tipo de producto inválido: " + request.getType());
        }
    }

    @Override
    public ProductResponse update(UUID id, ProductRequest request) {
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
    public ProductResponse updateStock(UUID id, int stock) {
        Product p = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        p.setStock(stock);

        Product saved = productRepository.save(p);
        return toResponse(saved);
    }

    @Override
    public void delete(UUID id) {
        if (!productRepository.existsById(id)) {
            throw new ProductNotFoundException(id);
        }
        productRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void decreaseStock(UUID productId, int quantity) {

        if (quantity <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a 0");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() ->
                        new RuntimeException("Producto no encontrado con id " + productId)
                );

        int currentStock = product.getStock();

        if (currentStock < quantity) {
            throw new IllegalStateException("Stock insuficiente");
        }

        int newStock = currentStock - quantity;

        product.setStock(newStock);
        productRepository.save(product);
    }

    // AUTO UPDATE (cada 3 días)
    @Scheduled(cron = "0 0 3 */3 * *") // Cada 3 días a las 3 AM
    public void updatePrices() {
        System.out.println("Iniciando actualización de precios... " + LocalDateTime.now());
        LocalDateTime limit = LocalDateTime.now().minusDays(3);
        List<Product> singles = productRepository.findSinglesNeedingUpdate(limit);
        for (Product p : singles) {
            try {
                // Saltear si el producto fue eliminado mientras esperaba en cola (ej. se vendió)
                if (!productRepository.existsById(p.getId())) {
                    continue;
                }
                BigDecimal usd = scryfallService.getPrice(
                    p.getScryfallId(),
                    p.getIsFoil()
                );
                BigDecimal ars = convertUsdToArs(usd);
                p.setPrice(ars);
                p.setLastPriceUpdate(LocalDateTime.now());
                productRepository.save(p);
            } catch (Exception e) {
                System.err.println("Error actualizando producto " + p.getId());
            }
        }
    }

    private BigDecimal convertUsdToArs(BigDecimal usd) {
        if (usd == null) return BigDecimal.ZERO;

        BigDecimal withMarkup = applyMarkup(usd);
        BigDecimal rate = dollarService.getRate();
        BigDecimal priceArs = withMarkup.multiply(rate);

        return applyRetailPricing(priceArs);
    }

    private BigDecimal applyRetailPricing(BigDecimal price) {
        if (price == null) return BigDecimal.ZERO;
        BigDecimal step = new BigDecimal("100");

        // Dividir, redondear hacia arriba y volver a multiplicar
        BigDecimal divided = price.divide(step, 0, RoundingMode.UP);
        BigDecimal rounded = divided.multiply(step);

        // Ajustar a .99
        return rounded.subtract(new BigDecimal("0.01"));
    }

    private BigDecimal applyMarkup(BigDecimal usd) {
        if (usd.compareTo(new BigDecimal("10")) < 0) {
            return usd.multiply(new BigDecimal("1.3"));
        } else {
            return usd.multiply(new BigDecimal("1.4"));
        }
    }

    private ProductResponse toResponse(Product p) {
        List<String> imageUrls;

        if (p.getCategory() != null && "SIN".equals(p.getCategory().getShortName()) && p.getScryfallId() != null) {
            imageUrls = scryfallService.getImageUrls(p.getScryfallId());
        } else {
            imageUrls = imageRepository
                    .findByProductIdOrderByIdAsc(p.getId())
                    .stream()
                    .map(Image::getUrl)
                    .collect(Collectors.toList());
        }

        return new ProductResponse(
                p.getId(),
                p.getName(),
                p.getDescription(),
                p.getPrice(),
                p.getStock(),
                p.getCategory() != null ? p.getCategory().getShortName() : null,
                p.getScryfallId(),
                p.getIsFoil(),
                p.getSet(),
                p.getCollectorNumber(),
                p.getCondition(),
                p.getLanguage(),
                p.getCategory() != null ? p.getCategory().getId() : null,
                imageUrls
        );
    }
}
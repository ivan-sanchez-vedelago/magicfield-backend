package com.magicfield.backend.service;

import com.magicfield.backend.dto.*;
import com.magicfield.backend.entity.*;
import com.magicfield.backend.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProductTypeService {

    private final ProductRepository productRepository;
    private final SingleCardRepository singleCardRepository;
    private final SealedProductRepository sealedProductRepository;
    private final OtherProductRepository otherProductRepository;
    private final ImageRepository imageRepository;
    private final ScryfallService scryfallService;

    public ProductTypeService(
            ProductRepository productRepository,
            SingleCardRepository singleCardRepository,
            SealedProductRepository sealedProductRepository,
            OtherProductRepository otherProductRepository,
            ImageRepository imageRepository,
            ScryfallService scryfallService
    ) {
        this.productRepository = productRepository;
        this.singleCardRepository = singleCardRepository;
        this.sealedProductRepository = sealedProductRepository;
        this.otherProductRepository = otherProductRepository;
        this.imageRepository = imageRepository;
        this.scryfallService = scryfallService;
    }

    /**
     * Create a single card product
     */
    @Transactional
    public ProductResponseWithDetails createSingleCard(CreateSingleCardRequest request) {
        // 1. Consult Scryfall API
        ScryfallCardResponse scryfallCard = scryfallService.getCardByName(request.getCardName());

        // 2. Create base product
        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setType(ProductType.SINGLE);

        Product savedProduct = productRepository.save(product);

        // 3. Create single card record
        SingleCard singleCard = new SingleCard();
        singleCard.setProduct(savedProduct);
        singleCard.setScryfallId(scryfallCard.getId());
        singleCard.setCardName(scryfallCard.getName());
        singleCard.setSetCode(scryfallCard.getSet());
        singleCard.setCollectorNumber(scryfallCard.getCollectorNumber());
        singleCard.setCondition(request.getCondition());
        singleCard.setLanguage(request.getLanguage());
        singleCard.setFoil(request.getFoil());

        singleCardRepository.save(singleCard);

        // 4. Auto-create image from Scryfall
        if (scryfallCard.getImageUris() != null && scryfallCard.getImageUris().getLarge() != null) {
            Image image = new Image();
            image.setProduct(savedProduct);
            image.setUrl(scryfallCard.getImageUris().getLarge());
            image.setPrimaryImage(true);
            imageRepository.save(image);
        }

        return buildProductResponseWithDetails(savedProduct, singleCard, null, null);
    }

    /**
     * Create a sealed product
     */
    @Transactional
    public ProductResponseWithDetails createSealedProduct(CreateSealedProductRequest request) {
        // 1. Create base product
        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setType(ProductType.SEALED);

        Product savedProduct = productRepository.save(product);

        // 2. Create sealed product record
        SealedProduct sealedProduct = new SealedProduct();
        sealedProduct.setProduct(savedProduct);

        sealedProductRepository.save(sealedProduct);

        return buildProductResponseWithDetails(savedProduct, null, sealedProduct, null);
    }

    /**
     * Create an other product
     */
    @Transactional
    public ProductResponseWithDetails createOtherProduct(CreateOtherProductRequest request) {
        // 1. Create base product
        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setType(ProductType.OTHER);

        Product savedProduct = productRepository.save(product);

        // 2. Create other product record
        OtherProduct otherProduct = new OtherProduct();
        otherProduct.setProduct(savedProduct);
        otherProduct.setCategory(request.getCategory());

        otherProductRepository.save(otherProduct);

        return buildProductResponseWithDetails(savedProduct, null, null, otherProduct);
    }

    /**
     * Get product with its specific details by ID
     */
    public ProductResponseWithDetails getProductWithDetails(UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        SingleCard singleCard = null;
        SealedProduct sealedProduct = null;
        OtherProduct otherProduct = null;

        if (product.getType() == ProductType.SINGLE) {
            singleCard = singleCardRepository.findByProductId(productId)
                    .orElseThrow(() -> new RuntimeException("Single card data not found"));
        } else if (product.getType() == ProductType.SEALED) {
            sealedProduct = sealedProductRepository.findByProductId(productId)
                    .orElseThrow(() -> new RuntimeException("Sealed product data not found"));
        } else if (product.getType() == ProductType.OTHER) {
            otherProduct = otherProductRepository.findByProductId(productId)
                    .orElseThrow(() -> new RuntimeException("Other product data not found"));
        }

        return buildProductResponseWithDetails(product, singleCard, sealedProduct, otherProduct);
    }

    /**
     * Get all single cards
     */
    public List<ProductResponseWithDetails> getAllSingleCards() {
        return productRepository.findByType(ProductType.SINGLE).stream()
                .map(product -> {
                    SingleCard singleCard = singleCardRepository.findByProductId(product.getId()).orElse(null);
                    return buildProductResponseWithDetails(product, singleCard, null, null);
                })
                .collect(Collectors.toList());
    }

    /**
     * Get all sealed products
     */
    public List<ProductResponseWithDetails> getAllSealedProducts() {
        return productRepository.findByType(ProductType.SEALED).stream()
                .map(product -> {
                    SealedProduct sealedProduct = sealedProductRepository.findByProductId(product.getId()).orElse(null);
                    return buildProductResponseWithDetails(product, null, sealedProduct, null);
                })
                .collect(Collectors.toList());
    }

    /**
     * Build complete product response with details
     */
    private ProductResponseWithDetails buildProductResponseWithDetails(
            Product product,
            SingleCard singleCard,
            SealedProduct sealedProduct,
            OtherProduct otherProduct
    ) {
        ProductResponseWithDetails response = new ProductResponseWithDetails();
        response.setId(product.getId());
        response.setName(product.getName());
        response.setDescription(product.getDescription());
        response.setPrice(product.getPrice());
        response.setStock(product.getStock());
        response.setType(product.getType());
        response.setCreatedAt(product.getCreatedAt());

        // Set images
        List<String> imageUrls = imageRepository.findByProductIdOrderByIdAsc(product.getId())
                .stream()
                .map(Image::getUrl)
                .collect(Collectors.toList());
        response.setImageUrls(imageUrls);

        // Set type-specific data
        if (singleCard != null) {
            response.setScryfallId(singleCard.getScryfallId());
            response.setCardName(singleCard.getCardName());
            response.setSetCode(singleCard.getSetCode());
            response.setCollectorNumber(singleCard.getCollectorNumber());
            response.setCondition(singleCard.getCondition());
            response.setLanguage(singleCard.getLanguage());
            response.setFoil(singleCard.getFoil());
        }

        if (sealedProduct != null) {
            // No additional fields needed for sealed products
        }

        if (otherProduct != null) {
            response.setCategory(otherProduct.getCategory());
        }

        return response;
    }
}

package com.magicfield.backend.service;

import com.magicfield.backend.dto.AvailabilityCheckRequest;
import com.magicfield.backend.dto.AvailabilityCheckResponse;
import com.magicfield.backend.dto.CheckoutItemRequest;
import com.magicfield.backend.dto.CsvImportResult;
import com.magicfield.backend.dto.CsvImportRowError;
import com.magicfield.backend.dto.ProductAvailabilityResult;
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
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl implements ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductServiceImpl.class);

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
    public AvailabilityCheckResponse checkAvailability(AvailabilityCheckRequest request) {
        List<ProductAvailabilityResult> results = request.getItems().stream()
                .map(this::checkItemAvailability)
                .collect(Collectors.toList());

        boolean allAvailable = results.stream().allMatch(ProductAvailabilityResult::isSufficient);

        return new AvailabilityCheckResponse(allAvailable, results);
    }

    private ProductAvailabilityResult checkItemAvailability(CheckoutItemRequest item) {
        return productRepository.findById(item.getProductId())
                .map(p -> new ProductAvailabilityResult(
                        p.getId(),
                        true,
                        p.getName(),
                        p.getStock(),
                        item.getQuantity(),
                        p.getStock() >= item.getQuantity()
                ))
                .orElseGet(() -> new ProductAvailabilityResult(
                        item.getProductId(),
                        false,
                        null,
                        0,
                        item.getQuantity(),
                        false
                ));
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
    public PagedProductResponse listPaged(String search, List<String> categories, int page, int size, String sort) {
        boolean allCategories = categories == null || categories.isEmpty();
        List<String> cats = allCategories ? List.of("") : categories;
        String normalizedSearch = (search == null) ? "" : search.trim();

        Sort pageSort = switch (sort == null ? "" : sort) {
            case "NAME_DESC"  -> Sort.by(Sort.Direction.DESC, "name");
            case "PRICE_ASC"  -> Sort.by(Sort.Direction.ASC,  "price");
            case "PRICE_DESC" -> Sort.by(Sort.Direction.DESC, "price");
            default           -> Sort.by(Sort.Direction.ASC,  "name");
        };

        Page<Product> productPage = productRepository.findPaged(
                normalizedSearch,
                cats,
                allCategories,
                PageRequest.of(page, size, pageSort)
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
        String description = p.getDescription();
        if (p.getCategory() != null && "SIN".equals(p.getCategory().getShortName()) && p.getScryfallId() != null) {
            ScryfallService.ScryfallCardData cardData = scryfallService.getCardData(p.getScryfallId());
            imageUrls = cardData.getImageUrls();
            if (cardData.getDescription() != null) description = cardData.getDescription();
        } else {
            imageUrls = imagesByProduct.getOrDefault(p.getId(), List.of());
        }
        return new ProductResponse(
                p.getId(),
                p.getName(),
                description,
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
                p.getCreatedAt(),
                imageUrls
        );
    }

    @Override
    public PagedProductResponse listRestorablePaged(String search, int page, int size) {
        String normalizedSearch = (search == null) ? "" : search.trim();

        Page<Product> productPage = productRepository.findRestorablePaged(
                normalizedSearch,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        List<ProductResponse> content = productPage.getContent().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return new PagedProductResponse(
                content,
                productPage.getTotalElements(),
                productPage.getTotalPages(),
                productPage.getNumber()
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
    public ProductResponse getByIdIncludingSoldOut(UUID id) {
        Product p = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
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
                p.setPrice(applyRetailPricing(request.getPrice()));
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
        p.setStock(request.getStock());

        boolean isSingle = p.getCategory() != null && "SIN".equals(p.getCategory().getShortName());
        if (!isSingle) {
            p.setPrice(applyRetailPricing(request.getPrice()));
        }

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

    @Override
    @Transactional
    public CsvImportResult importSinglesFromCsv(MultipartFile file) throws IOException {
        Category singleCategory = categoryRepository.findByShortName("SIN")
                .orElseThrow(() -> new IllegalStateException("No existe la categoría SIN"));

        List<CsvImportRowError> errors = new ArrayList<>();
        // Dedup dentro del mismo archivo: mismo scryfallId + foil -> se suman las cantidades.
        Map<String, ParsedSingleRow> byKey = new LinkedHashMap<>();
        int totalRows = 0;

        try (
                InputStreamReader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
                CSVParser parser = CSVFormat.DEFAULT.builder()
                        .setHeader()
                        .setSkipHeaderRecord(true)
                        .setTrim(true)
                        .setIgnoreSurroundingSpaces(true)
                        .build()
                        .parse(reader)
        ) {
            for (CSVRecord record : parser) {
                totalRows++;
                int fileLine = (int) record.getRecordNumber() + 1; // +1 por la fila de encabezado
                String cardName = safeGet(record, "Name");
                try {
                    ParsedSingleRow parsed = parseSingleRow(record);
                    String key = parsed.scryfallId() + ":" + parsed.isFoil();
                    byKey.merge(key, parsed, (existing, incoming) -> existing.withAddedQuantity(incoming.quantity()));
                } catch (IllegalArgumentException e) {
                    errors.add(new CsvImportRowError(fileLine, cardName, e.getMessage()));
                }
            }
        }

        int created = 0;
        int updatedExisting = 0;

        for (ParsedSingleRow row : byKey.values()) {
            var existing = productRepository.findByScryfallIdAndIsFoil(row.scryfallId(), row.isFoil());
            if (existing.isPresent()) {
                Product product = existing.get();
                product.setStock(product.getStock() + row.quantity());
                productRepository.save(product);
                updatedExisting++;
            } else {
                Product product = new Product();
                product.setName(row.name());
                product.setDescription("");
                product.setStock(row.quantity());
                product.setCategory(singleCategory);
                product.setScryfallId(row.scryfallId());
                product.setIsFoil(row.isFoil());
                product.setSet(row.setName());
                product.setCollectorNumber(row.collectorNumber());
                product.setCondition(row.condition());
                product.setLanguage(row.language());
                product.setPrice(convertUsdToArs(row.purchaseUsd()));
                product.setLastPriceUpdate(LocalDateTime.now());
                productRepository.save(product);
                created++;
            }
        }

        log.info("[CSV Import] {} filas, {} creados, {} actualizados, {} errores",
                totalRows, created, updatedExisting, errors.size());

        return new CsvImportResult(totalRows, created, updatedExisting, errors);
    }

    private record ParsedSingleRow(
            String name, String setName, String collectorNumber, boolean isFoil,
            int quantity, BigDecimal purchaseUsd, String condition, String language,
            String scryfallId
    ) {
        ParsedSingleRow withAddedQuantity(int extra) {
            return new ParsedSingleRow(name, setName, collectorNumber, isFoil,
                    quantity + extra, purchaseUsd, condition, language, scryfallId);
        }
    }

    // Exige que el CSV (exportado por ManaBox) traiga el Scryfall ID de cada carta:
    // es el identificador único de esa impresión exacta, así que no hace falta
    // consultar la API de Scryfall para "encontrar"/verificar la carta.
    private ParsedSingleRow parseSingleRow(CSVRecord record) {
        String name = requireNonBlank(record, "Name");
        String setName = requireNonBlank(record, "Set name");
        String collectorNumber = requireNonBlank(record, "Collector number");

        String foilRaw = requireNonBlank(record, "Foil");
        boolean isFoil;
        if (foilRaw.equalsIgnoreCase("foil")) {
            isFoil = true;
        } else if (foilRaw.equalsIgnoreCase("normal")) {
            isFoil = false;
        } else {
            throw new IllegalArgumentException("Valor de 'Foil' inválido: " + foilRaw);
        }

        String scryfallIdRaw = requireNonBlank(record, "Scryfall ID");
        UUID scryfallUuid;
        try {
            scryfallUuid = UUID.fromString(scryfallIdRaw);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Scryfall ID inválido: " + scryfallIdRaw);
        }

        int quantity;
        try {
            quantity = Integer.parseInt(requireNonBlank(record, "Quantity"));
            if (quantity <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Cantidad inválida");
        }

        String currency = requireNonBlank(record, "Purchase price currency");
        if (!currency.equalsIgnoreCase("USD")) {
            throw new IllegalArgumentException("Moneda no soportada: " + currency + " (solo USD)");
        }

        BigDecimal purchaseUsd;
        try {
            purchaseUsd = new BigDecimal(requireNonBlank(record, "Purchase price"));
            if (purchaseUsd.compareTo(BigDecimal.ZERO) <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Precio de compra inválido");
        }

        String condition = normalizeCondition(requireNonBlank(record, "Condition"));
        String language = normalizeLanguage(requireNonBlank(record, "Language"));

        return new ParsedSingleRow(name, setName, collectorNumber, isFoil, quantity,
                purchaseUsd, condition, language, scryfallUuid.toString());
    }

    private String requireNonBlank(CSVRecord record, String column) {
        if (!record.isMapped(column)) {
            throw new IllegalArgumentException("Falta la columna '" + column + "' en el CSV");
        }
        String value = record.get(column);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Falta el valor de '" + column + "'");
        }
        return value.trim();
    }

    private String safeGet(CSVRecord record, String column) {
        try {
            return record.isMapped(column) ? record.get(column) : "";
        } catch (Exception e) {
            return "";
        }
    }

    private String normalizeCondition(String raw) {
        String[] parts = raw.replace('_', ' ').trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1).toLowerCase(Locale.ROOT));
        }
        return sb.toString();
    }

    private static final Map<String, String> LANGUAGE_NAMES = Map.ofEntries(
            Map.entry("en", "English"),
            Map.entry("es", "Spanish"),
            Map.entry("fr", "French"),
            Map.entry("de", "German"),
            Map.entry("it", "Italian"),
            Map.entry("pt", "Portuguese"),
            Map.entry("ja", "Japanese"),
            Map.entry("ko", "Korean"),
            Map.entry("ru", "Russian"),
            Map.entry("zhs", "Simplified Chinese"),
            Map.entry("zht", "Traditional Chinese")
    );

    private String normalizeLanguage(String raw) {
        String key = raw.trim().toLowerCase(Locale.ROOT);
        return LANGUAGE_NAMES.getOrDefault(key, raw.toUpperCase(Locale.ROOT));
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

        // Piso mínimo de 1200 ARS
        BigDecimal minPrice = new BigDecimal("1200");
        if (price.compareTo(minPrice) < 0) {
            price = minPrice;
        }

        // Redondeo psicológico a .99:
        BigDecimal step = new BigDecimal("100");
        BigDecimal divided = price.divide(step, 0, RoundingMode.UP);
        BigDecimal rounded = divided.multiply(step);
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
        String description = p.getDescription();

        if (p.getCategory() != null && "SIN".equals(p.getCategory().getShortName()) && p.getScryfallId() != null) {
            ScryfallService.ScryfallCardData cardData = scryfallService.getCardData(p.getScryfallId());
            imageUrls = cardData.getImageUrls();
            if (cardData.getDescription() != null) description = cardData.getDescription();
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
                description,
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
                p.getCreatedAt(),
                imageUrls
        );
    }
}
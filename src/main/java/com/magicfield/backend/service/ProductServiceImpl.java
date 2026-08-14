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
import com.magicfield.backend.entity.CardCondition;
import com.magicfield.backend.entity.CardFinish;
import com.magicfield.backend.entity.CardLanguage;
import com.magicfield.backend.entity.Category;
import com.magicfield.backend.entity.Image;
import com.magicfield.backend.entity.Product;
import com.magicfield.backend.exception.ProductNotFoundException;
import com.magicfield.backend.service.ImageStorageService;
import com.magicfield.backend.repository.CardConditionRepository;
import com.magicfield.backend.repository.CardFinishRepository;
import com.magicfield.backend.repository.CardLanguageRepository;
import com.magicfield.backend.repository.CategoryRepository;
import com.magicfield.backend.repository.ImageRepository;
import com.magicfield.backend.repository.ProductRepository;
import jakarta.transaction.Transactional;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
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
    private final CardConditionRepository cardConditionRepository;
    private final CardLanguageRepository cardLanguageRepository;
    private final CardFinishRepository cardFinishRepository;

    public ProductServiceImpl(ProductRepository productRepository,
                              CategoryRepository categoryRepository,
                              ImageStorageService imageStorageService,
                              ImageRepository imageRepository,
                              ScryfallService scryfallService,
                              DollarService dollarService,
                              CardConditionRepository cardConditionRepository,
                              CardLanguageRepository cardLanguageRepository,
                              CardFinishRepository cardFinishRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.imageStorageService = imageStorageService;
        this.imageRepository = imageRepository;
        this.scryfallService = scryfallService;
        this.dollarService = dollarService;
        this.cardConditionRepository = cardConditionRepository;
        this.cardLanguageRepository = cardLanguageRepository;
        this.cardFinishRepository = cardFinishRepository;
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
        List<Product> inStock = productRepository.findAll()
                .stream()
                .filter(p -> p.getStock() > 0)
                .collect(Collectors.toList());

        // Bulk-load images for non-SIN products to eliminate N+1 queries (mismo criterio que
        // listPaged): los singles no entran acá, sus imágenes vienen de Scryfall (cacheadas).
        List<UUID> nonSinIds = inStock.stream()
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

        return inStock.stream()
                .map(p -> toResponseWithImages(p, imagesByProduct))
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

    // Catálogo público: agrupa los singles por (scryfallId, finish) sumando stock entre
    // condiciones/idiomas, y deja sellados/accesorios sin agrupar (comportamiento idéntico
    // a listPaged para esos). El admin sigue usando listPaged, no este método, así que ahí
    // cada fila se ve por separado como siempre.
    //
    // Se agrupa en memoria sobre un fetch sin paginar (no con una query SQL de ventana) por
    // simplicidad: es más fácil de mantener correcto combinado con los filtros existentes,
    // y a la escala de este catálogo el costo extra es insignificante.
    @Override
    public PagedProductResponse listCatalogPaged(String search, List<String> categories, int page, int size, String sort) {
        boolean allCategories = categories == null || categories.isEmpty();
        List<String> cats = allCategories ? List.of("") : categories;
        String normalizedSearch = (search == null) ? "" : search.trim();

        List<Product> all = productRepository.findAllMatching(normalizedSearch, cats, allCategories);

        Map<String, List<Product>> singleGroups = new LinkedHashMap<>();
        List<CatalogEntry> entries = new ArrayList<>();

        for (Product p : all) {
            boolean isSingle = p.getCategory() != null && "SIN".equals(p.getCategory().getShortName())
                    && p.getScryfallId() != null && p.getFinish() != null;
            if (isSingle) {
                String key = p.getScryfallId() + ":" + p.getFinish().getId();
                singleGroups.computeIfAbsent(key, k -> new ArrayList<>()).add(p);
            } else {
                entries.add(new CatalogEntry(p, p.getStock(), p.getPrice(), null));
            }
        }

        for (List<Product> group : singleGroups.values()) {
            Product representative = group.get(0);
            int totalStock = group.stream().mapToInt(Product::getStock).sum();
            BigDecimal minPrice = group.stream()
                    .map(Product::getPrice)
                    .min(Comparator.naturalOrder())
                    .orElse(representative.getPrice());
            entries.add(new CatalogEntry(representative, totalStock, minPrice, group.size()));
        }

        entries.sort(catalogComparator(sort));

        int totalElements = entries.size();
        int totalPages = size > 0 ? (int) Math.ceil(totalElements / (double) size) : 0;
        int fromIndex = Math.min(page * size, totalElements);
        int toIndex = Math.min(fromIndex + size, totalElements);

        List<ProductResponse> content = entries.subList(fromIndex, toIndex).stream()
                .map(this::toCatalogResponse)
                .collect(Collectors.toList());

        return new PagedProductResponse(content, totalElements, totalPages, page);
    }

    // Últimos "limit" productos agregados en stock, agrupando singles por (scryfallId, finish)
    // igual que listCatalogPaged -- sin cargar el catálogo completo como hacía listAll(), que
    // es lo que volvía lento el slider de "Novedades" con miles de productos.
    @Override
    public List<ProductResponse> listNewest(int limit) {
        // Sobre-fetchea filas crudas (varias condiciones/idiomas de la misma carta+finish son
        // filas separadas) para asegurar que entren suficientes cartas DISTINTAS antes de
        // agrupar -- margen generoso porque una carta rara vez tiene más de unas pocas
        // variantes de condición/idioma cargadas.
        int rawFetchSize = Math.max(limit * 10, 100);
        List<Product> raw = productRepository.findNewestInStock(PageRequest.of(0, rawFetchSize));

        // raw ya viene ordenado por createdAt DESC, así que la primera fila de cada grupo que
        // aparece es la más nueva de ese grupo.
        Map<String, Product> newestByGroup = new LinkedHashMap<>();
        for (Product p : raw) {
            boolean isSingle = p.getCategory() != null && "SIN".equals(p.getCategory().getShortName())
                    && p.getScryfallId() != null && p.getFinish() != null;
            String key = isSingle ? p.getScryfallId() + ":" + p.getFinish().getId() : p.getId().toString();
            if (newestByGroup.containsKey(key)) continue;
            newestByGroup.put(key, p);
            if (newestByGroup.size() >= limit) break;
        }

        List<CatalogEntry> entries = newestByGroup.values().stream()
                .map(representative -> {
                    boolean isSingle = representative.getCategory() != null
                            && "SIN".equals(representative.getCategory().getShortName())
                            && representative.getScryfallId() != null && representative.getFinish() != null;
                    if (!isSingle) {
                        return new CatalogEntry(representative, representative.getStock(), representative.getPrice(), null);
                    }
                    // Stock/precio agregados sobre TODAS las condiciones/idiomas de esta
                    // carta+finish (no solo las que entraron en el fetch crudo), para que el
                    // dato mostrado sea igual de correcto que en el catálogo/carrito.
                    List<Product> siblings = productRepository.findByScryfallIdAndFinishId(
                            representative.getScryfallId(), representative.getFinish().getId());
                    int totalStock = siblings.stream().mapToInt(Product::getStock).sum();
                    BigDecimal minPrice = siblings.stream()
                            .map(Product::getPrice)
                            .min(Comparator.naturalOrder())
                            .orElse(representative.getPrice());
                    return new CatalogEntry(representative, totalStock, minPrice, siblings.size());
                })
                .collect(Collectors.toList());

        return entries.stream()
                .map(this::toCatalogResponse)
                .collect(Collectors.toList());
    }

    private record CatalogEntry(Product representative, int stock, BigDecimal price, Integer variantCount) {}

    private ProductResponse toCatalogResponse(CatalogEntry entry) {
        ProductResponse response = toResponse(entry.representative());
        response.setStock(entry.stock());
        response.setPrice(entry.price());
        response.setVariantCount(entry.variantCount());
        return response;
    }

    private Comparator<CatalogEntry> catalogComparator(String sort) {
        Comparator<CatalogEntry> byName = Comparator.comparing(
                e -> e.representative().getName(), String.CASE_INSENSITIVE_ORDER);
        return switch (sort == null ? "" : sort) {
            case "NAME_DESC"  -> byName.reversed();
            case "PRICE_ASC"  -> Comparator.comparing(CatalogEntry::price);
            case "PRICE_DESC" -> Comparator.comparing(CatalogEntry::price, Comparator.reverseOrder());
            default           -> byName;
        };
    }

    // Todas las variantes (condición/idioma) en stock de la misma carta+finish que el
    // producto dado — alimenta el selector de variantes de la pantalla de detalle.
    @Override
    public List<ProductResponse> getVariants(UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        if (product.getScryfallId() == null || product.getFinish() == null) {
            return List.of(toResponse(product));
        }

        return productRepository.findByScryfallIdAndFinishId(product.getScryfallId(), product.getFinish().getId())
                .stream()
                .filter(p -> p.getStock() > 0)
                .map(this::toResponse)
                .collect(Collectors.toList());
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
        return buildResponse(p, description, imageUrls);
    }

    private ProductResponse buildResponse(Product p, String description, List<String> imageUrls) {
        Category category = p.getCategory();
        CardCondition condition = p.getCondition();
        CardLanguage language = p.getLanguage();
        CardFinish finish = p.getFinish();

        List<String> variantTagLabels = p.getVariantTagsList().stream()
                .map(tag -> ScryfallService.VARIANT_TAG_LABELS.getOrDefault(tag, tag))
                .toList();
        String displayName = variantTagLabels.isEmpty()
                ? p.getName()
                : p.getName() + variantTagLabels.stream().map(t -> " (" + t + ")").collect(Collectors.joining());

        return new ProductResponse(
                p.getId(),
                p.getName(),
                displayName,
                variantTagLabels,
                description,
                p.getPrice(),
                p.getStock(),
                category != null ? category.getShortName() : null,
                p.getScryfallId(),
                finish != null ? finish.getId() : null,
                finish != null ? finish.getShortName() : null,
                finish != null ? finish.getLongName() : null,
                p.getSet(),
                p.getCollectorNumber(),
                condition != null ? condition.getId() : null,
                condition != null ? condition.getLongName() : null,
                language != null ? language.getId() : null,
                language != null ? language.getLongName() : null,
                category != null ? category.getId() : null,
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
        Category category = categoryRepository.findByShortName(request.getType().toUpperCase())
                .orElseThrow(() -> new IllegalArgumentException("Tipo de producto inválido: " + request.getType()));

        if ("SIN".equals(category.getShortName())) {
            return createOrMergeSingle(request, category);
        }

        Product p = new Product();
        p.setName(request.getName());
        p.setDescription(request.getDescription());
        p.setStock(request.getStock());
        p.setCategory(category);
        p.setPrice(applyRetailPricing(request.getPrice()));

        Product saved = productRepository.save(p);
        return toResponse(saved);
    }

    // Si ya existe un producto con la misma variante exacta (carta+finish+condición+idioma),
    // suma el stock pedido a ese producto en vez de crear una fila duplicada.
    private ProductResponse createOrMergeSingle(ProductRequest request, Category category) {
        CardFinish finish = cardFinishRepository.findById(request.getFinishId())
                .orElseThrow(() -> new IllegalArgumentException("Finish inválido: " + request.getFinishId()));
        CardCondition condition = cardConditionRepository.findById(request.getConditionId())
                .orElseThrow(() -> new IllegalArgumentException("Condición inválida: " + request.getConditionId()));
        CardLanguage language = cardLanguageRepository.findById(request.getLanguageId())
                .orElseThrow(() -> new IllegalArgumentException("Idioma inválido: " + request.getLanguageId()));

        Optional<Product> existing = productRepository.findByScryfallIdAndFinishIdAndConditionIdAndLanguageId(
                request.getScryfallId(), finish.getId(), condition.getId(), language.getId());

        if (existing.isPresent()) {
            return mergeStockInto(existing.get(), request.getStock());
        }

        // Un solo GET a Scryfall para todo lo que hace falta al crear la fila (antes eran 2-3
        // llamadas sueltas): precio, descripción de respaldo, finishes disponibles y tags de
        // variante de arte/marco (borderless, extended art, etc.) vienen en la misma respuesta.
        ScryfallService.ScryfallCollectionData cardData = scryfallService.getFullCardData(request.getScryfallId());

        // La carta puede no tener las 4 variantes de finish (hay promos foil-only, por
        // ejemplo): se valida contra lo que Scryfall reporta antes de crear la fila.
        if (!scryfallService.isFinishAvailable(cardData.getFinishes(), finish.getShortName())) {
            throw new IllegalArgumentException(
                    "La carta no tiene el finish '" + finish.getShortName() + "' disponible según Scryfall");
        }

        // Si el admin no completó (o vació) la descripción en el form, se recurre a Scryfall
        // en vez de guardar la fila sin descripción -- mismo criterio que el import de CSV.
        String description = request.getDescription();
        if (description == null || description.isBlank()) {
            description = cardData.getDescription();
        }

        Product p = new Product();
        p.setName(request.getName());
        p.setDescription(description != null ? description : "");
        p.setStock(request.getStock());
        p.setCategory(category);
        p.setScryfallId(request.getScryfallId());
        p.setFinish(finish);
        p.setSet(request.getSet());
        p.setCollectorNumber(request.getCollectorNumber());
        p.setCondition(condition);
        p.setLanguage(language);
        p.setVariantTags(cardData.getVariantTags());

        BigDecimal usd = scryfallService.extractPrice(cardData.getPrices(), finish.getShortName());
        p.setPrice(convertUsdToArs(usd, condition.getPriceMultiplier()));
        p.setLastPriceUpdate(LocalDateTime.now());

        try {
            Product saved = productRepository.save(p);
            return toResponse(saved);
        } catch (DataIntegrityViolationException e) {
            // Carrera: otra request creó la misma variante en el medio. Reintentar como merge.
            Product raceExisting = productRepository.findByScryfallIdAndFinishIdAndConditionIdAndLanguageId(
                    request.getScryfallId(), finish.getId(), condition.getId(), language.getId())
                    .orElseThrow(() -> e);
            return mergeStockInto(raceExisting, request.getStock());
        }
    }

    private ProductResponse mergeStockInto(Product existing, int extraStock) {
        existing.setStock(existing.getStock() + extraStock);
        Product saved = productRepository.save(existing);
        ProductResponse response = toResponse(saved);
        response.setMerged(true);
        return response;
    }

    @Override
    public ProductResponse update(UUID id, ProductRequest request) {
        Product p = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        p.setName(request.getName());
        p.setDescription(request.getDescription());
        p.setStock(request.getStock());

        boolean isSingle = p.getCategory() != null && "SIN".equals(p.getCategory().getShortName());
        if (isSingle) {
            updateSingleFields(p, request);
        } else {
            p.setPrice(applyRetailPricing(request.getPrice()));
        }

        try {
            Product saved = productRepository.save(p);
            return toResponse(saved);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException(
                    "Ya existe otro producto con esa misma combinación de carta, finish, condición e idioma");
        }
    }

    // Aplica los campos propios de un single (set/N° de coleccionista/condición/idioma/finish)
    // que hasta ahora solo se podían fijar al crear -- el panel de edición los mandaba pero
    // update() los ignoraba por completo. Si cambia el finish o la condición, recalcula el
    // precio (mismo criterio que create()/updatePrices(): USD de Scryfall según el finish,
    // multiplicado por el multiplicador de la condición) para no dejar un precio inconsistente
    // con la variante recién elegida.
    private void updateSingleFields(Product p, ProductRequest request) {
        if (request.getSet() != null) {
            p.setSet(request.getSet());
        }
        if (request.getCollectorNumber() != null) {
            p.setCollectorNumber(request.getCollectorNumber());
        }

        boolean priceInputsChanged = false;
        // Snapshot de Scryfall reutilizado para validar el finish nuevo y, si hace falta,
        // recalcular el precio -- un solo GET en vez de uno para validar y otro para el precio.
        ScryfallService.ScryfallCollectionData finishChangeSnapshot = null;

        if (request.getFinishId() != null
                && (p.getFinish() == null || !p.getFinish().getId().equals(request.getFinishId()))) {
            CardFinish finish = cardFinishRepository.findById(request.getFinishId())
                    .orElseThrow(() -> new IllegalArgumentException("Finish inválido: " + request.getFinishId()));
            if (p.getScryfallId() != null) {
                finishChangeSnapshot = scryfallService.getFullCardData(p.getScryfallId());
                if (!scryfallService.isFinishAvailable(finishChangeSnapshot.getFinishes(), finish.getShortName())) {
                    throw new IllegalArgumentException(
                            "La carta no tiene el finish '" + finish.getShortName() + "' disponible según Scryfall");
                }
            }
            p.setFinish(finish);
            priceInputsChanged = true;
        }

        if (request.getConditionId() != null
                && (p.getCondition() == null || !p.getCondition().getId().equals(request.getConditionId()))) {
            CardCondition condition = cardConditionRepository.findById(request.getConditionId())
                    .orElseThrow(() -> new IllegalArgumentException("Condición inválida: " + request.getConditionId()));
            p.setCondition(condition);
            priceInputsChanged = true;
        }

        if (request.getLanguageId() != null
                && (p.getLanguage() == null || !p.getLanguage().getId().equals(request.getLanguageId()))) {
            CardLanguage language = cardLanguageRepository.findById(request.getLanguageId())
                    .orElseThrow(() -> new IllegalArgumentException("Idioma inválido: " + request.getLanguageId()));
            p.setLanguage(language);
        }

        if (priceInputsChanged && p.getScryfallId() != null && p.getFinish() != null && p.getCondition() != null) {
            BigDecimal usd = finishChangeSnapshot != null
                    ? scryfallService.extractPrice(finishChangeSnapshot.getPrices(), p.getFinish().getShortName())
                    : scryfallService.getPrice(p.getScryfallId(), p.getFinish().getShortName());
            p.setPrice(convertUsdToArs(usd, p.getCondition().getPriceMultiplier()));
            p.setLastPriceUpdate(LocalDateTime.now());
        }
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

        // Se traen una sola vez: evita golpear la DB por cada fila del CSV para resolver
        // condición/idioma/finish (son tablas chicas y no cambian durante el import).
        List<CardCondition> conditions = cardConditionRepository.findAll();
        List<CardLanguage> languages = cardLanguageRepository.findAll();
        List<CardFinish> finishes = cardFinishRepository.findAll();

        List<CsvImportRowError> errors = new ArrayList<>();
        // Dedup dentro del mismo archivo: misma carta+finish+condición+idioma -> se suman cantidades.
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
                    ParsedSingleRow parsed = parseSingleRow(record, conditions, languages, finishes);
                    String key = parsed.scryfallId() + ":" + parsed.finish().getId()
                            + ":" + parsed.condition().getId() + ":" + parsed.language().getId();
                    byKey.merge(key, parsed, (existing, incoming) -> existing.withAddedQuantity(incoming.quantity()));
                } catch (IllegalArgumentException e) {
                    errors.add(new CsvImportRowError(fileLine, cardName, e.getMessage()));
                }
            }
        }

        // Precio + descripción en lotes de hasta 75 ids (en vez de un GET por fila): con CSVs
        // de hasta ~1000 filas, un lookup por fila tardaría minutos y monopolizaría el rate
        // limit global de Scryfall que comparten el resto de los endpoints de la app.
        List<String> distinctScryfallIds = byKey.values().stream()
                .map(ParsedSingleRow::scryfallId)
                .distinct()
                .toList();
        Map<String, ScryfallService.ScryfallCollectionData> dataByCardId =
                scryfallService.getCollectionDataBulk(distinctScryfallIds);

        int created = 0;
        int updatedExisting = 0;

        for (ParsedSingleRow row : byKey.values()) {
            var existing = productRepository.findByScryfallIdAndFinishIdAndConditionIdAndLanguageId(
                    row.scryfallId(), row.finish().getId(), row.condition().getId(), row.language().getId());
            if (existing.isPresent()) {
                Product product = existing.get();
                product.setStock(product.getStock() + row.quantity());
                productRepository.save(product);
                updatedExisting++;
            } else {
                ScryfallService.ScryfallCollectionData cardData = dataByCardId.get(row.scryfallId());
                BigDecimal usd = scryfallService.extractPrice(
                        cardData != null ? cardData.getPrices() : null, row.finish().getShortName());
                String description = cardData != null && cardData.getDescription() != null
                        ? cardData.getDescription() : "";

                Product product = new Product();
                product.setName(row.name());
                product.setDescription(description);
                product.setStock(row.quantity());
                product.setCategory(singleCategory);
                product.setScryfallId(row.scryfallId());
                product.setFinish(row.finish());
                product.setSet(row.setName());
                product.setCollectorNumber(row.collectorNumber());
                product.setCondition(row.condition());
                product.setLanguage(row.language());
                product.setVariantTags(cardData != null ? cardData.getVariantTags() : null);
                product.setPrice(convertUsdToArs(usd, row.condition().getPriceMultiplier()));
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
            String name, String setName, String collectorNumber, CardFinish finish,
            int quantity, CardCondition condition, CardLanguage language,
            String scryfallId
    ) {
        ParsedSingleRow withAddedQuantity(int extra) {
            return new ParsedSingleRow(name, setName, collectorNumber, finish,
                    quantity + extra, condition, language, scryfallId);
        }
    }

    // Exige que el CSV (exportado por ManaBox) traiga el Scryfall ID de cada carta:
    // es el identificador único de esa impresión exacta, así que no hace falta
    // consultar la API de Scryfall para "encontrar"/verificar la carta.
    private ParsedSingleRow parseSingleRow(CSVRecord record, List<CardCondition> conditions,
                                            List<CardLanguage> languages, List<CardFinish> finishes) {
        String name = requireNonBlank(record, "Name");
        String setName = requireNonBlank(record, "Set name");
        String collectorNumber = requireNonBlank(record, "Collector number");

        // NOTA: ManaBox exporta al menos "foil"/"normal"; "etched" se soporta acá pero no
        // está confirmado contra un CSV real reciente si también exporta "glossy" — revisar
        // si aparece algún error de "Foil inválido" con un valor no contemplado.
        String foilRaw = requireNonBlank(record, "Foil");
        String finishShortName;
        switch (foilRaw.toLowerCase(Locale.ROOT)) {
            case "foil" -> finishShortName = "FOIL";
            case "normal" -> finishShortName = "NONFOIL";
            case "etched" -> finishShortName = "ETCHED";
            default -> throw new IllegalArgumentException("Valor de 'Foil' inválido: " + foilRaw);
        }
        CardFinish finish = resolveFinish(finishShortName, finishes);

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

        CardCondition condition = resolveCondition(requireNonBlank(record, "Condition"), conditions);
        CardLanguage language = resolveLanguage(requireNonBlank(record, "Language"), languages);

        return new ParsedSingleRow(name, setName, collectorNumber, finish, quantity,
                condition, language, scryfallUuid.toString());
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

    // ManaBox exporta 7 niveles de condición (mint, near_mint, excellent, good, light_played,
    // played, poor -- de mejor a peor, sin "damaged") pero la tabla semilla solo tiene 5
    // (NM/LP/MP/HP/DMG): se agrupan explícitamente por short_name acá, sin depender del
    // long_name (que puede haber sido renombrado en la DB) para estos términos conocidos.
    private static final Map<String, String> MANABOX_CONDITION_ALIASES = Map.ofEntries(
            Map.entry("mint", "NM"),
            Map.entry("near mint", "NM"),
            Map.entry("excellent", "LP"),
            Map.entry("good", "LP"),
            Map.entry("light played", "MP"),
            Map.entry("played", "HP"),
            Map.entry("poor", "DMG")
    );

    // Para cualquier valor que no sea uno de los términos conocidos de ManaBox, se sigue
    // aceptando texto que matchee (por short_name o long_name, sin distinguir mayúsculas,
    // y "_" tratado como espacio) alguna fila de la tabla semilla. Si tampoco matchea eso
    // (por ejemplo un término nuevo que ManaBox agregue más adelante), no se rechaza la fila:
    // se asume NM y se deja un warning en el log para poder revisarlo a mano después.
    private CardCondition resolveCondition(String raw, List<CardCondition> conditions) {
        String normalized = raw.replace('_', ' ').trim();
        String aliasedShortName = MANABOX_CONDITION_ALIASES.get(normalized.toLowerCase(Locale.ROOT));
        String target = aliasedShortName != null ? aliasedShortName : normalized;

        return conditions.stream()
                .filter(c -> c.getShortName().equalsIgnoreCase(target) || c.getLongName().equalsIgnoreCase(target))
                .findFirst()
                .orElseGet(() -> {
                    log.warn("[CSV Import] Condición no reconocida '{}', se usa NM como fallback", raw);
                    return conditions.stream()
                            .filter(c -> c.getShortName().equalsIgnoreCase("NM"))
                            .findFirst()
                            .orElseThrow(() -> new IllegalStateException("Condición semilla 'NM' no encontrada"));
                });
    }

    private CardLanguage resolveLanguage(String raw, List<CardLanguage> languages) {
        String normalized = raw.trim();
        return languages.stream()
                .filter(l -> l.getShortName().equalsIgnoreCase(normalized) || l.getLongName().equalsIgnoreCase(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Idioma no reconocido: " + raw));
    }

    private CardFinish resolveFinish(String shortName, List<CardFinish> finishes) {
        return finishes.stream()
                .filter(f -> f.getShortName().equalsIgnoreCase(shortName))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Finish no encontrado en la tabla semilla: " + shortName));
    }

    // AUTO UPDATE (cada 3 días)
    @Scheduled(cron = "0 0 3 */3 * *") // Cada 3 días a las 3 AM
    public void updatePrices() {
        System.out.println("Iniciando actualización de precios... " + LocalDateTime.now());
        LocalDateTime limit = LocalDateTime.now().minusDays(3);
        List<Product> singles = productRepository.findSinglesNeedingUpdate(limit);

        // Precios en lotes de hasta 75 ids (mismo motivo que en el import de CSV): evita un
        // GET por producto cuando puede haber muchos pendientes de actualizar a la vez.
        List<String> distinctScryfallIds = singles.stream()
                .map(Product::getScryfallId)
                .distinct()
                .toList();
        Map<String, ScryfallService.ScryfallCollectionData> dataByCardId =
                scryfallService.getCollectionDataBulk(distinctScryfallIds);

        for (Product p : singles) {
            try {
                // Saltear si el producto fue eliminado mientras esperaba en cola (ej. se vendió)
                if (!productRepository.existsById(p.getId())) {
                    continue;
                }
                String finishShortName = p.getFinish() != null ? p.getFinish().getShortName() : null;
                ScryfallService.ScryfallCollectionData cardData = dataByCardId.get(p.getScryfallId());
                BigDecimal usd = scryfallService.extractPrice(
                        cardData != null ? cardData.getPrices() : null, finishShortName);
                BigDecimal multiplier = p.getCondition() != null
                        ? p.getCondition().getPriceMultiplier()
                        : BigDecimal.ONE;
                BigDecimal ars = convertUsdToArs(usd, multiplier);
                p.setPrice(ars);
                p.setLastPriceUpdate(LocalDateTime.now());
                productRepository.save(p);
            } catch (Exception e) {
                System.err.println("Error actualizando producto " + p.getId());
            }
        }
    }

    // El multiplicador de condición se aplica ANTES del redondeo psicológico, así cada
    // condición obtiene su propio piso de $800 y su propio redondeo a ".99" en vez de
    // escalar un único precio NM ya redondeado.
    private BigDecimal convertUsdToArs(BigDecimal usd, BigDecimal conditionMultiplier) {
        if (usd == null) return BigDecimal.ZERO;

        BigDecimal withMarkup = applyMarkup(usd);
        BigDecimal rate = dollarService.getRate();
        BigDecimal multiplier = conditionMultiplier != null ? conditionMultiplier : BigDecimal.ONE;
        BigDecimal priceArs = withMarkup.multiply(rate).multiply(multiplier);

        return applyRetailPricing(priceArs);
    }

    private BigDecimal applyRetailPricing(BigDecimal price) {
        if (price == null) return BigDecimal.ZERO;

        // Piso mínimo de 800 ARS
        BigDecimal minPrice = new BigDecimal("800");
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

        return buildResponse(p, description, imageUrls);
    }

    // Completa variantTags para singles creados antes de que existiera esta funcionalidad (o
    // cuyo cálculo falló en su momento). Sin job periódico a propósito: el frame de una
    // impresión de Scryfall es inmutable, así que corriendo esto una vez alcanza.
    @Override
    @Transactional
    public int backfillVariantTags() {
        List<Product> singles = productRepository.findSinglesMissingVariantTags();
        if (singles.isEmpty()) {
            return 0;
        }

        List<String> distinctScryfallIds = singles.stream()
                .map(Product::getScryfallId)
                .distinct()
                .toList();
        Map<String, ScryfallService.ScryfallCollectionData> dataByCardId =
                scryfallService.getCollectionDataBulk(distinctScryfallIds);

        int updated = 0;
        for (Product p : singles) {
            ScryfallService.ScryfallCollectionData cardData = dataByCardId.get(p.getScryfallId());
            if (cardData == null) continue;
            p.setVariantTags(cardData.getVariantTags());
            productRepository.save(p);
            updated++;
        }

        log.info("[Backfill variantTags] {} de {} singles pendientes actualizados", updated, singles.size());
        return updated;
    }
}
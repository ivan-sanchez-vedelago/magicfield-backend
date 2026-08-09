package com.magicfield.backend.controller;

import com.magicfield.backend.dto.AvailabilityCheckRequest;
import com.magicfield.backend.dto.AvailabilityCheckResponse;
import com.magicfield.backend.dto.CsvImportResult;
import com.magicfield.backend.dto.PagedProductResponse;
import com.magicfield.backend.dto.ProductRequest;
import com.magicfield.backend.dto.ProductResponse;
import com.magicfield.backend.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public List<ProductResponse> list() {
        return productService.listAll();
    }

    @GetMapping("/paged")
    public ResponseEntity<PagedProductResponse> listPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "") String categories,
            @RequestParam(defaultValue = "NAME_ASC") String sort
    ) {
        int clampedSize = Math.min(Math.max(size, 1), 30);
        List<String> categoryList = categories.isBlank()
                ? List.of()
                : Arrays.asList(categories.split(","));
        PagedProductResponse result = productService.listPaged(search, categoryList, page, clampedSize, sort);
        return ResponseEntity.ok()
                .header("Cache-Control", "public, max-age=60, stale-while-revalidate=120")
                .body(result);
    }

    // Catálogo público: agrupa los singles por (carta+finish) sumando stock entre
    // condiciones/idiomas. Distinto de /paged (que sigue usando el admin, sin agrupar).
    @GetMapping("/catalog")
    public ResponseEntity<PagedProductResponse> listCatalog(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "") String categories,
            @RequestParam(defaultValue = "NAME_ASC") String sort
    ) {
        int clampedSize = Math.min(Math.max(size, 1), 30);
        List<String> categoryList = categories.isBlank()
                ? List.of()
                : Arrays.asList(categories.split(","));
        PagedProductResponse result = productService.listCatalogPaged(search, categoryList, page, clampedSize, sort);
        return ResponseEntity.ok()
                .header("Cache-Control", "public, max-age=60, stale-while-revalidate=120")
                .body(result);
    }

    // Todas las variantes (condición/idioma) en stock de la misma carta+finish que el
    // producto dado — para el selector de variantes de la pantalla de detalle.
    @GetMapping("/{id}/variants")
    public List<ProductResponse> getVariants(@PathVariable UUID id) {
        return productService.getVariants(id);
    }

    // Revalida stock/existencia de los items del carrito antes de avanzar a datos de envío o finalizar la compra.
    @PostMapping("/check-availability")
    public AvailabilityCheckResponse checkAvailability(
            @Valid @RequestBody AvailabilityCheckRequest request
    ) {
        return productService.checkAvailability(request);
    }

    @GetMapping("/restorable")
    public ResponseEntity<PagedProductResponse> listRestorable(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "") String search
    ) {
        int clampedSize = Math.min(Math.max(size, 1), 30);
        PagedProductResponse result = productService.listRestorablePaged(search, page, clampedSize);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ProductResponse getById(@PathVariable UUID id) {
        return productService.getById(id);
    }

    // Variante para admin: no oculta productos agotados (stock = 0), usada para restaurarlos.
    @GetMapping("/{id}/admin")
    public ProductResponse getByIdForAdmin(@PathVariable UUID id) {
        return productService.getByIdIncludingSoldOut(id);
    }

    @PostMapping
    public ResponseEntity<ProductResponse> create(
            @Valid @RequestBody ProductRequest request
    ) {
        ProductResponse created = productService.create(request);
        return ResponseEntity
                .created(URI.create("/api/products/" + created.getId()))
                .body(created);
    }

    @PutMapping("/{id}")
    public ProductResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody ProductRequest request
    ) {
        return productService.update(id, request);
    }

    @PatchMapping("/{id}/stock")
    public ProductResponse updateStock(
            @PathVariable UUID id,
            @RequestParam int stock
    ) {
        return productService.updateStock(id, stock);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id
    ) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/decrease-stock")
    public ResponseEntity<Void> decreaseStock(
            @PathVariable UUID id,
            @RequestParam int quantity
    ) {
        productService.decreaseStock(id, quantity);
        return ResponseEntity.noContent().build();
    }

    // Importación masiva de singles desde un CSV exportado de ManaBox.
    @PostMapping(value = "/import-singles-csv", consumes = "multipart/form-data")
    public ResponseEntity<CsvImportResult> importSinglesCsv(
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        CsvImportResult result = productService.importSinglesFromCsv(file);
        return ResponseEntity.ok(result);
    }
}
package com.magicfield.backend.service;

import com.magicfield.backend.dto.AvailabilityCheckRequest;
import com.magicfield.backend.dto.AvailabilityCheckResponse;
import com.magicfield.backend.dto.CsvImportResult;
import com.magicfield.backend.dto.PagedProductResponse;
import com.magicfield.backend.dto.ProductRequest;
import com.magicfield.backend.dto.ProductResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public interface ProductService {
    List<ProductResponse> listAll();

    AvailabilityCheckResponse checkAvailability(AvailabilityCheckRequest request);

    PagedProductResponse listPaged(String search, List<String> categories, int page, int size, String sort);

    PagedProductResponse listCatalogPaged(String search, List<String> categories, int page, int size, String sort);

    PagedProductResponse listRestorablePaged(String search, int page, int size);

    List<ProductResponse> getVariants(UUID productId);

    ProductResponse getById(UUID id);

    ProductResponse getByIdIncludingSoldOut(UUID id);

    ProductResponse create(ProductRequest request);

    ProductResponse update(UUID id, ProductRequest request);

    ProductResponse updateStock(UUID id, int stock);

    void delete(UUID id);

    void decreaseStock(UUID productId, int quantity);

    CsvImportResult importSinglesFromCsv(MultipartFile file) throws IOException;

    // Completa variantTags para singles creados antes de que existiera esta funcionalidad.
    // Devuelve la cantidad de productos actualizados.
    int backfillVariantTags();
}

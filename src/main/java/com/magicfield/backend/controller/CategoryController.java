package com.magicfield.backend.controller;

import com.magicfield.backend.dto.CategoryResponse;
import com.magicfield.backend.entity.Category;
import com.magicfield.backend.repository.CategoryRepository;
import com.magicfield.backend.repository.ProductRepository;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public CategoryController(CategoryRepository categoryRepository, ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    @GetMapping
    public List<CategoryResponse> listLeaf(
            @RequestParam(required = false, defaultValue = "false") boolean onlyWithProducts
    ) {
        List<Category> all = categoryRepository.findAll();
        List<Category> result = all.stream()
                .filter(c -> c.getParent() != null)
                .collect(Collectors.toList());

        if (onlyWithProducts) {
            Set<Long> withProducts = computeCategoryIdsWithProducts(all);
            result = result.stream()
                    .filter(c -> withProducts.contains(c.getId()))
                    .collect(Collectors.toList());
        }

        return result.stream()
                .map(c -> new CategoryResponse(
                        c.getId(),
                        c.getName(),
                        c.getShortName(),
                        c.getParent().getId()
                ))
                .collect(Collectors.toList());
    }

    // Une "tiene productos directos" con "algún descendiente tiene productos": una categoría
    // padre se oculta solo si todo su subárbol está vacío. La raíz real se autorreferencia
    // (id_parent apunta a sí misma) -- se excluye explícitamente de la lista de hijos de cada
    // categoría para no recursar infinitamente sobre ella misma.
    private Set<Long> computeCategoryIdsWithProducts(List<Category> all) {
        Set<Long> directIds = new HashSet<>(productRepository.findDistinctCategoryIdsWithStock());

        Map<Long, List<Category>> childrenByParentId = new HashMap<>();
        for (Category c : all) {
            Category parent = c.getParent();
            if (parent != null && !parent.getId().equals(c.getId())) {
                childrenByParentId.computeIfAbsent(parent.getId(), k -> new ArrayList<>()).add(c);
            }
        }

        Map<Long, Boolean> memo = new HashMap<>();
        Set<Long> withProducts = new HashSet<>();
        for (Category c : all) {
            if (hasProductsInSubtree(c, directIds, childrenByParentId, memo)) {
                withProducts.add(c.getId());
            }
        }
        return withProducts;
    }

    private boolean hasProductsInSubtree(
            Category cat,
            Set<Long> directIds,
            Map<Long, List<Category>> childrenByParentId,
            Map<Long, Boolean> memo
    ) {
        Boolean cached = memo.get(cat.getId());
        if (cached != null) return cached;

        boolean result = directIds.contains(cat.getId());
        if (!result) {
            for (Category child : childrenByParentId.getOrDefault(cat.getId(), List.of())) {
                if (hasProductsInSubtree(child, directIds, childrenByParentId, memo)) {
                    result = true;
                    break;
                }
            }
        }

        memo.put(cat.getId(), result);
        return result;
    }
}

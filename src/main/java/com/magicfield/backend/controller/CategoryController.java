package com.magicfield.backend.controller;

import com.magicfield.backend.dto.CategoryResponse;
import com.magicfield.backend.entity.Category;
import com.magicfield.backend.repository.CategoryRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryRepository categoryRepository;

    public CategoryController(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @GetMapping
    public List<CategoryResponse> listLeaf() {
        return categoryRepository.findAll()
                .stream()
                .filter(c -> c.getParent() != null)
                .map(c -> new CategoryResponse(
                        c.getId(),
                        c.getName(),
                        c.getShortName(),
                        c.getParent().getId()
                ))
                .collect(Collectors.toList());
    }
}

package com.magicfield.backend.config;

import com.magicfield.backend.entity.Category;
import com.magicfield.backend.repository.CategoryRepository;
import jakarta.transaction.Transactional;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class CategoryInitializer implements CommandLineRunner {

    private final CategoryRepository categoryRepository;
    private final JdbcTemplate jdbcTemplate;

    public CategoryInitializer(CategoryRepository categoryRepository,
                               JdbcTemplate jdbcTemplate) {
        this.categoryRepository = categoryRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (categoryRepository.count() > 0) {
            return;
        }

        Category root = new Category();
        root.setId(0L);
        root.setName("Root");
        root.setShortName("CAT");
        root.setParent(null);
        categoryRepository.save(root);

        Category sin = new Category();
        sin.setId(1L);
        sin.setName("Singles");
        sin.setShortName("SIN");
        sin.setParent(root);
        categoryRepository.save(sin);

        Category psl = new Category();
        psl.setId(2L);
        psl.setName("Sellados");
        psl.setShortName("PSL");
        psl.setParent(root);
        categoryRepository.save(psl);

        Category acc = new Category();
        acc.setId(3L);
        acc.setName("Accesorios");
        acc.setShortName("ACC");
        acc.setParent(root);
        categoryRepository.save(acc);

        jdbcTemplate.update(
            "UPDATE products SET category_id = 1 WHERE type = 'SINGLE' AND category_id IS NULL"
        );
        jdbcTemplate.update(
            "UPDATE products SET category_id = 2 WHERE type = 'SEALED' AND category_id IS NULL"
        );
        jdbcTemplate.update(
            "UPDATE products SET category_id = 3 WHERE type = 'OTHER' AND category_id IS NULL"
        );
    }
}

package com.magicfield.backend.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ImageJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    public ImageJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long insertImage(long productId, byte[] data, String filename, String mimeType) {
        String sql = "INSERT INTO images (data, filename, mime_type, product_id) VALUES (?, ?, ?, ?) RETURNING id";
        return jdbcTemplate.queryForObject(sql, new Object[]{data, filename, mimeType, productId}, Long.class);
    }
}

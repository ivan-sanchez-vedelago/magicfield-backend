package com.magicfield.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "csv_import_receipts")
public class CsvImportReceipt {

    @Id
    @Column(name = "file_hash", length = 64, nullable = false, updatable = false)
    private String fileHash;

    @Column(name = "imported_at", nullable = false, updatable = false)
    private LocalDateTime importedAt;

    protected CsvImportReceipt() {
    }
}

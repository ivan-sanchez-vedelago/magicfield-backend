package com.magicfield.backend.repository;

import com.magicfield.backend.entity.CsvImportReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CsvImportReceiptRepository extends JpaRepository<CsvImportReceipt, String> {

    @Modifying
    @Query(value = """
            INSERT INTO csv_import_receipts (file_hash, imported_at)
            VALUES (:fileHash, CURRENT_TIMESTAMP)
            ON CONFLICT (file_hash) DO NOTHING
            """, nativeQuery = true)
    int claimImport(@Param("fileHash") String fileHash);
}

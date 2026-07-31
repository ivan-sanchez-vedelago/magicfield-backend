package com.magicfield.backend.dto;

import java.util.List;

public class CsvImportResult {

    private int totalRows;
    private int created;
    private int updatedExisting;
    private List<CsvImportRowError> errors;

    public CsvImportResult() {
    }

    public CsvImportResult(int totalRows, int created, int updatedExisting, List<CsvImportRowError> errors) {
        this.totalRows = totalRows;
        this.created = created;
        this.updatedExisting = updatedExisting;
        this.errors = errors;
    }

    public int getTotalRows() { return totalRows; }
    public void setTotalRows(int totalRows) { this.totalRows = totalRows; }

    public int getCreated() { return created; }
    public void setCreated(int created) { this.created = created; }

    public int getUpdatedExisting() { return updatedExisting; }
    public void setUpdatedExisting(int updatedExisting) { this.updatedExisting = updatedExisting; }

    public List<CsvImportRowError> getErrors() { return errors; }
    public void setErrors(List<CsvImportRowError> errors) { this.errors = errors; }
}

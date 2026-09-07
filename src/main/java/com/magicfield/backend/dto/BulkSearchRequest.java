package com.magicfield.backend.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public class BulkSearchRequest {

    @NotEmpty(message = "La lista de búsquedas es obligatoria")
    @Size(max = 200, message = "No se pueden buscar más de 200 líneas a la vez")
    private List<String> queries;

    public BulkSearchRequest() {}

    public List<String> getQueries() { return queries; }
    public void setQueries(List<String> queries) { this.queries = queries; }
}

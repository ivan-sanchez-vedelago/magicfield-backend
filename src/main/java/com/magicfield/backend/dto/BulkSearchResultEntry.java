package com.magicfield.backend.dto;

import java.util.List;

/** Un elemento por cada línea pedida en /api/products/catalog/bulk-search, mismo orden que "queries". */
public class BulkSearchResultEntry {

    private String query;
    private List<ProductResponse> products;

    public BulkSearchResultEntry() {}

    public BulkSearchResultEntry(String query, List<ProductResponse> products) {
        this.query = query;
        this.products = products;
    }

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }

    public List<ProductResponse> getProducts() { return products; }
    public void setProducts(List<ProductResponse> products) { this.products = products; }
}

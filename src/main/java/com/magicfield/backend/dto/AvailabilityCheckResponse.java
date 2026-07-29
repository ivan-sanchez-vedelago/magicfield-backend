package com.magicfield.backend.dto;

import java.util.List;

public class AvailabilityCheckResponse {

    private boolean allAvailable;
    private List<ProductAvailabilityResult> results;

    public AvailabilityCheckResponse() {
    }

    public AvailabilityCheckResponse(boolean allAvailable, List<ProductAvailabilityResult> results) {
        this.allAvailable = allAvailable;
        this.results = results;
    }

    public boolean isAllAvailable() { return allAvailable; }
    public void setAllAvailable(boolean allAvailable) { this.allAvailable = allAvailable; }

    public List<ProductAvailabilityResult> getResults() { return results; }
    public void setResults(List<ProductAvailabilityResult> results) { this.results = results; }
}

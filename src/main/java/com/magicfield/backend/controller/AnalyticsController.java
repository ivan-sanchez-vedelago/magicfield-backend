package com.magicfield.backend.controller;

import com.magicfield.backend.dto.AnalyticsEventRequest;
import com.magicfield.backend.service.AnalyticsService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    // Llamado server-to-server desde el middleware del frontend (un evento por pageview real).
    @PostMapping("/collect")
    public ResponseEntity<Void> collect(@Valid @RequestBody AnalyticsEventRequest request) {
        analyticsService.recordEvent(request);
        return ResponseEntity.noContent().build();
    }
}

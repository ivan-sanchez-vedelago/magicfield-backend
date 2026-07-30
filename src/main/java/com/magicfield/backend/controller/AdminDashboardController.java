package com.magicfield.backend.controller;

import com.magicfield.backend.dto.DashboardStatsResponse;
import com.magicfield.backend.repository.ProductRepository;
import com.magicfield.backend.repository.SalesAuditRepository;
import com.magicfield.backend.service.AnalyticsService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminDashboardController {

    private final SalesAuditRepository salesAuditRepository;
    private final ProductRepository productRepository;
    private final AnalyticsService analyticsService;

    public AdminDashboardController(SalesAuditRepository salesAuditRepository,
                                    ProductRepository productRepository,
                                    AnalyticsService analyticsService) {
        this.salesAuditRepository = salesAuditRepository;
        this.productRepository = productRepository;
        this.analyticsService = analyticsService;
    }

    @GetMapping("/dashboard-stats")
    public DashboardStatsResponse getDashboardStats(
            @RequestParam(defaultValue = "7days") String period) {

        if (!period.matches("^(1day|7days|30days|365days)$")) {
            throw new IllegalArgumentException("Período inválido: " + period);
        }

        DashboardStatsResponse stats = new DashboardStatsResponse();

        var allProducts = productRepository.findAll();
        stats.setTotalProducts(allProducts.size());
        stats.setTotalInventoryValue(
            allProducts.stream()
                .map(p -> p.getPrice().multiply(BigDecimal.valueOf(p.getStock())))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
        );

        stats.setPendingOrders(salesAuditRepository.countDistinctOrdersByStatus("PENDING"));
        stats.setCompletedOrders(salesAuditRepository.countDistinctOrdersByStatus("COMPLETED"));
        stats.setCancelledOrders(salesAuditRepository.countDistinctOrdersByStatus("CANCELLED"));

        stats.setSiteAnalytics(analyticsService.getAnalytics(period));
        stats.setPeriod(period);

        return stats;
    }
}

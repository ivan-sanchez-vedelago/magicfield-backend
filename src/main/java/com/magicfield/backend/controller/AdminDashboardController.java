package com.magicfield.backend.controller;

import com.magicfield.backend.dto.DashboardStatsResponse;
import com.magicfield.backend.repository.ProductRepository;
import com.magicfield.backend.repository.SalesAuditRepository;
import com.magicfield.backend.service.UmamiAnalyticsService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminDashboardController {

    private final SalesAuditRepository salesAuditRepository;
    private final ProductRepository productRepository;
    private final UmamiAnalyticsService umamiAnalyticsService;

    public AdminDashboardController(SalesAuditRepository salesAuditRepository,
                                    ProductRepository productRepository,
                                    UmamiAnalyticsService umamiAnalyticsService) {
        this.salesAuditRepository = salesAuditRepository;
        this.productRepository = productRepository;
        this.umamiAnalyticsService = umamiAnalyticsService;
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

        stats.setUmamiAnalytics(umamiAnalyticsService.getAnalytics(period));
        stats.setPeriod(period);

        return stats;
    }
}

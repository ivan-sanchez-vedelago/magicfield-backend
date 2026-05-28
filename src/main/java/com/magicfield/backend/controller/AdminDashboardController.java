package com.magicfield.backend.controller;

import com.magicfield.backend.dto.DashboardStatsResponse;
import com.magicfield.backend.repository.ProductRepository;
import com.magicfield.backend.repository.SalesAuditRepository;
import com.magicfield.backend.service.VercelAnalyticsService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminDashboardController {

    private final SalesAuditRepository salesAuditRepository;
    private final ProductRepository productRepository;
    private final VercelAnalyticsService vercelAnalyticsService;

    public AdminDashboardController(SalesAuditRepository salesAuditRepository,
                                    ProductRepository productRepository,
                                    VercelAnalyticsService vercelAnalyticsService) {
        this.salesAuditRepository = salesAuditRepository;
        this.productRepository = productRepository;
        this.vercelAnalyticsService = vercelAnalyticsService;
    }

    @GetMapping("/dashboard-stats")
    public DashboardStatsResponse getDashboardStats(@RequestParam(defaultValue = "7days") String period) {
        // Validate period
        if (!period.matches("^(1day|7days|30days|365days)$")) {
            throw new IllegalArgumentException("Período inválido. Use: 1day, 7days, 30days, 365days");
        }

        DashboardStatsResponse stats = new DashboardStatsResponse();

        // Inventory stats from products table
        var allProducts = productRepository.findAll();
        stats.setTotalProducts(allProducts.size());
        stats.setTotalInventoryValue(
            allProducts.stream()
                .map(p -> p.getPrice().multiply(BigDecimal.valueOf(p.getStock())))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
        );

        // Status counts
        stats.setPendingOrders(salesAuditRepository.countDistinctOrdersByStatus("PENDING"));
        stats.setCompletedOrders(salesAuditRepository.countDistinctOrdersByStatus("COMPLETED"));
        stats.setCancelledOrders(salesAuditRepository.countDistinctOrdersByStatus("CANCELLED"));

        // Vercel Analytics
        stats.setVercelAnalytics(vercelAnalyticsService.getAnalytics(period));
        stats.setPeriod(period);

        return stats;
    }
}

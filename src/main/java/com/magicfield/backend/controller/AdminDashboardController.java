package com.magicfield.backend.controller;

import com.magicfield.backend.dto.DashboardStatsResponse;
import com.magicfield.backend.dto.TopProductDto;
import com.magicfield.backend.repository.ProductRepository;
import com.magicfield.backend.repository.SalesAuditRepository;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminDashboardController {

    private final SalesAuditRepository salesAuditRepository;
    private final ProductRepository productRepository;

    public AdminDashboardController(SalesAuditRepository salesAuditRepository,
                                    ProductRepository productRepository) {
        this.salesAuditRepository = salesAuditRepository;
        this.productRepository = productRepository;
    }

    @GetMapping("/dashboard-stats")
    public DashboardStatsResponse getDashboardStats() {
        DashboardStatsResponse stats = new DashboardStatsResponse();

        // Inventory stats from products table
        var allProducts = productRepository.findAll();
        stats.setTotalProducts(allProducts.size());
        stats.setTotalStock(allProducts.stream().mapToLong(p -> p.getStock()).sum());
        stats.setTotalInventoryValue(
            allProducts.stream()
                .map(p -> p.getPrice().multiply(BigDecimal.valueOf(p.getStock())))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
        );
        stats.setOutOfStockProducts(allProducts.stream().filter(p -> p.getStock() == 0).count());

        // Time boundaries
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        LocalDateTime startOfWeek = LocalDate.now()
                .with(DayOfWeek.MONDAY)
                .atStartOfDay();

        // Orders & revenue
        stats.setOrdersToday(salesAuditRepository.countDistinctOrdersSince(startOfToday));
        stats.setRevenueToday(salesAuditRepository.sumRevenueSince(startOfToday));
        stats.setOrdersThisWeek(salesAuditRepository.countDistinctOrdersSince(startOfWeek));
        stats.setRevenueThisWeek(salesAuditRepository.sumRevenueSince(startOfWeek));

        // Status counts
        stats.setPendingOrders(salesAuditRepository.countDistinctOrdersByStatus("PENDING"));
        stats.setCompletedOrders(salesAuditRepository.countDistinctOrdersByStatus("COMPLETED"));
        stats.setCancelledOrders(salesAuditRepository.countDistinctOrdersByStatus("CANCELLED"));

        // Top 5 products
        List<Object[]> topRaw = salesAuditRepository.findTopProducts();
        List<TopProductDto> topProducts = topRaw.stream()
                .map(row -> new TopProductDto(
                        (UUID) row[0],
                        (String) row[1],
                        ((Number) row[2]).longValue()
                ))
                .collect(Collectors.toList());
        stats.setTopProducts(topProducts);

        return stats;
    }
}

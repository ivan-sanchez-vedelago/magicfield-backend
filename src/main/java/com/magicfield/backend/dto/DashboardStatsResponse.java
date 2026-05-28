package com.magicfield.backend.dto;

import java.math.BigDecimal;
import java.util.List;

public class DashboardStatsResponse {

    // Inventory
    private long totalProducts;
    private long totalStock;
    private BigDecimal totalInventoryValue;
    private long outOfStockProducts;

    // Orders today
    private long ordersToday;
    private BigDecimal revenueToday;

    // Orders this week
    private long ordersThisWeek;
    private BigDecimal revenueThisWeek;

    // Order status counts
    private long pendingOrders;
    private long completedOrders;
    private long cancelledOrders;

    // Top products
    private List<TopProductDto> topProducts;

    public long getTotalProducts() { return totalProducts; }
    public void setTotalProducts(long totalProducts) { this.totalProducts = totalProducts; }

    public long getTotalStock() { return totalStock; }
    public void setTotalStock(long totalStock) { this.totalStock = totalStock; }

    public BigDecimal getTotalInventoryValue() { return totalInventoryValue; }
    public void setTotalInventoryValue(BigDecimal totalInventoryValue) { this.totalInventoryValue = totalInventoryValue; }

    public long getOutOfStockProducts() { return outOfStockProducts; }
    public void setOutOfStockProducts(long outOfStockProducts) { this.outOfStockProducts = outOfStockProducts; }

    public long getOrdersToday() { return ordersToday; }
    public void setOrdersToday(long ordersToday) { this.ordersToday = ordersToday; }

    public BigDecimal getRevenueToday() { return revenueToday; }
    public void setRevenueToday(BigDecimal revenueToday) { this.revenueToday = revenueToday; }

    public long getOrdersThisWeek() { return ordersThisWeek; }
    public void setOrdersThisWeek(long ordersThisWeek) { this.ordersThisWeek = ordersThisWeek; }

    public BigDecimal getRevenueThisWeek() { return revenueThisWeek; }
    public void setRevenueThisWeek(BigDecimal revenueThisWeek) { this.revenueThisWeek = revenueThisWeek; }

    public long getPendingOrders() { return pendingOrders; }
    public void setPendingOrders(long pendingOrders) { this.pendingOrders = pendingOrders; }

    public long getCompletedOrders() { return completedOrders; }
    public void setCompletedOrders(long completedOrders) { this.completedOrders = completedOrders; }

    public long getCancelledOrders() { return cancelledOrders; }
    public void setCancelledOrders(long cancelledOrders) { this.cancelledOrders = cancelledOrders; }

    public List<TopProductDto> getTopProducts() { return topProducts; }
    public void setTopProducts(List<TopProductDto> topProducts) { this.topProducts = topProducts; }
}

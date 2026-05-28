package com.magicfield.backend.dto;

import java.math.BigDecimal;
import java.util.List;

public class DashboardStatsResponse {

    // Inventory
    private long totalProducts;
    private BigDecimal totalInventoryValue;

    // Order status counts
    private long pendingOrders;
    private long completedOrders;
    private long cancelledOrders;

    // Vercel Analytics
    private VercelAnalyticsDTO vercelAnalytics;
    
    // Period
    private String period;

    public long getTotalProducts() { return totalProducts; }
    public void setTotalProducts(long totalProducts) { this.totalProducts = totalProducts; }

    public BigDecimal getTotalInventoryValue() { return totalInventoryValue; }
    public void setTotalInventoryValue(BigDecimal totalInventoryValue) { this.totalInventoryValue = totalInventoryValue; }

    public long getPendingOrders() { return pendingOrders; }
    public void setPendingOrders(long pendingOrders) { this.pendingOrders = pendingOrders; }

    public long getCompletedOrders() { return completedOrders; }
    public void setCompletedOrders(long completedOrders) { this.completedOrders = completedOrders; }

    public long getCancelledOrders() { return cancelledOrders; }
    public void setCancelledOrders(long cancelledOrders) { this.cancelledOrders = cancelledOrders; }

    public VercelAnalyticsDTO getVercelAnalytics() { return vercelAnalytics; }
    public void setVercelAnalytics(VercelAnalyticsDTO vercelAnalytics) { this.vercelAnalytics = vercelAnalytics; }

    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }
}

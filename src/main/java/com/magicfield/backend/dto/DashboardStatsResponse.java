package com.magicfield.backend.dto;

import java.math.BigDecimal;

public class DashboardStatsResponse {

    private long totalProducts;
    private BigDecimal totalInventoryValue;
    private long pendingOrders;
    private long completedOrders;
    private long cancelledOrders;
    private UmamiAnalyticsDTO umamiAnalytics;
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

    public UmamiAnalyticsDTO getUmamiAnalytics() { return umamiAnalytics; }
    public void setUmamiAnalytics(UmamiAnalyticsDTO umamiAnalytics) { this.umamiAnalytics = umamiAnalytics; }

    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }
}

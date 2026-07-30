package com.magicfield.backend.dto;

import java.util.List;

public class SiteAnalyticsDTO {

    public static class MetricItem {
        private String x;
        private int y;

        public MetricItem() {}

        public MetricItem(String x, int y) {
            this.x = x;
            this.y = y;
        }

        public String getX() { return x; }
        public void setX(String x) { this.x = x; }
        public int getY() { return y; }
        public void setY(int y) { this.y = y; }
    }

    private long pageViews;
    private long sessions;
    private double bounceRate;
    private List<MetricItem> topPages;
    private List<MetricItem> referrers;
    private List<MetricItem> countries;
    private List<MetricItem> browsers;
    private List<MetricItem> devices;
    private List<MetricItem> operatingSystems;
    private boolean available = true;
    private String unavailableReason;

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }

    public String getUnavailableReason() { return unavailableReason; }
    public void setUnavailableReason(String unavailableReason) { this.unavailableReason = unavailableReason; }

    public long getPageViews() { return pageViews; }
    public void setPageViews(long pageViews) { this.pageViews = pageViews; }

    public long getSessions() { return sessions; }
    public void setSessions(long sessions) { this.sessions = sessions; }

    public double getBounceRate() { return bounceRate; }
    public void setBounceRate(double bounceRate) { this.bounceRate = bounceRate; }

    public List<MetricItem> getTopPages() { return topPages; }
    public void setTopPages(List<MetricItem> topPages) { this.topPages = topPages; }

    public List<MetricItem> getReferrers() { return referrers; }
    public void setReferrers(List<MetricItem> referrers) { this.referrers = referrers; }

    public List<MetricItem> getCountries() { return countries; }
    public void setCountries(List<MetricItem> countries) { this.countries = countries; }

    public List<MetricItem> getBrowsers() { return browsers; }
    public void setBrowsers(List<MetricItem> browsers) { this.browsers = browsers; }

    public List<MetricItem> getDevices() { return devices; }
    public void setDevices(List<MetricItem> devices) { this.devices = devices; }

    public List<MetricItem> getOperatingSystems() { return operatingSystems; }
    public void setOperatingSystems(List<MetricItem> operatingSystems) { this.operatingSystems = operatingSystems; }
}

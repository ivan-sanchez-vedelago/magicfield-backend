package com.magicfield.backend.dto;

import java.util.List;

public class VercelAnalyticsDTO {

    // Main metrics
    private long totalVisitors;
    private long pageViews;
    private double bounceRate;

    // Details
    private List<PageViewDto> topPages;
    private List<ReferrerDto> topReferrers;
    private List<CountryDto> topCountries;
    private List<DeviceDto> devices;
    private List<BrowserDto> browsers;
    private List<OSDto> operatingSystems;

    public VercelAnalyticsDTO() {}

    public long getTotalVisitors() { return totalVisitors; }
    public void setTotalVisitors(long totalVisitors) { this.totalVisitors = totalVisitors; }

    public long getPageViews() { return pageViews; }
    public void setPageViews(long pageViews) { this.pageViews = pageViews; }

    public double getBounceRate() { return bounceRate; }
    public void setBounceRate(double bounceRate) { this.bounceRate = bounceRate; }

    public List<PageViewDto> getTopPages() { return topPages; }
    public void setTopPages(List<PageViewDto> topPages) { this.topPages = topPages; }

    public List<ReferrerDto> getTopReferrers() { return topReferrers; }
    public void setTopReferrers(List<ReferrerDto> topReferrers) { this.topReferrers = topReferrers; }

    public List<CountryDto> getTopCountries() { return topCountries; }
    public void setTopCountries(List<CountryDto> topCountries) { this.topCountries = topCountries; }

    public List<DeviceDto> getDevices() { return devices; }
    public void setDevices(List<DeviceDto> devices) { this.devices = devices; }

    public List<BrowserDto> getBrowsers() { return browsers; }
    public void setBrowsers(List<BrowserDto> browsers) { this.browsers = browsers; }

    public List<OSDto> getOperatingSystems() { return operatingSystems; }
    public void setOperatingSystems(List<OSDto> operatingSystems) { this.operatingSystems = operatingSystems; }

    // Inner DTOs
    public static class PageViewDto {
        private String page;
        private long visitors;

        public PageViewDto(String page, long visitors) {
            this.page = page;
            this.visitors = visitors;
        }

        public String getPage() { return page; }
        public void setPage(String page) { this.page = page; }

        public long getVisitors() { return visitors; }
        public void setVisitors(long visitors) { this.visitors = visitors; }
    }

    public static class ReferrerDto {
        private String referrer;
        private long count;

        public ReferrerDto(String referrer, long count) {
            this.referrer = referrer;
            this.count = count;
        }

        public String getReferrer() { return referrer; }
        public void setReferrer(String referrer) { this.referrer = referrer; }

        public long getCount() { return count; }
        public void setCount(long count) { this.count = count; }
    }

    public static class CountryDto {
        private String country;
        private String countryCode;
        private long visitors;

        public CountryDto(String country, String countryCode, long visitors) {
            this.country = country;
            this.countryCode = countryCode;
            this.visitors = visitors;
        }

        public String getCountry() { return country; }
        public void setCountry(String country) { this.country = country; }

        public String getCountryCode() { return countryCode; }
        public void setCountryCode(String countryCode) { this.countryCode = countryCode; }

        public long getVisitors() { return visitors; }
        public void setVisitors(long visitors) { this.visitors = visitors; }
    }

    public static class DeviceDto {
        private String device;
        private long count;

        public DeviceDto(String device, long count) {
            this.device = device;
            this.count = count;
        }

        public String getDevice() { return device; }
        public void setDevice(String device) { this.device = device; }

        public long getCount() { return count; }
        public void setCount(long count) { this.count = count; }
    }

    public static class BrowserDto {
        private String browser;
        private long count;

        public BrowserDto(String browser, long count) {
            this.browser = browser;
            this.count = count;
        }

        public String getBrowser() { return browser; }
        public void setBrowser(String browser) { this.browser = browser; }

        public long getCount() { return count; }
        public void setCount(long count) { this.count = count; }
    }

    public static class OSDto {
        private String os;
        private long count;

        public OSDto(String os, long count) {
            this.os = os;
            this.count = count;
        }

        public String getOs() { return os; }
        public void setOs(String os) { this.os = os; }

        public long getCount() { return count; }
        public void setCount(long count) { this.count = count; }
    }
}

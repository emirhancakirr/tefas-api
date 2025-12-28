package com.tefasfundapi.tefasFundAPI.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Playwright ve TEFAS client konfigürasyonu.
 * Tüm hard-coded değerler burada merkezi olarak yönetilir.
 */
@Component
@ConfigurationProperties(prefix = "tefas.playwright")
public class PlaywrightConfig {

    // Timeout değerleri (milisaniye)
    private int elementWaitTimeoutMs = 10000;
    private int navigationTimeoutMs = 30000;
    private int buttonClickTimeoutMs = 5000;
    private int wafWaitMs = 10000;
    private int pageLoadWaitMs = 2000;

    // Thread.sleep() değerleri (milisaniye)
    private int tableLoadWaitMs = 1500;
    private int tableDataExtractionWaitMs = 1000;
    private int filterApplyWaitMs = 1000;
    private int inputClearWaitMs = 200;
    private int inputFillWaitMs = 300;
    private int buttonClickWaitMs = 500;
    private int retryWaitMs = 200;

    // Browser ayarları
    private boolean headless = true;
    private String userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
    private int viewportWidth = 1920;
    private int viewportHeight = 1080;
    private String locale = "tr-TR";
    private String timezone = "Europe/Istanbul";

    // TEFAS URL'leri
    private String baseUrl = "https://www.tefas.gov.tr";
    private String historyPageUrl = "/TarihselVeriler.aspx";
    private String comparisonPageUrl = "/FonKarsilastirma.aspx";
    private String historyApiEndpoint = "/api/DB/BindHistoryInfo";
    private String comparisonApiEndpoint = "/api/DB/BindComparisonFundReturns";

    // Selectors
    private Selectors selectors = new Selectors();

    // Getters and Setters
    public int getElementWaitTimeoutMs() {
        return elementWaitTimeoutMs;
    }

    public void setElementWaitTimeoutMs(int elementWaitTimeoutMs) {
        this.elementWaitTimeoutMs = elementWaitTimeoutMs;
    }

    public int getNavigationTimeoutMs() {
        return navigationTimeoutMs;
    }

    public void setNavigationTimeoutMs(int navigationTimeoutMs) {
        this.navigationTimeoutMs = navigationTimeoutMs;
    }

    public int getButtonClickTimeoutMs() {
        return buttonClickTimeoutMs;
    }

    public void setButtonClickTimeoutMs(int buttonClickTimeoutMs) {
        this.buttonClickTimeoutMs = buttonClickTimeoutMs;
    }

    public int getWafWaitMs() {
        return wafWaitMs;
    }

    public void setWafWaitMs(int wafWaitMs) {
        this.wafWaitMs = wafWaitMs;
    }

    public int getPageLoadWaitMs() {
        return pageLoadWaitMs;
    }

    public void setPageLoadWaitMs(int pageLoadWaitMs) {
        this.pageLoadWaitMs = pageLoadWaitMs;
    }

    public int getTableLoadWaitMs() {
        return tableLoadWaitMs;
    }

    public void setTableLoadWaitMs(int tableLoadWaitMs) {
        this.tableLoadWaitMs = tableLoadWaitMs;
    }

    public int getTableDataExtractionWaitMs() {
        return tableDataExtractionWaitMs;
    }

    public void setTableDataExtractionWaitMs(int tableDataExtractionWaitMs) {
        this.tableDataExtractionWaitMs = tableDataExtractionWaitMs;
    }

    public int getFilterApplyWaitMs() {
        return filterApplyWaitMs;
    }

    public void setFilterApplyWaitMs(int filterApplyWaitMs) {
        this.filterApplyWaitMs = filterApplyWaitMs;
    }

    public int getInputClearWaitMs() {
        return inputClearWaitMs;
    }

    public void setInputClearWaitMs(int inputClearWaitMs) {
        this.inputClearWaitMs = inputClearWaitMs;
    }

    public int getInputFillWaitMs() {
        return inputFillWaitMs;
    }

    public void setInputFillWaitMs(int inputFillWaitMs) {
        this.inputFillWaitMs = inputFillWaitMs;
    }

    public int getButtonClickWaitMs() {
        return buttonClickWaitMs;
    }

    public void setButtonClickWaitMs(int buttonClickWaitMs) {
        this.buttonClickWaitMs = buttonClickWaitMs;
    }

    public int getRetryWaitMs() {
        return retryWaitMs;
    }

    public void setRetryWaitMs(int retryWaitMs) {
        this.retryWaitMs = retryWaitMs;
    }

    public boolean isHeadless() {
        return headless;
    }

    public void setHeadless(boolean headless) {
        this.headless = headless;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public int getViewportWidth() {
        return viewportWidth;
    }

    public void setViewportWidth(int viewportWidth) {
        this.viewportWidth = viewportWidth;
    }

    public int getViewportHeight() {
        return viewportHeight;
    }

    public void setViewportHeight(int viewportHeight) {
        this.viewportHeight = viewportHeight;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getHistoryPageUrl() {
        return historyPageUrl;
    }

    public void setHistoryPageUrl(String historyPageUrl) {
        this.historyPageUrl = historyPageUrl;
    }

    public String getComparisonPageUrl() {
        return comparisonPageUrl;
    }

    public void setComparisonPageUrl(String comparisonPageUrl) {
        this.comparisonPageUrl = comparisonPageUrl;
    }

    public String getHistoryApiEndpoint() {
        return historyApiEndpoint;
    }

    public void setHistoryApiEndpoint(String historyApiEndpoint) {
        this.historyApiEndpoint = historyApiEndpoint;
    }

    public String getComparisonApiEndpoint() {
        return comparisonApiEndpoint;
    }

    public void setComparisonApiEndpoint(String comparisonApiEndpoint) {
        this.comparisonApiEndpoint = comparisonApiEndpoint;
    }

    public Selectors getSelectors() {
        return selectors;
    }

    public void setSelectors(Selectors selectors) {
        this.selectors = selectors;
    }

    // Helper methods
    public String getHistoryReferer() {
        return baseUrl + historyPageUrl;
    }

    public String getComparisonReferer() {
        return baseUrl + comparisonPageUrl;
    }

    public String getHistoryApiUrl() {
        return baseUrl + historyApiEndpoint;
    }

    public String getComparisonApiUrl() {
        return baseUrl + comparisonApiEndpoint;
    }

    /**
     * CSS selector'lar için nested configuration class.
     */
    public static class Selectors {
        private String startDate = "#TextBoxStartDate, input[name*='TextBoxStartDate']";
        private String endDate = "#TextBoxEndDate, input[name*='TextBoxEndDate']";
        private String fundCodeFilter = "input[type='search'][aria-controls='table_general_info']";
        private String searchButton = "#ButtonSearchDates, input[name*='ButtonSearchDates'], input[value='Görüntüle']";

        public String getStartDate() {
            return startDate;
        }

        public void setStartDate(String startDate) {
            this.startDate = startDate;
        }

        public String getEndDate() {
            return endDate;
        }

        public void setEndDate(String endDate) {
            this.endDate = endDate;
        }

        public String getFundCodeFilter() {
            return fundCodeFilter;
        }

        public void setFundCodeFilter(String fundCodeFilter) {
            this.fundCodeFilter = fundCodeFilter;
        }

        public String getSearchButton() {
            return searchButton;
        }

        public void setSearchButton(String searchButton) {
            this.searchButton = searchButton;
        }
    }
}

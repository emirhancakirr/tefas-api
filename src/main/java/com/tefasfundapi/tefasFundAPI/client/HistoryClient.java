package com.tefasfundapi.tefasFundAPI.client;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import com.tefasfundapi.tefasFundAPI.config.PlaywrightConfig;
import com.tefasfundapi.tefasFundAPI.exception.TefasClientException;
import com.tefasfundapi.tefasFundAPI.exception.TefasTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * TEFAS tarihsel veri (NAV) istemcisi.
 * WAF bypass için sayfa üzerinden response dinleme yaklaşımını kullanır.
 */
@Component
public class HistoryClient {
    private static final Logger log = LoggerFactory.getLogger(HistoryClient.class);

    private final PlaywrightConfig config;

    public HistoryClient(PlaywrightConfig config) {
        this.config = config;
    }

    /**
     * Tek fon ve tarih aralığı için fiyat/NAV+diğer sütunlar JSON'u döner.
     * Sayfa üzerinden response dinleme yaklaşımını kullanır (WAF bypass için).
     * 
     * @param fundCode Fon kodu (örn: "AAK")
     * @param start    Başlangıç tarihi
     * @param end      Bitiş tarihi
     * @return JSON string
     */
    public String fetchHistoryJson(String fundCode, LocalDate start, LocalDate end) {
        try (Playwright pw = Playwright.create()) {
            log.debug("fetchHistoryJson started for fundCode={}, start={}, end={}", fundCode, start, end);
            // Launch browser with configuration
            BrowserType.LaunchOptions launchOptions = PlaywrightHelper.createLaunchOptions(config)
                    .setHeadless(config.isHeadless());
            try (Browser browser = pw.chromium().launch(launchOptions)) {
                BrowserContext ctx = browser.newContext(PlaywrightHelper.createContextOptions(config));
                try {
                    Page page = ctx.newPage();

                    // Request logging for debugging (optional, can remove if not needed)
                    setupRequestLogger(page);

                    navigateAndWaitForWaf(page);

                    // Step 1: Fill date fields
                    fillDateFields(page, start, end);

                    // Step 2: Fill fund code filter (before clicking button)
                    fillFundCodeFilter(page, fundCode);

                    // Step 3: Click "Görüntüle" button to load table
                    clickSearchButton(page);

                    // Step 4: Wait for table to load
                    waitForTableToLoad(page);

                    // Step 5: Extract filtered data from table and transform to API format
                    String tableJson = extractTableDataAsJson(page, fundCode);
                    return tableJson;
                } finally {
                    ctx.close();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TefasClientException("TEFAS/fetchHistoryJson interrupted", e);
        } catch (com.microsoft.playwright.TimeoutError e) {
            throw new TefasTimeoutException("fetchHistoryJson", config.getElementWaitTimeoutMs(), e);
        } catch (TefasClientException e) {
            // Re-throw custom exceptions as-is
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error in fetchHistoryJson for fundCode={}, start={}, end={}", fundCode, start, end,
                    e);
            throw new TefasClientException("TEFAS/fetchHistoryJson çağrısı başarısız: " + e.getMessage(), e);
        }
    }

    /**
     * Extracts raw table data from DOM and transforms it to API response format.
     * JavaScript extracts raw text, Java transforms it via TableDataTransformer.
     */
    private String extractTableDataAsJson(Page page, String fundCode) {
        try {
            // Wait for filtered rows
            page.waitForSelector("tbody tr:not(.dataTables_empty)",
                    new Page.WaitForSelectorOptions().setTimeout(config.getElementWaitTimeoutMs()));

            // Wait for DataTables to fully render and apply filter
            Thread.sleep(config.getTableDataExtractionWaitMs());

            // Extract raw data (simple JavaScript - just get text content)
            String rawJson = (String) page.evaluate("""
                    (function() {
                        const rows = document.querySelectorAll('tbody tr');
                        const data = [];

                        rows.forEach(row => {
                            if (row.classList.contains('dataTables_empty')) {
                                return; // Skip empty rows
                            }

                            const cells = row.querySelectorAll('td');
                            if (cells.length >= 7) {
                                data.push({
                                    tarih: cells[0].textContent.trim(),
                                    fonKodu: cells[1].textContent.trim(),
                                    fonUnvan: cells[2].textContent.trim(),
                                    fiyat: cells[3].textContent.trim(),
                                    paySayisi: cells[4].textContent.trim(),
                                    kisiSayisi: cells[5].textContent.trim(),
                                    toplamDeger: cells[6].textContent.trim()
                                });
                            }
                        });

                        return JSON.stringify({ data: data });
                    })();
                    """);

            log.debug("Raw extracted JSON length: {}", rawJson.length());

            // Transform using helper class
            return TableDataTransformer.transformToApiFormat(rawJson, fundCode);

        } catch (Exception e) {
            throw new TefasClientException("Failed to extract table data: " + e.getMessage(), e);
        }
    }

    /**
     * Navigates to TEFAS page and waits for WAF challenge to complete.
     */
    private void navigateAndWaitForWaf(Page page) throws InterruptedException {
        PlaywrightHelper.navigateForSession(page, config.getHistoryReferer(), config);
        page.waitForLoadState(LoadState.NETWORKIDLE);
    }

    /**
     * Sets up request logging for debugging purposes.
     * Logs request URL, method, and post data for API calls.
     */
    private void setupRequestLogger(Page page) {
        page.onRequest(request -> {
            if (request.url().contains(config.getHistoryApiEndpoint())) {
                log.debug("=== REQUEST DEBUG ===");
                log.debug("URL: {}", request.url());
                log.debug("Method: {}", request.method());
                log.debug("Post Data: {}", request.postData());
                log.debug("====================");
            }
        });
    }

    /**
     * Fills only the date fields (start and end dates).
     */
    private void fillDateFields(Page page, LocalDate start, LocalDate end) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        String startDateStr = start.format(formatter);
        String endDateStr = end.format(formatter);

        try {
            // Wait for form elements to be visible
            page.waitForSelector(config.getSelectors().getStartDate(),
                    new Page.WaitForSelectorOptions().setTimeout(config.getElementWaitTimeoutMs()));

            // Fill start date (TextBoxStartDate)
            PlaywrightHelper.fillInputField(page, config.getSelectors().getStartDate(), startDateStr, "Start Date",
                    config);

            // Fill end date (TextBoxEndDate)
            PlaywrightHelper.fillInputField(page, config.getSelectors().getEndDate(), endDateStr, "End Date", config);

            log.debug("Date fields filled: startDate={}, endDate={}", startDateStr, endDateStr);
        } catch (Exception e) {
            throw new TefasClientException("Failed to fill date fields: " + e.getMessage(), e);
        }
    }

    /**
     * Waits for the data table to fully load with actual data rows.
     */
    private void waitForTableToLoad(Page page) {
        try {
            // Wait for table structure
            page.waitForSelector("table tbody",
                    new Page.WaitForSelectorOptions().setTimeout(config.getElementWaitTimeoutMs()));

            // Wait for data rows to appear (not empty)
            page.waitForSelector("tbody tr:not(.dataTables_empty)",
                    new Page.WaitForSelectorOptions().setTimeout(config.getElementWaitTimeoutMs()));

            // Wait for at least one row with valid data (date format check)
            // Use evaluate with retry logic instead of waitForFunction
            long startTime = System.currentTimeMillis();
            boolean hasValidData = false;

            while (!hasValidData && (System.currentTimeMillis() - startTime) < config.getElementWaitTimeoutMs()) {
                Object result = page.evaluate("""
                        (function() {
                            const rows = document.querySelectorAll('tbody tr:not(.dataTables_empty)');
                            for (let row of rows) {
                                const cells = row.querySelectorAll('td');
                                if (cells.length >= 7) {
                                    const dateCell = cells[0].textContent.trim();
                                    // Check if it's a date in dd.MM.yyyy format
                                    if (dateCell && /^\\d{2}\\.\\d{2}\\.\\d{4}$/.test(dateCell)) {
                                        return true;
                                    }
                                }
                            }
                            return false;
                        })();
                        """);

                hasValidData = Boolean.TRUE.equals(result);

                if (!hasValidData) {
                    Thread.sleep(config.getRetryWaitMs());
                }
            }

            if (!hasValidData) {
                throw new TefasTimeoutException("waitForTableToLoad", config.getElementWaitTimeoutMs());
            }

            // Additional wait for DataTables to finish processing
            Thread.sleep(config.getTableLoadWaitMs());

            log.info("Table loaded successfully with data rows");

        } catch (TefasTimeoutException e) {
            // Re-throw timeout exceptions as-is
            throw e;
        } catch (Exception e) {
            throw new TefasClientException("Failed to wait for table to load: " + e.getMessage(), e);
        }
    }

    /**
     * Fills the fund code filter input (DataTables search box).
     * This filters the table client-side after it's loaded.
     */
    private void fillFundCodeFilter(Page page, String fundCode) {
        if (fundCode == null || fundCode.isBlank()) {
            log.debug("Fund code is empty, skipping filter");
            return;
        }

        try {
            // Wait for filter input to be available
            page.waitForSelector(config.getSelectors().getFundCodeFilter(),
                    new Page.WaitForSelectorOptions().setTimeout(config.getElementWaitTimeoutMs()));

            // Fill fund code in filter/search input (DataTables filter)
            PlaywrightHelper.fillInputField(page, config.getSelectors().getFundCodeFilter(), fundCode.trim(),
                    "Fund Code Filter", config);

            // Wait for DataTables to apply the filter
            Thread.sleep(config.getFilterApplyWaitMs());

            log.debug("Fund code filter filled: {}", fundCode);
        } catch (Exception e) {
            throw new TefasClientException("Failed to fill fund code filter: " + e.getMessage(), e);
        }
    }

    /**
     * Clicks the search/compare button to trigger API call.
     * Uses actual button ID from TEFAS HTML: ButtonSearchDates
     */
    private void clickSearchButton(Page page) {
        try {
            String searchButtonSelector = config.getSelectors().getSearchButton();
            // Wait for button to be visible and clickable
            page.waitForSelector(searchButtonSelector,
                    new Page.WaitForSelectorOptions()
                            .setTimeout(config.getElementWaitTimeoutMs()));

            // Click the button
            page.click(searchButtonSelector,
                    new Page.ClickOptions().setTimeout(config.getButtonClickTimeoutMs()));

            log.debug("Clicked search button: {}", searchButtonSelector);
            Thread.sleep(config.getButtonClickWaitMs());

        } catch (Exception e) {
            log.warn("Could not click button with selector '{}': {}", config.getSelectors().getSearchButton(),
                    e.getMessage());

            // Fallback: Try JavaScript click on button with onclick handler
            try {
                log.debug("Trying JavaScript click fallback...");
                boolean clicked = page.evaluate("""
                        (function() {
                            var btn = document.getElementById('ButtonSearchDates') ||
                                      document.querySelector('input[value="Karşılaştır"]') ||
                                      document.querySelector('input[name*="ButtonSearchDates"]');
                            if (btn) {
                                btn.click();
                                return true;
                            }
                            return false;
                        })();
                        """).toString().equals("true");

                if (clicked) {
                    log.debug("JavaScript click successful");
                    Thread.sleep(config.getButtonClickWaitMs());
                } else {
                    throw new TefasClientException("Could not find or click search button");
                }
            } catch (Exception jsError) {
                throw new TefasClientException("Failed to click search button: " + e.getMessage() +
                        ", JavaScript fallback also failed: " + jsError.getMessage(), jsError);
            }
        }
    }
}
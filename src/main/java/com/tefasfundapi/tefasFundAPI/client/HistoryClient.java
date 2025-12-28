package com.tefasfundapi.tefasFundAPI.client;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * TEFAS tarihsel veri (NAV) istemcisi.
 * WAF bypass için sayfa üzerinden response dinleme yaklaşımını kullanır.
 */
@Component
public class HistoryClient {
    private static final String BASE_URL = "https://www.tefas.gov.tr";
    private static final String REFERER = BASE_URL + "/TarihselVeriler.aspx";
    private static final String API = "/api/DB/BindHistoryInfo";

    // Form element selectors (based on actual TEFAS HTML structure)
    private static final String SELECTOR_START_DATE = "#TextBoxStartDate, input[name*='TextBoxStartDate']";
    private static final String SELECTOR_END_DATE = "#TextBoxEndDate, input[name*='TextBoxEndDate']";
    private static final String SELECTOR_FUND_CODE_FILTER = "input[type='search'][aria-controls='table_general_info']";
    private static final String SELECTOR_SEARCH_BUTTON = "#ButtonSearchDates, input[name*='ButtonSearchDates'], input[value='Görüntüle']";
    private static final int ELEMENT_WAIT_TIMEOUT_MS = 10000;

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
            System.out.println("HistoryClient/fetchHistoryJson started");
            // Launch browser in visible mode for debugging
            BrowserType.LaunchOptions launchOptions = PlaywrightHelper.createLaunchOptions().setHeadless(true);
            try (Browser browser = pw.chromium().launch(launchOptions)) {
                BrowserContext ctx = browser.newContext(PlaywrightHelper.createContextOptions());
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
            throw new RuntimeException("TEFAS/fetchHistoryJson interrupted", e);
        } catch (com.microsoft.playwright.TimeoutError e) {
            throw new RuntimeException(
                    "TEFAS/fetchHistoryJson timeout: Table data not loaded within timeout period. " + e.getMessage(),
                    e);
        } catch (Exception e) {
            throw new RuntimeException("TEFAS/fetchHistoryJson çağrısı başarısız: " + e.getMessage(), e);
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
                    new Page.WaitForSelectorOptions().setTimeout(ELEMENT_WAIT_TIMEOUT_MS));

            // Wait for DataTables to fully render and apply filter
            Thread.sleep(1000);

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

            System.out.println("Raw extracted JSON length: " + rawJson.length());

            // Transform using helper class
            return TableDataTransformer.transformToApiFormat(rawJson, fundCode);

        } catch (Exception e) {
            throw new RuntimeException("Failed to extract table data: " + e.getMessage(), e);
        }
    }

    /**
     * Navigates to TEFAS page and waits for WAF challenge to complete.
     */
    private void navigateAndWaitForWaf(Page page) throws InterruptedException {
        PlaywrightHelper.navigateForSession(page, REFERER);
        page.waitForLoadState(LoadState.NETWORKIDLE);
    }

    /**
     * Sets up request logging for debugging purposes.
     * Logs request URL, method, and post data for API calls.
     */
    private void setupRequestLogger(Page page) {
        page.onRequest(request -> {
            if (request.url().contains(API)) {
                System.out.println("\n=== REQUEST DEBUG ===");
                System.out.println("URL: " + request.url());
                System.out.println("Method: " + request.method());
                System.out.println("Post Data: " + request.postData());
                System.out.println("=====================\n");
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
            page.waitForSelector(SELECTOR_START_DATE,
                    new Page.WaitForSelectorOptions().setTimeout(ELEMENT_WAIT_TIMEOUT_MS));

            // Fill start date (TextBoxStartDate)
            fillInputField(page, SELECTOR_START_DATE, startDateStr, "Start Date");

            // Fill end date (TextBoxEndDate)
            fillInputField(page, SELECTOR_END_DATE, endDateStr, "End Date");

            System.out.println("Date fields filled: startDate=" + startDateStr + ", endDate=" + endDateStr);
        } catch (Exception e) {
            throw new RuntimeException("Failed to fill date fields: " + e.getMessage(), e);
        }
    }

    /**
     * Waits for the data table to fully load with actual data rows.
     */
    private void waitForTableToLoad(Page page) {
        try {
            // Wait for table structure
            page.waitForSelector("table tbody",
                    new Page.WaitForSelectorOptions().setTimeout(ELEMENT_WAIT_TIMEOUT_MS));

            // Wait for data rows to appear (not empty)
            page.waitForSelector("tbody tr:not(.dataTables_empty)",
                    new Page.WaitForSelectorOptions().setTimeout(ELEMENT_WAIT_TIMEOUT_MS));

            // Wait for at least one row with valid data (date format check)
            // Use evaluate with retry logic instead of waitForFunction
            long startTime = System.currentTimeMillis();
            boolean hasValidData = false;

            while (!hasValidData && (System.currentTimeMillis() - startTime) < ELEMENT_WAIT_TIMEOUT_MS) {
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
                    Thread.sleep(200); // Wait 200ms before retry
                }
            }

            if (!hasValidData) {
                throw new RuntimeException(
                        "Table loaded but no valid data rows found after " + ELEMENT_WAIT_TIMEOUT_MS + "ms");
            }

            // Additional wait for DataTables to finish processing
            Thread.sleep(1500);

            System.out.println("Table loaded successfully with data rows");

        } catch (Exception e) {
            throw new RuntimeException("Failed to wait for table to load: " + e.getMessage(), e);
        }
    }

    /**
     * Fills the fund code filter input (DataTables search box).
     * This filters the table client-side after it's loaded.
     */
    private void fillFundCodeFilter(Page page, String fundCode) {
        if (fundCode == null || fundCode.isBlank()) {
            System.out.println("Fund code is empty, skipping filter");
            return;
        }

        try {
            // Wait for filter input to be available
            page.waitForSelector(SELECTOR_FUND_CODE_FILTER,
                    new Page.WaitForSelectorOptions().setTimeout(ELEMENT_WAIT_TIMEOUT_MS));

            // Fill fund code in filter/search input (DataTables filter)
            fillInputField(page, SELECTOR_FUND_CODE_FILTER, fundCode.trim(), "Fund Code Filter");

            // Wait for DataTables to apply the filter
            Thread.sleep(1000);

            System.out.println("Fund code filter filled: " + fundCode);
        } catch (Exception e) {
            throw new RuntimeException("Failed to fill fund code filter: " + e.getMessage(), e);
        }
    }

    /**
     * Fills a single input field with retry logic.
     * Uses first() to handle multiple matches (strict mode violation).
     */
    private void fillInputField(Page page, String selector, String value, String fieldName) {
        try {
            // Use first() to handle multiple matches (strict mode violation)
            Locator locator = page.locator(selector).first();

            // Wait for element to be visible
            locator.waitFor(new Locator.WaitForOptions().setTimeout(ELEMENT_WAIT_TIMEOUT_MS));

            // Clear existing value first (watermark might be present)
            locator.clear();
            Thread.sleep(200);

            // Fill the field
            locator.fill(value);
            Thread.sleep(300);

            // Verify value was set
            String actualValue = locator.inputValue();
            if (!actualValue.equals(value)) {
                System.out.println(
                        "Warning: " + fieldName + " value mismatch. Expected: " + value + ", Got: " + actualValue);
            }
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to fill " + fieldName + " field with selector '" + selector + "': " + e.getMessage(), e);
        }
    }

    /**
     * Clicks the search/compare button to trigger API call.
     * Uses actual button ID from TEFAS HTML: ButtonSearchDates
     */
    private void clickSearchButton(Page page) {
        try {
            // Wait for button to be visible and clickable
            page.waitForSelector(SELECTOR_SEARCH_BUTTON,
                    new Page.WaitForSelectorOptions()
                            .setTimeout(ELEMENT_WAIT_TIMEOUT_MS));

            // Click the button
            page.click(SELECTOR_SEARCH_BUTTON,
                    new Page.ClickOptions().setTimeout(5000));

            System.out.println("Clicked search button: " + SELECTOR_SEARCH_BUTTON);
            Thread.sleep(500); // Wait for click to register

        } catch (Exception e) {
            System.out.println("Warning: Could not click button with selector '" + SELECTOR_SEARCH_BUTTON + "': "
                    + e.getMessage());

            // Fallback: Try JavaScript click on button with onclick handler
            try {
                System.out.println("Trying JavaScript click fallback...");
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
                    System.out.println("JavaScript click successful");
                    Thread.sleep(500);
                } else {
                    throw new RuntimeException("Could not find or click search button");
                }
            } catch (Exception jsError) {
                throw new RuntimeException("Failed to click search button: " + e.getMessage() +
                        ", JavaScript fallback also failed: " + jsError.getMessage(), e);
            }
        }
    }
}
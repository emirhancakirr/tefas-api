package com.tefasfundapi.tefasFundAPI.client;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import com.tefasfundapi.tefasFundAPI.config.PlaywrightConfig;
import com.tefasfundapi.tefasFundAPI.exception.TefasClientException;
import com.tefasfundapi.tefasFundAPI.exception.TefasNavigationException;
import com.tefasfundapi.tefasFundAPI.exception.TefasTimeoutException;
import com.tefasfundapi.tefasFundAPI.exception.TefasWafBlockedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Playwright ile TEFAS API çağrıları için ortak utility metodları.
 * DRY prensibi: Tekrar eden kodları buraya topladık.
 */
public final class PlaywrightHelper {
    private static final Logger log = LoggerFactory.getLogger(PlaywrightHelper.class);

    private PlaywrightHelper() {
        // Utility class - instantiate edilemez
    }

    // ==================== Browser Configuration ====================

    public static BrowserType.LaunchOptions createLaunchOptions(PlaywrightConfig config) {
        return new BrowserType.LaunchOptions()
                .setHeadless(config.isHeadless())
                .setArgs(List.of("--disable-blink-features=AutomationControlled"));
    }

    public static Browser.NewContextOptions createContextOptions(PlaywrightConfig config) {
        return new Browser.NewContextOptions()
                .setUserAgent(config.getUserAgent())
                .setViewportSize(config.getViewportWidth(), config.getViewportHeight())
                .setLocale(config.getLocale())
                .setTimezoneId(config.getTimezone());
    }

    // ==================== Navigation ====================

    public static void navigateForSession(Page page, String url, PlaywrightConfig config) {
        try {
            page.navigate(url);
            page.waitForLoadState(LoadState.LOAD,
                    new Page.WaitForLoadStateOptions().setTimeout(config.getNavigationTimeoutMs()));
            page.waitForTimeout(config.getWafWaitMs());
        } catch (Exception e) {
            throw new TefasNavigationException(url, e);
        }
    }

    public static void navigateAndWaitForWaf(Page page, String url, PlaywrightConfig config)
            throws InterruptedException {
        navigateForSession(page, url, config);
        page.waitForLoadState(LoadState.NETWORKIDLE);
    }

    // ==================== Form Interaction ====================

    public static void fillInputField(Page page, String selector, String value, String fieldName,
            PlaywrightConfig config) {
        try {
            Locator locator = page.locator(selector).first();
            locator.waitFor(new Locator.WaitForOptions().setTimeout(config.getElementWaitTimeoutMs()));
            locator.clear();
            Thread.sleep(config.getInputClearWaitMs());
            locator.fill(value);
            Thread.sleep(config.getInputFillWaitMs());

            String actualValue = locator.inputValue();
            if (!actualValue.equals(value)) {
                log.warn("{} value mismatch. Expected: {}, Got: {}", fieldName, value, actualValue);
            }
        } catch (Exception e) {
            throw new TefasClientException(
                    "Failed to fill " + fieldName + " field with selector '" + selector + "': " + e.getMessage(), e);
        }
    }

    public static void fillDateFields(Page page, LocalDate start, LocalDate end, PlaywrightConfig config) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        String startDateStr = start.format(formatter);
        String endDateStr = end.format(formatter);

        try {
            page.waitForSelector(config.getSelectors().getStartDate(),
                    new Page.WaitForSelectorOptions().setTimeout(config.getElementWaitTimeoutMs()));

            fillInputField(page, config.getSelectors().getStartDate(), startDateStr, "Start Date", config);
            fillInputField(page, config.getSelectors().getEndDate(), endDateStr, "End Date", config);

            log.debug("Date fields filled: startDate={}, endDate={}", startDateStr, endDateStr);
        } catch (Exception e) {
            throw new TefasClientException("Failed to fill date fields: " + e.getMessage(), e);
        }
    }

    public static void fillFundCodeFilter(Page page, String fundCode, PlaywrightConfig config) {
        if (fundCode == null || fundCode.isBlank()) {
            log.debug("Fund code is empty, skipping filter");
            return;
        }

        try {
            page.waitForSelector(config.getSelectors().getFundCodeFilter(),
                    new Page.WaitForSelectorOptions().setTimeout(config.getElementWaitTimeoutMs()));

            fillInputField(page, config.getSelectors().getFundCodeFilter(), fundCode.trim(),
                    "Fund Code Filter", config);

            Thread.sleep(config.getFilterApplyWaitMs());

            log.debug("Fund code filter filled: {}", fundCode);
        } catch (Exception e) {
            throw new TefasClientException("Failed to fill fund code filter: " + e.getMessage(), e);
        }
    }

    public static void clickSearchButton(Page page, PlaywrightConfig config) {
        try {
            String searchButtonSelector = config.getSelectors().getSearchButton();
            page.waitForSelector(searchButtonSelector,
                    new Page.WaitForSelectorOptions()
                            .setTimeout(config.getElementWaitTimeoutMs()));

            page.click(searchButtonSelector,
                    new Page.ClickOptions().setTimeout(config.getButtonClickTimeoutMs()));

            log.debug("Clicked search button: {}", searchButtonSelector);
            Thread.sleep(config.getButtonClickWaitMs());

        } catch (Exception e) {
            log.warn("Could not click button with selector '{}': {}", config.getSelectors().getSearchButton(),
                    e.getMessage());

            try {
                log.debug("Trying JavaScript click fallback...");
                boolean clicked = page.evaluate("""
                        (function() {
                            var btn = document.getElementById('ButtonSearchDates') ||
                                      document.querySelector('input[value="Görüntüle"]') ||
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

    // ==================== Request Logging ====================

    /**
     * Sets up request logging for debugging purposes.
     * Logs request URL, method, and post data for API calls matching the given
     * endpoint.
     * 
     * @param page     Playwright Page objesi
     * @param endpoint API endpoint string to match (e.g.,
     *                 "/api/DB/BindHistoryInfo")
     */
    public static void setupRequestLogger(Page page, String endpoint) {
        page.onRequest(request -> {
            if (request.url().contains(endpoint)) {
                log.debug("=== REQUEST DEBUG ===");
                log.debug("URL: {}", request.url());
                log.debug("Method: {}", request.method());
                log.debug("Post Data: {}", request.postData());
                log.debug("====================");
            }
        });
    }

    // ==================== Table Loading ====================

    /**
     * Waits for the data table to fully load with actual data rows.
     * Validates that rows contain valid date data in dd.MM.yyyy format.
     * 
     * @param page   Playwright Page objesi
     * @param config Playwright konfigürasyonu
     * @throws TefasTimeoutException if table doesn't load within timeout
     * @throws TefasClientException  if waiting fails
     */
    public static void waitForTableToLoad(Page page, PlaywrightConfig config) {
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

    // ==================== HTTP/API Helpers ====================

    public static void ensureOk(APIResponse response, String body) {
        int status = response.status();
        if (status == 401 || status == 403) {
            throw new TefasClientException("Unauthorized/Forbidden: " + status);
        }
        if (status < 200 || status >= 300) {
            throw new TefasClientException("Upstream " + status + " " + response.statusText() + " body=" + body);
        }
    }

    public static void ensureOk(APIResponse response) {
        ensureOk(response, "");
    }

    public static void checkWafBlock(String responseText) {
        if (responseText != null && responseText.trim().startsWith("<")) {
            String preview = responseText.length() > 500
                    ? responseText.substring(0, 500)
                    : responseText;
            throw new TefasWafBlockedException(preview);
        }
    }

    public static String validateResponse(Response response) {
        String text = response.text();
        checkWafBlock(text);

        int status = response.status();
        if (status == 401 || status == 403) {
            throw new TefasClientException("Unauthorized/Forbidden: " + status);
        }
        if (status < 200 || status >= 300) {
            throw new TefasClientException("Upstream error " + status + " " + response.statusText());
        }

        return text;
    }

    // ==================== Form Encoding ====================

    public static String toFormEncoded(Map<String, String> form) {
        if (form == null || form.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (var entry : form.entrySet()) {
            if (sb.length() > 0) {
                sb.append('&');
            }
            sb.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8))
                    .append('=')
                    .append(URLEncoder.encode(
                            entry.getValue() == null ? "" : entry.getValue(),
                            StandardCharsets.UTF_8));
        }
        return sb.toString();
    }

    // ==================== API Context ====================

    public static APIRequestContext createApiContext(
            Playwright playwright,
            String baseUrl,
            BrowserContext context,
            Map<String, String> headers) {
        return playwright.request().newContext(
                new APIRequest.NewContextOptions()
                        .setBaseURL(baseUrl)
                        .setStorageState(context.storageState())
                        .setExtraHTTPHeaders(headers));
    }

    // ==================== JavaScript Injection ====================

    public static void triggerApiCallViaJavaScript(Page page, String apiUrl, Map<String, String> formData) {
        String script = buildAjaxScript(apiUrl, formData);
        page.evaluate(script);
    }

    private static String buildAjaxScript(String apiUrl, Map<String, String> formData) {
        StringBuilder script = new StringBuilder();
        script.append("(function() {");

        script.append("var params = new URLSearchParams();");
        for (Map.Entry<String, String> entry : formData.entrySet()) {
            script.append("params.append(")
                    .append(escapeJsForJs(entry.getKey()))
                    .append(", ")
                    .append(escapeJsForJs(entry.getValue()))
                    .append(");");
        }
        script.append("var formDataString = params.toString();");

        script.append("if (typeof $ !== 'undefined') {");
        script.append("$.ajax({");
        script.append("url: ").append(escapeJsForJs(apiUrl)).append(",");
        script.append("type: 'POST',");
        script.append("contentType: 'application/x-www-form-urlencoded; charset=UTF-8',");
        script.append("processData: false,");
        script.append("data: formDataString,");
        script.append("success: function(data) { console.log('API call successful'); },");
        script.append("error: function(xhr, status, error) { console.log('API call error:', error); }");
        script.append("});");
        script.append("} else {");

        script.append("fetch(").append(escapeJsForJs(apiUrl)).append(", {");
        script.append("method: 'POST',");
        script.append("headers: {");
        script.append("'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8',");
        script.append("'X-Requested-With': 'XMLHttpRequest'");
        script.append("},");
        script.append("body: formDataString");
        script.append("}).then(function(response) { console.log('API call successful'); })");
        script.append(".catch(function(error) { console.log('API call error:', error); });");
        script.append("}");
        script.append("})();");

        return script.toString();
    }

    // ==================== JavaScript String Escaping ====================

    private static String escapeJsForJs(String value) {
        if (value == null) {
            return "''";
        }
        return "\"" + value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t") + "\"";
    }

    private static String escapeJs(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
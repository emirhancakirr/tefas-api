package com.tefasfundapi.tefasFundAPI.client;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import com.tefasfundapi.tefasFundAPI.config.PlaywrightConfig;
import com.tefasfundapi.tefasFundAPI.exception.TefasClientException;
import com.tefasfundapi.tefasFundAPI.exception.TefasNavigationException;
import com.tefasfundapi.tefasFundAPI.exception.TefasTimeoutException;
import com.tefasfundapi.tefasFundAPI.exception.TefasWafBlockedException;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingDeque;

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

    // PlaywrightHelper.java - clickSearchButton metodundan sonra ekle (satır
    // 211'den sonra)

    // ==================== Response Waiting ====================

    // PlaywrightHelper.java:124-180 - waitForApiResponse metodunu güncelle
    // PlaywrightHelper.java - waitForApiResponse'dan önce ekle

    /**
     * Sets up the response listener for a specific endpoint.
     * Must be called BEFORE triggering the action that will cause the API call.
     * 
     * @param page     Playwright Page objesi
     * @param endpoint API endpoint URL pattern
     * @param config   Playwright konfigürasyonu
     * @return BlockingQueue that will collect responses
     */
    public static java.util.concurrent.BlockingQueue<Response> setupResponseListener(
            Page page,
            String endpoint,
            PlaywrightConfig config) {
        log.info("🔧 Setting up response listener for endpoint: {}", endpoint);

        // Response'ları toplamak için queue
        java.util.concurrent.BlockingQueue<Response> responseQueue = new java.util.concurrent.LinkedBlockingQueue<>();
        log.info("✅ Response listener queue created. Waiting for responses...");

        // Response listener - tüm response'ları topla
        page.onResponse(response -> {
            String url = response.url();
            int status = response.status();
            String method = response.request().method();

            // 🔍 DEBUG: Tüm response'ları logla (özellikle POST ve API endpoint'leri)
            if (method.equals("POST") || url.contains("/api/") || url.contains("BindComparison")) {
                log.info("🔍 Response received: URL={}, Status={}, Method={}", url, status, method);
            } else {
                log.debug("Response received: URL={}, Status={}, Method={}", url, status, method);
            }

            // Endpoint kontrolü - daha esnek matching
            // endpoint="/api/DB/BindComparisonFundReturns" ama URL tam URL olabilir
            boolean matchesEndpoint = url.contains(endpoint) ||
                    url.endsWith(endpoint) ||
                    url.contains("BindComparisonFundReturns") ||
                    (url.contains("/api/DB/") && url.contains("BindComparison"));

            if (matchesEndpoint) {
                log.info("✅ Matching endpoint found! URL={}, Status={}, Method={}", url, status, method);

                // POST endpoint'i kontrol et - tüm 2xx status kodları kabul et
                if (status >= 200 && status < 300) {
                    log.info("✅ Valid response (status {}) - adding to queue", status);
                    try {
                        // ⚠️ ÖNEMLİ: response.text() sadece bir kez çağrılabilir!
                        // Body'yi burada okumayalım, waitForApiResponse içinde okuyalım
                        // Sadece response'u queue'ya ekle
                        boolean added = responseQueue.offer(response);
                        log.info("✅ Response added to queue: {}, Queue size: {}", added, responseQueue.size());
                    } catch (Exception e) {
                        log.error("❌ Failed to add response to queue: {}", e.getMessage(), e);
                    }
                } else {
                    log.warn("❌ Response status is not 2xx: status={} (URL: {})", status, url);
                    // Status code'u logla - belki 401, 403, 500 gibi bir hata var
                    try {
                        String errorBody = response.text();
                        log.warn("❌ Error response body (first 500 chars): {}",
                                errorBody != null && errorBody.length() > 500
                                        ? errorBody.substring(0, 500) + "..."
                                        : errorBody);
                    } catch (Exception e) {
                        log.warn("Could not read error response body: {}", e.getMessage());
                    }
                }
            } else {
                // Sadece POST ve API endpoint'leri için log
                if (method.equals("POST") && url.contains("/api/")) {
                    log.debug("Response URL doesn't match endpoint: {} (looking for: {})", url, endpoint);
                }
            }
        });

        // 🔍 DEBUG: Request'leri de logla
        page.onRequest(request -> {
            String url = request.url();
            if (url.contains(endpoint) || url.contains("BindComparisonFundReturns")) {
                log.info("✅ Request detected: URL={}, Method={}, PostData={}",
                        url, request.method(), request.postData());
            }
        });

        return responseQueue;
    }

    /**
     * Waits for a non-empty response from the queue.
     * Must be called AFTER setupResponseListener and AFTER triggering the action.
     * 
     * @param responseQueue Queue that collects responses (from
     *                      setupResponseListener)
     * @param endpoint      API endpoint URL pattern (for logging)
     * @param config        Playwright konfigürasyonu
     * @return Response body as string (non-empty)
     * @throws TefasTimeoutException if no non-empty response arrives within timeout
     * @throws TefasClientException  if waiting fails
     */
    public static String waitForApiResponse(
            java.util.concurrent.BlockingQueue<Response> responseQueue,
            String endpoint,
            PlaywrightConfig config) {
        try {
            log.debug("Waiting for non-empty API response from POST endpoint: {}", endpoint);
            log.debug("Queue size at start: {}", responseQueue.size());

            // Timeout kontrolü için
            // Response 11+ saniye sonra gelebiliyor, bu yüzden timeout'u biraz artırıyoruz
            long startTime = System.currentTimeMillis();
            long timeoutMs = Math.max(config.getElementWaitTimeoutMs(), 15000); // En az 15 saniye
            log.debug("Using timeout: {} ms", timeoutMs);

            // Dolu response bulana kadar bekle
            while (System.currentTimeMillis() - startTime < timeoutMs) {
                long remainingTime = timeoutMs - (System.currentTimeMillis() - startTime);
                log.debug("Polling queue... (remaining: {} ms, queue size: {})", remainingTime, responseQueue.size());

                Response response = responseQueue.poll(
                        Math.max(1000, remainingTime),
                        java.util.concurrent.TimeUnit.MILLISECONDS);

                if (response == null) {
                    // Timeout oldu, ama queue'da response var mı kontrol et (non-blocking)
                    response = responseQueue.poll(); // Non-blocking poll
                    if (response == null) {
                        log.warn("❌ No response in queue after polling. Queue size: {}", responseQueue.size());
                        // Gerçekten timeout - dolu response bulunamadı
                        throw new TefasTimeoutException(
                                "waitForApiResponse",
                                timeoutMs,
                                new Exception("No non-empty response received from endpoint: " + endpoint));
                    } else {
                        log.info("✅ Response found in queue after timeout check! Queue size: {}", responseQueue.size());
                    }
                }

                log.debug("✅ Response found in queue! Processing...");

                // Response body'yi al
                String body;
                try {
                    body = response.text();
                    log.debug("Response body length: {}", body != null ? body.length() : 0);
                } catch (Exception e) {
                    log.warn("Failed to read response body: {}, trying next response", e.getMessage());
                    continue;
                }

                // Boş response kontrolü
                if (body == null || body.trim().isEmpty()) {
                    log.debug("❌ Empty response received, waiting for next response...");
                    continue;
                }

                // Empty array kontrolü
                String trimmedBody = body.trim();
                if (trimmedBody.equals("[]")) {
                    log.debug("❌ Empty array response received, waiting for next response...");
                    continue;
                }

                // Error kontrolü
                if (trimmedBody.startsWith("{")) {
                    if (trimmedBody.contains("\"error\"") || trimmedBody.contains("\"Error\"")) {
                        log.warn("❌ API response contains error: {}",
                                trimmedBody.length() > 200 ? trimmedBody.substring(0, 200) + "..." : trimmedBody);
                        continue;
                    }
                }

                // Dolu response bulundu!
                log.info("✅ Non-empty API response received from {}: {} bytes", endpoint, body.length());
                return body;
            }

            // Timeout - ama queue'da response var mı son bir kontrol
            // Response async geldiği için bir süre daha bekleyelim
            log.warn("⚠️ Timeout reached, but checking queue one more time... Queue size: {}", responseQueue.size());

            // Response'ın gelmesi için 2 saniye daha bekle
            for (int i = 0; i < 20; i++) { // 20 x 100ms = 2 saniye
                Response finalResponse = responseQueue.poll(); // Non-blocking
                if (finalResponse != null) {
                    log.info("✅ Response found in queue after timeout! Processing...");
                    try {
                        String body = finalResponse.text();
                        if (body != null && !body.trim().isEmpty() && !body.trim().equals("[]")) {
                            log.info("✅ Non-empty API response received from {}: {} bytes", endpoint, body.length());
                            return body;
                        }
                    } catch (Exception e) {
                        log.warn("Failed to read final response body: {}", e.getMessage());
                    }
                }
                try {
                    Thread.sleep(100); // 100ms bekle
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            log.error("❌ Timeout reached! Queue size: {}", responseQueue.size());
            throw new TefasTimeoutException(
                    "waitForApiResponse",
                    timeoutMs,
                    new Exception("No non-empty response received from endpoint: " + endpoint + " within timeout"));

        } catch (TefasTimeoutException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TefasClientException("Interrupted while waiting for API response", e);
        } catch (Exception e) {
            throw new TefasClientException(
                    "Failed to wait for API response from " + endpoint + ": " + e.getMessage(),
                    e);
        }
    }

    /**
     * Fills the fund code filter input (DataTables search box).
     * This filters the table client-side after it's loaded.
     * 
     * @param page     Playwright Page objesi
     * @param fundCode Fon kodu
     * @param config   Playwright konfigürasyonu
     * @param selector Optional selector. If null, uses config selector. If empty
     *                 string, skips filter.
     */
    public static void fillFundCodeFilter(Page page, String fundCode, PlaywrightConfig config, String selector) {
        if (fundCode == null || fundCode.isBlank()) {
            log.debug("Fund code is empty, skipping filter");
            return;
        }

        // If selector is empty string, skip filter (for pages that don't have this
        // filter)
        if (selector != null && selector.isEmpty()) {
            log.debug("Fund code filter selector is empty, skipping filter");
            return;
        }

        try {
            // Use provided selector or fallback to config
            String finalSelector = (selector != null && !selector.isEmpty())
                    ? selector
                    : config.getSelectors().getFundCodeFilter();

            // Check if selector exists on page
            boolean exists = (Boolean) page.evaluate("""
                    (function() {
                        return document.querySelector(arguments[0]) !== null;
                    })();
                    """, finalSelector);

            if (!exists) {
                log.warn("Fund code filter selector not found on page: {}, skipping filter", finalSelector);
                return; // Skip if not found
            }

            page.waitForSelector(finalSelector,
                    new Page.WaitForSelectorOptions().setTimeout(config.getElementWaitTimeoutMs()));

            fillInputField(page, finalSelector, fundCode.trim(),
                    "Fund Code Filter", config);

            Thread.sleep(config.getFilterApplyWaitMs());

            log.debug("Fund code filter filled: {} with selector: {}", fundCode, finalSelector);
        } catch (Exception e) {
            log.warn("Failed to fill fund code filter (may not exist on this page): {}", e.getMessage());
            // Don't throw - just log warning and continue
        }
    }

    // Overloaded method for backward compatibility (uses config selector)
    public static void fillFundCodeFilter(Page page, String fundCode, PlaywrightConfig config) {
        fillFundCodeFilter(page, fundCode, config, null);
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

    // PlaywrightHelper.java - waitForTableToLoad'dan sonra ekle

    /**
     * Waits for the fund returns table (#table_fund_returns) to fully load.
     * This is specific to the comparison page (FonKarsilastirma.aspx).
     * 
     * @param page   Playwright Page objesi
     * @param config Playwright konfigürasyonu
     * @throws TefasTimeoutException if table doesn't load within timeout
     * @throws TefasClientException  if waiting fails
     */
    public static void waitForFundReturnsTable(Page page, PlaywrightConfig config) {
        try {
            // Wait for table structure
            page.waitForSelector("#table_fund_returns tbody",
                    new Page.WaitForSelectorOptions().setTimeout(config.getElementWaitTimeoutMs()));

            // Wait for data rows to appear (not empty)
            page.waitForSelector("#table_fund_returns tbody tr:not(.dataTables_empty)",
                    new Page.WaitForSelectorOptions().setTimeout(config.getElementWaitTimeoutMs()));

            // Wait for at least one row with valid data (check for fund code in first cell)
            long startTime = System.currentTimeMillis();
            boolean hasValidData = false;

            while (!hasValidData && (System.currentTimeMillis() - startTime) < config.getElementWaitTimeoutMs()) {
                Object result = page.evaluate("""
                        (function() {
                            const table = document.querySelector('#table_fund_returns');
                            if (!table) return false;

                            const rows = table.querySelectorAll('tbody tr:not(.dataTables_empty)');
                            for (let row of rows) {
                                const cells = row.querySelectorAll('td');
                                if (cells.length >= 4) {
                                    const fundCode = cells[0].textContent.trim();
                                    // Check if it's a valid fund code (not empty, not header)
                                    if (fundCode && fundCode.length >= 2 && fundCode !== 'Fon Kodu') {
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
                throw new TefasTimeoutException("waitForFundReturnsTable", config.getElementWaitTimeoutMs());
            }

            // Additional wait for DataTables to finish processing
            Thread.sleep(config.getTableLoadWaitMs());

            log.info("Fund returns table loaded successfully with data rows");

        } catch (TefasTimeoutException e) {
            // Re-throw timeout exceptions as-is
            throw e;
        } catch (Exception e) {
            throw new TefasClientException("Failed to wait for fund returns table to load: " + e.getMessage(), e);
        }
    }

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
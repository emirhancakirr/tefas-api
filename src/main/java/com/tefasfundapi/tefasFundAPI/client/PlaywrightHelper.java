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
import java.util.ArrayList;
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
    /**
     * Response wrapper class to cache the body since Playwright Response.text() can
     * only be called once.
     */
    public static class ResponseWithBody {
        private final Response response;
        private final String body;
        private final String url;
        private final int status;

        public ResponseWithBody(Response response, String body) {
            this.response = response;
            this.body = body;
            this.url = response.url();
            this.status = response.status();
        }

        public String getBody() {
            return body;
        }

        public String url() {
            return url;
        }

        public int status() {
            return status;
        }

        public Response getResponse() {
            return response;
        }
    }

    public static java.util.concurrent.BlockingQueue<ResponseWithBody> setupResponseListener(
            Page page,
            String endpoint,
            PlaywrightConfig config) {
        log.info("Setting up response listener for endpoint: {}", endpoint);

        java.util.concurrent.BlockingQueue<ResponseWithBody> responseQueue = new java.util.concurrent.LinkedBlockingQueue<>();

        page.onResponse(response -> {
            String url = response.url();
            int status = response.status();

            // Sadece status 200 ve endpoint match eden response'ları al
            boolean matchesEndpoint = url.contains(endpoint) || url.contains("BindComparisonFundReturns");

            if (matchesEndpoint && status == 200) {
                try {
                    log.info("📡 Matched response, reading body... URL={}", url);
                    String body = response.text();
                    log.info("📡 Body read successfully, length={}", body != null ? body.length() : 0);

                    ResponseWithBody responseWithBody = new ResponseWithBody(response, body);
                    boolean offered = responseQueue.offer(responseWithBody);

                    log.info("📥 Response queued: offered={}, URL={}, Status={}, Body length={}, Queue size={}",
                            offered, url, status, body != null ? body.length() : 0, responseQueue.size());
                } catch (Exception e) {
                    log.error("❌ Failed to read/queue response body from URL={}: {}", url, e.getMessage(), e);
                }
            }
        });

        return responseQueue;
    }

    /**
     * Collects ALL responses from the endpoint and returns the LAST one.
     * Useful when multiple POST requests are made to the same endpoint,
     * and you need the final (filtered) result.
     * 
     * @param responseQueue   Queue that collects responses (from
     *                        setupResponseListener)
     * @param endpoint        API endpoint URL pattern (for logging)
     * @param config          Playwright konfigürasyonu
     * @param waitAfterLastMs How long to wait after receiving a response to ensure
     *                        it's the last one
     * @return Response body of the LAST response (non-empty)
     * @throws TefasTimeoutException if no response arrives within timeout
     * @throws TefasClientException  if waiting fails
     */
    public static String waitForLastApiResponse(
            Page page,
            java.util.concurrent.BlockingQueue<ResponseWithBody> responseQueue,
            String endpoint,
            PlaywrightConfig config,
            long waitAfterLastMs) {
        return waitForLastApiResponse(page, responseQueue, endpoint, config, waitAfterLastMs, 2);
    }

    public static String waitForLastApiResponse(
            Page page,
            java.util.concurrent.BlockingQueue<ResponseWithBody> responseQueue,
            String endpoint,
            PlaywrightConfig config,
            long waitAfterLastMs,
            int minResponseCount) { // Yeni parametre
        try {
            long startTime = System.currentTimeMillis();
            long timeoutMs = Math.max(config.getElementWaitTimeoutMs(), 2000);

            List<ResponseWithBody> allResponses = new ArrayList<>();
            ResponseWithBody lastResponse = null;
            long lastResponseTime = 0;

            log.info("⏳ Collecting responses from endpoint (min: {}, max wait: {}s)...",
                    minResponseCount, timeoutMs / 1000);

            while (System.currentTimeMillis() - startTime < timeoutMs) {
                // Playwright event loop'unu çalıştırmak için kısa bekleme
                // Bu adım kritik: poll() metodunu doğrudan çağırmak yerine waitForTimeout
                // kullanıyoruz
                page.waitForTimeout(100);

                ResponseWithBody responseWithBody = responseQueue.poll();

                if (responseWithBody != null) {
                    String body = responseWithBody.getBody();

                    if (body != null && !body.trim().isEmpty() && !body.trim().equals("[]")) {
                        allResponses.add(responseWithBody);
                        lastResponse = responseWithBody;
                        lastResponseTime = System.currentTimeMillis();

                        log.info("📥 Response #{} received: {} bytes",
                                allResponses.size(), body.length());
                    }
                }

                // Minimum response sayısına ulaştıysak ve waitAfterLastMs geçtiyse bitir
                if (allResponses.size() >= minResponseCount &&
                        lastResponse != null &&
                        (System.currentTimeMillis() - lastResponseTime) >= waitAfterLastMs) {
                    log.info("✅ Received {} responses (min: {}), no more after {}ms, using last response",
                            allResponses.size(), minResponseCount, waitAfterLastMs);
                    break;
                }
            }

            log.info("🔍 Main loop ended, checking for any remaining responses in queue...");
            log.info("📊 Current queue size: {}, allResponses collected: {}", responseQueue.size(),
                    allResponses.size());

            // Final check - drain any remaining responses
            int nullCount = 0;
            int maxNullsBeforeBreak = 3;

            for (int i = 0; i < 10; i++) {
                page.waitForTimeout(500); // Pump event loop
                ResponseWithBody finalCheck = responseQueue.poll();

                if (finalCheck != null) {
                    nullCount = 0;
                    String body = finalCheck.getBody();
                    if (body != null && !body.trim().isEmpty() && !body.trim().equals("[]")) {
                        allResponses.add(finalCheck);
                        lastResponse = finalCheck;
                        log.info("📥 Response #{} received (final check #{}): {} bytes",
                                allResponses.size(), i + 1, body.length());
                        continue;
                    }
                } else {
                    nullCount++;
                    if (nullCount >= maxNullsBeforeBreak) {
                        break;
                    }
                }
            }

            if (allResponses.isEmpty()) {
                throw new TefasTimeoutException(
                        "waitForLastApiResponse",
                        timeoutMs,
                        new Exception("No responses received from endpoint: " + endpoint));
            }

            if (allResponses.size() < minResponseCount) {
                log.warn("⚠️ Only received {} responses, expected at least {}",
                        allResponses.size(), minResponseCount);
            }

            log.info("✅ Collected {} responses total, returning the LAST one", allResponses.size());
            ResponseWithBody finalResponse = allResponses.get(allResponses.size() - 1);
            return finalResponse.getBody();

        } catch (TefasTimeoutException e) {
            throw e;
        } catch (Exception e) {
            throw new TefasClientException(
                    "Failed to wait for API response from " + endpoint + ": " + e.getMessage(),
                    e);
        }
    }

    /**
     * Waits for a non-empty response from the queue.
     * Must be called AFTER setupResponseListener and AFTER triggering the action.
     * 
     * @param page          Playwright Page objesi
     * @param responseQueue Queue that collects responses (from
     *                      setupResponseListener)
     * @param endpoint      API endpoint URL pattern (for logging)
     * @param config        Playwright konfigürasyonu
     * @return Response body as string (non-empty)ç
     * @throws TefasTimeoutException if no non-empty response arrives within timeout
     * @throws TefasClientException  if waiting fails
     */
    public static String waitForApiResponse(
            Page page,
            java.util.concurrent.BlockingQueue<ResponseWithBody> responseQueue,
            String endpoint,
            PlaywrightConfig config) {
        try {
            long startTime = System.currentTimeMillis();
            long timeoutMs = Math.max(config.getElementWaitTimeoutMs(), 30000);

            while (System.currentTimeMillis() - startTime < timeoutMs) {
                page.waitForTimeout(200); // Yield to event loop

                ResponseWithBody responseWithBody = responseQueue.poll();

                if (responseWithBody == null) {
                    continue;
                }

                String body = responseWithBody.getBody();

                if (body != null && !body.trim().isEmpty() && !body.trim().equals("[]")) {
                    log.info("API response received: {} bytes", body.length());
                    return body;
                }
            }

            // Son kontrol
            page.waitForTimeout(500);
            ResponseWithBody finalResponse = responseQueue.poll();
            if (finalResponse != null) {
                String body = finalResponse.getBody();
                if (body != null && !body.trim().isEmpty() && !body.trim().equals("[]")) {
                    log.info("API response received after timeout: {} bytes", body.length());
                    return body;
                }
            }

            throw new TefasTimeoutException("waitForApiResponse timeout {}", timeoutMs);
        } catch (Exception e) {
            throw new TefasClientException("Failed to wait for API response: " + e.getMessage(), e);
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
            log.info("🔘 Attempting to click search button with selector: {}", searchButtonSelector);

            page.waitForSelector(searchButtonSelector,
                    new Page.WaitForSelectorOptions()
                            .setTimeout(config.getElementWaitTimeoutMs()));

            log.info("🔘 Button found, clicking...");
            page.click(searchButtonSelector,
                    new Page.ClickOptions().setTimeout(config.getButtonClickTimeoutMs()));

            log.info("✅ Search button clicked successfully: {}", searchButtonSelector);

        } catch (Exception e) {
            log.warn("⚠️ Could not click button with selector '{}': {}", config.getSelectors().getSearchButton(),
                    e.getMessage());

            try {
                log.info("🔄 Trying JavaScript click fallback...");
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
                    log.info("✅ JavaScript click successful");
                    Thread.sleep(config.getButtonClickWaitMs());
                    log.info("✅ Waiting {} ms for API call to be triggered...", config.getButtonClickWaitMs());
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
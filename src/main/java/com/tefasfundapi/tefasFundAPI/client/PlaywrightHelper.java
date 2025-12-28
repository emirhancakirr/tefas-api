package com.tefasfundapi.tefasFundAPI.client;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import com.tefasfundapi.tefasFundAPI.config.PlaywrightConfig;
import com.tefasfundapi.tefasFundAPI.exception.TefasClientException;
import com.tefasfundapi.tefasFundAPI.exception.TefasNavigationException;
import com.tefasfundapi.tefasFundAPI.exception.TefasWafBlockedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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

    /**
     * Standart browser launch options oluşturur.
     * Bot detection'ı bypass etmek için gerekli ayarları içerir.
     * 
     * @param config Playwright konfigürasyonu
     * @return BrowserType.LaunchOptions
     */
    public static BrowserType.LaunchOptions createLaunchOptions(PlaywrightConfig config) {
        return new BrowserType.LaunchOptions()
                .setHeadless(config.isHeadless())
                .setArgs(List.of("--disable-blink-features=AutomationControlled"));
    }

    /**
     * Standart browser context options oluşturur.
     * Gerçekçi browser ayarları (User-Agent, viewport, locale, timezone) içerir.
     * 
     * @param config Playwright konfigürasyonu
     * @return Browser.NewContextOptions
     */
    public static Browser.NewContextOptions createContextOptions(PlaywrightConfig config) {
        return new Browser.NewContextOptions()
                .setUserAgent(config.getUserAgent())
                .setViewportSize(config.getViewportWidth(), config.getViewportHeight())
                .setLocale(config.getLocale())
                .setTimezoneId(config.getTimezone());
    }

    /**
     * WAF/oturum için sayfaya gidip network idle bekler.
     * TEFAS'ın WAF korumasını bypass etmek için session oluşturur.
     * 
     * @param page   Playwright Page objesi
     * @param url    Navigate edilecek URL
     * @param config Playwright konfigürasyonu
     * @throws RuntimeException Navigation başarısız olursa
     */
    public static void navigateForSession(Page page, String url, PlaywrightConfig config) {
        try {
            page.navigate(url);
            // LOAD durumu: Sayfa yüklendi, tüm kaynaklar indirilmedi de olabilir
            page.waitForLoadState(LoadState.LOAD,
                    new Page.WaitForLoadStateOptions().setTimeout(config.getNavigationTimeoutMs()));
            // WAF için daha uzun bekleme
            page.waitForTimeout(config.getWafWaitMs());
        } catch (Exception e) {
            throw new TefasNavigationException(url, e);
        }
    }

    /**
     * HTTP response'un başarılı olup olmadığını kontrol eder.
     * 
     * @param response APIResponse objesi
     * @param body     Response body (hata mesajı için)
     * @throws RuntimeException Status code başarısız ise
     */
    public static void ensureOk(APIResponse response, String body) {
        int status = response.status();
        if (status == 401 || status == 403) {
            throw new TefasClientException("Unauthorized/Forbidden: " + status);
        }
        if (status < 200 || status >= 300) {
            throw new TefasClientException("Upstream " + status + " " + response.statusText() + " body=" + body);
        }
    }

    /**
     * HTTP response'un başarılı olup olmadığını kontrol eder (body olmadan).
     * 
     * @param response APIResponse objesi
     * @throws RuntimeException Status code başarısız ise
     */
    public static void ensureOk(APIResponse response) {
        ensureOk(response, "");
    }

    /**
     * WAF engeli kontrolü yapar. HTML response dönerse exception fırlatır.
     * 
     * @param responseText Response text
     * @throws RuntimeException HTML response dönerse (WAF engeli)
     */
    public static void checkWafBlock(String responseText) {
        if (responseText != null && responseText.trim().startsWith("<")) {
            String preview = responseText.length() > 500
                    ? responseText.substring(0, 500)
                    : responseText;
            throw new TefasWafBlockedException(preview);
        }
    }

    /**
     * Map'i form-urlencoded string'e çevirir.
     * 
     * @param form Form parametreleri Map'i
     * @return URL encoded form string (örn: "key1=value1&key2=value2")
     */
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

    /**
     * Standart API request context oluşturur.
     * Storage state (cookies) ve default headers ile.
     * 
     * @param playwright Playwright instance
     * @param baseUrl    Base URL
     * @param context    Browser context (storage state için)
     * @param headers    Ekstra HTTP headers
     * @return APIRequestContext
     */
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

    /**
     * Sayfada JavaScript ile AJAX/fetch kullanarak API çağrısı yapar.
     * Gerçek browser davranışını simüle eder (WAF bypass için).
     * 
     * @param page     Playwright Page objesi
     * @param apiUrl   API endpoint URL'i (örn: "/api/DB/BindHistoryInfo")
     * @param formData Form parametreleri Map'i
     */
    public static void triggerApiCallViaJavaScript(Page page, String apiUrl, Map<String, String> formData) {
        String script = buildAjaxScript(apiUrl, formData);
        page.evaluate(script);
    }

    /**
     * jQuery veya fetch kullanarak AJAX çağrısı yapan JavaScript kodu oluşturur.
     * 
     * @param apiUrl   API endpoint URL'i
     * @param formData Form parametreleri Map'i
     * @return JavaScript kodu
     */
    private static String buildAjaxScript(String apiUrl, Map<String, String> formData) {
        StringBuilder script = new StringBuilder();
        script.append("(function() {");

        // URLSearchParams oluştur (hem jQuery hem fetch için ortak)
        script.append("var params = new URLSearchParams();");
        for (Map.Entry<String, String> entry : formData.entrySet()) {
            script.append("params.append(")
                    .append(escapeJsForJs(entry.getKey()))
                    .append(", ")
                    .append(escapeJsForJs(entry.getValue()))
                    .append(");");
        }
        script.append("var formDataString = params.toString();");

        // jQuery ile AJAX çağrısı
        script.append("if (typeof $ !== 'undefined') {");
        script.append("$.ajax({");
        script.append("url: ").append(escapeJsForJs(apiUrl)).append(",");
        script.append("type: 'POST',");
        script.append("contentType: 'application/x-www-form-urlencoded; charset=UTF-8',");
        script.append("processData: false,"); // jQuery'nin otomatik serialize'ını devre dışı bırak
        script.append("data: formDataString,"); // Manuel serialize edilmiş string kullan
        script.append("success: function(data) { console.log('API call successful'); },");
        script.append("error: function(xhr, status, error) { console.log('API call error:', error); }");
        script.append("});");
        script.append("} else {");

        // Fetch fallback
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

    /**
     * JavaScript string literal'ı için escape eder (JSON.stringify benzeri).
     */
    private static String escapeJsForJs(String value) {
        if (value == null) {
            return "''";
        }
        // JSON.stringify kullanarak güvenli escape
        return "\"" + value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t") + "\"";
    }

    /**
     * JavaScript string'lerini escape eder.
     * 
     * @param value Escape edilecek string
     * @return Escape edilmiş string
     */
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

    /**
     * Response'un başarılı olup olmadığını kontrol eder ve WAF engelini kontrol
     * eder.
     * 
     * @param response Playwright Response objesi
     * @return Response text
     * @throws RuntimeException WAF engeli veya HTTP hatası varsa
     */
    public static String validateResponse(Response response) {
        String text = response.text();

        // WAF engeli kontrolü
        checkWafBlock(text);

        // Status kontrolü
        int status = response.status();
        if (status == 401 || status == 403) {
            throw new TefasClientException("Unauthorized/Forbidden: " + status);
        }
        if (status < 200 || status >= 300) {
            throw new TefasClientException("Upstream error " + status + " " + response.statusText());
        }

        return text;
    }

    /**
     * Fills a single input field with retry logic and validation.
     * Uses first() to handle multiple matches (strict mode violation).
     * 
     * @param page      Playwright Page objesi
     * @param selector  CSS selector for the input field
     * @param value     Value to fill
     * @param fieldName Field name for logging purposes
     * @param config    Playwright configuration
     * @throws RuntimeException if filling fails
     */
    public static void fillInputField(Page page, String selector, String value, String fieldName,
            PlaywrightConfig config) {
        try {
            // Use first() to handle multiple matches (strict mode violation)
            Locator locator = page.locator(selector).first();

            // Wait for element to be visible
            locator.waitFor(new Locator.WaitForOptions().setTimeout(config.getElementWaitTimeoutMs()));

            // Clear existing value first (watermark might be present)
            locator.clear();
            Thread.sleep(config.getInputClearWaitMs());

            // Fill the field
            locator.fill(value);
            Thread.sleep(config.getInputFillWaitMs());

            // Verify value was set
            String actualValue = locator.inputValue();
            if (!actualValue.equals(value)) {
                log.warn("{} value mismatch. Expected: {}, Got: {}", fieldName, value, actualValue);
            }
        } catch (Exception e) {
            throw new TefasClientException(
                    "Failed to fill " + fieldName + " field with selector '" + selector + "': " + e.getMessage(), e);
        }
    }
}

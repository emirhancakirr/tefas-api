package com.tefasfundapi.tefasFundAPI.client;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Playwright ile TEFAS API çağrıları için ortak utility metodları.
 * DRY prensibi: Tekrar eden kodları buraya topladık.
 */
public final class PlaywrightHelper {

    private static final String USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    private PlaywrightHelper() {
        // Utility class - instantiate edilemez
    }

    /**
     * Standart browser launch options oluşturur.
     * Bot detection'ı bypass etmek için gerekli ayarları içerir.
     */
    public static BrowserType.LaunchOptions createLaunchOptions() {
        return new BrowserType.LaunchOptions()
                .setHeadless(true)
                .setArgs(List.of("--disable-blink-features=AutomationControlled"));
    }

    /**
     * Standart browser context options oluşturur.
     * Gerçekçi browser ayarları (User-Agent, viewport, locale, timezone) içerir.
     */
    public static Browser.NewContextOptions createContextOptions() {
        return new Browser.NewContextOptions()
                .setUserAgent(USER_AGENT)
                .setViewportSize(1920, 1080)
                .setLocale("tr-TR")
                .setTimezoneId("Europe/Istanbul");
    }

    /**
     * WAF/oturum için sayfaya gidip network idle bekler.
     * TEFAS'ın WAF korumasını bypass etmek için session oluşturur.
     * 
     * @param page Playwright Page objesi
     * @param url  Navigate edilecek URL
     * @throws RuntimeException Navigation başarısız olursa
     */
    public static void navigateForSession(Page page, String url) {
        try {
            page.navigate(url);
            // LOAD durumu: Sayfa yüklendi, tüm kaynaklar indirilmedi de olabilir
            page.waitForLoadState(LoadState.LOAD,
                    new Page.WaitForLoadStateOptions().setTimeout(30_000));
            // WAF için daha uzun bekleme
            page.waitForTimeout(10000);
        } catch (Exception e) {
            throw new RuntimeException("Failed to navigate to " + url + ": " + e.getMessage(), e);
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
            throw new RuntimeException("Unauthorized/Forbidden: " + status);
        }
        if (status < 200 || status >= 300) {
            throw new RuntimeException("Upstream " + status + " " + response.statusText() + " body=" + body);
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
            throw new RuntimeException("TEFAS WAF blocked the request. Response: " + preview);
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
            throw new RuntimeException("Unauthorized/Forbidden: " + status);
        }
        if (status < 200 || status >= 300) {
            throw new RuntimeException("Upstream error " + status + " " + response.statusText());
        }

        return text;
    }
}

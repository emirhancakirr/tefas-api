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
            // Network idle için bekle (maksimum 20 saniye)
            page.waitForLoadState(LoadState.NETWORKIDLE,
                    new Page.WaitForLoadStateOptions().setTimeout(20_000));
            // Ekstra bekleme (WAF için)
            page.waitForTimeout(1000);
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
}

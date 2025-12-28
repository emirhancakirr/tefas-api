package com.tefasfundapi.tefasFundAPI.client;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.RequestOptions;
import com.tefasfundapi.tefasFundAPI.config.PlaywrightConfig;
import com.tefasfundapi.tefasFundAPI.dto.FundReturnQuery;
import com.tefasfundapi.tefasFundAPI.exception.TefasClientException;
import com.tefasfundapi.tefasFundAPI.exception.TefasTimeoutException;
import com.tefasfundapi.tefasFundAPI.exception.TefasWafBlockedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Playwright tabanlı TEFAS istemcisi.
 * - WAF/oturum çerezleri için ilgili sayfaya gidip session oluşturur.
 * - Aynı session ile hedef API uçlarına (XHR) istek atar.
 */
@Component
public class FundsClient {
    private static final Logger log = LoggerFactory.getLogger(FundsClient.class);

    private final PlaywrightConfig config;

    public FundsClient(PlaywrightConfig config) {
        this.config = config;
    }

    private Map<String, String> getDefaultHeaders() {
        return Map.of(
                "Origin", config.getBaseUrl(),
                "X-Requested-With", "XMLHttpRequest",
                "Accept", "application/json, text/javascript, */*; q=0.01",
                "Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
    }

    /*
     * =======================================================================
     * 1) Karşılaştırma (BindComparisonFundReturns)
     * =======================================================================
     */

    /** /api/DB/BindComparisonFundReturns çağrısı (form-encoded). */
    public String fetchComparisonFundReturns(FundReturnQuery q) {
        try (Playwright pw = Playwright.create()) {
            log.debug("fetchComparisonFundReturns started for query: {}", q);
            try (Browser browser = pw.chromium().launch(PlaywrightHelper.createLaunchOptions(config))) {
                BrowserContext ctx = browser.newContext(PlaywrightHelper.createContextOptions(config));
                try {
                    Page page = ctx.newPage();

                    // Response'u dinlemek için promise oluştur
                    java.util.concurrent.CompletableFuture<Response> responseFuture = new java.util.concurrent.CompletableFuture<>();

                    page.onResponse(response -> {
                        String url = response.url();
                        if (url.contains(config.getComparisonApiEndpoint())) {
                            if (!responseFuture.isDone()) {
                                responseFuture.complete(response);
                            }
                        }
                    });

                    // Sayfaya git
                    PlaywrightHelper.navigateForSession(page, config.getComparisonReferer(), config);

                    // Sayfa yüklendikten sonra biraz bekle
                    Thread.sleep(config.getPageLoadWaitMs());

                    // Form parametrelerini sayfaya gönder (JavaScript ile)
                    // veya sayfada filtreleme yap

                    Response response = responseFuture.get(30, java.util.concurrent.TimeUnit.SECONDS);

                    String json = response.text();

                    // HTML dönerse (WAF engeli) hata fırlat
                    if (json.trim().startsWith("<")) {
                        String preview = json.length() > 500 ? json.substring(0, 500) : json;
                        throw new TefasWafBlockedException(preview);
                    }

                    // Status kontrolü
                    if (response.status() == 401 || response.status() == 403) {
                        throw new TefasClientException("Unauthorized/Forbidden: " + response.status());
                    }
                    if (response.status() < 200 || response.status() >= 300) {
                        throw new TefasClientException(
                                "Upstream error " + response.status() + " " + response.statusText());
                    }

                    return json;
                } finally {
                    ctx.close();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TefasClientException("TEFAS/BindComparisonFundReturns interrupted", e);
        } catch (java.util.concurrent.TimeoutException e) {
            throw new TefasTimeoutException("fetchComparisonFundReturns", 30000, e);
        } catch (TefasWafBlockedException e) {
            // Re-throw WAF exceptions as-is
            throw e;
        } catch (TefasClientException e) {
            // Re-throw client exceptions as-is
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error in fetchComparisonFundReturns", e);
            throw new TefasClientException("TEFAS/BindComparisonFundReturns çağrısı başarısız: " + e.getMessage(), e);
        }
    }

    /*
     * =======================================================================
     * 2) Fon liste/arama (endpoint/body'yi Sniffer ile birebir doğrula)
     * =======================================================================
     */

    /**
     * Fon arama/listesi için XHR çağrısı.
     * NOT: Aşağıdaki endpoint/body KEY’LERİ örnek. Playwright Sniffer ile
     * gerçekte ne gönderiliyorsa birebir buraya yaz.
     * 
     */
    public String fetchFundsJson(String query, List<String> codes) {
        try (Playwright pw = Playwright.create()) {
            try (Browser browser = pw.chromium().launch(PlaywrightHelper.createLaunchOptions(config))) {
                BrowserContext ctx = browser.newContext(PlaywrightHelper.createContextOptions(config));
                try {
                    Page page = ctx.newPage();
                    PlaywrightHelper.navigateForSession(page, config.getComparisonReferer(), config);

                    // Sayfa yüklendikten sonra biraz bekle (WAF için)
                    Thread.sleep(config.getPageLoadWaitMs());

                    APIRequestContext api = createApiContext(pw, ctx, config.getComparisonReferer());
                    try {
                        String body = buildFundsFormBody(query, codes);
                        APIResponse res = api.post(config.getComparisonApiEndpoint(),
                                RequestOptions.create().setData(body));
                        String text = res.text();

                        // HTML dönerse (WAF engeli) hata fırlat
                        PlaywrightHelper.checkWafBlock(text);

                        PlaywrightHelper.ensureOk(res, text);
                        return text;
                    } finally {
                        api.dispose();
                    }
                } finally {
                    ctx.close();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TefasClientException("TEFAS/FundsSearch interrupted", e);
        } catch (TefasWafBlockedException e) {
            // Re-throw WAF exceptions as-is
            throw e;
        } catch (TefasClientException e) {
            // Re-throw client exceptions as-is
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error in fetchFundsJson", e);
            throw new TefasClientException("TEFAS/FundsSearch çağrısı başarısız: " + e.getMessage(), e);
        }
    }

    /*
     * =======================================================================
     * Ortak yardımcılar
     * =======================================================================
     */

    private APIRequestContext createApiContext(Playwright pw, BrowserContext ctx, String referer) {
        Map<String, String> headers = new HashMap<>(getDefaultHeaders());
        headers.put("Referer", referer);
        headers.put("User-Agent",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        headers.put("Accept-Language", "tr-TR,tr;q=0.9,en-US;q=0.8,en;q=0.7");
        headers.put("Accept-Encoding", "gzip, deflate, br");
        headers.put("Connection", "keep-alive");
        headers.put("Sec-Fetch-Dest", "empty");
        headers.put("Sec-Fetch-Mode", "cors");
        headers.put("Sec-Fetch-Site", "same-origin");

        return pw.request().newContext(
                new APIRequest.NewContextOptions()
                        .setBaseURL(config.getBaseUrl())
                        .setStorageState(ctx.storageState()) // çerez/localStorage kopyala
                        .setExtraHTTPHeaders(headers)
                        .setTimeout((double) config.getNavigationTimeoutMs()));
    }

    /**
     * Sayfaya gidip /api/DB/BindComparisonFundReturns endpoint'ini dinler ve JSON
     * response döndürür.
     * Sayfada otomatik olarak tetiklenen API isteğini yakalar.
     */
    public String fetchFunds() {
        try (Playwright pw = Playwright.create()) {
            try (Browser browser = pw.chromium().launch(PlaywrightHelper.createLaunchOptions(config))) {
                BrowserContext ctx = browser.newContext(PlaywrightHelper.createContextOptions(config));
                try {
                    Page page = ctx.newPage();

                    // Response'u dinlemek için promise oluştur
                    java.util.concurrent.CompletableFuture<Response> responseFuture = new java.util.concurrent.CompletableFuture<>();

                    page.onResponse(response -> {
                        String url = response.url();
                        if (url.contains(config.getComparisonApiEndpoint())) {
                            responseFuture.complete(response);
                        }
                    });

                    // Sayfaya git
                    PlaywrightHelper.navigateForSession(page, config.getComparisonReferer(), config);

                    // Sayfa yüklendikten sonra biraz bekle
                    Thread.sleep(config.getPageLoadWaitMs());

                    // Response'u bekle (maksimum 30 saniye)
                    Response response = responseFuture.get(30, java.util.concurrent.TimeUnit.SECONDS);

                    String json = response.text();

                    // HTML dönerse (WAF engeli) hata fırlat
                    if (json.trim().startsWith("<")) {
                        String preview = json.length() > 500 ? json.substring(0, 500) : json;
                        throw new TefasWafBlockedException(preview);
                    }

                    // Status kontrolü
                    if (response.status() == 401 || response.status() == 403) {
                        throw new TefasClientException("Unauthorized/Forbidden: " + response.status());
                    }
                    if (response.status() < 200 || response.status() >= 300) {
                        throw new TefasClientException(
                                "Upstream error " + response.status() + " " + response.statusText());
                    }

                    return json;
                } finally {
                    ctx.close();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TefasClientException("TEFAS/fetchFunds interrupted", e);
        } catch (java.util.concurrent.TimeoutException e) {
            throw new TefasTimeoutException("fetchFunds", 30000, e);
        } catch (TefasWafBlockedException e) {
            // Re-throw WAF exceptions as-is
            throw e;
        } catch (TefasClientException e) {
            // Re-throw client exceptions as-is
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error in fetchFunds", e);
            throw new TefasClientException("TEFAS/fetchFunds çağrısı başarısız: " + e.getMessage(), e);
        }
    }

    /* ---------- Body builders ---------- */

    /** Comparison form body: Sniffer’da gördüğün alanları birebir kullanıyoruz. */
    private static String buildComparisonFormBody(FundReturnQuery q) {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("calismatipi", nz(q.getCalismatipi()));
        form.put("fontip", nz(q.getFontip()));
        form.put("sfontur", nz(q.getSfontur()));
        form.put("kurucukod", nz(q.getKurucukod()));
        form.put("fongrup", nz(q.getFongrup()));
        form.put("bastarih", nz(q.getBastarih())); // YYYY-MM-DD
        form.put("bittarih", nz(q.getBittarih())); // YYYY-MM-DD
        form.put("fonturkod", nz(q.getFonturkod())); // "ABC,DEF"
        form.put("fonunvantip", nz(q.getFonunvantip()));
        form.put("strperiod", nz(q.getStrperiod())); // "m1,m3,..." vb.
        form.put("islemdurum", nz(q.getIslemdurum()));
        return PlaywrightHelper.toFormEncoded(form);
    }

    /**
     * Funds search body - Fon Karşılaştırma API formatını kullanıyor.
     * Belirli fon kodları için bilgi almak için fonturkod parametresi kullanılıyor.
     */
    private static String buildFundsFormBody(String query, List<String> codes) {
        Map<String, String> form = new LinkedHashMap<>();
        // Tüm parametreleri boş bırak, sadece fon kodlarını gönder
        form.put("calismatipi", "");
        form.put("fontip", "");
        form.put("sfontur", "");
        form.put("kurucukod", "");
        form.put("fongrup", "");
        form.put("bastarih", "");
        form.put("bittarih", "");
        // Fon kodlarını virgülle ayırarak gönder (örn: "TLY,ABC")
        form.put("fonturkod", codes == null || codes.isEmpty() ? "" : String.join(",", codes));
        form.put("fonunvantip", "");
        form.put("strperiod", ""); // Periyot bilgisi gerekmiyorsa boş
        form.put("islemdurum", "");
        return PlaywrightHelper.toFormEncoded(form);
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
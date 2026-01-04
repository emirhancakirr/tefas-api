package com.tefasfundapi.tefasFundAPI.client;

import com.microsoft.playwright.*;
import com.tefasfundapi.tefasFundAPI.config.PlaywrightConfig;
import com.tefasfundapi.tefasFundAPI.dto.FundReturnQuery;
import com.tefasfundapi.tefasFundAPI.exception.TefasClientException;
import com.tefasfundapi.tefasFundAPI.exception.TefasTimeoutException;
import com.tefasfundapi.tefasFundAPI.exception.TefasWafBlockedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
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
    public String fetchFundPerformance(String fundCode, LocalDate start, LocalDate end) {
        try (Playwright pw = Playwright.create()) {
            log.debug("fetchFundPerformance started for fundCode={}, start={}, end={}", fundCode, start, end);

            BrowserType.LaunchOptions launchOptions = PlaywrightHelper.createLaunchOptions(config)
                    .setHeadless(false);

            try (Browser browser = pw.chromium().launch(launchOptions)) {
                BrowserContext ctx = browser.newContext(PlaywrightHelper.createContextOptions(config));
                try {
                    Page page = ctx.newPage();

                    PlaywrightHelper.setupRequestLogger(page, config.getComparisonApiEndpoint());
                    PlaywrightHelper.navigateAndWaitForWaf(page, config.getComparisonReferer(), config);
                    PlaywrightHelper.fillDateFields(page, start, end, config);

                    // Setup response listener AFTER fillDateFields to avoid capturing initial page
                    // load response
                    // This way we only capture the response from clicking the search button
                    log.info(
                            "🎯 Setting up response listener AFTER fillDateFields to capture only button click response...");
                    java.util.concurrent.BlockingQueue<PlaywrightHelper.ResponseWithBody> responseQueue = PlaywrightHelper
                            .setupResponseListener(page, config.getComparisonApiEndpoint(), config);
                    log.info("✅ Response listener ready, queue size: {}", responseQueue.size());

                    // Button'a tıkla
                    log.info("🔘 Clicking search button...");
                    PlaywrightHelper.clickSearchButton(page, config);
                    log.info("✅ Button clicked, queue size: {}", responseQueue.size());

                    // Kısa bekleme
                    Thread.sleep(2000);
                    log.info("📊 After 2s wait, queue size: {}", responseQueue.size());

                    // İkinci POST ~30 saniye sürüyor, timeout'u artır
                    log.info("⏳ Starting waitForLastApiResponse...");
                    String apiResponse = PlaywrightHelper.waitForLastApiResponse(
                            page,
                            responseQueue,
                            config.getComparisonApiEndpoint(),
                            config,
                            5000, // Son response'tan 5sn sonra bitir
                            1); // En az 1 response (2 değil, sadece tarih filtreli istiyoruz)

                    // HTML dönerse (WAF engeli) hata fırlat
                    if (apiResponse.trim().startsWith("<")) {
                        String preview = apiResponse.length() > 500 ? apiResponse.substring(0, 500) : apiResponse;
                        throw new TefasWafBlockedException(preview);
                    }

                    log.debug("✅ API response received (last of all), response length: {}", apiResponse.length());
                    return apiResponse;

                } finally {
                    ctx.close();
                }
            } catch (TefasWafBlockedException e) {
                throw e;
            } catch (TefasClientException e) {
                throw e;
            }
        } catch (Exception e) {
            log.error("Unexpected error in fetchFundPerformance for fundCode={}, start={}, end={}",
                    fundCode, start, end, e);
            throw new TefasClientException("TEFAS/fetchFundPerformance çağrısı başarısız: " + e.getMessage(), e);
        }
    }
    // PlaywrightHelper.java'ya ekle

    /**
     * Extracts fund performance data from table_fund_returns.
     * 
     * @param page     Playwright Page objesi
     * @param fundCode Fon kodu (filtering için, null ise tüm fonlar)
     * @param config   Playwright konfigürasyonu
     * @return JSON string with fund performance data
     */
    public static String extractFundReturnsTableData(Page page, String fundCode, PlaywrightConfig config) {
        try {
            // Wait for table rows
            page.waitForSelector("#table_fund_returns tbody tr:not(.dataTables_empty)",
                    new Page.WaitForSelectorOptions().setTimeout(config.getElementWaitTimeoutMs()));

            // Wait for DataTables to fully render
            Thread.sleep(config.getTableDataExtractionWaitMs());

            // Extract raw data
            String rawJson = (String) page.evaluate("""
                    (function() {
                        const table = document.querySelector('#table_fund_returns');
                        if (!table) {
                            return JSON.stringify({ data: [] });
                        }

                        const rows = table.querySelectorAll('tbody tr');
                        const data = [];

                        rows.forEach(row => {
                            if (row.classList.contains('dataTables_empty')) {
                                return;
                            }

                            const cells = row.querySelectorAll('td');
                            if (cells.length >= 4) {
                                data.push({
                                    fonKodu: cells[0].textContent.trim(),
                                    fonAdi: cells[1].textContent.trim(),
                                    semsiyeFonTuru: cells[2].textContent.trim(),
                                    getiri: cells[3].textContent.trim()
                                });
                            }
                        });

                        return JSON.stringify({ data: data });
                    })();
                    """);

            log.debug("Raw extracted JSON length: {}", rawJson.length());

            // Filter by fund code if provided (in Java, not JavaScript)
            if (fundCode != null && !fundCode.isBlank()) {
                // Parse and filter JSON here if needed
                // Or filter in JavaScript above
            }

            return rawJson;

        } catch (Exception e) {
            throw new TefasClientException("Failed to extract fund returns table data: " + e.getMessage(), e);
        }
    }

    /*
     * =======================================================================
     * Ortak yardımcılar
     * =======================================================================
     */

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
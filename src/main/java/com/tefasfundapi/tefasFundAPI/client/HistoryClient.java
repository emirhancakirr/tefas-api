package com.tefasfundapi.tefasFundAPI.client;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * TEFAS tarihsel veri (NAV) istemcisi.
 * WAF bypass için sayfa üzerinden response dinleme yaklaşımını kullanır.
 */
@Component
public class HistoryClient {
    private static final String BASE_URL = "https://www.tefas.gov.tr";
    private static final String REFERER = BASE_URL + "/TarihselVeriler.aspx";
    private static final String API = "/api/DB/BindHistoryInfo";
    private static final int WAF_WAIT_MS = 5000;
    private static final int RESPONSE_TIMEOUT_SECONDS = 30;

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
            try (Browser browser = pw.chromium().launch(PlaywrightHelper.createLaunchOptions())) {
                BrowserContext ctx = browser.newContext(PlaywrightHelper.createContextOptions());
                try {
                    Page page = ctx.newPage();
                    CompletableFuture<Response> responseFuture = setupResponseListener(page);

                    navigateAndWaitForWaf(page);

                    Map<String, String> form = buildHistoryForm(fundCode, start, end);
                    PlaywrightHelper.triggerApiCallViaJavaScript(page, API, form);

                    Response response = responseFuture.get(RESPONSE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                    return PlaywrightHelper.validateResponse(response);
                } finally {
                    ctx.close();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("TEFAS/fetchHistoryJson interrupted", e);
        } catch (java.util.concurrent.TimeoutException e) {
            throw new RuntimeException(
                    "TEFAS/fetchHistoryJson timeout: API response not received within " + RESPONSE_TIMEOUT_SECONDS
                            + " seconds",
                    e);
        } catch (Exception e) {
            throw new RuntimeException("TEFAS/fetchHistoryJson çağrısı başarısız: " + e.getMessage(), e);
        }
    }

    /**
     * API response'unu dinlemek için listener kurar.
     */
    private CompletableFuture<Response> setupResponseListener(Page page) {
        CompletableFuture<Response> responseFuture = new CompletableFuture<>();

        page.onResponse(response -> {
            if (response.url().contains(API) && !responseFuture.isDone()) {
                responseFuture.complete(response);
            }
        });

        return responseFuture;
    }

    /**
     * Sayfaya gider, WAF challenge'ını bekler.
     */
    private void navigateAndWaitForWaf(Page page) throws InterruptedException {
        PlaywrightHelper.navigateForSession(page, REFERER);
        page.waitForLoadState(LoadState.NETWORKIDLE);
        Thread.sleep(WAF_WAIT_MS);
    }

    /**
     * TEFAS API'sine gönderilecek form parametrelerini oluşturur.
     * Gerçek site payload formatına uygun olarak tüm parametreleri içerir.
     * 
     * @param fundCode Fon kodu (örn: "AAK")
     * @param start    Başlangıç tarihi
     * @param end      Bitiş tarihi
     * @return Form parametreleri Map'i
     */
    private static Map<String, String> buildHistoryForm(String fundCode, LocalDate start, LocalDate end) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

        Map<String, String> form = new LinkedHashMap<>();
        form.put("fontip", "YAT");
        form.put("sfontur", "");
        form.put("fonkod", "");
        form.put("fongrup", "");
        form.put("bastarih", start.format(formatter));
        form.put("bittarih", end.format(formatter));
        form.put("fonturkod", fundCode != null ? fundCode : "");
        form.put("fonunvantip", "");
        form.put("kurucukod", "");

        return form;
    }
}
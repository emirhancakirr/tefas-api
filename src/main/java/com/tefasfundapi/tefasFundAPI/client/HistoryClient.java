package com.tefasfundapi.tefasFundAPI.client;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.RequestOptions;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class HistoryClient {
    private static final String BASE_URL = "https://www.tefas.gov.tr";
    private static final String REFERER = BASE_URL + "/TarihselVeriler.aspx";
    private static final String API = "/api/DB/BindHistoryInfo";

    private static final Map<String, String> DEFAULT_HEADERS = Map.of(
            "Origin", BASE_URL,
            "Referer", REFERER,
            "X-Requested-With", "XMLHttpRequest",
            "Accept", "application/json, text/javascript, */*; q=0.01",
            "Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

    /** Tek fon ve tarih aralığı için fiyat/NAV+diğer sütunlar JSON'u döner. */
    public String fetchHistoryJson(String fundCode, LocalDate start, LocalDate end) {
        try (Playwright pw = Playwright.create()) {
            try (Browser browser = pw.chromium().launch(PlaywrightHelper.createLaunchOptions())) {
                BrowserContext ctx = browser.newContext(PlaywrightHelper.createContextOptions());
                try {
            Page page = ctx.newPage();

                    // Önce sayfaya git ve session oluştur
                    PlaywrightHelper.navigateForSession(page, REFERER);

                    // Sayfa yüklendikten sonra biraz bekle (WAF için)
                    Thread.sleep(2000);

                    APIRequestContext api = PlaywrightHelper.createApiContext(
                            pw, BASE_URL, ctx, DEFAULT_HEADERS);
            try {
                Map<String, String> form = new LinkedHashMap<>();
                        form.put("fonturkod", fundCode);
                        form.put("bastarih", start.toString());
                        form.put("bittarih", end.toString());
                        String body = PlaywrightHelper.toFormEncoded(form);

                APIResponse res = api.post(API, RequestOptions.create().setData(body));
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
            throw new RuntimeException("TEFAS/fetchHistoryJson interrupted", e);
        } catch (Exception e) {
            throw new RuntimeException("TEFAS/fetchHistoryJson çağrısı başarısız: " + e.getMessage(), e);
        }
    }

}
package com.tefasfundapi.tefasFundAPI.client;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.RequestOptions;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class HistoryClient {
    private static final String BASE_URL = "https://www.tefas.gov.tr";
    private static final String REFERER  = BASE_URL + "/TarihselVeriler.aspx";
    private static final String API      = "/api/DB/BindHistoryInfo";

    private static final Map<String, String> DEFAULT_HEADERS = Map.of(
            "Origin", BASE_URL,
            "Referer", REFERER,
            "X-Requested-With", "XMLHttpRequest",
            "Accept", "application/json, text/javascript, */*; q=0.01",
            "Content-Type", "application/x-www-form-urlencoded; charset=UTF-8"
    );

    /** Tek fon ve tarih aralığı için fiyat/NAV+diğer sütunlar JSON’u döner. */
    public String fetchHistoryJson(String fundCode, LocalDate start, LocalDate end) {
        try (Playwright pw = Playwright.create();
             Browser browser = pw.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
             BrowserContext ctx = browser.newContext()) {

            Page page = ctx.newPage();
            page.navigate(REFERER);
            page.waitForLoadState(LoadState.NETWORKIDLE);

            APIRequestContext api = pw.request().newContext(
                    new APIRequest.NewContextOptions()
                            .setBaseURL(BASE_URL)
                            .setStorageState(ctx.storageState())
                            .setExtraHTTPHeaders(DEFAULT_HEADERS));
            try {
                Map<String, String> form = new LinkedHashMap<>();
                form.put("fonturkod", fundCode);       // Sniffer ile doğrula
                form.put("bastarih",  start.toString());
                form.put("bittarih",  end.toString());
                String body = toFormEncoded(form);

                APIResponse res = api.post(API, RequestOptions.create().setData(body));
                String text = res.text();
                ensureOk(res, text);
                return text;
            } finally {
                api.dispose();
            }
        } catch (Exception e) {
            throw new RuntimeException("HistoryClient çağrısı başarısız: " + e.getMessage(), e);
        }
    }

    private static void ensureOk(APIResponse res, String body) {
        int s = res.status();
        if (s == 401 || s == 403) throw new RuntimeException("Unauthorized/Forbidden: " + s);
        if (s < 200 || s >= 300)  throw new RuntimeException("Upstream " + s + " " + res.statusText() + " body=" + body);
    }

    private static String toFormEncoded(Map<String, String> form) {
        StringBuilder sb = new StringBuilder();
        for (var e : form.entrySet()) {
            if (sb.length() > 0) sb.append('&');
            sb.append(URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8))
                    .append('=')
                    .append(URLEncoder.encode(e.getValue() == null ? "" : e.getValue(), StandardCharsets.UTF_8));
        }
        return sb.toString();
    }
}
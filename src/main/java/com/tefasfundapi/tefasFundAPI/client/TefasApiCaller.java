package com.tefasfundapi.tefasFundAPI.client;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.RequestOptions;

import java.nio.file.Paths;
import java.util.Map;

public class TefasApiCaller {
    private static final String TARGET_API = "https://www.tefas.gov.tr/api/DB/BindHistoryInfo";
    private static final String REFERER = "https://www.tefas.gov.tr/TarihselVeriler.aspx";
    private static final String STATE_FILE = "com/tefasfundapi/tefasFundAPI/client/storageState.json";

    /**
     * BindHistoryInfo için programatik çağrı.
     * @param body     : Sniffer'dan gördüğün body'yi buraya koy (JSON ise aynen, form-encoded ise ona göre).
     * @param headers  : Gerekli ekstra header'lar (örn. RequestVerificationToken, X-Requested-With vs.)
     */
    public String postHistoryInfo(String body, Map<String, String> headers) {
        try (Playwright pw = Playwright.create()) {
            APIRequestContext request = pw.request().newContext(
                    new APIRequest.NewContextOptions()
                            .setStorageStatePath(Paths.get(STATE_FILE)) // cookie'ler otomatik eklenecek
                            .setExtraHTTPHeaders(new java.util.HashMap<>() {{
                                put("Referer", REFERER);
                                put("Accept", "application/json, text/javascript, */*; q=0.01");
                                put("X-Requested-With", "XMLHttpRequest");
                                // content-type'ı body tipine göre ayarla:
                                // JSON ise:
                                put("Content-Type", "application/json; charset=UTF-8");
                                // Form-urlencoded ise bu satırı değiştir:
                                // put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
                                if (headers != null) headers.forEach(this::put);
                            }})
            );

            APIResponse resp = request.post(TARGET_API, RequestOptions.create().setData(body));
            int status = resp.status();
            String text = resp.text();

            if (status == 401 || status == 403) {
                // cookie eskimiş olabilir → burada Session refresh akışını tetikleyip (Sniffer/SessionManager),
                // storageState'i yenileyip 1 kez daha deneyebilirsin.
                throw new RuntimeException("Unauthorized/Forbidden from upstream: " + status);
            }
            if (status < 200 || status >= 300) {
                throw new RuntimeException("Upstream error status: " + status + " body=" + text);
            }
            return text;
        }
    }

    public static void main(String[] args) {
        // ÖRNEK: Sniffer'da gördüğün payload'ı aynen koy.
        String exampleBody = """
      {
        "StartDate":"2025-09-01",
        "EndDate":"2025-09-05",
        "FundCodes":["ABC","DEF"]
      }
      """;

        TefasApiCaller caller = new TefasApiCaller();
        String json = caller.postHistoryInfo(exampleBody, Map.of(
                // Örnek: anti-forgery header'ı gerekiyorsa (sniffer’da gördüğün isim/değeri kullan)
                // "RequestVerificationToken", "xyz-123"
        ));
        System.out.println(json);
    }
}
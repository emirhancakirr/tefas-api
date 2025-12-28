package com.tefasfundapi.tefasFundAPI.client;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.RequestOptions;
import com.tefasfundapi.tefasFundAPI.config.PlaywrightConfig;
import com.tefasfundapi.tefasFundAPI.exception.TefasClientException;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ComparisonClient {
    private final PlaywrightConfig config;

    public ComparisonClient(PlaywrightConfig config) {
        this.config = config;
    }

    private Map<String, String> getDefaultHeaders() {
        return Map.of(
                "Origin", config.getBaseUrl(),
                "Referer", config.getComparisonReferer(),
                "X-Requested-With", "XMLHttpRequest",
                "Accept", "application/json, text/javascript, */*; q=0.01",
                "Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
    }

    /** periods veya start-end'e göre (aynı uç) performans/changes JSON'u döner. */
    public String postComparisonForm(Map<String, String> formParams) {
        try (Playwright pw = Playwright.create();
                Browser browser = pw.chromium().launch(PlaywrightHelper.createLaunchOptions(config));
                BrowserContext ctx = browser.newContext(PlaywrightHelper.createContextOptions(config))) {

            Page page = ctx.newPage();
            PlaywrightHelper.navigateForSession(page, config.getComparisonReferer(), config);
            page.waitForLoadState(LoadState.NETWORKIDLE);

            APIRequestContext api = pw.request().newContext(
                    new APIRequest.NewContextOptions()
                            .setBaseURL(config.getBaseUrl())
                            .setStorageState(ctx.storageState())
                            .setExtraHTTPHeaders(getDefaultHeaders()));
            try {
                String body = PlaywrightHelper.toFormEncoded(formParams);
                APIResponse res = api.post(config.getComparisonApiEndpoint(), RequestOptions.create().setData(body));
                String text = res.text();
                PlaywrightHelper.ensureOk(res, text);
                return text;
            } finally {
                api.dispose();
            }
        } catch (TefasClientException e) {
            // Re-throw custom exceptions as-is
            throw e;
        } catch (Exception e) {
            throw new TefasClientException("ComparisonClient çağrısı başarısız: " + e.getMessage(), e);
        }
    }
}
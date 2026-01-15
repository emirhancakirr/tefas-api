package com.tefasfundapi.tefasFundAPI.client;

import com.microsoft.playwright.*;
import com.tefasfundapi.tefasFundAPI.config.PlaywrightConfig;
import com.tefasfundapi.tefasFundAPI.exception.TefasClientException;
import com.tefasfundapi.tefasFundAPI.exception.TefasTimeoutException;
import com.tefasfundapi.tefasFundAPI.exception.TefasWafBlockedException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * TEFAS tarihsel veri (NAV) istemcisi.
 * WAF bypass için sayfa üzerinden response dinleme yaklaşımını kullanır.
 */
@Component
public class HistoryClient {
    private static final Logger log = LoggerFactory.getLogger(HistoryClient.class);

    private final PlaywrightConfig config;

    public HistoryClient(PlaywrightConfig config) {
        this.config = config;
    }

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
            log.debug("fetchHistoryJson started for fundCode={}, start={}, end={}", fundCode, start, end);
            // Launch browser with configuration
            BrowserType.LaunchOptions launchOptions = PlaywrightHelper.createLaunchOptions(config);
            try (Browser browser = pw.chromium().launch(launchOptions)) {
                BrowserContext ctx = browser.newContext(PlaywrightHelper.createContextOptions(config));
                try {
                    Page page = ctx.newPage();

                    // Request logging for debugging (optional, can remove if not needed)
                    PlaywrightHelper.setupRequestLogger(page, config.getHistoryApiEndpoint());
                    PlaywrightHelper.navigateAndWaitForWaf(page, config.getHistoryReferer(), config);
                    PlaywrightHelper.fillDateFields(page, start, end, config);

                    log.info("Setting up response listener to capture button click response...");
                    java.util.concurrent.BlockingQueue<PlaywrightHelper.ResponseWithBody> responseQueue = PlaywrightHelper
                            .setupResponseListener(page, config.getHistoryApiEndpoint(), config);
                    log.info("Response listener ready, queue size: {}", responseQueue.size());

                    log.info("Clicking search button...");
                    PlaywrightHelper.clickSearchButton(page, config);
                    Thread.sleep(2000);

                    String apiResponse = PlaywrightHelper.waitForLastApiResponse(
                            page,
                            responseQueue,
                            config.getHistoryApiEndpoint(),
                            config,
                            5000,
                            1);

                    if (apiResponse.trim().startsWith("<")) {
                        String preview = apiResponse.length() > 500 ? apiResponse.substring(0, 500) : apiResponse;
                        throw new TefasWafBlockedException(preview);
                    }

                    log.debug("API response received, response length: {}", apiResponse.length());
                    return apiResponse;
                } finally {
                    ctx.close();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TefasClientException("TEFAS/fetchHistoryJson interrupted", e);
        } catch (com.microsoft.playwright.TimeoutError e) {
            throw new TefasTimeoutException("fetchHistoryJson", config.getElementWaitTimeoutMs(), e);
        } catch (TefasClientException e) {
            // Re-throw custom exceptions as-is
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error in fetchHistoryJson for fundCode={}, start={}, end={}", fundCode, start, end,
                    e);
            throw new TefasClientException("TEFAS/fetchHistoryJson çağrısı başarısız: " + e.getMessage(), e);
        }
    }

    /**
     * Extracts raw table data from DOM and transforms it to API response format.
     * JavaScript extracts raw text, Java transforms it via TableDataTransformer.
     */
    private String extractTableDataAsJson(Page page, String fundCode) {
        try {
            // Wait for filtered rows
            page.waitForSelector("tbody tr:not(.dataTables_empty)",
                    new Page.WaitForSelectorOptions().setTimeout(config.getElementWaitTimeoutMs()));

            // Wait for DataTables to fully render and apply filter
            Thread.sleep(config.getTableDataExtractionWaitMs());

            // Extract raw data (simple JavaScript - just get text content)
            String rawJson = (String) page.evaluate("""
                    (function() {
                        const rows = document.querySelectorAll('tbody tr');
                        const data = [];

                        rows.forEach(row => {
                            if (row.classList.contains('dataTables_empty')) {
                                return; // Skip empty rows
                            }

                            const cells = row.querySelectorAll('td');
                            if (cells.length >= 7) {
                                data.push({
                                    tarih: cells[0].textContent.trim(),
                                    fonKodu: cells[1].textContent.trim(),
                                    fonUnvan: cells[2].textContent.trim(),
                                    fiyat: cells[3].textContent.trim(),
                                    paySayisi: cells[4].textContent.trim(),
                                    kisiSayisi: cells[5].textContent.trim(),
                                    toplamDeger: cells[6].textContent.trim()
                                });
                            }
                        });

                        return JSON.stringify({ data: data });
                    })();
                    """);

            log.debug("Raw extracted JSON length: {}", rawJson.length());

            // Transform using helper class
            return TableDataTransformer.transformToApiFormat(rawJson, fundCode);

        } catch (Exception e) {
            throw new TefasClientException("Failed to extract table data: " + e.getMessage(), e);
        }
    }
}
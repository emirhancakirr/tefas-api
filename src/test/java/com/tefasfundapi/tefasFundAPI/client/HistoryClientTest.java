package com.tefasfundapi.tefasFundAPI.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HistoryClient Integration Test
 * 
 * NOT: Bu testler gerçek TEFAS API'sine istek atar ve Playwright browser
 * başlatır.
 * Bu yüzden yavaş çalışabilir ve internet bağlantısı gerektirir.
 * 
 * Sadece manuel test için kullanılmalı veya CI/CD'de ayrı bir test suite olarak
 * çalıştırılmalı.
 * 
 * Testleri çalıştırmak için @Disabled annotation'ını kaldırın.
 */
@DisplayName("HistoryClient Integration Tests")
class HistoryClientTest {

    private HistoryClient historyClient;

    @BeforeEach
    void setUp() {
        historyClient = new HistoryClient();
    }

    @Test
    @DisplayName("fetchHistoryJson - Geçerli parametrelerle başarılı çağrı")
    void testFetchHistoryJson_ValidParameters_Success() {
        // Given: Geçerli fon kodu ve tarih aralığı
        String fundCode = "AAK";
        LocalDate start = LocalDate.of(2024, 1, 1);
        LocalDate end = LocalDate.of(2024, 1, 31);

        // When
        String result = historyClient.fetchHistoryJson(fundCode, start, end);

        // Then
        assertNotNull(result, "Response should not be null");
        assertFalse(result.trim().isEmpty(), "Response should not be empty");
        // JSON formatında olmalı
        String trimmed = result.trim();
        assertTrue(trimmed.startsWith("{") || trimmed.startsWith("["),
                "Response should be JSON format. Got: " + trimmed.substring(0, Math.min(100, trimmed.length())));
    }

    @Test
    @DisplayName("fetchHistoryJson - Geçersiz fon kodu ile exception fırlatır")
    void testFetchHistoryJson_InvalidFundCode_ThrowsException() {
        // Given: Geçersiz fon kodu
        String invalidFundCode = "INVALID_CODE_XYZ_12345";
        LocalDate start = LocalDate.of(2024, 1, 1);
        LocalDate end = LocalDate.of(2024, 1, 31);

        // When/Then: Exception fırlatmalı
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            historyClient.fetchHistoryJson(invalidFundCode, start, end);
        });

        assertNotNull(exception.getMessage());
        assertTrue(exception.getMessage().contains("TEFAS") ||
                exception.getMessage().contains("başarısız") ||
                exception.getMessage().contains("WAF"),
                "Exception message should contain TEFAS-related information");
    }

    @Test
    @DisplayName("fetchHistoryJson - Başlangıç tarihi bitiş tarihinden sonra")
    void testFetchHistoryJson_StartAfterEnd_ThrowsException() {
        // Given: Başlangıç tarihi bitiş tarihinden sonra
        String fundCode = "AAK";
        LocalDate start = LocalDate.of(2024, 1, 31);
        LocalDate end = LocalDate.of(2024, 1, 1); // start > end

        // When/Then: Exception fırlatabilir veya hata dönebilir
        // TEFAS API'si bu durumu nasıl handle ediyor bilinmiyor, test edilmeli
        assertThrows(Exception.class, () -> {
            historyClient.fetchHistoryJson(fundCode, start, end);
        });
    }

    @Test
    @DisplayName("fetchHistoryJson - Tek günlük tarih aralığı")
    void testFetchHistoryJson_SingleDayRange_Success() {
        // Given: Tek günlük tarih aralığı
        String fundCode = "AAK";
        LocalDate date = LocalDate.of(2024, 1, 15);
        LocalDate start = date;
        LocalDate end = date; // Aynı gün

        // When
        String result = historyClient.fetchHistoryJson(fundCode, start, end);

        // Then
        assertNotNull(result, "Response should not be null for single day range");
        // Tek gün için de veri dönebilir (iş günü ise)
    }

    @Test
    @DisplayName("fetchHistoryJson - Uzun tarih aralığı (1 yıl)")
    void testFetchHistoryJson_LongDateRange_Success() {
        // Given: Uzun tarih aralığı (1 yıl)
        String fundCode = "AAK";
        LocalDate start = LocalDate.of(2023, 1, 1);
        LocalDate end = LocalDate.of(2023, 12, 31);

        // When
        String result = historyClient.fetchHistoryJson(fundCode, start, end);

        // Then
        assertNotNull(result, "Response should not be null for long date range");
        assertFalse(result.trim().isEmpty(), "Response should not be empty");
    }

    @Test
    @DisplayName("fetchHistoryJson - Null fon kodu ile exception fırlatır")
    void testFetchHistoryJson_NullFundCode_ThrowsException() {
        // Given: Null fon kodu
        LocalDate start = LocalDate.of(2024, 1, 1);
        LocalDate end = LocalDate.of(2024, 1, 31);

        // When/Then
        assertThrows(Exception.class, () -> {
            historyClient.fetchHistoryJson(null, start, end);
        });
    }

    @Test
    @DisplayName("fetchHistoryJson - Boş fon kodu ile exception fırlatır")
    void testFetchHistoryJson_EmptyFundCode_ThrowsException() {
        // Given: Boş fon kodu
        String emptyCode = "";
        LocalDate start = LocalDate.of(2024, 1, 1);
        LocalDate end = LocalDate.of(2024, 1, 31);

        // When/Then
        assertThrows(Exception.class, () -> {
            historyClient.fetchHistoryJson(emptyCode, start, end);
        });
    }

    @Test
    @DisplayName("fetchHistoryJson - Response JSON formatında olmalı")
    void testFetchHistoryJson_ResponseIsJsonFormat() {
        // Given: Geçerli parametreler
        String fundCode = "AAK";
        LocalDate start = LocalDate.of(2024, 1, 1);
        LocalDate end = LocalDate.of(2024, 1, 10);

        // When
        String result = historyClient.fetchHistoryJson(fundCode, start, end);

        // Then: JSON formatında olmalı
        assertNotNull(result, "Response should not be null");
        String trimmed = result.trim();
        assertTrue(trimmed.startsWith("{") || trimmed.startsWith("["),
                "Response should be JSON format. Got: " + trimmed.substring(0, Math.min(100, trimmed.length())));

        // HTML olmamalı (WAF engeli)
        assertFalse(trimmed.startsWith("<"),
                "Response should not be HTML (WAF block). Got: "
                        + trimmed.substring(0, Math.min(100, trimmed.length())));
    }

    @Test
    @DisplayName("fetchHistoryJson - Farklı fon kodları ile test")
    void testFetchHistoryJson_DifferentFundCodes() {
        // Given: Farklı fon kodları test et
        LocalDate start = LocalDate.of(2024, 1, 1);
        LocalDate end = LocalDate.of(2024, 1, 31);

        String[] fundCodes = { "AAK", "AOY" }; // Geçerli fon kodları

        // When/Then: Her fon kodu için test et
        for (String code : fundCodes) {
            try {
                String result = historyClient.fetchHistoryJson(code, start, end);
                assertNotNull(result, "Response should not be null for fund code: " + code);
                assertFalse(result.trim().isEmpty(), "Response should not be empty for fund code: " + code);

                // JSON formatında olmalı
                String trimmed = result.trim();
                assertTrue(trimmed.startsWith("{") || trimmed.startsWith("["),
                        "Response should be JSON for fund code: " + code);
            } catch (Exception e) {
                // Bazı fon kodları için veri olmayabilir, bu normal
                System.out.println("Fund code " + code + " returned error: " + e.getMessage());
                // Bu durumda test başarısız olmamalı, sadece log'la
            }
        }
    }

    @Test
    @DisplayName("fetchHistoryJson - Geçmiş tarih aralığı")
    void testFetchHistoryJson_PastDateRange() {
        // Given: Geçmiş tarih aralığı
        String fundCode = "AAK";
        LocalDate start = LocalDate.of(2023, 6, 1);
        LocalDate end = LocalDate.of(2023, 6, 30);

        // When
        String result = historyClient.fetchHistoryJson(fundCode, start, end);
        System.out.println(result);

        // Then
        assertNotNull(result, "Response should not be null for past date range");
        assertFalse(result.trim().isEmpty(), "Response should not be empty");
    }

    @Test
    @DisplayName("fetchHistoryJson - Gelecek tarih aralığı")
    void testFetchHistoryJson_FutureDateRange() {
        // Given: Gelecek tarih aralığı (veri olmayabilir)
        String fundCode = "AAK";
        LocalDate start = LocalDate.now().plusDays(1);
        LocalDate end = LocalDate.now().plusDays(10);

        // When/Then: Exception fırlatabilir veya boş response dönebilir
        // Bu durum test edilmeli ama başarısız olmamalı
        try {
            String result = historyClient.fetchHistoryJson(fundCode, start, end);
            // Eğer response dönerse, boş olabilir
            assertNotNull(result, "Response should not be null even for future dates");
        } catch (Exception e) {
            // Exception beklenebilir, bu normal
            assertNotNull(e.getMessage());
        }
    }
}

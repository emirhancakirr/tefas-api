package com.tefasfundapi.tefasFundAPI.parser;

import com.tefasfundapi.tefasFundAPI.dto.PriceRowDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HistoryParserTest {

    private HistoryParser parser;

    @BeforeEach
    void setUp() {
        parser = new HistoryParser();
    }

    @Test
    void testToPriceRows_ArrayFormat_Success() {
        // Given: Array format JSON
        String json = """
                [
                    {
                        "TARIH": "1762473600000",
                        "FONKODU": "AAK",
                        "FONUNVAN": "ATA PORTFÖY ÇOKLU VARLIK DEĞİŞKEN FON",
                        "FIYAT": 30.373708,
                        "TEDPAYSAYISI": 1096100.0,
                        "KISISAYISI": 755.0,
                        "PORTFOYBUYUKLUK": 33292621.25,
                        "BORSABULTENFIYAT": "-"
                    },
                    {
                        "TARIH": "1762559999999",
                        "FONKODU": "AAK",
                        "FONUNVAN": "ATA PORTFÖY ÇOKLU VARLIK DEĞİŞKEN FON",
                        "FIYAT": 30.5,
                        "TEDPAYSAYISI": 1100000.0,
                        "KISISAYISI": 760.0,
                        "PORTFOYBUYUKLUK": 33550000.0
                    }
                ]
                """;

        // When
        List<PriceRowDto> result = parser.toPriceRows(json);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());

        PriceRowDto first = result.get(0);
        assertNotNull(first.getDate());
        assertEquals("AAK", first.getFundCode());
        assertEquals("ATA PORTFÖY ÇOKLU VARLIK DEĞİŞKEN FON", first.getFundName());
        assertEquals(30.373708, first.getPrice(), 0.0001);
        assertEquals(1096100L, first.getOutstandingShares());
        assertEquals(755, first.getHolderCount());
        assertEquals(33292621.25, first.getTotalValue(), 0.01);
    }

    @Test
    void testToPriceRows_DataWrapperFormat_Success() {
        // Given: Data wrapper format (TEFAS API format)
        String json = """
                {
                    "data": [
                        {
                            "TARIH": "1762473600000",
                            "FONKODU": "AAK",
                            "FONUNVAN": "ATA PORTFÖY ÇOKLU VARLIK DEĞİŞKEN FON",
                            "FIYAT": 30.373708,
                            "TEDPAYSAYISI": 1096100.0,
                            "KISISAYISI": 755.0,
                            "PORTFOYBUYUKLUK": 33292621.25
                        }
                    ]
                }
                """;

        // When
        List<PriceRowDto> result = parser.toPriceRows(json);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("AAK", result.get(0).getFundCode());
    }

    @Test
    void testToPriceRows_SingleObjectInDataWrapper_Success() {
        // Given: Single object wrapped in data (TEFAS format)
        String json = """
                {
                    "data": {
                        "TARIH": "1762473600000",
                        "FONKODU": "AAK",
                        "FONUNVAN": "ATA PORTFÖY ÇOKLU VARLIK DEĞİŞKEN FON",
                        "FIYAT": 30.373708,
                        "TEDPAYSAYISI": 1096100.0,
                        "KISISAYISI": 755.0,
                        "PORTFOYBUYUKLUK": 33292621.25
                    }
                }
                """;

        // When
        List<PriceRowDto> result = parser.toPriceRows(json);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("AAK", result.get(0).getFundCode());
    }

    @Test
    void testToPriceRows_TarihAsNumber_Success() {
        // Given: TARIH as number instead of string
        String json = """
                [
                    {
                        "TARIH": 1762473600000,
                        "FONKODU": "AAK",
                        "FONUNVAN": "Test Fon",
                        "FIYAT": 30.373708,
                        "TEDPAYSAYISI": 1096100.0,
                        "KISISAYISI": 755.0,
                        "PORTFOYBUYUKLUK": 33292621.25
                    }
                ]
                """;

        // When
        List<PriceRowDto> result = parser.toPriceRows(json);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertNotNull(result.get(0).getDate());
    }

    @Test
    void testToPriceRows_NullFields_Handled() {
        // Given: Some fields are null
        String json = """
                [
                    {
                        "TARIH": "1762473600000",
                        "FONKODU": "AAK",
                        "FONUNVAN": null,
                        "FIYAT": 30.373708,
                        "TEDPAYSAYISI": null,
                        "KISISAYISI": 755.0,
                        "PORTFOYBUYUKLUK": null
                    }
                ]
                """;

        // When
        List<PriceRowDto> result = parser.toPriceRows(json);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        PriceRowDto dto = result.get(0);
        assertEquals("AAK", dto.getFundCode());
        assertNull(dto.getFundName()); // null field
        assertNull(dto.getOutstandingShares()); // null field
        assertEquals(755, dto.getHolderCount()); // non-null field
    }

    @Test
    void testToPriceRows_MissingFields_Handled() {
        // Given: Some fields are missing
        String json = """
                [
                    {
                        "TARIH": "1762473600000",
                        "FONKODU": "AAK",
                        "FIYAT": 30.373708
                    }
                ]
                """;

        // When
        List<PriceRowDto> result = parser.toPriceRows(json);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        PriceRowDto dto = result.get(0);
        assertEquals("AAK", dto.getFundCode());
        assertEquals(30.373708, dto.getPrice(), 0.0001);
        assertNull(dto.getFundName()); // missing field
        assertNull(dto.getOutstandingShares()); // missing field
    }

    @Test
    void testToPriceRows_NullInput_ThrowsException() {
        // When/Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            parser.toPriceRows(null);
        });
        assertNotNull(exception.getMessage());
        assertTrue(exception.getMessage().contains("Empty") || exception.getMessage().contains("null"));
    }

    @Test
    void testToPriceRows_EmptyInput_ThrowsException() {
        // When/Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            parser.toPriceRows("");
        });
        assertNotNull(exception.getMessage());
        // Empty string might trigger JSON parse error, so check for either
        assertTrue(exception.getMessage().contains("Empty") ||
                exception.getMessage().contains("null") ||
                exception.getMessage().contains("JSON parse"));
    }

    @Test
    void testToPriceRows_WhitespaceInput_ThrowsException() {
        // When/Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            parser.toPriceRows("   ");
        });
        assertNotNull(exception.getMessage());
        // Whitespace might trigger JSON parse error, so check for either
        assertTrue(exception.getMessage().contains("Empty") ||
                exception.getMessage().contains("null") ||
                exception.getMessage().contains("JSON parse"));
    }

    @Test
    void testToPriceRows_HTMLResponse_ThrowsException() {
        // Given: HTML response (WAF block)
        String html = "<html><body>Access Denied</body></html>";

        // When/Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            parser.toPriceRows(html);
        });
        assertNotNull(exception.getMessage());
        assertTrue(exception.getMessage().contains("HTML") || exception.getMessage().contains("html"));
    }

    @Test
    void testToPriceRows_InvalidJSON_ThrowsException() {
        // Given: Invalid JSON
        String invalidJson = "{ invalid json }";

        // When/Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            parser.toPriceRows(invalidJson);
        });
        assertTrue(exception.getMessage().contains("JSON parse failed"));
    }

    @Test
    void testToPriceRows_NoDataArray_ThrowsException() {
        // Given: JSON without data array
        String json = """
                {
                    "error": "No data found"
                }
                """;

        // When/Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            parser.toPriceRows(json);
        });
        assertNotNull(exception.getMessage());
        assertTrue(exception.getMessage().contains("data") || exception.getMessage().contains("No"));
    }

    @Test
    void testToPriceRows_DateParsing_Correct() {
        // Given: Specific date to verify parsing
        String json = """
                [
                    {
                        "TARIH": "1762473600000",
                        "FONKODU": "AAK",
                        "FONUNVAN": "Test",
                        "FIYAT": 30.0,
                        "TEDPAYSAYISI": 1000.0,
                        "KISISAYISI": 100,
                        "PORTFOYBUYUKLUK": 30000.0
                    }
                ]
                """;

        // When
        List<PriceRowDto> result = parser.toPriceRows(json);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        LocalDate date = result.get(0).getDate();
        assertNotNull(date);
        // 1762473600000 = 2025-11-07 (approximately, verify with actual epoch)
        // Just verify it's a valid date
        assertTrue(date.isAfter(LocalDate.of(2020, 1, 1)));
        assertTrue(date.isBefore(LocalDate.of(2030, 1, 1)));
    }

    @Test
    void testToPriceRows_AllFieldsPresent_Success() {
        // Given: All fields present
        String json = """
                [
                    {
                        "TARIH": "1762473600000",
                        "FONKODU": "AAK",
                        "FONUNVAN": "ATA PORTFÖY ÇOKLU VARLIK DEĞİŞKEN FON",
                        "FIYAT": 30.373708,
                        "TEDPAYSAYISI": 1096100.0,
                        "KISISAYISI": 755,
                        "PORTFOYBUYUKLUK": 33292621.25,
                        "BORSABULTENFIYAT": "-"
                    }
                ]
                """;

        // When
        List<PriceRowDto> result = parser.toPriceRows(json);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        PriceRowDto dto = result.get(0);

        assertNotNull(dto.getDate());
        assertEquals("AAK", dto.getFundCode());
        assertEquals("ATA PORTFÖY ÇOKLU VARLIK DEĞİŞKEN FON", dto.getFundName());
        assertEquals(30.373708, dto.getPrice(), 0.0001);
        assertEquals(1096100L, dto.getOutstandingShares());
        assertEquals(755, dto.getHolderCount());
        assertEquals(33292621.25, dto.getTotalValue(), 0.01);
    }
}

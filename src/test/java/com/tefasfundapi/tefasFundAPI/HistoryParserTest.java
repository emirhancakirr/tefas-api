package com.tefasfundapi.tefasFundAPI;

import org.junit.jupiter.api.Test;

import com.tefasfundapi.tefasFundAPI.dto.PriceRowDto;
import com.tefasfundapi.tefasFundAPI.parser.HistoryParser;

import org.junit.jupiter.api.BeforeEach;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class HistoryParserTest {

    private HistoryParser historyParser;

    @BeforeEach
    void setUp() {
        historyParser = new HistoryParser();
    }

    @Test
    void testToPriceRows() {
        String json = "[{" +
                "\"TARIH\":\"1704067200000\"," +
                "\"FONKODU\":\"AAK\"," +
                "\"FONUNVAN\":\"Test Fon\"," +
                "\"FIYAT\":30.5," +
                "\"TEDPAYSAYISI\":1000," +
                "\"KISISAYISI\":50," +
                "\"PORTFOYBUYUKLUK\":30500.0" +
                "}]";

        List<PriceRowDto> result = historyParser.toPriceRows(json);

        assertNotNull(result);
        assertEquals(1, result.size());

        PriceRowDto dto = result.get(0);
        assertEquals("AAK", dto.getFundCode());
        assertEquals("Test Fon", dto.getFundName());
        assertEquals(LocalDate.of(2024, 1, 1), dto.getDate());
        assertEquals(30.5, dto.getPrice());
        assertEquals(1000, dto.getOutstandingShares());
        assertEquals(50, dto.getHolderCount());
        assertEquals(30500.0, dto.getTotalValue());

    }

}

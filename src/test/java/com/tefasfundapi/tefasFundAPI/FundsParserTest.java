package com.tefasfundapi.tefasFundAPI;

import com.tefasfundapi.tefasFundAPI.dto.FundDto;
import com.tefasfundapi.tefasFundAPI.parser.FundsParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FundsParserTest {

    private FundsParser parser;

    @BeforeEach
    void setUp() {
        parser = new FundsParser();
    }

    @Test
    void testToFunds_ValidJson_ReturnsList() {
        String json = "[{" +
                "\"FONKODU\":\"AAK\"," +
                "\"FONUNVAN\":\"Test Fon\"," +
                "\"FONTURACIKLAMA\":\"Hisse Senedi\"," +
                "\"GETIRI1A\":2.5," +
                "\"GETIRI3A\":15.0" +
                "}]";

        List<FundDto> result = parser.toFunds(json);

        assertNotNull(result);
        assertEquals(1, result.size());
        FundDto dto = result.get(0);
        assertEquals("AAK", dto.getFundCode());
        assertEquals("Test Fon", dto.getFundName());
        assertEquals(2.5, dto.getGetiri1A());
    }

    @Test
    void testToFunds_EmptyJson_ThrowsException() {
        assertThrows(RuntimeException.class, () -> {
            parser.toFunds("");
        });
    }

    @Test
    void testToFunds_HtmlResponse_ThrowsException() {
        String html = "<html><body>Error</body></html>";
        assertThrows(RuntimeException.class, () -> {
            parser.toFunds(html);
        });
    }
}
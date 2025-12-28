package com.tefasfundapi.tefasFundAPI.service;

import com.tefasfundapi.tefasFundAPI.client.HistoryClient;
import com.tefasfundapi.tefasFundAPI.dto.PagedResponse;
import com.tefasfundapi.tefasFundAPI.dto.PriceRowDto;
import com.tefasfundapi.tefasFundAPI.parser.HistoryParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TefasServiceImplTest {

    @Mock
    private HistoryClient historyClient;

    @Mock
    private HistoryParser historyParser;

    private TefasServiceImpl tefasService;

    @BeforeEach
    void setUp() {
        tefasService = new TefasServiceImpl(null, null, historyClient, historyParser);
    }

    @Test
    void getFundNav_Success_ReturnsPagedResponse() {
        // Given
        String fundCode = "AAK";
        LocalDate start = LocalDate.of(2025, 1, 1);
        LocalDate end = LocalDate.of(2025, 1, 31);
        Pageable pageable = PageRequest.of(0, 10);

        String rawJson = "{ \"data\": [] }";
        List<PriceRowDto> priceRows = createSamplePriceRows(25);

        when(historyClient.fetchHistoryJson(eq(fundCode), eq(start), eq(end))).thenReturn(rawJson);
        when(historyParser.toPriceRows(eq(rawJson))).thenReturn(priceRows);

        // When
        Optional<PagedResponse<PriceRowDto>> result = tefasService.getFundNav(fundCode, start, end, pageable);

        // Then
        assertTrue(result.isPresent());
        PagedResponse<PriceRowDto> response = result.get();
        
        assertNotNull(response.getData());
        assertEquals(10, response.getData().size()); // page size = 10
        
        PagedResponse.Meta meta = response.getMeta();
        assertNotNull(meta);
        assertEquals(0, meta.getPage());
        assertEquals(10, meta.getSize());
        assertEquals(25, meta.getTotalElements());
        assertEquals(3, meta.getTotalPages()); // ceil(25/10) = 3

        verify(historyClient, times(1)).fetchHistoryJson(fundCode, start, end);
        verify(historyParser, times(1)).toPriceRows(rawJson);
    }

    @Test
    void getFundNav_SecondPage_ReturnsCorrectSlice() {
        // Given
        String fundCode = "AAK";
        LocalDate start = LocalDate.of(2025, 1, 1);
        LocalDate end = LocalDate.of(2025, 1, 31);
        Pageable pageable = PageRequest.of(1, 10); // Second page

        String rawJson = "{ \"data\": [] }";
        List<PriceRowDto> priceRows = createSamplePriceRows(25);

        when(historyClient.fetchHistoryJson(eq(fundCode), eq(start), eq(end))).thenReturn(rawJson);
        when(historyParser.toPriceRows(eq(rawJson))).thenReturn(priceRows);

        // When
        Optional<PagedResponse<PriceRowDto>> result = tefasService.getFundNav(fundCode, start, end, pageable);

        // Then
        assertTrue(result.isPresent());
        PagedResponse<PriceRowDto> response = result.get();
        
        assertEquals(10, response.getData().size()); // Second page, still 10 items
        assertEquals(1, response.getMeta().getPage());
        assertEquals(25, response.getMeta().getTotalElements());
    }

    @Test
    void getFundNav_LastPartialPage_ReturnsRemainingItems() {
        // Given
        String fundCode = "AAK";
        LocalDate start = LocalDate.of(2025, 1, 1);
        LocalDate end = LocalDate.of(2025, 1, 31);
        Pageable pageable = PageRequest.of(2, 10); // Third page

        String rawJson = "{ \"data\": [] }";
        List<PriceRowDto> priceRows = createSamplePriceRows(25); // 25 items, last page has 5

        when(historyClient.fetchHistoryJson(eq(fundCode), eq(start), eq(end))).thenReturn(rawJson);
        when(historyParser.toPriceRows(eq(rawJson))).thenReturn(priceRows);

        // When
        Optional<PagedResponse<PriceRowDto>> result = tefasService.getFundNav(fundCode, start, end, pageable);

        // Then
        assertTrue(result.isPresent());
        PagedResponse<PriceRowDto> response = result.get();
        
        assertEquals(5, response.getData().size()); // Last page has 5 items (25 % 10)
        assertEquals(2, response.getMeta().getPage());
        assertEquals(25, response.getMeta().getTotalElements());
        assertEquals(3, response.getMeta().getTotalPages());
    }

    @Test
    void getFundNav_EmptyResult_ReturnsEmptyPage() {
        // Given
        String fundCode = "AAK";
        LocalDate start = LocalDate.of(2025, 1, 1);
        LocalDate end = LocalDate.of(2025, 1, 31);
        Pageable pageable = PageRequest.of(0, 10);

        String rawJson = "{ \"data\": [] }";
        List<PriceRowDto> emptyList = new ArrayList<>();

        when(historyClient.fetchHistoryJson(eq(fundCode), eq(start), eq(end))).thenReturn(rawJson);
        when(historyParser.toPriceRows(eq(rawJson))).thenReturn(emptyList);

        // When
        Optional<PagedResponse<PriceRowDto>> result = tefasService.getFundNav(fundCode, start, end, pageable);

        // Then
        assertTrue(result.isPresent());
        PagedResponse<PriceRowDto> response = result.get();
        
        assertTrue(response.getData().isEmpty());
        assertEquals(0, response.getMeta().getTotalElements());
        assertEquals(0, response.getMeta().getTotalPages());
    }

    @Test
    void getFundNav_NullCode_ReturnsEmpty() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        LocalDate start = LocalDate.of(2025, 1, 1);
        LocalDate end = LocalDate.of(2025, 1, 31);

        // When
        Optional<PagedResponse<PriceRowDto>> result = tefasService.getFundNav(null, start, end, pageable);

        // Then
        assertFalse(result.isPresent());
        verify(historyClient, never()).fetchHistoryJson(any(), any(), any());
        verify(historyParser, never()).toPriceRows(any());
    }

    @Test
    void getFundNav_BlankCode_ReturnsEmpty() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        LocalDate start = LocalDate.of(2025, 1, 1);
        LocalDate end = LocalDate.of(2025, 1, 31);

        // When
        Optional<PagedResponse<PriceRowDto>> result = tefasService.getFundNav("  ", start, end, pageable);

        // Then
        assertFalse(result.isPresent());
        verify(historyClient, never()).fetchHistoryJson(any(), any(), any());
        verify(historyParser, never()).toPriceRows(any());
    }

    @Test
    void getFundNav_CodeWithWhitespace_TrimsCode() {
        // Given
        String fundCode = "  AAK  ";
        LocalDate start = LocalDate.of(2025, 1, 1);
        LocalDate end = LocalDate.of(2025, 1, 31);
        Pageable pageable = PageRequest.of(0, 10);

        String rawJson = "{ \"data\": [] }";
        List<PriceRowDto> priceRows = createSamplePriceRows(5);

        when(historyClient.fetchHistoryJson(eq("AAK"), eq(start), eq(end))).thenReturn(rawJson);
        when(historyParser.toPriceRows(eq(rawJson))).thenReturn(priceRows);

        // When
        Optional<PagedResponse<PriceRowDto>> result = tefasService.getFundNav(fundCode, start, end, pageable);

        // Then
        assertTrue(result.isPresent());
        verify(historyClient, times(1)).fetchHistoryJson("AAK", start, end); // Trimmed
    }

    @Test
    void getFundNav_SingleItem_ReturnsCorrectly() {
        // Given
        String fundCode = "AAK";
        LocalDate start = LocalDate.of(2025, 1, 1);
        LocalDate end = LocalDate.of(2025, 1, 1);
        Pageable pageable = PageRequest.of(0, 10);

        String rawJson = "{ \"data\": [] }";
        List<PriceRowDto> priceRows = createSamplePriceRows(1);

        when(historyClient.fetchHistoryJson(eq(fundCode), eq(start), eq(end))).thenReturn(rawJson);
        when(historyParser.toPriceRows(eq(rawJson))).thenReturn(priceRows);

        // When
        Optional<PagedResponse<PriceRowDto>> result = tefasService.getFundNav(fundCode, start, end, pageable);

        // Then
        assertTrue(result.isPresent());
        PagedResponse<PriceRowDto> response = result.get();
        
        assertEquals(1, response.getData().size());
        assertEquals(1, response.getMeta().getTotalElements());
        assertEquals(1, response.getMeta().getTotalPages());
    }

    @Test
    void getFundNav_PageSizeLargerThanTotal_ReturnsAllItems() {
        // Given
        String fundCode = "AAK";
        LocalDate start = LocalDate.of(2025, 1, 1);
        LocalDate end = LocalDate.of(2025, 1, 31);
        Pageable pageable = PageRequest.of(0, 100); // Large page size

        String rawJson = "{ \"data\": [] }";
        List<PriceRowDto> priceRows = createSamplePriceRows(25);

        when(historyClient.fetchHistoryJson(eq(fundCode), eq(start), eq(end))).thenReturn(rawJson);
        when(historyParser.toPriceRows(eq(rawJson))).thenReturn(priceRows);

        // When
        Optional<PagedResponse<PriceRowDto>> result = tefasService.getFundNav(fundCode, start, end, pageable);

        // Then
        assertTrue(result.isPresent());
        PagedResponse<PriceRowDto> response = result.get();
        
        assertEquals(25, response.getData().size()); // All items fit in one page
        assertEquals(1, response.getMeta().getTotalPages());
    }

    @Test
    void getFundNav_PageBeyondRange_ReturnsEmptyData() {
        // Given
        String fundCode = "AAK";
        LocalDate start = LocalDate.of(2025, 1, 1);
        LocalDate end = LocalDate.of(2025, 1, 31);
        Pageable pageable = PageRequest.of(10, 10); // Page 10, but only 25 items (3 pages)

        String rawJson = "{ \"data\": [] }";
        List<PriceRowDto> priceRows = createSamplePriceRows(25);

        when(historyClient.fetchHistoryJson(eq(fundCode), eq(start), eq(end))).thenReturn(rawJson);
        when(historyParser.toPriceRows(eq(rawJson))).thenReturn(priceRows);

        // When
        Optional<PagedResponse<PriceRowDto>> result = tefasService.getFundNav(fundCode, start, end, pageable);

        // Then
        assertTrue(result.isPresent());
        PagedResponse<PriceRowDto> response = result.get();
        
        assertTrue(response.getData().isEmpty()); // Beyond available pages
        assertEquals(25, response.getMeta().getTotalElements());
        assertEquals(3, response.getMeta().getTotalPages());
    }

    /**
     * Helper method to create sample PriceRowDto objects for testing
     */
    private List<PriceRowDto> createSamplePriceRows(int count) {
        List<PriceRowDto> rows = new ArrayList<>();
        LocalDate baseDate = LocalDate.of(2025, 1, 1);
        
        for (int i = 0; i < count; i++) {
            PriceRowDto dto = new PriceRowDto();
            dto.setDate(baseDate.plusDays(i));
            dto.setFundCode("AAK");
            dto.setFundName("Test Fund");
            dto.setPrice(30.0 + i * 0.1);
            dto.setOutstandingShares(1000000L + i * 100);
            dto.setHolderCount(500 + i);
            dto.setTotalValue(30000000.0 + i * 1000);
            rows.add(dto);
        }
        
        return rows;
    }
}

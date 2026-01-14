
package com.tefasfundapi.tefasFundAPI.service;

import com.tefasfundapi.tefasFundAPI.client.FundsClient;
import com.tefasfundapi.tefasFundAPI.client.HistoryClient;
import com.tefasfundapi.tefasFundAPI.dto.FundDto;
import com.tefasfundapi.tefasFundAPI.dto.FundPerformanceDto;
import com.tefasfundapi.tefasFundAPI.dto.FundReturnQuery;
import com.tefasfundapi.tefasFundAPI.dto.PagedResponse;
import com.tefasfundapi.tefasFundAPI.dto.PriceRowDto;
import com.tefasfundapi.tefasFundAPI.parser.FundsParser;
import com.tefasfundapi.tefasFundAPI.parser.HistoryParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TefasServiceImplTest {

    @Mock
    private FundsClient fundsClient;

    @Mock
    private FundsParser fundsParser;

    @Mock
    private HistoryClient historyClient;

    @Mock
    private HistoryParser historyParser;

    @InjectMocks
    private TefasServiceImpl tefasService;

    private FundDto testFund;
    private PriceRowDto testPriceRow;
    private FundPerformanceDto testPerformance;

    @BeforeEach
    void setUp() {
        // Test verilerini hazırla
        testFund = new FundDto();
        testFund.setFundCode("AAK");
        testFund.setFundName("Test Fon");

        testPriceRow = new PriceRowDto();
        testPriceRow.setFundCode("AAK");
        testPriceRow.setDate(LocalDate.of(2024, 1, 1));
        testPriceRow.setPrice(30.5);

        testPerformance = new FundPerformanceDto();
        testPerformance.setFundCode("AAK");
        testPerformance.setFundName("Test Fon");
        testPerformance.setGetiri(15.5);
    }

    @Test
    void testGetFund_Success_ReturnsFund() {
        String fundCode = "AAK";
        String rawJson = "[{\"FONKODU\":\"AAK\",\"FONUNVAN\":\"Test Fon\"}]";
        List<FundDto> parsedFunds = Arrays.asList(testFund);

        when(fundsClient.fetchComparisonFundReturns(any(FundReturnQuery.class)))
                .thenReturn(rawJson);
        when(fundsParser.toFunds(rawJson)).thenReturn(parsedFunds);

        Optional<FundDto> result = tefasService.getFund(fundCode, null);

        assertTrue(result.isPresent());
        assertEquals("AAK", result.get().getFundCode());
        verify(fundsClient).fetchComparisonFundReturns(any(FundReturnQuery.class));
        verify(fundsParser).toFunds(rawJson);
    }

    @Test
    void testGetFund_EmptyCode_ReturnsEmpty() {
        Optional<FundDto> result = tefasService.getFund("", null);

        assertFalse(result.isPresent());
        verify(fundsClient, never()).fetchComparisonFundReturns(any());
    }

    @Test
    void testGetFund_NullCode_ReturnsEmpty() {
        Optional<FundDto> result = tefasService.getFund(null, null);

        assertFalse(result.isPresent());
        verify(fundsClient, never()).fetchComparisonFundReturns(any());
    }

    @Test
    void testGetFund_NotFound_ReturnsEmpty() {
        String fundCode = "INVALID";
        String rawJson = "[{\"FONKODU\":\"OTHER\"}]";
        FundDto otherFund = new FundDto();
        otherFund.setFundCode("OTHER");
        List<FundDto> parsedFunds = Arrays.asList(otherFund);

        when(fundsClient.fetchComparisonFundReturns(any(FundReturnQuery.class)))
                .thenReturn(rawJson);
        when(fundsParser.toFunds(rawJson)).thenReturn(parsedFunds);

        Optional<FundDto> result = tefasService.getFund(fundCode, null);

        assertFalse(result.isPresent()); // INVALID kod bulunamadığı için empty olmalı
    }

    @Test
    void testGetFund_QueryParametersSetCorrectly() {
        String fundCode = "AAK";
        String rawJson = "[{\"FONKODU\":\"AAK\",\"FONUNVAN\":\"Test Fon\"}]";
        when(fundsClient.fetchComparisonFundReturns(any(FundReturnQuery.class))).thenReturn(rawJson);
        when(fundsParser.toFunds(rawJson)).thenReturn(Arrays.asList(testFund));

        tefasService.getFund(fundCode, null);

        ArgumentCaptor<FundReturnQuery> queryCaptor = ArgumentCaptor.forClass(FundReturnQuery.class);
        verify(fundsClient).fetchComparisonFundReturns(queryCaptor.capture());

        FundReturnQuery capturedQuery = queryCaptor.getValue();
        assertEquals("AAK", capturedQuery.getFonturkod());
        assertEquals("", capturedQuery.getCalismatipi());
    }

    @Test
    void testGetFundNav_Success_ReturnsPagedResponse() {
        // Given
        String fundCode = "AAK";
        LocalDate start = LocalDate.of(2024, 1, 1);
        LocalDate end = LocalDate.of(2024, 1, 31);
        Pageable pageable = PageRequest.of(0, 20);
        String rawJson = "[{\"fundCode\":\"AAK\"}]";
        List<PriceRowDto> parsedRows = Arrays.asList(testPriceRow);

        when(historyClient.fetchHistoryJson(eq(fundCode), eq(start), eq(end)))
                .thenReturn(rawJson);
        when(historyParser.toPriceRows(rawJson)).thenReturn(parsedRows);

        Optional<PagedResponse<PriceRowDto>> result = tefasService.getFundNav(fundCode, start, end, pageable);

        assertTrue(result.isPresent());
        assertEquals(1, result.get().getData().size());
        assertEquals(0, result.get().getMeta().getPage());
        assertEquals(20, result.get().getMeta().getSize());
        assertEquals(1, result.get().getMeta().getTotalElements());
    }

    @Test
    void testGetFundNav_Pagination_SecondPage() {
        String fundCode = "AAK";
        LocalDate start = LocalDate.of(2024, 1, 1);
        LocalDate end = LocalDate.of(2024, 1, 31);
        Pageable pageable = PageRequest.of(1, 10); // Page 1, size 10

        PriceRowDto row1 = new PriceRowDto();
        row1.setFundCode("AAK");
        PriceRowDto row2 = new PriceRowDto();
        row2.setFundCode("AAK");
        PriceRowDto row3 = new PriceRowDto();
        row3.setFundCode("AAK");
        List<PriceRowDto> parsedRows = Arrays.asList(row1, row2, row3);

        when(historyClient.fetchHistoryJson(eq(fundCode), eq(start), eq(end)))
                .thenReturn("[]");
        when(historyParser.toPriceRows(any())).thenReturn(parsedRows);

        Optional<PagedResponse<PriceRowDto>> result = tefasService.getFundNav(fundCode, start, end, pageable);

        assertTrue(result.isPresent());
        assertEquals(1, result.get().getMeta().getPage());
        assertEquals(3, result.get().getMeta().getTotalElements());
    }

    @Test
    void testFilterByFundCode_CaseInsensitive() {
        String fundCode = "aak";
        String rawJson = "[{\"FONKODU\":\"AAK\"}]";
        when(fundsClient.fetchComparisonFundReturns(any())).thenReturn(rawJson);
        when(fundsParser.toFunds(rawJson)).thenReturn(Arrays.asList(testFund));

        Optional<FundDto> result = tefasService.getFund(fundCode, null);

        assertTrue(result.isPresent());
        assertEquals("AAK", result.get().getFundCode());
    }
}
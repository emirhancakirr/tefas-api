package com.tefasfundapi.tefasFundAPI.service;

import com.tefasfundapi.tefasFundAPI.client.FundsClient;
import com.tefasfundapi.tefasFundAPI.dto.FundDto;
import com.tefasfundapi.tefasFundAPI.dto.FundPerformanceDto;
import com.tefasfundapi.tefasFundAPI.dto.FundReturnQuery;
import com.tefasfundapi.tefasFundAPI.dto.PagedResponse;
import com.tefasfundapi.tefasFundAPI.dto.PriceRowDto;
import com.tefasfundapi.tefasFundAPI.parser.FundsParser;
import com.tefasfundapi.tefasFundAPI.client.HistoryClient;
import com.tefasfundapi.tefasFundAPI.parser.HistoryParser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Controller -> Service -> Client -> Parser -> DTO akışını koordine eder.
 * - Upstream çağrıları client'lara delegedir.
 * - JSON/HTML'den DTO'ya dönüşüm parser'larda yapılır.
 * - Servis katmanı; sıralama, sayfalama ve "fields" filtrelemesini üstlenir.
 */
@Service
public class TefasServiceImpl implements TefasService {

    private static final Logger log = LoggerFactory.getLogger(TefasServiceImpl.class);

    private final FundsClient fundsClient;
    private final FundsParser fundsParser;
    private final HistoryClient historyClient;
    private final HistoryParser historyParser;

    private record PaginationInfo(int startIndex, int endIndex, int totalElements, int totalPages) {
    }

    public TefasServiceImpl(FundsClient fundsClient, FundsParser fundsParser, HistoryClient historyClient,
            HistoryParser historyParser) {
        this.fundsClient = fundsClient;
        this.fundsParser = fundsParser;
        this.historyClient = historyClient;
        this.historyParser = historyParser;
    }

    /* ----------------------------- FUNDS ------------------------------ */

    @Override
    public Optional<FundDto> getFund(String code, List<String> fields) {
        log.info("getFund called with code={} and fields={}", code, fields);
        if (code == null || code.isBlank())
            return Optional.empty();

        FundReturnQuery query = new FundReturnQuery();
        query.setFonturkod(code.trim());
        query.setCalismatipi("");
        query.setFontip("");
        query.setBastarih("");
        query.setBittarih("");
        query.setStrperiod("");
        query.setIslemdurum("");

        String raw = fundsClient.fetchComparisonFundReturns(query);
        List<FundDto> list = fundsParser.toFunds(raw);

        List<FundDto> filtered = filterByFundCode(list, code);

        return filtered.stream().findFirst();
    }

    @Override
    public Optional<PagedResponse<PriceRowDto>> getFundNav(String code, LocalDate start, LocalDate end,
            Pageable pageable) {
        log.info("TefasServiceImpl: getFundNav called with code=" + code + " and start=" + start + " and end="
                + end + " and pageable=" + pageable);
        if (code == null || code.isBlank())
            return Optional.empty();

        String raw = historyClient.fetchHistoryJson(code.trim(), start, end);
        List<PriceRowDto> list = historyParser.toPriceRows(raw);

        PaginationInfo pagination = calculatePaginationInfo(pageable, list);

        List<PriceRowDto> pagedList;

        if (pagination.startIndex() >= pagination.totalElements) {
            pagedList = List.of();
        } else {
            pagedList = list.subList(pagination.startIndex(), pagination.endIndex());
        }

        PagedResponse.Meta meta = new PagedResponse.Meta(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                pagination.totalElements(),
                pagination.totalPages());

        return Optional.of(new PagedResponse<>(pagedList, meta));
    }

    @Override
    public Optional<PagedResponse<FundPerformanceDto>> getFundPerformance(String code, LocalDate start, LocalDate end,
            Pageable pageable) {
        log.debug("TefasServiceImpl: getFundPerformance called with code=" + code + "and start=" + start + " and end="
                + end + " and pageable=" + pageable);
        if (code == null || code.isBlank())
            return Optional.empty();

        String raw = fundsClient.fetchFundPerformance(start, end);
        List<FundPerformanceDto> list = fundsParser.toPerformanceDtos(raw);
        List<FundPerformanceDto> filteredList = filterByFundCode(list, code);

        if (filteredList.isEmpty()) {
            return Optional.empty();
        }

        PaginationInfo pagination = calculatePaginationInfo(pageable, filteredList);

        List<FundPerformanceDto> pagedList;

        if (pagination.startIndex() >= pagination.totalElements) {
            pagedList = List.of();
        } else {
            pagedList = filteredList.subList(pagination.startIndex(), pagination.endIndex());
        }

        PagedResponse.Meta meta = new PagedResponse.Meta(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                pagination.totalElements(),
                pagination.totalPages());

        return Optional.of(new PagedResponse<>(pagedList, meta));
    }

    private <T> PaginationInfo calculatePaginationInfo(Pageable pageable, List<T> list) {
        int page = pageable.getPageNumber();
        int size = pageable.getPageSize();
        int totalElements = list.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);

        int startIndex = page * size;
        int endIndex = Math.min(startIndex + size, totalElements);

        return new PaginationInfo(startIndex, endIndex, totalElements, totalPages);
    }

    private <T> List<T> filterByFundCode(List<T> list, String code) {
        if (code == null || code.isBlank() || list == null || list.isEmpty()) {
            return List.of();
        }
        String trimmedCode = code.trim();
        return list.stream()
                .filter(item -> {
                    if (item instanceof FundDto dto) {
                        return dto.getFundCode() != null && dto.getFundCode().equalsIgnoreCase(trimmedCode);
                    } else if (item instanceof FundPerformanceDto dto) {
                        return dto.getFundCode() != null && dto.getFundCode().equalsIgnoreCase(trimmedCode);
                    }
                    return false;
                })
                .toList();
    }
}

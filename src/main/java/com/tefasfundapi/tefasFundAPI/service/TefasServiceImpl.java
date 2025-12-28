package com.tefasfundapi.tefasFundAPI.service;

import com.tefasfundapi.tefasFundAPI.client.FundsClient;
import com.tefasfundapi.tefasFundAPI.dto.FundDto;
import com.tefasfundapi.tefasFundAPI.dto.FundReturnQuery;
import com.tefasfundapi.tefasFundAPI.dto.PagedResponse;
import com.tefasfundapi.tefasFundAPI.dto.PriceRowDto;
import com.tefasfundapi.tefasFundAPI.parser.FundsParser;
import com.tefasfundapi.tefasFundAPI.client.HistoryClient;
import com.tefasfundapi.tefasFundAPI.parser.HistoryParser;
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

    private final FundsClient fundsClient;
    private final FundsParser fundsParser;
    private final HistoryClient historyClient;
    private final HistoryParser historyParser;

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
        System.out.println("TefasServiceImpl: getFund called with code=" + code + " and fields=" + fields);
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

        Optional<FundDto> match = list.stream()
                .filter(f -> f.getFundCode() != null && f.getFundCode().equalsIgnoreCase(code.trim()))
                .findFirst();

        return match;
    }

    @Override
    public Optional<PagedResponse<PriceRowDto>> getFundNav(String code, LocalDate start, LocalDate end,
            Pageable pageable) {
        System.out.println("TefasServiceImpl: getFundNav called with code=" + code + " and start=" + start + " and end="
                + end + " and pageable=" + pageable);
        if (code == null || code.isBlank())
            return Optional.empty();

        String raw = historyClient.fetchHistoryJson(code.trim(), start, end);
        List<PriceRowDto> list = historyParser.toPriceRows(raw);

        int page = pageable.getPageNumber();
        int size = pageable.getPageSize();
        int totalElements = list.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);

        int startIndex = page * size;
        int endIndex = Math.min(startIndex + size, totalElements);
        List<PriceRowDto> pagedList;

        if (startIndex >= totalElements) {
            pagedList = List.of();
        } else {
            pagedList = list.subList(startIndex, endIndex);
        }

        PagedResponse.Meta meta = new PagedResponse.Meta(page, size, totalElements, totalPages);

        return Optional.of(new PagedResponse<>(pagedList, meta));
    }

}
package com.tefasfundapi.tefasFundAPI.service;

import com.tefasfundapi.tefasFundAPI.client.FundsClient;
import com.tefasfundapi.tefasFundAPI.dto.FundDto;
import com.tefasfundapi.tefasFundAPI.dto.FundReturnQuery;
import com.tefasfundapi.tefasFundAPI.parser.FundsParser;
import org.springframework.stereotype.Service;

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

    public TefasServiceImpl(FundsClient fundsClient, FundsParser fundsParser) {
        this.fundsClient = fundsClient;
        this.fundsParser = fundsParser;
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

        // burada DTO olarak bırakıyoruz.
        return match;
    }

}
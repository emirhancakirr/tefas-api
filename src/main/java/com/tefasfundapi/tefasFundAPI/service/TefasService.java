package com.tefasfundapi.tefasFundAPI.service;

import com.tefasfundapi.tefasFundAPI.dto.FundDto;
import com.tefasfundapi.tefasFundAPI.dto.FundPerformanceDto;
import com.tefasfundapi.tefasFundAPI.dto.PriceRowDto;
import com.tefasfundapi.tefasFundAPI.dto.PagedResponse;
import java.time.LocalDate;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface TefasService {
    Optional<FundDto> getFund(String code, List<String> fields);

    Optional<PagedResponse<PriceRowDto>> getFundNav(String code, LocalDate start, LocalDate end, Pageable pageable);

    // Optional<PagedResponse<FundPerformanceDto>> getFundPerformance(String code,
    // LocalDate start, LocalDate end,
    // Pageable pageable);
}
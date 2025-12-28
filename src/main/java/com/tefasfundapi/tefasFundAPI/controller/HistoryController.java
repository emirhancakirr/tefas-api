package com.tefasfundapi.tefasFundAPI.controller;

import com.tefasfundapi.tefasFundAPI.exception.FundNotFoundException;
import com.tefasfundapi.tefasFundAPI.exception.InvalidDateRangeException;
import com.tefasfundapi.tefasFundAPI.service.TefasService;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/v1/funds")
@Validated
public class HistoryController {

    private final TefasService tefasService;

    public HistoryController(TefasService tefasService) {
        this.tefasService = tefasService;
    }

    /**
     * Tek fonun tarihsel fiyat/NAV akışı
     * GET /v1/funds/{code}/nav?start=YYYY-MM-DD&end=YYYY-MM-DD&page=0&size=20
     */
    @GetMapping("/{code}/nav")
    public ResponseEntity<?> getNav(
            @PathVariable @NotBlank(message = "Fund code cannot be blank") String code,
            @RequestParam @NotNull(message = "Start date is required") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @NotNull(message = "End date is required") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @PageableDefault(size = 20) Pageable pageable) {

        if (start.isAfter(end)) {
            throw new InvalidDateRangeException("start date must be <= end date");
        }

        return tefasService.getFundNav(code.trim(), start, end, pageable)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new FundNotFoundException(code));
    }
}
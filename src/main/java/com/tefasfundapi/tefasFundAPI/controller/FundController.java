package com.tefasfundapi.tefasFundAPI.controller;

import com.tefasfundapi.tefasFundAPI.dto.FundDto;
import com.tefasfundapi.tefasFundAPI.exception.FundNotFoundException;
import com.tefasfundapi.tefasFundAPI.exception.InvalidDateRangeException;
import com.tefasfundapi.tefasFundAPI.filter.FieldFilter;
import com.tefasfundapi.tefasFundAPI.service.TefasService;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.List;
import java.time.LocalDate;

@RestController
@RequestMapping("/v1/funds")
@Validated
public class FundController {

    private final TefasService tefasService;

    public FundController(TefasService tefasService) {
        this.tefasService = tefasService;
    }

    // GET /v1/funds/{code}?fields=code,name,...
    @GetMapping("/{code}")
    public ResponseEntity<?> getFund(
            @PathVariable @NotBlank(message = "Fund code cannot be blank") String code,
            @RequestParam(required = false, name = "fields") String fieldsCsv) {
        List<String> fields = FieldFilter.parse(fieldsCsv);
        FundDto fund = tefasService.getFund(code.trim(), fields)
                .orElseThrow(() -> new FundNotFoundException(code));
        return ResponseEntity.ok(FieldFilter.apply(fund, fields));
    }

    // FundController.java
    // @GetMapping("/{code}/performance")
    // public ResponseEntity<?> getPerformance(
    //         @PathVariable @NotBlank(message = "Fund code cannot be blank") String code,
    //         @RequestParam @NotNull(message = "Start date is required") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
    //         @RequestParam @NotNull(message = "End date is required") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {

    //     if (start.isAfter(end)) {
    //         throw new InvalidDateRangeException("start date must be <= end date");
    //     }

    //     return tefasService.getFundPerformance(code.trim(), start, end)
    //             .map(ResponseEntity::ok)
    //             .orElseThrow(() -> new FundNotFoundException(code));
    // }
}
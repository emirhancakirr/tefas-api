package com.tefasfundapi.tefasFundAPI.controller;

import com.tefasfundapi.tefasFundAPI.exception.FundNotFoundException;
import com.tefasfundapi.tefasFundAPI.exception.InvalidDateRangeException;
import com.tefasfundapi.tefasFundAPI.service.TefasService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/v1/funds")
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
            @PathVariable String code,
            @RequestParam String start,
            @RequestParam String end,
            @PageableDefault(size = 20) Pageable pageable) {
        LocalDate s = LocalDate.parse(start);
        LocalDate e = LocalDate.parse(end);
        if (s.isAfter(e)) {
            throw new InvalidDateRangeException("start date must be <= end date");
        }

        return tefasService.getFundNav(code.trim(), s, e, pageable)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new FundNotFoundException(code));
    }
}
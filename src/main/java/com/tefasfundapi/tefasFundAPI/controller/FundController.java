package com.tefasfundapi.tefasFundAPI.controller;

import com.tefasfundapi.tefasFundAPI.dto.FundDto;
import com.tefasfundapi.tefasFundAPI.exception.FundNotFoundException;
import com.tefasfundapi.tefasFundAPI.filter.FieldFilter;
import com.tefasfundapi.tefasFundAPI.service.TefasService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/funds")
public class FundController {

    private final TefasService tefasService;

    public FundController(TefasService tefasService) {
        this.tefasService = tefasService;
    }

    // GET /v1/funds/{code}?fields=code,name,...
    @GetMapping("/{code}")
    public ResponseEntity<?> getFund(@PathVariable String code,
            @RequestParam(required = false, name = "fields") String fieldsCsv) {
        List<String> fields = FieldFilter.parse(fieldsCsv);
        FundDto fund = tefasService.getFund(code.trim(), fields)
                .orElseThrow(() -> new FundNotFoundException(code));
        return ResponseEntity.ok(FieldFilter.apply(fund, fields));
    }
}
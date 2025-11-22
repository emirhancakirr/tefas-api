package com.tefasfundapi.tefasFundAPI.controller;

import com.tefasfundapi.tefasFundAPI.dto.FundDto;
import com.tefasfundapi.tefasFundAPI.service.TefasService;
import com.tefasfundapi.tefasFundAPI.filter.FieldFilter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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
        return tefasService.getFund(code.trim(), fields)
                .<ResponseEntity<?>>map(dto -> ResponseEntity.ok(FieldFilter.apply(dto, fields)))
                .orElseGet(() -> ResponseEntity.status(404).body(Map.of(
                        "error", "NOT_FOUND",
                        "message", "Fund not found: " + code
                )));
    }
}
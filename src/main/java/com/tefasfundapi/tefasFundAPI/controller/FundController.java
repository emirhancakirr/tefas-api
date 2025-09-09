package com.tefasfundapi.tefasFundAPI.controller;

import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/v1/funds")
public class FundController {

    // GET /v1/funds?query=...
    @GetMapping
    public Map<String, Object> listFunds(@RequestParam(required = false) String query) {
        // Şimdilik sabit örnek dönelim
        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> data = new ArrayList<>();

        Map<String, Object> fund = new HashMap<>();
        fund.put("fundCode", "ABC");
        fund.put("fundName", "ABC Hisse");
        fund.put("umbrellaType", "Hisse Senedi");
        data.add(fund);

        response.put("data", data);
        response.put("meta", Map.of(
                "page", 0,
                "size", 1,
                "totalElements", 1,
                "totalPages", 1
        ));

        return response;
    }

    // GET /v1/funds/{code}
    @GetMapping("/{code}")
    public Map<String, Object> getFund(@PathVariable String code) {
        // Şimdilik sabit örnek dönelim
        return Map.of(
                "fundCode", code,
                "fundName", "ABC Hisse",
                "umbrellaType", "Hisse Senedi",
                "issuer", "XYZ Portföy"
        );
    }
}
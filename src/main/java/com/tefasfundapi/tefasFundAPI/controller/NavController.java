package com.tefasfundapi.tefasFundAPI.controller;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/v1/funds/{code}/nav")
public class NavController {

    @GetMapping
    public Map<String, Object> getNav(
            @PathVariable String code,
            @RequestParam @NotNull
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @NotNull
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {

        // Şimdilik sabit örnek
        List<Map<String, Object>> data = new ArrayList<>();

        Map<String, Object> row = new HashMap<>();
        row.put("date", start.toString());
        row.put("fundCode", code);
        row.put("fundName", "ABC Hisse");
        row.put("price", 3.1425);
        row.put("outstandingShares", 123456789);
        row.put("totalValue", 387654321.12);
        row.put("holderCount", 15234);

        data.add(row);

        Map<String, Object> response = new HashMap<>();
        response.put("data", data);
        response.put("meta", Map.of(
                "page", 0,
                "size", 1,
                "totalElements", 1,
                "totalPages", 1
        ));

        return response;
    }
}

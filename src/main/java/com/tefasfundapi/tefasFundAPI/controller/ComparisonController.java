package com.tefasfundapi.tefasFundAPI.controller;

import org.springframework.web.bind.annotation.*;
import jakarta.validation.constraints.NotNull;

import java.util.*;

@RestController
@RequestMapping("/v1/comparisons")
public class ComparisonController {

    // GET /v1/comparisons/periods?periods=m1,m3,ytd&codes=ABC,DEF
    @GetMapping("/periods")
    public List<Map<String, Object>> getPeriodComparisons(
            @RequestParam(required = false) String periods,
            @RequestParam(required = false) String codes) {

        // Şimdilik sabit bir örnek
        Map<String, Object> row = new HashMap<>();
        row.put("fundCode", "ABC");
        row.put("fundName", "ABC Hisse");
        row.put("umbrellaType", "Hisse Senedi");
        row.put("m1", 0.021);
        row.put("m3", 0.084);
        row.put("m6", 0.151);
        row.put("ytd", 0.192);
        row.put("y1", 0.241);
        row.put("y3", 0.701);
        row.put("y5", 1.230);

        return List.of(row);
    }

    // GET /v1/comparisons/changes?start=2025-09-01&end=2025-09-05&codes=ABC,DEF
    @GetMapping("/changes")
    public List<Map<String, Object>> getChanges(
            @RequestParam @NotNull String start,
            @RequestParam @NotNull String end,
            @RequestParam(required = false) String codes) {

        // Şimdilik sabit bir örnek
        Map<String, Object> row = new HashMap<>();
        row.put("fundCode", "XYZ");
        row.put("fundName", "XYZ Katılım");
        row.put("umbrellaType", "Katılım");
        row.put("change", 0.057);

        return List.of(row);
    }
}
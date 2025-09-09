package com.tefasfundapi.tefasFundAPI.controller;

import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/v1/meta")
public class MetaController {

    // GET /v1/meta/last-sync
    @GetMapping("/last-sync")
    public Map<String, Object> getLastSync() {
        Map<String, Object> resources = new HashMap<>();

        // Örnek: fiyat listesi için
        Map<String, Object> pricesMeta = new HashMap<>();
        pricesMeta.put("lastChecked", "2025-09-07T20:01:33+03:00");
        pricesMeta.put("status", "OK");
        pricesMeta.put("lastEtag", "W/\"abc123\"");

        // Örnek: comparisons/periods için
        Map<String, Object> comparisonsMeta = new HashMap<>();
        comparisonsMeta.put("lastChecked", "2025-09-07T20:00:15+03:00");
        comparisonsMeta.put("status", "OK");
        comparisonsMeta.put("lastEtag", null);

        resources.put("prices:2025-09-01:2025-09-05", pricesMeta);
        resources.put("comparisons:periods:default", comparisonsMeta);

        return Map.of("resources", resources);
    }
}
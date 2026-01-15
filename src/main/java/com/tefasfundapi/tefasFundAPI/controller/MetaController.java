package com.tefasfundapi.tefasFundAPI.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/v1/meta")
@Tag(name = "Meta", description = "API metadata ve cache bilgilerini getiren endpoint'ler")
public class MetaController {

    @Operation(
            summary = "Son senkronizasyon bilgileri",
            description = "API'nin son veri senkronizasyon bilgilerini getirir. " +
                    "Her kaynak için son kontrol zamanı, durum ve ETag bilgilerini içerir."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Metadata başarıyla getirildi",
                    content = @Content(mediaType = "application/json")
            )
    })
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
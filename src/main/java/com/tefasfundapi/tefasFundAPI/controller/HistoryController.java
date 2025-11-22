package com.tefasfundapi.tefasFundAPI.controller;

import com.tefasfundapi.tefasFundAPI.client.HistoryClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/v1/funds")
public class HistoryController {

    private final HistoryClient historyClient;

    public HistoryController(HistoryClient historyClient) {
        this.historyClient = historyClient;
    }

    /**
     * Tek fonun tarihsel fiyat/NAV akışı
     * GET /v1/funds/{code}/nav?start=YYYY-MM-DD&end=YYYY-MM-DD
     */
    @GetMapping("/{code}/nav")
    public ResponseEntity<String> getNav(
            @PathVariable String code,
            @RequestParam String start,
            @RequestParam String end
    ) {
        LocalDate s = LocalDate.parse(start);
        LocalDate e = LocalDate.parse(end);
        if (s.isAfter(e)) return ResponseEntity.badRequest().body("{\"error\":\"start must be <= end\"}");

        String json = historyClient.fetchHistoryJson(code.trim(), s, e);
        return ResponseEntity.ok(json);
    }
}
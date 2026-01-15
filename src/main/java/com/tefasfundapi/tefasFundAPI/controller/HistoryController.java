package com.tefasfundapi.tefasFundAPI.controller;

import com.tefasfundapi.tefasFundAPI.exception.FundNotFoundException;
import com.tefasfundapi.tefasFundAPI.exception.InvalidDateRangeException;
import com.tefasfundapi.tefasFundAPI.service.TefasService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/v1/funds")
@Validated
@Tag(name = "Fund History", description = "Fonların tarihsel NAV (Net Aktif Değer) verilerini getiren endpoint'ler")
public class HistoryController {

    private final TefasService tefasService;

    public HistoryController(TefasService tefasService) {
        this.tefasService = tefasService;
    }

    @Operation(summary = "Fon NAV geçmişi getir", description = "Fonun belirli bir tarih aralığındaki NAV (Net Aktif Değer) geçmişini getirir. "
            +
            "Her gün için fiyat, çıkışta dolaşan pay sayısı, toplam değer ve yatırımcı sayısı bilgilerini içerir. " +
            "Sayfalama desteği vardır (page, size parametreleri).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "NAV geçmişi başarıyla getirildi", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404", description = "Fon bulunamadı", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "400", description = "Geçersiz tarih aralığı veya parametre", content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/{code}/nav")
    public ResponseEntity<?> getNav(
            @Parameter(description = "Fon kodu", required = true, example = "AAK") @PathVariable @NotBlank(message = "Fund code cannot be blank") String code,
            @Parameter(description = "Başlangıç tarihi (YYYY-MM-DD formatında)", required = true, example = "2024-01-01") @RequestParam @NotNull(message = "Start date is required") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @Parameter(description = "Bitiş tarihi (YYYY-MM-DD formatında)", required = true, example = "2024-01-31") @RequestParam @NotNull(message = "End date is required") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {

        if (start.isAfter(end)) {
            throw new InvalidDateRangeException("start date must be <= end date");
        }

        return tefasService.getFundNav(code.trim(), start, end, pageable)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new FundNotFoundException(code));
    }
}
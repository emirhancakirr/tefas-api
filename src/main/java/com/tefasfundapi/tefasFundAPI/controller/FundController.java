package com.tefasfundapi.tefasFundAPI.controller;

import com.tefasfundapi.tefasFundAPI.dto.FundDto;
import com.tefasfundapi.tefasFundAPI.exception.FundNotFoundException;
import com.tefasfundapi.tefasFundAPI.exception.InvalidDateRangeException;
import com.tefasfundapi.tefasFundAPI.filter.FieldFilter;
import com.tefasfundapi.tefasFundAPI.service.TefasService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.List;
import java.time.LocalDate;

@RestController
@RequestMapping("/v1/funds")
@Validated
@Tag(name = "Funds", description = "Fon bilgileri ve performans verilerini getiren endpoint'ler")
public class FundController {

    private final TefasService tefasService;

    public FundController(TefasService tefasService) {
        this.tefasService = tefasService;
    }

    @Operation(summary = "Fon detayı getir", description = "Belirli bir fonun detaylı bilgilerini getirir. Fon kodu ile sorgulama yapılır. "
            +
            "İsteğe bağlı olarak sadece belirli alanları döndürmek için 'fields' parametresi kullanılabilir.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Fon bilgileri başarıyla getirildi", content = @Content(mediaType = "application/json", schema = @Schema(implementation = FundDto.class))),
            @ApiResponse(responseCode = "404", description = "Fon bulunamadı", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "400", description = "Geçersiz parametre", content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/{code}")
    public ResponseEntity<?> getFund(
            @Parameter(description = "Fon kodu (örn: AAK, AOY)", required = true, example = "AAK") @PathVariable @NotBlank(message = "Fund code cannot be blank") String code,
            @Parameter(description = "Döndürülecek alanlar (virgülle ayrılmış). Örn: fundCode,fundName,getiri1A", example = "fundCode,fundName,getiri1A") @RequestParam(required = false, name = "fields") String fieldsCsv) {
        List<String> fields = FieldFilter.parse(fieldsCsv);
        FundDto fund = tefasService.getFund(code.trim(), fields)
                .orElseThrow(() -> new FundNotFoundException(code));
        return ResponseEntity.ok(FieldFilter.apply(fund, fields));
    }

    @Operation(summary = "Fon performansı getir", description = "Belirli bir fonun seçilen tarih aralığındaki performans getirilerini getirir. "
            +
            "Tarih aralığına göre fon getirilerini (BindComparisonFundReturns) döndürür. " +
            "Sayfalama desteği vardır (page, size parametreleri).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Fon performans verileri başarıyla getirildi", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404", description = "Fon bulunamadı", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "400", description = "Geçersiz tarih aralığı veya parametre", content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/{code}/performance")
    public ResponseEntity<?> getPerformance(
            @Parameter(description = "Fon kodu", required = true, example = "AAK") @PathVariable @NotBlank(message = "Fund code cannot be blank") String code,
            @Parameter(description = "Başlangıç tarihi (YYYY-MM-DD formatında)", required = true, example = "2024-01-01") @RequestParam @NotNull(message = "Start date is required") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @Parameter(description = "Bitiş tarihi (YYYY-MM-DD formatında)", required = true, example = "2024-03-01") @RequestParam @NotNull(message = "End date is required") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {

        if (start.isAfter(end)) {
            throw new InvalidDateRangeException("start date must be <= end date");
        }

        return tefasService.getFundPerformance(code.trim(), start, end, pageable)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new FundNotFoundException(code));
    }
}
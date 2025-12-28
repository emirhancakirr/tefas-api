package com.tefasfundapi.tefasFundAPI.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tefasfundapi.tefasFundAPI.dto.FundDto;
import com.tefasfundapi.tefasFundAPI.dto.FundPerformanceDto;
import com.tefasfundapi.tefasFundAPI.exception.TefasParseException;
import com.tefasfundapi.tefasFundAPI.exception.TefasWafBlockedException;

import io.swagger.v3.core.util.Json;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
public class FundsParser {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Beklenen: JSON array ya da "data":[...] benzeri bir yapı.
     * Sniffer ile gördüğün alan adlarını burada eşle.
     */
    public List<FundDto> toFunds(String rawJson) {
        try {
            if (rawJson == null || rawJson.trim().isEmpty()) {
                throw new TefasParseException("Empty or null response from TEFAS API");
            }

            // HTML response kontrolü (WAF engeli veya hata sayfası)
            String trimmed = rawJson.trim();
            if (trimmed.startsWith("<") || trimmed.startsWith("<!DOCTYPE") || trimmed.startsWith("<html")) {
                String preview = trimmed.length() > 500 ? trimmed.substring(0, 500) : trimmed;
                throw new TefasWafBlockedException(preview);
            }

            // İlk 500 karakteri logla (debug için)
            String preview = rawJson.length() > 500 ? rawJson.substring(0, 500) + "..." : rawJson;
            System.out.println("FundsParser: Received response (first 500 chars): " + preview);

            JsonNode root = MAPPER.readTree(rawJson);
            JsonNode arr = root.isArray() ? root
                    : (root.has("data") ? root.get("data") : root);
            List<FundDto> out = new ArrayList<>();

            if (arr.isArray()) {
                for (JsonNode n : arr) {
                    out.add(mapOneFund(n));
                }
            } else if (arr.isObject()) {
                // tek kayıt dönmüş olabilir
                out.add(mapOneFund(arr));
            }
            return out;
        } catch (TefasParseException | TefasWafBlockedException e) {
            // Re-throw parse/WAF exceptions as-is
            throw e;
        } catch (Exception e) {
            String preview = rawJson != null && rawJson.length() > 200 ? rawJson.substring(0, 200) : rawJson;
            throw new TefasParseException("JSON parse failed. Response preview: " + preview, e);
        }
    }

    // FundsParser.java

    public List<FundPerformanceDto> toPerformanceDtos(String rawJson) {
        try {
            if (rawJson == null || rawJson.trim().isEmpty()) {
                throw new TefasParseException("Empty or null response from Tefas API");
            }

            // HTML response kontrolü (WAF engeli veya hata sayfası)
            String trimmed = rawJson.trim();
            if (trimmed.startsWith("<") || trimmed.startsWith("<!DOCTYPE") || trimmed.startsWith("<html")) {
                String preview = trimmed.length() > 500 ? trimmed.substring(0, 500) : trimmed;
                throw new TefasWafBlockedException(preview);
            }

            JsonNode root = MAPPER.readTree(rawJson);
            JsonNode arr = root.isArray() ? root
                    : (root.has("data") ? root.get("data") : null);

            if (arr == null) {
                throw new TefasParseException("No 'data' array found in response");
            }

            List<FundPerformanceDto> out = new ArrayList<>();
            if (arr.isArray()) {
                for (JsonNode n : arr) {
                    out.add(mapOnePerformance(n)); // ✅ mapOne yerine mapOnePerformance
                }
            } else if (arr.isObject()) {
                out.add(mapOnePerformance(arr)); // ✅ mapOne yerine mapOnePerformance
            }
            return out;

        } catch (TefasParseException | TefasWafBlockedException e) {
            // Re-throw parse/WAF exceptions as-is
            throw e;
        } catch (Exception e) {
            String preview = rawJson != null && rawJson.length() > 200 ? rawJson.substring(0, 200) : rawJson;
            throw new TefasParseException("JSON parse failed. Response preview: " + preview, e);
        }
    }

    /**
     * Maps a single JSON node to FundPerformanceDto.
     * Expected fields from table_fund_returns: fonKodu, fonAdi, semsiyeFonTuru,
     * getiri
     */
    private FundPerformanceDto mapOnePerformance(JsonNode n) {
        FundPerformanceDto dto = new FundPerformanceDto();

        // Table extraction'dan gelen field'lar (camelCase)
        dto.setFundCode(text(n, "fonKodu", "FONKODU", "FundCode", "fundCode"));
        dto.setFundName(text(n, "fonAdi", "FONADI", "FonAdi", "FundName", "fundName"));
        dto.setUmbrellaType(
                text(n, "semsiyeFonTuru", "SEMSIYEFONTURU", "SemsiyeFonTuru", "UmbrellaType", "umbrellaType"));

        // Getiri: String olarak geliyor (örn: "1,6770"), parse etmek gerekiyor
        String getiriStr = text(n, "getiri", "GETIRI", "Getiri", "getiri");
        if (getiriStr != null && !getiriStr.isEmpty()) {
            try {
                // Türkçe format: "1,6770" -> 1.6770
                String normalized = getiriStr.replace(",", ".");
                dto.setGetiri(Double.parseDouble(normalized));
            } catch (NumberFormatException e) {
                // Parse edilemezse null bırak
                dto.setGetiri(null);
            }
        }

        return dto;
    }

    private FundDto mapOneFund(JsonNode n) {
        FundDto dto = new FundDto();

        // Türkçe alan adları ile eşleştirme
        dto.setFundCode(text(n, "FONKODU", "FundCode", "fundCode", "Code"));
        dto.setFundName(text(n, "FONUNVAN", "FundName", "fundName", "Name"));
        dto.setUmbrellaType(text(n, "FONTURACIKLAMA", "UmbrellaType", "umbrellaType", "Type"));
        dto.setIssuer(text(n, "Issuer", "issuer"));

        String inception = text(n, "InceptionDate", "inceptionDate");
        if (inception != null && inception.length() >= 10) {
            dto.setInceptionDate(LocalDate.parse(inception.substring(0, 10)));
        }

        dto.setExpenseRatio(number(n, "ExpenseRatio", "expenseRatio"));

        // Getiri alanları (Türkçe alan adları)
        dto.setGetiri1A(number(n, "GETIRI1A", "getiri1A"));
        dto.setGetiri3A(number(n, "GETIRI3A", "getiri3A"));
        dto.setGetiri6A(number(n, "GETIRI6A", "getiri6A"));
        dto.setGetiri1Y(number(n, "GETIRI1Y", "getiri1Y"));
        dto.setGetiriYB(number(n, "GETIRIYB", "getiriYB"));
        dto.setGetiri3Y(number(n, "GETIRI3Y", "getiri3Y"));
        dto.setGetiri5Y(number(n, "GETIRI5Y", "getiri5Y"));

        return dto;
    }

    private String text(JsonNode n, String... keys) {
        for (String k : keys)
            if (n.has(k) && !n.get(k).isNull())
                return n.get(k).asText();
        return null;
    }

    private Double number(JsonNode n, String... keys) {
        for (String k : keys)
            if (n.has(k) && n.get(k).isNumber())
                return n.get(k).asDouble();
        return null;
    }
}
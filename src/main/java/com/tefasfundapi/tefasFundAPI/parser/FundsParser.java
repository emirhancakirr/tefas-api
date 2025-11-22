package com.tefasfundapi.tefasFundAPI.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tefasfundapi.tefasFundAPI.dto.FundDto;
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
                throw new RuntimeException("FundsParser: Empty or null response from TEFAS API");
            }

            // HTML response kontrolü (WAF engeli veya hata sayfası)
            String trimmed = rawJson.trim();
            if (trimmed.startsWith("<") || trimmed.startsWith("<!DOCTYPE") || trimmed.startsWith("<html")) {
                String errorMsg = "FundsParser: Received HTML response instead of JSON. " +
                        "This usually indicates a WAF block or server error. Response preview: " +
                        (trimmed.length() > 500 ? trimmed.substring(0, 500) : trimmed);
                throw new RuntimeException(errorMsg);
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
                    out.add(mapOne(n));
                }
            } else if (arr.isObject()) {
                // tek kayıt dönmüş olabilir
                out.add(mapOne(arr));
            }
            return out;
        } catch (Exception e) {
            String errorMsg = "FundsParser: JSON parse failed. Response preview: " +
                    (rawJson != null && rawJson.length() > 200 ? rawJson.substring(0, 200) : rawJson);
            throw new RuntimeException(errorMsg, e);
        }
    }

    private FundDto mapOne(JsonNode n) {
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
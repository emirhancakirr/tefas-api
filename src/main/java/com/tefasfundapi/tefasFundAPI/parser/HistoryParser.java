package com.tefasfundapi.tefasFundAPI.parser;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tefasfundapi.tefasFundAPI.dto.PriceRowDto;
import java.util.List;
import java.util.ArrayList;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDate;
import java.time.Instant;
import java.time.ZoneId;

/*
 Örnek JSON:
         {
            "TARIH": "1762473600000",
            "FONKODU": "AAK",
            "FONUNVAN": "ATA PORTFÖY ÇOKLU VARLIK DEĞİŞKEN FON",
            "FIYAT": 30.373708,
            "TEDPAYSAYISI": 1096100.0,
            "KISISAYISI": 755.0,
            "PORTFOYBUYUKLUK": 33292621.25,
            "BORSABULTENFIYAT": "-"
        },

*/
@Component
public class HistoryParser {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public List<PriceRowDto> toPriceRows(String rawJson) {
        try {
            if (rawJson == null || rawJson.trim().isEmpty()) {
                throw new RuntimeException("HistoryParser: Empty or null response from TEFAS API");
            }

            String trimmed = rawJson.trim();
            if (trimmed.startsWith("<") || trimmed.startsWith("<!DOCTYPE") || trimmed.startsWith("<html")) {
                throw new RuntimeException("HistoryParser: Received HTML response instead of JSON. " +
                        "This usually indicates a WAF block or server error. Response preview: " +
                        (trimmed.length() > 500 ? trimmed.substring(0, 500) : trimmed));
            }

            JsonNode root = MAPPER.readTree(rawJson);
            JsonNode arr = root.isArray() ? root
                    : (root.has("data") ? root.get("data") : null);

            if (arr == null) {
                throw new RuntimeException("HistoryParser: No 'data' array found in response");
            }

            List<PriceRowDto> out = new ArrayList<>();
            if (arr.isArray()) {
                for (JsonNode n : arr) {
                    out.add(mapOne(n));
                }
            } else if (arr.isObject()) {
                out.add(mapOne(arr));
            }
            return out;

        } catch (Exception e) {
            throw new RuntimeException("HistoryParser: JSON parse failed. Response preview: " +
                    (rawJson != null && rawJson.length() > 200 ? rawJson.substring(0, 200) : rawJson), e);
        }

    }

    private PriceRowDto mapOne(JsonNode n) {
        PriceRowDto dto = new PriceRowDto();

        // TARIH: Epoch milliseconds (string veya number olabilir) -> LocalDate
        if (n.has("TARIH") && !n.get("TARIH").isNull()) {
            long epochMillis;
            if (n.get("TARIH").isTextual()) {
                // String olarak geliyorsa parse et
                epochMillis = Long.parseLong(n.get("TARIH").asText());
            } else {
                // Number olarak geliyorsa direkt al
                epochMillis = n.get("TARIH").asLong();
            }
            // Epoch milliseconds -> LocalDate
            LocalDate date = Instant.ofEpochMilli(epochMillis)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
            dto.setDate(date);
        }

        // FONKODU
        if (n.has("FONKODU") && !n.get("FONKODU").isNull()) {
            dto.setFundCode(n.get("FONKODU").asText());
        }

        // FONUNVAN
        if (n.has("FONUNVAN") && !n.get("FONUNVAN").isNull()) {
            dto.setFundName(n.get("FONUNVAN").asText());
        }

        // FIYAT
        if (n.has("FIYAT") && !n.get("FIYAT").isNull() && n.get("FIYAT").isNumber()) {
            dto.setPrice(n.get("FIYAT").asDouble());
        }

        // TEDPAYSAYISI (Outstanding Shares)
        if (n.has("TEDPAYSAYISI") && !n.get("TEDPAYSAYISI").isNull() && n.get("TEDPAYSAYISI").isNumber()) {
            dto.setOutstandingShares(n.get("TEDPAYSAYISI").asLong());
        }

        // KISISAYISI (Holder Count)
        if (n.has("KISISAYISI") && !n.get("KISISAYISI").isNull() && n.get("KISISAYISI").isNumber()) {
            dto.setHolderCount(n.get("KISISAYISI").asInt());
        }

        // PORTFOYBUYUKLUK (Total Value) - Eksikti, eklendi
        if (n.has("PORTFOYBUYUKLUK") && !n.get("PORTFOYBUYUKLUK").isNull() && n.get("PORTFOYBUYUKLUK").isNumber()) {
            dto.setTotalValue(n.get("PORTFOYBUYUKLUK").asDouble());
        }

        return dto;
    }
}

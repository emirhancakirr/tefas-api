package com.tefasfundapi.tefasFundAPI.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Transforms raw table data extracted from DOM to API response format.
 * Handles date parsing, number parsing, and field name mapping.
 */
public final class TableDataTransformer {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private TableDataTransformer() {
        // Utility class - cannot be instantiated
    }

    /**
     * Transforms raw table data (from DOM) to API response format.
     * 
     * @param rawJson Raw JSON string extracted from table DOM
     * @param fundCode Fund code to filter by (null to include all)
     * @return Transformed JSON string matching API response format
     * @throws RuntimeException if transformation fails
     */
    public static String transformToApiFormat(String rawJson, String fundCode) {
        try {
            JsonNode root = MAPPER.readTree(rawJson);
            JsonNode rawData = root.get("data");

            if (rawData == null || !rawData.isArray()) {
                throw new RuntimeException("Invalid raw JSON structure: missing or invalid 'data' array");
            }

            List<Map<String, Object>> transformedData = new ArrayList<>();

            for (JsonNode row : rawData) {
                if (shouldSkipRow(row, fundCode)) {
                    continue;
                }

                Map<String, Object> transformedRow = buildTransformedRow(row);
                transformedData.add(transformedRow);
            }

            Map<String, Object> result = Map.of("data", transformedData);
            String resultJson = MAPPER.writeValueAsString(result);

            System.out.println("Transformed " + transformedData.size() + " rows to API format");
            return resultJson;

        } catch (Exception e) {
            throw new RuntimeException("Failed to transform raw table data: " + e.getMessage(), e);
        }
    }

    /**
     * Determines if a row should be skipped (header row, invalid data, or fund code mismatch).
     */
    private static boolean shouldSkipRow(JsonNode row, String fundCode) {
        String tarih = getTextValue(row, "tarih");
        String fonKodu = getTextValue(row, "fonKodu");

        // Skip header rows
        if (isHeaderRow(tarih, fonKodu)) {
            return true;
        }

        // Skip if fund code doesn't match
        if (fundCode != null && !fundCode.equals(fonKodu)) {
            return true;
        }

        // Skip if date is invalid
        return !isValidDate(tarih);
    }

    /**
     * Checks if a row is a header row.
     */
    private static boolean isHeaderRow(String tarih, String fonKodu) {
        return tarih.isEmpty() || tarih.equals("Fon") ||
                fonKodu.isEmpty() || fonKodu.equals("Fon Türü") ||
                fonKodu.contains("Seçiniz");
    }

    /**
     * Checks if a date string is valid.
     */
    private static boolean isValidDate(String dateStr) {
        return parseDateToEpoch(dateStr) > 0;
    }

    /**
     * Builds a transformed row in API response format.
     */
    private static Map<String, Object> buildTransformedRow(JsonNode row) {
        String tarih = getTextValue(row, "tarih");
        String fonKodu = getTextValue(row, "fonKodu");

        Map<String, Object> transformedRow = new LinkedHashMap<>();
        transformedRow.put("TARIH", String.valueOf(parseDateToEpoch(tarih)));
        transformedRow.put("FONKODU", fonKodu);
        transformedRow.put("FONUNVAN", getTextValue(row, "fonUnvan"));
        transformedRow.put("FIYAT", parseNumber(getTextValue(row, "fiyat")));
        transformedRow.put("TEDPAYSAYISI", parseNumber(getTextValue(row, "paySayisi")));
        transformedRow.put("KISISAYISI", parseInteger(getTextValue(row, "kisiSayisi")));
        transformedRow.put("PORTFOYBUYUKLUK", parseNumber(getTextValue(row, "toplamDeger")));

        return transformedRow;
    }

    /**
     * Gets text value from JsonNode with default empty string.
     */
    private static String getTextValue(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        return field != null ? field.asText("") : "";
    }

    /**
     * Parses date string "dd.MM.yyyy" to epoch milliseconds.
     * Returns 0 if parsing fails.
     */
    private static long parseDateToEpoch(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return 0;
        }

        try {
            LocalDate date = LocalDate.parse(dateStr.trim(), DATE_FORMATTER);
            return date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Parses Turkish number format (e.g., "1.234,56") to Double.
     * Returns null if parsing fails.
     */
    private static Double parseNumber(String numStr) {
        if (numStr == null || numStr.trim().isEmpty()) {
            return null;
        }

        try {
            String cleaned = numStr.trim().replace(".", "").replace(",", ".");
            return Double.parseDouble(cleaned);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Parses Turkish number format to Integer (no decimals).
     * Returns null if parsing fails.
     */
    private static Integer parseInteger(String numStr) {
        if (numStr == null || numStr.trim().isEmpty()) {
            return null;
        }

        try {
            String cleaned = numStr.trim().replace(".", "").replace(",", "");
            return Integer.parseInt(cleaned);
        } catch (Exception e) {
            return null;
        }
    }
}


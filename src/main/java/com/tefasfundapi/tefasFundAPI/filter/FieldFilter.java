package com.tefasfundapi.tefasFundAPI.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import java.util.stream.Collectors;

/** Basit alan seçici: fields=null/boş ise nesneyi olduğu gibi döndürür. */
public class FieldFilter {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static List<String> parse(String fieldsCsv) {
        if (fieldsCsv == null || fieldsCsv.isBlank()) return List.of();
        return Arrays.stream(fieldsCsv.split(","))
                .map(String::trim).filter(s -> !s.isBlank())
                .distinct()
                .collect(Collectors.toList());
    }

    /** DTO -> Map ve sadece istenen alanları bırakır. Alan yoksa DTO'yu döner. */
    @SuppressWarnings("unchecked")
    public static Object apply(Object dto, List<String> fields) {
        if (dto == null || fields == null || fields.isEmpty()) return dto;
        Map<String, Object> asMap = MAPPER.convertValue(dto, Map.class);
        Map<String, Object> filtered = new LinkedHashMap<>();
        for (String f : fields) {
            if (asMap.containsKey(f)) filtered.put(f, asMap.get(f));
        }
        return filtered.isEmpty() ? dto : filtered;
    }
}
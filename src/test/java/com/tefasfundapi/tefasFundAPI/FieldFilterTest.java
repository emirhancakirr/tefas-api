package com.tefasfundapi.tefasFundAPI;

import com.tefasfundapi.tefasFundAPI.dto.FundDto;
import com.tefasfundapi.tefasFundAPI.filter.FieldFilter;  
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FieldFilterTest {

    @Test
    void testParse_Null_ReturnsEmptyList() {
        List<String> result = FieldFilter.parse(null);
        assertTrue(result.isEmpty());
    }

    @Test
    void testParse_EmptyString_ReturnsEmptyList() {
        List<String> result = FieldFilter.parse("");
        assertTrue(result.isEmpty());
    }

    @Test
    void testParse_ValidCsv_ReturnsList() {
        List<String> result = FieldFilter.parse("code,name,price");
        assertEquals(3, result.size());
        assertEquals("code", result.get(0));
        assertEquals("name", result.get(1));
        assertEquals("price", result.get(2));
    }

    @Test
    void testParse_WithSpaces_TrimsSpaces() {
        List<String> result = FieldFilter.parse(" code , name , price ");
        assertEquals(3, result.size());
        assertEquals("code", result.get(0));
    }

    @Test
    void testParse_Duplicates_RemovesDuplicates() {
        List<String> result = FieldFilter.parse("code,code,name");
        assertEquals(2, result.size());
    }

    @Test
    void testApply_NullDto_ReturnsNull() {
        Object result = FieldFilter.apply(null, List.of("code"));
        assertNull(result);
    }

    @Test
    void testApply_EmptyFields_ReturnsDto() {
        FundDto dto = new FundDto();
        dto.setFundCode("AAK");
        
        Object result = FieldFilter.apply(dto, List.of());
        assertSame(dto, result);
    }

    @Test
    void testApply_ValidFields_ReturnsFilteredMap() {
        FundDto dto = new FundDto();
        dto.setFundCode("AAK");
        dto.setFundName("Test Fon");
        dto.setGetiri1A(2.5);

        Object result = FieldFilter.apply(dto, List.of("fundCode", "fundName"));
        
        assertTrue(result instanceof Map);
        Map<String, Object> map = (Map<String, Object>) result;
        assertEquals(2, map.size());
        assertTrue(map.containsKey("fundCode"));
        assertTrue(map.containsKey("fundName"));
        assertFalse(map.containsKey("getiri1A"));
    }
}
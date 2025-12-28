package com.tefasfundapi.tefasFundAPI;

import com.tefasfundapi.tefasFundAPI.dto.PagedResponse;
import com.tefasfundapi.tefasFundAPI.dto.PriceRowDto;
import com.tefasfundapi.tefasFundAPI.service.TefasService;
import com.tefasfundapi.tefasFundAPI.controller.HistoryController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HistoryController.class)
class HistoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TefasService tefasService;

    @Test
    void testGetNav_Success_Returns200() throws Exception {
        PriceRowDto dto = new PriceRowDto();
        dto.setFundCode("AAK");
        dto.setPrice(30.5);
        dto.setDate(LocalDate.of(2024, 1, 1));

        PagedResponse<PriceRowDto> response = new PagedResponse<>(
                Arrays.asList(dto),
                new PagedResponse.Meta(0, 20, 1, 1));

        when(tefasService.getFundNav(eq("AAK"), any(), any(), any()))
                .thenReturn(Optional.of(response));

        mockMvc.perform(get("/v1/funds/AAK/nav")
                .param("start", "2024-01-01")
                .param("end", "2024-01-31"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.data[0].fundCode").value("AAK"))
                .andExpect(jsonPath("$.meta.page").value(0));
    }

    @Test
    void testGetNav_InvalidDateRange_Returns400() throws Exception {
        mockMvc.perform(get("/v1/funds/AAK/nav")
                .param("start", "2024-01-31")
                .param("end", "2024-01-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }

    @Test
    void testGetNav_NotFound_Returns404() throws Exception {
        when(tefasService.getFundNav(any(), any(), any(), any()))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/v1/funds/INVALID/nav")
                .param("start", "2024-01-01")
                .param("end", "2024-01-31"))
                .andExpect(status().isNotFound());
    }
}
package com.tefasfundapi.tefasFundAPI;

import com.tefasfundapi.tefasFundAPI.dto.FundDto;
import com.tefasfundapi.tefasFundAPI.service.TefasService;
import com.tefasfundapi.tefasFundAPI.controller.FundController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FundController.class)
class FundControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TefasService tefasService;

    @Test
    void testGetFund_Success_Returns200() throws Exception {
        FundDto dto = new FundDto();
        dto.setFundCode("AAK");
        dto.setFundName("Test Fon");

        when(tefasService.getFund(eq("AAK"), any())).thenReturn(Optional.of(dto));

        mockMvc.perform(get("/v1/funds/AAK"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.fundCode").value("AAK"));
    }

    @Test
    void testGetFund_NotFound_Returns404() throws Exception {
        when(tefasService.getFund(eq("INVALID"), any())).thenReturn(Optional.empty());

        mockMvc.perform(get("/v1/funds/INVALID"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void testGetFund_WithFields_ReturnsFiltered() throws Exception {
        FundDto dto = new FundDto();
        dto.setFundCode("AAK");
        dto.setFundName("Test Fon");

        when(tefasService.getFund(eq("AAK"), any())).thenReturn(Optional.of(dto));

        mockMvc.perform(get("/v1/funds/AAK").param("fields", "fundCode,fundName"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fundCode").exists())
                .andExpect(jsonPath("$.fundName").exists());
    }
}
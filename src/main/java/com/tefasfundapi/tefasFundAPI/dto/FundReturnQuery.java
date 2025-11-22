package com.tefasfundapi.tefasFundAPI.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class FundReturnQuery {
    @NotBlank private String calismatipi;          // ör: "2"
    @NotBlank private String fontip;               // ör: "YAT"
    private String sfontur = "";
    private String kurucukod = "";
    private String fongrup  = "";

    @NotBlank
    @Pattern(regexp="\\d{2}\\.\\d{2}\\.\\d{4}")    // dd.MM.yyyy
    private String bastarih;

    @NotBlank
    @Pattern(regexp="\\d{2}\\.\\d{2}\\.\\d{4}")
    private String bittarih;

    private String fonturkod = "";
    private String fonunvantip = "";
    @NotBlank private String strperiod;            // "1,1,1,1,1,1,1,1"
    @NotBlank private String islemdurum;           // "1"
}
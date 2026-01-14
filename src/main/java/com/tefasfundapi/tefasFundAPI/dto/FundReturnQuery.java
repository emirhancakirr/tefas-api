package com.tefasfundapi.tefasFundAPI.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class FundReturnQuery {
    @NotBlank
    private String calismatipi; // ör: "2"
    @NotBlank
    private String fontip; // ör: "YAT"
    private String sfontur = "";
    private String kurucukod = "";
    private String fongrup = "";

    @NotBlank
    @Pattern(regexp = "\\d{2}\\.\\d{2}\\.\\d{4}") // dd.MM.yyyy
    private String bastarih;

    @NotBlank
    @Pattern(regexp = "\\d{2}\\.\\d{2}\\.\\d{4}")
    private String bittarih;

    private String fonturkod = "";
    private String fonunvantip = "";
    @NotBlank
    private String strperiod; // "1,1,1,1,1,1,1,1"
    @NotBlank
    private String islemdurum; // "1"

    public String getCalismatipi() {
        return calismatipi;
    }

    public void setCalismatipi(String calismatipi) {
        this.calismatipi = calismatipi;
    }

    public String getFontip() {
        return fontip;
    }

    public void setFontip(String fontip) {
        this.fontip = fontip;
    }

    public String getSfontur() {
        return sfontur;
    }

    public void setSfontur(String sfontur) {
        this.sfontur = sfontur;
    }

    public String getKurucukod() {
        return kurucukod;
    }

    public void setKurucukod(String kurucukod) {
        this.kurucukod = kurucukod;
    }

    public String getFongrup() {
        return fongrup;
    }

    public void setFongrup(String fongrup) {
        this.fongrup = fongrup;
    }

    public String getBastarih() {
        return bastarih;
    }

    public void setBastarih(String bastarih) {
        this.bastarih = bastarih;
    }

    public String getBittarih() {
        return bittarih;
    }

    public void setBittarih(String bittarih) {
        this.bittarih = bittarih;
    }

    public String getFonturkod() {
        return fonturkod;
    }

    public void setFonturkod(String fonturkod) {
        this.fonturkod = fonturkod;
    }

    public String getFonunvantip() {
        return fonunvantip;
    }

    public void setFonunvantip(String fonunvantip) {
        this.fonunvantip = fonunvantip;
    }

    public String getStrperiod() {
        return strperiod;
    }

    public void setStrperiod(String strperiod) {
        this.strperiod = strperiod;
    }

    public String getIslemdurum() {
        return islemdurum;
    }

    public void setIslemdurum(String islemdurum) {
        this.islemdurum = islemdurum;
    }
}
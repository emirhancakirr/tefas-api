package com.tefasfundapi.tefasFundAPI.dto;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL) // null alanları JSON'a koyma
public class PeriodComparisonDto {

    private String fundCode;
    private String fundName;
    private String umbrellaType;

    private Double m1;
    private Double m3;
    private Double m6;
    private Double ytd;
    private Double y1;
    private Double y3;
    private Double y5;

    // Getter & Setter
    public String getFundCode() { return fundCode; }
    public void setFundCode(String fundCode) { this.fundCode = fundCode; }

    public String getFundName() { return fundName; }
    public void setFundName(String fundName) { this.fundName = fundName; }

    public String getUmbrellaType() { return umbrellaType; }
    public void setUmbrellaType(String umbrellaType) { this.umbrellaType = umbrellaType; }

    public Double getM1() { return m1; }
    public void setM1(Double m1) { this.m1 = m1; }

    public Double getM3() { return m3; }
    public void setM3(Double m3) { this.m3 = m3; }

    public Double getM6() { return m6; }
    public void setM6(Double m6) { this.m6 = m6; }

    public Double getYtd() { return ytd; }
    public void setYtd(Double ytd) { this.ytd = ytd; }

    public Double getY1() { return y1; }
    public void setY1(Double y1) { this.y1 = y1; }

    public Double getY3() { return y3; }
    public void setY3(Double y3) { this.y3 = y3; }

    public Double getY5() { return y5; }
    public void setY5(Double y5) { this.y5 = y5; }
}

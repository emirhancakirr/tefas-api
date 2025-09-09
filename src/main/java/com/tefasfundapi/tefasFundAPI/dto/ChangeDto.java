package com.tefasfundapi.tefasFundAPI.dto;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL) // null alanları JSON'a koyma
public class ChangeDto {

    private String fundCode;
    private String fundName;
    private String umbrellaType;
    private Double change; // örn: 0.057 = %5.7

    // Getter & Setter
    public String getFundCode() { return fundCode; }
    public void setFundCode(String fundCode) { this.fundCode = fundCode; }

    public String getFundName() { return fundName; }
    public void setFundName(String fundName) { this.fundName = fundName; }

    public String getUmbrellaType() { return umbrellaType; }
    public void setUmbrellaType(String umbrellaType) { this.umbrellaType = umbrellaType; }

    public Double getChange() { return change; }
    public void setChange(Double change) { this.change = change; }
}

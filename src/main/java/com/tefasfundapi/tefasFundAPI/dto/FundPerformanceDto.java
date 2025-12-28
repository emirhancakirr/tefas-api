package com.tefasfundapi.tefasFundAPI.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class FundPerformanceDto {
    private String fundCode;
    private String fundName;
    private String umbrellaType;

    private Double getiri;

    public String getFundCode() {
        return fundCode;
    }

    public void setFundCode(String fundCode) {
        this.fundCode = fundCode;
    }

    public String getFundName() {
        return fundName;
    }

    public void setFundName(String fundName) {
        this.fundName = fundName;
    }

    public String getUmbrellaType() {
        return umbrellaType;
    }

    public void setUmbrellaType(String umbrellaType) {
        this.umbrellaType = umbrellaType;
    }

    public Double getGetiri() {
        return getiri;
    }

    public void setGetiri(Double getiri) {
        this.getiri = getiri;
    }
}

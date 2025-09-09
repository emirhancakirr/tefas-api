package com.tefasfundapi.tefasFundAPI.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDate;

@JsonInclude(JsonInclude.Include.NON_NULL) // null alanları JSON'a koyma
public class FundDto {

    private String fundCode;
    private String fundName;
    private String umbrellaType;
    private String issuer;
    private LocalDate inceptionDate;
    private Double expenseRatio;

    // Getter & Setter
    public String getFundCode() { return fundCode; }
    public void setFundCode(String fundCode) { this.fundCode = fundCode; }

    public String getFundName() { return fundName; }
    public void setFundName(String fundName) { this.fundName = fundName; }

    public String getUmbrellaType() { return umbrellaType; }
    public void setUmbrellaType(String umbrellaType) { this.umbrellaType = umbrellaType; }

    public String getIssuer() { return issuer; }
    public void setIssuer(String issuer) { this.issuer = issuer; }

    public LocalDate getInceptionDate() { return inceptionDate; }
    public void setInceptionDate(LocalDate inceptionDate) { this.inceptionDate = inceptionDate; }

    public Double getExpenseRatio() { return expenseRatio; }
    public void setExpenseRatio(Double expenseRatio) { this.expenseRatio = expenseRatio; }
}
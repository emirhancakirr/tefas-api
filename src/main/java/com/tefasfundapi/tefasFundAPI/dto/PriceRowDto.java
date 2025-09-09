package com.tefasfundapi.tefasFundAPI.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDate;

@JsonInclude(JsonInclude.Include.NON_NULL) // null alanlar JSON'a girmez
public class PriceRowDto {

    private LocalDate date;
    private String fundCode;
    private String fundName;
    private Double price;
    private Long outstandingShares;
    private Double totalValue;
    private Integer holderCount;

    // Getter & Setter
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public String getFundCode() { return fundCode; }
    public void setFundCode(String fundCode) { this.fundCode = fundCode; }

    public String getFundName() { return fundName; }
    public void setFundName(String fundName) { this.fundName = fundName; }

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }

    public Long getOutstandingShares() { return outstandingShares; }
    public void setOutstandingShares(Long outstandingShares) { this.outstandingShares = outstandingShares; }

    public Double getTotalValue() { return totalValue; }
    public void setTotalValue(Double totalValue) { this.totalValue = totalValue; }

    public Integer getHolderCount() { return holderCount; }
    public void setHolderCount(Integer holderCount) { this.holderCount = holderCount; }
}

package com.tefasfundapi.tefasFundAPI.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDate;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class FundDto {
    private String fundCode;
    private String fundName;
    private String umbrellaType;
    private String issuer;
    private LocalDate inceptionDate;
    private Double expenseRatio;

    // Getiri alanları
    private Double getiri1A; // 1 ay getiri
    private Double getiri3A; // 3 ay getiri
    private Double getiri6A; // 6 ay getiri
    private Double getiri1Y; // 1 yıl getiri
    private Double getiriYB; // Yılbaşından itibaren getiri
    private Double getiri3Y; // 3 yıl getiri
    private Double getiri5Y; // 5 yıl getiri

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

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public LocalDate getInceptionDate() {
        return inceptionDate;
    }

    public void setInceptionDate(LocalDate inceptionDate) {
        this.inceptionDate = inceptionDate;
    }

    public Double getExpenseRatio() {
        return expenseRatio;
    }

    public void setExpenseRatio(Double expenseRatio) {
        this.expenseRatio = expenseRatio;
    }

    // Getiri getter/setter'ları
    public Double getGetiri1A() {
        return getiri1A;
    }

    public void setGetiri1A(Double getiri1A) {
        this.getiri1A = getiri1A;
    }

    public Double getGetiri3A() {
        return getiri3A;
    }

    public void setGetiri3A(Double getiri3A) {
        this.getiri3A = getiri3A;
    }

    public Double getGetiri6A() {
        return getiri6A;
    }

    public void setGetiri6A(Double getiri6A) {
        this.getiri6A = getiri6A;
    }

    public Double getGetiri1Y() {
        return getiri1Y;
    }

    public void setGetiri1Y(Double getiri1Y) {
        this.getiri1Y = getiri1Y;
    }

    public Double getGetiriYB() {
        return getiriYB;
    }

    public void setGetiriYB(Double getiriYB) {
        this.getiriYB = getiriYB;
    }

    public Double getGetiri3Y() {
        return getiri3Y;
    }

    public void setGetiri3Y(Double getiri3Y) {
        this.getiri3Y = getiri3Y;
    }

    public Double getGetiri5Y() {
        return getiri5Y;
    }

    public void setGetiri5Y(Double getiri5Y) {
        this.getiri5Y = getiri5Y;
    }
}
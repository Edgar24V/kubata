package ao.allon.kubata.faturacao.service.agt.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

/**
 * Objeto taxes (Impostos da Linha) conforme Decreto Executivo n.º 683/25.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TaxDTO {
    
    @JsonProperty("taxType")
    private String taxType;
    
    @JsonProperty("taxCountryRegion")
    private String taxCountryRegion;
    
    @JsonProperty("taxCode")
    private String taxCode;
    
    @JsonProperty("taxPercentage")
    private BigDecimal taxPercentage;
    
    @JsonProperty("taxAmount")
    private BigDecimal taxAmount;
    
    @JsonProperty("taxContribution")
    private BigDecimal taxContribution;
    
    @JsonProperty("taxExemptionCode")
    private String taxExemptionCode;
    
    public TaxDTO() {}
    
    // Getters and Setters
    public String getTaxType() {
        return taxType;
    }
    
    public void setTaxType(String taxType) {
        this.taxType = taxType;
    }
    
    public String getTaxCountryRegion() {
        return taxCountryRegion;
    }
    
    public void setTaxCountryRegion(String taxCountryRegion) {
        this.taxCountryRegion = taxCountryRegion;
    }
    
    public String getTaxCode() {
        return taxCode;
    }
    
    public void setTaxCode(String taxCode) {
        this.taxCode = taxCode;
    }
    
    public BigDecimal getTaxPercentage() {
        return taxPercentage;
    }
    
    public void setTaxPercentage(BigDecimal taxPercentage) {
        this.taxPercentage = taxPercentage;
    }
    
    public BigDecimal getTaxAmount() {
        return taxAmount;
    }
    
    public void setTaxAmount(BigDecimal taxAmount) {
        this.taxAmount = taxAmount;
    }
    
    public BigDecimal getTaxContribution() {
        return taxContribution;
    }
    
    public void setTaxContribution(BigDecimal taxContribution) {
        this.taxContribution = taxContribution;
    }
    
    public String getTaxExemptionCode() {
        return taxExemptionCode;
    }
    
    public void setTaxExemptionCode(String taxExemptionCode) {
        this.taxExemptionCode = taxExemptionCode;
    }
}

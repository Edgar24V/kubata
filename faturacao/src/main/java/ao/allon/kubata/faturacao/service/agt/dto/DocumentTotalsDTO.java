package ao.allon.kubata.faturacao.service.agt.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

/**
 * Objeto documentTotals (Totais do Documento) conforme Decreto Executivo n.º 683/25.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DocumentTotalsDTO {
    
    @JsonProperty("taxPayable")
    private BigDecimal taxPayable;
    
    @JsonProperty("netTotal")
    private BigDecimal netTotal;
    
    @JsonProperty("grossTotal")
    private BigDecimal grossTotal;
    
    @JsonProperty("currency")
    private CurrencyDTO currency;
    
    public DocumentTotalsDTO() {}
    
    // Getters and Setters
    public BigDecimal getTaxPayable() {
        return taxPayable;
    }
    
    public void setTaxPayable(BigDecimal taxPayable) {
        this.taxPayable = taxPayable;
    }
    
    public BigDecimal getNetTotal() {
        return netTotal;
    }
    
    public void setNetTotal(BigDecimal netTotal) {
        this.netTotal = netTotal;
    }
    
    public BigDecimal getGrossTotal() {
        return grossTotal;
    }
    
    public void setGrossTotal(BigDecimal grossTotal) {
        this.grossTotal = grossTotal;
    }
    
    public CurrencyDTO getCurrency() {
        return currency;
    }
    
    public void setCurrency(CurrencyDTO currency) {
        this.currency = currency;
    }
    
    /**
     * Objeto com informação da moeda (obrigatório se diferente de AOA)
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CurrencyDTO {
        
        @JsonProperty("currencyCode")
        private String currencyCode;
        
        @JsonProperty("currencyAmount")
        private BigDecimal currencyAmount;
        
        @JsonProperty("exchangeRate")
        private BigDecimal exchangeRate;
        
        public CurrencyDTO() {}
        
        public String getCurrencyCode() {
            return currencyCode;
        }
        
        public void setCurrencyCode(String currencyCode) {
            this.currencyCode = currencyCode;
        }
        
        public BigDecimal getCurrencyAmount() {
            return currencyAmount;
        }
        
        public void setCurrencyAmount(BigDecimal currencyAmount) {
            this.currencyAmount = currencyAmount;
        }
        
        public BigDecimal getExchangeRate() {
            return exchangeRate;
        }
        
        public void setExchangeRate(BigDecimal exchangeRate) {
            this.exchangeRate = exchangeRate;
        }
    }
}

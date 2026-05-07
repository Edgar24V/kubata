package ao.allon.kubata.faturacao.service.agt.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.List;

/**
 * Estrutura das Linhas (lines) conforme Decreto Executivo n.º 683/25.
 * Para FT e FR - Linhas de artigos/serviços.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LineDTO {
    
    @JsonProperty("lineNumber")
    private int lineNumber;
    
    @JsonProperty("productCode")
    private String productCode;
    
    @JsonProperty("productDescription")
    private String productDescription;
    
    @JsonProperty("quantity")
    private BigDecimal quantity;
    
    @JsonProperty("unitOfMeasure")
    private String unitOfMeasure;
    
    @JsonProperty("unitPrice")
    private BigDecimal unitPrice;
    
    @JsonProperty("unitPriceBase")
    private BigDecimal unitPriceBase;
    
    @JsonProperty("debitAmount")
    private BigDecimal debitAmount;
    
    @JsonProperty("creditAmount")
    private BigDecimal creditAmount;
    
    @JsonProperty("settlementAmount")
    private BigDecimal settlementAmount;
    
    @JsonProperty("taxes")
    private List<TaxDTO> taxes;
    
    @JsonProperty("referenceInfo")
    private ReferenceInfoDTO referenceInfo;
    
    public LineDTO() {}
    
    // Getters and Setters
    public int getLineNumber() {
        return lineNumber;
    }
    
    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }
    
    public String getProductCode() {
        return productCode;
    }
    
    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }
    
    public String getProductDescription() {
        return productDescription;
    }
    
    public void setProductDescription(String productDescription) {
        this.productDescription = productDescription;
    }
    
    public BigDecimal getQuantity() {
        return quantity;
    }
    
    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }
    
    public String getUnitOfMeasure() {
        return unitOfMeasure;
    }
    
    public void setUnitOfMeasure(String unitOfMeasure) {
        this.unitOfMeasure = unitOfMeasure;
    }
    
    public BigDecimal getUnitPrice() {
        return unitPrice;
    }
    
    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }
    
    public BigDecimal getUnitPriceBase() {
        return unitPriceBase;
    }
    
    public void setUnitPriceBase(BigDecimal unitPriceBase) {
        this.unitPriceBase = unitPriceBase;
    }
    
    public BigDecimal getDebitAmount() {
        return debitAmount;
    }
    
    public void setDebitAmount(BigDecimal debitAmount) {
        this.debitAmount = debitAmount;
    }
    
    public BigDecimal getCreditAmount() {
        return creditAmount;
    }
    
    public void setCreditAmount(BigDecimal creditAmount) {
        this.creditAmount = creditAmount;
    }
    
    public BigDecimal getSettlementAmount() {
        return settlementAmount;
    }
    
    public void setSettlementAmount(BigDecimal settlementAmount) {
        this.settlementAmount = settlementAmount;
    }
    
    public List<TaxDTO> getTaxes() {
        return taxes;
    }
    
    public void setTaxes(List<TaxDTO> taxes) {
        this.taxes = taxes;
    }
    
    public ReferenceInfoDTO getReferenceInfo() {
        return referenceInfo;
    }
    
    public void setReferenceInfo(ReferenceInfoDTO referenceInfo) {
        this.referenceInfo = referenceInfo;
    }
    
    /**
     * Informação de referência ao documento de origem (obrigatório para NC)
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ReferenceInfoDTO {
        @JsonProperty("referenceNo")
        private String referenceNo;
        
        @JsonProperty("referenceDate")
        private String referenceDate;
        
        public ReferenceInfoDTO() {}
        
        public String getReferenceNo() {
            return referenceNo;
        }
        
        public void setReferenceNo(String referenceNo) {
            this.referenceNo = referenceNo;
        }
        
        public String getReferenceDate() {
            return referenceDate;
        }
        
        public void setReferenceDate(String referenceDate) {
            this.referenceDate = referenceDate;
        }
    }
}

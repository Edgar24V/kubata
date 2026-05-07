package ao.allon.kubata.faturacao.service.agt.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

/**
 * Retenção na fonte (WithholdingTax) conforme Decreto Executivo n.º 683/25.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WithholdingTaxDTO {
    
    @JsonProperty("withholdingTaxType")
    private String withholdingTaxType;
    
    @JsonProperty("withholdingTaxDescription")
    private String withholdingTaxDescription;
    
    @JsonProperty("withholdingTaxAmount")
    private BigDecimal withholdingTaxAmount;
    
    public WithholdingTaxDTO() {}
    
    // Getters and Setters
    public String getWithholdingTaxType() {
        return withholdingTaxType;
    }
    
    public void setWithholdingTaxType(String withholdingTaxType) {
        this.withholdingTaxType = withholdingTaxType;
    }
    
    public String getWithholdingTaxDescription() {
        return withholdingTaxDescription;
    }
    
    public void setWithholdingTaxDescription(String withholdingTaxDescription) {
        this.withholdingTaxDescription = withholdingTaxDescription;
    }
    
    public BigDecimal getWithholdingTaxAmount() {
        return withholdingTaxAmount;
    }
    
    public void setWithholdingTaxAmount(BigDecimal withholdingTaxAmount) {
        this.withholdingTaxAmount = withholdingTaxAmount;
    }
}

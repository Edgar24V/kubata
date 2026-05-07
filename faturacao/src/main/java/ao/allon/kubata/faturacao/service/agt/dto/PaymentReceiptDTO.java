package ao.allon.kubata.faturacao.service.agt.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.List;

/**
 * Objeto paymentReceipt (Para Fatura/Recibo - FR) conforme Decreto Executivo n.º 683/25.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentReceiptDTO {
    
    @JsonProperty("sourceDocuments")
    private List<SourceDocumentDTO> sourceDocuments;
    
    public PaymentReceiptDTO() {}
    
    // Getters and Setters
    public List<SourceDocumentDTO> getSourceDocuments() {
        return sourceDocuments;
    }
    
    public void setSourceDocuments(List<SourceDocumentDTO> sourceDocuments) {
        this.sourceDocuments = sourceDocuments;
    }
    
    /**
     * Objeto sourceDocumentID (dentro do array)
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SourceDocumentDTO {
        
        @JsonProperty("lineNo")
        private int lineNo;
        
        @JsonProperty("sourceDocumentID")
        private SourceDocumentIDDTO sourceDocumentID;
        
        @JsonProperty("debitAmount")
        private BigDecimal debitAmount;
        
        @JsonProperty("creditAmount")
        private BigDecimal creditAmount;
        
        public SourceDocumentDTO() {}
        
        public int getLineNo() {
            return lineNo;
        }
        
        public void setLineNo(int lineNo) {
            this.lineNo = lineNo;
        }
        
        public SourceDocumentIDDTO getSourceDocumentID() {
            return sourceDocumentID;
        }
        
        public void setSourceDocumentID(SourceDocumentIDDTO sourceDocumentID) {
            this.sourceDocumentID = sourceDocumentID;
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
    }
    
    /**
     * Contém OriginatingON e documentDate
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SourceDocumentIDDTO {
        
        @JsonProperty("originatingON")
        private String originatingON;
        
        @JsonProperty("documentDate")
        private String documentDate;
        
        public SourceDocumentIDDTO() {}
        
        public String getOriginatingON() {
            return originatingON;
        }
        
        public void setOriginatingON(String originatingON) {
            this.originatingON = originatingON;
        }
        
        public String getDocumentDate() {
            return documentDate;
        }
        
        public void setDocumentDate(String documentDate) {
            this.documentDate = documentDate;
        }
    }
}

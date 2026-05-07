package ao.allon.kubata.faturacao.service.agt.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.List;

/**
 * Estrutura do Documento (Objeto document) conforme Decreto Executivo n.º 683/25.
 * Núcleo da Fatura (FT) ou Fatura/Recibo (FR).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DocumentDTO {
    
    @JsonProperty("documentNo")
    private String documentNo;
    
    @JsonProperty("documentStatus")
    private String documentStatus;
    
    @JsonProperty("documentCancelReason")
    private String documentCancelReason;
    
    @JsonProperty("rejectedDocumentNo")
    private String rejectedDocumentNo;
    
    @JsonProperty("jwsDocumentSignature")
    private String jwsDocumentSignature;
    
    @JsonProperty("documentDate")
    private String documentDate;
    
    @JsonProperty("documentType")
    private String documentType;
    
    @JsonProperty("systemEntryDate")
    private String systemEntryDate;
    
    @JsonProperty("customerCountry")
    private String customerCountry;
    
    @JsonProperty("customerTaxID")
    private String customerTaxID;
    
    @JsonProperty("companyName")
    private String companyName;
    
    @JsonProperty("eacCode")
    private String eacCode;
    
    @JsonProperty("lines")
    private List<LineDTO> lines;
    
    @JsonProperty("paymentReceipt")
    private PaymentReceiptDTO paymentReceipt;
    
    @JsonProperty("documentTotals")
    private DocumentTotalsDTO documentTotals;
    
    @JsonProperty("withholdingTaxList")
    private List<WithholdingTaxDTO> withholdingTaxList;
    
    public DocumentDTO() {}
    
    // Getters and Setters
    public String getDocumentNo() {
        return documentNo;
    }
    
    public void setDocumentNo(String documentNo) {
        this.documentNo = documentNo;
    }
    
    public String getDocumentStatus() {
        return documentStatus;
    }
    
    public void setDocumentStatus(String documentStatus) {
        this.documentStatus = documentStatus;
    }
    
    public String getDocumentCancelReason() {
        return documentCancelReason;
    }
    
    public void setDocumentCancelReason(String documentCancelReason) {
        this.documentCancelReason = documentCancelReason;
    }
    
    public String getRejectedDocumentNo() {
        return rejectedDocumentNo;
    }
    
    public void setRejectedDocumentNo(String rejectedDocumentNo) {
        this.rejectedDocumentNo = rejectedDocumentNo;
    }
    
    public String getJwsDocumentSignature() {
        return jwsDocumentSignature;
    }
    
    public void setJwsDocumentSignature(String jwsDocumentSignature) {
        this.jwsDocumentSignature = jwsDocumentSignature;
    }
    
    public String getDocumentDate() {
        return documentDate;
    }
    
    public void setDocumentDate(String documentDate) {
        this.documentDate = documentDate;
    }
    
    public String getDocumentType() {
        return documentType;
    }
    
    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }
    
    public String getSystemEntryDate() {
        return systemEntryDate;
    }
    
    public void setSystemEntryDate(String systemEntryDate) {
        this.systemEntryDate = systemEntryDate;
    }
    
    public String getCustomerCountry() {
        return customerCountry;
    }
    
    public void setCustomerCountry(String customerCountry) {
        this.customerCountry = customerCountry;
    }
    
    public String getCustomerTaxID() {
        return customerTaxID;
    }
    
    public void setCustomerTaxID(String customerTaxID) {
        this.customerTaxID = customerTaxID;
    }
    
    public String getCompanyName() {
        return companyName;
    }
    
    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }
    
    public String getEacCode() {
        return eacCode;
    }
    
    public void setEacCode(String eacCode) {
        this.eacCode = eacCode;
    }
    
    public List<LineDTO> getLines() {
        return lines;
    }
    
    public void setLines(List<LineDTO> lines) {
        this.lines = lines;
    }
    
    public PaymentReceiptDTO getPaymentReceipt() {
        return paymentReceipt;
    }
    
    public void setPaymentReceipt(PaymentReceiptDTO paymentReceipt) {
        this.paymentReceipt = paymentReceipt;
    }
    
    public DocumentTotalsDTO getDocumentTotals() {
        return documentTotals;
    }
    
    public void setDocumentTotals(DocumentTotalsDTO documentTotals) {
        this.documentTotals = documentTotals;
    }
    
    public List<WithholdingTaxDTO> getWithholdingTaxList() {
        return withholdingTaxList;
    }
    
    public void setWithholdingTaxList(List<WithholdingTaxDTO> withholdingTaxList) {
        this.withholdingTaxList = withholdingTaxList;
    }
}

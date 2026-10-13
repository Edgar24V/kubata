package ao.allon.kubata.faturacao.service.agt.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Envoltório principal (Wrapper) da requisição de submissão de faturas à AGT.
 * Conforme Decreto Executivo n.º 683/25 - Anexo I.
 */
public class AGTInvoiceSubmissionRequest {
    
    @JsonProperty("schemaVersion")
    private String schemaVersion = "2.0";
    
    @JsonProperty("submissionUUID")
    private String submissionUUID;
    
    @JsonProperty("taxRegistrationNumber")
    private String taxRegistrationNumber;
    
    @JsonProperty("submissionTimeStamp")
    private String submissionTimeStamp;
    
    @JsonProperty("softwareInfo")
    private SoftwareInfo softwareInfo;
    
    @JsonProperty("numberOfEntries")
    private int numberOfEntries;
    
    @JsonProperty("documents")
    private List<DocumentDTO> documents;
    
    public AGTInvoiceSubmissionRequest() {}
    
    public AGTInvoiceSubmissionRequest(String submissionGUID, String taxRegistrationNumber, 
                                     String submissionTimeStamp, SoftwareInfo softwareInfo,
                                     List<DocumentDTO> documents) {
        this.submissionGUID = submissionGUID;
        this.taxRegistrationNumber = taxRegistrationNumber;
        this.submissionTimeStamp = submissionTimeStamp;
        this.softwareInfo = softwareInfo;
        this.documents = documents;
        this.numberOfEntries = documents != null ? documents.size() : 0;
    }
    
    // Getters and Setters
    public String getSchemaVersion() {
        return schemaVersion;
    }
    
    public void setSchemaVersion(String schemaVersion) {
        this.schemaVersion = schemaVersion;
    }
    
    public String getSubmissionUUID() {
        return submissionGUID;
    }
    
    public void setSubmissionGUID(String submissionGUID) {
        this.submissionGUID = submissionGUID;
    }
    
    public String getTaxRegistrationNumber() {
        return taxRegistrationNumber;
    }
    
    public void setTaxRegistrationNumber(String taxRegistrationNumber) {
        this.taxRegistrationNumber = taxRegistrationNumber;
    }
    
    public String getSubmissionTimeStamp() {
        return submissionTimeStamp;
    }
    
    public void setSubmissionTimeStamp(String submissionTimeStamp) {
        this.submissionTimeStamp = submissionTimeStamp;
    }
    
    public SoftwareInfo getSoftwareInfo() {
        return softwareInfo;
    }
    
    public void setSoftwareInfo(SoftwareInfo softwareInfo) {
        this.softwareInfo = softwareInfo;
    }
    
    public int getNumberOfEntries() {
        return numberOfEntries;
    }
    
    public void setNumberOfEntries(int numberOfEntries) {
        this.numberOfEntries = numberOfEntries;
    }
    
    public List<DocumentDTO> getDocuments() {
        return documents;
    }
    
    public void setDocuments(List<DocumentDTO> documents) {
        this.documents = documents;
        this.numberOfEntries = documents != null ? documents.size() : 0;
    }
    
    /**
     * Software Info conforme especificação AGT
     */
    public static class SoftwareInfo {
        @JsonProperty("softwareInfoDetail")
        private SoftwareInfoDetail softwareInfoDetail;
        
        @JsonProperty("jwsSoftwareSignature")
        private String jwsSoftwareSignature;
        
        public SoftwareInfo() {}
        
        public SoftwareInfo(SoftwareInfoDetail softwareInfoDetail, String jwsSoftwareSignature) {
            this.softwareInfoDetail = softwareInfoDetail;
            this.jwsSoftwareSignature = jwsSoftwareSignature;
        }
        
        public SoftwareInfoDetail getSoftwareInfoDetail() {
            return softwareInfoDetail;
        }
        
        public void setSoftwareInfoDetail(SoftwareInfoDetail softwareInfoDetail) {
            this.softwareInfoDetail = softwareInfoDetail;
        }
        
        public String getJwsSoftwareSignature() {
            return jwsSoftwareSignature;
        }
        
        public void setJwsSoftwareSignature(String jwsSoftwareSignature) {
            this.jwsSoftwareSignature = jwsSoftwareSignature;
        }
    }
    
    /**
     * Detalhes do software de faturação
     */
    public static class SoftwareInfoDetail {
        @JsonProperty("productId")
        private String productId;
        
        @JsonProperty("productVersion")
        private String productVersion;
        
        @JsonProperty("softwareValidationNumber")
        private String softwareValidationNumber;
        
        public SoftwareInfoDetail() {}
        
        @JsonProperty("signatureVersion")
        private Integer signatureVersion = 1;

        public SoftwareInfoDetail() {}

        public SoftwareInfoDetail(String productId, String productVersion, String softwareValidationNumber) {
            this(productId, productVersion, softwareValidationNumber, 1);
        }

        public SoftwareInfoDetail(String productId, String productVersion, String softwareValidationNumber, Integer signatureVersion) {
            this.productId = productId;
            this.productVersion = productVersion;
            this.softwareValidationNumber = softwareValidationNumber;
            this.signatureVersion = signatureVersion == null ? 1 : signatureVersion;
        }
        
        public String getProductId() {
            return productId;
        }
        
        public void setProductId(String productId) {
            this.productId = productId;
        }
        
        public String getProductVersion() {
            return productVersion;
        }
        
        public void setProductVersion(String productVersion) {
            this.productVersion = productVersion;
        }
        
        public String getSoftwareValidationNumber() {
            return softwareValidationNumber;
        }
        
        public void setSoftwareValidationNumber(String softwareValidationNumber) {
            this.softwareValidationNumber = softwareValidationNumber;
        }

        public Integer getSignatureVersion() {
            return signatureVersion;
        }

        public void setSignatureVersion(Integer signatureVersion) {
            this.signatureVersion = signatureVersion;
        }
    }
}

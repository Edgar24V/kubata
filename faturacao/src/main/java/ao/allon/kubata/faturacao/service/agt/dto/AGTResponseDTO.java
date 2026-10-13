package ao.allon.kubata.faturacao.service.agt.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAlias;
import java.util.List;

/**
 * Resposta da API AGT após submissão de faturas.
 * Conforme modelo de validação a posteriori (asynchronous response).
 */
public class AGTResponseDTO {
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("message")
    private String message;
    
    @JsonProperty("submissionId")
    private String submissionId;
    
    @JsonProperty("validationResults")
    private List<ValidationResultDTO> validationResults;

    @JsonProperty("requestID")
    private String requestID;

    @JsonProperty("errorList")
    private List<AGTErrorEntryDTO> errorList;

    @JsonProperty("resultCode")
    private Integer resultCode;

    @JsonProperty("taxRegistrationNumber")
    private String taxRegistrationNumber;

    @JsonProperty("documentStatusList")
    private List<DocumentStatusDTO> documentStatusList;

    @JsonProperty("requestErrorList")
    private List<AGTErrorEntryDTO> requestErrorList;

    @JsonProperty("successRequestID")
    private String successRequestID;
    
    public AGTResponseDTO() {}
    
    // Getters and Setters
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getSubmissionId() {
        return submissionId;
    }
    
    public void setSubmissionId(String submissionId) {
        this.submissionId = submissionId;
    }
    
    public List<ValidationResultDTO> getValidationResults() {
        return validationResults;
    }
    
    public void setValidationResults(List<ValidationResultDTO> validationResults) {
        this.validationResults = validationResults;
    }

    public String getRequestID() { return requestID; }
    public void setRequestID(String requestID) { this.requestID = requestID; }
    public List<AGTErrorEntryDTO> getErrorList() { return errorList; }
    public void setErrorList(List<AGTErrorEntryDTO> errorList) { this.errorList = errorList; }
    public Integer getResultCode() { return resultCode; }
    public void setResultCode(Integer resultCode) { this.resultCode = resultCode; }
    public String getTaxRegistrationNumber() { return taxRegistrationNumber; }
    public void setTaxRegistrationNumber(String taxRegistrationNumber) { this.taxRegistrationNumber = taxRegistrationNumber; }
    public List<DocumentStatusDTO> getDocumentStatusList() { return documentStatusList; }
    public void setDocumentStatusList(List<DocumentStatusDTO> documentStatusList) { this.documentStatusList = documentStatusList; }
    public List<AGTErrorEntryDTO> getRequestErrorList() { return requestErrorList; }
    public void setRequestErrorList(List<AGTErrorEntryDTO> requestErrorList) { this.requestErrorList = requestErrorList; }
    public String getSuccessRequestID() { return successRequestID; }
    public void setSuccessRequestID(String successRequestID) { this.successRequestID = successRequestID; }
    
    /**
     * Resultado de validação individual por documento
     */
    public static class ValidationResultDTO {
        
        @JsonProperty("documentNo")
        private String documentNo;
        
        @JsonProperty("status")
        private String status; // ACCEPTED, REJECTED, PENDING
        
        @JsonProperty("validationCode")
        private String validationCode; // ID único atribuído pela AGT
        
        @JsonProperty("validationDate")
        private String validationDate;
        
        @JsonProperty("errors")
        private List<ValidationErrorDTO> errors;
        
        public ValidationResultDTO() {}
        
        public String getDocumentNo() {
            return documentNo;
        }
        
        public void setDocumentNo(String documentNo) {
            this.documentNo = documentNo;
        }
        
        public String getStatus() {
            return status;
        }
        
        public void setStatus(String status) {
            this.status = status;
        }
        
        public String getValidationCode() {
            return validationCode;
        }
        
        public void setValidationCode(String validationCode) {
            this.validationCode = validationCode;
        }
        
        public String getValidationDate() {
            return validationDate;
        }
        
        public void setValidationDate(String validationDate) {
            this.validationDate = validationDate;
        }
        
        public List<ValidationErrorDTO> getErrors() {
            return errors;
        }
        
        public void setErrors(List<ValidationErrorDTO> errors) {
            this.errors = errors;
        }
    }
    
    /**
     * Erro de validação específico
     */
    public static class ValidationErrorDTO {
        
        @JsonProperty("code")
        private String code;
        
        @JsonProperty("field")
        private String field;
        
        @JsonProperty("message")
        private String message;
        
        public ValidationErrorDTO() {}
        
        public String getCode() {
            return code;
        }
        
        public void setCode(String code) {
            this.code = code;
        }
        
        public String getField() {
            return field;
        }
        
        public void setField(String field) {
            this.field = field;
        }
        
        public String getMessage() {
            return message;
        }
        
        public void setMessage(String message) {
            this.message = message;
        }
    }

    /** Estrutura de erro utilizada nos serviços oficiais de registo/consulta da AGT. */
    public static class AGTErrorEntryDTO {
        @JsonProperty("errorCode")
        @JsonAlias("idError")
        private String errorCode;
        @JsonProperty("errorDescription")
        @JsonAlias("descriptionError")
        private String errorDescription;
        @JsonProperty("documentNo")
        private String documentNo;

        public String getDocumentNo() { return documentNo; }
        public void setDocumentNo(String documentNo) { this.documentNo = documentNo; }
        public String getErrorCode() { return errorCode; }
        public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
        public String getErrorDescription() { return errorDescription; }
        public void setErrorDescription(String errorDescription) { this.errorDescription = errorDescription; }
    }

    /** Estado oficial devolvido pela consulta posterior do pedido de registo. */
    public static class DocumentStatusDTO {
        @JsonProperty("documentNo")
        private String documentNo;
        @JsonProperty("documentStatus")
        private String documentStatus;
        @JsonProperty("errorList")
        private List<AGTErrorEntryDTO> errorList;

        public String getDocumentNo() { return documentNo; }
        public void setDocumentNo(String documentNo) { this.documentNo = documentNo; }
        public String getDocumentStatus() { return documentStatus; }
        public void setDocumentStatus(String documentStatus) { this.documentStatus = documentStatus; }
        public List<AGTErrorEntryDTO> getErrorList() { return errorList; }
        public void setErrorList(List<AGTErrorEntryDTO> errorList) { this.errorList = errorList; }
    }

}

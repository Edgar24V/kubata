package ao.allon.kubata.faturacao.service.agt.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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
}

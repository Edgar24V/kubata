package ao.allon.kubata.faturacao.service.agt.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Documento de origem a que uma nota de crédito/débito se refere no schema AGT.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReferenceInfoDTO {
    @JsonProperty("reference")
    private String reference;

    @JsonProperty("reason")
    private String reason;

    public ReferenceInfoDTO() {}

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}

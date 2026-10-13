package ao.allon.kubata.faturacao.service.agt.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AGTInvoiceSubmissionRequestTest {

    @Test
    void serializesSchemaVersionSubmissionUuidAndSignatureVersion() throws Exception {
        AGTInvoiceSubmissionRequest.SoftwareInfoDetail detail =
            new AGTInvoiceSubmissionRequest.SoftwareInfoDetail("Kubata ERP", "1.0.0", "C_TEST", 1);
        AGTInvoiceSubmissionRequest.SoftwareInfo info =
            new AGTInvoiceSubmissionRequest.SoftwareInfo(detail, "test-signature");
        AGTInvoiceSubmissionRequest request = new AGTInvoiceSubmissionRequest(
            "550e8400-e29b-41d4-a716-446655440000",
            "5001636863",
            "2026-10-13T12:00:00Z",
            info,
            List.of()
        );

        JsonNode json = new ObjectMapper().readTree(new ObjectMapper().writeValueAsBytes(request));

        assertEquals("2.0", json.path("schemaVersion").asText());
        assertEquals("550e8400-e29b-41d4-a716-446655440000", json.path("submissionUUID").asText());
        assertFalse(json.has("submissionGUID"));
        assertEquals(1, json.path("softwareInfo").path("softwareInfoDetail")
            .path("signatureVersion").asInt());
        assertEquals(0, json.path("numberOfEntries").asInt());
    }
}

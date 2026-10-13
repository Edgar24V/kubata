package ao.allon.kubata.faturacao.service.agt;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JWSDigitalSignatureServiceTest {

    @Test
    void developmentSignatureIsOnlyProducedWhenExplicitlyEnabled() {
        JWSDigitalSignatureService service = new JWSDigitalSignatureService();
        ReflectionTestUtils.setField(service, "signatureType", "RSA_SHA256");
        ReflectionTestUtils.setField(service, "allowDevelopmentSignatures", true);

        String signature = service.signJwsPayload(Map.of("productId", "Kubata"));

        assertNotNull(signature);
        assertFalse(signature.contains("."),
            "A development marker must not be mistaken for a compact JWS.");
    }

    @Test
    void productionModeMustRejectMissingCertificateInsteadOfReturningFakeJws() {
        JWSDigitalSignatureService service = new JWSDigitalSignatureService();
        ReflectionTestUtils.setField(service, "signatureType", "RSA_SHA256");
        ReflectionTestUtils.setField(service, "certificatePath", "");
        ReflectionTestUtils.setField(service, "certificatePassword", "");
        ReflectionTestUtils.setField(service, "allowDevelopmentSignatures", false);

        assertThrows(IllegalStateException.class,
            () -> service.signJwsPayload(Map.of("productId", "Kubata")));
        assertThrows(IllegalStateException.class, service::assertProductionReady);
    }
}

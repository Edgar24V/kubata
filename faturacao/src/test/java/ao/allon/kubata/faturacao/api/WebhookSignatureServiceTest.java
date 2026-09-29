package ao.allon.kubata.faturacao.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WebhookSignatureServiceTest {

    private final WebhookSignatureService service = new WebhookSignatureService();

    @Test
    void shouldSignAndVerifyPayload() {
        String timestamp = "2026-09-29T12:00:00Z";
        String payload = "{\"id\":\"evt_1\",\"type\":\"webhook.test\"}";
        String secret = "kubata-test-secret";

        String signature = service.sign(timestamp, payload, secret);

        assertTrue(signature.startsWith("sha256="));
        assertTrue(service.verify(timestamp, payload, secret, signature));
        assertFalse(service.verify(timestamp, payload + "x", secret, signature));
    }
}

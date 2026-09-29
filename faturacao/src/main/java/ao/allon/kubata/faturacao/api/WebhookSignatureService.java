package ao.allon.kubata.faturacao.api;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

public final class WebhookSignatureService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    public String sign(String timestamp, String payload, String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("Webhook secret não configurado.");
        }

        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            byte[] digest = mac.doFinal((timestamp + "." + payload).getBytes(StandardCharsets.UTF_8));
            return "sha256=" + HexFormat.of().formatHex(digest);
        } catch (Exception ex) {
            throw new IllegalStateException("Não foi possível assinar o webhook.", ex);
        }
    }

    public boolean verify(String timestamp, String payload, String secret, String signature) {
        if (signature == null || signature.isBlank()) {
            return false;
        }

        String expected = sign(timestamp, payload, secret);
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8)
        );
    }
}

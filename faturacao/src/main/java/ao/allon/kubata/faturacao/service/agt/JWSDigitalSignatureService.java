package ao.allon.kubata.faturacao.service.agt;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

/**
 * Serviço de Assinatura Digital JWS para conformidade com Decreto 683/25
 * Implementa assinatura de documentos fiscais no formato JSON Web Signature (JWS)
 * conforme especificações da AGT para Faturação Eletrónica em Tempo Real
 */
@Service
public class JWSDigitalSignatureService {

    @Value("${agt.assinatura.tipo:HMAC_SHA256}")
    private String signatureType;

    @Value("${agt.assinatura.chave:}")
    private String hmacSecretKey;

    @Value("${agt.certificado.path:}")
    private String certificatePath;

    @Value("${agt.certificado.password:}")
    private String certificatePassword;

    @Value("${agt.allow-development-signatures:true}")
    private boolean allowDevelopmentSignatures;

    private final ObjectMapper objectMapper;
    private PrivateKey privateKey;
    private X509Certificate certificate;

    public JWSDigitalSignatureService() {
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Gera uma assinatura JWS compacta para o documento fiscal
     * Formato: BASE64URL(UTF8(JWS Protected Header)) || '.' ||
     *          BASE64URL(JWS Payload) || '.' ||
     *          BASE64URL(JWS Signature)
     *
     * @param documentNumber Número do documento fiscal (ex: FT 2025/001)
     * @param documentDate Data de emissão (formato ISO 8601)
     * @param totalAmount Valor total do documento
     * @param taxId NIF do cliente
     * @param hash Hash do documento (SAF-T-AO)
     * @return Assinatura JWS compacta ou null se não configurado
     */
    public String signDocument(String documentNumber, String documentDate,
                             java.math.BigDecimal totalAmount, String taxId, String hash) {
        try {
            // Verificar se temos configuração válida
            if (!isConfigured()) {
                if (!allowDevelopmentSignatures) {
                    throw new IllegalStateException("Assinatura fiscal não configurada. A assinatura de desenvolvimento está desactivada.");
                }
                return generateDevelopmentSignature(documentNumber, hash);
            }
            if (!allowDevelopmentSignatures && !"RSA_SHA256".equals(signatureType)) {
                throw new IllegalStateException("O ambiente de produção exige assinatura RS256 (RSA-SHA256).");
            }

            // Criar o payload do JWS
            JWSPayload payload = new JWSPayload(
                documentNumber,
                documentDate,
                totalAmount.toString(),
                taxId,
                hash,
                System.currentTimeMillis()
            );

            String payloadJson = objectMapper.writeValueAsString(payload);
            String encodedPayload = base64UrlEncode(payloadJson.getBytes(StandardCharsets.UTF_8));

            // Criar o header protegido
            JWSHeader header = new JWSHeader(getAlgorithm(), "JOSE");
            String headerJson = objectMapper.writeValueAsString(header);
            String encodedHeader = base64UrlEncode(headerJson.getBytes(StandardCharsets.UTF_8));

            // Criar a assinatura
            String signingInput = encodedHeader + "." + encodedPayload;
            byte[] signature = createSignature(signingInput);
            String encodedSignature = base64UrlEncode(signature);

            // Retornar JWS compacto
            return encodedHeader + "." + encodedPayload + "." + encodedSignature;

        } catch (Exception e) {
            throw new RuntimeException("Erro ao assinar documento digitalmente: " + e.getMessage(), e);
        }
    }

    /**
     * Cria uma JWS compacta para um payload oficial da AGT.
     * O chamador deve construir um Map ordenado com exactamente os campos
     * exigidos pelo contrato AGT; a ordenação faz parte dos bytes assinados.
     */
    public String signJwsPayload(Object payload) {
        try {
            if (!isConfigured()) {
                if (!allowDevelopmentSignatures) {
                    throw new IllegalStateException("Assinatura RS256 não configurada; assinatura simulada proibida.");
                }
                String json = objectMapper.writeValueAsString(payload);
                return generateDevelopmentSignature("PAYLOAD", sha256Prefix(json));
            }
            if (!allowDevelopmentSignatures && !"RSA_SHA256".equals(signatureType)) {
                throw new IllegalStateException("A AGT exige assinatura RS256 em produção.");
            }
            String payloadJson = objectMapper.writeValueAsString(payload);
            String encodedPayload = base64UrlEncode(payloadJson.getBytes(StandardCharsets.UTF_8));
            String headerJson = objectMapper.writeValueAsString(new JWSHeader(getAlgorithm(), "JOSE"));
            String encodedHeader = base64UrlEncode(headerJson.getBytes(StandardCharsets.UTF_8));
            String signingInput = encodedHeader + "." + encodedPayload;
            return signingInput + "." + base64UrlEncode(createSignature(signingInput));
        } catch (Exception e) {
            throw new IllegalStateException("Não foi possível assinar o payload JWS da AGT.", e);
        }
    }

    /**
     * Verifica se o serviço está configurado para assinatura real.
     */
    public boolean isConfigured() {
        if ("HMAC_SHA256".equals(signatureType)) {
            return hmacSecretKey != null && !hmacSecretKey.isEmpty()
                && !"chave_secreta_desenvolvimento_apenas".equals(hmacSecretKey);
        } else if ("RSA_SHA256".equals(signatureType)) {
            return certificatePath != null && !certificatePath.isEmpty()
                && certificatePassword != null && !certificatePassword.isEmpty();
        }
        return false;
    }

    /**
     * Devolve a chave privada carregada do certificado PKCS#12 de produção para
     * a assinatura de hash SAF-T, evitando chaves de desenvolvimento independentes.
     * Devolve null apenas no modo de desenvolvimento, para permitir os testes legados.
     */
    public PrivateKey getPrivateKeyForFiscalHash() {
        if (!"RSA_SHA256".equals(signatureType) || !isConfigured()) {
            if (!allowDevelopmentSignatures) {
                throw new IllegalStateException("A chave privada fiscal RSA não está configurada.");
            }
            return null;
        }
        try {
            loadCertificate();
            if (certificate == null || privateKey == null
                    || !"RSA".equalsIgnoreCase(privateKey.getAlgorithm())) {
                throw new IllegalStateException("O certificado não contém uma chave privada RSA válida.");
            }
            certificate.checkValidity();
            return privateKey;
        } catch (Exception ex) {
            throw new IllegalStateException("Não foi possível obter a chave privada do certificado fiscal.", ex);
        }
    }

    /**
     * Falha explicitamente se o material de assinatura não for adequado para produção.
     * A validação do registo do software junto da AGT é um requisito separado.
     */
    public void assertProductionReady() {
        if (!"RSA_SHA256".equals(signatureType)) {
            throw new IllegalStateException("A produção exige KUBATA_AGT_SIGNATURE_TYPE=RSA_SHA256.");
        }
        if (!isConfigured()) {
            throw new IllegalStateException("Certificado PKCS#12 e palavra-passe fiscal não configurados.");
        }
        try {
            loadCertificate();
            if (certificate == null || privateKey == null) {
                throw new IllegalStateException("O certificado não contém uma chave privada e certificado X.509.");
            }
            certificate.checkValidity();
            if (!"RSA".equalsIgnoreCase(privateKey.getAlgorithm())) {
                throw new IllegalStateException("A chave privada configurada não é RSA.");
            }
        } catch (Exception e) {
            throw new IllegalStateException("Certificado fiscal inválido ou inacessível.", e);
        }
    }

    /**
     * Gera uma assinatura de desenvolvimento (não válida para produção).
     */
    private String generateDevelopmentSignature(String documentNumber, String hash) {
        String safeHash = (hash == null || hash.isBlank()) ? "NO-HASH" : hash;
        String digest = sha256Prefix(documentNumber + "|" + safeHash);
        String devSignature = "DEV." + documentNumber + "." + digest;
        return base64UrlEncode(devSignature.getBytes(StandardCharsets.UTF_8));
    }

    private String sha256Prefix(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest).substring(0, 20);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 não está disponível.", e);
        }
    }

    /**
     * Cria a assinatura criptográfica usando HMAC ou RSA
     */
    private byte[] createSignature(String signingInput) throws Exception {
        if ("HMAC_SHA256".equals(signatureType)) {
            return createHmacSignature(signingInput);
        } else if ("RSA_SHA256".equals(signatureType)) {
            return createRsaSignature(signingInput);
        }
        throw new IllegalStateException("Tipo de assinatura não suportado: " + signatureType);
    }

    /**
     * Cria assinatura HMAC-SHA256
     */
    private byte[] createHmacSignature(String signingInput) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(
            hmacSecretKey.getBytes(StandardCharsets.UTF_8),
            "HmacSHA256"
        );
        mac.init(secretKey);
        return mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Cria assinatura RSA-SHA256 usando certificado digital
     */
    private byte[] createRsaSignature(String signingInput) throws Exception {
        if (privateKey == null) {
            loadCertificate();
        }

        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        signature.update(signingInput.getBytes(StandardCharsets.UTF_8));
        return signature.sign();
    }

    /**
     * Carrega o certificado digital PKCS12
     */
    private synchronized void loadCertificate() throws Exception {
        if (privateKey != null) return;

        java.security.KeyStore keyStore = java.security.KeyStore.getInstance("PKCS12");

        try (java.io.InputStream is = getCertificateInputStream()) {
            keyStore.load(is, certificatePassword.toCharArray());
        }

        java.util.Enumeration<String> aliases = keyStore.aliases();
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            if (keyStore.isKeyEntry(alias)) {
                privateKey = (PrivateKey) keyStore.getKey(alias, certificatePassword.toCharArray());
                Certificate cert = keyStore.getCertificate(alias);
                if (cert instanceof X509Certificate) {
                    certificate = (X509Certificate) cert;
                }
                break;
            }
        }

        if (privateKey == null) {
            throw new IllegalStateException("Não foi possível carregar a chave privada do certificado");
        }
    }

    /**
     * Obtém o InputStream do certificado
     */
    private java.io.InputStream getCertificateInputStream() throws Exception {
        if (certificatePath.startsWith("classpath:")) {
            String resourcePath = certificatePath.substring("classpath:".length());
            java.io.InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath);
            if (is == null) {
                throw new IllegalStateException("Certificado não encontrado no classpath: " + resourcePath);
            }
            return is;
        } else if (certificatePath.startsWith("file://")) {
            java.nio.file.Path path = java.nio.file.Paths.get(
                java.net.URI.create(certificatePath)
            );
            return java.nio.file.Files.newInputStream(path);
        } else {
            return java.nio.file.Files.newInputStream(java.nio.file.Paths.get(certificatePath));
        }
    }

    /**
     * Retorna o algoritmo de assinatura
     */
    private String getAlgorithm() {
        if ("HMAC_SHA256".equals(signatureType)) {
            return "HS256";
        } else if ("RSA_SHA256".equals(signatureType)) {
            return "RS256";
        }
        return "HS256";
    }

    /**
     * Codificação Base64Url (sem padding, sem caracteres +/)
     */
    private String base64UrlEncode(byte[] data) {
        return Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(data);
    }

    // Classes internas para estrutura JWS
    private static class JWSHeader {
        public final String typ;
        public final String alg;

        public JWSHeader(String alg, String typ) {
            this.typ = typ;
            this.alg = alg;
        }
    }

    private static class JWSPayload {
        public final String docNumber;
        public final String docDate;
        public final String totalAmount;
        public final String taxId;
        public final String hash;
        public final long timestamp;

        public JWSPayload(String docNumber, String docDate, String totalAmount,
                         String taxId, String hash, long timestamp) {
            this.docNumber = docNumber;
            this.docDate = docDate;
            this.totalAmount = totalAmount;
            this.taxId = taxId;
            this.hash = hash;
            this.timestamp = timestamp;
        }
    }
}

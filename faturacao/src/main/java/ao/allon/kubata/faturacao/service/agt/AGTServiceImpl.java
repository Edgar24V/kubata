package ao.allon.kubata.faturacao.service.agt;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.time.format.DateTimeFormatter;
import java.math.RoundingMode;
import java.util.Base64;
import java.util.logging.Logger;

/**
 * Implementação do serviço de Hash e QR Code da AGT (Angola).
 */
@Service
public class AGTServiceImpl implements AGTService {

    private static final Logger logger = Logger.getLogger(AGTServiceImpl.class.getName());
    private static final String SIGNATURE_ALGORITHM = "SHA1withRSA";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @Override
    public String generateDocumentHash(AGTInvoiceData data, PrivateKey privateKey) throws Exception {
        // Concatenação de campos conforme norma AGT (Angola):
        // DataFatura;DataEmissao;NumeroFatura;TotalBruto;HashAnterior
        // O TotalBruto deve usar ponto como separador decimal e ter 2 casas decimais.
        
        String previousHash = (data.previousHash() != null && !data.previousHash().isEmpty()) ? data.previousHash() : "";
        
        String total = data.totalAmount().setScale(2, RoundingMode.HALF_UP).toPlainString();
        String contentToSign = data.invoiceDate().format(DATE_FORMATTER) + ";"
                + data.systemEntryDate().format(DATETIME_FORMATTER) + ";"
                + data.invoiceNo() + ";" + total + ";" + previousHash;

        // Não registar em log o conteúdo fiscal usado na assinatura.

        Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
        signature.initSign(privateKey);
        signature.update(contentToSign.getBytes(StandardCharsets.UTF_8));
        byte[] signedBytes = signature.sign();

        String base64Signature = Base64.getEncoder().encodeToString(signedBytes);
        
        // Validação de segurança básica
        if (base64Signature.length() < 172) {
            logger.warning("ALERTA: O hash gerado parece curto (" + base64Signature.length() + " chars). Verifique o tamanho da chave RSA.");
        }
        
        return base64Signature;
    }

    @Override
    public String formatQRCodeData(String emitterTaxID, AGTInvoiceData data) {
        /*
         * O QR Code impresso deve abrir a consulta pública da factura da AGT.
         * Especificação oficial:
         * https://portaldoparceiro.minfin.gov.ao/doc-agt/doc-agt/2/qrcode.html
         * Os espaços do número documental são codificados como %20.
         */
        if (emitterTaxID == null || emitterTaxID.isBlank()) {
            throw new IllegalArgumentException("O NIF do emitente é obrigatório para gerar o QR Code AGT.");
        }
        if (data == null || data.invoiceNo() == null || data.invoiceNo().isBlank()) {
            throw new IllegalArgumentException("O número do documento é obrigatório para gerar o QR Code AGT.");
        }

        String nifEmitente = emitterTaxID.replaceAll("\\D", "");
        if (nifEmitente.isBlank()) {
            throw new IllegalArgumentException("O NIF do emitente não contém dígitos válidos.");
        }
        String documento = java.net.URLEncoder.encode(data.invoiceNo().trim(), StandardCharsets.UTF_8)
                .replace("+", "%20")
                .replace("%2F", "/");
        return "https://quiosqueagt.minfin.gov.ao/facturacao-eletronica/consultar-fe"
                + "?emissor=" + nifEmitente + "&document=" + documento;
    }

    @Override
    public boolean verifyDocumentHash(AGTInvoiceData data, String hash, PublicKey publicKey) {
        try {
            String previousHash = (data.previousHash() != null && !data.previousHash().isEmpty()) ? data.previousHash() : "";
            
            String total = data.totalAmount().setScale(2, RoundingMode.HALF_UP).toPlainString();
            String contentToVerify = data.invoiceDate().format(DATE_FORMATTER) + ";"
                    + data.systemEntryDate().format(DATETIME_FORMATTER) + ";"
                    + data.invoiceNo() + ";" + total + ";" + previousHash;

            Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
            signature.initVerify(publicKey);
            signature.update(contentToVerify.getBytes(StandardCharsets.UTF_8));

            byte[] decodedHash = Base64.getDecoder().decode(hash);
            return signature.verify(decodedHash);
        } catch (Exception e) {
            // Logar erro de verificação
            logger.severe("Erro na verificação do hash: " + e.getMessage());
            return false;
        }
    }
}

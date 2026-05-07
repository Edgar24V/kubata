package ao.allon.kubata.faturacao.service.agt;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.time.format.DateTimeFormatter;
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
        
        String contentToSign = String.format(java.util.Locale.US, "%s;%s;%s;%.2f;%s",
                data.invoiceDate().format(DATE_FORMATTER),
                data.systemEntryDate().format(DATETIME_FORMATTER),
                data.invoiceNo(),
                data.totalAmount().doubleValue(),
                previousHash);
        
        // Log para auditoria/debug da string a assinar (CRÍTICO para validação AGT)
        logger.info("AGT Signing String: [" + contentToSign + "]");

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
        // Formato obrigatório AGT (conforme prompt): 
        // NIF_Emitente;NIF_Cliente;TipoDoc;NumDoc;Data;Total;Hash
        
        String tipoDoc = "FT"; // Padrão
        String numDoc = data.invoiceNo();
        if (numDoc != null && numDoc.contains(" ")) {
            tipoDoc = numDoc.substring(0, numDoc.indexOf(' ')).trim();
        }

        return String.format(java.util.Locale.US, "%s;%s;%s;%s;%s;%.2f;%s",
                emitterTaxID,
                (data.customerTaxID() != null && !data.customerTaxID().isEmpty()) ? data.customerTaxID() : "000000000",
                tipoDoc,
                numDoc,
                data.invoiceDate().format(DATE_FORMATTER),
                data.totalAmount().doubleValue(),
                data.hash() != null ? data.hash() : "");
    }

    @Override
    public boolean verifyDocumentHash(AGTInvoiceData data, String hash, PublicKey publicKey) {
        try {
            String previousHash = (data.previousHash() != null && !data.previousHash().isEmpty()) ? data.previousHash() : "";
            
            String contentToVerify = String.format(java.util.Locale.US, "%s;%s;%s;%.2f;%s",
                    data.invoiceDate().format(DATE_FORMATTER),
                    data.systemEntryDate().format(DATETIME_FORMATTER),
                    data.invoiceNo(),
                    data.totalAmount().doubleValue(),
                    previousHash);

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

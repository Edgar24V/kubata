package ao.allon.kubata.faturacao.service.agt;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.LocalDateTime;

/**
 * Serviço responsável pela geração de Hash e assinatura digital conforme normas da AGT (Angola).
 */
public interface AGTService {

    /**
     * Gera o Hash de um documento fiscal.
     * Deve seguir o algoritmo RSA-SHA1.
     * O input deve ser a concatenação dos campos: DataFatura;DataEmissao;NumeroFatura;TotalBruto;HashAnterior
     *
     * @param data Dados da fatura.
     * @param privateKey Chave privada para assinatura.
     * @return Hash assinado em Base64.
     * @throws Exception Se houver erro na assinatura.
     */
    String generateDocumentHash(AGTInvoiceData data, PrivateKey privateKey) throws Exception;

    /**
     * Formata os dados para o QR Code da fatura conforme exigido pela AGT.
     * Formato: NIF_Emitente;NIF_Cliente;TipoDoc;NumDoc;Data;Total;Hash
     *
     * @param emitterTaxID NIF da empresa emitente.
     * @param data Dados da fatura já com o hash gerado.
     * @return String formatada para o QR Code.
     */
    String formatQRCodeData(String emitterTaxID, AGTInvoiceData data);

    /**
     * Verifica a assinatura de um documento.
     * Útil para auditoria interna.
     *
     * @param data Dados originais da fatura.
     * @param hash Hash assinado a ser verificado.
     * @param publicKey Chave pública correspondente.
     * @return true se a assinatura for válida.
     */
    boolean verifyDocumentHash(AGTInvoiceData data, String hash, PublicKey publicKey);
}

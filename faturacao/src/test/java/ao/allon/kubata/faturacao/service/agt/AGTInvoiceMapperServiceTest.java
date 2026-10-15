package ao.allon.kubata.faturacao.service.agt;

import ao.allon.kubata.faturacao.domain.Fatura;
import ao.allon.kubata.faturacao.domain.ItemFatura;
import ao.allon.kubata.faturacao.domain.enums.TipoDocumento;
import ao.allon.kubata.faturacao.domain.enums.StatusFatura;
import com.fasterxml.jackson.databind.ObjectMapper;
import ao.allon.kubata.faturacao.service.agt.dto.DocumentDTO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AGTInvoiceMapperServiceTest {

    @Test
    void cashReceiptRcMustUseDocumentLinesAndMustNotIncludePaymentReceipt() {
        JWSDigitalSignatureService signer = mock(JWSDigitalSignatureService.class);
        when(signer.signJwsPayload(any())).thenReturn("signed-jws");
        AGTInvoiceMapperService mapper = new AGTInvoiceMapperService(signer);

        Fatura fatura = new Fatura();
        fatura.setNumero("RC 2026/001");
        fatura.setTipoDocumento(TipoDocumento.RECIBO);
        fatura.setDataEmissao(LocalDate.of(2026, 10, 15));
        fatura.setSystemEntryDate(LocalDateTime.of(2026, 10, 15, 10, 30));
        fatura.setSubtotal(new BigDecimal("100.00"));
        fatura.setIva(new BigDecimal("14.00"));
        fatura.setTotal(new BigDecimal("114.00"));

        ItemFatura item = new ItemFatura();
        item.setDescricao("Serviço de demonstração");
        item.setQuantidade(1);
        item.setPrecoUnitario(new BigDecimal("100.00"));
        item.setTaxaIva(new BigDecimal("14.00"));
        item.setSubtotal(new BigDecimal("100.00"));
        item.setValorIva(new BigDecimal("14.00"));
        item.setTotal(new BigDecimal("114.00"));
        fatura.setItens(List.of(item));

        DocumentDTO document = mapper.mapToDocumentDTO(fatura);

        assertEquals("RC", document.getDocumentType());
        assertNull(document.getPaymentReceipt(),
            "O schema AGT não utiliza paymentReceipt no tipo RC.");
        assertNotNull(document.getLines());
        assertEquals(1, document.getLines().size());
        assertEquals("Serviço de demonstração", document.getLines().get(0).getProductDescription());
        assertEquals(new BigDecimal("114.00"), document.getDocumentTotals().getGrossTotal());
    }

    @Test
    void agtReceiptRgUsesPaymentReceiptWithoutCommercialLinesAndExactSchemaFieldNames() throws Exception {
        JWSDigitalSignatureService signer = mock(JWSDigitalSignatureService.class);
        when(signer.signJwsPayload(any())).thenReturn("signed-jws");
        AGTInvoiceMapperService mapper = new AGTInvoiceMapperService(signer);

        Fatura origem = criarFacturaOrigemIntegralmentePaga();
        Fatura recibo = criarRascunhoRecibo(origem, TipoDocumento.RECIBO_GERAL);

        DocumentDTO document = mapper.mapToDocumentDTO(recibo);

        assertEquals("RG", document.getDocumentType());
        assertNull(document.getLines(), "AR/RG não enviam linhas comerciais no schema AGT v2.");
        assertNotNull(document.getPaymentReceipt());
        assertEquals(1, document.getPaymentReceipt().getSourceDocuments().size());

        PaymentReceiptDTO.SourceDocumentDTO linha =
            document.getPaymentReceipt().getSourceDocuments().get(0);
        assertEquals(1, linha.getLineNo());
        assertEquals("FT 2026/001", linha.getSourceDocumentID().getOriginatingON());
        assertEquals("2026-10-15", linha.getSourceDocumentID().getDocumentDate());
        assertEquals(new BigDecimal("100.00"), linha.getCreditAmount());
        assertNull(linha.getDebitAmount());
        assertEquals(new BigDecimal("100.00"), document.getDocumentTotals().getNetTotal());
        assertEquals(new BigDecimal("14.00"), document.getDocumentTotals().getTaxPayable());
        assertEquals(new BigDecimal("114.00"), document.getDocumentTotals().getGrossTotal());

        String sourceIdJson = new ObjectMapper().writeValueAsString(linha.getSourceDocumentID());
        assertTrue(sourceIdJson.contains("\"OriginatingON\""),
            "A capitalização de OriginatingON faz parte do contrato AGT.");
        assertFalse(sourceIdJson.contains("\"originatingON\""));
    }

    @Test
    void agtReceiptArUsesTheSameReceiptSchema() {
        JWSDigitalSignatureService signer = mock(JWSDigitalSignatureService.class);
        when(signer.signJwsPayload(any())).thenReturn("signed-jws");
        AGTInvoiceMapperService mapper = new AGTInvoiceMapperService(signer);

        Fatura origem = criarFacturaOrigemIntegralmentePaga();
        Fatura recibo = criarRascunhoRecibo(origem, TipoDocumento.AVISO_COBRANCA_RECIBO);

        DocumentDTO document = mapper.mapToDocumentDTO(recibo);

        assertEquals("AR", document.getDocumentType());
        assertNull(document.getLines());
        assertNotNull(document.getPaymentReceipt());
    }

    @Test
    void agtReceiptMustRejectTotalsThatDoNotMatchTheSourceInvoice() {
        JWSDigitalSignatureService signer = mock(JWSDigitalSignatureService.class);
        when(signer.signJwsPayload(any())).thenReturn("signed-jws");
        AGTInvoiceMapperService mapper = new AGTInvoiceMapperService(signer);

        Fatura origem = criarFacturaOrigemIntegralmentePaga();
        Fatura recibo = criarRascunhoRecibo(origem, TipoDocumento.RECIBO_GERAL);
        recibo.setTotal(new BigDecimal("50.00"));

        IllegalStateException error = assertThrows(
            IllegalStateException.class, () -> mapper.mapToDocumentDTO(recibo));
        assertTrue(error.getMessage().contains("reconciliar"));
    }

    private Fatura criarFacturaOrigemIntegralmentePaga() {
        Fatura origem = new Fatura();
        origem.setId(70L);
        origem.setTipoDocumento(TipoDocumento.FATURA);
        origem.setNumero("FT 2026/001");
        origem.setDataEmissao(LocalDate.of(2026, 10, 15));
        origem.setSystemEntryDate(LocalDateTime.of(2026, 10, 15, 9, 0));
        origem.setStatus(StatusFatura.PAGA);
        origem.setSubtotal(new BigDecimal("100.00"));
        origem.setIva(new BigDecimal("14.00"));
        origem.setTotal(new BigDecimal("114.00"));
        origem.setTotalRetencao(BigDecimal.ZERO);

        ItemFatura item = new ItemFatura();
        item.setDescricao("Serviço de demonstração");
        item.setSubtotal(new BigDecimal("100.00"));
        item.setValorIva(new BigDecimal("14.00"));
        item.setTotal(new BigDecimal("114.00"));
        origem.setItens(List.of(item));
        return origem;
    }

    private Fatura criarRascunhoRecibo(Fatura origem, TipoDocumento tipo) {
        Fatura recibo = new Fatura();
        recibo.setId(71L);
        recibo.setTipoDocumento(tipo);
        recibo.setNumero(tipo.getCodigo() + " 2026/002");
        recibo.setDataEmissao(LocalDate.of(2026, 10, 15));
        recibo.setSystemEntryDate(LocalDateTime.of(2026, 10, 15, 10, 0));
        recibo.setStatus(StatusFatura.RASCUNHO);
        recibo.setFaturaReferencia(origem);
        recibo.setSubtotal(new BigDecimal("100.00"));
        recibo.setIva(new BigDecimal("14.00"));
        recibo.setTotal(new BigDecimal("114.00"));
        recibo.setTotalRetencao(BigDecimal.ZERO);
        recibo.setItens(List.of());
        return recibo;
    }

}

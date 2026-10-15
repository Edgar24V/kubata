package ao.allon.kubata.faturacao.service.agt;

import ao.allon.kubata.faturacao.domain.Fatura;
import ao.allon.kubata.faturacao.domain.ItemFatura;
import ao.allon.kubata.faturacao.domain.enums.TipoDocumento;
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
}

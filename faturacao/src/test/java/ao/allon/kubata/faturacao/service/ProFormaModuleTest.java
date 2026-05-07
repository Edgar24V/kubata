package ao.allon.kubata.faturacao.service;

import ao.allon.kubata.core.repository.PlanoContaRepository;
import ao.allon.kubata.core.service.ContabilidadeService;
import ao.allon.kubata.faturacao.domain.Cliente;
import ao.allon.kubata.faturacao.domain.Fatura;
import ao.allon.kubata.faturacao.domain.ItemFatura;
import ao.allon.kubata.faturacao.domain.enums.TipoDocumento;
import ao.allon.kubata.faturacao.repository.FaturaRepository;
import ao.allon.kubata.faturacao.repository.ItemFaturaRepository;
import ao.allon.kubata.faturacao.service.agt.AGTElectronicInvoiceService;
import ao.allon.kubata.faturacao.service.agt.AGTService;
import ao.allon.kubata.faturacao.service.agt.JWSDigitalSignatureService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ProFormaModuleTest {

    private FaturaRepository faturaRepository;
    private ItemFaturaRepository itemFaturaRepository;
    private ProdutoService produtoService;
    private SerieService serieService;
    private AGTService agtService;
    private AGTElectronicInvoiceService agtElectronicInvoiceService;
    private JWSDigitalSignatureService signatureService;
    private ContabilidadeService contabilidadeService;
    private PlanoContaRepository planoContaRepository;
    private SessionManager sessionManager;

    private FaturaService faturaService;

    @BeforeEach
    void setup() {
        faturaRepository = Mockito.mock(FaturaRepository.class);
        itemFaturaRepository = Mockito.mock(ItemFaturaRepository.class);
        produtoService = Mockito.mock(ProdutoService.class);
        serieService = Mockito.mock(SerieService.class);
        agtService = Mockito.mock(AGTService.class);
        agtElectronicInvoiceService = Mockito.mock(AGTElectronicInvoiceService.class);
        signatureService = Mockito.mock(JWSDigitalSignatureService.class);
        contabilidadeService = Mockito.mock(ContabilidadeService.class);
        planoContaRepository = Mockito.mock(PlanoContaRepository.class);
        sessionManager = Mockito.mock(SessionManager.class);

        when(agtElectronicInvoiceService.submitInvoiceAsync(any())).thenReturn(CompletableFuture.completedFuture(new ao.allon.kubata.faturacao.service.agt.dto.AGTResponseDTO()));

        faturaService = new FaturaService(faturaRepository, itemFaturaRepository, produtoService, serieService, agtService, agtElectronicInvoiceService, signatureService, contabilidadeService, planoContaRepository, sessionManager);
    }

    @Test
    void criarRascunho_ProForma_DeveDefinirTipoProForma() {
        Cliente c = new Cliente();
        c.setNome("Cliente Teste");
        when(faturaRepository.save(ArgumentMatchers.any(Fatura.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Fatura pf = faturaService.criarRascunho(c, TipoDocumento.PRO_FORMA);
        assertEquals(TipoDocumento.PRO_FORMA, pf.getTipoDocumento());
        assertNotNull(pf.getNumero());
    }

    @Test
    void converterProFormaEmFatura_DeveCopiarItens() {
        Fatura proForma = new Fatura();
        proForma.setId(1L);
        proForma.setTipoDocumento(TipoDocumento.PRO_FORMA);
        ItemFatura it = new ItemFatura();
        it.setDescricao("Item");
        it.setQuantidade(2);
        it.setPrecoUnitario(new BigDecimal("100.00"));
        it.setPercentualIva(new BigDecimal("14"));
        proForma.addItem(it);
        when(faturaRepository.findById(1L)).thenReturn(Optional.of(proForma));
        when(faturaRepository.save(ArgumentMatchers.any(Fatura.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Fatura nova = faturaService.converterProFormaEmFatura(1L);
        assertEquals(1, nova.getItens().size());
        assertEquals("Item", nova.getItens().get(0).getDescricao());
    }
}

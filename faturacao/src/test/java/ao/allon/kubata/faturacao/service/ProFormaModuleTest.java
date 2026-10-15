package ao.allon.kubata.faturacao.service;

import ao.allon.kubata.core.repository.PlanoContaRepository;
import ao.allon.kubata.core.service.ContabilidadeService;
import ao.allon.kubata.faturacao.domain.Cliente;
import ao.allon.kubata.faturacao.domain.Fatura;
import ao.allon.kubata.faturacao.domain.ItemFatura;
import ao.allon.kubata.faturacao.domain.enums.TipoDocumento;
import ao.allon.kubata.faturacao.domain.enums.StatusFatura;
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
    void converterProFormaEmFatura_DevePreservarLinhasEReferenciarOrigem() {
        Fatura proForma = new Fatura();
        proForma.setId(1L);
        proForma.setNumero("PF 2026/1");
        proForma.setTipoDocumento(TipoDocumento.PRO_FORMA);
        proForma.setStatus(StatusFatura.RASCUNHO);
        Cliente cliente = new Cliente();
        cliente.setId(20L);
        cliente.setNome("Cliente Teste");
        proForma.setCliente(cliente);

        ItemFatura it = new ItemFatura();
        it.setDescricao("Serviço por hora");
        it.setQuantidade(2);
        it.setQuantidadeDecimal(new BigDecimal("2.500"));
        it.setPrecoUnitario(new BigDecimal("100.00"));
        it.setPercentualIva(new BigDecimal("14"));
        it.setDesconto(new BigDecimal("5"));
        it.setLote("LOTE-2026-A");
        it.setPesoUnitario(new BigDecimal("1.25"));
        proForma.addItem(it);

        when(faturaRepository.findById(1L)).thenReturn(Optional.of(proForma));
        when(faturaRepository.save(ArgumentMatchers.any(Fatura.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Fatura nova = faturaService.converterProFormaEmFatura(1L);

        assertEquals(TipoDocumento.FATURA, nova.getTipoDocumento());
        assertEquals(1, nova.getItens().size());
        ItemFatura linha = nova.getItens().get(0);
        assertEquals("Serviço por hora", linha.getDescricao());
        assertEquals(0, new BigDecimal("2.500").compareTo(linha.getQuantidadeDecimal()));
        assertEquals(0, new BigDecimal("5").compareTo(linha.getDesconto()));
        assertEquals("LOTE-2026-A", linha.getLote());
        assertEquals(0, new BigDecimal("1.25").compareTo(linha.getPesoUnitario()));
        assertEquals(proForma, nova.getFaturaReferencia());
        assertEquals(StatusFatura.PROCESSADA, proForma.getStatus());
        verify(faturaRepository, times(2)).save(any(Fatura.class));
    }

    @Test
    void converterProFormaEmFatura_DeveBloquearSegundaConversao() {
        Fatura proForma = new Fatura();
        proForma.setId(1L);
        proForma.setTipoDocumento(TipoDocumento.PRO_FORMA);
        proForma.setStatus(StatusFatura.PROCESSADA);
        when(faturaRepository.findById(1L)).thenReturn(Optional.of(proForma));

        assertThrows(IllegalStateException.class,
                () -> faturaService.converterProFormaEmFatura(1L));
        verify(faturaRepository, never()).save(any(Fatura.class));
    }

    @Test
    void converterProFormaEmFatura_DeveBloquearOrigemSemLinhas() {
        Fatura proForma = new Fatura();
        proForma.setId(1L);
        proForma.setTipoDocumento(TipoDocumento.ORCAMENTO);
        proForma.setStatus(StatusFatura.RASCUNHO);
        proForma.setCliente(new Cliente());
        when(faturaRepository.findById(1L)).thenReturn(Optional.of(proForma));

        assertThrows(IllegalStateException.class,
                () -> faturaService.converterProFormaEmFatura(1L));
        verify(faturaRepository, never()).save(any(Fatura.class));
    }
}

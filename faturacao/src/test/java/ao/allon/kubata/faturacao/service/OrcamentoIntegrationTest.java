package ao.allon.kubata.faturacao.service;

import ao.allon.kubata.core.repository.PlanoContaRepository;
import ao.allon.kubata.core.service.ContabilidadeService;
import ao.allon.kubata.faturacao.domain.Cliente;
import ao.allon.kubata.faturacao.domain.Fatura;
import ao.allon.kubata.faturacao.domain.ItemFatura;
import ao.allon.kubata.faturacao.domain.Serie;
import ao.allon.kubata.faturacao.domain.enums.StatusFatura;
import ao.allon.kubata.faturacao.domain.enums.TipoDocumento;
import ao.allon.kubata.faturacao.repository.FaturaRepository;
import ao.allon.kubata.faturacao.repository.ItemFaturaRepository;
import ao.allon.kubata.faturacao.service.agt.AGTElectronicInvoiceService;
import ao.allon.kubata.faturacao.service.agt.AGTService;
import ao.allon.kubata.faturacao.service.agt.JWSDigitalSignatureService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OrcamentoIntegrationTest {

    private FaturaService faturaService;
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

    @BeforeEach
    void setUp() {
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
        
        // Mock default async behavior for AGTElectronicInvoiceService
        when(agtElectronicInvoiceService.submitInvoiceAsync(any())).thenReturn(java.util.concurrent.CompletableFuture.completedFuture(new ao.allon.kubata.faturacao.service.agt.dto.AGTResponseDTO()));

        faturaService = new FaturaService(faturaRepository, itemFaturaRepository, produtoService, serieService, agtService, agtElectronicInvoiceService, signatureService, contabilidadeService, planoContaRepository, sessionManager);
    }

    @Test
    void deveCriarOrcamentoComSucesso() {
        Fatura orcamento = new Fatura();
        orcamento.setTipoDocumento(TipoDocumento.ORCAMENTO);
        orcamento.setCliente(new Cliente());
        
        when(faturaRepository.save(any(Fatura.class))).thenAnswer(i -> i.getArgument(0));
        
        Fatura salvo = faturaService.salvar(orcamento);
        
        assertNotNull(salvo);
        assertEquals(TipoDocumento.ORCAMENTO, salvo.getTipoDocumento());
        verify(faturaRepository).save(orcamento);
    }

    @Test
    void deveEmitirOrcamentoCorretamente() {
        Fatura orcamento = new Fatura();
        orcamento.setId(1L);
        orcamento.setTipoDocumento(TipoDocumento.ORCAMENTO);
        orcamento.setStatus(StatusFatura.RASCUNHO);
        orcamento.setCliente(new Cliente());
        
        Serie serieOrcamento = new Serie();
        serieOrcamento.setTipoDocumento(TipoDocumento.ORCAMENTO);
        serieOrcamento.setDesignacao("2024");
        // serieOrcamento.setCodigo("ORC"); // O código vem do TipoDocumento
        serieOrcamento.setAno(2024);
        serieOrcamento.setAtiva(true);
        
        when(faturaRepository.findById(1L)).thenReturn(Optional.of(orcamento));
        when(serieService.findPadrao(TipoDocumento.ORCAMENTO)).thenReturn(Optional.of(serieOrcamento));
        when(serieService.getProximoNumero(serieOrcamento)).thenReturn(1L);
        when(faturaRepository.save(any(Fatura.class))).thenAnswer(i -> i.getArgument(0));
        
        Fatura emitido = faturaService.emitirFatura(1L);
        
        assertEquals(StatusFatura.EMITIDA, emitido.getStatus());
        assertEquals("OR 2024/1", emitido.getNumero());
        assertNotNull(emitido.getDataEmissao());
    }

    @Test
    void deveConverterOrcamentoEmFatura() {
        Fatura orcamento = new Fatura();
        orcamento.setId(1L);
        orcamento.setTipoDocumento(TipoDocumento.ORCAMENTO);
        orcamento.setStatus(StatusFatura.EMITIDA);
        orcamento.setCliente(new Cliente());
        orcamento.setNumero("ORC 2024/1");

        ItemFatura item = new ItemFatura();
        item.setDescricao("Serviço Dev");
        item.setQuantidade(1);
        item.setQuantidadeDecimal(new BigDecimal("1.500"));
        item.setPrecoUnitario(BigDecimal.valueOf(1000));
        item.setTaxaIva(BigDecimal.valueOf(14));
        item.setDesconto(new BigDecimal("7.5"));
        item.setLote("SERV-2024");
        item.setPesoUnitario(new BigDecimal("2.25"));
        orcamento.addItem(item);

        when(faturaRepository.findById(1L)).thenReturn(Optional.of(orcamento));
        when(faturaRepository.save(any(Fatura.class))).thenAnswer(i -> {
            Fatura f = i.getArgument(0);
            if (f.getId() == null) f.setId(2L);
            return f;
        });

        Fatura novaFatura = faturaService.converterProFormaEmFatura(1L);

        assertNotNull(novaFatura);
        assertEquals(StatusFatura.RASCUNHO, novaFatura.getStatus());
        assertEquals(TipoDocumento.FATURA, novaFatura.getTipoDocumento());
        assertEquals(1, novaFatura.getItens().size());
        ItemFatura linha = novaFatura.getItens().get(0);
        assertEquals("Serviço Dev", linha.getDescricao());
        assertEquals(0, new BigDecimal("1.500").compareTo(linha.getQuantidadeDecimal()));
        assertEquals(0, new BigDecimal("7.5").compareTo(linha.getDesconto()));
        assertEquals("SERV-2024", linha.getLote());
        assertEquals(0, new BigDecimal("2.25").compareTo(linha.getPesoUnitario()));
        assertEquals(orcamento, novaFatura.getFaturaReferencia());
        assertEquals(StatusFatura.PROCESSADA, orcamento.getStatus());
        assertTrue(novaFatura.getObservacoes().contains("Convertido de ORC 2024/1"));
    }

    @Test
    void naoDeveConverterEncomendaCanceladaEmFatura() {
        Fatura encomenda = new Fatura();
        encomenda.setId(3L);
        encomenda.setTipoDocumento(TipoDocumento.ENCOMENDA);
        encomenda.setStatus(StatusFatura.CANCELADA);
        when(faturaRepository.findById(3L)).thenReturn(Optional.of(encomenda));

        assertThrows(IllegalStateException.class,
                () -> faturaService.converterEncomendaEmFatura(3L));
        verify(faturaRepository, never()).save(any(Fatura.class));
    }
}

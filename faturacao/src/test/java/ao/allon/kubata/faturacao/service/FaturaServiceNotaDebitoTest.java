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
import ao.allon.kubata.faturacao.service.agt.AGTService;
import ao.allon.kubata.faturacao.service.agt.AGTElectronicInvoiceService;
import ao.allon.kubata.faturacao.service.agt.JWSDigitalSignatureService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class FaturaServiceNotaDebitoTest {

    private FaturaService faturaService;
    private FaturaRepository faturaRepository;
    private ItemFaturaRepository itemFaturaRepository;
    private SerieService serieService;
    private AGTService agtService;
    private AGTElectronicInvoiceService agtElectronicInvoiceService;
    private JWSDigitalSignatureService signatureService;
    private ProdutoService produtoService;
    private ContabilidadeService contabilidadeService;
    private PlanoContaRepository planoContaRepository;
    private SessionManager sessionManager;

    @BeforeEach
    void setup() {
        faturaRepository = mock(FaturaRepository.class);
        itemFaturaRepository = mock(ItemFaturaRepository.class);
        serieService = mock(SerieService.class);
        agtService = mock(AGTService.class);
        agtElectronicInvoiceService = mock(AGTElectronicInvoiceService.class);
        signatureService = mock(JWSDigitalSignatureService.class);
        produtoService = mock(ProdutoService.class);
        contabilidadeService = mock(ContabilidadeService.class);
        planoContaRepository = mock(PlanoContaRepository.class);
        sessionManager = mock(SessionManager.class);
        
        // Mock default async behavior for AGTElectronicInvoiceService
        when(agtElectronicInvoiceService.submitInvoiceAsync(any())).thenReturn(java.util.concurrent.CompletableFuture.completedFuture(new ao.allon.kubata.faturacao.service.agt.dto.AGTResponseDTO()));

        faturaService = new FaturaService(faturaRepository, itemFaturaRepository, produtoService, serieService, agtService, agtElectronicInvoiceService, signatureService, contabilidadeService, planoContaRepository, sessionManager);
    }

    @Test
    void emitirNotaDebito_DeveCriarDocumentoComItensAdicionais() {
        Fatura faturaOriginal = new Fatura();
        faturaOriginal.setId(1L);
        Cliente c = new Cliente();
        c.setNome("Cliente X");
        c.setNif("999999999");
        faturaOriginal.setCliente(c);
        faturaOriginal.setTipoDocumento(TipoDocumento.FATURA);
        faturaOriginal.setStatus(StatusFatura.EMITIDA);
        faturaOriginal.setDataEmissao(LocalDate.now());
        when(faturaRepository.findById(1L)).thenReturn(Optional.of(faturaOriginal));
        Serie serie = new Serie();
        serie.setTipoDocumento(TipoDocumento.NOTA_DEBITO);
        serie.setDesignacao(String.valueOf(LocalDate.now().getYear()));
        serie.setAno(LocalDate.now().getYear());
        serie.setAtiva(true);
        when(serieService.findPadrao(TipoDocumento.NOTA_DEBITO)).thenReturn(Optional.of(serie));
        when(serieService.getProximoNumero(serie)).thenReturn(1L);
        when(faturaRepository.save(ArgumentMatchers.any(Fatura.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ItemFatura it = new ItemFatura();
        it.setDescricao("Diferença de preço");
        it.setQuantidade(1);
        it.setPrecoUnitario(new BigDecimal("100.00"));
        it.setTaxaIva(new BigDecimal("14.00"));
        List<ItemFatura> itens = new ArrayList<>();
        itens.add(it);

        Fatura nd = faturaService.emitirNotaDebito(1L, "Acréscimo", itens);

        assertNotNull(nd);
        assertEquals(TipoDocumento.NOTA_DEBITO, nd.getTipoDocumento());
        assertEquals(faturaOriginal, nd.getFaturaReferencia());
        assertEquals(1, nd.getItens().size());
        assertEquals("Diferença de preço", nd.getItens().get(0).getDescricao());
    }

    @Test
    void naoDeveEmitirNotaDebitoSobreRascunho() {
        Fatura origem = new Fatura();
        origem.setId(2L);
        origem.setTipoDocumento(TipoDocumento.FATURA);
        origem.setStatus(StatusFatura.RASCUNHO);
        origem.setCliente(new Cliente());
        when(faturaRepository.findById(2L)).thenReturn(Optional.of(origem));

        ItemFatura item = new ItemFatura();
        item.setDescricao("Acréscimo");
        item.setQuantidade(1);
        item.setPrecoUnitario(new BigDecimal("10.00"));

        assertThrows(IllegalStateException.class,
                () -> faturaService.emitirNotaDebito(2L, "Ajuste", List.of(item)));
        verify(faturaRepository, never()).save(any(Fatura.class));
    }

    @Test
    void naoDeveEmitirNotaDebitoSemLinhas() {
        Fatura origem = new Fatura();
        origem.setId(3L);
        origem.setTipoDocumento(TipoDocumento.FATURA);
        origem.setStatus(StatusFatura.EMITIDA);
        origem.setCliente(new Cliente());
        when(faturaRepository.findById(3L)).thenReturn(Optional.of(origem));

        assertThrows(IllegalArgumentException.class,
                () -> faturaService.emitirNotaDebito(3L, "Ajuste", List.of()));
        verify(faturaRepository, never()).save(any(Fatura.class));
    }

    @Test
    void naoDeveEmitirNotaDebitoSemMotivo() {
        Fatura origem = new Fatura();
        origem.setId(4L);
        origem.setTipoDocumento(TipoDocumento.FATURA);
        origem.setStatus(StatusFatura.EMITIDA);
        origem.setCliente(new Cliente());
        when(faturaRepository.findById(4L)).thenReturn(Optional.of(origem));

        ItemFatura item = new ItemFatura();
        item.setDescricao("Acréscimo");
        item.setQuantidade(1);
        item.setPrecoUnitario(new BigDecimal("10.00"));

        assertThrows(IllegalArgumentException.class,
                () -> faturaService.emitirNotaDebito(4L, "  ", List.of(item)));
        verify(faturaRepository, never()).save(any(Fatura.class));
    }
}

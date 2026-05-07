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
import ao.allon.kubata.faturacao.service.agt.AGTElectronicInvoiceService;
import ao.allon.kubata.faturacao.service.agt.AGTService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GuiaTransporteModuleTest {

    @Mock
    private FaturaRepository faturaRepository;
    @Mock
    private ProdutoService produtoService;
    @Mock
    private SerieService serieService;
    @Mock
    private AGTService agtService;
    @Mock
    private AGTElectronicInvoiceService agtElectronicInvoiceService;
    @Mock
    private ContabilidadeService contabilidadeService;
    @Mock
    private PlanoContaRepository planoContaRepository;

    @InjectMocks
    private FaturaService faturaService;

    private Fatura guia;

    @BeforeEach
    void setUp() {
        lenient().when(agtElectronicInvoiceService.submitInvoiceAsync(any())).thenReturn(CompletableFuture.completedFuture(new ao.allon.kubata.faturacao.service.agt.dto.AGTResponseDTO()));
        guia = new Fatura();
        guia.setTipoDocumento(TipoDocumento.GUIA_TRANSPORTE);
        Cliente c = new Cliente();
        c.setId(10L);
        c.setNome("Cliente Transporte");
        c.setNif("5000000000");
        guia.setCliente(c);
        guia.setDataEmissao(LocalDate.now());
        guia.setDataVencimento(LocalDate.now());
        guia.setLocalCarga("Armazém Central");
        guia.setLocalDescarga("Cliente Final");
        guia.setMatriculaViatura("LD-00-00-AA");
        guia.setMotorista("Motorista XPTO");
        ItemFatura it = new ItemFatura();
        it.setDescricao("Mercadoria");
        it.setQuantidade(1);
        it.setPrecoUnitario(new BigDecimal("0.00"));
        it.setPercentualIva(BigDecimal.ZERO);
        guia.addItem(it);

        when(faturaRepository.save(any(Fatura.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void salvarEEmitir_GuiaTransporte_DeveGerarNumeroEStatusEmitida() throws Exception {
        Serie serie = new Serie("2026", TipoDocumento.GUIA_TRANSPORTE, LocalDate.now().getYear());
        serie.setId(2L);
        when(serieService.findPadrao(TipoDocumento.GUIA_TRANSPORTE)).thenReturn(Optional.of(serie));
        when(serieService.getProximoNumero(serie)).thenReturn(1L);
        when(agtService.generateDocumentHash(any(), any())).thenReturn("HASH1234567890");

        Fatura res = faturaService.salvarEEmitir(guia);

        assertEquals(StatusFatura.EMITIDA, res.getStatus());
        assertNotNull(res.getNumero());
        assertTrue(res.getNumero().contains(TipoDocumento.GUIA_TRANSPORTE.getCodigo()));
        verify(faturaRepository, atLeastOnce()).save(any(Fatura.class));
    }

    @Test
    void buscarGuiasTransporte_DeveDelegarAoRepositorio() {
        when(faturaRepository.searchByTipoAndFilters(eq(TipoDocumento.GUIA_TRANSPORTE), any(), any(), any(), any()))
                .thenReturn(List.of(guia));

        List<Fatura> res = faturaService.buscarGuiasTransporte(LocalDate.now().minusDays(7), LocalDate.now(), null, null);

        assertEquals(1, res.size());
        verify(faturaRepository, times(1))
                .searchByTipoAndFilters(eq(TipoDocumento.GUIA_TRANSPORTE), any(), any(), any(), any());
    }
}


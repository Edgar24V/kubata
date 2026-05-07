package ao.allon.kubata.faturacao.service;

import ao.allon.kubata.core.repository.PlanoContaRepository;
import ao.allon.kubata.core.service.ContabilidadeService;
import ao.allon.kubata.faturacao.domain.Fatura;
import ao.allon.kubata.faturacao.domain.ItemFatura;
import ao.allon.kubata.faturacao.domain.enums.StatusFatura;
import ao.allon.kubata.faturacao.repository.FaturaRepository;
import ao.allon.kubata.faturacao.service.agt.AGTService;
import ao.allon.kubata.faturacao.service.agt.AGTElectronicInvoiceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockitoAnnotations;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FaturaServiceTest {

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

    private Fatura fatura;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        fatura = new Fatura();
        fatura.setId(1L);
        fatura.setNumero("FT202600001");
        fatura.setDataEmissao(LocalDate.now());
        fatura.setDataVencimento(LocalDate.now().plusDays(30));
        fatura.setStatus(StatusFatura.RASCUNHO);
        ItemFatura item = new ItemFatura();
        item.setDescricao("Produto A");
        item.setQuantidade(2);
        item.setPrecoUnitario(new BigDecimal("1000"));
        item.setPercentualIva(new BigDecimal("14.00"));
        item.setProdutoId(10L);
        fatura.addItem(item);
        when(faturaRepository.findById(1L)).thenReturn(Optional.of(fatura));
        when(faturaRepository.save(any(Fatura.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void emitirFatura_DeveBaixarEstoqueESetarStatusEmitida() {
        Fatura res = faturaService.emitirFatura(1L);
        assertEquals(StatusFatura.EMITIDA, res.getStatus());
        verify(produtoService, times(1)).registarSaida(eq(10L), eq(2), anyString());
        verify(faturaRepository, times(1)).save(any(Fatura.class));
    }

    @Test
    void cancelarFatura_DeveDevolverEstoqueQuandoEmitida() {
        fatura.setStatus(StatusFatura.EMITIDA);
        faturaService.cancelarFatura(1L, "Cancelamento Teste");
        assertEquals(StatusFatura.CANCELADA, fatura.getStatus());
        verify(produtoService, times(1)).incrementarEstoque(eq(10L), eq(2));
        verify(faturaRepository, times(1)).save(any(Fatura.class));
    }

    @Test
    void registrarRecebimento_DeveMarcarComoPagaQuandoIgualOuMaiorQueTotal() {
        fatura.recalculateTotals();
        Fatura res = faturaService.registrarRecebimento(1L, fatura.getTotal(), "REF123");
        assertEquals(StatusFatura.PAGA, res.getStatus());
        assertTrue(res.getObservacoes().contains("Recebimento"));
    }

    @Test
    void emitirNotaCredito_DeveDevolverEstoqueEAdicionarObservacao() {
        fatura.setStatus(StatusFatura.EMITIDA);
        Fatura res = faturaService.emitirNotaCredito(1L, "Devolução parcial");
        assertTrue(res.getObservacoes().contains("Nota de Crédito"));
        verify(produtoService, times(1)).registarEntrada(eq(10L), eq(2), anyString());
    }
}

package ao.allon.kubata.faturacao.service;

import ao.allon.kubata.core.domain.PlanoConta;
import ao.allon.kubata.core.repository.PlanoContaRepository;
import ao.allon.kubata.core.service.ContabilidadeService;
import ao.allon.kubata.faturacao.domain.Fatura;
import ao.allon.kubata.faturacao.domain.Recibo;
import ao.allon.kubata.faturacao.domain.enums.MetodoPagamento;
import ao.allon.kubata.faturacao.domain.enums.StatusFatura;
import ao.allon.kubata.faturacao.domain.enums.TipoDocumento;
import ao.allon.kubata.faturacao.repository.FaturaRepository;
import ao.allon.kubata.faturacao.repository.ReciboRepository;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReciboServiceTest {

    @Mock
    private ReciboRepository reciboRepository;

    @Mock
    private FaturaRepository faturaRepository;

    @Mock
    private ContabilidadeService contabilidadeService;

    @Mock
    private PlanoContaRepository planoContaRepository;

    @InjectMocks
    private ReciboService reciboService;

    private Fatura fatura;

    @BeforeEach
    void setUp() {
        fatura = new Fatura();
        fatura.setId(1L);
        fatura.setNumero("FT 2026/001");
        fatura.setTipoDocumento(TipoDocumento.FATURA);
        fatura.setStatus(StatusFatura.EMITIDA);
        fatura.setTotal(new BigDecimal("100.00"));

        lenient().when(faturaRepository.findById(1L)).thenReturn(Optional.of(fatura));
        lenient().when(reciboRepository.sumValorByFaturaId(1L)).thenReturn(new BigDecimal("0.00"));
        lenient().when(reciboRepository.count()).thenReturn(0L);
        lenient().when(reciboRepository.saveAndFlush(any(Recibo.class))).thenAnswer(invocation -> {
            Recibo recibo = invocation.getArgument(0);
            if (recibo.getId() == null) recibo.setId(1L);
            return recibo;
        });
        lenient().when(reciboRepository.save(any(Recibo.class))).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(planoContaRepository.findByCodigo("45.1"))
                .thenReturn(Optional.of(PlanoConta.builder().codigo("45.1").descricao("Caixa").movimento(true).build()));
        lenient().when(planoContaRepository.findByCodigo("31.1"))
                .thenReturn(Optional.of(PlanoConta.builder().codigo("31.1").descricao("Clientes").movimento(true).build()));
    }

    @Test
    void registarPagamentoParcialMantemFacturaEmAbertoEGravaMetodo() {
        Recibo recibo = reciboService.registrar(
                1L, new BigDecimal("40.00"), "TRF-001", "Adiantamento",
                MetodoPagamento.TRANSFERENCIA_BANCARIA);

        assertEquals(new BigDecimal("40.00"), recibo.getValor());
        assertEquals("RC" + LocalDate.now().getYear() + "000001", recibo.getNumero());
        assertEquals(MetodoPagamento.TRANSFERENCIA_BANCARIA, recibo.getMetodoPagamento());
        verify(reciboRepository).saveAndFlush(any(Recibo.class));
        assertEquals(new BigDecimal("40.00"), fatura.getValorRecebido());
        assertEquals(StatusFatura.EMITIDA, fatura.getStatus());
        verify(reciboRepository).save(any(Recibo.class));
        verify(contabilidadeService).lancar(
                any(LocalDate.class), any(PlanoConta.class), any(PlanoConta.class), any(BigDecimal.class),
                anyString(), anyString(), anyString(), any());
    }

    @Test
    void registarPagamentoDoSaldoMarcaFacturaComoPaga() {
        when(reciboRepository.sumValorByFaturaId(1L)).thenReturn(new BigDecimal("60.00"));

        reciboService.registrar(1L, new BigDecimal("40.00"), "TRF-002", "Liquidação");

        assertEquals(StatusFatura.PAGA, fatura.getStatus());
        assertEquals(new BigDecimal("100.00"), fatura.getValorRecebido());
        verify(faturaRepository).save(fatura);
    }

    @Test
    void rejeitaValorSuperiorAoSaldoSemGravarRecibo() {
        when(reciboRepository.sumValorByFaturaId(1L)).thenReturn(new BigDecimal("80.00"));

        assertThrows(IllegalArgumentException.class,
                () -> reciboService.registrar(1L, new BigDecimal("30.00"), "TRF-003", null));

        verify(reciboRepository, never()).save(any(Recibo.class));
        verifyNoInteractions(contabilidadeService);
    }

    @Test
    void rejeitaRecebimentoDeRascunho() {
        fatura.setStatus(StatusFatura.RASCUNHO);

        assertThrows(IllegalStateException.class,
                () -> reciboService.registrar(1L, new BigDecimal("10.00"), "TRF-004", null));

        verify(reciboRepository, never()).save(any(Recibo.class));
    }

    @Test
    void falhaContabilisticaNaoEEngolida() {
        doThrow(new IllegalStateException("falha do diário")).when(contabilidadeService).lancar(
                any(LocalDate.class), any(PlanoConta.class), any(PlanoConta.class), any(BigDecimal.class),
                anyString(), anyString(), anyString(), any());

        assertThrows(IllegalStateException.class,
                () -> reciboService.registrar(1L, new BigDecimal("10.00"), "TRF-005", null));

        verify(contabilidadeService).lancar(
                any(LocalDate.class), any(PlanoConta.class), any(PlanoConta.class), any(BigDecimal.class),
                anyString(), anyString(), anyString(), any());
    }

    @Test
    void exigeContasDeCaixaEClientesQuandoContabilidadeEObrigatoria() {
        when(planoContaRepository.findByCodigo("45.1")).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class,
                () -> reciboService.registrar(1L, new BigDecimal("10.00"), "TRF-006", null));
    }

    @Test
    void findByPeriodoDeveRetornarLista() {
        when(reciboRepository.findByDataRecebimentoBetween(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(new Recibo(), new Recibo()));

        List<Recibo> recibos = reciboService.findByPeriodo(LocalDate.now().minusDays(7), LocalDate.now());

        assertEquals(2, recibos.size());
    }
}

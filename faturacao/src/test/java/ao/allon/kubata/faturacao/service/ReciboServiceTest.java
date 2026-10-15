package ao.allon.kubata.faturacao.service;

import ao.allon.kubata.core.repository.PlanoContaRepository;
import ao.allon.kubata.core.service.ContabilidadeService;
import ao.allon.kubata.faturacao.domain.Fatura;
import ao.allon.kubata.faturacao.domain.Recibo;
import ao.allon.kubata.faturacao.domain.enums.StatusFatura;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReciboServiceTest {

    @Mock private ReciboRepository reciboRepository;
    @Mock private FaturaRepository faturaRepository;
    @Mock private ContabilidadeService contabilidadeService;
    @Mock private PlanoContaRepository planoContaRepository;

    @InjectMocks private ReciboService reciboService;

    private Fatura fatura;

    @BeforeEach
    void setUp() {
        fatura = new Fatura();
        fatura.setId(501L);
        fatura.setNumero("FT 2026/1");
        fatura.setStatus(StatusFatura.EMITIDA);
        fatura.setTotal(new BigDecimal("100.00"));

        when(faturaRepository.findByIdForUpdate(501L)).thenReturn(Optional.of(fatura));
        when(faturaRepository.save(any(Fatura.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(reciboRepository.save(any(Recibo.class))).thenAnswer(invocation -> {
            Recibo recibo = invocation.getArgument(0);
            if (recibo.getId() == null) recibo.setId(880L);
            return recibo;
        });
        when(planoContaRepository.findByCodigo(anyString())).thenReturn(Optional.empty());
    }

    @Test
    void deveRegistarReciboParcialSemMarcarFacturaComoPaga() {
        when(reciboRepository.sumValorByFaturaId(501L)).thenReturn(BigDecimal.ZERO);

        Recibo recibo = reciboService.registrar(501L, new BigDecimal("30.00"), "TALAO-01", "Adiantamento");

        assertEquals(StatusFatura.EMITIDA, fatura.getStatus());
        assertEquals(new BigDecimal("30.00"), recibo.getValor());
        assertEquals("TALAO-01", recibo.getReferencia());
        assertTrue(recibo.getNumero().startsWith("RC"));
        verify(faturaRepository).save(fatura);
        verify(reciboRepository, times(2)).save(any(Recibo.class));
    }

    @Test
    void deveMarcarFacturaComoPagaQuandoSaldoFicaExactamenteLiquidado() {
        when(reciboRepository.sumValorByFaturaId(501L)).thenReturn(new BigDecimal("40.00"));

        Recibo recibo = reciboService.registrar(501L, new BigDecimal("60.00"), "TRANSFERENCIA-02", null);

        assertEquals(StatusFatura.PAGA, fatura.getStatus());
        assertNotNull(recibo.getNumero());
        verify(faturaRepository).save(fatura);
    }

    @Test
    void deveRejeitarValorSuperiorAoSaldoEmAberto() {
        when(reciboRepository.sumValorByFaturaId(501L)).thenReturn(new BigDecimal("90.00"));

        assertThrows(IllegalArgumentException.class,
                () -> reciboService.registrar(501L, new BigDecimal("11.00"), "REF-03", null));

        verify(reciboRepository, never()).save(any(Recibo.class));
        assertEquals(StatusFatura.EMITIDA, fatura.getStatus());
    }

    @Test
    void deveRejeitarRecebimentoDeDocumentoQueNaoEstaEmitido() {
        fatura.setStatus(StatusFatura.RASCUNHO);

        assertThrows(IllegalStateException.class,
                () -> reciboService.registrar(501L, new BigDecimal("10.00"), "REF-04", null));

        verify(reciboRepository, never()).save(any(Recibo.class));
    }

    @Test
    void findByPeriodo_DeveRetornarLista() {
        when(reciboRepository.findByDataRecebimentoBetween(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(new Recibo(), new Recibo()));
        List<Recibo> recibos = reciboService.findByPeriodo(LocalDate.now().minusDays(7), LocalDate.now());
        assertEquals(2, recibos.size());
    }
}

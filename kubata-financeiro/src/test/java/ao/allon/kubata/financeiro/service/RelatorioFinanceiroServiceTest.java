package ao.allon.kubata.financeiro.service;

import ao.allon.kubata.financeiro.domain.Caixa;
import ao.allon.kubata.financeiro.domain.ContaBancaria;
import ao.allon.kubata.financeiro.domain.MovimentoBancario;
import ao.allon.kubata.financeiro.domain.MovimentoCaixa;
import ao.allon.kubata.financeiro.enums.TipoMovimentoBancario;
import ao.allon.kubata.financeiro.enums.TipoMovimentoCaixa;
import ao.allon.kubata.financeiro.service.RelatorioFinanceiroService.Relatorio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RelatorioFinanceiroServiceTest {
    @Mock private CaixaService caixaService;
    @Mock private MovimentoCaixaService movimentoCaixaService;
    @Mock private ContaBancariaService contaBancariaService;

    @InjectMocks private RelatorioFinanceiroService service;

    @Test
    void deveConsolidarEntradasSaidasECaixasEBancosNoPeriodo() {
        LocalDate inicio = LocalDate.of(2026, 10, 1);
        LocalDate fim = LocalDate.of(2026, 10, 31);

        Caixa caixa = new Caixa();
        caixa.setId(1L);
        caixa.setNome("Caixa principal");
        caixa.setResponsavel("Tesouraria");
        caixa.setIsAtivo(true);

        ContaBancaria conta = new ContaBancaria();
        conta.setId(2L);
        conta.setBanco("BAI");
        conta.setNumeroConta("0001");
        conta.setIsAtivo(true);

        when(caixaService.findAllAtivos()).thenReturn(List.of(caixa));
        when(contaBancariaService.findAllAtivas()).thenReturn(List.of(conta));

        MovimentoCaixa entradaCaixa = movimentoCaixa(
                TipoMovimentoCaixa.ENTRADA, "100.00", LocalDateTime.of(2026, 10, 10, 9, 30));
        MovimentoCaixa saidaCaixa = movimentoCaixa(
                TipoMovimentoCaixa.SAIDA, "20.00", LocalDateTime.of(2026, 10, 12, 11, 0));
        when(movimentoCaixaService.extrato(eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(saidaCaixa, entradaCaixa));

        MovimentoBancario creditoBanco = movimentoBanco(
                TipoMovimentoBancario.CREDITO, "50.00", LocalDateTime.of(2026, 10, 15, 10, 0));
        MovimentoBancario debitoBanco = movimentoBanco(
                TipoMovimentoBancario.DEBITO, "30.00", LocalDateTime.of(2026, 10, 20, 14, 45));
        when(contaBancariaService.getExtrato(eq(2L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(debitoBanco, creditoBanco));

        Relatorio relatorio = service.gerar(inicio, fim);

        assertEquals(new BigDecimal("150.00"), relatorio.totalEntradas());
        assertEquals(new BigDecimal("50.00"), relatorio.totalSaidas());
        assertEquals(new BigDecimal("100.00"), relatorio.saldoLiquido());
        assertEquals(4, relatorio.movimentos().size());
        assertEquals("Débito", relatorio.movimentos().get(0).natureza());
        assertEquals(LocalDateTime.of(2026, 10, 20, 14, 45), relatorio.movimentos().get(0).data());

        verify(movimentoCaixaService).extrato(
                eq(1L), eq(inicio.atStartOfDay()), eq(fim.plusDays(1).atStartOfDay().minusNanos(1)));
        verify(contaBancariaService).getExtrato(
                eq(2L), eq(inicio.atStartOfDay()), eq(fim.plusDays(1).atStartOfDay().minusNanos(1)));
    }

    @Test
    void deveRejeitarIntervaloInvalidoAntesDeConsultarDados() {
        assertThrows(IllegalArgumentException.class,
                () -> service.gerar(LocalDate.of(2026, 10, 2), LocalDate.of(2026, 10, 1)));
        verifyNoInteractions(caixaService, movimentoCaixaService, contaBancariaService);
    }

    @Test
    void deveExigirAmbasAsDatas() {
        assertThrows(IllegalArgumentException.class, () -> service.gerar(null, LocalDate.now()));
        assertThrows(IllegalArgumentException.class, () -> service.gerar(LocalDate.now(), null));
        verifyNoInteractions(caixaService, movimentoCaixaService, contaBancariaService);
    }

    private MovimentoCaixa movimentoCaixa(TipoMovimentoCaixa tipo, String valor, LocalDateTime data) {
        MovimentoCaixa movimento = new MovimentoCaixa();
        movimento.setTipo(tipo);
        movimento.setValor(new BigDecimal(valor));
        movimento.setDataMovimento(data);
        movimento.setDescricao("Movimento de teste");
        movimento.setDocumentoRef("DOC-CX");
        movimento.setSaldoAtual(new BigDecimal("200.00"));
        return movimento;
    }

    private MovimentoBancario movimentoBanco(TipoMovimentoBancario tipo, String valor, LocalDateTime data) {
        MovimentoBancario movimento = new MovimentoBancario();
        movimento.setTipo(tipo);
        movimento.setValor(new BigDecimal(valor));
        movimento.setDataMovimento(data);
        movimento.setDescricao("Movimento de teste");
        movimento.setReferenciaDocumento("DOC-BANCO");
        movimento.setSaldoAtual(new BigDecimal("200.00"));
        return movimento;
    }
}

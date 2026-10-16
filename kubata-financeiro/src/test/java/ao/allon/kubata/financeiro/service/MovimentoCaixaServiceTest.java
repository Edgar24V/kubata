package ao.allon.kubata.financeiro.service;

import ao.allon.kubata.financeiro.domain.Caixa;
import ao.allon.kubata.financeiro.domain.MovimentoCaixa;
import ao.allon.kubata.financeiro.repository.CaixaRepository;
import ao.allon.kubata.financeiro.repository.MovimentoCaixaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MovimentoCaixaServiceTest {

    @Mock
    private CaixaRepository caixaRepository;

    @Mock
    private MovimentoCaixaRepository movimentoRepository;

    @InjectMocks
    private MovimentoCaixaService service;

    @Test
    void registarEntradaDeveActualizarSaldoEGuardarLivro() {
        Caixa caixa = caixa(1L, "Caixa principal", "Operador", "100.00");
        when(caixaRepository.findById(1L)).thenReturn(Optional.of(caixa));
        when(caixaRepository.save(any(Caixa.class))).thenAnswer(call -> call.getArgument(0));
        when(movimentoRepository.save(any(MovimentoCaixa.class))).thenAnswer(call -> call.getArgument(0));

        MovimentoCaixa movimento = service.registarEntrada(
                1L, new BigDecimal("25.50"), "Reposição de caixa", "DOC-001");

        assertEquals(0, caixa.getSaldo().compareTo(new BigDecimal("125.50")));
        assertEquals(0, movimento.getSaldoAnterior().compareTo(new BigDecimal("100.00")));
        assertEquals(0, movimento.getSaldoAtual().compareTo(new BigDecimal("125.50")));
        assertEquals("DOC-001", movimento.getDocumentoRef());
        verify(caixaRepository).save(caixa);
        verify(movimentoRepository).save(movimento);
    }

    @Test
    void registarSaidaDeveBloquearSaldoInsuficienteSemGravar() {
        Caixa caixa = caixa(2L, "Caixa secundário", "Operador", "10.00");
        when(caixaRepository.findById(2L)).thenReturn(Optional.of(caixa));

        assertThrows(IllegalStateException.class, () -> service.registarSaida(
                2L, new BigDecimal("10.01"), "Pagamento", "PG-001"));

        assertEquals(0, caixa.getSaldo().compareTo(new BigDecimal("10.00")));
        verify(caixaRepository, never()).save(any(Caixa.class));
        verify(movimentoRepository, never()).save(any(MovimentoCaixa.class));
    }

    @Test
    void aberturaDeveSerOPrimeiroMovimentoDoCaixa() {
        Caixa caixa = caixa(3L, "Caixa novo", "Operador", "0.00");
        when(caixaRepository.findById(3L)).thenReturn(Optional.of(caixa));
        when(movimentoRepository.existsByCaixa_Id(3L)).thenReturn(false);
        when(caixaRepository.save(any(Caixa.class))).thenAnswer(call -> call.getArgument(0));
        when(movimentoRepository.save(any(MovimentoCaixa.class))).thenAnswer(call -> call.getArgument(0));

        MovimentoCaixa movimento = service.abrirCaixa(
                3L, new BigDecimal("500.00"), "Abertura do turno");

        assertEquals("ABERTURA", movimento.getTipo().name());
        assertEquals(0, caixa.getSaldo().compareTo(new BigDecimal("500.00")));
        verify(movimentoRepository).save(movimento);
    }

    private Caixa caixa(Long id, String nome, String responsavel, String saldo) {
        Caixa caixa = new Caixa();
        caixa.setId(id);
        caixa.setNome(nome);
        caixa.setResponsavel(responsavel);
        caixa.setSaldo(new BigDecimal(saldo));
        caixa.setIsAtivo(Boolean.TRUE);
        return caixa;
    }
}

package ao.allon.kubata.financeiro.service;

import ao.allon.kubata.financeiro.domain.ContaBancaria;
import ao.allon.kubata.financeiro.domain.MovimentoBancario;
import ao.allon.kubata.financeiro.enums.TipoMovimentoBancario;
import ao.allon.kubata.financeiro.repository.ContaBancariaRepository;
import ao.allon.kubata.financeiro.repository.MovimentoBancarioRepository;
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
class ContaBancariaServiceTest {

    @Mock
    private ContaBancariaRepository contaRepository;

    @Mock
    private MovimentoBancarioRepository movimentoRepository;

    @InjectMocks
    private ContaBancariaService service;

    @Test
    void transferirDeveActualizarOsDoisSaldosENaoPerderMovimentos() {
        ContaBancaria origem = conta(1L, "BAI", "001", "500.00");
        ContaBancaria destino = conta(2L, "BFA", "002", "100.00");
        when(contaRepository.findById(1L)).thenReturn(Optional.of(origem));
        when(contaRepository.findById(2L)).thenReturn(Optional.of(destino));
        when(contaRepository.save(any(ContaBancaria.class))).thenAnswer(call -> call.getArgument(0));
        when(movimentoRepository.save(any(MovimentoBancario.class))).thenAnswer(call -> call.getArgument(0));

        service.transferir(1L, 2L, new BigDecimal("125.00"), "Reforço de tesouraria");

        assertEquals(0, origem.getSaldo().compareTo(new BigDecimal("375.00")));
        assertEquals(0, destino.getSaldo().compareTo(new BigDecimal("225.00")));

        ArgumentCaptor<MovimentoBancario> captor = ArgumentCaptor.forClass(MovimentoBancario.class);
        verify(movimentoRepository, times(2)).save(captor.capture());
        assertEquals(TipoMovimentoBancario.DEBITO, captor.getAllValues().get(0).getTipo());
        assertEquals(TipoMovimentoBancario.CREDITO, captor.getAllValues().get(1).getTipo());
        assertEquals("TRANSFERENCIA", captor.getAllValues().get(0).getCategoria());
        assertEquals(0, captor.getAllValues().get(0).getSaldoAtual().compareTo(new BigDecimal("375.00")));
        assertEquals(0, captor.getAllValues().get(1).getSaldoAtual().compareTo(new BigDecimal("225.00")));
    }

    @Test
    void transferirDeveBloquearQuandoSaldoDeOrigemForInsuficiente() {
        ContaBancaria origem = conta(1L, "BAI", "001", "50.00");
        ContaBancaria destino = conta(2L, "BFA", "002", "100.00");
        when(contaRepository.findById(1L)).thenReturn(Optional.of(origem));
        when(contaRepository.findById(2L)).thenReturn(Optional.of(destino));

        assertThrows(IllegalStateException.class, () -> service.transferir(
                1L, 2L, new BigDecimal("50.01"), "Transferência"));

        assertEquals(0, origem.getSaldo().compareTo(new BigDecimal("50.00")));
        assertEquals(0, destino.getSaldo().compareTo(new BigDecimal("100.00")));
        verify(contaRepository, never()).save(any(ContaBancaria.class));
        verify(movimentoRepository, never()).save(any(MovimentoBancario.class));
    }

    @Test
    void transferirDeveRejeitarMesmaConta() {
        assertThrows(IllegalArgumentException.class, () -> service.transferir(
                1L, 1L, new BigDecimal("1.00"), "Teste"));
        verifyNoInteractions(contaRepository, movimentoRepository);
    }

    private ContaBancaria conta(Long id, String banco, String numero, String saldo) {
        ContaBancaria conta = new ContaBancaria();
        conta.setId(id);
        conta.setBanco(banco);
        conta.setNumeroConta(numero);
        conta.setTitular("Empresa de teste");
        conta.setIban("AO06TEST" + numero);
        conta.setSaldo(new BigDecimal(saldo));
        conta.setIsAtivo(Boolean.TRUE);
        return conta;
    }
}

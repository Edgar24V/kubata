package ao.allon.kubata.faturacao.service;

import ao.allon.kubata.core.repository.PlanoContaRepository;
import ao.allon.kubata.core.service.ContabilidadeService;
import ao.allon.kubata.faturacao.domain.Fatura;
import ao.allon.kubata.faturacao.domain.Recibo;
import ao.allon.kubata.faturacao.repository.FaturaRepository;
import ao.allon.kubata.faturacao.repository.ReciboRepository;
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

    @Test
    void registrar_DeveCriarRecibo_QuandoDadosValidos() {
        Fatura f = new Fatura();
        f.setId(1L);
        when(faturaRepository.findById(1L)).thenReturn(Optional.of(f));
        when(reciboRepository.save(any(Recibo.class))).thenAnswer(inv -> inv.getArgument(0));

        Recibo r = reciboService.registrar(1L, new BigDecimal("1500.00"), "REF123", "Pagamento parcial");
        assertNotNull(r.getNumero());
        assertEquals(new BigDecimal("1500.00"), r.getValor());
        assertEquals("REF123", r.getReferencia());
        verify(reciboRepository, times(1)).save(any(Recibo.class));
    }

    @Test
    void findByPeriodo_DeveRetornarLista() {
        when(reciboRepository.findByDataRecebimentoBetween(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(new Recibo(), new Recibo()));
        List<Recibo> rs = reciboService.findByPeriodo(LocalDate.now().minusDays(7), LocalDate.now());
        assertEquals(2, rs.size());
    }
}

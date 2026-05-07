package ao.allon.kubata.faturacao.service;

import ao.allon.kubata.faturacao.domain.Despesa;
import ao.allon.kubata.faturacao.domain.Fornecedor;
import ao.allon.kubata.core.domain.PlanoConta;
import ao.allon.kubata.core.repository.PlanoContaRepository;
import ao.allon.kubata.core.service.ContabilidadeService;
import ao.allon.kubata.faturacao.domain.StatusDespesa;
import ao.allon.kubata.faturacao.repository.DespesaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DespesaServiceTest {

    @Mock
    private DespesaRepository despesaRepository;
    @Mock
    private ContaBancariaService contaBancariaService;
    @Mock
    private ContabilidadeService contabilidadeService;
    @Mock
    private PlanoContaRepository planoContaRepository;

    @InjectMocks
    private DespesaService despesaService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void registrarPagamento_DeveLancarNaContabilidade() {
        // Dados
        Long despesaId = 1L;
        BigDecimal valor = new BigDecimal("100.00");
        Despesa despesa = new Despesa();
        despesa.setId(despesaId);
        despesa.setValor(new BigDecimal("100.00"));
        despesa.setStatus(StatusDespesa.ABERTA);
        despesa.setDescricao("Teste Despesa");
        
        Fornecedor fornecedor = new Fornecedor();
        fornecedor.setNome("Fornecedor Teste");
        despesa.setFornecedor(fornecedor);

        PlanoConta contaFornecedor = new PlanoConta();
        contaFornecedor.setCodigo("32.1");
        PlanoConta contaCaixa = new PlanoConta();
        contaCaixa.setCodigo("45.1");

        // Mocks
        when(despesaRepository.findById(despesaId)).thenReturn(Optional.of(despesa));
        when(despesaRepository.save(any(Despesa.class))).thenAnswer(i -> i.getArguments()[0]);
        when(planoContaRepository.findByCodigo("32.1")).thenReturn(Optional.of(contaFornecedor));
        when(planoContaRepository.findByCodigo("45.1")).thenReturn(Optional.of(contaCaixa));

        // Ação
        Despesa resultado = despesaService.registrarPagamento(despesaId, valor, "REF123", "Obs", 10L);

        // Verificação
        assertEquals(StatusDespesa.PAGA, resultado.getStatus());
        verify(contaBancariaService).registrarDebito(eq(10L), eq(valor), anyString(), anyString(), anyString());
        verify(contabilidadeService).lancar(
            any(LocalDate.class),
            eq(contaFornecedor),
            eq(contaCaixa),
            eq(valor),
            contains("Pagamento Despesa"),
            any(),
            any(),
            any()
        );
    }
}

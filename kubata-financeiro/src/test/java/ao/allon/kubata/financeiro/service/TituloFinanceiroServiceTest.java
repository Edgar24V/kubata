package ao.allon.kubata.financeiro.service;

import ao.allon.kubata.financeiro.domain.Caixa;
import ao.allon.kubata.financeiro.domain.LiquidacaoTitulo;
import ao.allon.kubata.financeiro.domain.TituloFinanceiro;
import ao.allon.kubata.financeiro.enums.EstadoTituloFinanceiro;
import ao.allon.kubata.financeiro.enums.MeioPagamentoFinanceiro;
import ao.allon.kubata.financeiro.enums.TipoTituloFinanceiro;
import ao.allon.kubata.financeiro.repository.CaixaRepository;
import ao.allon.kubata.financeiro.repository.ContaBancariaRepository;
import ao.allon.kubata.financeiro.repository.LiquidacaoTituloRepository;
import ao.allon.kubata.financeiro.repository.TituloFinanceiroRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class TituloFinanceiroServiceTest {
    @Mock private TituloFinanceiroRepository tituloRepository;
    @Mock private LiquidacaoTituloRepository liquidacaoRepository;
    @Mock private CaixaRepository caixaRepository;
    @Mock private ContaBancariaRepository contaRepository;
    @Mock private MovimentoCaixaService movimentoCaixaService;
    @Mock private ContaBancariaService contaBancariaService;

    @InjectMocks private TituloFinanceiroService service;

    private TituloFinanceiro pagar;
    private Caixa caixa;

    @BeforeEach
    void setUp() {
        pagar = new TituloFinanceiro();
        pagar.setId(10L);
        pagar.setTipo(TipoTituloFinanceiro.PAGAR);
        pagar.setCodigo("PAG-2026-001");
        pagar.setEntidade("Fornecedor de teste");
        pagar.setDescricao("Serviços de manutenção");
        pagar.setDataEmissao(LocalDate.now());
        pagar.setDataVencimento(LocalDate.now().plusDays(15));
        pagar.setValorOriginal(new BigDecimal("100.00"));
        pagar.setValorLiquidado(BigDecimal.ZERO.setScale(2));
        pagar.setEstado(EstadoTituloFinanceiro.ABERTO);

        caixa = new Caixa();
        caixa.setId(5L);
        caixa.setNome("Caixa principal");
        caixa.setResponsavel("Tesouraria");
        caixa.setSaldo(new BigDecimal("500.00"));
        caixa.setIsAtivo(true);
    }

    @Test
    void salvarDeveValidarEInicializarTituloEmAberto() {
        TituloFinanceiro novo = new TituloFinanceiro();
        novo.setTipo(TipoTituloFinanceiro.RECEBER);
        novo.setCodigo(" rec-2026-002 ");
        novo.setEntidade("Cliente A");
        novo.setDescricao("Factura de serviços");
        novo.setDataEmissao(LocalDate.now());
        novo.setDataVencimento(LocalDate.now().plusDays(20));
        novo.setValorOriginal(new BigDecimal("250.00"));
        when(tituloRepository.existsByCodigoIgnoreCase("REC-2026-002")).thenReturn(false);
        when(tituloRepository.save(any(TituloFinanceiro.class))).thenAnswer(call -> call.getArgument(0));

        TituloFinanceiro saved = service.salvar(novo);

        assertEquals("REC-2026-002", saved.getCodigo());
        assertEquals(EstadoTituloFinanceiro.ABERTO, saved.getEstado());
        assertEquals(0, saved.getValorLiquidado().compareTo(BigDecimal.ZERO.setScale(2)));
        assertEquals(0, saved.getSaldoPendente().compareTo(new BigDecimal("250.00")));
    }

    @Test
    void liquidarParcialmenteDeveCriarMovimentoEActualizarEstado() {
        when(tituloRepository.findById(10L)).thenReturn(Optional.of(pagar));
        when(caixaRepository.findById(5L)).thenReturn(Optional.of(caixa));
        when(movimentoCaixaService.registarSaida(eq(5L), any(BigDecimal.class), anyString(), any()))
                .thenReturn(null);
        when(tituloRepository.save(any(TituloFinanceiro.class))).thenAnswer(call -> call.getArgument(0));
        when(liquidacaoRepository.save(any(LiquidacaoTitulo.class))).thenAnswer(call -> call.getArgument(0));

        LiquidacaoTitulo result = service.liquidar(
                10L, new BigDecimal("35.00"), MeioPagamentoFinanceiro.CAIXA, 5L, "REC-35");

        assertEquals(new BigDecimal("35.00"), result.getValor());
        assertEquals(MeioPagamentoFinanceiro.CAIXA, result.getMeioPagamento());
        assertEquals(caixa, result.getCaixa());
        assertEquals(EstadoTituloFinanceiro.PARCIAL, pagar.getEstado());
        assertEquals(0, pagar.getSaldoPendente().compareTo(new BigDecimal("65.00")));
        verify(movimentoCaixaService).registarSaida(eq(5L), eq(new BigDecimal("35.00")), contains("PAG-2026-001"), eq("REC-35"));
        verify(liquidacaoRepository).save(result);
    }

    @Test
    void liquidarAcimaDoSaldoPendenteDeveFalharSemCriarMovimento() {
        when(tituloRepository.findById(10L)).thenReturn(Optional.of(pagar));

        assertThrows(IllegalArgumentException.class, () -> service.liquidar(
                10L, new BigDecimal("100.01"), MeioPagamentoFinanceiro.CAIXA, 5L, null));

        verifyNoInteractions(caixaRepository, contaRepository, movimentoCaixaService, contaBancariaService);
        verify(tituloRepository, never()).save(any(TituloFinanceiro.class));
        verify(liquidacaoRepository, never()).save(any(LiquidacaoTitulo.class));
    }

    @Test
    void liquidacaoTotalDeveMarcarTituloComoLiquidado() {
        when(tituloRepository.findById(10L)).thenReturn(Optional.of(pagar));
        when(caixaRepository.findById(5L)).thenReturn(Optional.of(caixa));
        when(movimentoCaixaService.registarSaida(eq(5L), any(BigDecimal.class), anyString(), any()))
                .thenReturn(null);
        when(tituloRepository.save(any(TituloFinanceiro.class))).thenAnswer(call -> call.getArgument(0));
        when(liquidacaoRepository.save(any(LiquidacaoTitulo.class))).thenAnswer(call -> call.getArgument(0));

        service.liquidar(10L, new BigDecimal("100.00"), MeioPagamentoFinanceiro.CAIXA, 5L, "PG-100");

        assertEquals(EstadoTituloFinanceiro.LIQUIDADO, pagar.getEstado());
        assertEquals(0, pagar.getSaldoPendente().signum());
    }
}

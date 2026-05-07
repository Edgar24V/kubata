package ao.allon.kubata.faturacao.service;

import ao.allon.kubata.faturacao.domain.Cliente;
import ao.allon.kubata.faturacao.domain.Devolucao;
import ao.allon.kubata.faturacao.domain.Fatura;
import ao.allon.kubata.faturacao.domain.ItemDevolucao;
import ao.allon.kubata.faturacao.domain.Produto;
import ao.allon.kubata.faturacao.domain.enums.StatusDevolucao;
import ao.allon.kubata.faturacao.domain.enums.StatusFatura;
import ao.allon.kubata.faturacao.repository.DevolucaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DevolucaoServiceTest {

    @Mock
    private DevolucaoRepository devolucaoRepository;
    @Mock
    private FaturaService faturaService;
    @Mock
    private ProdutoService produtoService;
    @Mock
    private SessionManager sessionManager;

    @InjectMocks
    private DevolucaoService devolucaoService;

    private Devolucao devolucao;
    private Fatura faturaOrigem;
    private Cliente cliente;

    @BeforeEach
    void setUp() {
        cliente = new Cliente();
        cliente.setId(1L);
        cliente.setNome("Cliente Teste");

        faturaOrigem = new Fatura();
        faturaOrigem.setId(10L);
        faturaOrigem.setNumero("FT2026/1");
        faturaOrigem.setStatus(StatusFatura.EMITIDA);

        devolucao = new Devolucao();
        devolucao.setId(1L);
        devolucao.setCliente(cliente);
        devolucao.setFaturaOrigem(faturaOrigem);
        devolucao.setStatus(StatusDevolucao.PENDENTE);
        
        ItemDevolucao item = new ItemDevolucao();
        item.setProduto(new Produto());
        item.setQuantidade(1);
        item.setValorUnitario(BigDecimal.TEN);
        item.calculateTotal();
        devolucao.addItem(item);

        when(sessionManager.getCurrentUser()).thenReturn("admin");
    }

    @Test
    void criarSolicitacao_DeveGerarNumeroEDefinirStatusPendente() {
        when(devolucaoRepository.save(any(Devolucao.class))).thenAnswer(i -> i.getArgument(0));

        Devolucao criada = devolucaoService.criarSolicitacao(devolucao);

        assertNotNull(criada.getNumero());
        assertTrue(criada.getNumero().startsWith("DEV-"));
        assertEquals(StatusDevolucao.PENDENTE, criada.getStatus());
        assertEquals("admin", criada.getUsuarioSolicitante());
        verify(devolucaoRepository).save(devolucao);
    }

    @Test
    void analisarSolicitacao_DeveAprovarEDefinirStatusAprovada() {
        when(devolucaoRepository.findById(1L)).thenReturn(Optional.of(devolucao));
        when(devolucaoRepository.save(any(Devolucao.class))).thenAnswer(i -> i.getArgument(0));

        Devolucao analisada = devolucaoService.analisarSolicitacao(1L, true, "Parecer OK");

        assertEquals(StatusDevolucao.APROVADA, analisada.getStatus());
        assertEquals("Parecer OK", analisada.getParecerAnalise());
        assertEquals("admin", analisada.getUsuarioAnalista());
    }

    @Test
    void analisarSolicitacao_DeveRejeitarEDefinirStatusRejeitada() {
        when(devolucaoRepository.findById(1L)).thenReturn(Optional.of(devolucao));
        when(devolucaoRepository.save(any(Devolucao.class))).thenAnswer(i -> i.getArgument(0));

        Devolucao analisada = devolucaoService.analisarSolicitacao(1L, false, "Recusado");

        assertEquals(StatusDevolucao.REJEITADA, analisada.getStatus());
    }

    @Test
    void processarDevolucao_DeveGerarNCeConcluir() {
        devolucao.setStatus(StatusDevolucao.APROVADA);
        when(devolucaoRepository.findById(1L)).thenReturn(Optional.of(devolucao));
        when(devolucaoRepository.save(any(Devolucao.class))).thenAnswer(i -> i.getArgument(0));
        
        Fatura ncSimulada = new Fatura();
        ncSimulada.setId(50L);
        when(faturaService.emitirNotaCredito(eq(10L), anyString())).thenReturn(ncSimulada);

        Devolucao processada = devolucaoService.processarDevolucao(1L);

        assertEquals(StatusDevolucao.CONCLUIDA, processada.getStatus());
        assertNotNull(processada.getNotaCredito());
        assertNotNull(processada.getDataConclusao());
        verify(faturaService).emitirNotaCredito(eq(10L), contains("Devolução"));
    }
}

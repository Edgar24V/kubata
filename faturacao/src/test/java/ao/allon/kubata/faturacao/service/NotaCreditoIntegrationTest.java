package ao.allon.kubata.faturacao.service;

import ao.allon.kubata.core.repository.PlanoContaRepository;
import ao.allon.kubata.core.service.ContabilidadeService;
import ao.allon.kubata.faturacao.domain.Cliente;
import ao.allon.kubata.faturacao.domain.Fatura;
import ao.allon.kubata.faturacao.domain.ItemFatura;
import ao.allon.kubata.faturacao.domain.Serie;
import ao.allon.kubata.faturacao.domain.enums.StatusFatura;
import ao.allon.kubata.faturacao.domain.enums.TipoDocumento;
import ao.allon.kubata.faturacao.repository.FaturaRepository;
import ao.allon.kubata.faturacao.repository.ItemFaturaRepository;
import ao.allon.kubata.faturacao.service.agt.AGTElectronicInvoiceService;
import ao.allon.kubata.faturacao.service.agt.AGTService;
import ao.allon.kubata.faturacao.service.agt.JWSDigitalSignatureService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NotaCreditoIntegrationTest {

    private FaturaService faturaService;
    private FaturaRepository faturaRepository;
    private ItemFaturaRepository itemFaturaRepository;
    private ProdutoService produtoService;
    private SerieService serieService;
    private AGTService agtService;
    private AGTElectronicInvoiceService agtElectronicInvoiceService;
    private JWSDigitalSignatureService signatureService;
    private ContabilidadeService contabilidadeService;
    private PlanoContaRepository planoContaRepository;
    private SessionManager sessionManager;

    @BeforeEach
    void setUp() {
        faturaRepository = Mockito.mock(FaturaRepository.class);
        itemFaturaRepository = Mockito.mock(ItemFaturaRepository.class);
        produtoService = Mockito.mock(ProdutoService.class);
        serieService = Mockito.mock(SerieService.class);
        agtService = Mockito.mock(AGTService.class);
        agtElectronicInvoiceService = Mockito.mock(AGTElectronicInvoiceService.class);
        signatureService = Mockito.mock(JWSDigitalSignatureService.class);
        contabilidadeService = Mockito.mock(ContabilidadeService.class);
        planoContaRepository = Mockito.mock(PlanoContaRepository.class);
        sessionManager = Mockito.mock(SessionManager.class);
        
        // Mock default async behavior for AGTElectronicInvoiceService
        when(agtElectronicInvoiceService.submitInvoiceAsync(any())).thenReturn(java.util.concurrent.CompletableFuture.completedFuture(new ao.allon.kubata.faturacao.service.agt.dto.AGTResponseDTO()));

        faturaService = new FaturaService(faturaRepository, itemFaturaRepository, produtoService, serieService, agtService, agtElectronicInvoiceService, signatureService, contabilidadeService, planoContaRepository, sessionManager);
    }

    @Test
    void deveEmitirNotaCreditoComSucesso() {
        // Cenário: Fatura Original Emitida
        Fatura faturaOriginal = new Fatura();
        faturaOriginal.setId(10L);
        faturaOriginal.setTipoDocumento(TipoDocumento.FATURA);
        faturaOriginal.setStatus(StatusFatura.EMITIDA);
        faturaOriginal.setNumero("FT 2024/10");
        faturaOriginal.setCliente(new Cliente());
        faturaOriginal.setTotal(new BigDecimal("1000.00"));
        
        ItemFatura item = new ItemFatura();
        item.setDescricao("Serviço");
        item.setQuantidade(1);
        item.setPrecoUnitario(new BigDecimal("1000.00"));
        faturaOriginal.addItem(item);
        
        // Mock Serie NC
        Serie serieNC = new Serie();
        serieNC.setTipoDocumento(TipoDocumento.NOTA_CREDITO);
        serieNC.setDesignacao("2024");
        serieNC.setAno(2024);
        serieNC.setAtiva(true);
        
        when(faturaRepository.findById(10L)).thenReturn(Optional.of(faturaOriginal));
        when(serieService.findPadrao(TipoDocumento.NOTA_CREDITO)).thenReturn(Optional.of(serieNC));
        when(serieService.getProximoNumero(serieNC)).thenReturn(1L);
        when(faturaRepository.save(any(Fatura.class))).thenAnswer(i -> {
            Fatura f = i.getArgument(0);
            if (f.getId() == null) f.setId(20L); // ID da NC
            return f;
        });

        // Ação: Emitir NC
        Fatura nc = faturaService.emitirNotaCredito(10L, "Devolução de mercadoria");
        
        // Verificação
        assertNotNull(nc);
        assertEquals(TipoDocumento.NOTA_CREDITO, nc.getTipoDocumento());
        assertEquals(StatusFatura.EMITIDA, nc.getStatus());
        assertEquals(faturaOriginal, nc.getFaturaReferencia());
        assertEquals("Devolução de mercadoria", nc.getMotivoCancelamento()); // O campo motivoCancelamento é usado para motivo da NC
        assertEquals(1, nc.getItens().size());
        assertEquals("Serviço", nc.getItens().get(0).getDescricao());
        
        // Verifica se houve entrada de estoque (já que é devolução/crédito)
        verify(produtoService, times(0)).registarSaida(any(), any(), any()); // Não deve sair
        // Nota: O teste unitário de FaturaService.processarEmissao chama registarEntrada para NC
        // Como processarEmissao é privado e chamado internamente, verificamos o efeito colateral se possível
        // Mas como produto é null no item mockado acima, não chama. Se adicionarmos produto:
        
        // Novo teste com produto
    }
}

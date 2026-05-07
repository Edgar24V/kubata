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
import ao.allon.kubata.faturacao.service.agt.AGTService;
import ao.allon.kubata.faturacao.service.agt.AGTElectronicInvoiceService;
import ao.allon.kubata.faturacao.service.agt.JWSDigitalSignatureService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class FaturaServiceNotaDebitoTest {

    private FaturaService faturaService;
    private FaturaRepository faturaRepository;
    private ItemFaturaRepository itemFaturaRepository;
    private SerieService serieService;
    private AGTService agtService;
    private AGTElectronicInvoiceService agtElectronicInvoiceService;
    private JWSDigitalSignatureService signatureService;
    private ProdutoService produtoService;
    private ContabilidadeService contabilidadeService;
    private PlanoContaRepository planoContaRepository;
    private SessionManager sessionManager;

    @BeforeEach
    void setup() {
        faturaRepository = mock(FaturaRepository.class);
        itemFaturaRepository = mock(ItemFaturaRepository.class);
        serieService = mock(SerieService.class);
        agtService = mock(AGTService.class);
        agtElectronicInvoiceService = mock(AGTElectronicInvoiceService.class);
        signatureService = mock(JWSDigitalSignatureService.class);
        produtoService = mock(ProdutoService.class);
        contabilidadeService = mock(ContabilidadeService.class);
        planoContaRepository = mock(PlanoContaRepository.class);
        sessionManager = mock(SessionManager.class);
        
        // Mock default async behavior for AGTElectronicInvoiceService
        when(agtElectronicInvoiceService.submitInvoiceAsync(any())).thenReturn(java.util.concurrent.CompletableFuture.completedFuture(new ao.allon.kubata.faturacao.service.agt.dto.AGTResponseDTO()));

        faturaService = new FaturaService(faturaRepository, itemFaturaRepository, produtoService, serieService, agtService, agtElectronicInvoiceService, signatureService, contabilidadeService, planoContaRepository, sessionManager);
    }

    @Test
    void emitirNotaDebito_DeveCriarDocumentoComItensAdicionais() {
        Fatura faturaOriginal = new Fatura();
        faturaOriginal.setId(1L);
        Cliente c = new Cliente();
        c.setNome("Cliente X");
        c.setNif("999999999");
        faturaOriginal.setCliente(c);
        faturaOriginal.setTipoDocumento(TipoDocumento.FATURA);
        faturaOriginal.setStatus(StatusFatura.EMITIDA);
        faturaOriginal.setDataEmissao(LocalDate.now());
        when(faturaRepository.findById(1L)).thenReturn(Optional.of(faturaOriginal));
        Serie serie = new Serie();
        when(serieService.findPadrao(TipoDocumento.NOTA_DEBITO)).thenReturn(Optional.of(serie));
        when(faturaRepository.save(ArgumentMatchers.any(Fatura.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ItemFatura it = new ItemFatura();
        it.setDescricao("Diferença de preço");
        it.setQuantidade(1);
        it.setPrecoUnitario(new BigDecimal("100.00"));
        it.setTaxaIva(new BigDecimal("14.00"));
        List<ItemFatura> itens = new ArrayList<>();
        itens.add(it);

        Fatura nd = faturaService.emitirNotaDebito(1L, "Acréscimo", itens);

        assertNotNull(nd);
        assertEquals(TipoDocumento.NOTA_DEBITO, nd.getTipoDocumento());
        assertEquals(1, nd.getItens().size());
        assertEquals("Diferença de preço", nd.getItens().get(0).getDescricao());
    }
}

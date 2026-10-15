package ao.allon.kubata.faturacao.service;

import ao.allon.kubata.core.repository.PlanoContaRepository;
import ao.allon.kubata.core.service.ContabilidadeService;
import ao.allon.kubata.faturacao.domain.Cliente;
import ao.allon.kubata.faturacao.domain.Fatura;
import ao.allon.kubata.faturacao.domain.ItemFatura;
import ao.allon.kubata.faturacao.domain.Produto;
import ao.allon.kubata.faturacao.domain.Serie;
import ao.allon.kubata.faturacao.domain.enums.DocumentCancelReason;
import ao.allon.kubata.faturacao.domain.enums.StatusFatura;
import ao.allon.kubata.faturacao.domain.enums.TipoCliente;
import ao.allon.kubata.faturacao.domain.enums.TipoDocumento;
import ao.allon.kubata.faturacao.repository.FaturaRepository;
import ao.allon.kubata.faturacao.repository.ItemFaturaRepository;
import ao.allon.kubata.faturacao.service.agt.AGTInvoiceData;
import ao.allon.kubata.faturacao.service.agt.AGTService;
import ao.allon.kubata.faturacao.service.agt.AGTElectronicInvoiceService;
import ao.allon.kubata.faturacao.service.agt.JWSDigitalSignatureService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.security.PrivateKey;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FaturaServiceTest {

    @Mock private FaturaRepository faturaRepository;
    @Mock private ItemFaturaRepository itemFaturaRepository;
    @Mock private ProdutoService produtoService;
    @Mock private SerieService serieService;
    @Mock private AGTService agtService;
    @Mock private AGTElectronicInvoiceService agtElectronicInvoiceService;
    @Mock private JWSDigitalSignatureService signatureService;
    @Mock private ContabilidadeService contabilidadeService;
    @Mock private PlanoContaRepository planoContaRepository;
    @Mock private SessionManager sessionManager;
    @Mock private ReciboService reciboService;
    @Mock private AgtSubmissionStatusService agtSubmissionStatusService;

    @InjectMocks private FaturaService faturaService;

    private Fatura fatura;
    private Produto produto;
    private final AtomicLong nextId = new AtomicLong(2L);

    @BeforeEach
    void setUp() throws Exception {
        fatura = new Fatura();
        fatura.setId(1L);
        fatura.setNumero("RASCUNHO-FT-1");
        fatura.setTipoDocumento(TipoDocumento.FATURA);
        fatura.setDataEmissao(LocalDate.now());
        fatura.setHoraEmissao(LocalTime.now());
        fatura.setDataVencimento(LocalDate.now().plusDays(30));
        fatura.setStatus(StatusFatura.RASCUNHO);

        Cliente cliente = new Cliente();
        cliente.setId(20L);
        cliente.setNome("Cliente de Teste, Lda.");
        cliente.setNif("5412345670");
        cliente.setTipo(TipoCliente.EMPRESA);
        fatura.setCliente(cliente);

        produto = new Produto();
        produto.setId(10L);
        produto.setNome("Produto A");
        produto.setCodigoBarra("TEST-PROD-10");
        produto.setPrecoUnitario(new BigDecimal("1000.00"));
        produto.setPercentualIva(new BigDecimal("14.00"));

        ItemFatura item = new ItemFatura();
        item.setProduto(produto);
        item.setDescricao("Produto A");
        item.setQuantidade(2);
        item.setPrecoUnitario(new BigDecimal("1000.00"));
        item.setTaxaIva(new BigDecimal("14.00"));
        fatura.addItem(item);

        when(faturaRepository.findById(1L)).thenReturn(Optional.of(fatura));
        when(faturaRepository.save(any(Fatura.class))).thenAnswer(invocation -> {
            Fatura salvo = invocation.getArgument(0);
            if (salvo.getId() == null) salvo.setId(nextId.getAndIncrement());
            return salvo;
        });
        when(agtElectronicInvoiceService.isRealIntegrationEnabled()).thenReturn(false);
        when(serieService.findPadrao(any(TipoDocumento.class))).thenAnswer(invocation ->
                Optional.of(new Serie("2026", invocation.getArgument(0), LocalDate.now().getYear())));
        when(serieService.getProximoNumero(any(Serie.class))).thenReturn(1L);
        when(agtService.generateDocumentHash(any(AGTInvoiceData.class), any(PrivateKey.class)))
                .thenReturn("A".repeat(40));
        when(planoContaRepository.findByCodigo(anyString())).thenReturn(Optional.empty());
    }

    @Test
    void emitirFatura_DeveValidarEmitirERegistarMovimentoDeStock() {
        Fatura resultado = faturaService.emitirFatura(1L);

        assertEquals(StatusFatura.EMITIDA, resultado.getStatus());
        assertNotNull(resultado.getHash());
        assertEquals("FT 2026/1", resultado.getNumero());
        assertEquals(new BigDecimal("2280.00"), resultado.getTotal().setScale(2));
        verify(produtoService).registarSaida(eq(10L), eq(2), contains("Emissão"));
        verify(faturaRepository, atLeastOnce()).save(any(Fatura.class));
    }

    @Test
    void cancelarFacturaEmitida_DeveExigirMotivoFiscalEReverterStock() {
        fatura.setStatus(StatusFatura.EMITIDA);

        faturaService.cancelarFatura(1L, "Documento não foi enviado ao cliente",
                DocumentCancelReason.NAO_ENVIADO);

        assertEquals(StatusFatura.CANCELADA, fatura.getStatus());
        assertEquals(DocumentCancelReason.NAO_ENVIADO, fatura.getDocumentCancelReason());
        verify(produtoService).registarEntrada(eq(10L), eq(2), contains("Estorno de stock"));
        verify(faturaRepository).save(any(Fatura.class));
    }

    @Test
    void cancelarRascunhoFiscalDeveFalharSemMovimentarStock() {
        assertThrows(IllegalStateException.class, () ->
                faturaService.cancelarFatura(1L, "Ainda não emitida", DocumentCancelReason.NAO_ENVIADO));
        verify(produtoService, never()).registarEntrada(anyLong(), anyInt(), anyString());
    }

    @Test
    void editarDocumentoEmitidoDeveFalharMesmoSemHash() {
        fatura.setStatus(StatusFatura.EMITIDA);
        fatura.setHash(null);

        assertThrows(IllegalStateException.class, () -> faturaService.salvar(fatura));
        verify(faturaRepository, never()).save(any(Fatura.class));
    }

    @Test
    void emitirSemClienteDeveFalharAntesDeConsumirNumero() {
        fatura.setCliente(null);

        assertThrows(IllegalArgumentException.class, () -> faturaService.emitirFatura(1L));
        verify(serieService, never()).getProximoNumero(any(Serie.class));
    }

    @Test
    void registrarRecebimentoDeveDelegarParaLivroDeRecibos() {
        fatura.setStatus(StatusFatura.EMITIDA);

        Fatura resultado = faturaService.registrarRecebimento(1L, new BigDecimal("50.00"), "REF-123");

        assertSame(fatura, resultado);
        verify(reciboService).registrar(1L, new BigDecimal("50.00"), "REF-123", null);
    }

    @Test
    void emitirNotaCreditoDeveRegistarEntradaDeStock() {
        fatura.setStatus(StatusFatura.EMITIDA);

        Fatura notaCredito = faturaService.emitirNotaCredito(1L, "Devolução por defeito");

        assertEquals(TipoDocumento.NOTA_CREDITO, notaCredito.getTipoDocumento());
        assertTrue(notaCredito.getObservacoes().contains("Nota de Crédito"));
        verify(produtoService).registarEntrada(eq(10L), eq(2), contains("Emissão"));
    }
}

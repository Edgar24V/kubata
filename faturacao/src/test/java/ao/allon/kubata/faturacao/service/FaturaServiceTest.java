package ao.allon.kubata.faturacao.service;

import ao.allon.kubata.core.repository.PlanoContaRepository;
import ao.allon.kubata.core.service.ContabilidadeService;
import ao.allon.kubata.faturacao.domain.Fatura;
import ao.allon.kubata.faturacao.domain.Produto;
import ao.allon.kubata.faturacao.domain.Recibo;
import ao.allon.kubata.faturacao.domain.ItemFatura;
import ao.allon.kubata.faturacao.domain.Serie;
import ao.allon.kubata.faturacao.domain.enums.TipoDocumento;
import ao.allon.kubata.faturacao.domain.enums.MetodoPagamento;
import ao.allon.kubata.faturacao.domain.enums.StatusFatura;
import ao.allon.kubata.faturacao.domain.enums.TaxExemptionCode;
import ao.allon.kubata.faturacao.repository.FaturaRepository;
import ao.allon.kubata.faturacao.service.agt.AGTService;
import ao.allon.kubata.faturacao.service.agt.AGTElectronicInvoiceService;
import ao.allon.kubata.faturacao.service.agt.JWSDigitalSignatureService;
import ao.allon.kubata.faturacao.service.agt.dto.AGTResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FaturaServiceTest {

    @Mock
    private FaturaRepository faturaRepository;

    @Mock
    private ProdutoService produtoService;

    @Mock
    private SerieService serieService;

    @Mock
    private AGTService agtService;

    @Mock
    private AGTElectronicInvoiceService agtElectronicInvoiceService;

    @Mock
    private JWSDigitalSignatureService signatureService;

    @Mock
    private ContabilidadeService contabilidadeService;

    @Mock
    private PlanoContaRepository planoContaRepository;

    @Mock
    private ReciboService reciboService;

    @InjectMocks
    private FaturaService faturaService;

    private Fatura fatura;

    @BeforeEach
    void setUp() {
        fatura = new Fatura();
        fatura.setId(1L);
        fatura.setNumero("FT202600001");
        fatura.setDataEmissao(LocalDate.now());
        fatura.setDataVencimento(LocalDate.now().plusDays(30));
        fatura.setStatus(StatusFatura.RASCUNHO);
        Serie serie = new Serie("2026", TipoDocumento.FATURA, LocalDate.now().getYear());
        serie.setId(2L);
        serie.setUltimoNumero(0L);
        lenient().when(serieService.findPadrao(TipoDocumento.FATURA)).thenReturn(Optional.of(serie));
        lenient().when(serieService.getProximoNumero(serie)).thenReturn(1L);
        ItemFatura item = new ItemFatura();
        item.setDescricao("Produto A");
        item.setQuantidade(2);
        item.setPrecoUnitario(new BigDecimal("1000"));
        item.setPercentualIva(new BigDecimal("14.00"));
        Produto produto = new Produto();
        produto.setId(10L);
        item.setProduto(produto);
        fatura.addItem(item);
        lenient().when(faturaRepository.findById(1L)).thenReturn(Optional.of(fatura));
        lenient().when(faturaRepository.save(any(Fatura.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void emitirFatura_ProducaoDeveExigirOperadorAutenticado() {
        when(agtElectronicInvoiceService.isProductionReadinessEnforced()).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> faturaService.emitirFatura(1L));

        assertEquals(StatusFatura.RASCUNHO, fatura.getStatus());
        verify(faturaRepository, never()).save(any(Fatura.class));
        verify(signatureService, never()).getPrivateKeyForFiscalHash();
    }

    @Test
    void emitirFatura_RejeicaoAgtDeveFicarDistintaDeFalhaDeTransporte() {
        AGTResponseDTO resposta = new AGTResponseDTO();
        resposta.setStatus("REJECTED");
        resposta.setRequestID("req-rejeitado-1");
        resposta.setErrorList(List.of(new AGTResponseDTO.AGTErrorEntryDTO()));
        when(agtElectronicInvoiceService.submitInvoiceAsync(any(Fatura.class)))
                .thenReturn(CompletableFuture.completedFuture(resposta));

        Fatura res = faturaService.emitirFatura(1L);

        assertEquals("REJECTED", res.getAgtSubmissionStatus());
        assertEquals("req-rejeitado-1", res.getAgtRequestId());
        verify(faturaRepository, atLeastOnce()).save(any(Fatura.class));
    }

    @Test
    void emitirFatura_DeveBaixarEstoqueESetarStatusEmitida() {
        Fatura res = faturaService.emitirFatura(1L);
        assertEquals(StatusFatura.EMITIDA, res.getStatus());
        verify(produtoService, times(1)).registarSaida(eq(10L), eq(2), anyString());
        // A emissão persiste o documento e o callback assíncrono pode persistir
        // novamente o estado da submissão AGT (SUBMITTED/FAILED/SIMULATED).
        verify(faturaRepository, atLeastOnce()).save(any(Fatura.class));
    }

    @Test
    void emitirVendaADinheiro_DeveBaixarEstoqueEMarcarPaga() {
        fatura.setTipoDocumento(TipoDocumento.VENDA_A_DINHEIRO);
        Serie serieVenda = new Serie("2026-TV", TipoDocumento.VENDA_A_DINHEIRO, LocalDate.now().getYear());
        serieVenda.setId(4L);
        serieVenda.setUltimoNumero(0L);
        when(serieService.findPadrao(TipoDocumento.VENDA_A_DINHEIRO)).thenReturn(Optional.of(serieVenda));
        when(serieService.getProximoNumero(serieVenda)).thenReturn(1L);

        Fatura res = faturaService.emitirFatura(1L);

        assertEquals(StatusFatura.PAGA, res.getStatus());
        verify(produtoService, times(1)).registarSaida(eq(10L), eq(2), anyString());
    }

    @Test
    void submeterAgtManualmente_DeveBloquearDocumentoJaPendenteParaEvitarDuplicacao() {
        fatura.setTipoDocumento(TipoDocumento.FATURA);
        fatura.setAgtSubmissionStatus("PENDING");

        assertThrows(IllegalStateException.class,
            () -> faturaService.submeterAgtManualmente(1L));

        verify(agtElectronicInvoiceService, never()).assertReadyForFiscalEmission();
        verify(agtElectronicInvoiceService, never()).submitInvoiceAsync(any(Fatura.class));
        verify(faturaRepository, never()).save(any(Fatura.class));
    }

    @Test
    void cancelarFatura_DeveBloquearAnulacaoFiscalSemFluxoAgtHomologado() {
        fatura.setStatus(StatusFatura.EMITIDA);

        assertThrows(IllegalStateException.class,
            () -> faturaService.cancelarFatura(1L, "Cancelamento Teste"));

        assertEquals(StatusFatura.EMITIDA, fatura.getStatus());
        verify(produtoService, never()).registarEntrada(eq(10L), eq(2), anyString());
        verify(agtElectronicInvoiceService, never()).submitInvoiceAsync(any(Fatura.class));
        verify(faturaRepository, never()).save(any(Fatura.class));
    }

    @Test
    void cancelarRascunho_DeveCancelarLocalmenteSemMovimentarEstoqueNemEnviarAgt() {
        fatura.setStatus(StatusFatura.RASCUNHO);

        faturaService.cancelarFatura(1L, "Rascunho abandonado");

        assertEquals(StatusFatura.CANCELADA, fatura.getStatus());
        verify(faturaRepository, times(1)).save(any(Fatura.class));
        verify(produtoService, never()).registarEntrada(eq(10L), eq(2), anyString());
        verify(agtElectronicInvoiceService, never()).submitInvoiceAsync(any(Fatura.class));
    }

    @Test
    void salvar_DeveImpedirEdicaoDeDocumentoEmitidoMesmoSemHash() {
        fatura.setStatus(StatusFatura.EMITIDA);
        fatura.setHash(null);

        assertThrows(IllegalStateException.class, () -> faturaService.salvar(fatura));
        verify(faturaRepository, never()).save(any(Fatura.class));
    }

    @Test
    void emitirFatura_DeveBloquearReemissaoDeDocumentosPagosOuCancelados() {
        for (StatusFatura estado : new StatusFatura[] { StatusFatura.PAGA, StatusFatura.CANCELADA }) {
            fatura.setStatus(estado);
            assertThrows(IllegalStateException.class, () -> faturaService.emitirFatura(1L));
        }
        verify(produtoService, never()).registarSaida(eq(10L), eq(2), anyString());
    }

    @Test
    void registrarRecebimento_DeveDelegarParaReciboEActualizarLiquidacao() {
        fatura.setStatus(StatusFatura.EMITIDA);
        fatura.recalculateTotals();
        when(reciboService.registrar(eq(1L), eq(fatura.getTotal()), eq("REF123"), anyString(),
                eq(MetodoPagamento.DINHEIRO))).thenAnswer(invocation -> {
            fatura.setStatus(StatusFatura.PAGA);
            return new Recibo();
        });

        Fatura res = faturaService.registrarRecebimento(1L, fatura.getTotal(), "REF123");

        assertEquals(StatusFatura.PAGA, res.getStatus());
        verify(reciboService).registrar(eq(1L), eq(fatura.getTotal()), eq("REF123"), anyString(),
                eq(MetodoPagamento.DINHEIRO));
        verify(faturaRepository, never()).save(any(Fatura.class));
    }

    @Test
    void emitirNotaCredito_DeveDevolverEstoqueESemAlterarFacturaOriginal() {
        fatura.setStatus(StatusFatura.EMITIDA);
        // O fluxo de nota de crédito exige linhas persistidas e usa uma leitura
        // versionada para impedir créditos concorrentes da mesma factura.
        fatura.getItens().get(0).setId(11L);
        when(faturaRepository.findByIdWithItensForCreditNote(1L)).thenReturn(Optional.of(fatura));
        fatura.setObservacoes("Observação original imutável");
        Serie serieNc = new Serie("2026-NC", TipoDocumento.NOTA_CREDITO, LocalDate.now().getYear());
        serieNc.setId(3L);
        serieNc.setUltimoNumero(0L);
        when(serieService.findPadrao(TipoDocumento.NOTA_CREDITO)).thenReturn(Optional.of(serieNc));
        when(serieService.getProximoNumero(serieNc)).thenReturn(1L);

        Fatura res = faturaService.emitirNotaCredito(1L, "Devolução parcial");

        assertTrue(res.getObservacoes().contains("Nota de Crédito"));
        assertEquals("Observação original imutável", fatura.getObservacoes());
        verify(faturaRepository, never()).save(same(fatura));
        verify(produtoService, times(1)).registarEntrada(eq(10L), eq(2), anyString());
    }

    @Test
    void corrigirItensSemCodigoIsencao_DeveExigirCodigoFiscalExplicito() {
        assertThrows(IllegalArgumentException.class,
                () -> faturaService.corrigirItensSemCodigoIsencao((String) null));
        verify(faturaRepository, never()).findAll();
    }

    @Test
    void corrigirItensSemCodigoIsencao_DeveModificarApenasRascunhos() {
        Fatura rascunho = facturaComItemIvaZero(StatusFatura.RASCUNHO, "Serviço em rascunho");
        Fatura emitida = facturaComItemIvaZero(StatusFatura.EMITIDA, "Serviço emitido");
        when(faturaRepository.findAll()).thenReturn(List.of(rascunho, emitida));

        int corrigidos = faturaService.corrigirItensSemCodigoIsencao(null, TaxExemptionCode.M99);

        assertEquals(1, corrigidos);
        assertEquals("M99", rascunho.getItens().get(0).getCodigoIsencao());
        assertEquals(TaxExemptionCode.M99.getDescription(), rascunho.getItens().get(0).getMotivoIsencao());
        assertNull(emitida.getItens().get(0).getCodigoIsencao(),
                "Um documento emitido não pode ser alterado pela correcção em massa.");
        verify(faturaRepository, times(1)).save(rascunho);
        verify(faturaRepository, never()).save(emitida);
    }

    private Fatura facturaComItemIvaZero(StatusFatura status, String descricao) {
        Fatura documento = new Fatura();
        documento.setStatus(status);
        ItemFatura item = new ItemFatura();
        item.setDescricao(descricao);
        item.setQuantidade(1);
        item.setPrecoUnitario(new BigDecimal("100.00"));
        item.setTaxaIva(BigDecimal.ZERO);
        documento.addItem(item);
        return documento;
    }
}

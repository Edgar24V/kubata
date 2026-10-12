package ao.allon.kubata.core.service;

import ao.allon.kubata.core.domain.Empresa;
import ao.allon.kubata.core.domain.ExercicioFiscal;
import ao.allon.kubata.core.domain.SerieDocumento;
import ao.allon.kubata.core.domain.SerieDocumento.TipoDocumentoSAFT;
import ao.allon.kubata.core.repository.ExercicioFiscalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExercicioFiscalServiceTest {

    @Mock
    private ExercicioFiscalRepository repository;

    @Mock
    private SerieDocumentoService serieDocumentoService;

    private ExercicioFiscalService service;
    private Empresa empresa;
    private ExercicioFiscal base;

    @BeforeEach
    void setUp() {
        service = new ExercicioFiscalService(repository, serieDocumentoService);

        empresa = new Empresa();
        empresa.setId(12L);
        empresa.setNome("Empresa de Teste");
        empresa.setAtiva(true);
        empresa.setExercicioActual(2025);

        base = ExercicioFiscal.builder()
                .id(101L)
                .empresa(empresa)
                .ano(2025)
                .dataInicio(java.time.LocalDate.of(2025, 1, 1))
                .dataFim(java.time.LocalDate.of(2025, 12, 31))
                .estado(ExercicioFiscal.EstadoExercicio.FECHADO)
                .build();
    }

    @Test
    void deveExigirOExercicioImediatamenteAnterior() {
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> service.criarExercicioAssistido(
                        empresa, 2024, 2026, Set.of("VENDAS"), false
                )
        );

        assertTrue(error.getMessage().contains("imediatamente anterior"));
        verifyNoInteractions(repository, serieDocumentoService);
    }

    @Test
    void deveRejeitarBaseInexistenteSemCriarDestino() {
        when(repository.findByEmpresaIdAndAno(12L, 2025)).thenReturn(Optional.empty());

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> service.criarExercicioAssistido(
                        empresa, 2025, 2026, Set.of("VENDAS"), false
                )
        );

        assertTrue(error.getMessage().contains("Não existe o exercício-base"));
        verify(repository, never()).save(any(ExercicioFiscal.class));
        verifyNoInteractions(serieDocumentoService);
    }

    @Test
    void deveReplicarApenasSeriesDoAnoBaseEDosModulosSeleccionados() {
        SerieDocumento vendaBase = serie(2025, TipoDocumentoSAFT.FT, "A");
        SerieDocumento vendaAntiga = serie(2024, TipoDocumentoSAFT.FT, "A");
        SerieDocumento stockBase = serie(2025, TipoDocumentoSAFT.GT, "GT");

        when(repository.findByEmpresaIdAndAno(12L, 2025)).thenReturn(Optional.of(base));
        when(repository.findByEmpresaIdAndAno(12L, 2026)).thenReturn(Optional.empty());
        when(repository.save(any(ExercicioFiscal.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(serieDocumentoService.listarPorEmpresa(12L))
                .thenReturn(List.of(vendaBase, vendaAntiga, stockBase));

        SerieDocumento vendaDestino = serie(2026, TipoDocumentoSAFT.FT, "A");
        when(serieDocumentoService.replicarParaExercicio(
                eq(empresa), anyList(), eq(2026)
        )).thenReturn(List.of(vendaDestino));

        ExercicioFiscalService.ResultadoCriacao result = service.criarExercicioAssistido(
                empresa, 2025, 2026, Set.of("VENDAS"), false
        );

        assertEquals(2026, result.exercicio().getAno());
        assertEquals(2025, result.anoBase());
        assertEquals(1, result.seriesCriadas());
        assertTrue(result.modulosPreparados().contains("VENDAS"));
        assertFalse(result.definidoComoActual());
        assertEquals("Preparado pelo Assistente Kubata | Base: 2025 | Módulos: VENDAS | Séries criadas: 1.",
                result.exercicio().getObservacoes());

        verify(serieDocumentoService).replicarParaExercicio(
                eq(empresa),
                argThat(series -> series.size() == 1 && series.get(0) == vendaBase),
                eq(2026)
        );
        verify(repository, times(2)).save(any(ExercicioFiscal.class));
        assertEquals(2025, empresa.getExercicioActual());
    }

    @Test
    void deveManterExercicioActualQuandoNaoSolicitado() {
        when(repository.findByEmpresaIdAndAno(12L, 2025)).thenReturn(Optional.of(base));
        when(repository.findByEmpresaIdAndAno(12L, 2026)).thenReturn(Optional.empty());
        when(repository.save(any(ExercicioFiscal.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(serieDocumentoService.listarPorEmpresa(12L)).thenReturn(List.of());

        ExercicioFiscalService.ResultadoCriacao result = service.criarExercicioAssistido(
                empresa, 2025, 2026, Set.of(), false
        );

        assertFalse(result.definidoComoActual());
        assertEquals(2025, empresa.getExercicioActual());
        verify(serieDocumentoService, never()).replicarParaExercicio(
                any(Empresa.class), anyList(), anyInt()
        );
    }

    @Test
    void devePermitirMarcarNovoExercicioComoActual() {
        when(repository.findByEmpresaIdAndAno(12L, 2025)).thenReturn(Optional.of(base));
        when(repository.findByEmpresaIdAndAno(12L, 2026)).thenReturn(Optional.empty());
        when(repository.save(any(ExercicioFiscal.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(serieDocumentoService.listarPorEmpresa(12L)).thenReturn(List.of());

        ExercicioFiscalService.ResultadoCriacao result = service.criarExercicioAssistido(
                empresa, 2025, 2026, Set.of(), true
        );

        assertTrue(result.definidoComoActual());
        assertEquals(2026, empresa.getExercicioActual());
    }

    @Test
    void deveRejeitarEmpresaInactiva() {
        empresa.setAtiva(false);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.criarExercicioAssistido(
                        empresa, 2025, 2026, Set.of(), false
                )
        );

        verifyNoInteractions(repository, serieDocumentoService);
    }

    private SerieDocumento serie(int ano, TipoDocumentoSAFT tipo, String codigo) {
        return SerieDocumento.builder()
                .empresa(empresa)
                .tipoDocumento(tipo)
                .serie(codigo)
                .exercicio(ano)
                .numeroInicial(1L)
                .ultimoNumero(0L)
                .build();
    }
}

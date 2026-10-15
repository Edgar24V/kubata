package ao.allon.kubata.faturacao.service;

import ao.allon.kubata.faturacao.domain.Serie;
import ao.allon.kubata.faturacao.repository.ClienteRepository;
import ao.allon.kubata.faturacao.repository.FaturaRepository;
import ao.allon.kubata.faturacao.repository.SerieRepository;
import ao.allon.kubata.faturacao.service.agt.AGTElectronicInvoiceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

@ExtendWith(MockitoExtension.class)
class SerieServiceTest {

    @Mock
    private SerieRepository serieRepository;

    @Mock
    private FaturaRepository faturaRepository;

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private AGTElectronicInvoiceService agtElectronicInvoiceService;

    @InjectMocks
    private SerieService serieService;

    @Test
    void verificarGaps_DeveDetectarLacunasIntermediasEFinais() {
        Serie serie = new Serie("A", null, 2026);
        serie.setId(1L);
        serie.setUltimoNumero(4L);
        when(faturaRepository.findAllNumerosSequenciaisBySerie(serie))
                .thenReturn(List.of(1L, 3L));

        assertEquals(List.of(2L, 4L), serieService.verificarGaps(serie));
    }

    @Test
    void verificarGaps_DeveRespeitarInicioDeSerieAutorizadaPelaAgt() {
        Serie serie = new Serie("AGT-A", null, 2026);
        serie.setId(2L);
        serie.setAgtSeriesCode("AGT-A");
        serie.setAgtFirstDocumentNumber(100L);
        serie.setAgtLastDocumentNumber(110L);
        serie.setAgtAuthorizedQuantity(11L);
        serie.setEstablishmentNumber("1");
        serie.setSeriesContingencyIndicator("N");
        serie.setUltimoNumero(103L);
        when(faturaRepository.findAllNumerosSequenciaisBySerie(serie))
                .thenReturn(List.of(100L, 102L));

        assertEquals(List.of(101L, 103L), serieService.verificarGaps(serie));
    }

    @Test
    void gerarDocumentosFiller_DeveBloquearCriacaoFiscalFicticia() {
        Serie serie = new Serie("A", null, 2026);
        serie.setId(3L);
        serie.setUltimoNumero(5L);

        assertThrows(IllegalStateException.class,
                () -> serieService.gerarDocumentosFiller(serie, List.of(2L)));

        verify(faturaRepository, never()).save(any());
        verify(clienteRepository, never()).findByNif(anyString());
        verify(serieRepository, never()).save(any());
    }
}

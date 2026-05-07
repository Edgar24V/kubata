package ao.allon.kubata.faturacao.service;

import ao.allon.kubata.faturacao.domain.*;
import ao.allon.kubata.faturacao.domain.enums.*;
import ao.allon.kubata.faturacao.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SaftAoExportServiceTest {

    @Mock
    private EmpresaService empresaService;
    @Mock
    private FaturaRepository faturaRepository;
    @Mock
    private ProdutoRepository produtoRepository;
    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private ImpostoRepository impostoRepository;
    @Mock
    private ReciboRepository reciboRepository;
    @Mock
    private MotivoIsencaoRepository motivoIsencaoRepository;
    @Mock
    private SaftValidatorService saftValidatorService;

    @InjectMocks
    private SaftAoExportService saftAoExportService;

    @Test
    public void testExportWithPayments() throws Exception {
        // Mock dependencies
        Empresa empresa = new Empresa();
        empresa.setNif("123456789");
        empresa.setNome("Test Company");
        when(empresaService.getDadosEmpresa()).thenReturn(empresa);

        LocalDate now = LocalDate.now();
        
        // Mock Fatura
        Fatura fatura = new Fatura();
        fatura.setNumero("FT 2024/1");
        fatura.setDataEmissao(now);
        Serie serie = new Serie();
        serie.setTipoDocumento(TipoDocumento.FATURA);
        fatura.setSerie(serie);
        Cliente cliente = new Cliente();
        cliente.setId(1L);
        fatura.setCliente(cliente);
        fatura.setTotal(new BigDecimal("1000.00"));
        fatura.setHash("hash123");
        fatura.setHashControl("1");
        
        when(faturaRepository.findByDataEmissaoBetween(any(), any())).thenReturn(Collections.singletonList(fatura));
        
        // Mock Recibo
        Recibo recibo = new Recibo();
        recibo.setNumero("RC 2024/1");
        recibo.setDataRecebimento(now);
        recibo.setValor(new BigDecimal("1000.00"));
        recibo.setMetodoPagamento(MetodoPagamento.DINHEIRO);
        recibo.setFatura(fatura);
        
        when(reciboRepository.findByDataRecebimentoBetween(any(), any())).thenReturn(Collections.singletonList(recibo));
        
        when(saftValidatorService.validateForExport(any(), any())).thenReturn(Collections.emptyList());

        // Create temp file
        File tempFile = File.createTempFile("saft_test", ".xml");
        
        // Execute
        saftAoExportService.exportFullSaft(tempFile, now, now);
        
        // Verify content
        String content = Files.readString(tempFile.toPath());
        assertTrue(content.contains("<Payments>"));
        assertTrue(content.contains("<PaymentRefNo>RC 2024/1</PaymentRefNo>"));
        assertTrue(content.contains("<PaymentMechanism>NU</PaymentMechanism>")); // DINHEIRO -> NU
        
        // Cleanup
        tempFile.delete();
    }
}

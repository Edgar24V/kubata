package ao.allon.kubata.faturacao.service.report;

import ao.allon.kubata.faturacao.domain.Empresa;
import ao.allon.kubata.faturacao.repository.FaturaRepository;
import ao.allon.kubata.faturacao.service.EmpresaService;
import ao.allon.kubata.faturacao.service.agt.AGTElectronicInvoiceService;
import ao.allon.kubata.faturacao.service.agt.AGTService;
import ao.allon.kubata.faturacao.service.barcode.BarcodeService;
import ao.allon.kubata.faturacao.service.printer.PrinterService;
import net.sf.jasperreports.engine.JasperPrint;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvoiceReportServiceTest {

    @Mock
    private AGTService agtService;

    @Mock
    private AGTElectronicInvoiceService agtElectronicInvoiceService;

    @Mock
    private BarcodeService barcodeService;

    @Mock
    private PrinterService printerService;

    @Mock
    private EmpresaService empresaService;

    @Mock
    private FaturaRepository faturaRepository;

    @InjectMocks
    private InvoiceReportService invoiceReportService;

    @BeforeEach
    void setUp() {
        when(empresaService.getDadosEmpresa()).thenReturn(new Empresa());
    }

    @Test
    void testPdfGeneration() throws Exception {
        InvoiceReportData header = createSampleHeader();
        List<InvoiceItemData> items = createSampleItems();

        byte[] pdfBytes = invoiceReportService.generateInvoicePdf(header, items);
        
        Assertions.assertNotNull(pdfBytes);
        Assertions.assertTrue(pdfBytes.length > 0, "PDF não deve estar vazio");
    }

    @Test
    void testThermalPrinting() throws Exception {
        InvoiceReportData header = createSampleHeader();
        List<InvoiceItemData> items = createSampleItems();
        String printerName = "Impressora Termica 80mm";

        invoiceReportService.printInvoiceThermal(header, items, printerName);

        ArgumentCaptor<JasperPrint> jasperPrintCaptor = ArgumentCaptor.forClass(JasperPrint.class);
        verify(printerService).printReport(jasperPrintCaptor.capture(), eq(printerName));

        JasperPrint capturedPrint = jasperPrintCaptor.getValue();
        Assertions.assertNotNull(capturedPrint);
        Assertions.assertEquals("invoice_thermal_80mm", capturedPrint.getName());
        Assertions.assertEquals(226, capturedPrint.getPageWidth());
    }

    private InvoiceReportData createSampleHeader() {
        return new InvoiceReportData(
                "FT 2024/100",
                LocalDateTime.now(),
                LocalDateTime.now(),
                "Operador Teste",
                "Cliente Exemplo",
                "999999999",
                new BigDecimal("1000.00"),
                new BigDecimal("140.00"),
                new BigDecimal("1140.00"),
                BigDecimal.ZERO, // totalDiscount
                BigDecimal.ZERO, // totalRetencao
                "HASH_TESTE_BASE64_MUITO_LONGO_PARA_SIMULAR_RSA",
                "HASH_ANTERIOR"
        );
    }

    private List<InvoiceItemData> createSampleItems() {
        return List.of(
                new InvoiceItemData("PROD01", "Produto Teste 1", BigDecimal.ONE, "un", new BigDecimal("500.00"), new BigDecimal("14"), new BigDecimal("570.00"), null, null),
                new InvoiceItemData("PROD02", "Produto Teste 2", BigDecimal.ONE, "un", new BigDecimal("500.00"), new BigDecimal("14"), new BigDecimal("570.00"), null, null)
        );
    }
}

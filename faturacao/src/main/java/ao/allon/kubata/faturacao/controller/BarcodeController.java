package ao.allon.kubata.faturacao.controller;

import ao.allon.kubata.faturacao.service.agt.AGTInvoiceData;
import ao.allon.kubata.faturacao.service.agt.AGTService;
import ao.allon.kubata.faturacao.service.barcode.BarcodeService;
import ao.allon.kubata.faturacao.service.barcode.BarcodeTypeEnum;
import ao.allon.kubata.faturacao.service.report.InvoiceItemData;
import ao.allon.kubata.faturacao.service.report.InvoiceReportData;
import ao.allon.kubata.faturacao.service.report.InvoiceReportService;
import ao.allon.kubata.faturacao.service.EmpresaService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Controller para operações relacionadas a códigos de barras e AGT.
 * Útil para testes e integração via API local.
 */
@RestController
@RequestMapping("/api/barcode")
public class BarcodeController {

    private final BarcodeService barcodeService;
    private final AGTService agtService;
    private final InvoiceReportService reportService;
    private final EmpresaService empresaService;

    public BarcodeController(BarcodeService barcodeService, AGTService agtService, InvoiceReportService reportService, EmpresaService empresaService) {
        this.barcodeService = barcodeService;
        this.agtService = agtService;
        this.reportService = reportService;
        this.empresaService = empresaService;
    }

    /**
     * Gera um PDF de teste de fatura com QR Code.
     */
    @GetMapping(value = "/pdf-test", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> generatePdfTest(
            @RequestParam(defaultValue = "FT 2024/100") String invoiceNo,
            @RequestParam(defaultValue = "1000.00") double totalAmount) {
        
        try {
            InvoiceReportData header = new InvoiceReportData(
                    invoiceNo,
                    LocalDateTime.now(),
                    LocalDateTime.now(),
                    "",
                    "Cliente Teste PDF",
                    "999999999",
                    BigDecimal.valueOf(totalAmount * 0.86), // Liq aprox
                    BigDecimal.valueOf(totalAmount * 0.14), // Tax aprox
                    BigDecimal.valueOf(totalAmount),
                    BigDecimal.ZERO, // totalDiscount
                    BigDecimal.ZERO, // totalRetencao
                    "HASH_TESTE_PDF_BASE64_SIMULADO",
                    "HASH_ANTERIOR"
            );

            List<InvoiceItemData> items = List.of(
                    new InvoiceItemData("P001", "Produto Exemplo 1", BigDecimal.ONE, "un", new BigDecimal("500.00"), new BigDecimal("14"), new BigDecimal("570.00"), null, null),
                    new InvoiceItemData("P002", "Produto Exemplo 2", BigDecimal.ONE, "un", new BigDecimal("500.00"), new BigDecimal("14"), new BigDecimal("570.00"), null, null)
            );

            byte[] pdfBytes = reportService.generateInvoicePdf(header, items);
            
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=fatura_teste.pdf");
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdfBytes);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Gera um código de barras genérico.
     */
    @GetMapping(value = "/generate", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> generateBarcode(
            @RequestParam String content,
            @RequestParam(defaultValue = "QR_CODE") BarcodeTypeEnum type,
            @RequestParam(defaultValue = "300") int width,
            @RequestParam(defaultValue = "300") int height) {
        
        try {
            byte[] imageBytes = barcodeService.generateBarcodeBytes(content, type, width, height);
            return ResponseEntity.ok(imageBytes);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Simula a geração de um QR Code AGT para uma fatura de teste.
     * Nota: Em produção, o Hash viria do processo de emissão real com chave privada.
     */
    @GetMapping(value = "/agt-simulation", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> generateAGTQRCodeSimulation(
            @RequestParam String invoiceNo,
            @RequestParam double totalAmount,
            @RequestParam(required = false) String nif) {
        
        try {
            // Dados simulados
            AGTInvoiceData data = new AGTInvoiceData(
                invoiceNo,
                LocalDateTime.now(),
                LocalDateTime.now(),
                nif,
                BigDecimal.valueOf(totalAmount),
                BigDecimal.ZERO,
                "PREVIOUS_HASH_SIMULATION",
                "CURRENT_HASH_SIMULATION_BASE64"
            );

            // Formata string AGT
            String nifEmpresa = empresaService.getDadosEmpresa().getNif();
            if (nifEmpresa == null) nifEmpresa = "999999999";
            String qrContent = agtService.formatQRCodeData(nifEmpresa, data);
            
            // Gera imagem
            byte[] imageBytes = barcodeService.generateBarcodeBytes(qrContent, BarcodeTypeEnum.QR_CODE, 300, 300);
            
            HttpHeaders headers = new HttpHeaders();
            headers.add("X-AGT-Content", qrContent);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(imageBytes);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Endpoint para testar impressão térmica direta.
     */
    @GetMapping(value = "/print-test")
    public ResponseEntity<String> printTest(
            @RequestParam(defaultValue = "FT 2024/100") String invoiceNo,
            @RequestParam(defaultValue = "1000.00") double totalAmount,
            @RequestParam(required = false) String printerName) {
        
        try {
            InvoiceReportData header = new InvoiceReportData(
                    invoiceNo,
                    LocalDateTime.now(),
                    LocalDateTime.now(),
                    "",
                    "Cliente Consumidor Final",
                    "999999999",
                    BigDecimal.valueOf(totalAmount * 0.86),
                    BigDecimal.valueOf(totalAmount * 0.14),
                    BigDecimal.valueOf(totalAmount),
                    BigDecimal.ZERO, // totalDiscount
                    BigDecimal.ZERO, // totalRetencao
                    "HASH_TESTE_TERMICA_SIMULADO",
                    "HASH_ANTERIOR"
            );

            List<InvoiceItemData> items = List.of(
                    new InvoiceItemData("P001", "Coca Cola", new BigDecimal("2"), "un", new BigDecimal("250.00"), new BigDecimal("14"), new BigDecimal("500.00"), null, null),
                    new InvoiceItemData("P002", "Pão", new BigDecimal("5"), "un", new BigDecimal("50.00"), new BigDecimal("14"), new BigDecimal("250.00"), null, null),
                    new InvoiceItemData("P003", "Manteiga", new BigDecimal("1"), "un", new BigDecimal("250.00"), new BigDecimal("14"), new BigDecimal("250.00"), null, null)
            );

            reportService.printInvoiceThermal(header, items, printerName);
            
            return ResponseEntity.ok("Comando de impressão enviado para: " + (printerName != null ? printerName : "Impressora Padrão"));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro na impressão: " + e.getMessage());
        }
    }
}

package ao.allon.kubata.faturacao.service.barcode;

import ao.allon.kubata.faturacao.service.agt.AGTInvoiceData;
import ao.allon.kubata.faturacao.service.agt.AGTService;
import ao.allon.kubata.faturacao.service.agt.AGTServiceImpl;
import ao.allon.kubata.faturacao.service.barcode.BarcodeTypeEnum;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;

class BarcodeServiceTest {

    private final BarcodeService barcodeService = new BarcodeServiceImpl();
    private final AGTService agtService = new AGTServiceImpl();

    @Test
    void testBarcodeGeneration() throws IOException {
        String content = "123456789012";
        byte[] image = barcodeService.generateBarcodeBytes(content, BarcodeTypeEnum.EAN_13, 200, 100);
        Assertions.assertNotNull(image);
        Assertions.assertTrue(image.length > 0);
    }

    @Test
    void testAGTFormat() {
        AGTInvoiceData data = new AGTInvoiceData(
                "FT 2024/1",
                LocalDateTime.of(2024, 1, 1, 10, 0),
                LocalDateTime.of(2024, 1, 1, 10, 0, 5),
                "999999999",
                new BigDecimal("1000.50"),
                BigDecimal.ZERO,
                "PREVIOUS",
                "HASH_BASE64_STRING_EXAMPLE"
        );

        String formatted = agtService.formatQRCodeData("123456789", data);
        
        // O formato actual é: NIF_Emitente;NIF_Cliente;TipoDoc;NumDoc;Data;Total;Hash
        Assertions.assertTrue(formatted.startsWith("123456789;999999999;FT;FT 2024/1;2024-01-01;1000.50;"));
        Assertions.assertTrue(formatted.endsWith("HASH_BASE64_STRING_EXAMPLE"));
    }

    @Test
    void testValidation() {
        Assertions.assertTrue(barcodeService.validateContent("1234567890123", BarcodeTypeEnum.EAN_13));
        Assertions.assertFalse(barcodeService.validateContent("ABC", BarcodeTypeEnum.EAN_13));
    }
}

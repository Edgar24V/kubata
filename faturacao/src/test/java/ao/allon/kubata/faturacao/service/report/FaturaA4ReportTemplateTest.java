package ao.allon.kubata.faturacao.service.report;

import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperReport;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class FaturaA4ReportTemplateTest {

    @Test
    void templateA4CompilaEDeclaraDadosLegaisDeNotaDeCredito() throws Exception {
        try (InputStream stream = getClass().getResourceAsStream("/reports/fatura_a4.jrxml")) {
            assertNotNull(stream, "O template A4 tem de estar disponível no classpath.");
            JasperReport report = JasperCompileManager.compileReport(stream);

            assertNotNull(report);
            assertTrue(Arrays.stream(report.getParameters())
                    .anyMatch(parameter -> "FATURA_NOTA_CREDITO_INFO".equals(parameter.getName())),
                    "O relatório deve declarar o parâmetro de finalidade, motivo e conhecimento do adquirente.");
        }
    }
}

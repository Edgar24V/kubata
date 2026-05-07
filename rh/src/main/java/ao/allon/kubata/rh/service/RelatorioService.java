package ao.allon.kubata.rh.service;

import ao.allon.kubata.rh.domain.Colaborador;
import ao.allon.kubata.rh.domain.Contrato;
import ao.allon.kubata.rh.domain.PedidoFerias;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.util.JRLoader;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimplePdfExporterConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RelatorioService {

    @Autowired
    private ColaboradorService colaboradorService;

    @Autowired
    private ContratoService contratoService;

    @Autowired
    private PedidoFeriasService pedidoFeriasService;

    /**
     * Gera relatório de lista de colaboradores ativos
     */
    public byte[] gerarRelatorioColaboradores() throws Exception {
        List<Colaborador> colaboradores = colaboradorService.findAtivos();
        
        // Parâmetros do relatório
        Map<String, Object> parametros = new HashMap<>();
        parametros.put("TITULO", "Lista de Colaboradores");
        parametros.put("DATA_EMISSAO", LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        parametros.put("TOTAL_REGISTROS", colaboradores.size());

        JasperReport jasperReport = loadOrCompileReport("colaboradores");
        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(colaboradores);
        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parametros, dataSource);

        return exportarParaPdf(jasperPrint);
    }

    /**
     * Gera relatório de contratos a expirar nos próximos dias
     */
    public byte[] gerarRelatorioContratosExpirar(int dias) throws Exception {
        LocalDate hoje = LocalDate.now();
        LocalDate dataLimite = hoje.plusDays(dias);
        List<Contrato> contratos = contratoService.findContratosExpirando(dataLimite);
        
        Map<String, Object> parametros = new HashMap<>();
        parametros.put("TITULO", "Contratos a Expirar");
        parametros.put("DATA_EMISSAO", hoje.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        parametros.put("DATA_LIMITE", dataLimite.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        parametros.put("DIAS", dias);
        parametros.put("TOTAL_REGISTROS", contratos.size());

        JasperReport jasperReport = loadOrCompileReport("contratos-expirar");
        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(contratos);
        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parametros, dataSource);

        return exportarParaPdf(jasperPrint);
    }

    /**
     * Gera mapa de férias para um período
     */
    public byte[] gerarRelatorioMapaFerias(LocalDate dataInicio, LocalDate dataFim) throws Exception {
        List<PedidoFerias> ferias = pedidoFeriasService.findByDataInicioBetween(dataInicio, dataFim);
        
        // Filtra apenas férias aprovadas para o mapa
        List<PedidoFerias> feriasAprovadas = ferias.stream()
            .filter(f -> f.getEstado() == PedidoFerias.EstadoPedido.APROVADO)
            .toList();
        
        Map<String, Object> parametros = new HashMap<>();
        parametros.put("TITULO", "Mapa de Férias");
        parametros.put("DATA_EMISSAO", LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        parametros.put("DATA_INICIO", dataInicio.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        parametros.put("DATA_FIM", dataFim.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        parametros.put("TOTAL_REGISTROS", feriasAprovadas.size());

        JasperReport jasperReport = loadOrCompileReport("mapa-ferias");
        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(feriasAprovadas);
        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parametros, dataSource);

        return exportarParaPdf(jasperPrint);
    }

    private JasperReport loadOrCompileReport(String reportBaseName) throws JRException {
        InputStream compiledStream = getClass().getResourceAsStream("/reports/" + reportBaseName + ".jasper");
        if (compiledStream != null) {
            return (JasperReport) JRLoader.loadObject(compiledStream);
        }

        InputStream jrxmlStream = getClass().getResourceAsStream("/reports/" + reportBaseName + ".jrxml");
        if (jrxmlStream == null) {
            throw new JRException("Template do relatório não encontrado: " + reportBaseName);
        }

        return JasperCompileManager.compileReport(jrxmlStream);
    }

    /**
     * Exporta JasperPrint para PDF
     */
    private byte[] exportarParaPdf(JasperPrint jasperPrint) throws JRException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        JRPdfExporter exporter = new JRPdfExporter();
        exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
        exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(outputStream));
        
        SimplePdfExporterConfiguration configuration = new SimplePdfExporterConfiguration();
        configuration.setCompressed(true);
        exporter.setConfiguration(configuration);
        
        exporter.exportReport();
        return outputStream.toByteArray();
    }

    /**
     * Gera um relatório simples em formato texto quando Jasper não está disponível
     */
    private byte[] gerarRelatorioSimples(List<?> dados, Map<String, Object> parametros) {
        StringBuilder sb = new StringBuilder();
        sb.append("=====================================\n");
        sb.append(parametros.getOrDefault("TITULO", "Relatório")).append("\n");
        sb.append("=====================================\n");
        sb.append("Data: ").append(parametros.getOrDefault("DATA_EMISSAO", "-")).append("\n");
        sb.append("Total de registros: ").append(parametros.getOrDefault("TOTAL_REGISTROS", 0)).append("\n");
        sb.append("-------------------------------------\n\n");
        
        for (Object item : dados) {
            sb.append(item.toString()).append("\n");
        }
        
        return sb.toString().getBytes();
    }
}

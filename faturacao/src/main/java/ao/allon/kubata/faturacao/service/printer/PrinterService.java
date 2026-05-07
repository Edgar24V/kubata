package ao.allon.kubata.faturacao.service.printer;

import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.export.JRPrintServiceExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimplePrintServiceExporterConfiguration;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import org.springframework.stereotype.Service;

@Service
public class PrinterService {

    private static final Logger LOGGER = Logger.getLogger(PrinterService.class.getName());

    public List<String> getAvailablePrinters() {
        PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
        List<String> printerNames = new ArrayList<>();
        for (PrintService service : services) {
            printerNames.add(service.getName());
        }
        return printerNames;
    }

    public PrintService getDefaultPrinter() {
        return PrintServiceLookup.lookupDefaultPrintService();
    }

    public void printReport(JasperPrint jasperPrint, String printerName) throws Exception {
        PrintService selectedService = null;

        if (printerName != null && !printerName.isEmpty()) {
            PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
            Optional<PrintService> serviceOpt = Arrays.stream(services)
                    .filter(s -> s.getName().equalsIgnoreCase(printerName))
                    .findFirst();
            if (serviceOpt.isPresent()) {
                selectedService = serviceOpt.get();
            }
        }

        if (selectedService == null) {
            selectedService = getDefaultPrinter();
            if (selectedService == null) {
                throw new NoPrinterFoundException("Nenhuma impressora encontrada ou selecionada.");
            }
            LOGGER.info("Impressora não especificada ou não encontrada. Usando padrão: " + selectedService.getName());
        }

        LOGGER.info("Iniciando impressão na impressora: " + selectedService.getName());

        JRPrintServiceExporter exporter = new JRPrintServiceExporter();
        exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
        
        SimplePrintServiceExporterConfiguration configuration = new SimplePrintServiceExporterConfiguration();
        configuration.setPrintService(selectedService);
        configuration.setDisplayPageDialog(false);
        configuration.setDisplayPrintDialog(false);
        
        exporter.setConfiguration(configuration);
        exporter.exportReport();
        
        LOGGER.info("Impressão concluída com sucesso.");
    }
}

package ao.allon.kubata.faturacao.service.report;

import ao.allon.kubata.faturacao.service.agt.AGTElectronicInvoiceService;
import ao.allon.kubata.faturacao.service.agt.AGTInvoiceData;
import ao.allon.kubata.faturacao.service.agt.AGTService;
import ao.allon.kubata.faturacao.service.barcode.BarcodeService;
import ao.allon.kubata.faturacao.service.barcode.BarcodeTypeEnum;
import ao.allon.kubata.faturacao.service.printer.PrinterService;
import ao.allon.kubata.faturacao.service.EmpresaService;
import ao.allon.kubata.faturacao.domain.Empresa;
import ao.allon.kubata.faturacao.domain.Fatura;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import ao.allon.kubata.faturacao.util.NumberToWordsConverter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import ao.allon.kubata.faturacao.domain.enums.TipoDocumento;

@Service
public class InvoiceReportService {

    private static final Logger LOGGER = Logger.getLogger(InvoiceReportService.class.getName());

    private final AGTService agtService;
    private final AGTElectronicInvoiceService agtElectronicInvoiceService;
    private final BarcodeService barcodeService;
    private final PrinterService printerService;
    private final EmpresaService empresaService;
    private final ao.allon.kubata.faturacao.repository.FaturaRepository faturaRepository;

    public InvoiceReportService(AGTService agtService, AGTElectronicInvoiceService agtElectronicInvoiceService, BarcodeService barcodeService, PrinterService printerService, EmpresaService empresaService, ao.allon.kubata.faturacao.repository.FaturaRepository faturaRepository) {
        this.agtService = agtService;
        this.agtElectronicInvoiceService = agtElectronicInvoiceService;
        this.barcodeService = barcodeService;
        this.printerService = printerService;
        this.empresaService = empresaService;
        this.faturaRepository = faturaRepository;
    }

    /**
     * Gera o relatório da fatura em formato PDF (A4).
     */
    public byte[] generateInvoicePdf(InvoiceReportData invoiceData, List<InvoiceItemData> items) throws Exception {
        JasperPrint jasperPrint = prepareJasperPrint("/reports/invoice_agt.jrxml", invoiceData, items);
        return JasperExportManager.exportReportToPdf(jasperPrint);
    }

    /**
     * Imprime a fatura em impressora térmica (80mm).
     *
     * @param invoiceData Dados da fatura.
     * @param items       Itens da fatura.
     * @param printerName Nome da impressora (opcional). Se null, usa a padrão.
     */
    public void printInvoiceThermal(InvoiceReportData invoiceData, List<InvoiceItemData> items, String printerName) throws Exception {
        LOGGER.info("Iniciando impressão térmica para fatura: " + invoiceData.invoiceNo());
        JasperPrint jasperPrint = prepareJasperPrint("/reports/invoice_thermal_80mm.jrxml", invoiceData, items);
        printerService.printReport(jasperPrint, printerName);
    }

    public java.util.List<String> getAvailablePrinters() {
        return printerService.getAvailablePrinters();
    }

    public String getDefaultPrinterName() {
        javax.print.PrintService s = printerService.getDefaultPrinter();
        return s != null ? s.getName() : null;
    }

    public JasperPrint createInvoiceThermalPrint(InvoiceReportData invoiceData, List<InvoiceItemData> items) throws Exception {
        return prepareJasperPrint("/reports/invoice_thermal_80mm.jrxml", invoiceData, items);
    }

    public void printJasper(JasperPrint jasperPrint, String printerName) throws Exception {
        printerService.printReport(jasperPrint, printerName);
    }

    private JasperPrint prepareJasperPrint(String templatePath, InvoiceReportData invoiceData, List<InvoiceItemData> items) throws Exception {
        Empresa empresa = empresaService.getDadosEmpresa();
        
        // Obter entidade Fatura para verificar campos como modoFormacao
        Fatura f = faturaRepository.findWithDetailsByNumero(invoiceData.invoiceNo()).orElse(null);

        // 1. Carregar Template
        InputStream reportStream = getClass().getResourceAsStream(templatePath);
        if (reportStream == null) {
            throw new FileNotFoundException("Template não encontrado: " + templatePath);
        }
        JasperReport jasperReport = JasperCompileManager.compileReport(reportStream);

        // 2. Preparar dados para o QR Code e Hash
        AGTInvoiceData agtData = new AGTInvoiceData(
                invoiceData.invoiceNo(),
                invoiceData.invoiceDate(),
                invoiceData.systemEntryDate(),
                invoiceData.customerNif(),
                invoiceData.totalGross(),
                invoiceData.totalTax(),
                invoiceData.previousHash(),
                invoiceData.hash()
        );

        String nifEmpresa = empresa.getNif() != null ? empresa.getNif() : "999999999";
        String qrContent = agtService.formatQRCodeData(nifEmpresa, agtData);
        BufferedImage qrCodeImage = barcodeService.generateBarcode(qrContent, BarcodeTypeEnum.QR_CODE, 200, 200);

        // 3. Parâmetros do Relatório
        Map<String, Object> parameters = new HashMap<>();

        String nomeEmpresa = empresa.getNome() != null ? empresa.getNome() : "Empresa";
        String enderecoEmpresa = empresa.getEndereco() != null ? empresa.getEndereco() : "";
        String cidadeEmpresa = empresa.getCidade() != null ? empresa.getCidade() : "";
        String telefoneEmpresa = empresa.getTelefone() != null ? empresa.getTelefone() : "";
        String emailEmpresa = empresa.getEmail() != null ? empresa.getEmail() : "";
        String caeEmpresa = empresa.getRegimeIva() != null ? empresa.getRegimeIva() : "";

        String enderecoCompleto = enderecoEmpresa;
        if (!cidadeEmpresa.isBlank()) {
            enderecoCompleto = enderecoCompleto.isBlank()
                    ? cidadeEmpresa
                    : enderecoCompleto + ", " + cidadeEmpresa;
        }

        parameters.put("companyName", nomeEmpresa);
        parameters.put("companyNif", nifEmpresa);
        parameters.put("companyAddress", enderecoCompleto);
        parameters.put("companyPhone", telefoneEmpresa);
        parameters.put("companyEmail", emailEmpresa);
        parameters.put("companyCae", caeEmpresa);

        // Logo (se existir)
        try {
            byte[] logoBytes = empresa.getLogotipo();
            if (logoBytes != null && logoBytes.length > 0) {
                parameters.put("companyLogoImage", ImageIO.read(new ByteArrayInputStream(logoBytes)));
            } else {
                parameters.put("companyLogoImage", null);
            }
        } catch (Exception ignore) {
            parameters.put("companyLogoImage", null);
        }

        parameters.put("customerName", invoiceData.customerName());
        parameters.put("customerNif", invoiceData.customerNif());
        parameters.put("customerAddress", "");
        parameters.put("customerCity", "");
        parameters.put("customerCountry", "AO");
        
        parameters.put("invoiceNo", invoiceData.invoiceNo());
        parameters.put("documentNo", invoiceData.invoiceNo());
        
        parameters.put("invoiceDate", invoiceData.invoiceDate().toString());
        parameters.put("documentDate", invoiceData.invoiceDate().toString());
        
        parameters.put("invoiceDueDate", "");
        
        parameters.put("invoiceCurrency", "AOA");
        parameters.put("currencyCode", "AOA");
        
        parameters.put("operator", invoiceData.operator() != null ? invoiceData.operator() : "");
        parameters.put("terminal", "");
        
        parameters.put("serie", "");
        parameters.put("series", "");

        // Software Info
        parameters.put("softwareProductId", "Kubata Software");
        parameters.put("softwareVersion", "1.0");

        // Estado visual (default)
        parameters.put("estadoDocumentoVisual", "ORIGINAL");
        parameters.put("documentStatus", "N"); // Normal

        // Retificação via NC/ND (conforme recomendação AGT: documento original mantém estado SAF-T válido)
        parameters.put("retificado", Boolean.FALSE);
        parameters.put("retificadoPor", "");

        // Campos recomendados pela AGT (quando disponíveis)
        parameters.put("SYSTEM_ENTRY_DATE", invoiceData.systemEntryDate() != null ? invoiceData.systemEntryDate().toString() : "");
        parameters.put("systemEntryDate", invoiceData.systemEntryDate() != null ? invoiceData.systemEntryDate().toString() : "");
        
        parameters.put("DOCUMENT_STATUS", "N");
        parameters.put("DOCUMENT_CANCEL_REASON", "");
        parameters.put("documentCancelReason", "");
        
        parameters.put("REJECTED_DOCUMENT_NO", "");
        parameters.put("rejectedDocumentNo", "");
        
        parameters.put("JWS_DOCUMENT_SIGNATURE", "");
        parameters.put("jwsDocumentSignature", "");

        // Pagamento (quando disponível)
        parameters.put("paymentMethod", "");
        parameters.put("paymentBank", "");
        parameters.put("paymentIban", "");
        parameters.put("paymentRef", "");
        parameters.put("paymentTerms", "");

        // Definir título e observação com base no prefixo do número (código do TipoDocumento)
        String docTitle = "FACTURA-RECIBO";
        String docType = "FR";
        String footerNote = null;
        if (invoiceData.invoiceNo() != null && invoiceData.invoiceNo().contains(" ")) {
            String code = invoiceData.invoiceNo().substring(0, invoiceData.invoiceNo().indexOf(' ')).trim();
            docType = code;
            switch (code) {
                case "FT": docTitle = "FACTURA"; break;
                case "FR": docTitle = "FACTURA-RECIBO"; break;
                case "VD": docTitle = "VENDA A DINHEIRO"; break;
                case "RC": docTitle = "RECIBO"; break;
                case "NC": docTitle = "NOTA DE CRÉDITO"; break;
                case "ND": docTitle = "NOTA DE DÉBITO"; break;
                case "GR": docTitle = "GUIA DE REMESSA"; footerNote = "Documento de transporte — sem valor comercial"; break;
                case "GT": docTitle = "GUIA DE TRANSPORTE"; footerNote = "Documento de transporte — sem valor comercial"; break;
                case "AF": docTitle = "AUTOFACTURA"; break;
                case "EC": docTitle = "ENCOMENDA"; footerNote = "Documento sem validade fiscal"; break;
                case "OR": docTitle = "ORÇAMENTO"; footerNote = "Documento sem validade fiscal — Proposta sujeita a alteração"; break;
                case "PP": docTitle = "FATURA PRÓ-FORMA"; footerNote = "Documento sem validade fiscal"; break;
                case "DC": docTitle = "DOCUMENTO DE CONFERÊNCIA"; footerNote = "Este documento não substitui Factura ou Factura-Recibo"; break;
                default: break;
            }
        }
        parameters.put("documentTitle", docTitle);
        parameters.put("documentType", docType);
        parameters.put("footerNote", footerNote != null ? footerNote : "");

        parameters.put("hashFull", invoiceData.hash());
        parameters.put("hashControl", (invoiceData.hash() != null && invoiceData.hash().length() >= 4) ? invoiceData.hash().substring(0, 4) : "XXXX");

        String softVal = empresa.getSoftwareValidationNumber();
        if (softVal == null || softVal.isBlank() || softVal.contains("0000")) {
            softVal = "0000"; // Placeholder para desenvolvimento
        }
        parameters.put("softwareValidationNumber", softVal);
        
        String validationText;
        if (invoiceData.hash() != null && !invoiceData.hash().isEmpty()) {
            validationText = "Processado por programa validado n.º " + softVal + "/AGT";
        } else {
            validationText = "Emitido por programa validado n.º " + softVal + "/AGT";
        }
        parameters.put("programaValidadoNo", softVal);
        parameters.put("validationText", validationText);

        if (f != null && Boolean.TRUE.equals(f.getModoFormacao())) {
            parameters.put("trainingModeText", "Documento emitido para fins de Formação");
        } else {
            parameters.put("trainingModeText", "");
        }

        // Complementar parâmetros a partir da entidade Fatura (quando existe)
        if (f != null) {
            if (f.getCliente() != null) {
                String morada = f.getCliente().getEndereco() != null ? f.getCliente().getEndereco() : "";
                String cidade = f.getCliente().getCidade() != null ? f.getCliente().getCidade() : "";
                parameters.put("customerAddress", morada);
                parameters.put("customerCity", cidade);
            }

            parameters.put("invoiceDueDate", f.getDataVencimento() != null ? f.getDataVencimento().toString() : "");
            
            String status = f.getDocumentStatus() != null ? f.getDocumentStatus().getCodigo() : "N";
            parameters.put("DOCUMENT_STATUS", status);
            parameters.put("documentStatus", status);
            
            String cancelReason = f.getDocumentCancelReason() != null ? f.getDocumentCancelReason().getCodigo() : "";
            parameters.put("DOCUMENT_CANCEL_REASON", cancelReason);
            parameters.put("documentCancelReason", cancelReason);
            
            String rejNo = f.getRejectedDocumentNo() != null ? f.getRejectedDocumentNo() : "";
            parameters.put("REJECTED_DOCUMENT_NO", rejNo);
            parameters.put("rejectedDocumentNo", rejNo);
            
            String sig = f.getJwsDocumentSignature() != null ? f.getJwsDocumentSignature() : "";
            parameters.put("JWS_DOCUMENT_SIGNATURE", sig);
            parameters.put("jwsDocumentSignature", sig);
            
            String sysDate = f.getSystemEntryDate() != null ? f.getSystemEntryDate().toString() : (invoiceData.systemEntryDate() != null ? invoiceData.systemEntryDate().toString() : "");
            parameters.put("SYSTEM_ENTRY_DATE", sysDate);
            parameters.put("systemEntryDate", sysDate);

            // Série (tentativa simples: parte antes do '/'
            if (f.getNumero() != null) {
                String n = f.getNumero();
                String serie = n.contains("/") ? n.substring(0, n.indexOf('/')) : "";
                parameters.put("serie", serie);
                parameters.put("series", serie);
            }

            // Operador e Terminal (se existirem)
            try {
                if (f.getUsuario() != null) {
                    String operador = (f.getUsuario().getNome() != null && !f.getUsuario().getNome().isBlank())
                            ? f.getUsuario().getNome()
                            : (f.getUsuario().getUsername() != null ? f.getUsuario().getUsername() : "");
                    
                    if (!operador.isEmpty()) {
                        parameters.put("operator", operador);
                    }
                }
            } catch (Exception ignore) {
                // não interromper impressão
            }

            // Estado visual do documento
            try {
                String vis = "ORIGINAL";

                // Se estiver anulado/cancelado, forçar ANULADO
                if (f.getDocumentStatus() != null && "A".equalsIgnoreCase(f.getDocumentStatus().getCodigo())) {
                    vis = "ANULADO";
                } else if (f.getStatus() == ao.allon.kubata.faturacao.domain.enums.StatusFatura.RASCUNHO) {
                    vis = "RASCUNHO";
                } else if (f.getEstadoDocumento() != null) {
                    // Mapear enum para os textos do layout
                    String enumName = f.getEstadoDocumento().name();
                    if ("SEGUNDA_VIA".equalsIgnoreCase(enumName) || "SEGUNDA_VIA".equalsIgnoreCase(enumName.replace(' ', '_'))) {
                        vis = "2ª VIA";
                    } else if ("RETIFICADO".equalsIgnoreCase(enumName)) {
                        vis = "RETIFICADO";
                    } else if ("ANULADO".equalsIgnoreCase(enumName)) {
                        vis = "ANULADO";
                    } else if ("RASCUNHO".equalsIgnoreCase(enumName)) {
                        vis = "RASCUNHO";
                    } else {
                        vis = "ORIGINAL";
                    }
                }

                // Se houver Notas de Crédito/Débito que referenciam este documento, sinalizar RETIFICADO
                try {
                    if (f.getTipoDocumento() == TipoDocumento.FATURA || f.getTipoDocumento() == TipoDocumento.FATURA_RECIBO) {
                        long qtd = faturaRepository.countByFaturaReferenciaIdAndTipoDocumentoIn(
                                f.getId(),
                                java.util.List.of(TipoDocumento.NOTA_CREDITO, TipoDocumento.NOTA_DEBITO)
                        );
                        if (qtd > 0) {
                            vis = "RETIFICADO";
                            parameters.put("retificado", Boolean.TRUE);
                            java.util.List<Fatura> notas = faturaRepository.findByFaturaReferenciaIdAndTipoDocumentoInOrderByDataEmissaoAsc(
                                    f.getId(),
                                    java.util.List.of(TipoDocumento.NOTA_CREDITO, TipoDocumento.NOTA_DEBITO)
                            );
                            if (notas != null && !notas.isEmpty() && notas.get(0) != null) {
                                Fatura n0 = notas.get(0);
                                String info = (n0.getTipoDocumento() != null ? n0.getTipoDocumento().getCodigo() : "")
                                        + " " + (n0.getNumero() != null ? n0.getNumero() : "");
                                parameters.put("retificadoPor", info.trim());
                            }
                        }
                    }
                } catch (Exception ignore) {
                    // manter vis calculado
                }

                parameters.put("estadoDocumentoVisual", vis);
            } catch (Exception ignore) {
                parameters.put("estadoDocumentoVisual", "ORIGINAL");
            }
            try {
                parameters.put("terminal", "");
            } catch (Exception ignore) {
                // não interromper impressão
            }

            // Pagamentos (primeiro pagamento, se existir)
            if (f.getPagamentos() != null && !f.getPagamentos().isEmpty() && f.getPagamentos().get(0) != null) {
                var p = f.getPagamentos().get(0);
                try {
                    parameters.put("paymentMethod", p.getMetodo() != null ? p.getMetodo().toString() : "");
                } catch (Exception ignore) {
                    parameters.put("paymentMethod", "");
                }
            }

            // Referência a documento original (NC, ND, RC)
            if (f.getFaturaReferencia() != null) {
                parameters.put("referenciaDoc", f.getFaturaReferencia().getNumero());
                parameters.put("referenciaData", f.getFaturaReferencia().getDataEmissao() != null ? f.getFaturaReferencia().getDataEmissao().toString() : "");
                parameters.put("referenciaTotal", f.getFaturaReferencia().getTotal());
            }

            // Dados de transporte (GR, GT)
            parameters.put("localCarga", f.getLocalCarga() != null ? f.getLocalCarga() : "");
            parameters.put("localDescarga", f.getLocalDescarga() != null ? f.getLocalDescarga() : "");
            parameters.put("dataCarga", f.getDataCarga() != null ? f.getDataCarga().toString() : "");
            parameters.put("dataDescarga", f.getDataDescarga() != null ? f.getDataDescarga().toString() : "");
            parameters.put("matriculaViatura", f.getMatriculaViatura() != null ? f.getMatriculaViatura() : "");
            parameters.put("motorista", f.getMotorista() != null ? f.getMotorista() : "");
        }

        parameters.put("totalDiscount", invoiceData.totalDiscount());
        parameters.put("totalRetencao", invoiceData.totalRetencao());
        parameters.put("withholdingTaxAmount", invoiceData.totalRetencao());
        parameters.put("totalExtenso", NumberToWordsConverter.convertToExtenso(invoiceData.totalGross()));
        parameters.put("qrCodeImage", qrCodeImage);

        if (f != null && f.getAgtValidationCode() != null && !f.getAgtValidationCode().isEmpty()) {
            parameters.put("agtValidationCode", f.getAgtValidationCode());
            parameters.put("submissionStatus", "Fatura Eletrónica submetida com sucesso");
        } else {
            parameters.put("agtValidationCode", "");
            parameters.put("submissionStatus", "");
        }

        parameters.put("totalNet", invoiceData.totalNet());
        parameters.put("totalTax", invoiceData.totalTax());
        parameters.put("totalGross", invoiceData.totalGross());

        // 4. Fonte de Dados
        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(items);

        // 5. Preencher
        return JasperFillManager.fillReport(jasperReport, parameters, dataSource);
    }
}

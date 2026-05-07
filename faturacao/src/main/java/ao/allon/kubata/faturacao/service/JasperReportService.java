package ao.allon.kubata.faturacao.service;

import ao.allon.kubata.core.service.ContabilidadeService.BalanceteItemDTO;
import ao.allon.kubata.faturacao.ui.reports.JasperViewerPane;
import ao.allon.kubata.faturacao.ui.util.AlertUtils;
import ao.allon.kubata.faturacao.ui.modal.ModalService;
import atlantafx.base.theme.Styles;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.FileChooser;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.design.*;
import net.sf.jasperreports.engine.export.JRXlsExporter;
import net.sf.jasperreports.engine.type.HorizontalTextAlignEnum;
import net.sf.jasperreports.engine.type.VerticalTextAlignEnum;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimpleXlsReportConfiguration;
import org.springframework.stereotype.Service;

import org.springframework.core.io.ResourceLoader;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import ao.allon.kubata.faturacao.domain.enums.TipoDocumento;
import ao.allon.kubata.faturacao.repository.FaturaRepository;

@Service
public class JasperReportService {

    private final EmpresaService empresaService;
    private final ResourceLoader resourceLoader;
    private final ModalService modalService;
    private final FaturaRepository faturaRepository;
    private final ReciboService reciboService;

    public JasperReportService(EmpresaService empresaService, ResourceLoader resourceLoader, ModalService modalService, FaturaRepository faturaRepository, ReciboService reciboService) {
        this.empresaService = empresaService;
        this.resourceLoader = resourceLoader;
        this.modalService = modalService;
        this.faturaRepository = faturaRepository;
        this.reciboService = reciboService;
    }

    // --- NOVO MÉTODO CENTRALIZADO PARA EXIBIR RELATÓRIOS ---

    public void showReport(JasperPrint jasperPrint) {
        if (jasperPrint == null || jasperPrint.getPages() == null || jasperPrint.getPages().isEmpty()) {
            AlertUtils.showErrorAlert("Relatório Vazio", "Não há dados para gerar este relatório.");
            return;
        }

        try {
            JasperViewerPane viewerPane = new JasperViewerPane(jasperPrint);

            // Cache para as páginas já renderizadas
            final Map<Integer, Image> renderedPages = new HashMap<>();
            final float dpi = 150f;

            // Fornece a lógica de renderização para o painel do visualizador
            viewerPane.setPageRenderer(pageIndex -> {
                return new Task<Image>() {
                    @Override
                    protected Image call() throws Exception {
                        if (renderedPages.containsKey(pageIndex)) {
                            return renderedPages.get(pageIndex);
                        }
                        BufferedImage image = (BufferedImage) JasperPrintManager.printPageToImage(jasperPrint, pageIndex, dpi);
                        Image fxImage = SwingFXUtils.toFXImage(image, null);
                        renderedPages.put(pageIndex, fxImage);
                        return fxImage;
                    }
                };
            });

            // Configurar ações de exportação e impressão
            viewerPane.getBtnImprimir().setOnAction(e -> {
                try {
                    JasperPrintManager.printReport(jasperPrint, true);
                } catch (JRException ex) {
                    AlertUtils.showExceptionAlert("Erro de Impressão", "Não foi possível imprimir o relatório.", ex);
                }
            });

            viewerPane.getItemExportarPDF().setOnAction(e -> exportToFile(jasperPrint, "pdf"));
            viewerPane.getItemExportarExcel().setOnAction(e -> exportToFile(jasperPrint, "xls"));

            modalService.create()
                    .title("Visualizador de Relatórios - " + jasperPrint.getName())
                    .content(viewerPane)
                    .dynamicSize()
                    .buildAndShow();

        } catch (Exception e) {
            AlertUtils.showExceptionAlert("Erro Crítico no Visualizador", "Ocorreu um erro inesperado ao tentar abrir o visualizador de relatórios.", e);
        }
    }

    public JasperPrint prepararVendasPorDia(List<Object[]> data, LocalDate inicio, LocalDate fim) {
        try {
            Map<String, Object> params = getEmpresaParams();
            params.put("PERIOD", inicio.toString() + " a " + fim.toString());

            List<RowTax> rows = new ArrayList<>();
            for (Object[] row : data) {
                if (row == null || row.length < 3) continue;
                Object d0 = row[0];
                String dataStr = d0 != null ? d0.toString() : "";
                BigDecimal total = row[1] instanceof BigDecimal ? (BigDecimal) row[1] : BigDecimal.ZERO;
                BigDecimal iva = row[2] instanceof BigDecimal ? (BigDecimal) row[2] : BigDecimal.ZERO;
                rows.add(new RowTax(dataStr, total, iva));
            }

            InputStream reportStream = resourceLoader.getResource("classpath:reports/tax_report.jrxml").getInputStream();
            JasperReport jr = JasperCompileManager.compileReport(reportStream);
            JasperPrint jp = JasperFillManager.fillReport(jr, params, new JRBeanCollectionDataSource(rows));
            jp.setName("Vendas_Por_Dia");
            return jp;
        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar Vendas por Dia: " + e.getMessage(), e);
        }
    }

    public JasperPrint prepararMapaImpostosProfissional(Collection<ao.allon.kubata.faturacao.domain.Fatura> faturas, LocalDate inicio, LocalDate fim, String usuario) {
        try {
            Map<String, Object> params = getEmpresaParams();
            params.put("PERIODO_INICIO", inicio != null ? inicio.toString() : "");
            params.put("PERIODO_FIM", fim != null ? fim.toString() : "");
            params.put("USUARIO", usuario != null ? usuario : "");
            params.put("DATA_EMISSAO", new java.util.Date());

            Collection<ao.allon.kubata.faturacao.domain.Fatura> ftrs = filtrarPorPeriodo(faturas, inicio, fim);
            List<RowMapaImpostos> rows = buildMapaImpostosRows(ftrs);

            int totalFaturas = (int) ftrs.stream().filter(f -> f != null).count();
            BigDecimal totalBase = rows.stream().map(RowMapaImpostos::getBase).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalIva = rows.stream().map(RowMapaImpostos::getIva).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalGeral = rows.stream().map(RowMapaImpostos::getTotal).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
            params.put("TOTAL_FATURAS", totalFaturas);
            params.put("TOTAL_GERAL_BASE", totalBase);
            params.put("TOTAL_GERAL_IMPOSTO", totalIva);
            params.put("TOTAL_GERAL", totalGeral);

            InputStream reportStream = resourceLoader.getResource("classpath:reports/mapa_impostos_profissional.jrxml").getInputStream();
            JasperReport jr = JasperCompileManager.compileReport(reportStream);
            JasperPrint jp = JasperFillManager.fillReport(jr, params, new JRBeanCollectionDataSource(rows));
            jp.setName("Mapa_Impostos_" + (inicio != null ? inicio : "") + "_a_" + (fim != null ? fim : ""));
            return jp;
        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar Mapa de Impostos Profissional: " + e.getMessage(), e);
        }
    }

    public JasperPrint prepararRelatorioImpostosProfissional(Collection<ao.allon.kubata.faturacao.domain.Fatura> faturas, LocalDate inicio, LocalDate fim, String usuario) {
        try {
            Map<String, Object> params = getEmpresaParams();
            params.put("PERIODO_INICIO", inicio != null ? inicio.toString() : "");
            params.put("PERIODO_FIM", fim != null ? fim.toString() : "");
            params.put("USUARIO", usuario != null ? usuario : "");
            params.put("DATA_EMISSAO", new java.util.Date());

            Collection<ao.allon.kubata.faturacao.domain.Fatura> ftrs = filtrarPorPeriodo(faturas, inicio, fim);
            List<RowRelatorioImpostos> rows = buildRelatorioImpostosRows(ftrs);

            InputStream reportStream = resourceLoader.getResource("classpath:reports/relatorio_impostos_profissional.jrxml").getInputStream();
            JasperReport jr = JasperCompileManager.compileReport(reportStream);
            JasperPrint jp = JasperFillManager.fillReport(jr, params, new JRBeanCollectionDataSource(rows));
            jp.setName("Relatorio_Impostos_" + (inicio != null ? inicio : "") + "_a_" + (fim != null ? fim : ""));
            return jp;
        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar Relatório de Impostos Profissional: " + e.getMessage(), e);
        }
    }

    public JasperPrint prepararRelatorioRetencoesFonteProfissional(Collection<ao.allon.kubata.faturacao.domain.Fatura> faturas, LocalDate inicio, LocalDate fim, String usuario) {
        try {
            Map<String, Object> params = getEmpresaParams();
            params.put("PERIODO_INICIO", inicio != null ? inicio.toString() : "");
            params.put("PERIODO_FIM", fim != null ? fim.toString() : "");
            params.put("USUARIO", usuario != null ? usuario : "");
            params.put("DATA_EMISSAO", new java.util.Date());

            Collection<ao.allon.kubata.faturacao.domain.Fatura> ftrs = filtrarPorPeriodo(faturas, inicio, fim);
            List<RowRetencaoFonteReport> rows = buildRetencoesFonteRows(ftrs);
            BigDecimal totalRetido = rows.stream().map(RowRetencaoFonteReport::getValorRetido).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalOperacoes = rows.stream().map(RowRetencaoFonteReport::getValorBase).reduce(BigDecimal.ZERO, BigDecimal::add);
            params.put("TOTAL_RETIDO", totalRetido);
            params.put("TOTAL_OPERACOES", totalOperacoes);

            InputStream reportStream = resourceLoader.getResource("classpath:reports/relatorio_retencoes_fonte_profissional.jrxml").getInputStream();
            JasperReport jr = JasperCompileManager.compileReport(reportStream);
            JasperPrint jp = JasperFillManager.fillReport(jr, params, new JRBeanCollectionDataSource(rows));
            jp.setName("Relatorio_Retencoes_" + (inicio != null ? inicio : "") + "_a_" + (fim != null ? fim : ""));
            return jp;
        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar Relatório de Retenções na Fonte Profissional: " + e.getMessage(), e);
        }
    }

    private void exportToFile(JasperPrint jasperPrint, String format) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Salvar Relatório como " + format.toUpperCase());
        String defaultFileName = jasperPrint.getName().replaceAll("[^a-zA-Z0-9.-]", "_") + "." + format;
        fileChooser.setInitialFileName(defaultFileName);
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(format.toUpperCase() + " Files", "*." + format));

        File file = fileChooser.showSaveDialog(null);
        if (file != null) {
            try {
                if ("pdf".equalsIgnoreCase(format)) {
                    exportToPdf(jasperPrint, file);
                } else if ("xls".equalsIgnoreCase(format)) {
                    exportToXls(jasperPrint, file);
                }
                AlertUtils.showInfoAlert("Exportação Concluída", "O relatório foi salvo com sucesso em: " + file.getAbsolutePath());
            } catch (Exception e) {
                AlertUtils.showExceptionAlert("Erro de Exportação", "Não foi possível exportar o relatório.", e);
            }
        }
    }


    private Map<String, Object> getEmpresaParams() {
        ao.allon.kubata.faturacao.domain.Empresa empresa = empresaService.getDadosEmpresa();
        Map<String, Object> params = new HashMap<>();
        
        // Dados da Empresa
        params.put("EMPRESA_NOME", empresa.getNome());
        params.put("EMPRESA_NIF", empresa.getNif());
        params.put("EMPRESA_ENDERECO", empresa.getEndereco());
        params.put("EMPRESA_TELEFONE", empresa.getTelefone());
        params.put("EMPRESA_EMAIL", empresa.getEmail());
        params.put("EMPRESA_WEBSITE", empresa.getWebsite());
        if (empresa.getLogotipo() != null && empresa.getLogotipo().length > 0) {
            params.put("EMPRESA_LOGO", new java.io.ByteArrayInputStream(empresa.getLogotipo()));
        }

        // AGT: Nº do Programa Validado
        params.put("PROGRAMA_VALIDADO_NO", empresa.getSoftwareValidationNumber() != null ? empresa.getSoftwareValidationNumber() : "");
        
        // Dados Bancários
        params.put("BANCO_NOME_1", empresa.getBanco1());
        params.put("BANCO_IBAN_1", empresa.getIban1());
        params.put("BANCO_NOME_2", empresa.getBanco2());
        params.put("BANCO_IBAN_2", empresa.getIban2());
        
        return params;
    }

    // --- MÉTODOS REATORADOS PARA RETORNAR JasperPrint ---

    public JasperPrint prepararBalancete(List<BalanceteItemDTO> dados, LocalDate dataCorte) {
        try {
            Map<String, Object> params = getEmpresaParams();
            params.put("DATA_CORTE", dataCorte.toString());

            InputStream reportStream = resourceLoader.getResource("classpath:reports/balancete.jrxml").getInputStream();
            JasperReport jr = JasperCompileManager.compileReport(reportStream);

            return JasperFillManager.fillReport(jr, params, new JRBeanCollectionDataSource(dados));
        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar Balancete: " + e.getMessage(), e);
        }
    }

    public JasperPrint prepararDRE(List<BalanceteItemDTO> dados, LocalDate inicio, LocalDate fim) {
        try {
            Map<String, Object> params = getEmpresaParams();
            params.put("PERIODO", inicio.toString() + " a " + fim.toString());

            InputStream reportStream = resourceLoader.getResource("classpath:reports/dre.jrxml").getInputStream();
            JasperReport jr = JasperCompileManager.compileReport(reportStream);

            return JasperFillManager.fillReport(jr, params, new JRBeanCollectionDataSource(dados));
        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar DRE: " + e.getMessage(), e);
        }
    }

    public JasperPrint prepararFatura(ao.allon.kubata.faturacao.domain.Fatura fatura) {
        try {
            Map<String, Object> params = getEmpresaParams();

            // Dados da Fatura
            params.put("FATURA_NUMERO", fatura.getNumero());
            params.put("FATURA_DATA", fatura.getDataEmissao() != null ? fatura.getDataEmissao().toString() : "");
            params.put("FATURA_VENCIMENTO", fatura.getDataVencimento() != null ? fatura.getDataVencimento().toString() : "");
            params.put("FATURA_HASH", fatura.getHash());
            params.put("SYSTEM_ENTRY_DATE", fatura.getSystemEntryDate() != null ? fatura.getSystemEntryDate().toString() : "");
            params.put("DOCUMENT_STATUS", fatura.getDocumentStatus() != null ? fatura.getDocumentStatus().getCodigo() : "");
            params.put("DOCUMENT_CANCEL_REASON", fatura.getDocumentCancelReason() != null ? fatura.getDocumentCancelReason().getCodigo() : "");
            params.put("REJECTED_DOCUMENT_NO", fatura.getRejectedDocumentNo() != null ? fatura.getRejectedDocumentNo() : "");
            params.put("CUSTOMER_COUNTRY", fatura.getCustomerCountry() != null ? fatura.getCustomerCountry() : "AO");
            params.put("EAC_CODE", fatura.getEacCode() != null ? fatura.getEacCode() : "");
            params.put("JWS_DOCUMENT_SIGNATURE", fatura.getJwsDocumentSignature() != null ? fatura.getJwsDocumentSignature() : "");

            // RETIFICADO (conforme recomendação AGT): documento original mantém estado SAF-T,
            // e a retificação é indicada pela existência de NC/ND de referência.
            try {
                boolean retificado = false;
                String retificadoPor = "";

                if (fatura.getId() != null &&
                        (fatura.getTipoDocumento() == TipoDocumento.FATURA || fatura.getTipoDocumento() == TipoDocumento.FATURA_RECIBO)) {

                    long qtd = faturaRepository.countByFaturaReferenciaIdAndTipoDocumentoIn(
                            fatura.getId(),
                            java.util.List.of(TipoDocumento.NOTA_CREDITO, TipoDocumento.NOTA_DEBITO)
                    );
                    if (qtd > 0) {
                        retificado = true;
                        java.util.List<ao.allon.kubata.faturacao.domain.Fatura> notas = faturaRepository
                                .findByFaturaReferenciaIdAndTipoDocumentoInOrderByDataEmissaoAsc(
                                        fatura.getId(),
                                        java.util.List.of(TipoDocumento.NOTA_CREDITO, TipoDocumento.NOTA_DEBITO)
                                );
                        if (notas != null && !notas.isEmpty() && notas.get(0) != null) {
                            var n0 = notas.get(0);
                            String info = (n0.getTipoDocumento() != null ? n0.getTipoDocumento().getCodigo() : "")
                                    + " " + (n0.getNumero() != null ? n0.getNumero() : "");
                            retificadoPor = info.trim();
                        }
                    }
                }

                params.put("RETIFICADO", retificado);
                params.put("RETIFICADO_POR", retificadoPor);
            } catch (Exception ignore) {
                params.put("RETIFICADO", Boolean.FALSE);
                params.put("RETIFICADO_POR", "");
            }
            String titulo = "FATURA";
            String noteFooter = null;
            if (fatura.getTipoDocumento() != null) {
                String desc = fatura.getTipoDocumento().getDescricao();
                switch (desc) {
                    case "Fatura":
                        titulo = "FATURA";
                        break;
                    case "Fatura/Recibo":
                        titulo = "FATURA/RECIBO";
                        break;
                    case "Recibo":
                        titulo = "RECIBO";
                        break;
                    case "Nota de Crédito":
                        titulo = "NOTA DE CRÉDITO";
                        break;
                    case "Nota de Débito":
                        titulo = "NOTA DE DÉBITO";
                        break;
                    case "Guia de Transporte":
                        titulo = "GUIA DE TRANSPORTE";
                        noteFooter = "Documento de transporte";
                        break;
                    case "Guia de Remessa":
                        titulo = "GUIA DE REMESSA";
                        noteFooter = "Documento de transporte";
                        break;
                    case "Encomenda":
                        titulo = "ENCOMENDA";
                        noteFooter = "Documento sem validade fiscal";
                        break;
                    case "Orçamento":
                        titulo = "ORÇAMENTO";
                        noteFooter = "Documento sem validade fiscal";
                        break;
                    case "Fatura Pró-Forma":
                        titulo = "FATURA PRÓ-FORMA";
                        noteFooter = "Documento sem validade fiscal";
                        break;
                    default:
                        titulo = desc.toUpperCase();
                }
            }
            params.put("DOCUMENTO_TITULO", titulo);
            params.put("NOTE_FOOTER", noteFooter != null ? noteFooter : "");
            
            // Estado do documento conforme SAF-T-AO
            String estadoDoc = fatura.getEstadoDocumento() != null ? fatura.getEstadoDocumento().getDescricao() : "ORIGINAL";
            params.put("ESTADO_DOCUMENTO", estadoDoc);
            
            // Estado visual (mapeamento para o layout)
            String estadoVisual = "ORIGINAL";
            if (fatura.getDocumentStatus() != null && "A".equalsIgnoreCase(fatura.getDocumentStatus().getCodigo())) {
                estadoVisual = "ANULADO";
            } else if (fatura.getStatus() == ao.allon.kubata.faturacao.domain.enums.StatusFatura.RASCUNHO) {
                estadoVisual = "RASCUNHO";
            } else if (fatura.getEstadoDocumento() != null) {
                String enumName = fatura.getEstadoDocumento().name();
                if ("SEGUNDA_VIA".equalsIgnoreCase(enumName)) {
                    estadoVisual = "2ª VIA";
                } else if ("RETIFICADO".equalsIgnoreCase(enumName)) {
                    estadoVisual = "RETIFICADO";
                } else if ("ANULADO".equalsIgnoreCase(enumName)) {
                    estadoVisual = "ANULADO";
                } else if ("RASCUNHO".equalsIgnoreCase(enumName)) {
                    estadoVisual = "RASCUNHO";
                }
            }
            params.put("ESTADO_DOCUMENTO_VISUAL", estadoVisual);
            
            // Operador
            String operador = "";
            if (fatura.getUsuario() != null) {
                operador = (fatura.getUsuario().getNome() != null && !fatura.getUsuario().getNome().isBlank())
                        ? fatura.getUsuario().getNome()
                        : (fatura.getUsuario().getUsername() != null ? fatura.getUsuario().getUsername() : "");
            }
            params.put("OPERADOR", operador);
            
            // Totais (Formatados)
            params.put("FATURA_SUBTOTAL", String.format("Kz %.2f", fatura.getSubtotal()));
            params.put("FATURA_IVA", String.format("Kz %.2f", fatura.getIva()));
            params.put("FATURA_TOTAL", String.format("Kz %.2f", fatura.getTotal()));

            // Dados do Cliente
            if (fatura.getCliente() != null) {
                params.put("CLIENTE_NOME", fatura.getCliente().getNome());
                params.put("CLIENTE_NIF", fatura.getCliente().getNif());
                params.put("CLIENTE_ENDERECO", fatura.getCliente().getEndereco());
            } else {
                params.put("CLIENTE_NOME", "Consumidor Final");
                params.put("CLIENTE_NIF", "999999999");
            }

            // Carregar e compilar o relatório
            InputStream reportStream = resourceLoader.getResource("classpath:reports/fatura_a4.jrxml").getInputStream();
            JasperReport jr = JasperCompileManager.compileReport(reportStream);

            // Preencher e retornar o JasperPrint
            JasperPrint jp = JasperFillManager.fillReport(jr, params, new JRBeanCollectionDataSource(fatura.getItens()));
            if (fatura.getTipoDocumento() != null && "Fatura Pró-Forma".equals(fatura.getTipoDocumento().getDescricao())) {
                jp.setName("Pró-Forma " + (fatura.getNumero() != null ? fatura.getNumero() : ""));
            }
            return jp;
            
        } catch (JRException | IOException e) {
            throw new RuntimeException("Falha ao preparar a Fatura: " + e.getMessage(), e);
        }
    }

    public JasperPrint prepararRelatorioCaixaXZ(String titulo, ao.allon.kubata.faturacao.domain.Caixa caixa, java.util.Map<String, String> header, java.util.List<java.util.Map<String, String>> linhas) {
        try {
            JRDesignStyle normalStyle = new JRDesignStyle();
            normalStyle.setName("Normal");
            normalStyle.setDefault(true);
            normalStyle.setFontName("SansSerif");
            normalStyle.setFontSize(10f);
            normalStyle.setPdfFontName("Helvetica");
            normalStyle.setPdfEncoding("Cp1252");
            normalStyle.setPdfEmbedded(false);

            JasperDesign design = new JasperDesign();
            design.setName("RelatorioCaixa");
            design.setPageWidth(595);
            design.setPageHeight(842);
            design.setColumnWidth(515);
            design.setLeftMargin(40);
            design.setRightMargin(40);
            design.setTopMargin(40);
            design.setBottomMargin(40);
            design.addStyle(normalStyle);

            JRDesignField fLabel = new JRDesignField();
            fLabel.setName("label");
            fLabel.setValueClass(String.class);
            design.addField(fLabel);
            JRDesignField fValue = new JRDesignField();
            fValue.setName("value");
            fValue.setValueClass(String.class);
            design.addField(fValue);

            JRDesignBand titleBand = new JRDesignBand();
            titleBand.setHeight(60);
            JRDesignStaticText stTitle = new JRDesignStaticText();
            stTitle.setText(titulo);
            stTitle.setX(0); stTitle.setY(0);
            stTitle.setWidth(515); stTitle.setHeight(20);
            stTitle.setHorizontalTextAlign(HorizontalTextAlignEnum.CENTER);
            stTitle.setStyle(normalStyle);
            titleBand.addElement(stTitle);

            int y = 24;
            for (java.util.Map.Entry<String, String> e : header.entrySet()) {
                JRDesignStaticText st = new JRDesignStaticText();
                st.setText(e.getKey() + ": " + e.getValue());
                st.setX(0); st.setY(y);
                st.setWidth(515); st.setHeight(14);
                st.setStyle(normalStyle);
                titleBand.addElement(st);
                y += 16;
            }
            if (y + 14 > titleBand.getHeight()) {
                titleBand.setHeight(y + 20);
            }
            design.setTitle(titleBand);

            JRDesignBand detail = new JRDesignBand();
            detail.setHeight(18);
            JRDesignTextField tfLabel = new JRDesignTextField();
            tfLabel.setX(0); tfLabel.setY(0);
            tfLabel.setWidth(250); tfLabel.setHeight(18);
            tfLabel.setExpression(new JRDesignExpression("$F{label}"));
            tfLabel.setStyle(normalStyle);
            detail.addElement(tfLabel);
            JRDesignTextField tfValue = new JRDesignTextField();
            tfValue.setX(260); tfValue.setY(0);
            tfValue.setWidth(255); tfValue.setHeight(18);
            tfValue.setHorizontalTextAlign(HorizontalTextAlignEnum.RIGHT);
            tfValue.setExpression(new JRDesignExpression("$F{value}"));
            tfValue.setStyle(normalStyle);
            detail.addElement(tfValue);

            JRDesignSection detailSection = (JRDesignSection) design.getDetailSection();
            detailSection.addBand(detail);

            JasperReport report = JasperCompileManager.compileReport(design);
            java.util.List<java.util.Map<String, ?>> data = new java.util.ArrayList<>();
            for (java.util.Map<String, String> row : linhas) {
                data.add(new java.util.HashMap<>(row));
            }
            java.util.Map<String, Object> params = getEmpresaParams();
            params.put("CAIXA_ID", String.valueOf(caixa.getId()));
            JasperPrint jp = JasperFillManager.fillReport(report, params, new JRBeanCollectionDataSource(data));
            jp.setName(titulo.replace(" ", "_"));
            return jp;
        } catch (Exception e) {
            throw new RuntimeException("Erro ao preparar Relatório de Caixa: " + e.getMessage(), e);
        }
    }
    public JasperPrint prepararRecibo(ao.allon.kubata.faturacao.domain.Fatura fatura) {
        try {
            Map<String, Object> params = getEmpresaParams();

            // Dados da Fatura/Recibo
            params.put("FATURA_NUMERO", fatura.getNumero());
            params.put("FATURA_DATA", fatura.getDataEmissao() != null ? fatura.getDataEmissao().toString() : "");
            params.put("FATURA_VENCIMENTO", fatura.getDataVencimento() != null ? fatura.getDataVencimento().toString() : "");
            params.put("FATURA_HASH", fatura.getHash());
            params.put("SYSTEM_ENTRY_DATE", fatura.getSystemEntryDate() != null ? fatura.getSystemEntryDate().toString() : "");
            params.put("DOCUMENT_STATUS", fatura.getDocumentStatus() != null ? fatura.getDocumentStatus().getCodigo() : "");
            params.put("DOCUMENT_CANCEL_REASON", fatura.getDocumentCancelReason() != null ? fatura.getDocumentCancelReason().getCodigo() : "");
            params.put("REJECTED_DOCUMENT_NO", fatura.getRejectedDocumentNo() != null ? fatura.getRejectedDocumentNo() : "");
            params.put("CUSTOMER_COUNTRY", fatura.getCustomerCountry() != null ? fatura.getCustomerCountry() : "AO");
            params.put("EAC_CODE", fatura.getEacCode() != null ? fatura.getEacCode() : "");
            params.put("JWS_DOCUMENT_SIGNATURE", fatura.getJwsDocumentSignature() != null ? fatura.getJwsDocumentSignature() : "");
            String titulo = "RECIBO";
            String noteFooter = null;
            if (fatura.getTipoDocumento() != null) {
                String desc = fatura.getTipoDocumento().getDescricao();
                if ("Fatura/Recibo".equals(desc)) titulo = "FATURA/RECIBO";
                else if ("Recibo".equals(desc)) titulo = "RECIBO";
            }
            params.put("DOCUMENTO_TITULO", titulo);
            params.put("NOTE_FOOTER", noteFooter != null ? noteFooter : "");
            
            // Estado do documento conforme SAF-T-AO
            String estadoDoc = fatura.getEstadoDocumento() != null ? fatura.getEstadoDocumento().getDescricao() : "ORIGINAL";
            params.put("ESTADO_DOCUMENTO", estadoDoc);
            
            // Estado visual (mapeamento para o layout)
            String estadoVisualRecibo = "ORIGINAL";
            if (fatura.getDocumentStatus() != null && "A".equalsIgnoreCase(fatura.getDocumentStatus().getCodigo())) {
                estadoVisualRecibo = "ANULADO";
            } else if (fatura.getStatus() == ao.allon.kubata.faturacao.domain.enums.StatusFatura.RASCUNHO) {
                estadoVisualRecibo = "RASCUNHO";
            } else if (fatura.getEstadoDocumento() != null) {
                String enumNameRecibo = fatura.getEstadoDocumento().name();
                if ("SEGUNDA_VIA".equalsIgnoreCase(enumNameRecibo)) {
                    estadoVisualRecibo = "2ª VIA";
                } else if ("RETIFICADO".equalsIgnoreCase(enumNameRecibo)) {
                    estadoVisualRecibo = "RETIFICADO";
                } else if ("ANULADO".equalsIgnoreCase(enumNameRecibo)) {
                    estadoVisualRecibo = "ANULADO";
                } else if ("RASCUNHO".equalsIgnoreCase(enumNameRecibo)) {
                    estadoVisualRecibo = "RASCUNHO";
                }
            }
            params.put("ESTADO_DOCUMENTO_VISUAL", estadoVisualRecibo);
            
            // Operador
            String operadorRecibo = "";
            if (fatura.getUsuario() != null) {
                operadorRecibo = (fatura.getUsuario().getNome() != null && !fatura.getUsuario().getNome().isBlank())
                        ? fatura.getUsuario().getNome()
                        : (fatura.getUsuario().getUsername() != null ? fatura.getUsuario().getUsername() : "");
            }
            params.put("OPERADOR", operadorRecibo);
            
            // Totais (Formatados)
            params.put("FATURA_SUBTOTAL", String.format("Kz %.2f", fatura.getSubtotal()));
            params.put("FATURA_IVA", String.format("Kz %.2f", fatura.getIva()));
            params.put("FATURA_TOTAL", String.format("Kz %.2f", fatura.getTotal()));

            // Dados do Cliente
            if (fatura.getCliente() != null) {
                params.put("CLIENTE_NOME", fatura.getCliente().getNome());
                params.put("CLIENTE_NIF", fatura.getCliente().getNif());
                params.put("CLIENTE_ENDERECO", fatura.getCliente().getEndereco());
            } else {
                params.put("CLIENTE_NOME", "Consumidor Final");
                params.put("CLIENTE_NIF", "999999999");
            }

            // Carregar e compilar o relatório
            InputStream reportStream = resourceLoader.getResource("classpath:reports/recibo_a4.jrxml").getInputStream();
            JasperReport jr = JasperCompileManager.compileReport(reportStream);

            // Preencher e retornar o JasperPrint
            return JasperFillManager.fillReport(jr, params, new JRBeanCollectionDataSource(fatura.getItens()));
            
        } catch (JRException | IOException e) {
            throw new RuntimeException("Falha ao preparar o Recibo: " + e.getMessage(), e);
        }
    }

    public JasperPrint prepararExtratoClientes(Collection<ao.allon.kubata.faturacao.domain.Fatura> faturas, Map<String, Object> params) {
        try {
            LocalDate inicio = params != null && params.get("periodoInicio") instanceof LocalDate ? (LocalDate) params.get("periodoInicio") : null;
            LocalDate fim = params != null && params.get("periodoFim") instanceof LocalDate ? (LocalDate) params.get("periodoFim") : null;
            
            // Adiciona os parâmetros da empresa
            Map<String, Object> fullParams = getEmpresaParams();
            if (params != null) {
                fullParams.putAll(params);
            }

            // Filtra e processa os dados
            Collection<ao.allon.kubata.faturacao.domain.Fatura> ftrs = filtrarPorPeriodo(faturas, inicio, fim);
            List<RowCliente> rows = agruparPorCliente(ftrs);

            // Carrega e compila o relatório .jrxml
            InputStream reportStream = resourceLoader.getResource("classpath:reports/extrato_clientes.jrxml").getInputStream();
            JasperReport jr = JasperCompileManager.compileReport(reportStream);

            // Preenche e retorna o JasperPrint
            return JasperFillManager.fillReport(jr, fullParams, new JRBeanCollectionDataSource(rows));

        } catch (JRException | IOException e) {
            throw new RuntimeException("Falha ao preparar o Extrato de Clientes: " + e.getMessage(), e);
        }
    }

    public JasperPrint prepararExtratoFornecedores(Collection<ao.allon.kubata.faturacao.domain.Despesa> despesas, Map<String, Object> params) {
        try {
            LocalDate inicio = params != null && params.get("periodoInicio") instanceof LocalDate ? (LocalDate) params.get("periodoInicio") : null;
            LocalDate fim = params != null && params.get("periodoFim") instanceof LocalDate ? (LocalDate) params.get("periodoFim") : null;

            // Adiciona os parâmetros da empresa
            Map<String, Object> fullParams = getEmpresaParams();
            if (params != null) {
                fullParams.putAll(params);
            }

            // Filtra e processa os dados
            Collection<ao.allon.kubata.faturacao.domain.Despesa> dsps = filtrarDespesasPorPeriodo(despesas, inicio, fim);
            List<RowFornecedor> rows = agruparPorFornecedor(dsps);

            // Carrega e compila o relatório .jrxml
            InputStream reportStream = resourceLoader.getResource("classpath:reports/extrato_fornecedores.jrxml").getInputStream();
            JasperReport jr = JasperCompileManager.compileReport(reportStream);

            // Preencher e retornar o JasperPrint
            return JasperFillManager.fillReport(jr, fullParams, new JRBeanCollectionDataSource(rows));

        } catch (JRException | IOException e) {
            throw new RuntimeException("Falha ao preparar o Extrato de Fornecedores: " + e.getMessage(), e);
        }
    }

    public JasperPrint prepararListaInventario(Collection<ao.allon.kubata.faturacao.domain.Produto> produtos) {
        try {
            Map<String, Object> params = getEmpresaParams();
            params.put("DATA_EMISSAO", LocalDate.now().toString());

            // Carrega e compila o relatório .jrxml
            InputStream reportStream = resourceLoader.getResource("classpath:reports/lista_inventario.jrxml").getInputStream();
            JasperReport jr = JasperCompileManager.compileReport(reportStream);

            // Preenche e retorna o JasperPrint
            return JasperFillManager.fillReport(jr, params, new JRBeanCollectionDataSource(produtos));

        } catch (JRException | IOException e) {
            throw new RuntimeException("Falha ao preparar a Lista de Inventário: " + e.getMessage(), e);
        }
    }
    
    public JasperPrint prepararListaGuiasRemessa(Collection<ao.allon.kubata.faturacao.domain.Fatura> guias) {
        String[] titles = {"Número", "Data", "Cliente", "Origem", "Destino", "Motorista", "Matrícula", "Estado"};
        String[] fields = {"numero", "data", "cliente", "origem", "destino", "motorista", "matricula", "estado"};
        java.util.List<RowGuia> rows = new java.util.ArrayList<>();
        for (ao.allon.kubata.faturacao.domain.Fatura f : guias) {
            String numero = f.getNumero() != null ? f.getNumero() : "";
            String data = f.getDataEmissao() != null ? f.getDataEmissao().toString() : "";
            String cliente = f.getCliente() != null ? f.getCliente().getNome() : "Consumidor Final";
            String origem = f.getLocalCarga() != null ? f.getLocalCarga() : "";
            String destino = f.getLocalDescarga() != null ? f.getLocalDescarga() : "";
            String motorista = f.getMotorista() != null ? f.getMotorista() : "";
            String matricula = f.getMatriculaViatura() != null ? f.getMatriculaViatura() : "";
            String estado = f.getStatus() != null ? f.getStatus().name() : "";
            rows.add(new RowGuia(numero, data, cliente, origem, destino, motorista, matricula, estado));
        }
        return buildTablePrint("Guias de Remessa", rows, getEmpresaParams(), titles, fields);
    }

    // --- MÉTODOS DE GERAÇÃO DE ARQUIVO (LEGADO E EXPORTAÇÃO) ---

    public void gerarFaturaPdf(ao.allon.kubata.faturacao.domain.Fatura fatura, File outFile) {
        try {
            JasperPrint jp = prepararFatura(fatura);
            exportToPdf(jp, outFile);
        } catch (RuntimeException e) {
            throw new RuntimeException("Falha ao gerar PDF da Fatura: " + e.getMessage(), e);
        }
    }

    public JasperPrint prepararTopProducts(List<Object[]> data, LocalDate inicio, LocalDate fim) {
        try {
            Map<String, Object> params = getEmpresaParams();
            params.put("PERIOD", inicio.toString() + " a " + fim.toString());
            
            List<RowTopProduct> rows = new ArrayList<>();
            for (Object[] row : data) {
                rows.add(new RowTopProduct((String)row[0], (Long)row[1], (BigDecimal)row[2]));
            }

            InputStream reportStream = resourceLoader.getResource("classpath:reports/top_products.jrxml").getInputStream();
            JasperReport jr = JasperCompileManager.compileReport(reportStream);
            return JasperFillManager.fillReport(jr, params, new JRBeanCollectionDataSource(rows));
        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar Ranking de Produtos: " + e.getMessage(), e);
        }
    }

    public JasperPrint prepararTaxReport(List<Object[]> data, LocalDate inicio, LocalDate fim) {
        try {
            Map<String, Object> params = getEmpresaParams();
            params.put("PERIOD", inicio.toString() + " a " + fim.toString());
            
            List<RowTax> rows = new ArrayList<>();
            for (Object[] row : data) {
                rows.add(new RowTax(row[0].toString(), (BigDecimal)row[1], (BigDecimal)row[2]));
            }

            InputStream reportStream = resourceLoader.getResource("classpath:reports/tax_report.jrxml").getInputStream();
            JasperReport jr = JasperCompileManager.compileReport(reportStream);
            return JasperFillManager.fillReport(jr, params, new JRBeanCollectionDataSource(rows));
        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar Mapa de Impostos: " + e.getMessage(), e);
        }
    }

    public JasperPrint prepararPerformanceReport(List<Object[]> data, LocalDate inicio, LocalDate fim) {
        try {
            Map<String, Object> params = getEmpresaParams();
            params.put("PERIOD", inicio.toString() + " a " + fim.toString());
            
            List<RowPerformance> rows = new ArrayList<>();
            for (Object[] row : data) {
                rows.add(new RowPerformance((String)row[0], (Long)row[1], (BigDecimal)row[2]));
            }

            InputStream reportStream = resourceLoader.getResource("classpath:reports/performance_report.jrxml").getInputStream();
            JasperReport jr = JasperCompileManager.compileReport(reportStream);
            return JasperFillManager.fillReport(jr, params, new JRBeanCollectionDataSource(rows));
        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar Desempenho por Caixa: " + e.getMessage(), e);
        }
    }

    // --- INNER CLASSES FOR DATA ROWS ---
    public static class RowTopProduct {
        private String descricao;
        private Long quantidade;
        private BigDecimal total;
        public RowTopProduct(String d, Long q, BigDecimal t) { this.descricao = d; this.quantidade = q; this.total = t; }
        public String getDescricao() { return descricao; }
        public Long getQuantidade() { return quantidade; }
        public BigDecimal getTotal() { return total; }
    }

    public static class RowTax {
        private String data;
        private BigDecimal totalVendas;
        private BigDecimal totalIva;
        public RowTax(String d, BigDecimal v, BigDecimal i) { this.data = d; this.totalVendas = v; this.totalIva = i; }
        public String getData() { return data; }
        public BigDecimal getTotalVendas() { return totalVendas; }
        public BigDecimal getTotalIva() { return totalIva; }
    }

    public static class RowPerformance {
        private String caixa;
        private Long qtdVendas;
        private BigDecimal totalVendas;
        public RowPerformance(String c, Long q, BigDecimal v) { this.caixa = c; this.qtdVendas = q; this.totalVendas = v; }
        public String getCaixa() { return caixa; }
        public Long getQtdVendas() { return qtdVendas; }
        public BigDecimal getTotalVendas() { return totalVendas; }
    }

    public static class RowMapaImpostos {
        private final BigDecimal taxa;
        private final BigDecimal base;
        private final BigDecimal iva;
        private final BigDecimal total;
        private final Integer quantidadeFaturas;
        private final BigDecimal percentualRepresentacao;
        private final String codigoIsencao;
        private final String motivoIsencao;
        private final BigDecimal valorIsencao;

        public RowMapaImpostos(BigDecimal taxa, BigDecimal base, BigDecimal iva, Integer quantidadeFaturas, BigDecimal percentualRepresentacao,
                               String codigoIsencao, String motivoIsencao, BigDecimal valorIsencao) {
            this.taxa = taxa;
            this.base = base;
            this.iva = iva;
            this.total = (base != null ? base : BigDecimal.ZERO).add(iva != null ? iva : BigDecimal.ZERO);
            this.quantidadeFaturas = quantidadeFaturas;
            this.percentualRepresentacao = percentualRepresentacao;
            this.codigoIsencao = codigoIsencao;
            this.motivoIsencao = motivoIsencao;
            this.valorIsencao = valorIsencao;
        }

        public BigDecimal getTaxa() { return taxa; }
        public BigDecimal getBase() { return base; }
        public BigDecimal getIva() { return iva; }
        public BigDecimal getTotal() { return total; }
        public Integer getQuantidadeFaturas() { return quantidadeFaturas; }
        public BigDecimal getPercentualRepresentacao() { return percentualRepresentacao; }
        public String getCodigoIsencao() { return codigoIsencao; }
        public String getMotivoIsencao() { return motivoIsencao; }
        public BigDecimal getValorIsencao() { return valorIsencao; }
    }

    public static class RowRelatorioImpostos {
        private final String codigo;
        private final String descricao;
        private final String tipo;
        private final BigDecimal percentual;
        private final String motivoIsencaoCodigo;
        private final String motivoIsencaoDescricao;
        private final Integer totalFaturas;
        private final BigDecimal baseTributavel;
        private final BigDecimal valorImposto;
        private final BigDecimal valorTotal;

        public RowRelatorioImpostos(String codigo, String descricao, String tipo, BigDecimal percentual,
                                   String motivoIsencaoCodigo, String motivoIsencaoDescricao,
                                   Integer totalFaturas, BigDecimal baseTributavel, BigDecimal valorImposto, BigDecimal valorTotal) {
            this.codigo = codigo;
            this.descricao = descricao;
            this.tipo = tipo;
            this.percentual = percentual;
            this.motivoIsencaoCodigo = motivoIsencaoCodigo;
            this.motivoIsencaoDescricao = motivoIsencaoDescricao;
            this.totalFaturas = totalFaturas;
            this.baseTributavel = baseTributavel;
            this.valorImposto = valorImposto;
            this.valorTotal = valorTotal;
        }

        public String getCodigo() { return codigo; }
        public String getDescricao() { return descricao; }
        public String getTipo() { return tipo; }
        public BigDecimal getPercentual() { return percentual; }
        public String getMotivoIsencaoCodigo() { return motivoIsencaoCodigo; }
        public String getMotivoIsencaoDescricao() { return motivoIsencaoDescricao; }
        public Integer getTotalFaturas() { return totalFaturas; }
        public BigDecimal getBaseTributavel() { return baseTributavel; }
        public BigDecimal getValorImposto() { return valorImposto; }
        public BigDecimal getValorTotal() { return valorTotal; }
    }

    public static class RowRetencaoFonteReport {
        private final String codigo;
        private final String descricao;
        private final BigDecimal taxa;
        private final String tipoRendimento;
        private final Integer numeroOperacoes;
        private final BigDecimal valorBase;
        private final BigDecimal valorRetido;
        private final java.util.Date dataUltimaOperacao;

        public RowRetencaoFonteReport(String codigo, String descricao, BigDecimal taxa, String tipoRendimento, Integer numeroOperacoes,
                                      BigDecimal valorBase, BigDecimal valorRetido, java.util.Date dataUltimaOperacao) {
            this.codigo = codigo;
            this.descricao = descricao;
            this.taxa = taxa;
            this.tipoRendimento = tipoRendimento;
            this.numeroOperacoes = numeroOperacoes;
            this.valorBase = valorBase;
            this.valorRetido = valorRetido;
            this.dataUltimaOperacao = dataUltimaOperacao;
        }

        public String getCodigo() { return codigo; }
        public String getDescricao() { return descricao; }
        public BigDecimal getTaxa() { return taxa; }
        public String getTipoRendimento() { return tipoRendimento; }
        public Integer getNumeroOperacoes() { return numeroOperacoes; }
        public BigDecimal getValorBase() { return valorBase; }
        public BigDecimal getValorRetido() { return valorRetido; }
        public java.util.Date getDataUltimaOperacao() { return dataUltimaOperacao; }
    }

    public void gerarReciboPdf(ao.allon.kubata.faturacao.domain.Fatura fatura, File outFile) {
        try {
            JasperPrint jp = prepararRecibo(fatura);
            exportToPdf(jp, outFile);
        } catch (RuntimeException e) {
            throw new RuntimeException("Falha ao gerar PDF do Recibo: " + e.getMessage(), e);
        }
    }
    
    public void gerarExtratoClientesPdf(Collection<ao.allon.kubata.faturacao.domain.Fatura> faturas, Map<String, Object> params, File outFile) {
        JasperPrint print = prepararExtratoClientes(faturas, params);
        exportToPdf(print, outFile);
    }

    public void gerarExtratoFornecedoresPdf(Collection<ao.allon.kubata.faturacao.domain.Despesa> despesas, Map<String, Object> params, File outFile) {
        JasperPrint print = prepararExtratoFornecedores(despesas, params);
        exportToPdf(print, outFile);
    }

    public JasperPrint prepararPermissoesUsuario(ao.allon.kubata.core.domain.User user, java.util.List<ao.allon.kubata.core.domain.UserAccessPermission> permissoes) {
        try {
            Map<String, Object> params = getEmpresaParams();
            params.put("USUARIO_NOME", user.getNome());
            params.put("USUARIO_EMAIL", user.getEmail());
            java.util.List<java.util.Map<String, Object>> data = new java.util.ArrayList<>();
            for (ao.allon.kubata.core.domain.UserAccessPermission p : permissoes) {
                java.util.Map<String, Object> row = new java.util.HashMap<>();
                row.put("modulo", p.getModulo());
                row.put("opcao", p.getOpcao());
                data.add(row);
            }
            InputStream reportStream = resourceLoader.getResource("classpath:reports/permission_report.jrxml").getInputStream();
            JasperReport jr = JasperCompileManager.compileReport(reportStream);
            JasperPrint jp = JasperFillManager.fillReport(jr, params, new JRBeanCollectionDataSource(data));
            jp.setName("Permissoes_" + user.getNome());
            return jp;
        } catch (Exception e) {
            throw new RuntimeException("Falha ao preparar Relatório de Permissões: " + e.getMessage(), e);
        }
    }

    // --- MÉTODOS DE EXPORTAÇÃO ---

    public void exportToPdf(JasperPrint print, File outFile) {
        try {
            JasperExportManager.exportReportToPdfFile(print, outFile.getAbsolutePath());
        } catch (JRException e) {
            throw new RuntimeException("Falha ao exportar para PDF: " + e.getMessage(), e);
        }
    }

    public void exportToXls(JasperPrint print, File outFile) {
        try {
            JRXlsExporter exporter = new JRXlsExporter();
            exporter.setExporterInput(new SimpleExporterInput(print));
            exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(outFile));
            SimpleXlsReportConfiguration configuration = new SimpleXlsReportConfiguration();
            configuration.setOnePagePerSheet(false);
            configuration.setDetectCellType(true);
            configuration.setCollapseRowSpan(false);
            exporter.setConfiguration(configuration);
            exporter.exportReport();
        } catch (JRException e) {
            throw new RuntimeException("Falha ao exportar para XLS: " + e.getMessage(), e);
        }
    }

    // --- LÓGICA INTERNA E CLASSES DE DADOS ---

    private List<RowCliente> agruparPorCliente(Collection<ao.allon.kubata.faturacao.domain.Fatura> faturas) {
        Map<String, BigDecimal[]> map = new LinkedHashMap<>();
        for (ao.allon.kubata.faturacao.domain.Fatura f : faturas) {
            String cliente = f.getCliente() != null ? f.getCliente().getNome() : "Sem Cliente";
            BigDecimal total = f.getTotal() != null ? f.getTotal() : BigDecimal.ZERO;
            BigDecimal recebido = BigDecimal.ZERO;
            try {
                if (reciboService != null && f.getId() != null) {
                    recebido = reciboService.totalRecebidoFatura(f.getId());
                }
            } catch (Exception ignore) {
                recebido = BigDecimal.ZERO;
            }
            BigDecimal aberto = total.subtract(recebido);
            
            BigDecimal[] acc = map.getOrDefault(cliente, new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO});
            acc[0] = acc[0].add(total);
            acc[1] = acc[1].add(recebido);
            acc[2] = acc[2].add(aberto);
            map.put(cliente, acc);
        }
        
        List<RowCliente> rows = new ArrayList<>();
        for (Map.Entry<String, BigDecimal[]> e : map.entrySet()) {
            BigDecimal[] v = e.getValue();
            rows.add(new RowCliente(e.getKey(), v[0], v[1], v[2]));
        }
        return rows;
    }

    public JasperPrint prepararContasReceberPorCliente(Collection<ao.allon.kubata.faturacao.domain.Fatura> faturas, LocalDate inicio, LocalDate fim) {
        try {
            Map<String, Object> params = getEmpresaParams();
            params.put("periodoInicio", inicio);
            params.put("periodoFim", fim);

            Collection<ao.allon.kubata.faturacao.domain.Fatura> ftrs = filtrarPorPeriodo(faturas, inicio, fim);
            List<RowCliente> rows = agruparPorCliente(ftrs);

            InputStream reportStream = resourceLoader.getResource("classpath:reports/extrato_clientes.jrxml").getInputStream();
            JasperReport jr = JasperCompileManager.compileReport(reportStream);
            JasperPrint jp = JasperFillManager.fillReport(jr, params, new JRBeanCollectionDataSource(rows));
            jp.setName("Contas_Receber_Por_Cliente");
            return jp;
        } catch (Exception e) {
            throw new RuntimeException("Falha ao preparar Contas a Receber por Cliente: " + e.getMessage(), e);
        }
    }

    private List<RowFornecedor> agruparPorFornecedor(Collection<ao.allon.kubata.faturacao.domain.Despesa> despesas) {
        Map<String, BigDecimal[]> map = new LinkedHashMap<>();
        for (ao.allon.kubata.faturacao.domain.Despesa d : despesas) {
            String fornecedor = d.getFornecedor() != null ? d.getFornecedor().getNome() : "Sem Fornecedor";
            BigDecimal valor = d.getValor() != null ? d.getValor() : BigDecimal.ZERO;
            BigDecimal pago = d.getValorPago() != null ? d.getValorPago() : BigDecimal.ZERO;
            BigDecimal aberto = valor.subtract(pago);
            BigDecimal[] acc = map.getOrDefault(fornecedor, new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO});
            acc[0] = acc[0].add(valor);
            acc[1] = acc[1].add(pago);
            acc[2] = acc[2].add(aberto);
            map.put(fornecedor, acc);
        }
        List<RowFornecedor> rows = new ArrayList<>();
        for (Map.Entry<String, BigDecimal[]> e : map.entrySet()) {
            BigDecimal[] v = e.getValue();
            rows.add(new RowFornecedor(e.getKey(), v[0], v[1], v[2]));
        }
        return rows;
    }

    private JasperPrint buildTablePrint(String titleText, Collection<?> rows, Map<String, Object> params,
                                        String[] columnTitles, String[] fieldNames) {
        try {
            JasperDesign design = new JasperDesign();
            design.setName(titleText.replace(" ", "_"));
            design.setPageWidth(595);
            design.setPageHeight(842);
            design.setColumnWidth(515);
            design.setLeftMargin(40);
            design.setRightMargin(40);
            design.setTopMargin(40);
            design.setBottomMargin(40);

            JRDesignBand title = new JRDesignBand();
            title.setHeight(50);
            JRDesignStaticText titleLabel = new JRDesignStaticText();
            titleLabel.setText(titleText + " - " + LocalDate.now());
            titleLabel.setX(0); titleLabel.setY(0); titleLabel.setWidth(515); titleLabel.setHeight(24);
            Map<String, Object> parametersForTitle = params != null ? new HashMap<>(params) : new HashMap<>();
            Object pIni = parametersForTitle.get("periodoInicio");
            Object pFim = parametersForTitle.get("periodoFim");
            String periodo = (pIni instanceof java.time.LocalDate && pFim instanceof java.time.LocalDate)
                    ? " (" + pIni + " a " + pFim + ")"
                    : "";
            titleLabel.setText(titleText + periodo + " - " + LocalDate.now());
            titleLabel.setHorizontalTextAlign(HorizontalTextAlignEnum.LEFT);
            titleLabel.setVerticalTextAlign(VerticalTextAlignEnum.MIDDLE);
            JRDesignStyle titleStyle = new JRDesignStyle();
            titleStyle.setName("TitleStyle");
            titleStyle.setBold(true);
            titleStyle.setFontSize(14f);
            design.addStyle(titleStyle);
            titleLabel.setStyle(titleStyle);
            title.addElement(titleLabel);
            design.setTitle(title);

            // Fields
            for (int i = 0; i < fieldNames.length; i++) {
                JRDesignField field = new JRDesignField();
                field.setName(fieldNames[i]);
                field.setValueClass(String.class);
                design.addField(field);
            }

            // Column header
            JRDesignBand header = new JRDesignBand();
            header.setHeight(20);
            int[] widths = calcWidths(fieldNames.length, 515);
            int x = 0;
            for (int i = 0; i < columnTitles.length; i++) {
                JRDesignStaticText st = new JRDesignStaticText();
                st.setText(columnTitles[i]);
                st.setX(x); st.setY(0); st.setWidth(widths[i]); st.setHeight(18);
                st.setHorizontalTextAlign(HorizontalTextAlignEnum.LEFT);
                JRDesignStyle hdr = new JRDesignStyle();
                hdr.setName("Hdr" + i);
                hdr.setBold(true);
                hdr.setFontSize(11f);
                design.addStyle(hdr);
                st.setStyle(hdr);
                header.addElement(st);
                x += widths[i];
            }
            design.setColumnHeader(header);

            // Detail band
            JRDesignBand detail = new JRDesignBand();
            detail.setHeight(18);
            x = 0;
            for (int i = 0; i < fieldNames.length; i++) {
                JRDesignTextField tf = new JRDesignTextField();
                tf.setX(x); tf.setY(0); tf.setWidth(widths[i]); tf.setHeight(16);
                tf.setHorizontalTextAlign(HorizontalTextAlignEnum.LEFT);
                JRDesignExpression exp = new JRDesignExpression();
                exp.setText("$F{" + fieldNames[i] + "}");
                tf.setExpression(exp);
                detail.addElement(tf);
                x += widths[i];
            }
            JRDesignSection detailSection = (JRDesignSection) design.getDetailSection();
            detailSection.addBand(detail);

            JasperReport report = JasperCompileManager.compileReport(design);
            JRBeanCollectionDataSource ds = new JRBeanCollectionDataSource(rows);
            Map<String, Object> parameters = params != null ? new HashMap<>(params) : new HashMap<>();
            return JasperFillManager.fillReport(report, parameters, ds);
        } catch (JRException e) {
            throw new RuntimeException("Falha ao gerar relatório Jasper: " + e.getMessage(), e);
        }
    }

    private int[] calcWidths(int columns, int totalWidth) {
        int[] w = new int[columns];
        int avg = totalWidth / columns;
        Arrays.fill(w, avg);
        w[0] = (int)(avg * 1.4);
        int rest = totalWidth - w[0];
        int each = rest / (columns - 1);
        for (int i = 1; i < columns; i++) w[i] = each;
        return w;
    }

    private List<RowMapaImpostos> buildMapaImpostosRows(Collection<ao.allon.kubata.faturacao.domain.Fatura> faturas) {
        Map<BigDecimal, BigDecimal[]> byTaxa = new HashMap<>();
        Map<BigDecimal, Set<Long>> faturaIdsByTaxa = new HashMap<>();
        Map<String, BigDecimal> byIsencao = new HashMap<>();
        BigDecimal totalGeral = BigDecimal.ZERO;

        for (ao.allon.kubata.faturacao.domain.Fatura f : faturas) {
            if (f == null || f.getItens() == null) continue;
            for (ao.allon.kubata.faturacao.domain.ItemFatura it : f.getItens()) {
                if (it == null) continue;
                BigDecimal base = it.getSubtotal() != null ? it.getSubtotal() : BigDecimal.ZERO;
                BigDecimal taxa = it.getPercentualIva() != null ? it.getPercentualIva() : BigDecimal.ZERO;
                BigDecimal iva = it.getValorIva() != null ? it.getValorIva() : BigDecimal.ZERO;
                BigDecimal total = it.getTotal() != null ? it.getTotal() : base.add(iva);
                totalGeral = totalGeral.add(total);

                if (taxa.compareTo(BigDecimal.ZERO) == 0 && it.getCodigoIsencao() != null && !it.getCodigoIsencao().isBlank()) {
                    String key = it.getCodigoIsencao().trim() + "|" + (it.getMotivoIsencao() != null ? it.getMotivoIsencao().trim() : "");
                    byIsencao.merge(key, base, BigDecimal::add);
                    continue;
                }

                byTaxa.computeIfAbsent(taxa, k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO});
                BigDecimal[] acc = byTaxa.get(taxa);
                acc[0] = acc[0].add(base);
                acc[1] = acc[1].add(iva);
                acc[2] = acc[2].add(total);
                faturaIdsByTaxa.computeIfAbsent(taxa, k -> new HashSet<>());
                if (f.getId() != null) {
                    faturaIdsByTaxa.get(taxa).add(f.getId());
                }
            }
        }

        List<RowMapaImpostos> rows = new ArrayList<>();
        for (Map.Entry<BigDecimal, BigDecimal[]> e : byTaxa.entrySet()) {
            BigDecimal taxa = e.getKey();
            BigDecimal base = e.getValue()[0];
            BigDecimal iva = e.getValue()[1];
            BigDecimal total = e.getValue()[2];
            int qtdFat = faturaIdsByTaxa.getOrDefault(taxa, Collections.emptySet()).size();
            BigDecimal representacao = BigDecimal.ZERO;
            if (totalGeral.compareTo(BigDecimal.ZERO) > 0) {
                representacao = total.multiply(BigDecimal.valueOf(100)).divide(totalGeral, 2, RoundingMode.HALF_UP);
            }
            rows.add(new RowMapaImpostos(taxa, base, iva, qtdFat, representacao, null, null, BigDecimal.ZERO));
        }

        for (Map.Entry<String, BigDecimal> e : byIsencao.entrySet()) {
            String[] parts = e.getKey().split("\\|", 2);
            String codigo = parts.length > 0 ? parts[0] : "";
            String motivo = parts.length > 1 ? parts[1] : "";
            BigDecimal baseIsenta = e.getValue();
            BigDecimal representacao = BigDecimal.ZERO;
            if (totalGeral.compareTo(BigDecimal.ZERO) > 0) {
                representacao = baseIsenta.multiply(BigDecimal.valueOf(100)).divide(totalGeral, 2, RoundingMode.HALF_UP);
            }
            rows.add(new RowMapaImpostos(BigDecimal.ZERO, baseIsenta, BigDecimal.ZERO, 0, representacao, codigo, motivo, baseIsenta));
        }

        rows.sort((a, b) -> {
            if (a.getTaxa() == null && b.getTaxa() == null) return 0;
            if (a.getTaxa() == null) return 1;
            if (b.getTaxa() == null) return -1;
            return b.getTaxa().compareTo(a.getTaxa());
        });
        return rows;
    }

    private List<RowRelatorioImpostos> buildRelatorioImpostosRows(Collection<ao.allon.kubata.faturacao.domain.Fatura> faturas) {
        class Acc {
            int totalFaturas = 0;
            BigDecimal base = BigDecimal.ZERO;
            BigDecimal imposto = BigDecimal.ZERO;
            BigDecimal total = BigDecimal.ZERO;
        }
        Map<String, Acc> map = new LinkedHashMap<>();
        Map<String, Set<Long>> faturaIds = new HashMap<>();

        for (ao.allon.kubata.faturacao.domain.Fatura f : faturas) {
            if (f == null || f.getItens() == null) continue;
            for (ao.allon.kubata.faturacao.domain.ItemFatura it : f.getItens()) {
                if (it == null) continue;
                BigDecimal taxa = it.getPercentualIva() != null ? it.getPercentualIva() : BigDecimal.ZERO;
                BigDecimal base = it.getSubtotal() != null ? it.getSubtotal() : BigDecimal.ZERO;
                BigDecimal iva = it.getValorIva() != null ? it.getValorIva() : BigDecimal.ZERO;
                BigDecimal total = it.getTotal() != null ? it.getTotal() : base.add(iva);

                String tipo = "IVA";
                String codigo;
                String descricao;
                String isencaoCod = null;
                String isencaoDesc = null;
                if (taxa.compareTo(BigDecimal.ZERO) == 0 && it.getCodigoIsencao() != null && !it.getCodigoIsencao().isBlank()) {
                    codigo = it.getCodigoIsencao().trim();
                    descricao = "Isenção";
                    isencaoCod = codigo;
                    isencaoDesc = it.getMotivoIsencao();
                } else {
                    codigo = "IVA" + taxa.stripTrailingZeros().toPlainString();
                    descricao = "IVA " + taxa.stripTrailingZeros().toPlainString() + "%";
                }

                String key = codigo + "|" + taxa.stripTrailingZeros().toPlainString() + "|" + (isencaoCod != null ? isencaoCod : "");
                map.computeIfAbsent(key, k -> new Acc());
                Acc acc = map.get(key);
                acc.base = acc.base.add(base);
                acc.imposto = acc.imposto.add(iva);
                acc.total = acc.total.add(total);
                faturaIds.computeIfAbsent(key, k -> new HashSet<>());
                if (f.getId() != null) faturaIds.get(key).add(f.getId());

                acc.totalFaturas = faturaIds.get(key).size();
            }
        }

        List<RowRelatorioImpostos> rows = new ArrayList<>();
        for (Map.Entry<String, Acc> e : map.entrySet()) {
            String[] parts = e.getKey().split("\\|", 3);
            String codigo = parts[0];
            BigDecimal taxa = new BigDecimal(parts[1]);
            String isencaoCod = parts.length > 2 && !parts[2].isBlank() ? parts[2] : null;

            String tipo = "IVA";
            String descricao = isencaoCod != null ? "Isenção" : ("IVA " + taxa.stripTrailingZeros().toPlainString() + "%");
            String isencaoDesc = null;
            if (isencaoCod != null) {
                isencaoDesc = mapMotivoIsencaoDescricao(faturas, isencaoCod);
            }

            Acc acc = e.getValue();
            rows.add(new RowRelatorioImpostos(codigo, descricao, tipo, taxa, isencaoCod, isencaoDesc, acc.totalFaturas, acc.base, acc.imposto, acc.total));
        }
        return rows;
    }

    private String mapMotivoIsencaoDescricao(Collection<ao.allon.kubata.faturacao.domain.Fatura> faturas, String codigoIsencao) {
        if (codigoIsencao == null) return null;
        for (ao.allon.kubata.faturacao.domain.Fatura f : faturas) {
            if (f == null || f.getItens() == null) continue;
            for (ao.allon.kubata.faturacao.domain.ItemFatura it : f.getItens()) {
                if (it != null && codigoIsencao.equalsIgnoreCase(it.getCodigoIsencao())) {
                    return it.getMotivoIsencao();
                }
            }
        }
        return null;
    }

    private List<RowRetencaoFonteReport> buildRetencoesFonteRows(Collection<ao.allon.kubata.faturacao.domain.Fatura> faturas) {
        Map<String, RowRetencaoFonteReport> map = new LinkedHashMap<>();
        for (ao.allon.kubata.faturacao.domain.Fatura f : faturas) {
            if (f == null) continue;
            BigDecimal retido = f.getTotalRetencao() != null ? f.getTotalRetencao() : BigDecimal.ZERO;
            if (retido.compareTo(BigDecimal.ZERO) <= 0) continue;

            String codigo = "RET";
            String descricao = "Retenção na Fonte";
            BigDecimal taxa = BigDecimal.ZERO;
            String tipoRendimento = "";
            BigDecimal base = f.getSubtotal() != null ? f.getSubtotal() : BigDecimal.ZERO;

            if (!map.containsKey(codigo)) {
                map.put(codigo, new RowRetencaoFonteReport(codigo, descricao, taxa, tipoRendimento, 1, base, retido,
                        f.getDataEmissao() != null ? java.sql.Date.valueOf(f.getDataEmissao()) : new java.util.Date()));
            } else {
                RowRetencaoFonteReport cur = map.get(codigo);
                map.put(codigo, new RowRetencaoFonteReport(
                        cur.getCodigo(),
                        cur.getDescricao(),
                        cur.getTaxa(),
                        cur.getTipoRendimento(),
                        cur.getNumeroOperacoes() + 1,
                        cur.getValorBase().add(base),
                        cur.getValorRetido().add(retido),
                        cur.getDataUltimaOperacao()
                ));
            }
        }
        return new ArrayList<>(map.values());
    }

    public static class RowCliente {
        private String cliente; private String total; private String recebido; private String aberto;
        public RowCliente(String cliente, BigDecimal total, BigDecimal recebido, BigDecimal aberto) {
            this.cliente = cliente;
            this.total = fmt(total);
            this.recebido = fmt(recebido);
            this.aberto = fmt(aberto);
        }
        public String getCliente() { return cliente; }
        public String getTotal() { return total; }
        public String getRecebido() { return recebido; }
        public String getAberto() { return aberto; }
    }

    public static class RowFornecedor {
        private String fornecedor; private String valor; private String pago; private String aberto;
        public RowFornecedor(String fornecedor, BigDecimal valor, BigDecimal pago, BigDecimal aberto) {
            this.fornecedor = fornecedor;
            this.valor = fmt(valor);
            this.pago = fmt(pago);
            this.aberto = fmt(aberto);
        }
        public String getFornecedor() { return fornecedor; }
        public String getValor() { return valor; }
        public String getPago() { return pago; }
        public String getAberto() { return aberto; }
    }
    
    public static class RowGuia {
        private final String numero, data, cliente, origem, destino, motorista, matricula, estado;
        public RowGuia(String numero, String data, String cliente, String origem, String destino, String motorista, String matricula, String estado) {
            this.numero = numero; this.data = data; this.cliente = cliente; this.origem = origem; this.destino = destino; this.motorista = motorista; this.matricula = matricula; this.estado = estado;
        }
        public String getNumero() { return numero; }
        public String getData() { return data; }
        public String getCliente() { return cliente; }
        public String getOrigem() { return origem; }
        public String getDestino() { return destino; }
        public String getMotorista() { return motorista; }
        public String getMatricula() { return matricula; }
        public String getEstado() { return estado; }
    }

    private static String fmt(BigDecimal v) { return String.format("Kz %.2f", v != null ? v : BigDecimal.ZERO); }

    private static Collection<ao.allon.kubata.faturacao.domain.Fatura> filtrarPorPeriodo(Collection<ao.allon.kubata.faturacao.domain.Fatura> faturas, LocalDate inicio, LocalDate fim) {
        if (inicio == null && fim == null) return faturas;
        java.util.List<ao.allon.kubata.faturacao.domain.Fatura> list = new java.util.ArrayList<>();
        for (ao.allon.kubata.faturacao.domain.Fatura f : faturas) {
            LocalDate d = f.getDataEmissao();
            boolean ok = true;
            if (inicio != null && d != null && d.isBefore(inicio)) ok = false;
            if (fim != null && d != null && d.isAfter(fim)) ok = false;
            if (ok) list.add(f);
        }
        return list;
    }

    private static Collection<ao.allon.kubata.faturacao.domain.Despesa> filtrarDespesasPorPeriodo(Collection<ao.allon.kubata.faturacao.domain.Despesa> despesas, LocalDate inicio, LocalDate fim) {
        if (inicio == null && fim == null) return despesas;
        java.util.List<ao.allon.kubata.faturacao.domain.Despesa> list = new java.util.ArrayList<>();
        for (ao.allon.kubata.faturacao.domain.Despesa d : despesas) {
            LocalDate dt = d.getDataEmissao();
            boolean ok = true;
            if (inicio != null && dt != null && dt.isBefore(inicio)) ok = false;
            if (fim != null && dt != null && dt.isAfter(fim)) ok = false;
            if (ok) list.add(d);
        }
        return list;
    }

    // Métodos que usavam JRXML diretamente e foram descontinuados ou integrados
    // Foram removidos para simplificar a classe, já que a abordagem de build programático é mais flexível
    // public void gerarExtratoClientesPdfJRXML(...)
    // public void gerarExtratoFornecedoresPdfJRXML(...)
}

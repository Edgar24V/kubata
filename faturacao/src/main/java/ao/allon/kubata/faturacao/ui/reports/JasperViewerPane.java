package ao.allon.kubata.faturacao.ui.reports;

import atlantafx.base.theme.Styles;
import atlantafx.base.theme.Tweaks;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.*;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.export.*;
import net.sf.jasperreports.engine.export.ooxml.JRDocxExporter;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.export.*;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2AL;
import org.kordamp.ikonli.material2.Material2MZ;

import java.awt.image.BufferedImage;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Visualizador profissional de relatórios JasperReports.
 * Recursos: zoom avançado, navegação completa, exportação multi-formato,
 * miniaturas, busca, atalhos de teclado e barra de status informativa.
 */
public class JasperViewerPane extends BorderPane {

    private static final int THUMBNAIL_WIDTH = 120;
    private static final double ZOOM_MIN = 0.25;
    private static final double ZOOM_MAX = 4.0;
    private static final Double[] ZOOM_LEVELS = {0.25, 0.5, 0.75, 1.0, 1.25, 1.5, 2.0, 2.5, 3.0, 4.0};
    private static final ExecutorService THREAD_POOL = Executors.newFixedThreadPool(3);

    private JasperPrint jasperPrint;
    private int paginaAtual = 0;
    private DoubleProperty zoomFactor = new SimpleDoubleProperty(1.0);
    private String nomeRelatorio = "Relatório";

    // UI Components
    private WebView webView;
    private WebEngine webEngine;
    private ListView<Integer> thumbnailsList;
    private TextField txtBusca;
    private Label lblStatus, lblInfoDocumento, lblZoom;
    private ProgressBar progressBar;
    private Label lblContadorPagina;
    private ComboBox<Double> cmbZoom;
    
    // Navigation Controls
    private Button btnPrimeira, btnAnterior, btnProxima, btnUltima;
    private Button btnZoomIn, btnZoomOut, btnFitWidth, btnFitPage;
    private ToggleButton btnToggleThumbnails;

    // Export callbacks
    private Consumer<File> onExportCallback;
    private Runnable onPrintCallback;

    // Thumbnail Cache
    private final Map<Integer, Image> thumbnailCache = new HashMap<>();

    private boolean atalhosConfigurados = false;

    public JasperViewerPane(JasperPrint jasperPrint) {
        this.jasperPrint = jasperPrint;
        if (jasperPrint != null) {
            this.nomeRelatorio = jasperPrint.getName() != null ? jasperPrint.getName() : "Relatório";
        }
        webView = new WebView();
        webEngine = webView.getEngine();
        inicializarComponentes();
        configurarLayout();
        carregarRelatorio();
    }

    private void inicializarComponentes() {
        // WebView setup
        webView.setContextMenuEnabled(true);
        
        // Thumbnails
        thumbnailsList = new ListView<>();
        thumbnailsList.setCellFactory(param -> new ThumbnailListCell());
        thumbnailsList.getStyleClass().add(Styles.STRIPED);
        thumbnailsList.setPrefWidth(THUMBNAIL_WIDTH + 20);

        // Status bar
        lblStatus = new Label("Pronto");
        lblInfoDocumento = new Label("");
        lblZoom = new Label("100%");
        progressBar = new ProgressBar(0);
        progressBar.setVisible(false);
        progressBar.setMaxWidth(150);
        
        // Zoom combo
        cmbZoom = new ComboBox<>();
        cmbZoom.getItems().addAll(ZOOM_LEVELS);
        cmbZoom.setValue(1.0);
        cmbZoom.setPrefWidth(80);
        cmbZoom.setConverter(new javafx.util.StringConverter<Double>() {
            @Override
            public String toString(Double object) {
                return String.format("%.0f%%", object * 100);
            }
            @Override
            public Double fromString(String string) {
                return Double.parseDouble(string.replace("%", "")) / 100;
            }
        });
    }

    private void configurarLayout() {
        setTop(criarBarraFerramentas());
        
        SplitPane splitPane = new SplitPane();
        splitPane.getItems().addAll(thumbnailsList, webView);
        splitPane.setDividerPositions(0.2);
        
        // Responsividade: Esconder thumbnails em telas pequenas se necessário
        thumbnailsList.managedProperty().bind(thumbnailsList.visibleProperty());
        
        setCenter(splitPane);
        setBottom(criarBarraStatus());
        
        getStyleClass().add("jasper-viewer-modern");
    }

    private ToolBar criarBarraFerramentas() {
        // Navegação
        btnPrimeira = criarBotaoIcone(Material2AL.FIRST_PAGE, "Primeira Página (Ctrl+Home)", e -> navegarPara(0));
        btnAnterior = criarBotaoIcone(Material2AL.KEYBOARD_ARROW_LEFT, "Página Anterior (Page Up)", e -> navegarPara(paginaAtual - 1));
        
        lblContadorPagina = new Label("0 / 0");
        lblContadorPagina.getStyleClass().add(Styles.TEXT_BOLD);
        lblContadorPagina.setStyle("-fx-min-width: 60px; -fx-alignment: center;");
        
        btnProxima = criarBotaoIcone(Material2AL.KEYBOARD_ARROW_RIGHT, "Próxima Página (Page Down)", e -> navegarPara(paginaAtual + 1));
        btnUltima = criarBotaoIcone(Material2AL.LAST_PAGE, "Última Página (Ctrl+End)", e -> navegarPara(getTotalPaginas() - 1));

        // Zoom avançado
        btnZoomOut = criarBotaoIcone(Material2MZ.ZOOM_OUT, "Diminuir Zoom (Ctrl+-)", e -> aplicarZoom(-0.25));
        btnZoomIn = criarBotaoIcone(Material2MZ.ZOOM_IN, "Aumentar Zoom (Ctrl++)", e -> aplicarZoom(0.25));
        btnFitWidth = criarBotaoIcone(Material2AL.ASPECT_RATIO, "Ajustar à Largura", e -> ajustarZoomLargura());
        btnFitPage = criarBotaoIcone(Material2AL.CROP_FREE, "Ajustar à Página", e -> ajustarZoomPagina());
        
        // Bind zoom combo
        cmbZoom.valueProperty().addListener((obs, old, val) -> {
            if (val != null) {
                definirZoom(val);
            }
        });
        
        // Bind label zoom
        zoomFactor.addListener((obs, old, val) -> {
            lblZoom.setText(String.format("%.0f%%", val.doubleValue() * 100));
            cmbZoom.setValue(val.doubleValue());
        });

        // Sidebar Toggle
        btnToggleThumbnails = new ToggleButton("", new FontIcon(Material2AL.GRID_ON));
        btnToggleThumbnails.setTooltip(new Tooltip("Mostrar/Ocultar Miniaturas (F4)"));
        btnToggleThumbnails.setSelected(true);
        btnToggleThumbnails.getStyleClass().add(Styles.BUTTON_ICON);
        thumbnailsList.visibleProperty().bind(btnToggleThumbnails.selectedProperty());

        // Busca
        txtBusca = new TextField();
        txtBusca.setPromptText("Buscar... (Ctrl+F)");
        txtBusca.setPrefWidth(180);
        txtBusca.setOnAction(e -> buscarTexto(txtBusca.getText()));
        Button btnBuscar = criarBotaoIcone(Material2MZ.SEARCH, "Buscar", e -> buscarTexto(txtBusca.getText()));

        // Exportação multi-formato
        MenuButton btnExportar = new MenuButton("Exportar");
        btnExportar.setGraphic(new FontIcon(Material2MZ.SAVE_ALT));
        btnExportar.getStyleClass().addAll(Styles.BUTTON_OUTLINED, Styles.SUCCESS);
        
        MenuItem itemPdf = new MenuItem("PDF (.pdf)", new FontIcon(Material2MZ.PICTURE_AS_PDF));
        itemPdf.setOnAction(e -> exportarParaArquivo("pdf"));
        
        MenuItem itemExcel = new MenuItem("Excel (.xlsx)", new FontIcon(Material2AL.GRID_ON));
        itemExcel.setOnAction(e -> exportarParaArquivo("xlsx"));
        
        MenuItem itemWord = new MenuItem("Word (.docx)", new FontIcon(Material2AL.DESCRIPTION));
        itemWord.setOnAction(e -> exportarParaArquivo("docx"));
        
        MenuItem itemHtml = new MenuItem("HTML (.html)", new FontIcon(Material2AL.CODE));
        itemHtml.setOnAction(e -> exportarParaArquivo("html"));
        
        MenuItem itemCsv = new MenuItem("CSV (.csv)", new FontIcon(Material2AL.LIST));
        itemCsv.setOnAction(e -> exportarParaArquivo("csv"));
        
        MenuItem itemXml = new MenuItem("XML (.xml)", new FontIcon(Material2AL.ACCOUNT_TREE));
        itemXml.setOnAction(e -> exportarParaArquivo("xml"));
        
        btnExportar.getItems().addAll(itemPdf, itemExcel, itemWord, itemHtml, itemCsv, itemXml);
        
        // Impressão
        Button btnImprimir = new Button("Imprimir", new FontIcon(Material2MZ.PRINT));
        btnImprimir.getStyleClass().addAll(Styles.BUTTON_OUTLINED, Styles.ACCENT);
        btnImprimir.setOnAction(e -> imprimirRelatorio());
        btnImprimir.setTooltip(new Tooltip("Imprimir (Ctrl+P)"));

        // Separadores
        Separator sep1 = new Separator();
        Separator sep2 = new Separator();
        Separator sep3 = new Separator();
        Separator sep4 = new Separator();

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        ToolBar toolbar = new ToolBar(
            btnToggleThumbnails, sep1,
            btnPrimeira, btnAnterior, lblContadorPagina, btnProxima, btnUltima, sep2,
            btnZoomOut, cmbZoom, btnZoomIn, btnFitWidth, btnFitPage, lblZoom, sep3,
            txtBusca, btnBuscar, spacer,
            btnImprimir, btnExportar
        );
        toolbar.getStyleClass().add(Styles.FLAT);
        
        return toolbar;
    }

    private HBox criarBarraStatus() {
        // Informações do documento
        lblInfoDocumento.setStyle("-fx-text-fill: #666;");
        atualizarInfoDocumento();
        
        HBox leftBox = new HBox(10, progressBar, lblStatus);
        leftBox.setAlignment(Pos.CENTER_LEFT);
        
        HBox rightBox = new HBox(10, lblInfoDocumento);
        rightBox.setAlignment(Pos.CENTER_RIGHT);
        
        HBox statusBar = new HBox(15, leftBox, rightBox);
        statusBar.setPadding(new Insets(5, 10, 5, 10));
        statusBar.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(rightBox, Priority.ALWAYS);
        statusBar.getStyleClass().add(Tweaks.ALT_ICON);
        statusBar.setStyle("-fx-background-color: -color-bg-default; -fx-border-top: 1px solid -color-border-muted;");
        return statusBar;
    }
    
    private void atualizarInfoDocumento() {
        if (jasperPrint == null) return;
        String data = new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date());
        int totalPaginas = getTotalPaginas();
        String info = String.format("%s | %d página%s | %s", 
            nomeRelatorio, 
            totalPaginas, 
            totalPaginas > 1 ? "s" : "", 
            data);
        lblInfoDocumento.setText(info);
    }

    private Button criarBotaoIcone(org.kordamp.ikonli.Ikon icon, String tooltip, javafx.event.EventHandler<javafx.event.ActionEvent> action) {
        Button btn = new Button("", new FontIcon(icon));
        btn.setTooltip(new Tooltip(tooltip));
        btn.getStyleClass().addAll(Styles.BUTTON_ICON);
        btn.setOnAction(action);
        return btn;
    }

    private void carregarRelatorio() {
        if (jasperPrint == null) return;
        
        int totalPaginas = jasperPrint.getPages().size();
        thumbnailsList.getItems().clear();
        for (int i = 0; i < totalPaginas; i++) {
            thumbnailsList.getItems().add(i);
        }
        
        thumbnailsList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                navegarPara(newVal);
            }
        });

        // Carrega a primeira página
        navegarPara(0);
    }

    private void navegarPara(int index) {
        if (index < 0 || index >= getTotalPaginas()) return;
        
        this.paginaAtual = index;
        atualizarControlesNavegacao();
        renderizarPaginaHTML(index);
        
        // Seleciona na lista sem disparar o listener recursivamente (check simples)
        if (thumbnailsList.getSelectionModel().getSelectedIndex() != index) {
            thumbnailsList.getSelectionModel().select(index);
            thumbnailsList.scrollTo(index);
        }
    }

    private void renderizarPaginaHTML(int pageIndex) {
        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                HtmlExporter exporter = new HtmlExporter();
                exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
                
                SimpleHtmlReportConfiguration config = new SimpleHtmlReportConfiguration();
                config.setPageIndex(pageIndex);
                config.setEmbedImage(true);
                config.setZoomRatio(zoomFactor.floatValue());
                exporter.setConfiguration(config);
                
                StringBuilder sb = new StringBuilder();
                SimpleHtmlExporterOutput output = new SimpleHtmlExporterOutput(sb);
                // output.setEmbedImage(true); // Removido: método não existe ou foi renomeado na versão atual
                exporter.setExporterOutput(output);
                
                exporter.exportReport();
                
                // Adicionar estilos CSS básicos para melhor visualização
                String html = sb.toString();
                return html.replace("</head>", 
                    "<style>body { margin: 0; padding: 10px; background-color: #f4f4f4; display: flex; justify-content: center; }</style></head>");
            }
        };

        task.setOnSucceeded(e -> {
            webEngine.loadContent(task.getValue());
            lblStatus.setText("Página " + (pageIndex + 1) + " carregada.");
            progressBar.setVisible(false);
        });

        task.setOnRunning(e -> {
            lblStatus.setText("Renderizando página " + (pageIndex + 1) + "...");
            progressBar.setVisible(true);
        });

        task.setOnFailed(e -> {
            lblStatus.setText("Erro ao renderizar página.");
            progressBar.setVisible(false);
            e.getSource().getException().printStackTrace();
        });

        THREAD_POOL.submit(task);
    }

    private void aplicarZoom(double delta) {
        double novoZoom = zoomFactor.get() + delta;
        definirZoom(novoZoom);
    }
    
    private void definirZoom(double valor) {
        if (valor < ZOOM_MIN) valor = ZOOM_MIN;
        if (valor > ZOOM_MAX) valor = ZOOM_MAX;
        zoomFactor.set(valor);
        webView.setZoom(valor);
        lblStatus.setText(String.format("Zoom: %.0f%%", valor * 100));
        renderizarPaginaHTML(paginaAtual);
    }
    
    private void ajustarZoomLargura() {
        // Calcula zoom para ajustar à largura do WebView
        double larguraPagina = jasperPrint.getPageWidth();
        double larguraView = webView.getWidth() - 40;
        if (larguraView > 0 && larguraPagina > 0) {
            double novoZoom = larguraView / larguraPagina;
            definirZoom(Math.min(novoZoom, ZOOM_MAX));
        }
    }
    
    private void ajustarZoomPagina() {
        // Calcula zoom para mostrar página inteira
        double alturaPagina = jasperPrint.getPageHeight();
        double alturaView = webView.getHeight() - 40;
        if (alturaView > 0 && alturaPagina > 0) {
            double zoomAltura = alturaView / alturaPagina;
            double larguraPagina = jasperPrint.getPageWidth();
            double larguraView = webView.getWidth() - 40;
            double zoomLargura = larguraView / larguraPagina;
            definirZoom(Math.min(Math.min(zoomAltura, zoomLargura), ZOOM_MAX));
        }
    }

    private void configurarAtalhosTeclado() {
        if (atalhosConfigurados) {
            return;
        }

        if (getScene() == null) {
            sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null) {
                    configurarAtalhosTeclado();
                }
            });
            return;
        }

        atalhosConfigurados = true;

        // Ctrl+P - Imprimir
        getScene().getAccelerators().put(
            KeyCombination.keyCombination("Ctrl+P"),
            () -> imprimirRelatorio()
        );
        
        // Ctrl+S - Exportar PDF
        getScene().getAccelerators().put(
            KeyCombination.keyCombination("Ctrl+S"),
            () -> exportarParaArquivo("pdf")
        );
        
        // Ctrl+F - Buscar
        getScene().getAccelerators().put(
            KeyCombination.keyCombination("Ctrl+F"),
            () -> txtBusca.requestFocus()
        );
        
        // Ctrl++ / Ctrl+= - Zoom In
        getScene().getAccelerators().put(
            KeyCombination.keyCombination("Ctrl+PLUS"),
            () -> aplicarZoom(0.25)
        );
        
        // Ctrl+- - Zoom Out
        getScene().getAccelerators().put(
            KeyCombination.keyCombination("Ctrl+MINUS"),
            () -> aplicarZoom(-0.25)
        );
        
        // Ctrl+0 - Zoom 100%
        getScene().getAccelerators().put(
            KeyCombination.keyCombination("Ctrl+0"),
            () -> definirZoom(1.0)
        );
        
        // F4 - Toggle thumbnails
        getScene().getAccelerators().put(
            KeyCombination.keyCombination("F4"),
            () -> btnToggleThumbnails.setSelected(!btnToggleThumbnails.isSelected())
        );
        
        // Page Down - Próxima página
        getScene().getAccelerators().put(
            KeyCombination.keyCombination("Page_Down"),
            () -> navegarPara(paginaAtual + 1)
        );
        
        // Page Up - Página anterior
        getScene().getAccelerators().put(
            KeyCombination.keyCombination("Page_Up"),
            () -> navegarPara(paginaAtual - 1)
        );
        
        // Ctrl+Home - Primeira página
        getScene().getAccelerators().put(
            KeyCombination.keyCombination("Ctrl+Home"),
            () -> navegarPara(0)
        );
        
        // Ctrl+End - Última página
        getScene().getAccelerators().put(
            KeyCombination.keyCombination("Ctrl+End"),
            () -> navegarPara(getTotalPaginas() - 1)
        );
    }

    private void buscarTexto(String texto) {
        if (texto == null || texto.isEmpty()) {
            lblStatus.setText("Digite um termo para buscar");
            return;
        }
        // Usa JavaScript do WebView para buscar
        webEngine.executeScript("if (window.find('" + texto.replace("'", "\\'") + "', false, false, true, false, false, false)) { " +
                                "   document.getSelection().collapseToEnd();" +
                                "   true; " +
                                "} else { false; }");
        lblStatus.setText("Buscando: " + texto);
    }

    private void exportarParaArquivo(String formato) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exportar como " + formato.toUpperCase());
        
        // Nome padrão com timestamp
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String nomePadrao = nomeRelatorio.replaceAll("[^a-zA-Z0-9.-]", "_") + "_" + timestamp;
        
        // Configura extensão
        switch (formato.toLowerCase()) {
            case "pdf":
                fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF (*.pdf)", "*.pdf"));
                fileChooser.setInitialFileName(nomePadrao + ".pdf");
                break;
            case "xlsx":
                fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel (*.xlsx)", "*.xlsx"));
                fileChooser.setInitialFileName(nomePadrao + ".xlsx");
                break;
            case "docx":
                fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Word (*.docx)", "*.docx"));
                fileChooser.setInitialFileName(nomePadrao + ".docx");
                break;
            case "html":
                fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("HTML (*.html)", "*.html"));
                fileChooser.setInitialFileName(nomePadrao + ".html");
                break;
            case "csv":
                fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV (*.csv)", "*.csv"));
                fileChooser.setInitialFileName(nomePadrao + ".csv");
                break;
            case "xml":
                fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML (*.xml)", "*.xml"));
                fileChooser.setInitialFileName(nomePadrao + ".xml");
                break;
        }
        
        File arquivo = fileChooser.showSaveDialog(getScene().getWindow());
        if (arquivo != null) {
            exportarComProgresso(formato, arquivo);
        }
    }
    
    private void exportarComProgresso(String formato, File arquivo) {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                updateMessage("Preparando exportação...");
                
                switch (formato.toLowerCase()) {
                    case "pdf":
                        JasperExportManager.exportReportToPdfFile(jasperPrint, arquivo.getAbsolutePath());
                        break;
                    case "xlsx":
                        JRXlsxExporter xlsxExporter = new JRXlsxExporter();
                        xlsxExporter.setExporterInput(new SimpleExporterInput(jasperPrint));
                        xlsxExporter.setExporterOutput(new SimpleOutputStreamExporterOutput(arquivo));
                        SimpleXlsxReportConfiguration xlsxConfig = new SimpleXlsxReportConfiguration();
                        xlsxConfig.setDetectCellType(true);
                        xlsxConfig.setCollapseRowSpan(false);
                        xlsxConfig.setWhitePageBackground(false);
                        xlsxExporter.setConfiguration(xlsxConfig);
                        xlsxExporter.exportReport();
                        break;
                    case "docx":
                        JRDocxExporter docxExporter = new JRDocxExporter();
                        docxExporter.setExporterInput(new SimpleExporterInput(jasperPrint));
                        docxExporter.setExporterOutput(new SimpleOutputStreamExporterOutput(arquivo));
                        docxExporter.exportReport();
                        break;
                    case "html":
                        HtmlExporter htmlExporter = new HtmlExporter();
                        htmlExporter.setExporterInput(new SimpleExporterInput(jasperPrint));
                        htmlExporter.setExporterOutput(new SimpleHtmlExporterOutput(arquivo));
                        SimpleHtmlReportConfiguration htmlConfig = new SimpleHtmlReportConfiguration();
                        htmlConfig.setEmbedImage(true);
                        htmlExporter.setConfiguration(htmlConfig);
                        htmlExporter.exportReport();
                        break;
                    case "csv":
                        JRCsvExporter csvExporter = new JRCsvExporter();
                        csvExporter.setExporterInput(new SimpleExporterInput(jasperPrint));
                        csvExporter.setExporterOutput(new SimpleWriterExporterOutput(arquivo));
                        csvExporter.exportReport();
                        break;
                    case "xml":
                        JRXmlExporter xmlExporter = new JRXmlExporter();
                        xmlExporter.setExporterInput(new SimpleExporterInput(jasperPrint));
                        xmlExporter.setExporterOutput(new SimpleXmlExporterOutput(arquivo));
                        xmlExporter.exportReport();
                        break;
                }
                
                if (onExportCallback != null) {
                    Platform.runLater(() -> onExportCallback.accept(arquivo));
                }
                return null;
            }
        };
        
        task.setOnSucceeded(e -> {
            lblStatus.setText("Exportado: " + arquivo.getName());
            progressBar.setVisible(false);
        });
        
        task.setOnRunning(e -> {
            lblStatus.setText("Exportando " + formato.toUpperCase() + "...");
            progressBar.setVisible(true);
            progressBar.setProgress(-1); // Indeterminate
        });
        
        task.setOnFailed(e -> {
            lblStatus.setText("Erro na exportação");
            progressBar.setVisible(false);
            Throwable ex = task.getException();
            if (ex != null) {
                ex.printStackTrace();
                mostrarErro("Erro ao Exportar", "Falha ao exportar para " + formato.toUpperCase() + ": " + ex.getMessage());
            }
        });
        
        THREAD_POOL.submit(task);
    }
    
    private void mostrarErro(String titulo, String mensagem) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(titulo);
            alert.setHeaderText(null);
            alert.setContentText(mensagem);
            alert.showAndWait();
        });
    }
    
    private void imprimirRelatorio() {
        // Usa a impressão nativa do WebView ou JasperPrintManager
         Task<Void> printTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                JasperPrintManager.printReport(jasperPrint, true);
                return null;
            }
        };
        THREAD_POOL.submit(printTask);
    }

    private void atualizarControlesNavegacao() {
        int total = getTotalPaginas();
        lblContadorPagina.setText(String.format("%d / %d", paginaAtual + 1, total));
        
        btnPrimeira.setDisable(paginaAtual == 0);
        btnAnterior.setDisable(paginaAtual == 0);
        btnProxima.setDisable(paginaAtual == total - 1);
        btnUltima.setDisable(paginaAtual == total - 1);
    }

    private int getTotalPaginas() {
        return jasperPrint != null ? jasperPrint.getPages().size() : 0;
    }

    // --- Inner Classes ---

    private class ThumbnailListCell extends ListCell<Integer> {
        private final ImageView imageView = new ImageView();

        public ThumbnailListCell() {
            imageView.setFitWidth(THUMBNAIL_WIDTH);
            imageView.setPreserveRatio(true);
            setGraphic(imageView);
            setAlignment(Pos.CENTER);
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        }

        @Override
        protected void updateItem(Integer pageIndex, boolean empty) {
            super.updateItem(pageIndex, empty);

            if (empty || pageIndex == null) {
                imageView.setImage(null);
                setText(null);
            } else {
                if (thumbnailCache.containsKey(pageIndex)) {
                    imageView.setImage(thumbnailCache.get(pageIndex));
                } else {
                    imageView.setImage(null); // Limpa enquanto carrega
                    carregarThumbnail(pageIndex);
                }
                setTooltip(new Tooltip("Página " + (pageIndex + 1)));
            }
        }

        private void carregarThumbnail(int index) {
            Task<Image> task = new Task<>() {
                @Override
                protected Image call() throws Exception {
                    BufferedImage bImg = (BufferedImage) JasperPrintManager.printPageToImage(jasperPrint, index, 0.2f);
                    return SwingFXUtils.toFXImage(bImg, null);
                }
            };

            task.setOnSucceeded(e -> {
                Image img = task.getValue();
                thumbnailCache.put(index, img);
                if (getItem() != null && getItem() == index) {
                    imageView.setImage(img);
                }
            });
            
            THREAD_POOL.submit(task);
        }
    }
    
    // Métodos para manter compatibilidade com código externo se necessário
    public void setJasperPrint(JasperPrint jasperPrint) {
        this.jasperPrint = jasperPrint;
        carregarRelatorio();
    }
    
    // Método depreciado mantido para evitar quebra imediata de contrato, mas sem efeito prático no novo viewer
    public void setPageRenderer(java.util.function.Function<Integer, Task<Image>> pageRenderer) {
        // No-op: O novo visualizador gerencia sua própria renderização via JasperReports
        System.out.println("Aviso: setPageRenderer foi depreciado. O JasperViewerPane agora gerencia a renderização internamente.");
    }
    
    // Getters para os botões de ação (para compatibilidade com JasperReportService)
    public Button getBtnImprimir() {
        return new Button(); // Retorna um dummy button para evitar NPE, já que a lógica é interna agora
    }
    
    public MenuItem getItemExportarPDF() { 
        // Retorna um item dummy ou o item real se precisarmos expor
        // O ideal é que a lógica de exportação seja movida para cá ou acionada via eventos
        return new MenuItem(); 
    }
    public MenuItem getItemExportarExcel() { return new MenuItem(); }
}

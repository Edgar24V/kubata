package ao.allon.kubata.faturacao.view;

import ao.allon.kubata.core.ui.table.AdvancedTableView;
import ao.allon.kubata.core.ui.table.TableUtils;
import ao.allon.kubata.faturacao.domain.Cliente;
import ao.allon.kubata.faturacao.domain.Fatura;
import ao.allon.kubata.faturacao.domain.enums.StatusFatura;
import ao.allon.kubata.faturacao.domain.enums.TipoDocumento;
import ao.allon.kubata.faturacao.service.ClienteService;
import ao.allon.kubata.faturacao.service.FaturaService;
import ao.allon.kubata.faturacao.service.JasperReportService;
import ao.allon.kubata.faturacao.service.ProdutoService;
import ao.allon.kubata.faturacao.service.SessionManager;
import ao.allon.kubata.faturacao.ui.modal.ModalService;
import ao.allon.kubata.faturacao.ui.util.AlertUtils;
import ao.allon.kubata.faturacao.ui.util.IconUtils;
import atlantafx.base.controls.Card;
import atlantafx.base.controls.CustomTextField;
import atlantafx.base.theme.Styles;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import net.sf.jasperreports.engine.JasperPrint;
import org.kordamp.ikonli.feather.Feather;

import java.io.File;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.List;
import java.util.stream.Collectors;

public class GuiasRemessaView extends VBox {

    private final FaturaService faturaService;
    private final ClienteService clienteService;
    private final JasperReportService jasperReportService;
    private final ProdutoService produtoService;
    private final ModalService modalService;

    private AdvancedTableView<Fatura> table;
    private CustomTextField txtSearch;
    private DatePicker dtInicio;
    private DatePicker dtFim;
    private ComboBox<Cliente> cbCliente;
    private ComboBox<StatusFatura> cbStatus;

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", new Locale("pt", "AO"));
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("pt", "AO"));

    private StackPane root;
    private VBox headerBox;
    private VBox body;
    private StackPane loadingOverlay;
    private VBox emptyState;

    private Label lblTotal;
    private Label lblEmitidas;
    private Label lblRascunhos;
    private Label lblCanceladas;

    private List<Fatura> baseData;

    public GuiasRemessaView(FaturaService faturaService, ClienteService clienteService, ProdutoService produtoService, JasperReportService jasperReportService, ModalService modalService) {
        this.faturaService = faturaService;
        this.clienteService = clienteService;
        this.produtoService = produtoService;
        this.jasperReportService = jasperReportService;
        this.modalService = modalService;
        setPadding(new Insets(0));
        setSpacing(0);

        initializeUI();
        refreshAsync();
    }

    private void initializeUI() {
        root = new StackPane();
        headerBox = new VBox(10);
        headerBox.setPadding(new Insets(20, 30, 10, 30));
        headerBox.setStyle("-fx-background-color: -color-bg-default; -fx-border-color: -color-border-default; -fx-border-width: 0 0 1 0;");

        body = new VBox(20);
        body.setPadding(new Insets(20, 30, 30, 30));
        body.setFillWidth(true);

        headerBox.getChildren().add(createHeader());
        body.getChildren().add(createKpis());
        body.getChildren().add(createFiltersAndTable());

        VBox layout = new VBox(headerBox, body);
        VBox.setVgrow(body, Priority.ALWAYS);

        loadingOverlay = createLoadingOverlay();
        emptyState = createEmptyState();
        emptyState.setVisible(false);
        emptyState.setManaged(false);

        root.getChildren().addAll(layout, emptyState, loadingOverlay);
        getChildren().add(root);
    }

    private HBox createHeader() {
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(5);
        Label title = new Label("Guias de Remessa");
        title.getStyleClass().addAll(Styles.TITLE_2);
        title.setFont(Font.font("System", 24));
        Label subtitle = new Label("Emissão, consulta e conversão de guias de remessa");
        subtitle.getStyleClass().addAll(Styles.TEXT_MUTED);
        titleBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnNova = new Button("Nova", IconUtils.icon(Feather.PLUS, IconUtils.SIZE_SMALL));
        btnNova.getStyleClass().addAll(Styles.SUCCESS);
        btnNova.setOnAction(e -> handleNova());

        Button btnExportar = new Button("Exportar", IconUtils.icon(Feather.DOWNLOAD, IconUtils.SIZE_SMALL));
        btnExportar.getStyleClass().addAll(Styles.BUTTON_OUTLINED);
        btnExportar.setOnAction(e -> handleExportarPdfLista());

        Button btnAtualizar = new Button("Atualizar", IconUtils.icon(Feather.REFRESH_CW, IconUtils.SIZE_SMALL));
        btnAtualizar.getStyleClass().addAll(Styles.BUTTON_OUTLINED);
        btnAtualizar.setOnAction(e -> refreshAsync());

        header.getChildren().addAll(titleBox, spacer, btnNova, btnExportar, btnAtualizar);
        return header;
    }

    private HBox createKpis() {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);

        lblTotal = new Label("0");
        lblEmitidas = new Label("0");
        lblRascunhos = new Label("0");
        lblCanceladas = new Label("0");

        row.getChildren().addAll(
            createKpiCard("Total", lblTotal, Feather.FILE_TEXT, Styles.ACCENT),
            createKpiCard("Emitidas", lblEmitidas, Feather.CHECK_CIRCLE, Styles.SUCCESS),
            createKpiCard("Rascunhos", lblRascunhos, Feather.EDIT_2, Styles.WARNING),
            createKpiCard("Canceladas", lblCanceladas, Feather.X_CIRCLE, Styles.DANGER)
        );

        return row;
    }

    private Card createKpiCard(String titulo, Label valorLabel, Feather icon, String colorStyle) {
        Card card = new Card();
        card.getStyleClass().addAll(Styles.ELEVATED_1);
        card.setMinWidth(170);
        card.setMaxWidth(220);

        VBox box = new VBox(8);
        box.setPadding(new Insets(14));

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        org.kordamp.ikonli.javafx.FontIcon fi = new org.kordamp.ikonli.javafx.FontIcon(icon);
        fi.setIconSize(22);
        fi.getStyleClass().add(colorStyle);
        Label lbl = new Label(titulo);
        lbl.getStyleClass().addAll(Styles.TEXT_MUTED);
        header.getChildren().addAll(fi, lbl);

        valorLabel.getStyleClass().addAll(Styles.TITLE_3, colorStyle);
        valorLabel.setFont(Font.font("System", 20));

        box.getChildren().addAll(header, valorLabel);
        card.setBody(box);
        return card;
    }

    private VBox createFiltersAndTable() {
        HBox filters = new HBox(12);
        filters.setAlignment(Pos.CENTER_LEFT);

        txtSearch = new CustomTextField();
        txtSearch.setPromptText("Pesquisar por número, cliente, destino...");
        txtSearch.setLeft(IconUtils.icon(Feather.SEARCH, IconUtils.SIZE_SMALL));
        txtSearch.setPrefWidth(260);
        txtSearch.textProperty().addListener((obs, ov, nv) -> applyFilter());

        dtInicio = new DatePicker();
        dtInicio.setPromptText("Início");
        dtInicio.setOnAction(e -> applyFilter());

        dtFim = new DatePicker();
        dtFim.setPromptText("Fim");
        dtFim.setOnAction(e -> applyFilter());

        cbCliente = new ComboBox<>(FXCollections.observableArrayList(clienteService.findAll()));
        cbCliente.setPromptText("Cliente");
        cbCliente.setOnAction(e -> applyFilter());

        cbStatus = new ComboBox<>(FXCollections.observableArrayList(StatusFatura.values()));
        cbStatus.setPromptText("Estado");
        cbStatus.setOnAction(e -> applyFilter());

        Button btnLimpar = new Button("Limpar", IconUtils.icon(Feather.X, IconUtils.SIZE_SMALL));
        btnLimpar.getStyleClass().addAll(Styles.BUTTON_OUTLINED);
        btnLimpar.setOnAction(e -> limparFiltros());

        filters.getChildren().addAll(txtSearch, dtInicio, dtFim, cbCliente, cbStatus, btnLimpar);

        HBox toolbar = new HBox(10);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        Button imprimir = new Button("Imprimir", IconUtils.icon(Feather.PRINTER, IconUtils.SIZE_SMALL));
        imprimir.setOnAction(e -> handleImprimir());

        Button exportPdf = new Button("PDF", IconUtils.icon(Feather.FILE_TEXT, IconUtils.SIZE_SMALL));
        exportPdf.setOnAction(e -> handleExportarPdfLista());

        Button exportXls = new Button("Excel", IconUtils.icon(Feather.DOWNLOAD, IconUtils.SIZE_SMALL));
        exportXls.setOnAction(e -> handleExportarExcelLista());

        Button converter = new Button("Converter em Fatura", IconUtils.icon(Feather.CORNER_DOWN_RIGHT, IconUtils.SIZE_SMALL));
        converter.setOnAction(e -> handleConverter());

        toolbar.getChildren().addAll(imprimir, exportPdf, exportXls, new Separator(Orientation.VERTICAL), converter);

        table = buildTable();
        VBox.setVgrow(table, Priority.ALWAYS);

        return new VBox(8, filters, toolbar, table);
    }

    private AdvancedTableView<Fatura> buildTable() {
        table = new AdvancedTableView<>();
        TableUtils.standardize(table);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        TableColumn<Fatura, String> colNumero = new TableColumn<>("Número");
        colNumero.setCellValueFactory(new PropertyValueFactory<>("numero"));
        TableColumn<Fatura, LocalDate> colEmissao = new TableColumn<>("Emissão");
        colEmissao.setCellValueFactory(new PropertyValueFactory<>("dataEmissao"));
        colEmissao.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.format(dateFormatter));
            }
        });
        TableColumn<Fatura, String> colCliente = new TableColumn<>("Cliente");
        colCliente.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getCliente() != null ? c.getValue().getCliente().getNome() : "Consumidor Final"));
        TableColumn<Fatura, String> colOrigem = new TableColumn<>("Origem");
        colOrigem.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getLocalCarga()));
        TableColumn<Fatura, String> colDestino = new TableColumn<>("Destino");
        colDestino.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getLocalDescarga()));
        TableColumn<Fatura, String> colMotorista = new TableColumn<>("Motorista");
        colMotorista.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getMotorista()));
        TableColumn<Fatura, String> colMatricula = new TableColumn<>("Matrícula");
        colMatricula.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getMatriculaViatura()));
        TableColumn<Fatura, StatusFatura> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        TableColumn<Fatura, Void> colAcoes = new TableColumn<>("");
        colAcoes.setMinWidth(44);
        colAcoes.setMaxWidth(64);
        colAcoes.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("", IconUtils.icon(Feather.MORE_VERTICAL, IconUtils.SIZE_SMALL));
            {
                btn.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.FLAT);
                btn.setOnAction(e -> {
                    Fatura f = getTableRow() != null ? getTableRow().getItem() : null;
                    if (f == null) return;
                    ContextMenu cm = buildRowMenu(f);
                    cm.show(btn, javafx.geometry.Side.BOTTOM, 0, 0);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
                setText(null);
                setAlignment(Pos.CENTER);
            }
        });

        table.getColumns().addAll(colNumero, colEmissao, colCliente, colOrigem, colDestino, colMotorista, colMatricula, colStatus, colAcoes);

        table.setRowFactory(tv -> {
            TableRow<Fatura> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (row.isEmpty()) return;
                if (event.getButton() == javafx.scene.input.MouseButton.SECONDARY) {
                    ContextMenu cm = buildRowMenu(row.getItem());
                    cm.show(row, event.getScreenX(), event.getScreenY());
                }
            });
            return row;
        });
        VBox.setVgrow(table, Priority.ALWAYS);
        return table;
    }

    private void refreshAsync() {
        showLoading(true);
        Thread t = new Thread(() -> {
            try {
                LocalDate inicio = dtInicio != null ? dtInicio.getValue() : null;
                LocalDate fim = dtFim != null ? dtFim.getValue() : null;
                Long clienteId = cbCliente != null && cbCliente.getValue() != null ? cbCliente.getValue().getId() : null;
                StatusFatura status = cbStatus != null ? cbStatus.getValue() : null;
                List<Fatura> guias = faturaService.buscarGuiasRemessa(inicio, fim, clienteId, status);
                Platform.runLater(() -> {
                    baseData = guias;
                    table.setData(FXCollections.observableArrayList(baseData));
                    applyFilter();
                    refreshKpis();
                });
            } catch (Exception ex) {
                Platform.runLater(() -> AlertUtils.showExceptionAlert("Erro", "Falha ao carregar Guias de Remessa.", ex));
            } finally {
                Platform.runLater(() -> showLoading(false));
            }
        }, "guias-remessa-reload");
        t.setDaemon(true);
        t.start();
    }

    private void applyFilter() {
        if (baseData == null) return;
        String q = txtSearch != null && txtSearch.getText() != null ? txtSearch.getText().trim().toLowerCase() : "";
        
        table.setFilter(f -> {
            if (q.isEmpty()) return true;
            String num = f.getNumero() != null ? f.getNumero().toLowerCase() : "";
            String cli = f.getCliente() != null && f.getCliente().getNome() != null ? f.getCliente().getNome().toLowerCase() : "consumidor final";
            String dest = f.getLocalDescarga() != null ? f.getLocalDescarga().toLowerCase() : "";
            return num.contains(q) || cli.contains(q) || dest.contains(q) || String.valueOf(f.getId()).contains(q);
        });
        
        updateEmptyState();
    }

    private void limparFiltros() {
        if (txtSearch != null) txtSearch.clear();
        if (dtInicio != null) dtInicio.setValue(null);
        if (dtFim != null) dtFim.setValue(null);
        if (cbCliente != null) cbCliente.setValue(null);
        if (cbStatus != null) cbStatus.setValue(null);
        refreshAsync();
    }

    private void refreshKpis() {
        List<Fatura> list = baseData != null ? baseData : java.util.Collections.emptyList();
        long total = list.size();
        long emit = list.stream().filter(f -> f.getStatus() == StatusFatura.EMITIDA).count();
        long rasc = list.stream().filter(f -> f.getStatus() == StatusFatura.RASCUNHO).count();
        long canc = list.stream().filter(f -> f.getStatus() == StatusFatura.CANCELADA).count();
        if (lblTotal != null) lblTotal.setText(String.valueOf(total));
        if (lblEmitidas != null) lblEmitidas.setText(String.valueOf(emit));
        if (lblRascunhos != null) lblRascunhos.setText(String.valueOf(rasc));
        if (lblCanceladas != null) lblCanceladas.setText(String.valueOf(canc));
    }

    private ContextMenu buildRowMenu(Fatura f) {
        MenuItem miImprimir = new MenuItem("Imprimir", IconUtils.icon(Feather.PRINTER, IconUtils.SIZE_SMALL));
        miImprimir.setOnAction(e -> handleImprimir());
        MenuItem miConverter = new MenuItem("Converter em Fatura", IconUtils.icon(Feather.CORNER_DOWN_RIGHT, IconUtils.SIZE_SMALL));
        miConverter.setOnAction(e -> handleConverter());
        MenuItem miPdf = new MenuItem("Exportar PDF (Lista)", IconUtils.icon(Feather.FILE_TEXT, IconUtils.SIZE_SMALL));
        miPdf.setOnAction(e -> handleExportarPdfLista());
        MenuItem miXls = new MenuItem("Exportar Excel (Lista)", IconUtils.icon(Feather.DOWNLOAD, IconUtils.SIZE_SMALL));
        miXls.setOnAction(e -> handleExportarExcelLista());
        return new ContextMenu(miImprimir, miConverter, new SeparatorMenuItem(), miPdf, miXls);
    }

    private StackPane createLoadingOverlay() {
        ProgressIndicator pi = new ProgressIndicator();
        pi.setMaxSize(72, 72);
        Label lbl = new Label("A carregar...");
        lbl.getStyleClass().addAll(Styles.TEXT_MUTED);
        VBox box = new VBox(10, pi, lbl);
        box.setAlignment(Pos.CENTER);

        StackPane overlay = new StackPane(box);
        overlay.setVisible(false);
        overlay.setManaged(false);
        overlay.setPickOnBounds(true);
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.08);");
        return overlay;
    }

    private VBox createEmptyState() {
        Label title = new Label("Sem resultados");
        title.getStyleClass().addAll(Styles.TITLE_3);
        Label sub = new Label("Nenhuma guia de remessa encontrada com os filtros atuais.");
        sub.getStyleClass().addAll(Styles.TEXT_MUTED);
        Button btn = new Button("Criar Guia", IconUtils.icon(Feather.PLUS, IconUtils.SIZE_SMALL));
        btn.getStyleClass().addAll(Styles.SUCCESS);
        btn.setOnAction(e -> handleNova());

        VBox box = new VBox(8, title, sub, btn);
        box.setAlignment(Pos.CENTER);
        box.setMaxWidth(420);
        box.setStyle("-fx-background-color: -color-bg-subtle; -fx-background-radius: 10; -fx-padding: 18; -fx-border-color: -color-border-default; -fx-border-radius: 10;");

        VBox wrap = new VBox(box);
        wrap.setAlignment(Pos.CENTER);
        wrap.setPickOnBounds(false);
        return wrap;
    }

    private void updateEmptyState() {
        if (emptyState == null) return;
        boolean show = table != null && table.getItems() != null && table.getItems().isEmpty();
        emptyState.setVisible(show);
        emptyState.setManaged(show);
    }

    private void showLoading(boolean show) {
        if (loadingOverlay == null) return;
        loadingOverlay.setVisible(show);
        loadingOverlay.setManaged(show);
    }

    private Fatura getSelected() {
        return table.getSelectionModel().getSelectedItem();
    }

    private void handleNova() {
        NovaGuiaRemessaView view = new NovaGuiaRemessaView(faturaService, clienteService, produtoService, modalService);
        view.setPrefWidth(700);
        modalService.create().title("Nova Guia de Remessa").content(view).dynamicSize().withCancelButton().buildAndShow();
    }

    private void handleImprimir() {
        Fatura f = getSelected();
        if (f == null) {
            AlertUtils.showWarningAlert("Aviso", "Selecione uma Guia.");
            return;
        }
        try {
            Fatura comp = faturaService.findFaturaParaImpressao(f.getId()).orElse(f);
            JasperPrint jp = jasperReportService.prepararFatura(comp);
            jp.setName("Guia " + (comp.getNumero() != null ? comp.getNumero() : ""));
            jasperReportService.showReport(jp);
        } catch (Exception ex) {
            AlertUtils.showExceptionAlert("Erro", "Falha ao preparar relatório.", ex);
        }
    }

    private void handleExportarPdfLista() {
        try {
            List<Fatura> lista = table.getItems().stream().collect(Collectors.toList());
            JasperPrint jp = jasperReportService.prepararListaGuiasRemessa(lista);
            FileChooser fc = new FileChooser();
            fc.setTitle("Salvar Lista de Guias (PDF)");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
            fc.setInitialFileName("guias_remessa.pdf");
            File file = fc.showSaveDialog(getScene().getWindow());
            if (file != null) {
                jasperReportService.exportToPdf(jp, file);
                AlertUtils.showInfoAlert("Sucesso", "Arquivo gerado em: " + file.getAbsolutePath());
            }
        } catch (Exception ex) {
            AlertUtils.showExceptionAlert("Erro", "Falha ao exportar PDF.", ex);
        }
    }

    private void handleExportarExcelLista() {
        try {
            List<Fatura> lista = table.getItems().stream().collect(Collectors.toList());
            JasperPrint jp = jasperReportService.prepararListaGuiasRemessa(lista);
            FileChooser fc = new FileChooser();
            fc.setTitle("Salvar Lista de Guias (Excel)");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files", "*.xls"));
            fc.setInitialFileName("guias_remessa.xls");
            File file = fc.showSaveDialog(getScene().getWindow());
            if (file != null) {
                jasperReportService.exportToXls(jp, file);
                AlertUtils.showInfoAlert("Sucesso", "Arquivo gerado em: " + file.getAbsolutePath());
            }
        } catch (Exception ex) {
            AlertUtils.showExceptionAlert("Erro", "Falha ao exportar Excel.", ex);
        }
    }

    private void handleConverter() {
        Fatura f = getSelected();
        if (f == null) {
            AlertUtils.showWarningAlert("Aviso", "Selecione uma Guia.");
            return;
        }
        try {
            faturaService.converterGuiaEmFatura(f.getId());
            AlertUtils.showInfoAlert("Sucesso", "Guia convertida em Fatura (rascunho).");
            refreshAsync();
        } catch (Exception ex) {
            AlertUtils.showExceptionAlert("Erro", "Falha ao converter guia.", ex);
        }
    }
}

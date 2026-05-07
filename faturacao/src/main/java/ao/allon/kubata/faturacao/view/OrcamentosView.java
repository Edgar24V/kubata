package ao.allon.kubata.faturacao.view;

import ao.allon.kubata.core.ui.table.AdvancedTableView;
import ao.allon.kubata.core.ui.table.TableUtils;
import ao.allon.kubata.faturacao.domain.Fatura;
import ao.allon.kubata.faturacao.domain.enums.StatusFatura;
import ao.allon.kubata.faturacao.domain.enums.TipoDocumento;
import ao.allon.kubata.faturacao.service.ClienteService;
import ao.allon.kubata.faturacao.service.FaturaService;
import ao.allon.kubata.faturacao.service.JasperReportService;
import ao.allon.kubata.faturacao.service.ProdutoService;
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
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.List;
import java.util.stream.Collectors;

public class OrcamentosView extends VBox {

    private final FaturaService faturaService;
    private final ClienteService clienteService;
    private final ProdutoService produtoService;
    private final JasperReportService jasperReportService;
    private final ModalService modalService;

    private AdvancedTableView<Fatura> table;
    private CustomTextField txtSearch;
    private DatePicker dpInicio;
    private DatePicker dpFim;
    private ComboBox<StatusFatura> cbStatus;

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", new Locale("pt", "AO"));
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("pt", "AO"));

    private StackPane root;
    private VBox headerBox;
    private VBox body;
    private StackPane loadingOverlay;
    private VBox emptyState;

    private Label lblTotalOrcamentos;
    private Label lblValorTotal;
    private Label lblRascunhos;
    private Label lblEmitidos;

    private List<Fatura> baseData;

    public OrcamentosView(FaturaService faturaService, ClienteService clienteService, ProdutoService produtoService, JasperReportService jasperReportService, ModalService modalService) {
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
        Label title = new Label("Orçamentos");
        title.getStyleClass().addAll(Styles.TITLE_2);
        title.setFont(Font.font("System", 24));

        Label subtitle = new Label("Criação, emissão e conversão de orçamentos");
        subtitle.getStyleClass().add(Styles.TEXT_MUTED);
        titleBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnNovo = new Button("Novo", IconUtils.icon(Feather.PLUS, IconUtils.SIZE_SMALL));
        btnNovo.getStyleClass().addAll(Styles.SUCCESS);
        btnNovo.setOnAction(e -> handleNovo());

        Button btnExportar = new Button("Exportar", IconUtils.icon(Feather.DOWNLOAD, IconUtils.SIZE_SMALL));
        btnExportar.getStyleClass().addAll(Styles.BUTTON_OUTLINED);
        btnExportar.setOnAction(e -> handleExportarExcel());

        Button btnAtualizar = new Button("Atualizar", IconUtils.icon(Feather.REFRESH_CW, IconUtils.SIZE_SMALL));
        btnAtualizar.getStyleClass().addAll(Styles.BUTTON_OUTLINED);
        btnAtualizar.setOnAction(e -> refreshAsync());

        header.getChildren().addAll(titleBox, spacer, btnNovo, btnExportar, btnAtualizar);
        return header;
    }

    private HBox createKpis() {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);

        lblTotalOrcamentos = new Label("0");
        lblValorTotal = new Label(currencyFormat.format(0));
        lblRascunhos = new Label("0");
        lblEmitidos = new Label("0");

        row.getChildren().addAll(
            createKpiCard("Total", lblTotalOrcamentos, Feather.FILE_TEXT, Styles.ACCENT),
            createKpiCard("Valor Total", lblValorTotal, Feather.DOLLAR_SIGN, Styles.SUCCESS),
            createKpiCard("Rascunhos", lblRascunhos, Feather.EDIT_2, Styles.WARNING),
            createKpiCard("Emitidos", lblEmitidos, Feather.CHECK_CIRCLE, Styles.SUCCESS)
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
        FontIcon fi = new FontIcon(icon);
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
        txtSearch.setPromptText("Pesquisar por número, cliente...");
        txtSearch.setLeft(IconUtils.icon(Feather.SEARCH, IconUtils.SIZE_SMALL));
        txtSearch.setPrefWidth(260);
        txtSearch.textProperty().addListener((obs, ov, nv) -> applyFilter());

        cbStatus = new ComboBox<>();
        cbStatus.setPromptText("Status");
        cbStatus.getItems().addAll(StatusFatura.values());
        cbStatus.setOnAction(e -> applyFilter());
        cbStatus.setPrefWidth(140);

        dpInicio = new DatePicker();
        dpInicio.setPromptText("Início");
        dpInicio.setOnAction(e -> applyFilter());

        dpFim = new DatePicker();
        dpFim.setPromptText("Fim");
        dpFim.setOnAction(e -> applyFilter());

        Button btnLimpar = new Button("Limpar", IconUtils.icon(Feather.X, IconUtils.SIZE_SMALL));
        btnLimpar.getStyleClass().addAll(Styles.BUTTON_OUTLINED);
        btnLimpar.setOnAction(e -> limparFiltros());

        filters.getChildren().addAll(txtSearch, cbStatus, dpInicio, dpFim, btnLimpar);

        HBox toolbar = new HBox(10);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        Button editar = new Button("Editar", IconUtils.icon(Feather.EDIT_2, IconUtils.SIZE_SMALL));
        editar.setOnAction(e -> handleEditar());

        Button emitir = new Button("Emitir", IconUtils.icon(Feather.CHECK, IconUtils.SIZE_SMALL));
        emitir.setOnAction(e -> handleEmitir());

        Button imprimir = new Button("Imprimir", IconUtils.icon(Feather.PRINTER, IconUtils.SIZE_SMALL));
        imprimir.setOnAction(e -> handleImprimir());

        Button converter = new Button("Converter em Fatura", IconUtils.icon(Feather.CORNER_DOWN_RIGHT, IconUtils.SIZE_SMALL));
        converter.setOnAction(e -> handleConverter());

        Button cancelar = new Button("Cancelar", IconUtils.icon(Feather.X, IconUtils.SIZE_SMALL));
        cancelar.getStyleClass().addAll(Styles.DANGER);
        cancelar.setOnAction(e -> handleCancelar());

        toolbar.getChildren().addAll(
            editar, emitir, imprimir,
            new Separator(Orientation.VERTICAL),
            converter, cancelar
        );

        table = buildTable();
        VBox.setVgrow(table, Priority.ALWAYS);

        return new VBox(8, filters, toolbar, table);
    }

    private AdvancedTableView<Fatura> buildTable() {
        table = new AdvancedTableView<>();
        TableUtils.standardize(table);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        
        TableColumn<Fatura, Long> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colId.setMinWidth(60);
        colId.setMaxWidth(80);
        
        TableColumn<Fatura, String> colNumero = new TableColumn<>("Número");
        colNumero.setCellValueFactory(new PropertyValueFactory<>("numero"));
        colNumero.setMinWidth(120);
        
        TableColumn<Fatura, String> colCliente = new TableColumn<>("Cliente");
        colCliente.setCellValueFactory(cellData -> {
            if (cellData.getValue().getCliente() != null) {
                return new javafx.beans.property.SimpleStringProperty(cellData.getValue().getCliente().getNome());
            }
            return new javafx.beans.property.SimpleStringProperty("Consumidor Final");
        });

        TableColumn<Fatura, LocalDate> colEmissao = new TableColumn<>("Emissão");
        colEmissao.setCellValueFactory(new PropertyValueFactory<>("dataEmissao"));
        colEmissao.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.format(dateFormatter));
            }
        });
        
        TableColumn<Fatura, LocalDate> colVencimento = new TableColumn<>("Validade");
        colVencimento.setCellValueFactory(new PropertyValueFactory<>("dataVencimento"));
        colVencimento.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.format(dateFormatter));
            }
        });
        
        TableColumn<Fatura, BigDecimal> colTotal = new TableColumn<>("Total");
        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        colTotal.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : currencyFormat.format(item));
                if (!empty && item != null) setAlignment(Pos.CENTER_RIGHT);
            }
        });
        colTotal.setMinWidth(140);
        
        TableColumn<Fatura, ao.allon.kubata.faturacao.domain.enums.StatusFatura> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colStatus.setMinWidth(110);

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
        
        table.getColumns().addAll(colId, colNumero, colCliente, colEmissao, colVencimento, colTotal, colStatus, colAcoes);

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
                List<Fatura> todas = faturaService.findAll();
                List<Fatura> orcamentos = todas.stream()
                        .filter(f -> f.getTipoDocumento() == TipoDocumento.ORCAMENTO)
                        .collect(Collectors.toList());
                Platform.runLater(() -> {
                    baseData = orcamentos;
                    table.setData(FXCollections.observableArrayList(baseData));
                    applyFilter();
                    refreshKpis();
                });
            } catch (Exception ex) {
                Platform.runLater(() -> AlertUtils.showExceptionAlert("Erro", "Falha ao carregar Orçamentos.", ex));
            } finally {
                Platform.runLater(() -> showLoading(false));
            }
        }, "orcamentos-reload");
        t.setDaemon(true);
        t.start();
    }

    private void applyFilter() {
        if (baseData == null) return;

        String q = txtSearch != null && txtSearch.getText() != null ? txtSearch.getText().trim().toLowerCase() : "";
        StatusFatura st = cbStatus != null ? cbStatus.getValue() : null;
        LocalDate ini = dpInicio != null ? dpInicio.getValue() : null;
        LocalDate fim = dpFim != null ? dpFim.getValue() : null;

        table.setFilter(f -> {
            if (!q.isEmpty()) {
                String num = f.getNumero() != null ? f.getNumero().toLowerCase() : "";
                String cli = f.getCliente() != null && f.getCliente().getNome() != null ? f.getCliente().getNome().toLowerCase() : "consumidor final";
                if (!(num.contains(q) || cli.contains(q) || String.valueOf(f.getId()).contains(q))) return false;
            }
            if (st != null && f.getStatus() != st) return false;
            if (ini != null && (f.getDataEmissao() == null || f.getDataEmissao().isBefore(ini))) return false;
            if (fim != null && (f.getDataEmissao() == null || f.getDataEmissao().isAfter(fim))) return false;
            return true;
        });

        updateEmptyState();
    }

    private void limparFiltros() {
        if (txtSearch != null) txtSearch.clear();
        if (cbStatus != null) cbStatus.setValue(null);
        if (dpInicio != null) dpInicio.setValue(null);
        if (dpFim != null) dpFim.setValue(null);
        applyFilter();
    }

    private void refreshKpis() {
        List<Fatura> list = baseData != null ? baseData : java.util.Collections.emptyList();
        long total = list.size();
        long rasc = list.stream().filter(f -> f.getStatus() == StatusFatura.RASCUNHO).count();
        long emit = list.stream().filter(f -> f.getStatus() == StatusFatura.EMITIDA).count();
        BigDecimal soma = list.stream().map(Fatura::getTotal).filter(v -> v != null).reduce(BigDecimal.ZERO, BigDecimal::add);

        if (lblTotalOrcamentos != null) lblTotalOrcamentos.setText(String.valueOf(total));
        if (lblRascunhos != null) lblRascunhos.setText(String.valueOf(rasc));
        if (lblEmitidos != null) lblEmitidos.setText(String.valueOf(emit));
        if (lblValorTotal != null) lblValorTotal.setText(currencyFormat.format(soma));
    }

    private ContextMenu buildRowMenu(Fatura f) {
        MenuItem miEditar = new MenuItem("Editar", IconUtils.icon(Feather.EDIT_2, IconUtils.SIZE_SMALL));
        miEditar.setOnAction(e -> handleEditar());

        MenuItem miEmitir = new MenuItem("Emitir", IconUtils.icon(Feather.CHECK, IconUtils.SIZE_SMALL));
        miEmitir.setOnAction(e -> handleEmitir());

        MenuItem miImprimir = new MenuItem("Imprimir", IconUtils.icon(Feather.PRINTER, IconUtils.SIZE_SMALL));
        miImprimir.setOnAction(e -> handleImprimir());

        MenuItem miExportar = new MenuItem("Exportar Excel", IconUtils.icon(Feather.DOWNLOAD, IconUtils.SIZE_SMALL));
        miExportar.setOnAction(e -> handleExportarExcel());

        MenuItem miConverter = new MenuItem("Converter em Fatura", IconUtils.icon(Feather.CORNER_DOWN_RIGHT, IconUtils.SIZE_SMALL));
        miConverter.setOnAction(e -> handleConverter());

        MenuItem miCancelar = new MenuItem("Cancelar", IconUtils.icon(Feather.X, IconUtils.SIZE_SMALL));
        miCancelar.setOnAction(e -> handleCancelar());

        return new ContextMenu(miEditar, miEmitir, miImprimir, new SeparatorMenuItem(), miExportar, miConverter, new SeparatorMenuItem(), miCancelar);
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
        Label sub = new Label("Nenhum orçamento encontrado com os filtros atuais.");
        sub.getStyleClass().addAll(Styles.TEXT_MUTED);
        Button btn = new Button("Criar Orçamento", IconUtils.icon(Feather.PLUS, IconUtils.SIZE_SMALL));
        btn.getStyleClass().addAll(Styles.SUCCESS);
        btn.setOnAction(e -> handleNovo());

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

    private void handleNovo() {
        NovaFaturaView view = new NovaFaturaView(faturaService, clienteService, produtoService, modalService, TipoDocumento.ORCAMENTO);
        view.setPrefWidth(680);
        modalService.create()
                .title("Novo Orçamento")
                .content(view)
                .dynamicSize()
                .buildAndShow();
        // Nota: O refresh precisa ser chamado após o fechamento do modal.
        // Como o modal é assíncrono (JavaFX), idealmente teríamos um callback.
        // Por simplicidade, o usuário pode clicar em Atualizar ou implementamos um evento global.
    }

    private void handleEditar() {
        Fatura f = getSelected();
        if (f == null) {
            AlertUtils.showWarningAlert("Aviso", "Selecione um Orçamento.");
            return;
        }
        if (f.getStatus() != StatusFatura.RASCUNHO) {
            AlertUtils.showWarningAlert("Aviso", "Apenas rascunhos podem ser editados.");
            return;
        }
        
        // Recarregar com itens para evitar LazyInitializationException
        Fatura faturaCompleta = faturaService.findFaturaParaImpressao(f.getId()).orElse(f);

        NovaFaturaView view = new NovaFaturaView(faturaService, clienteService, produtoService, modalService, TipoDocumento.ORCAMENTO, faturaCompleta);
        view.setPrefWidth(680);
        modalService.create()
                .title("Editar Orçamento")
                .content(view)
                .dynamicSize()
                .buildAndShow();
    }

    private void handleEmitir() {
        Fatura f = getSelected();
        if (f == null) {
            AlertUtils.showWarningAlert("Aviso", "Selecione um Orçamento.");
            return;
        }
        if (f.getStatus() == StatusFatura.EMITIDA) {
            AlertUtils.showWarningAlert("Aviso", "Orçamento já emitido.");
            return;
        }
        try {
            // Recarregar com itens para garantir integridade
            Fatura faturaCompleta = faturaService.findFaturaParaImpressao(f.getId()).orElse(f);
            faturaService.emitirFatura(faturaCompleta.getId());
            refreshAsync();
            AlertUtils.showInfoAlert("Sucesso", "Orçamento emitido com sucesso.");
        } catch (Exception ex) {
            AlertUtils.showExceptionAlert("Erro", "Falha ao emitir Orçamento.", ex);
        }
    }

    private void handleImprimir() {
        Fatura f = getSelected();
        if (f == null) {
            AlertUtils.showWarningAlert("Aviso", "Selecione um Orçamento.");
            return;
        }
        try {
            Fatura faturaCompleta = faturaService.findFaturaParaImpressao(f.getId())
                    .orElseThrow(() -> new IllegalStateException("Orçamento não encontrado para impressão. ID: " + f.getId()));
            JasperPrint jp = jasperReportService.prepararFatura(faturaCompleta);
            jp.setName("Orçamento " + (faturaCompleta.getNumero() != null ? faturaCompleta.getNumero() : ""));
            jasperReportService.showReport(jp);
        } catch (Exception ex) {
            AlertUtils.showExceptionAlert("Erro", "Falha ao preparar relatório.", ex);
        }
    }
    
    private void handleExportarExcel() {
        Fatura f = getSelected();
        if (f == null) {
            AlertUtils.showWarningAlert("Aviso", "Selecione um Orçamento.");
            return;
        }
        try {
            Fatura faturaCompleta = faturaService.findFaturaParaImpressao(f.getId())
                    .orElseThrow(() -> new IllegalStateException("Orçamento não encontrado. ID: " + f.getId()));
            
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Salvar Orçamento em Excel");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files", "*.xls"));
            fileChooser.setInitialFileName("Orcamento_" + (faturaCompleta.getNumero() != null ? faturaCompleta.getNumero().replace("/", "_") : "novo") + ".xls");
            
            File file = fileChooser.showSaveDialog(getScene().getWindow());
            if (file != null) {
                JasperPrint jp = jasperReportService.prepararFatura(faturaCompleta);
                jasperReportService.exportToXls(jp, file);
                AlertUtils.showInfoAlert("Sucesso", "Arquivo salvo em: " + file.getAbsolutePath());
            }
        } catch (Exception ex) {
            AlertUtils.showExceptionAlert("Erro", "Falha ao exportar para Excel.", ex);
        }
    }

    private void handleCancelar() {
        Fatura f = getSelected();
        if (f == null) {
            AlertUtils.showWarningAlert("Aviso", "Selecione um Orçamento.");
            return;
        }
        TextField txtMotivo = new TextField();
        VBox content = new VBox(10, new Label("Motivo do cancelamento"), txtMotivo);
        content.setPadding(new Insets(10));
        modalService.create()
                .title("Cancelar Orçamento")
                .content(content)
                .autoSize()
                .withConfirmButton("Confirmar", () -> {
                    try {
                        String motivo = txtMotivo.getText();
                        if (motivo == null || motivo.isEmpty()) return false;
                        faturaService.cancelarFatura(f.getId(), motivo);
                        refreshAsync();
                        return true;
                    } catch (Exception ex) {
                        AlertUtils.showExceptionAlert("Erro", "Falha ao cancelar Orçamento.", ex);
                        return false;
                    }
                })
                .withCancelButton("Fechar")
                .buildAndShow();
    }

    private void handleConverter() {
        Fatura f = getSelected();
        if (f == null) {
            AlertUtils.showWarningAlert("Aviso", "Selecione um Orçamento.");
            return;
        }
        try {
            // Recarregar com itens para garantir integridade na conversão
            faturaService.converterProFormaEmFatura(f.getId());
            AlertUtils.showInfoAlert("Sucesso", "Convertido em Fatura (rascunho).");
            refreshAsync();
        } catch (Exception ex) {
            AlertUtils.showExceptionAlert("Erro", "Falha ao converter em Fatura.", ex);
        }
    }
}

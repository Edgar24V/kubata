package ao.allon.kubata.faturacao.view;

import ao.allon.kubata.core.ui.table.AdvancedTableView;
import ao.allon.kubata.core.ui.table.TableUtils;
import ao.allon.kubata.faturacao.domain.Fatura;
import ao.allon.kubata.faturacao.domain.Cliente;
import ao.allon.kubata.faturacao.domain.enums.StatusFatura;
import ao.allon.kubata.faturacao.domain.enums.EstadoDocumento;
import ao.allon.kubata.faturacao.service.FaturaService;
import ao.allon.kubata.faturacao.service.ClienteService;
import ao.allon.kubata.faturacao.service.SessionManager;
import ao.allon.kubata.faturacao.service.JasperReportService;
import ao.allon.kubata.faturacao.service.EmailService;
import ao.allon.kubata.faturacao.service.agt.AGTElectronicInvoiceService;
import ao.allon.kubata.faturacao.service.agt.dto.AGTResponseDTO;
import ao.allon.kubata.faturacao.service.report.InvoiceReportService;
import ao.allon.kubata.faturacao.service.report.InvoiceReportData;
import ao.allon.kubata.faturacao.service.report.InvoiceItemData;
import ao.allon.kubata.faturacao.ui.modal.ModalService;
import ao.allon.kubata.faturacao.ui.util.AlertUtils;
import ao.allon.kubata.faturacao.ui.util.IconUtils;
import atlantafx.base.controls.Card;
import atlantafx.base.controls.CustomTextField;
import atlantafx.base.theme.Styles;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Orientation;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import ao.allon.kubata.faturacao.ui.loading.LoadingService;
import ao.allon.kubata.faturacao.ui.loading.LoadingScreen;
import javafx.application.Platform;
import javafx.scene.input.MouseButton;
import net.sf.jasperreports.engine.JasperPrint;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.context.ApplicationContext;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
import java.util.Map;
import java.util.stream.Collectors;

public class FaturasView extends BorderPane {

    private final FaturaService faturaService;
    private final ClienteService clienteService;
    private final SessionManager sessionManager;
    private final JasperReportService jasperReportService;
    private final InvoiceReportService invoiceReportService;
    private final EmailService emailService;
    private final ApplicationContext applicationContext;
    private final ModalService modalService;
    private final ao.allon.kubata.faturacao.service.DocumentoEstadoService documentoEstadoService;
    private final AGTElectronicInvoiceService agtElectronicInvoiceService;

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", new Locale("pt", "AO"));
    private final DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("MMM/yyyy", new Locale("pt", "AO"));
    private final ObservableList<Fatura> masterData = FXCollections.observableArrayList();

    // KPI Labels
    private Label lblFaturamentoDia;
    private Label lblFaturamentoMes;
    private Label lblEmAberto;
    private Label lblTotalFaturas;

    // UI Components
    private AdvancedTableView<Fatura> table;
    private CustomTextField txtSearch;
    private ComboBox<Cliente> cbCliente;
    private ComboBox<StatusFatura> cbStatus;
    private DatePicker dpInicio;
    private DatePicker dpFim;
    private BarChart<String, Number> chartFaturamento;

    private TableColumn<Fatura, ao.allon.kubata.faturacao.domain.enums.DocumentStatus> colDocStatus;
    private TableColumn<Fatura, String> colAgtStatus;

    private StackPane root;
    private VBox headerBox;
    private VBox body;
    private StackPane loadingOverlay;
    private VBox emptyState;

    private final LoadingService loadingService;

    public FaturasView(FaturaService faturaService, SessionManager sessionManager, JasperReportService jasperReportService, EmailService emailService, ApplicationContext applicationContext, ModalService modalService, ao.allon.kubata.faturacao.service.DocumentoEstadoService documentoEstadoService, LoadingService loadingService, AGTElectronicInvoiceService agtElectronicInvoiceService) {
        this.faturaService = faturaService;
        this.sessionManager = sessionManager;
        this.jasperReportService = jasperReportService;
        this.invoiceReportService = applicationContext.getBean(InvoiceReportService.class);
        this.emailService = emailService;
        this.applicationContext = applicationContext;
        this.modalService = modalService;
        this.documentoEstadoService = documentoEstadoService;
        this.loadingService = loadingService;
        this.clienteService = applicationContext.getBean(ClienteService.class);
        this.agtElectronicInvoiceService = agtElectronicInvoiceService;

        getStyleClass().add("faturas-view");
        initializeUI();
        loadData();
        refreshKPIs();
    }

    private void initializeUI() {
        setPadding(new Insets(0));

        root = new StackPane();
        headerBox = new VBox(10);
        headerBox.setPadding(new Insets(20, 30, 10, 30));
        headerBox.setStyle("-fx-background-color: transparent; -fx-background: -color-bg-default; -fx-border-color: -color-border-default; -fx-border-width: 0 0 1 0;");

        body = new VBox(20);
        body.setPadding(new Insets(20, 30, 30, 30));
        body.setFillWidth(true);

        headerBox.getChildren().add(createHeader());
        body.getChildren().add(createKPISection());
        body.getChildren().add(createFiltersAndTableSection());
        body.getChildren().add(createChartSection());

        VBox layout = new VBox(headerBox, body);
        VBox.setVgrow(body, Priority.ALWAYS);

        loadingOverlay = createLoadingOverlay();
        emptyState = createEmptyState();
        emptyState.setVisible(false);
        emptyState.setManaged(false);

        root.getChildren().addAll(layout, emptyState, loadingOverlay);
        setCenter(root);
    }

    private HBox createHeader() {
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(5);
        Label title = new Label("Gestão de Faturas");
        title.getStyleClass().addAll(Styles.TITLE_2);
        title.setFont(Font.font("System", FontWeight.BOLD, 24));

        Label subtitle = new Label("Controle completo de faturação e documentos fiscais");
        subtitle.getStyleClass().add(Styles.TEXT_MUTED);
        titleBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnNova = new Button("Novo", IconUtils.icon(Feather.PLUS, IconUtils.SIZE_SMALL));
        btnNova.getStyleClass().addAll(Styles.SUCCESS);
        btnNova.setOnAction(e -> criarNovaFatura());

        Button btnExportar = new Button("Exportar", IconUtils.icon(Feather.DOWNLOAD, IconUtils.SIZE_SMALL));
        btnExportar.getStyleClass().addAll(Styles.BUTTON_OUTLINED);
        btnExportar.setOnAction(e -> exportarCSV());

        Button btnAtualizar = new Button("Atualizar", IconUtils.icon(Feather.REFRESH_CW, IconUtils.SIZE_SMALL));
        btnAtualizar.getStyleClass().addAll(Styles.BUTTON_OUTLINED);
        btnAtualizar.setOnAction(e -> recarregarAsync());

        header.getChildren().addAll(titleBox, spacer, btnNova, btnExportar, btnAtualizar);
        return header;
    }

    private FlowPane createKPISection() {
        FlowPane pane = new FlowPane();
        pane.setHgap(20);
        pane.setVgap(20);
        pane.setAlignment(Pos.TOP_LEFT);

        lblFaturamentoDia = new Label("Kz 0,00");
        lblFaturamentoMes = new Label("Kz 0,00");
        lblEmAberto = new Label("Kz 0,00");
        lblTotalFaturas = new Label("0");

        pane.getChildren().addAll(
            createKPICard("Faturamento Hoje", lblFaturamentoDia, Feather.DOLLAR_SIGN, Styles.SUCCESS),
            createKPICard("Faturamento Mês", lblFaturamentoMes, Feather.TRENDING_UP, Styles.ACCENT),
            createKPICard("Em Aberto", lblEmAberto, Feather.CLOCK, Styles.WARNING),
            createKPICard("Total Faturas", lblTotalFaturas, Feather.FILE_TEXT, Styles.TEXT_MUTED)
        );

        return pane;
    }

    private Card createKPICard(String titulo, Label valorLabel, Feather icono, String colorStyle) {
        Card card = new Card();
        card.getStyleClass().addAll(Styles.ELEVATED_1);
        card.setMinWidth(180);
        card.setMaxWidth(220);

        VBox content = new VBox(8);
        content.setPadding(new Insets(15));

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        FontIcon icon = new FontIcon(icono);
        icon.setIconSize(24);
        icon.getStyleClass().add(colorStyle);

        Label lblTitulo = new Label(titulo);
        lblTitulo.getStyleClass().add(Styles.TEXT_MUTED);
        lblTitulo.setWrapText(true);

        header.getChildren().addAll(icon, lblTitulo);

        valorLabel.getStyleClass().addAll(Styles.TITLE_3, colorStyle);
        valorLabel.setFont(Font.font("System", FontWeight.BOLD, 20));

        content.getChildren().addAll(header, valorLabel);
        card.setBody(content);

        return card;
    }

    private VBox createFiltersAndTableSection() {
        // Filtros
        HBox filters = new HBox(15);
        filters.setAlignment(Pos.CENTER_LEFT);
        filters.setPadding(new Insets(0, 0, 10, 0));

        txtSearch = new CustomTextField();
        txtSearch.setPromptText("Pesquisar por número, cliente...");
        txtSearch.setLeft(IconUtils.icon(Feather.SEARCH, IconUtils.SIZE_SMALL));
        txtSearch.setPrefWidth(250);
        txtSearch.textProperty().addListener((obs, ov, nv) -> applyFilters());

        cbCliente = new ComboBox<>();
        cbCliente.setPromptText("Cliente");
        cbCliente.setPrefWidth(180);
        cbCliente.getItems().addAll(clienteService.findAll());
        cbCliente.setOnAction(e -> applyFilters());

        cbStatus = new ComboBox<>();
        cbStatus.setPromptText("Status");
        cbStatus.setPrefWidth(130);
        cbStatus.getItems().addAll(StatusFatura.values());
        cbStatus.setOnAction(e -> applyFilters());

        dpInicio = new DatePicker();
        dpInicio.setPromptText("Início");
        dpInicio.setOnAction(e -> applyFilters());

        dpFim = new DatePicker();
        dpFim.setPromptText("Fim");
        dpFim.setOnAction(e -> applyFilters());

        Button btnLimpar = new Button("Limpar", IconUtils.icon(Feather.X, IconUtils.SIZE_SMALL));
        btnLimpar.getStyleClass().add(Styles.BUTTON_OUTLINED);
        btnLimpar.setOnAction(e -> limparFiltros());

        filters.getChildren().addAll(
            txtSearch, cbCliente, cbStatus, dpInicio, dpFim, btnLimpar
        );

        // Toolbar de ações
        HBox toolbar = new HBox(10);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(5, 0, 10, 0));

        Button btnNova = new Button("Nova", IconUtils.icon(Feather.PLUS, IconUtils.SIZE_SMALL));
        btnNova.getStyleClass().addAll(Styles.SUCCESS);
        btnNova.setOnAction(e -> criarNovaFatura());

        Button btnEmitir = new Button("Emitir", IconUtils.icon(Feather.CHECK, IconUtils.SIZE_SMALL));
        btnEmitir.setOnAction(e -> emitirFatura());

        Button btnCancelar = new Button("Cancelar", IconUtils.icon(Feather.X, IconUtils.SIZE_SMALL));
        btnCancelar.getStyleClass().add(Styles.DANGER);
        btnCancelar.setOnAction(e -> cancelarFatura());

        Button btnRecibo = new Button("Recibo", IconUtils.icon(Feather.FILE_TEXT, IconUtils.SIZE_SMALL));
        btnRecibo.setOnAction(e -> registrarRecibo());

        Button btnImprimir = new Button("Imprimir", IconUtils.icon(Feather.PRINTER, IconUtils.SIZE_SMALL));
        btnImprimir.setOnAction(e -> imprimirFatura());

        Button btnEmail = new Button("Email", IconUtils.icon(Feather.MAIL, IconUtils.SIZE_SMALL));
        btnEmail.setOnAction(e -> enviarEmail());

        Button btnNotaCredito = new Button("Nota Crédito", IconUtils.icon(Feather.FILE_MINUS, IconUtils.SIZE_SMALL));
        btnNotaCredito.setOnAction(e -> emitirNotaCredito());

        Button btnDuplicado = new Button("Duplicado", IconUtils.icon(Feather.COPY, IconUtils.SIZE_SMALL));
        btnDuplicado.setOnAction(e -> gerarDuplicado());

        Button btnSegundaVia = new Button("2ª Via", IconUtils.icon(Feather.FILE_PLUS, IconUtils.SIZE_SMALL));
        btnSegundaVia.setOnAction(e -> gerarSegundaVia());

        Button btnSubmeterAGT = new Button("Submeter AGT", IconUtils.icon(Feather.UPLOAD, IconUtils.SIZE_SMALL));
        btnSubmeterAGT.getStyleClass().addAll(Styles.ACCENT);
        btnSubmeterAGT.setOnAction(e -> submeterAGT());

        // Permissões
        boolean canEmitir = sessionManager.hasAccess("FATURACAO", "Emitir");
        boolean canAnular = sessionManager.hasAccess("FATURACAO", "Anular");
        boolean canRecibo = sessionManager.hasAccess("RECIBOS", "Emitir");
        boolean canImprimir = sessionManager.hasAccess("FATURACAO", "Ver");
        boolean canAGT = sessionManager.hasAccess("AGT", "Submeter");

        btnNova.setDisable(!canEmitir);
        btnEmitir.setDisable(!canEmitir);
        btnCancelar.setDisable(!canAnular);
        btnRecibo.setDisable(!canRecibo);
        btnImprimir.setDisable(!canImprimir);
        btnEmail.setDisable(!canImprimir);
        btnNotaCredito.setDisable(!canAnular);
        btnDuplicado.setDisable(!canImprimir);
        btnSegundaVia.setDisable(!canImprimir);
        btnSubmeterAGT.setDisable(!canAGT);

        toolbar.getChildren().addAll(
            btnNova, new Separator(Orientation.VERTICAL),
            btnEmitir, btnCancelar, btnRecibo, btnNotaCredito,
            new Separator(Orientation.VERTICAL),
            btnImprimir, btnEmail, btnDuplicado, btnSegundaVia,
            new Separator(Orientation.VERTICAL),
            btnSubmeterAGT
        );

        // Tabela
        table = createTable();

        VBox section = new VBox(5, filters, toolbar, table);
        VBox.setVgrow(table, Priority.ALWAYS);
        return section;
    }

    private AdvancedTableView<Fatura> createTable() {
        AdvancedTableView<Fatura> t = new AdvancedTableView<>();
        t.setData(masterData);
        TableUtils.standardize(t);
        t.setPrefHeight(350);

        TableColumn<Fatura, Long> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colId.setMinWidth(60);
        colId.setMaxWidth(80);

        TableColumn<Fatura, String> colNumero = new TableColumn<>("Número");
        colNumero.setCellValueFactory(new PropertyValueFactory<>("numero"));
        colNumero.setMinWidth(100);

        TableColumn<Fatura, LocalDate> colEmissao = new TableColumn<>("Emissão");
        colEmissao.setCellValueFactory(new PropertyValueFactory<>("dataEmissao"));
        colEmissao.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.format(dateFormatter));
            }
        });
        colEmissao.setMinWidth(100);

        TableColumn<Fatura, String> colCliente = new TableColumn<>("Cliente");
        colCliente.setCellValueFactory(data -> {
            if (data.getValue().getCliente() != null) {
                return new javafx.beans.property.SimpleStringProperty(data.getValue().getCliente().getNome());
            } else {
                return new javafx.beans.property.SimpleStringProperty("Consumidor Final");
            }
        });
        colCliente.setMinWidth(200);

        TableColumn<Fatura, String> colNif = new TableColumn<>("NIF");
        colNif.setCellValueFactory(data -> {
            if (data.getValue().getCliente() != null && data.getValue().getCliente().getNif() != null) {
                return new javafx.beans.property.SimpleStringProperty(data.getValue().getCliente().getNif());
            } else {
                return new javafx.beans.property.SimpleStringProperty("999999999");
            }
        });
        colNif.setMinWidth(100);

        TableColumn<Fatura, BigDecimal> colTotal = new TableColumn<>("Total");
        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        colTotal.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("Kz %,.2f", item));
                    setAlignment(Pos.CENTER_RIGHT);
                }
            }
        });
        colTotal.setMinWidth(120);

        TableColumn<Fatura, String> colOperador = new TableColumn<>("Operador");
        colOperador.setCellValueFactory(data -> {
            if (data.getValue().getUsuario() != null) {
                String nome = data.getValue().getUsuario().getNome();
                if (nome == null || nome.isBlank()) {
                    nome = data.getValue().getUsuario().getUsername();
                }
                return new javafx.beans.property.SimpleStringProperty(nome);
            } else {
                return new javafx.beans.property.SimpleStringProperty("-");
            }
        });
        colOperador.setMinWidth(120);

        TableColumn<Fatura, StatusFatura> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(StatusFatura item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item.name());
                    switch (item) {
                        case RASCUNHO -> setTextFill(Color.GRAY);
                        case EMITIDA -> setTextFill(Color.BLUE);
                        case PAGA -> setTextFill(Color.GREEN);
                        case CANCELADA -> setTextFill(Color.RED);
                    }
                    setStyle("-fx-font-weight: bold;");
                }
            }
        });
        colStatus.setMinWidth(100);

        TableColumn<Fatura, EstadoDocumento> colEstadoDoc = new TableColumn<>("Estado Doc.");
        colEstadoDoc.setCellValueFactory(new PropertyValueFactory<>("estadoDocumento"));
        colEstadoDoc.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(EstadoDocumento item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item.getDescricao());
                    if (item == EstadoDocumento.ORIGINAL) {
                        setTextFill(Color.GREEN);
                    } else {
                        setTextFill(Color.ORANGE);
                    }
                    setStyle("-fx-font-weight: bold;");
                }
            }
        });
        colEstadoDoc.setMinWidth(120);

        // Initialize colDocStatus
        colDocStatus = new TableColumn<>("Doc.");
        colDocStatus.setCellValueFactory(new PropertyValueFactory<>("documentStatus"));
        colDocStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(ao.allon.kubata.faturacao.domain.enums.DocumentStatus item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setTooltip(null);
                } else {
                    setText(item.getCodigo());
                    setTooltip(new Tooltip(item.getDescricao()));
                    switch (item) {
                        case NORMAL -> setTextFill(Color.GREEN);
                        case AUTOFATURACAO -> setTextFill(Color.BLUE);
                        case ANULADO -> setTextFill(Color.RED);
                        case CORRECAO_REJEITADO -> setTextFill(Color.ORANGE);
                    }
                    setStyle("-fx-font-weight: bold;");
                }
            }
        });
        colDocStatus.setMinWidth(50);

        // Initialize colAgtStatus
        colAgtStatus = new TableColumn<>("AGT");
        colAgtStatus.setCellValueFactory(data -> {
            String status = data.getValue().getAgtSubmissionStatus();
            return javafx.beans.binding.Bindings.createStringBinding(() -> {
                if (status == null) return "-";
                return switch (status) {
                    case "PENDING" -> "⏳";
                    case "SUCCESS" -> "✓";
                    case "FAILED" -> "✗";
                    default -> "-";
                };
            });
        });
        colAgtStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setTooltip(null);
                } else {
                    setText(item);
                    Fatura fatura = getTableRow() != null ? getTableRow().getItem() : null;
                    if (fatura != null) {
                        String status = fatura.getAgtSubmissionStatus();
                        String tooltipText = switch (status != null ? status : "") {
                            case "PENDING" -> "Pendente";
                            case "SUCCESS" -> "Validado: " + (fatura.getAgtValidationCode() != null ? fatura.getAgtValidationCode() : "");
                            case "FAILED" -> "Falhou";
                            default -> "Não submetido";
                        };
                        setTooltip(new Tooltip(tooltipText));
                        
                        switch (status != null ? status : "") {
                            case "PENDING" -> setTextFill(Color.ORANGE);
                            case "SUCCESS" -> setTextFill(Color.GREEN);
                            case "FAILED" -> setTextFill(Color.RED);
                            default -> setTextFill(Color.BLACK);
                        }
                    }
                }
            }
        });
        colAgtStatus.setMinWidth(50);

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

        t.getColumns().addAll(colId, colNumero, colEmissao, colCliente, colNif, colTotal, colOperador, colStatus, colEstadoDoc, colDocStatus, colAgtStatus, colAcoes);

        t.setRowFactory(tv -> {
            TableRow<Fatura> row = new TableRow<>();

            row.setOnMouseClicked(event -> {
                if (row.isEmpty()) return;
                if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                    // TODO: Abrir detalhes
                }
                if (event.getButton() == MouseButton.SECONDARY) {
                    ContextMenu cm = buildRowMenu(row.getItem());
                    cm.show(row, event.getScreenX(), event.getScreenY());
                }
            });

            return row;
        });

        return t;
    }

    private ContextMenu buildRowMenu(Fatura f) {
        MenuItem miEmitir = new MenuItem("Emitir", IconUtils.icon(Feather.CHECK, IconUtils.SIZE_SMALL));
        miEmitir.setOnAction(e -> emitirFatura());

        MenuItem miCancelar = new MenuItem("Cancelar", IconUtils.icon(Feather.X, IconUtils.SIZE_SMALL));
        miCancelar.setOnAction(e -> cancelarFatura());

        MenuItem miRecibo = new MenuItem("Registrar Recibo", IconUtils.icon(Feather.FILE_TEXT, IconUtils.SIZE_SMALL));
        miRecibo.setOnAction(e -> registrarRecibo());

        MenuItem miImprimir = new MenuItem("Imprimir", IconUtils.icon(Feather.PRINTER, IconUtils.SIZE_SMALL));
        miImprimir.setOnAction(e -> imprimirFatura());

        MenuItem miEmail = new MenuItem("Enviar Email", IconUtils.icon(Feather.MAIL, IconUtils.SIZE_SMALL));
        miEmail.setOnAction(e -> enviarEmail());

        MenuItem miNotaCredito = new MenuItem("Nota de Crédito", IconUtils.icon(Feather.FILE_MINUS, IconUtils.SIZE_SMALL));
        miNotaCredito.setOnAction(e -> emitirNotaCredito());

        ContextMenu cm = new ContextMenu(miEmitir, miCancelar, new SeparatorMenuItem(), miRecibo, miNotaCredito, new SeparatorMenuItem(), miImprimir, miEmail);
        return cm;
    }

    private TitledPane createChartSection() {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Valor (Kz)");

        chartFaturamento = new BarChart<>(xAxis, yAxis);
        chartFaturamento.setTitle("Faturamento dos Últimos 6 Meses");
        chartFaturamento.setLegendVisible(false);
        chartFaturamento.setPrefHeight(250);

        TitledPane pane = new TitledPane("Análise de Faturamento", chartFaturamento);
        pane.setExpanded(false);

        return pane;
    }

    private void loadData() {
        masterData.setAll(faturaService.findAll());
        table.setData(masterData);
        updateEmptyState();
    }

    private void applyFilters() {
        String searchText = txtSearch.getText() != null ? txtSearch.getText().toLowerCase() : "";
        Cliente cliente = cbCliente.getValue();
        StatusFatura status = cbStatus.getValue();
        LocalDate inicio = dpInicio.getValue();
        LocalDate fim = dpFim.getValue();

        table.setFilter(fatura -> {
            if (!searchText.isEmpty()) {
                String numero = fatura.getNumero() != null ? fatura.getNumero().toLowerCase() : "";
                String clienteNome = fatura.getCliente() != null ? fatura.getCliente().getNome().toLowerCase() : "consumidor final";
                String nif = fatura.getCliente() != null && fatura.getCliente().getNif() != null ? fatura.getCliente().getNif() : "";

                if (!numero.contains(searchText) &&
                    !clienteNome.contains(searchText) &&
                    !nif.contains(searchText) &&
                    !String.valueOf(fatura.getId()).contains(searchText)) {
                    return false;
                }
            }

            if (cliente != null && (fatura.getCliente() == null || !fatura.getCliente().getId().equals(cliente.getId()))) {
                return false;
            }

            if (status != null && fatura.getStatus() != status) {
                return false;
            }

            if (inicio != null && (fatura.getDataEmissao() == null || fatura.getDataEmissao().isBefore(inicio))) {
                return false;
            }
            if (fim != null && (fatura.getDataEmissao() == null || fatura.getDataEmissao().isAfter(fim))) {
                return false;
            }

            return true;
        });

        updateEmptyState();
    }

    private void recarregarAsync() {
        showLoading(true);
        Thread t = new Thread(() -> {
            try {
                java.util.List<Fatura> all = faturaService.findAll();
                Platform.runLater(() -> {
                    masterData.setAll(all);
                    table.setData(masterData);
                    applyFilters();
                    refreshKPIs();
                });
            } catch (Exception ex) {
                Platform.runLater(() -> AlertUtils.showExceptionAlert("Erro", "Não foi possível atualizar dados.", ex));
            } finally {
                Platform.runLater(() -> showLoading(false));
            }
        }, "faturas-reload");
        t.setDaemon(true);
        t.start();
    }

    private void showLoading(boolean show) {
        if (loadingOverlay == null) return;
        loadingOverlay.setVisible(show);
        loadingOverlay.setManaged(show);
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
        Label sub = new Label("Nenhuma fatura encontrada com os filtros atuais.");
        sub.getStyleClass().addAll(Styles.TEXT_MUTED);
        Button btn = new Button("Criar Fatura", IconUtils.icon(Feather.PLUS, IconUtils.SIZE_SMALL));
        btn.getStyleClass().addAll(Styles.SUCCESS);
        btn.setOnAction(e -> criarNovaFatura());

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
        boolean show = table.getItems().isEmpty();
        emptyState.setVisible(show);
        emptyState.setManaged(show);
    }

    private void limparFiltros() {
        txtSearch.clear();
        cbCliente.setValue(null);
        cbStatus.setValue(null);
        dpInicio.setValue(null);
        dpFim.setValue(null);
        applyFilters();
    }

    private void refreshKPIs() {
        LocalDate hoje = LocalDate.now();
        LocalDate inicioMes = hoje.withDayOfMonth(1);

        BigDecimal faturamentoDia = masterData.stream()
            .filter(f -> f.getDataEmissao() != null && f.getDataEmissao().equals(hoje))
            .filter(f -> f.getStatus() == StatusFatura.EMITIDA || f.getStatus() == StatusFatura.PAGA)
            .map(Fatura::getTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal faturamentoMes = masterData.stream()
            .filter(f -> f.getDataEmissao() != null && !f.getDataEmissao().isBefore(inicioMes))
            .filter(f -> f.getStatus() == StatusFatura.EMITIDA || f.getStatus() == StatusFatura.PAGA)
            .map(Fatura::getTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal emAberto = masterData.stream()
            .filter(f -> f.getStatus() == StatusFatura.EMITIDA)
            .map(Fatura::getTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalFaturas = masterData.size();

        lblFaturamentoDia.setText(String.format("Kz %,.2f", faturamentoDia));
        lblFaturamentoMes.setText(String.format("Kz %,.2f", faturamentoMes));
        lblEmAberto.setText(String.format("Kz %,.2f", emAberto));
        lblTotalFaturas.setText(String.valueOf(totalFaturas));

        atualizarGrafico();
    }

    private void atualizarGrafico() {
        XYChart.Series<String, Number> series = new XYChart.Series<>();

        LocalDate hoje = LocalDate.now();
        for (int i = 5; i >= 0; i--) {
            LocalDate inicioMes = hoje.minusMonths(i).withDayOfMonth(1);
            LocalDate fimMes = inicioMes.withDayOfMonth(inicioMes.lengthOfMonth());
            String mesAno = inicioMes.format(monthFormatter);

            BigDecimal total = masterData.stream()
                .filter(f -> f.getDataEmissao() != null &&
                    !f.getDataEmissao().isBefore(inicioMes) &&
                    !f.getDataEmissao().isAfter(fimMes))
                .filter(f -> f.getStatus() == StatusFatura.EMITIDA || f.getStatus() == StatusFatura.PAGA)
                .map(Fatura::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            series.getData().add(new XYChart.Data<>(mesAno, total.doubleValue()));
        }

        chartFaturamento.getData().clear();
        chartFaturamento.getData().add(series);
    }

    // === AÇÕES SIMPLIFICADAS ===

    private void criarNovaFatura() {
        AlertUtils.showInfoAlert("Nova Fatura", "Navegar para tela de criação de fatura.");
    }

    private Fatura getSelectedFatura() {
        return table.getSelectionModel().getSelectedItem();
    }

    private void emitirFatura() {
        Fatura f = getSelectedFatura();
        if (f == null) {
            AlertUtils.showWarningAlert("Aviso", "Selecione uma fatura.");
            return;
        }
        
        String loadingId = loadingService.showLoading("Emitindo fatura...", LoadingScreen.LoadingStyle.SPINNER);
        
        new Thread(() -> {
            try {
                faturaService.emitirFatura(f.getId());
                Platform.runLater(() -> {
                    loadData();
                    refreshKPIs();
                });
            } catch (Exception ex) {
                Platform.runLater(() -> AlertUtils.showExceptionAlert("Erro", "Não foi possível emitir a fatura.", ex));
            } finally {
                Platform.runLater(() -> loadingService.hideLoading(loadingId));
            }
        }).start();
    }

    private void cancelarFatura() {
        Fatura f = getSelectedFatura();
        if (f == null) {
            AlertUtils.showWarningAlert("Aviso", "Selecione uma fatura.");
            return;
        }

        TextField txtMotivo = new TextField();
        txtMotivo.setPromptText("Motivo do cancelamento");

        modalService.create()
            .title("Cancelar Fatura")
            .content(new VBox(10, new Label("Informe o motivo:"), txtMotivo))
            .autoSize()
            .withConfirmButton("Confirmar", () -> {
                String motivo = txtMotivo.getText();
                if (motivo == null || motivo.trim().isEmpty()) {
                    AlertUtils.showWarningAlert("Motivo Obrigatório", "Informe o motivo.");
                    return false;
                }
                
                String loadingId = loadingService.showLoading("Cancelando fatura...", LoadingScreen.LoadingStyle.SPINNER);
                
                new Thread(() -> {
                    try {
                        faturaService.cancelarFatura(f.getId(), motivo);
                        Platform.runLater(() -> {
                            loadData();
                            refreshKPIs();
                        });
                    } catch (Exception ex) {
                        Platform.runLater(() -> AlertUtils.showExceptionAlert("Erro", "Não foi possível cancelar.", ex));
                    } finally {
                        Platform.runLater(() -> loadingService.hideLoading(loadingId));
                    }
                }).start();
                return true;
            })
            .withCancelButton("Cancelar")
            .buildAndShow();
    }

    private void registrarRecibo() {
        Fatura f = getSelectedFatura();
        if (f == null) {
            AlertUtils.showWarningAlert("Aviso", "Selecione uma fatura.");
            return;
        }

        TextField valorField = new TextField(String.valueOf(f.getTotal()));
        VBox content = new VBox(8, new Label("Valor recebido"), valorField);
        content.setPadding(new Insets(10));

        modalService.create()
            .title("Registrar Recebimento")
            .content(content)
            .autoSize()
            .withConfirmButton("Confirmar", () -> {
                String loadingId = loadingService.showLoading("Registrando recebimento...", LoadingScreen.LoadingStyle.SPINNER);
                
                new Thread(() -> {
                    try {
                        BigDecimal valor = new BigDecimal(valorField.getText());
                        faturaService.registrarRecebimento(f.getId(), valor, "Recebimento");
                        Platform.runLater(() -> {
                            loadData();
                            refreshKPIs();
                        });
                    } catch (Exception ex) {
                        Platform.runLater(() -> AlertUtils.showExceptionAlert("Erro", "Não foi possível registrar.", ex));
                    } finally {
                        Platform.runLater(() -> loadingService.hideLoading(loadingId));
                    }
                }).start();
                return true;
            })
            .withCancelButton("Cancelar")
            .buildAndShow();
    }

    private void imprimirFatura() {
        Fatura f = getSelectedFatura();
        if (f == null) {
            AlertUtils.showWarningAlert("Aviso", "Selecione uma fatura.");
            return;
        }
        if (f.getStatus() != StatusFatura.EMITIDA) {
            AlertUtils.showWarningAlert("Aviso", "Apenas faturas emitidas podem ser impressas.");
            return;
        }
        handleImprimir();
    }

    private void enviarEmail() {
        Fatura f = getSelectedFatura();
        if (f == null) {
            AlertUtils.showWarningAlert("Aviso", "Selecione uma fatura.");
            return;
        }
        handleEmail();
    }

    private void emitirNotaCredito() {
        Fatura f = getSelectedFatura();
        if (f == null) {
            AlertUtils.showWarningAlert("Aviso", "Selecione uma fatura.");
            return;
        }

        TextField motivoField = new TextField();
        motivoField.setPromptText("Motivo da nota de crédito");
        VBox content = new VBox(8, new Label("Motivo"), motivoField);
        content.setPadding(new Insets(10));

        modalService.create()
            .title("Nota de Crédito")
            .content(content)
            .autoSize()
            .withConfirmButton("Confirmar", () -> {
                String loadingId = loadingService.showLoading("Emitindo nota de crédito...", LoadingScreen.LoadingStyle.SPINNER);
                
                new Thread(() -> {
                    try {
                        faturaService.emitirNotaCredito(f.getId(), motivoField.getText());
                        Platform.runLater(() -> {
                            loadData();
                            refreshKPIs();
                        });
                    } catch (Exception ex) {
                        Platform.runLater(() -> AlertUtils.showExceptionAlert("Erro", "Não foi possível emitir.", ex));
                    } finally {
                        Platform.runLater(() -> loadingService.hideLoading(loadingId));
                    }
                }).start();
                return true;
            })
            .withCancelButton("Cancelar")
            .buildAndShow();
    }

    private void gerarDuplicado() {
        Fatura f = getSelectedFatura();
        if (f == null) {
            AlertUtils.showWarningAlert("Aviso", "Selecione uma fatura.");
            return;
        }

        TextField motivoField = new TextField("Duplicado para arquivo");
        VBox content = new VBox(8, new Label("Motivo"), motivoField);
        content.setPadding(new Insets(10));

        modalService.create()
            .title("Gerar Duplicado")
            .content(content)
            .autoSize()
            .withConfirmButton("Gerar", () -> {
                try {
                    documentoEstadoService.gerarDuplicado(f, 1L, motivoField.getText());
                    loadData();
                    return true;
                } catch (Exception ex) {
                    AlertUtils.showExceptionAlert("Erro", "Não foi possível gerar.", ex);
                    return false;
                }
            })
            .withCancelButton("Cancelar")
            .buildAndShow();
    }

    private void gerarSegundaVia() {
        Fatura f = getSelectedFatura();
        if (f == null) {
            AlertUtils.showWarningAlert("Aviso", "Selecione uma fatura.");
            return;
        }

        TextField motivoField = new TextField("Solicitada pelo cliente");
        VBox content = new VBox(8, new Label("Motivo"), motivoField);
        content.setPadding(new Insets(10));

        modalService.create()
            .title("Gerar Segunda Via")
            .content(content)
            .autoSize()
            .withConfirmButton("Gerar", () -> {
                try {
                    documentoEstadoService.gerarSegundaVia(f, 1L, motivoField.getText());
                    loadData();
                    return true;
                } catch (Exception ex) {
                    AlertUtils.showExceptionAlert("Erro", "Não foi possível gerar.", ex);
                    return false;
                }
            })
            .withCancelButton("Cancelar")
            .buildAndShow();
    }

    private void exportarCSV() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exportar Faturas");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
        fileChooser.setInitialFileName("faturas_" + LocalDate.now() + ".csv");

        File file = fileChooser.showSaveDialog(getScene().getWindow());
        if (file != null) {
            String loadingId = loadingService.showLoading("Exportando faturas...", LoadingScreen.LoadingStyle.SPINNER);
            
            new Thread(() -> {
                try (FileWriter writer = new FileWriter(file)) {
                    writer.write("ID;Número;Data;Cliente;NIF;Total;Status\n");
                    for (Fatura f : table.getItems()) {
                        writer.write(String.format("%d;%s;%s;%s;%s;%.2f;%s\n",
                            f.getId(),
                            f.getNumero(),
                            f.getDataEmissao() != null ? f.getDataEmissao().format(dateFormatter) : "",
                            f.getCliente() != null ? f.getCliente().getNome() : "Consumidor Final",
                            f.getCliente() != null && f.getCliente().getNif() != null ? f.getCliente().getNif() : "999999999",
                            f.getTotal() != null ? f.getTotal() : BigDecimal.ZERO,
                            f.getStatus()
                        ));
                    }
                    Platform.runLater(() -> AlertUtils.showInfoAlert("Sucesso", "Exportado com sucesso!"));
                } catch (IOException e) {
                    Platform.runLater(() -> AlertUtils.showExceptionAlert("Erro", "Não foi possível exportar.", e));
                } finally {
                    Platform.runLater(() -> loadingService.hideLoading(loadingId));
                }
            }).start();
        }
    }

    // === MÉTODOS ORIGINAIS ===

    private ToolBar buildToolbar() {
        ToolBar bar = new ToolBar();

        Button emitir = new Button("Emitir", IconUtils.icon(Feather.CHECK, IconUtils.SIZE_SMALL));
        emitir.setOnAction(e -> emitirFatura());

        Button cancelar = new Button("Cancelar", IconUtils.icon(Feather.X, IconUtils.SIZE_SMALL));
        cancelar.setOnAction(e -> cancelarFatura());

        Button recibo = new Button("Recibo", IconUtils.icon(Feather.FILE_TEXT, IconUtils.SIZE_SMALL));
        recibo.setOnAction(e -> registrarRecibo());

        Button notaCredito = new Button("Nota Crédito", IconUtils.icon(Feather.FILE_TEXT, IconUtils.SIZE_SMALL));
        notaCredito.setOnAction(e -> emitirNotaCredito());

        Button imprimir = new Button("Imprimir", IconUtils.icon(Feather.PRINTER, IconUtils.SIZE_SMALL));
        imprimir.setOnAction(e -> imprimirFatura());

        Button email = new Button("Email", IconUtils.icon(Feather.MAIL, IconUtils.SIZE_SMALL));
        email.setOnAction(e -> enviarEmail());

        Button duplicado = new Button("Duplicado", IconUtils.icon(Feather.COPY, IconUtils.SIZE_SMALL));
        duplicado.setOnAction(e -> handleGerarDuplicado());

        Button segundaVia = new Button("2ª Via", IconUtils.icon(Feather.FILE_PLUS, IconUtils.SIZE_SMALL));
        segundaVia.setOnAction(e -> handleGerarSegundaVia());

        Button exportar = new Button("Exportar", IconUtils.icon(Feather.DOWNLOAD, IconUtils.SIZE_SMALL));
        exportar.setOnAction(e -> exportarCSV());

        boolean canEmitir = sessionManager.hasAccess("FATURACAO", "Emitir");
        boolean canAnular = sessionManager.hasAccess("FATURACAO", "Anular");
        boolean canRecibo = sessionManager.hasAccess("RECIBOS", "Emitir");
        boolean canImprimir = sessionManager.hasAccess("FATURACAO", "Ver");
        boolean canEmail = sessionManager.hasAccess("FATURACAO", "Ver");

        emitir.setDisable(!canEmitir);
        cancelar.setDisable(!canAnular);
        recibo.setDisable(!canRecibo);
        notaCredito.setDisable(!canAnular);
        imprimir.setDisable(!canImprimir);
        email.setDisable(!canEmail);
        duplicado.setDisable(!canImprimir);
        segundaVia.setDisable(!canImprimir);

        bar.getItems().addAll(
            emitir,
            cancelar,
            recibo,
            notaCredito,
            new Separator(Orientation.VERTICAL),
            imprimir,
            email,
            duplicado,
            segundaVia,
            new Separator(Orientation.VERTICAL),
            exportar
        );

        return bar;
    }

    private AdvancedTableView<Fatura> buildTable() {
        table = new AdvancedTableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        TableColumn<Fatura, Long> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("id"));
        TableColumn<Fatura, String> colNumero = new TableColumn<>("Número");
        colNumero.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("numero"));
        TableColumn<Fatura, java.time.LocalDate> colEmissao = new TableColumn<>("Emissão");
        colEmissao.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("dataEmissao"));
        TableColumn<Fatura, java.math.BigDecimal> colTotal = new TableColumn<>("Total");
        colTotal.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("total"));
        TableColumn<Fatura, ao.allon.kubata.faturacao.domain.enums.StatusFatura> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("status"));
        TableColumn<Fatura, ao.allon.kubata.faturacao.domain.enums.EstadoDocumento> colEstadoDoc = new TableColumn<>("Estado");
        colEstadoDoc.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("estadoDocumento"));
        colEstadoDoc.setCellFactory(col -> new javafx.scene.control.TableCell<>() {
            @Override
            protected void updateItem(ao.allon.kubata.faturacao.domain.enums.EstadoDocumento item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item.getDescricao());
                    if (item == ao.allon.kubata.faturacao.domain.enums.EstadoDocumento.ORIGINAL) {
                        setStyle("-fx-text-fill: green;");
                    } else {
                        setStyle("-fx-text-fill: orange; -fx-font-weight: bold;");
                    }
                }
            }
        });
        // Coluna Document Status (N, S, A, C) - Decreto 683/25
        colDocStatus = new TableColumn<>("Doc.");
        colDocStatus.setCellValueFactory(new PropertyValueFactory<>("documentStatus"));
        colDocStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(ao.allon.kubata.faturacao.domain.enums.DocumentStatus item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    setTooltip(null);
                } else {
                    setText(item.getCodigo());
                    setTooltip(new Tooltip(item.getDescricao()));
                    switch (item) {
                        case NORMAL -> setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                        case AUTOFATURACAO -> setStyle("-fx-text-fill: blue; -fx-font-weight: bold;");
                        case ANULADO -> setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                        case CORRECAO_REJEITADO -> setStyle("-fx-text-fill: orange; -fx-font-weight: bold;");
                    }
                }
            }
        });
        colDocStatus.setPrefWidth(50);

        // Coluna AGT Submission Status
        colAgtStatus = new TableColumn<>("AGT");
        colAgtStatus.setCellValueFactory(data -> {
            String status = data.getValue().getAgtSubmissionStatus();
            return javafx.beans.binding.Bindings.createStringBinding(() -> {
                if (status == null) return "-";
                return switch (status) {
                    case "PENDING" -> "⏳";
                    case "SUCCESS" -> "✓";
                    case "FAILED" -> "✗";
                    default -> "-";
                };
            });
        });
        colAgtStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    setTooltip(null);
                } else {
                    setText(item);
                    Fatura fatura = getTableView().getItems().get(getIndex());
                    if (fatura != null) {
                        String status = fatura.getAgtSubmissionStatus();
                        String tooltipText = switch (status) {
                            case "PENDING" -> "Pendente";
                            case "SUCCESS" -> "Validado: " + (fatura.getAgtValidationCode() != null ? fatura.getAgtValidationCode() : "");
                            case "FAILED" -> "Falhou";
                            default -> "Não submetido";
                        };
                        setTooltip(new Tooltip(tooltipText));
                        setStyle(switch (status) {
                            case "PENDING" -> "-fx-text-fill: orange;";
                            case "SUCCESS" -> "-fx-text-fill: green;";
                            case "FAILED" -> "-fx-text-fill: red;";
                            default -> "";
                        });
                    }
                }
            }
        });
        colAgtStatus.setPrefWidth(50);

        table.getColumns().addAll(colId, colNumero, colEmissao, colTotal, colStatus, colEstadoDoc, colDocStatus, colAgtStatus);
        refresh();
        VBox.setVgrow(table, Priority.ALWAYS);
        return table;
    }

    public void refresh() {
        table.getItems().setAll(faturaService.findAll());
    }

    public void applyFilter(String q) {
        // Método não utilizado na nova implementação
    }

    private void handleImprimir() {
        Fatura faturaSelecionada = getSelectedFatura();
        if (faturaSelecionada == null) {
            AlertUtils.showWarningAlert("Aviso", "Selecione uma fatura.");
            return;
        }
        if (faturaSelecionada.getStatus() != StatusFatura.EMITIDA) {
            AlertUtils.showWarningAlert("Aviso", "Apenas faturas emitidas podem ser impressas.");
            return;
        }

        try {
            Fatura faturaCompleta = faturaService.findFaturaParaImpressao(faturaSelecionada.getId())
                    .orElseThrow(() -> new IllegalStateException("Fatura não encontrada para impressão. ID: " + faturaSelecionada.getId()));

            ToggleGroup group = new ToggleGroup();
            RadioButton rbA4 = new RadioButton("Fatura A4 (PDF)");
            rbA4.setToggleGroup(group);
            rbA4.setSelected(true);
            RadioButton rbTermica = new RadioButton("Talão Térmico 80mm");
            rbTermica.setToggleGroup(group);

            VBox content = new VBox(10,
                    new Label("Selecione o formato de impressão:"),
                    rbA4,
                    rbTermica
            );
            content.setPadding(new Insets(10));

            modalService.create()
                    .title("Imprimir Fatura")
                    .content(content)
                    .autoSize()
                    .withConfirmButton("Imprimir", () -> {
                        try {
                            if (rbA4.isSelected()) {
                                JasperPrint jasperPrint = jasperReportService.prepararFatura(faturaCompleta);
                                jasperReportService.showReport(jasperPrint);
                            } else {
                                var data = new InvoiceReportData(
                                        faturaCompleta.getNumero(),
                                        faturaCompleta.getDataEmissao() != null ? faturaCompleta.getDataEmissao().atStartOfDay() : LocalDateTime.now(),
                                        faturaCompleta.getSystemEntryDate() != null ? faturaCompleta.getSystemEntryDate() : LocalDateTime.now(),
                                        (faturaCompleta.getUsuario() != null
                                                ? ((faturaCompleta.getUsuario().getNome() != null && !faturaCompleta.getUsuario().getNome().isBlank())
                                                    ? faturaCompleta.getUsuario().getNome()
                                                    : (faturaCompleta.getUsuario().getUsername() != null ? faturaCompleta.getUsuario().getUsername() : ""))
                                                : ""),
                                        faturaCompleta.getCliente() != null ? faturaCompleta.getCliente().getNome() : "Consumidor Final",
                                        faturaCompleta.getCliente() != null && faturaCompleta.getCliente().getNif() != null ? faturaCompleta.getCliente().getNif() : "999999999",
                                        faturaCompleta.getSubtotal() != null ? faturaCompleta.getSubtotal() : BigDecimal.ZERO,
                                        faturaCompleta.getIva() != null ? faturaCompleta.getIva() : BigDecimal.ZERO,
                                        faturaCompleta.getTotal() != null ? faturaCompleta.getTotal() : BigDecimal.ZERO,
                                        BigDecimal.ZERO, // totalDiscount
                                        faturaCompleta.getTotalRetencao() != null ? faturaCompleta.getTotalRetencao() : BigDecimal.ZERO,
                                        faturaCompleta.getHash(),
                                        faturaCompleta.getSerie() != null ? faturaCompleta.getSerie().getHashAnterior() : null
                                );

                                java.util.List<InvoiceItemData> items = faturaCompleta.getItens().stream()
                                        .map(it -> {
                                            String unit = "un";
                                            if (it.getProduto() != null && it.getProduto().getUnidadeMedida() != null) {
                                                unit = switch (it.getProduto().getUnidadeMedida()) {
                                                    case KILOGRAMA -> "kg";
                                                    case LITRO -> "L";
                                                    case METRO -> "m";
                                                    case HORA, SERVICO -> "h";
                                                    case CAIXA -> "cx";
                                                    default -> "un";
                                                };
                                            }
                                            return new InvoiceItemData(
                                                it.getProduto() != null ? it.getProduto().getCodigo() : "",
                                                it.getDescricao(),
                                                it.getQuantidadeDecimal() != null
                                                        ? it.getQuantidadeDecimal()
                                                        : BigDecimal.valueOf(it.getQuantidade() != null ? it.getQuantidade() : 0),
                                                unit,
                                                it.getPrecoUnitario(),
                                                it.getPercentualIva(),
                                                it.getTotal(),
                                                it.getCodigoIsencao(),
                                                it.getMotivoIsencao()
                                            );
                                        })
                                        .toList();

                                JasperPrint jp = invoiceReportService.createInvoiceThermalPrint(data, items);
                                try {
                                    invoiceReportService.printJasper(jp, null);
                                } catch (Exception npf) {
                                    modalService.create()
                                            .title("Impressora não encontrada")
                                            .content(new javafx.scene.control.Label("Nenhuma impressora encontrada ou selecionada. Deseja abrir o Visualizador de Relatório?"))
                                            .autoSize()
                                            .withConfirmButton("Abrir Visualizador", () -> {
                                                jasperReportService.showReport(jp);
                                                return true;
                                            })
                                            .withCancelButton("Cancelar")
                                            .buildAndShow();
                                }
                            }
                            return true;
                        } catch (Exception e) {
                            AlertUtils.showExceptionAlert("Erro ao Imprimir Fatura", "Não foi possível imprimir a fatura.", e);
                            return false;
                        }
                    })
                    .withCancelButton("Cancelar")
                    .buildAndShow();

        } catch (Exception ex) {
            AlertUtils.showExceptionAlert("Erro ao Gerar Relatório", "Não foi possível gerar o relatório da fatura.", ex);
        }
    }


    private void handleEmail() {
        Fatura selecionada = getSelectedFatura();
        if (selecionada == null) {
            AlertUtils.showWarningAlert("Aviso", "Selecione uma fatura.");
            return;
        }
        
        if (selecionada.getCliente() == null || selecionada.getCliente().getEmail() == null || selecionada.getCliente().getEmail().isEmpty()) {
            AlertUtils.showErrorAlert("Erro", "O cliente desta fatura não possui e-mail cadastrado.");
            return;
        }

        // Load complete fatura data first
        Fatura faturaCompleta;
        java.io.File tempFile;
        try {
            faturaCompleta = faturaService.findFaturaParaImpressao(selecionada.getId())
                    .orElseThrow(() -> new IllegalStateException("Fatura não encontrada para envio. ID: " + selecionada.getId()));
            tempFile = java.io.File.createTempFile("fatura_" + faturaCompleta.getNumero().replace("/", "_"), ".pdf");
            jasperReportService.gerarFaturaPdf(faturaCompleta, tempFile);
        } catch (Exception e) {
            AlertUtils.showExceptionAlert("Erro", "Não foi possível gerar o PDF da fatura.", e);
            return;
        }

        // Create modern email dialog content
        VBox dialogContent = new VBox(15);
        dialogContent.setPadding(new Insets(20));
        dialogContent.setPrefWidth(550);

        // 1. Invoice Info Card
        Card infoCard = new Card();
        infoCard.getStyleClass().add(Styles.ELEVATED_1);
        
        VBox infoBox = new VBox(8);
        infoBox.setPadding(new Insets(15));
        
        Label lblFatura = new Label("Fatura: " + faturaCompleta.getNumero());
        lblFatura.getStyleClass().addAll(Styles.TITLE_3, Styles.TEXT_BOLD);
        
        Label lblCliente = new Label("Cliente: " + faturaCompleta.getCliente().getNome());
        lblCliente.getStyleClass().add(Styles.TEXT_MUTED);
        
        Label lblValor = new Label("Valor: " + java.text.NumberFormat.getCurrencyInstance(new java.util.Locale("pt", "AO")).format(faturaCompleta.getTotal()));
        lblValor.getStyleClass().add(Styles.TEXT_MUTED);
        
        infoBox.getChildren().addAll(lblFatura, lblCliente, lblValor);
        infoCard.setBody(infoBox);

        // 2. Email Fields Card
        Card emailCard = new Card();
        emailCard.getStyleClass().add(Styles.ELEVATED_1);
        emailCard.setHeader(new Label("Destinatário e Assunto"));
        
        GridPane emailGrid = new GridPane();
        emailGrid.setVgap(10);
        emailGrid.setHgap(10);
        emailGrid.setPadding(new Insets(15));
        
        Label lblEmail = new Label("Para:");
        TextField txtEmail = new TextField(faturaCompleta.getCliente().getEmail());
        txtEmail.setPrefWidth(400);
        
        Label lblAssunto = new Label("Assunto:");
        TextField txtAssunto = new TextField("Fatura " + faturaCompleta.getNumero() + " - " + faturaCompleta.getCliente().getNome());
        txtAssunto.setPrefWidth(400);
        
        emailGrid.add(lblEmail, 0, 0);
        emailGrid.add(txtEmail, 1, 0);
        emailGrid.add(lblAssunto, 0, 1);
        emailGrid.add(txtAssunto, 1, 1);
        
        emailCard.setBody(emailGrid);

        // 3. Message Card
        Card messageCard = new Card();
        messageCard.getStyleClass().add(Styles.ELEVATED_1);
        messageCard.setHeader(new Label("Mensagem"));
        
        TextArea txtMensagem = new TextArea(
            "Prezado(a) " + faturaCompleta.getCliente().getNome() + ",\n\n" +
            "Segue em anexo a fatura " + faturaCompleta.getNumero() + " no valor de " + 
            java.text.NumberFormat.getCurrencyInstance(new java.util.Locale("pt", "AO")).format(faturaCompleta.getTotal()) + ".\n\n" +
            "Agradecemos a sua preferência.\n\n" +
            "Atenciosamente,\n" +
            "Kubata Faturação"
        );
        txtMensagem.setPrefRowCount(8);
        txtMensagem.setWrapText(true);
        txtMensagem.setPadding(new Insets(10));
        
        VBox messageBox = new VBox(txtMensagem);
        messageBox.setPadding(new Insets(0, 15, 15, 15));
        messageCard.setMinHeight(300);
        messageCard.setBody(messageBox);

        // 4. Attachment Card
        Card attachmentCard = new Card();
        attachmentCard.getStyleClass().add(Styles.ELEVATED_1);
        
        HBox attachmentBox = new HBox(10);
        attachmentBox.setAlignment(Pos.CENTER_LEFT);
        attachmentBox.setPadding(new Insets(15));
        
        FontIcon fileIcon = new FontIcon(Feather.FILE_TEXT);
        fileIcon.setIconSize(24);
        fileIcon.setIconColor(javafx.scene.paint.Color.web("#e74c3c"));
        
        VBox fileInfo = new VBox(3);
        Label lblFileName = new Label("Fatura_" + faturaCompleta.getNumero().replace("/", "_") + ".pdf");
        lblFileName.getStyleClass().add(Styles.TEXT_BOLD);
        Label lblFileSize = new Label(String.format("%.1f KB", tempFile.length() / 1024.0));
        lblFileSize.getStyleClass().add(Styles.TEXT_MUTED);
        fileInfo.getChildren().addAll(lblFileName, lblFileSize);
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Button btnVerPdf = new Button("Visualizar", IconUtils.icon(Feather.EYE, IconUtils.SIZE_SMALL));
        btnVerPdf.getStyleClass().add(Styles.BUTTON_OUTLINED);
        btnVerPdf.setOnAction(e -> {
            try {
                java.awt.Desktop.getDesktop().open(tempFile);
            } catch (Exception ex) {
                AlertUtils.showErrorAlert("Erro", "Não foi possível abrir o PDF.");
            }
        });
        
        attachmentBox.getChildren().addAll(fileIcon, fileInfo, spacer, btnVerPdf);
        attachmentCard.setBody(attachmentBox);

        // Add all cards to dialog
        dialogContent.getChildren().addAll(infoCard, emailCard, messageCard, attachmentCard);

        // Show modal
        modalService.create()
                .title("Enviar Fatura por Email")
                .content(dialogContent)
                .dynamicSize()
                .withConfirmButton("Enviar Email", () -> {
                    try {
                        // Validate email
                        String email = txtEmail.getText().trim();
                        if (email.isEmpty() || !email.contains("@")) {
                            AlertUtils.showWarningAlert("Aviso", "Por favor, insira um email válido.");
                            return false;
                        }
                        
                        String assunto = txtAssunto.getText().trim();
                        if (assunto.isEmpty()) {
                            AlertUtils.showWarningAlert("Aviso", "Por favor, insira um assunto.");
                            return false;
                        }
                        
                        // Usar novo método com rastreamento
                        String enviadoPor = sessionManager.getUserObject() != null ? 
                            sessionManager.getUserObject().getUsername() : "Sistema";
                        
                        emailService.enviarEmailComAnexo(
                                email,
                                assunto,
                                txtMensagem.getText(),
                                tempFile,
                                faturaCompleta.getId(),
                                faturaCompleta.getNumero(),
                                enviadoPor
                        );

                        AlertUtils.showInfoAlert("Sucesso", "Email enviado com sucesso para " + email + "! Verifique o status na Gestão de Emails.");
                        return true;
                    } catch (Exception e) {
                        AlertUtils.showExceptionAlert("Erro ao Enviar Email", "Não foi possível enviar o e-mail com a fatura. Verifique a conexão SMTP nas configurações.", e);
                        return false;
                    }
                })
                .withCancelButton("Cancelar")
                .buildAndShow();
    }

    private void handleGerarDuplicado() {
        Fatura f = getSelectedFatura();
        if (f == null) {
            AlertUtils.showWarningAlert("Aviso", "Selecione uma fatura.");
            return;
        }
        if (f.getStatus() != StatusFatura.EMITIDA) {
            AlertUtils.showWarningAlert("Aviso", "Apenas faturas emitidas podem gerar duplicados.");
            return;
        }
        TextField motivoField = new TextField("Duplicado para arquivo");
        VBox content = new VBox(8, new Label("Motivo do duplicado"), motivoField);
        content.setPadding(new Insets(10));
        modalService.create()
                .title("Gerar Duplicado")
                .content(content)
                .autoSize()
                .withConfirmButton("Gerar", () -> {
                    try {
                        String motivo = motivoField.getText();
                        documentoEstadoService.gerarDuplicado(f, 1L, motivo);
                        AlertUtils.showInfoAlert("Sucesso", "Duplicado gerado com sucesso!");
                        refresh();
                        return true;
                    } catch (Exception ex) {
                        AlertUtils.showExceptionAlert("Erro ao Gerar Duplicado", "Não foi possível gerar o duplicado.", ex);
                        return false;
                    }
                })
                .withCancelButton("Cancelar")
                .buildAndShow();
    }

    private void handleGerarSegundaVia() {
        Fatura f = getSelectedFatura();
        if (f == null) {
            AlertUtils.showWarningAlert("Aviso", "Selecione uma fatura.");
            return;
        }
        if (f.getStatus() != StatusFatura.EMITIDA) {
            AlertUtils.showWarningAlert("Aviso", "Apenas faturas emitidas podem gerar segunda via.");
            return;
        }
        TextField motivoField = new TextField("Segunda via solicitada pelo cliente");
        VBox content = new VBox(8, new Label("Motivo da segunda via"), motivoField);
        content.setPadding(new Insets(10));
        modalService.create()
                .title("Gerar Segunda Via")
                .content(content)
                .autoSize()
                .withConfirmButton("Gerar", () -> {
                    try {
                        String motivo = motivoField.getText();
                        documentoEstadoService.gerarSegundaVia(f, 1L, motivo);
                        AlertUtils.showInfoAlert("Sucesso", "Segunda via gerada com sucesso!");
                        refresh();
                        return true;
                    } catch (Exception ex) {
                        AlertUtils.showExceptionAlert("Erro ao Gerar Segunda Via", "Não foi possível gerar a segunda via.", ex);
                        return false;
                    }
                })
                .withCancelButton("Cancelar")
                .buildAndShow();
    }
    /**
     * Submeter fatura à AGT manualmente
     */
    private void submeterAGT() {
        Fatura f = getSelectedFatura();
        if (f == null) {
            AlertUtils.showWarningAlert("Aviso", "Selecione uma fatura para submeter à AGT.");
            return;
        }

        // Verificar se a fatura já foi submetida
        if ("SUCCESS".equals(f.getAgtSubmissionStatus())) {
            AlertUtils.showWarningAlert("Aviso", "Esta fatura já foi submetida e validada pela AGT.");
            return;
        }

        boolean confirmed = AlertUtils.showConfirmation("Confirmar Submissão AGT",
            "Deseja submeter a fatura " + f.getNumero() + " à AGT?");

        if (confirmed) {
            String loadingId = loadingService.showLoading("A submeter fatura à AGT...");

            agtElectronicInvoiceService.submitInvoiceAsync(f)
                .thenAccept(response -> {
                    javafx.application.Platform.runLater(() -> {
                        loadingService.hideLoading(loadingId);
                        if (response != null && response.getValidationResults() != null && !response.getValidationResults().isEmpty()) {
                            String validationCode = response.getValidationResults().get(0).getValidationCode();
                            AlertUtils.showInfoAlert("Sucesso", "Fatura submetida com sucesso!\nCódigo de validação: " + validationCode);
                        } else {
                            AlertUtils.showInfoAlert("Sucesso", "Fatura submetida com sucesso!");
                        }
                        refresh();
                    });
                })
                .exceptionally(ex -> {
                    javafx.application.Platform.runLater(() -> {
                        loadingService.hideLoading(loadingId);
                        AlertUtils.showExceptionAlert("Erro", "Falha ao submeter fatura à AGT", ex);
                    });
                    return null;
                });
        }

    }
 
    /**
     * Cancelar/Anular fatura na AGT
     */
    private void cancelarAGT() {
    Fatura f = getSelectedFatura();
    if (f == null) {
        AlertUtils.showWarningAlert("Aviso", "Selecione uma fatura para cancelar na AGT.");
        return;
    }

    // Verificar se a fatura já foi submetida
    if (!"SUCCESS".equals(f.getAgtSubmissionStatus())) {
        AlertUtils.showWarningAlert("Aviso", "Apenas faturas validadas pela AGT podem ser canceladas.");
        return;
    }

    // Modal para motivo de cancelamento
    Dialog<ButtonType> dialog = new Dialog<>();
    dialog.setTitle("Cancelar Fatura na AGT");
    dialog.setHeaderText("Selecione o motivo de anulação");

    VBox content = new VBox(10);
    content.setPadding(new Insets(10));

    Label lblMotivo = new Label("Motivo de anulação:");
    ComboBox<ao.allon.kubata.faturacao.domain.enums.DocumentCancelReason> cmbMotivo = new ComboBox<>();
    cmbMotivo.getItems().addAll(ao.allon.kubata.faturacao.domain.enums.DocumentCancelReason.values());
    cmbMotivo.setPromptText("Selecione o motivo");

    content.getChildren().addAll(lblMotivo, cmbMotivo);
    dialog.getDialogPane().setContent(content);
    dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

    Optional<ButtonType> result = dialog.showAndWait();
    if (result.isPresent() && result.get() == ButtonType.OK) {
        if (cmbMotivo.getValue() == null) {
            AlertUtils.showWarningAlert("Aviso", "Selecione um motivo de anulação.");
            return;
        }

        String loadingId = loadingService.showLoading("A cancelar fatura na AGT...");

        agtElectronicInvoiceService.cancelInvoice(f, cmbMotivo.getValue())
            .thenAccept(response -> {
                javafx.application.Platform.runLater(() -> {
                    loadingService.hideLoading(loadingId);
                    AlertUtils.showInfoAlert("Sucesso", "Fatura cancelada na AGT com sucesso!");
                    refresh();
                });
            })
            .exceptionally(ex -> {
                javafx.application.Platform.runLater(() -> {
                    loadingService.hideLoading(loadingId);
                    AlertUtils.showExceptionAlert("Erro", "Falha ao cancelar fatura na AGT", ex);
                });
                return null;
            });
    }
}

/**
 * Corrigir documento rejeitado na AGT
 */
private void corrigirAGT() {
        Fatura f = getSelectedFatura();
        if (f == null) {
            AlertUtils.showWarningAlert("Aviso", "Selecione uma fatura para corrigir.");
            return;
        }

        if (!"FAILED".equals(f.getAgtSubmissionStatus())) {
            AlertUtils.showWarningAlert("Aviso", "Apenas faturas rejeitadas podem ser corrigidas.");
            return;
        }

        boolean confirmed = AlertUtils.showConfirmation("Corrigir Fatura na AGT",
            "Deseja submeter a correção da fatura " + f.getNumero() + " à AGT?\n\n" +
            "Documento rejeitado: " + (f.getRejectedDocumentNo() != null ? f.getRejectedDocumentNo() : "N/A"));

        if (confirmed) {
            String loadingId = loadingService.showLoading("A submeter correção à AGT...");

        agtElectronicInvoiceService.submitCorrection(f, f.getRejectedDocumentNo())
            .thenAccept(response -> {
                javafx.application.Platform.runLater(() -> {
                    loadingService.hideLoading(loadingId);
                    if (response != null && response.getValidationResults() != null && !response.getValidationResults().isEmpty()) {
                        String validationCode = response.getValidationResults().get(0).getValidationCode();
                        AlertUtils.showInfoAlert("Sucesso", "Correção submetida com sucesso!\nCódigo: " + validationCode);
                    } else {
                        AlertUtils.showInfoAlert("Sucesso", "Correção submetida com sucesso!");
                    }
                    refresh();
                });
            })
            .exceptionally(ex -> {
                javafx.application.Platform.runLater(() -> {
                    loadingService.hideLoading(loadingId);
                    AlertUtils.showExceptionAlert("Erro", "Falha ao submeter correção", ex);
                });
                return null;
            });
    }
}
}

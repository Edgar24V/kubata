package ao.allon.kubata.faturacao.view;

import ao.allon.kubata.core.ui.table.AdvancedTableView;
import ao.allon.kubata.core.ui.table.TableUtils;
import ao.allon.kubata.faturacao.domain.Recibo;
import ao.allon.kubata.faturacao.domain.Fatura;
import ao.allon.kubata.faturacao.service.ReciboService;
import ao.allon.kubata.faturacao.service.FaturaService;
import ao.allon.kubata.faturacao.service.SessionManager;
import ao.allon.kubata.faturacao.ui.modal.ModalService;
import ao.allon.kubata.faturacao.ui.util.AlertUtils;
import ao.allon.kubata.faturacao.ui.util.IconUtils;
import atlantafx.base.controls.Card;
import atlantafx.base.controls.CustomTextField;
import atlantafx.base.theme.Styles;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import javafx.stage.FileChooser;

public class RecibosView extends BorderPane {

    private final ReciboService reciboService;
    private final FaturaService faturaService;
    private final SessionManager sessionManager;
    private final ModalService modalService;

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", new Locale("pt", "AO"));
    private final ObservableList<Recibo> masterData = FXCollections.observableArrayList();

    // KPI Labels
    private Label lblTotalRecebidoHoje;
    private Label lblTotalRecebidoMes;
    private Label lblTotalRecibos;
    private Label lblMediaRecebimento;

    // UI Components
    private AdvancedTableView<Recibo> table;
    private CustomTextField txtSearch;
    private DatePicker dpInicio;
    private DatePicker dpFim;

    private StackPane root;
    private VBox headerBox;
    private VBox body;
    private StackPane loadingOverlay;
    private VBox emptyState;

    public RecibosView(ReciboService reciboService, FaturaService faturaService, SessionManager sessionManager, ModalService modalService) {
        this.reciboService = reciboService;
        this.faturaService = faturaService;
        this.sessionManager = sessionManager;
        this.modalService = modalService;

        getStyleClass().add("recibos-view");
        initializeUI();
        loadData();
    }

    private void initializeUI() {
        setPadding(new Insets(0));

        root = new StackPane();
        headerBox = new VBox(10);
        headerBox.setPadding(new Insets(20, 30, 10, 30));
        headerBox.setStyle("-fx-background-color: -color-bg-default; -fx-border-color: -color-border-default; -fx-border-width: 0 0 1 0;");

        body = new VBox(20);
        body.setPadding(new Insets(20, 30, 30, 30));
        body.setFillWidth(true);

        headerBox.getChildren().add(createHeader());
        body.getChildren().add(createKPISection());
        body.getChildren().add(createFiltersAndTableSection());

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
        Label title = new Label("Gestão de Recibos");
        title.getStyleClass().addAll(Styles.TITLE_2);
        title.setFont(Font.font("System", FontWeight.BOLD, 24));

        Label subtitle = new Label("Controle de recebimentos e comprovantes");
        subtitle.getStyleClass().add(Styles.TEXT_MUTED);
        titleBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnNovo = new Button("Novo", IconUtils.icon(Feather.PLUS, IconUtils.SIZE_SMALL));
        btnNovo.getStyleClass().addAll(Styles.SUCCESS);
        btnNovo.setOnAction(e -> showRegistrarModal());
        boolean canRecibo = sessionManager.hasAccess("RECIBOS", "Emitir");
        btnNovo.setDisable(!canRecibo);

        Button btnExportar = new Button("Exportar", IconUtils.icon(Feather.DOWNLOAD, IconUtils.SIZE_SMALL));
        btnExportar.getStyleClass().addAll(Styles.BUTTON_OUTLINED);
        btnExportar.setOnAction(e -> exportarCSV());

        Button btnAtualizar = new Button("Atualizar", IconUtils.icon(Feather.REFRESH_CW, IconUtils.SIZE_SMALL));
        btnAtualizar.getStyleClass().addAll(Styles.BUTTON_OUTLINED);
        btnAtualizar.setOnAction(e -> recarregarAsync());

        header.getChildren().addAll(titleBox, spacer, btnNovo, btnExportar, btnAtualizar);
        return header;
    }

    private FlowPane createKPISection() {
        FlowPane pane = new FlowPane();
        pane.setHgap(20);
        pane.setVgap(20);
        pane.setAlignment(Pos.TOP_LEFT);

        lblTotalRecebidoHoje = new Label("Kz 0,00");
        lblTotalRecebidoMes = new Label("Kz 0,00");
        lblTotalRecibos = new Label("0");
        lblMediaRecebimento = new Label("Kz 0,00");

        pane.getChildren().addAll(
            createKPICard("Recebido Hoje", lblTotalRecebidoHoje, Feather.DOLLAR_SIGN, Styles.SUCCESS),
            createKPICard("Recebido Mês", lblTotalRecebidoMes, Feather.TRENDING_UP, Styles.ACCENT),
            createKPICard("Total Recibos", lblTotalRecibos, Feather.FILE_TEXT, Styles.WARNING),
            createKPICard("Média/Recibo", lblMediaRecebimento, Feather.ACTIVITY, Styles.TEXT_MUTED)
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
        HBox filters = new HBox(15);
        filters.setAlignment(Pos.CENTER_LEFT);
        filters.setPadding(new Insets(0, 0, 10, 0));

        txtSearch = new CustomTextField();
        txtSearch.setPromptText("Pesquisar por número, fatura...");
        txtSearch.setLeft(IconUtils.icon(Feather.SEARCH, IconUtils.SIZE_SMALL));
        txtSearch.setPrefWidth(250);
        txtSearch.textProperty().addListener((obs, ov, nv) -> applyFilters());

        dpInicio = new DatePicker();
        dpInicio.setPromptText("Data Início");
        dpInicio.setOnAction(e -> applyFilters());

        dpFim = new DatePicker();
        dpFim.setPromptText("Data Fim");
        dpFim.setOnAction(e -> applyFilters());

        Button btnLimpar = new Button("Limpar", IconUtils.icon(Feather.X, IconUtils.SIZE_SMALL));
        btnLimpar.getStyleClass().add(Styles.BUTTON_OUTLINED);
        btnLimpar.setOnAction(e -> limparFiltros());

        filters.getChildren().addAll(txtSearch, dpInicio, dpFim, btnLimpar);

        table = createTable();

        VBox section = new VBox(10, filters, table);
        VBox.setVgrow(table, Priority.ALWAYS);
        return section;
    }

    private AdvancedTableView<Recibo> createTable() {
        AdvancedTableView<Recibo> t = new AdvancedTableView<>();
        TableUtils.standardize(t);
        t.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        t.setPrefHeight(400);

        TableColumn<Recibo, Long> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colId.setMinWidth(60);
        colId.setMaxWidth(80);

        TableColumn<Recibo, String> colNumero = new TableColumn<>("Número");
        colNumero.setCellValueFactory(new PropertyValueFactory<>("numero"));
        colNumero.setMinWidth(100);

        TableColumn<Recibo, LocalDate> colData = new TableColumn<>("Data Recebimento");
        colData.setCellValueFactory(new PropertyValueFactory<>("dataRecebimento"));
        colData.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.format(dateFormatter));
            }
        });
        colData.setMinWidth(120);

        TableColumn<Recibo, String> colFatura = new TableColumn<>("Fatura");
        colFatura.setCellValueFactory(cell -> {
            Fatura f = cell.getValue().getFatura();
            return new javafx.beans.property.SimpleStringProperty(f != null ? f.getNumero() : "-");
        });
        colFatura.setMinWidth(100);

        TableColumn<Recibo, String> colCliente = new TableColumn<>("Cliente");
        colCliente.setCellValueFactory(cell -> {
            Fatura f = cell.getValue().getFatura();
            return new javafx.beans.property.SimpleStringProperty(
                f != null && f.getCliente() != null ? f.getCliente().getNome() : "-"
            );
        });
        colCliente.setMinWidth(200);

        TableColumn<Recibo, BigDecimal> colValor = new TableColumn<>("Valor");
        colValor.setCellValueFactory(new PropertyValueFactory<>("valor"));
        colValor.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.format("Kz %,.2f", item));
                if (!empty && item != null) setAlignment(Pos.CENTER_RIGHT);
            }
        });
        colValor.setMinWidth(120);

        TableColumn<Recibo, String> colReferencia = new TableColumn<>("Referência");
        colReferencia.setCellValueFactory(new PropertyValueFactory<>("referencia"));
        colReferencia.setMinWidth(150);

        TableColumn<Recibo, Void> colAcoes = new TableColumn<>("");
        colAcoes.setMinWidth(44);
        colAcoes.setMaxWidth(64);
        colAcoes.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("", IconUtils.icon(Feather.MORE_VERTICAL, IconUtils.SIZE_SMALL));
            {
                btn.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.FLAT);
                btn.setOnAction(e -> {
                    Recibo r = getTableRow() != null ? getTableRow().getItem() : null;
                    if (r == null) return;
                    ContextMenu cm = buildRowMenu(r);
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

        t.getColumns().addAll(colId, colNumero, colData, colFatura, colCliente, colValor, colReferencia, colAcoes);

        t.setRowFactory(tv -> {
            TableRow<Recibo> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (row.isEmpty()) return;
                if (event.getButton() == javafx.scene.input.MouseButton.SECONDARY) {
                    ContextMenu cm = buildRowMenu(row.getItem());
                    cm.show(row, event.getScreenX(), event.getScreenY());
                }
            });
            return row;
        });
        return t;
    }

    private ContextMenu buildRowMenu(Recibo r) {
        MenuItem miNovo = new MenuItem("Novo Recibo", IconUtils.icon(Feather.PLUS, IconUtils.SIZE_SMALL));
        miNovo.setOnAction(e -> showRegistrarModal());

        MenuItem miAtualizar = new MenuItem("Atualizar", IconUtils.icon(Feather.REFRESH_CW, IconUtils.SIZE_SMALL));
        miAtualizar.setOnAction(e -> recarregarAsync());

        MenuItem miExportar = new MenuItem("Exportar CSV", IconUtils.icon(Feather.DOWNLOAD, IconUtils.SIZE_SMALL));
        miExportar.setOnAction(e -> exportarCSV());

        return new ContextMenu(miNovo, miAtualizar, new SeparatorMenuItem(), miExportar);
    }

    private void loadData() {
        masterData.setAll(reciboService.findAll());
        table.setData(masterData);
        refreshKPIs();
        updateEmptyState();
    }

    private void applyFilters() {
        String searchText = txtSearch.getText() != null ? txtSearch.getText().toLowerCase() : "";
        LocalDate inicio = dpInicio.getValue();
        LocalDate fim = dpFim.getValue();

        table.setFilter(recibo -> {
            if (!searchText.isEmpty()) {
                String numero = recibo.getNumero() != null ? recibo.getNumero().toLowerCase() : "";
                String id = String.valueOf(recibo.getId());
                String fatura = recibo.getFatura() != null && recibo.getFatura().getNumero() != null 
                    ? recibo.getFatura().getNumero().toLowerCase() : "";
                String cliente = recibo.getFatura() != null && recibo.getFatura().getCliente() != null
                    ? recibo.getFatura().getCliente().getNome().toLowerCase() : "";

                if (!numero.contains(searchText) && !id.contains(searchText) && 
                    !fatura.contains(searchText) && !cliente.contains(searchText)) {
                    return false;
                }
            }

            if (inicio != null && (recibo.getDataRecebimento() == null || recibo.getDataRecebimento().isBefore(inicio))) {
                return false;
            }
            if (fim != null && (recibo.getDataRecebimento() == null || recibo.getDataRecebimento().isAfter(fim))) {
                return false;
            }

            return true;
        });

        refreshKPIs();
        updateEmptyState();
    }

    private void recarregarAsync() {
        showLoading(true);
        Thread t = new Thread(() -> {
            try {
                java.util.List<Recibo> all = reciboService.findAll();
                Platform.runLater(() -> {
                    masterData.setAll(all);
                    table.setData(masterData);
                    applyFilters();
                });
            } catch (Exception ex) {
                Platform.runLater(() -> AlertUtils.showExceptionAlert("Erro", "Não foi possível atualizar dados.", ex));
            } finally {
                Platform.runLater(() -> showLoading(false));
            }
        }, "recibos-reload");
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
        Label sub = new Label("Nenhum recibo encontrado com os filtros atuais.");
        sub.getStyleClass().addAll(Styles.TEXT_MUTED);
        Button btn = new Button("Criar Recibo", IconUtils.icon(Feather.PLUS, IconUtils.SIZE_SMALL));
        btn.getStyleClass().addAll(Styles.SUCCESS);
        btn.setOnAction(e -> showRegistrarModal());

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
        dpInicio.setValue(null);
        dpFim.setValue(null);
        applyFilters();
    }

    private void refreshKPIs() {
        LocalDate hoje = LocalDate.now();
        LocalDate inicioMes = hoje.withDayOfMonth(1);

        BigDecimal recebidoHoje = table.getItems().stream()
            .filter(r -> r.getDataRecebimento() != null && r.getDataRecebimento().equals(hoje))
            .map(Recibo::getValor)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal recebidoMes = table.getItems().stream()
            .filter(r -> r.getDataRecebimento() != null && !r.getDataRecebimento().isBefore(inicioMes))
            .map(Recibo::getValor)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalRecibos = table.getItems().size();

        BigDecimal media = totalRecibos > 0 
            ? table.getItems().stream().map(Recibo::getValor).reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(totalRecibos), 2, java.math.RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        lblTotalRecebidoHoje.setText(String.format("Kz %,.2f", recebidoHoje));
        lblTotalRecebidoMes.setText(String.format("Kz %,.2f", recebidoMes));
        lblTotalRecibos.setText(String.valueOf(totalRecibos));
        lblMediaRecebimento.setText(String.format("Kz %,.2f", media));
    }

    private void showRegistrarModal() {
        TextField faturaId = new TextField();
        faturaId.setPromptText("ID da Fatura");
        TextField valor = new TextField();
        valor.setPromptText("Valor recebido");
        TextField referencia = new TextField();
        referencia.setPromptText("Referência (opcional)");
        TextArea obs = new TextArea();
        obs.setPromptText("Observações");
        obs.setPrefRowCount(3);

        VBox content = new VBox(8, 
            new Label("Fatura ID"), faturaId, 
            new Label("Valor"), valor, 
            new Label("Referência"), referencia, 
            new Label("Observações"), obs
        );
        content.setPadding(new Insets(10));

        modalService.create()
            .title("Registrar Recebimento")
            .content(content)
            .autoSize()
            .withConfirmButton("Confirmar", () -> {
                try {
                    Long id = Long.parseLong(faturaId.getText());
                    BigDecimal v = new BigDecimal(valor.getText().replace(",", "."));
                    reciboService.registrar(id, v, referencia.getText(), obs.getText());
                    loadData();
                    return true;
                } catch (Exception ex) {
                    AlertUtils.showErrorAlert("Erro", ex.getMessage());
                    return false;
                }
            })
            .withCancelButton("Cancelar")
            .buildAndShow();
    }

    private void exportarCSV() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exportar Recibos");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
        fileChooser.setInitialFileName("recibos_" + LocalDate.now() + ".csv");

        File file = fileChooser.showSaveDialog(getScene().getWindow());
        if (file != null) {
            try (FileWriter writer = new FileWriter(file)) {
                writer.write("ID;Número;Data;Fatura;Cliente;Valor;Referência\n");
                for (Recibo r : table.getItems()) {
                    writer.write(String.format("%d;%s;%s;%s;%s;%.2f;%s\n",
                        r.getId(),
                        r.getNumero(),
                        r.getDataRecebimento() != null ? r.getDataRecebimento().format(dateFormatter) : "",
                        r.getFatura() != null ? r.getFatura().getNumero() : "",
                        r.getFatura() != null && r.getFatura().getCliente() != null ? r.getFatura().getCliente().getNome() : "",
                        r.getValor() != null ? r.getValor() : BigDecimal.ZERO,
                        r.getReferencia() != null ? r.getReferencia() : ""
                    ));
                }
                AlertUtils.showInfoAlert("Sucesso", "Recibos exportados com sucesso!");
            } catch (IOException e) {
                AlertUtils.showExceptionAlert("Erro", "Não foi possível exportar.", e);
            }
        }
    }
}

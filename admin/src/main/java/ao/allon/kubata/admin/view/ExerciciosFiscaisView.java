package ao.allon.kubata.admin.view;

import ao.allon.kubata.admin.service.PersistenceService;
import ao.allon.kubata.admin.service.SessionManager;
import ao.allon.kubata.admin.ui.modal.ModalManager;
import ao.allon.kubata.admin.ui.util.IconUtils;
import ao.allon.kubata.core.domain.Empresa;
import ao.allon.kubata.core.domain.ExercicioFiscal;
import ao.allon.kubata.core.domain.ExercicioFiscal.EstadoExercicio;
import ao.allon.kubata.core.domain.SerieDocumento;
import ao.allon.kubata.core.service.SerieDocumentoService;
import ao.allon.kubata.core.repository.EmpresaRepository;
import ao.allon.kubata.core.repository.ExercicioFiscalRepository;
import ao.allon.kubata.core.service.AcessoService;
import ao.allon.kubata.core.service.ExercicioFiscalService;
import ao.allon.kubata.core.ui.table.AdvancedTableView;
import ao.allon.kubata.core.ui.table.TableUtils;
import ao.allon.kubata.core.ui.table.TextTableCell;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Component
public class ExerciciosFiscaisView extends VBox {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ExercicioFiscalService exercicioService;
    private final SerieDocumentoService serieDocumentoService;
    private final ExercicioFiscalRepository exercicioRepository;
    private final EmpresaRepository empresaRepository;
    private final AcessoService acessoService;
    private final SessionManager sessionManager;
    private final ModalManager modalManager;
    private final PersistenceService persistenceService;

    private final ObservableList<ExercicioFiscal> exercicios =
            FXCollections.observableArrayList();
    private final ObservableList<Empresa> empresas =
            FXCollections.observableArrayList();

    private FilteredList<ExercicioFiscal> exerciciosFiltrados;

    private AdvancedTableView<ExercicioFiscal> table;
    private TextField searchField;
    private ComboBox<Empresa> empresaCombo;
    private ComboBox<String> estadoCombo;

    private Label totalLabel;
    private Label abertosLabel;
    private Label encerramentoLabel;
    private Label fechadosLabel;

    private Label detailTitle;
    private Label detailEmpresa;
    private Label detailAno;
    private Label detailPeriodo;
    private Label detailStatus;
    private Label detailActual;
    private Label detailEncerrado;

    private VBox detailsPane;

    private Button btnActual;
    private Button btnEncerramento;
    private Button btnFechar;
    private Button btnReabrir;

    private boolean dataLoaded = false;

    public ExerciciosFiscaisView(ExercicioFiscalService exercicioService,
                                SerieDocumentoService serieDocumentoService,
                                ExercicioFiscalRepository exercicioRepository,
                                EmpresaRepository empresaRepository,
                                AcessoService acessoService,
                                SessionManager sessionManager,
                                ModalManager modalManager,
                                PersistenceService persistenceService) {
        this.exercicioService = exercicioService;
        this.serieDocumentoService = serieDocumentoService;
        this.exercicioRepository = exercicioRepository;
        this.empresaRepository = empresaRepository;
        this.acessoService = acessoService;
        this.sessionManager = sessionManager;
        this.modalManager = modalManager;
        this.persistenceService = persistenceService;

        buildUI();
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();

        if (!dataLoaded && getScene() != null) {
            dataLoaded = true;
            loadData();
        }
    }

    private void buildUI() {
        getStyleClass().add("exercicios-view");
        setSpacing(0);
        setPadding(Insets.EMPTY);
        setFillWidth(true);

        VBox header = buildHeader();
        HBox kpis = buildKpis();

        table = buildTable();
        detailsPane = buildDetails();

        BorderPane body = new BorderPane();
        body.setPadding(new Insets(0, 18, 18, 18));

        SplitPane split = new SplitPane(table, detailsPane);
        split.setDividerPositions(0.71);
        split.getStyleClass().add("empresa-content-split");
        BorderPane.setMargin(split, new Insets(12, 0, 0, 0));
        body.setCenter(split);

        getChildren().addAll(header, kpis, body);
        VBox.setVgrow(body, Priority.ALWAYS);
    }

    private VBox buildHeader() {
        VBox root = new VBox(10);
        root.setPadding(new Insets(16, 18, 10, 18));
        root.getStyleClass().add("empresa-header");

        HBox titleRow = new HBox(12);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label icon = new Label("", IconUtils.icon(Feather.CALENDAR, 18));
        icon.getStyleClass().add("empresa-title-icon");

        VBox titles = new VBox(3);
        Label title = new Label("Exercícios Fiscais");
        title.getStyleClass().add("h2");

        Label subtitle = new Label(
                "Gestão dos períodos fiscais, exercício actual e operações de encerramento"
        );
        subtitle.getStyleClass().add("text-muted");
        titles.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        searchField = new TextField();
        searchField.setPromptText("Pesquisar empresa, ano, estado ou observações...");
        searchField.setPrefWidth(300);
        searchField.setMaxWidth(360);
        searchField.getStyleClass().add("empresa-search");
        searchField.textProperty().addListener((obs, old, value) -> applyFilters());

        Button btnNovo = new Button(
                "Novo exercício",
                IconUtils.icon(Feather.PLUS_CIRCLE, IconUtils.SIZE_SMALL)
        );
        btnNovo.getStyleClass().add("button-primary");
        btnNovo.setOnAction(e -> showNovoExercicioDialog());

        Button btnRefresh = new Button(
                "",
                IconUtils.icon(Feather.REFRESH_CW, IconUtils.SIZE_SMALL)
        );
        btnRefresh.getStyleClass().add("button-outlined");
        btnRefresh.setTooltip(new Tooltip("Actualizar exercícios fiscais"));
        btnRefresh.setOnAction(e -> loadData());

        titleRow.getChildren().addAll(
                icon, titles, spacer, searchField, btnNovo, btnRefresh
        );

        HBox filterRow = buildFilterRow();
        root.getChildren().addAll(titleRow, filterRow);
        return root;
    }

    private HBox buildFilterRow() {
        HBox box = new HBox(10);
        box.getStyleClass().add("exercicios-header-filter-row");
        box.setAlignment(Pos.CENTER_LEFT);

        Label companyLabel = new Label("Empresa");
        companyLabel.getStyleClass().add("exercicios-filter-label");

        empresaCombo = new ComboBox<>(empresas);
        empresaCombo.setPromptText("Todas as empresas");
        empresaCombo.setPrefWidth(250);
        empresaCombo.setMaxWidth(320);
        empresaCombo.getStyleClass().add("exercicios-filter-combo");
        empresaCombo.setOnAction(e -> applyFilters());

        Label estadoLabel = new Label("Estado");
        estadoLabel.getStyleClass().add("exercicios-filter-label");

        estadoCombo = new ComboBox<>(
                FXCollections.observableArrayList(
                        "Todos",
                        "Aberto",
                        "Em Encerramento",
                        "Fechado"
                )
        );
        estadoCombo.setValue("Todos");
        estadoCombo.setPrefWidth(180);
        estadoCombo.getStyleClass().add("exercicios-filter-combo");
        estadoCombo.setOnAction(e -> applyFilters());

        Label hint = new Label("Seleccione uma linha para consultar os detalhes e operações.");
        hint.getStyleClass().add("exercicios-filter-hint");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        box.getChildren().addAll(
                companyLabel, empresaCombo,
                estadoLabel, estadoCombo,
                spacer, hint
        );
        return box;
    }

    private HBox buildKpis() {
        HBox row = new HBox(12);
        row.getStyleClass().add("empresa-kpi-row");
        row.setPadding(new Insets(4, 18, 8, 18));

        totalLabel = new Label("0");
        abertosLabel = new Label("0");
        encerramentoLabel = new Label("0");
        fechadosLabel = new Label("0");

        row.getChildren().addAll(
                kpi("Total de exercícios", totalLabel, Feather.LAYERS),
                kpi("Abertos", abertosLabel, Feather.UNLOCK),
                kpi("Em encerramento", encerramentoLabel, Feather.POWER),
                kpi("Fechados", fechadosLabel, Feather.LOCK)
        );
        return row;
    }

    private VBox kpi(String title, Label value, Feather iconCode) {
        Label icon = new Label("", IconUtils.icon(iconCode, 16));
        icon.getStyleClass().add("empresa-kpi-icon");

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("empresa-kpi-title");
        value.getStyleClass().add("empresa-kpi-value");

        VBox text = new VBox(1, titleLabel, value);
        VBox card = new VBox(4, new HBox(8, icon, text));
        card.getStyleClass().add("empresa-kpi-card");
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    private AdvancedTableView<ExercicioFiscal> buildTable() {
        table = new AdvancedTableView<>();
        table.setEditable(true);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        TableUtils.standardize(table);
        table.setEntityName("Exercício");
        table.setOnRefresh(this::loadData);

        exerciciosFiltrados = new FilteredList<>(exercicios, item -> true);
        table.setData(exerciciosFiltrados);

        TableColumn<ExercicioFiscal, String> colEmpresa = new TableColumn<>("Empresa");
        colEmpresa.setCellValueFactory(cell ->
                new SimpleStringProperty(
                        cell.getValue().getEmpresa() == null
                                ? "—"
                                : cell.getValue().getEmpresa().getNome()
                )
        );
        colEmpresa.setPrefWidth(220);

        TableColumn<ExercicioFiscal, Integer> colAno = new TableColumn<>("Ano");
        colAno.setCellValueFactory(new PropertyValueFactory<>("ano"));
        colAno.setPrefWidth(80);

        TableColumn<ExercicioFiscal, LocalDate> colInicio = new TableColumn<>("Início");
        colInicio.setCellValueFactory(new PropertyValueFactory<>("dataInicio"));
        colInicio.setCellFactory(col -> dateCell());
        colInicio.setPrefWidth(105);

        TableColumn<ExercicioFiscal, LocalDate> colFim = new TableColumn<>("Fim");
        colFim.setCellValueFactory(new PropertyValueFactory<>("dataFim"));
        colFim.setCellFactory(col -> dateCell());
        colFim.setPrefWidth(105);

        TableColumn<ExercicioFiscal, String> colEstado = new TableColumn<>("Estado");
        colEstado.setCellValueFactory(cell ->
                new SimpleStringProperty(
                        cell.getValue().getEstado() == null
                                ? "—"
                                : cell.getValue().getEstado().toString()
                )
        );
        colEstado.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                Label badge = new Label(item);
                badge.getStyleClass().add("exercicio-status-badge");

                if ("Aberto".equals(item)) {
                    badge.getStyleClass().add("status-open");
                } else if ("Em Encerramento".equals(item)) {
                    badge.getStyleClass().add("status-closing");
                } else if ("Fechado".equals(item)) {
                    badge.getStyleClass().add("status-closed");
                } else {
                    badge.getStyleClass().add("status-future");
                }

                setGraphic(badge);
                setText(null);
            }
        });
        colEstado.setPrefWidth(155);

        TableColumn<ExercicioFiscal, String> colActual = new TableColumn<>("Actual");
        colActual.setCellValueFactory(cell -> {
            ExercicioFiscal ex = cell.getValue();
            boolean actual = ex.getEmpresa() != null
                    && Objects.equals(ex.getEmpresa().getExercicioActual(), ex.getAno());
            return new SimpleStringProperty(actual ? "SIM" : "");
        });
        colActual.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.isBlank()) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label badge = new Label("●  ACTUAL");
                    badge.getStyleClass().add("exercicio-actual-badge");
                    setGraphic(badge);
                    setText(null);
                }
            }
        });
        colActual.setPrefWidth(105);

        TableColumn<ExercicioFiscal, String> colObs = new TableColumn<>("Observações");
        colObs.setCellValueFactory(cell ->
                new SimpleStringProperty(
                        cell.getValue().getObservacoes() == null
                                ? ""
                                : cell.getValue().getObservacoes()
                )
        );
        colObs.setCellFactory(tc -> TextTableCell.create());
        colObs.setOnEditCommit(event -> {
            ExercicioFiscal ef = event.getRowValue();
            ef.setObservacoes(event.getNewValue());
            saveExercicioInline(ef);
        });
        colObs.setPrefWidth(260);

        table.getColumns().addAll(
                colEmpresa, colAno, colInicio, colFim, colEstado, colActual, colObs
        );

        table.setOnViewDetails(this::showDetails);
        table.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldValue, newValue) -> showDetails(newValue));

        table.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && table.getSelectionModel().getSelectedItem() != null) {
                showDetails(table.getSelectionModel().getSelectedItem());
            }
        });

        return table;
    }

    private TableCell<ExercicioFiscal, LocalDate> dateCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : DATE_FORMAT.format(item));
            }
        };
    }

    private VBox buildDetails() {
        VBox root = new VBox(12);
        root.getStyleClass().add("empresa-details");
        root.setPadding(new Insets(16));
        root.setMinWidth(310);

        HBox heading = new HBox(9);
        heading.setAlignment(Pos.CENTER_LEFT);

        Label icon = new Label("", IconUtils.icon(Feather.CALENDAR, 18));
        detailTitle = new Label("Nenhum exercício seleccionado");
        detailTitle.getStyleClass().add("h3");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        detailStatus = new Label("—");
        detailStatus.getStyleClass().add("empresa-status");

        heading.getChildren().addAll(icon, detailTitle, spacer, detailStatus);

        Separator separator = new Separator();

        VBox facts = new VBox(8);
        facts.getStyleClass().add("empresa-details-facts");

        detailEmpresa = fact();
        detailAno = fact();
        detailPeriodo = fact();
        detailActual = fact();
        detailEncerrado = fact();

        facts.getChildren().addAll(
                factRow("Empresa", detailEmpresa),
                factRow("Ano fiscal", detailAno),
                factRow("Período", detailPeriodo),
                factRow("Exercício actual", detailActual),
                factRow("Encerrado em", detailEncerrado)
        );

        Label actionsTitle = new Label("Operações do exercício");
        actionsTitle.getStyleClass().add("empresa-details-section");

        VBox actions = new VBox(7);

        btnActual = actionButton(
                "Definir como exercício actual",
                Feather.CHECK_CIRCLE,
                "button-primary",
                e -> definirActual()
        );
        btnEncerramento = actionButton(
                "Iniciar encerramento",
                Feather.ARROW_RIGHT_CIRCLE,
                "button-outlined",
                e -> iniciarEncerramento()
        );
        btnFechar = actionButton(
                "Fechar exercício",
                Feather.LOCK,
                "button-outlined-danger",
                e -> fecharExercicio()
        );
        btnReabrir = actionButton(
                "Reabrir exercício",
                Feather.UNLOCK,
                "button-outlined",
                e -> reabrirExercicio()
        );

        actions.getChildren().addAll(
                btnActual, btnEncerramento, btnFechar, btnReabrir
        );

        Label help = new Label(
                "O encerramento é sequencial: Aberto → Em Encerramento → Fechado."
        );
        help.setWrapText(true);
        help.getStyleClass().add("exercicios-details-help");

        Region bottomSpacer = new Region();
        VBox.setVgrow(bottomSpacer, Priority.ALWAYS);

        root.getChildren().addAll(
                heading, separator, facts,
                actionsTitle, actions, bottomSpacer, help
        );
        clearDetails();
        return root;
    }

    private Label fact() {
        Label label = new Label("—");
        label.getStyleClass().add("empresa-fact-value");
        label.setWrapText(true);
        return label;
    }

    private HBox factRow(String caption, Label value) {
        Label title = new Label(caption);
        title.getStyleClass().add("empresa-fact-label");
        title.setMinWidth(95);

        HBox row = new HBox(8, title, value);
        HBox.setHgrow(value, Priority.ALWAYS);
        row.setAlignment(Pos.TOP_LEFT);
        return row;
    }



    private Button actionButton(String text,
                                Feather icon,
                                String style,
                                javafx.event.EventHandler<javafx.event.ActionEvent> handler) {
        Button button = new Button(text, IconUtils.icon(icon, IconUtils.SIZE_SMALL));
        button.setMaxWidth(Double.MAX_VALUE);
        button.setAlignment(Pos.CENTER_LEFT);
        button.getStyleClass().add(style);
        button.setOnAction(handler);
        return button;
    }

    private void loadData() {
        table.setLoading(true);

        persistenceService.executeAsync(() -> {
            try {
                List<Empresa> empresasResult = empresaRepository.findAll();
                List<ExercicioFiscal> exerciciosResult =
                        exercicioService.listarTodosComEmpresa();

                Platform.runLater(() -> {
                    empresas.setAll(empresasResult);
                    exercicios.setAll(exerciciosResult);

                    if (empresaCombo.getValue() != null) {
                        Long selectedId = empresaCombo.getValue().getId();
                        empresas.stream()
                                .filter(e -> Objects.equals(e.getId(), selectedId))
                                .findFirst()
                                .ifPresent(empresaCombo.getSelectionModel()::select);
                    }

                    applyFilters();
                    table.setLoading(false);
                    updateKpis();
                    showDetails(table.getSelectionModel().getSelectedItem());
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    table.setLoading(false);
                    modalManager.alert(
                            "Erro ao carregar exercícios",
                            "Não foi possível carregar os exercícios fiscais: "
                                    + rootMessage(e),
                            "error",
                            e
                    );
                });
            }
        }, "READ", "EXERCICIO", "Carregamento dos exercícios fiscais", null);
    }

    private void applyFilters() {
        if (exerciciosFiltrados == null) {
            return;
        }

        Empresa selectedEmpresa = empresaCombo == null ? null : empresaCombo.getValue();
        String estado = estadoCombo == null ? "Todos" : estadoCombo.getValue();
        String query = searchField == null || searchField.getText() == null
                ? ""
                : searchField.getText().trim().toLowerCase(Locale.ROOT);

        exerciciosFiltrados.setPredicate(exercicio -> {
            boolean empresaOk = selectedEmpresa == null
                    || (exercicio.getEmpresa() != null
                    && Objects.equals(exercicio.getEmpresa().getId(), selectedEmpresa.getId()));

            boolean estadoOk = estado == null
                    || "Todos".equals(estado)
                    || (exercicio.getEstado() != null
                    && estado.equals(exercicio.getEstado().toString()));

            String nomeEmpresa = exercicio.getEmpresa() == null
                    ? ""
                    : value(exercicio.getEmpresa().getNome());
            String estadoExercicio = exercicio.getEstado() == null
                    ? ""
                    : exercicio.getEstado().toString();
            String observacoes = exercicio.getObservacoes() == null
                    ? ""
                    : exercicio.getObservacoes();

            String searchable = String.join(" ",
                    nomeEmpresa,
                    value(exercicio.getAno()),
                    formatDate(exercicio.getDataInicio()),
                    formatDate(exercicio.getDataFim()),
                    estadoExercicio,
                    observacoes
            ).toLowerCase(Locale.ROOT);

            boolean queryOk = query.isBlank() || searchable.contains(query);
            return empresaOk && estadoOk && queryOk;
        });

        updateKpis();
        showDetails(table == null ? null : table.getSelectionModel().getSelectedItem());
    }



    private void updateKpis() {
        long total = exerciciosFiltrados == null ? 0 : exerciciosFiltrados.size();
        long abertos = count(EstadoExercicio.ABERTO);
        long encerramento = count(EstadoExercicio.ENCERRAMENTO);
        long fechados = count(EstadoExercicio.FECHADO);

        totalLabel.setText(String.valueOf(total));
        abertosLabel.setText(String.valueOf(abertos));
        encerramentoLabel.setText(String.valueOf(encerramento));
        fechadosLabel.setText(String.valueOf(fechados));
    }

    private long count(EstadoExercicio target) {
        if (exerciciosFiltrados == null) {
            return 0;
        }

        return exerciciosFiltrados.stream()
                .filter(e -> e.getEstado() == target)
                .count();
    }

    private void showDetails(ExercicioFiscal exercicio) {
        if (exercicio == null) {
            clearDetails();
            return;
        }

        String empresaNome = exercicio.getEmpresa() == null
                ? "—"
                : value(exercicio.getEmpresa().getNome());

        boolean actual = exercicio.getEmpresa() != null
                && Objects.equals(
                exercicio.getEmpresa().getExercicioActual(),
                exercicio.getAno()
        );

        detailTitle.setText("Exercício " + value(exercicio.getAno()));

        String state = exercicio.getEstado() == null
                ? "—"
                : exercicio.getEstado().toString();
        detailStatus.setText(state.equals("—") ? "—" : state.toUpperCase(Locale.ROOT));
        detailStatus.getStyleClass().removeAll("active", "inactive", "closing");
        if (exercicio.getEstado() == EstadoExercicio.ABERTO) {
            detailStatus.getStyleClass().add("active");
        } else if (exercicio.getEstado() == EstadoExercicio.ENCERRAMENTO) {
            detailStatus.getStyleClass().add("closing");
        } else if (exercicio.getEstado() == EstadoExercicio.FECHADO) {
            detailStatus.getStyleClass().add("inactive");
        }

        detailEmpresa.setText(empresaNome);
        detailAno.setText(value(exercicio.getAno()));
        detailPeriodo.setText(
                formatDate(exercicio.getDataInicio())
                        + " — "
                        + formatDate(exercicio.getDataFim())
        );
        detailActual.setText(actual ? "Sim" : "Não");
        detailEncerrado.setText(
                exercicio.getEncerradoEm() == null
                        ? "—"
                        : DATE_FORMAT.format(exercicio.getEncerradoEm().toLocalDate())
                                + " por " + value(exercicio.getEncerradoPor())
        );

        btnActual.setDisable(actual || exercicio.getEstado() == EstadoExercicio.FECHADO);
        btnEncerramento.setDisable(exercicio.getEstado() != EstadoExercicio.ABERTO);
        btnFechar.setDisable(exercicio.getEstado() != EstadoExercicio.ENCERRAMENTO);
        btnReabrir.setDisable(exercicio.getEstado() != EstadoExercicio.FECHADO);
    }

    private void clearDetails() {
        if (detailTitle != null) detailTitle.setText("Nenhum exercício seleccionado");
        if (detailStatus != null) {
            detailStatus.setText("—");
            detailStatus.getStyleClass().removeAll("active", "inactive", "closing");
        }
        if (detailEmpresa != null) detailEmpresa.setText("—");
        if (detailAno != null) detailAno.setText("—");
        if (detailPeriodo != null) detailPeriodo.setText("—");
        if (detailActual != null) detailActual.setText("—");
        if (detailEncerrado != null) detailEncerrado.setText("—");

        if (btnActual != null) btnActual.setDisable(true);
        if (btnEncerramento != null) btnEncerramento.setDisable(true);
        if (btnFechar != null) btnFechar.setDisable(true);
        if (btnReabrir != null) btnReabrir.setDisable(true);
    }



    /**
     * Assistente guiado para a preparação segura de um exercício fiscal.
     * Etapas: empresa/base, módulos de séries, revisão e finalização.
     */
    public void showNovoExercicioDialog() {
        List<Empresa> empresasActivas = empresas.stream()
                .filter(Empresa::getAtiva)
                .toList();

        if (empresasActivas.isEmpty()) {
            loadData();
            modalManager.alert(
                    "Empresa activa necessária",
                    "Crie ou active uma empresa antes de abrir um exercício fiscal.",
                    "warning",
                    null
            );
            return;
        }

        VBox root = new VBox(14);
        root.getStyleClass().add("exercicio-wizard");
        root.setPadding(new Insets(4));
        root.setPrefWidth(790);
        root.setMinWidth(620);

        Label stepTitle = new Label("Etapa 1 de 3 · Empresa e exercício-base");
        stepTitle.getStyleClass().add("exercicio-wizard-step-title");

        ProgressBar progress = new ProgressBar(1.0 / 3.0);
        progress.setMaxWidth(Double.MAX_VALUE);
        progress.getStyleClass().add("exercicio-wizard-progress");

        StackPane pageHost = new StackPane();
        pageHost.setMinHeight(300);
        VBox.setVgrow(pageHost, Priority.ALWAYS);

        // Etapa 1 — identificação do período e validação da base.
        VBox page1 = new VBox(14);
        page1.getStyleClass().add("exercicio-wizard-page");

        Label intro = new Label(
                "Escolha a empresa e o ano destino. O Kubata exige que a base seja "
                        + "o ano imediatamente anterior; não permite usar um exercício futuro como origem."
        );
        intro.setWrapText(true);
        intro.getStyleClass().add("exercicios-modal-intro");

        ComboBox<Empresa> empresaField =
                new ComboBox<>(FXCollections.observableArrayList(empresasActivas));
        empresaField.setPromptText("Seleccione a empresa");
        empresaField.setMaxWidth(Double.MAX_VALUE);
        empresaField.setPrefWidth(400);

        Spinner<Integer> anoDestinoField = new Spinner<>();
        anoDestinoField.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(
                2001, 2100, LocalDate.now().getYear()
        ));
        anoDestinoField.setEditable(true);
        anoDestinoField.setPrefWidth(150);

        ComboBox<Integer> anoBaseField = new ComboBox<>();
        anoBaseField.setPromptText("Seleccione o exercício anterior");
        anoBaseField.setPrefWidth(220);
        anoBaseField.setMaxWidth(Double.MAX_VALUE);

        Label dataPeriodo = new Label("Período destino: —");
        dataPeriodo.getStyleClass().add("exercicio-wizard-period");

        Label baseHint = new Label();
        baseHint.setWrapText(true);
        baseHint.getStyleClass().add("exercicios-modal-note");

        GridPane fields = new GridPane();
        fields.setHgap(14);
        fields.setVgap(12);
        ColumnConstraints labelColumn = new ColumnConstraints();
        labelColumn.setMinWidth(145);
        ColumnConstraints inputColumn = new ColumnConstraints();
        inputColumn.setHgrow(Priority.ALWAYS);
        fields.getColumnConstraints().addAll(labelColumn, inputColumn);
        fields.add(label("Empresa *"), 0, 0);
        fields.add(empresaField, 1, 0);
        fields.add(label("Ano destino *"), 0, 1);
        fields.add(anoDestinoField, 1, 1);
        fields.add(label("Ano-base *"), 0, 2);
        fields.add(anoBaseField, 1, 2);
        fields.add(dataPeriodo, 1, 3);
        fields.add(baseHint, 1, 4);

        Region page1Spacer = new Region();
        VBox.setVgrow(page1Spacer, Priority.ALWAYS);
        page1.getChildren().addAll(intro, fields, page1Spacer);

        // Etapa 2 — grupos com configuração documental que pode ser reiniciada com segurança.
        VBox page2 = new VBox(12);
        page2.getStyleClass().add("exercicio-wizard-page");

        Label moduleIntro = new Label(
                "Seleccione as áreas cujas séries documentais devem ser preparadas a partir da base. "
                        + "A numeração começa novamente no destino e o estado de registo AGT é reiniciado."
        );
        moduleIntro.setWrapText(true);
        moduleIntro.getStyleClass().add("exercicios-modal-intro");

        GridPane moduleGrid = new GridPane();
        moduleGrid.setHgap(10);
        moduleGrid.setVgap(10);

        Map<String, String> moduleLabels = new LinkedHashMap<>();
        moduleLabels.put("VENDAS", "Vendas");
        moduleLabels.put("COMPRAS", "Compras");
        moduleLabels.put("INVENTARIO", "Inventário / Stock");
        moduleLabels.put("FINANCEIRO", "Financeiro");

        Map<String, String> moduleAreas = new LinkedHashMap<>();
        moduleAreas.put("VENDAS", "Vendas");
        moduleAreas.put("COMPRAS", "Compras");
        moduleAreas.put("INVENTARIO", "Stock");
        moduleAreas.put("FINANCEIRO", "Financeiro");

        Map<String, CheckBox> moduleChecks = new LinkedHashMap<>();
        Map<String, Label> moduleCounts = new LinkedHashMap<>();

        int position = 0;
        for (Map.Entry<String, String> entry : moduleLabels.entrySet()) {
            VBox card = new VBox(7);
            card.getStyleClass().add("exercicio-wizard-module");
            card.setPadding(new Insets(12));
            card.setMinHeight(100);

            CheckBox check = new CheckBox(entry.getValue());
            check.getStyleClass().add("exercicio-wizard-module-check");

            Label count = new Label("A verificar exercício-base…");
            count.setWrapText(true);
            count.getStyleClass().add("exercicio-wizard-module-count");

            Label explanation = new Label(
                    "Só serão copiadas séries e configurações de numeração. "
                            + "Os documentos e movimentos permanecem intactos."
            );
            explanation.setWrapText(true);
            explanation.getStyleClass().add("exercicio-wizard-module-description");

            card.getChildren().addAll(check, count, explanation);
            moduleChecks.put(entry.getKey(), check);
            moduleCounts.put(entry.getKey(), count);
            moduleGrid.add(card, position % 2, position / 2);
            GridPane.setHgrow(card, Priority.ALWAYS);
            position++;
        }
        ColumnConstraints moduleCol1 = new ColumnConstraints();
        moduleCol1.setPercentWidth(50);
        moduleCol1.setHgrow(Priority.ALWAYS);
        ColumnConstraints moduleCol2 = new ColumnConstraints();
        moduleCol2.setPercentWidth(50);
        moduleCol2.setHgrow(Priority.ALWAYS);
        moduleGrid.getColumnConstraints().addAll(moduleCol1, moduleCol2);

        Label moduleWarning = new Label(
                "A criação do exercício não duplica facturas, recibos, movimentos de stock, "
                        + "lançamentos contabilísticos, saldos ou dados de clientes/fornecedores."
        );
        moduleWarning.setWrapText(true);
        moduleWarning.getStyleClass().add("exercicio-wizard-warning");
        page2.getChildren().addAll(moduleIntro, moduleGrid, moduleWarning);

        // Etapa 3 — revisão dos parâmetros, aviso da base e escolha do exercício actual.
        VBox page3 = new VBox(14);
        page3.getStyleClass().add("exercicio-wizard-page");

        Label reviewIntro = new Label(
                "Revise a configuração antes de finalizar. A criação e a preparação das séries "
                        + "decorrem na mesma transacção; se uma etapa falhar, a operação é revertida."
        );
        reviewIntro.setWrapText(true);
        reviewIntro.getStyleClass().add("exercicios-modal-intro");

        VBox reviewFacts = new VBox(9);
        reviewFacts.getStyleClass().add("exercicio-wizard-review");

        Label reviewCompany = new Label("Empresa: —");
        Label reviewYear = new Label("Novo exercício: —");
        Label reviewBase = new Label("Exercício-base: —");
        Label reviewModules = new Label("Áreas seleccionadas: —");
        Label reviewSeries = new Label("Séries disponíveis para preparar: —");
        for (Label fact : List.of(reviewCompany, reviewYear, reviewBase, reviewModules, reviewSeries)) {
            fact.setWrapText(true);
            fact.getStyleClass().add("exercicio-wizard-review-line");
        }
        reviewFacts.getChildren().addAll(
                reviewCompany, reviewYear, reviewBase, reviewModules, reviewSeries
        );

        CheckBox makeCurrent = new CheckBox("Definir o novo exercício como exercício actual da empresa");
        makeCurrent.getStyleClass().add("exercicio-wizard-current-check");

        Label finalWarning = new Label(
                "Nota de integridade: apenas a configuração das séries documentais será replicada. "
                        + "Os documentos e movimentos do exercício anterior não são copiados."
        );
        finalWarning.setWrapText(true);
        finalWarning.getStyleClass().add("exercicio-wizard-warning");

        Label operationStatus = new Label();
        operationStatus.setWrapText(true);
        operationStatus.getStyleClass().add("text-muted");

        ProgressIndicator busy = new ProgressIndicator();
        busy.setMaxSize(26, 26);
        busy.setVisible(false);
        busy.setManaged(false);

        page3.getChildren().addAll(reviewIntro, reviewFacts, makeCurrent, finalWarning, operationStatus, busy);

        HBox navigation = new HBox(10);
        navigation.setAlignment(Pos.CENTER_RIGHT);
        Button back = new Button("Anterior", IconUtils.icon(Feather.ARROW_LEFT, IconUtils.SIZE_SMALL));
        back.getStyleClass().add("button-outlined");
        Button cancel = new Button("Cancelar", IconUtils.icon(Feather.X, IconUtils.SIZE_SMALL));
        cancel.getStyleClass().add("button-outlined");
        Button next = new Button("Seguinte", IconUtils.icon(Feather.ARROW_RIGHT, IconUtils.SIZE_SMALL));
        next.getStyleClass().add("button-primary");

        Region navSpacer = new Region();
        HBox.setHgrow(navSpacer, Priority.ALWAYS);
        navigation.getChildren().addAll(navSpacer, back, cancel, next);

        root.getChildren().addAll(stepTitle, progress, pageHost, navigation);

        // Estado inicial: empresa activa já seleccionada, preferencialmente a filtrada na grelha principal.
        Empresa initialCompany = empresaCombo == null ? null : empresaCombo.getValue();
        if (initialCompany != null && initialCompany.getAtiva()
                && empresasActivas.stream().anyMatch(e -> Objects.equals(e.getId(), initialCompany.getId()))) {
            empresaField.setValue(empresasActivas.stream()
                    .filter(e -> Objects.equals(e.getId(), initialCompany.getId()))
                    .findFirst().orElse(null));
        } else {
            empresaField.setValue(empresasActivas.get(0));
        }

        final int[] currentStep = {0};

        Runnable updateBaseSelection = () -> {
            Empresa selectedCompany = empresaField.getValue();
            Integer target = anoDestinoField.getValue();
            List<Integer> availableYears = selectedCompany == null ? List.of()
                    : exercicios.stream()
                    .filter(ex -> ex.getEmpresa() != null
                            && Objects.equals(ex.getEmpresa().getId(), selectedCompany.getId())
                            && ex.getAno() != null
                            && ex.getEstado() != EstadoExercicio.FUTURO)
                    .map(ExercicioFiscal::getAno)
                    .distinct()
                    .sorted(java.util.Comparator.reverseOrder())
                    .toList();

            anoBaseField.getItems().setAll(availableYears);
            Integer requiredBase = target == null ? null : target - 1;
            if (requiredBase != null && availableYears.contains(requiredBase)) {
                anoBaseField.setValue(requiredBase);
            } else {
                anoBaseField.setValue(null);
            }

            if (target != null) {
                dataPeriodo.setText(
                        "Período destino: 01/01/" + target + " — 31/12/" + target
                );
            } else {
                dataPeriodo.setText("Período destino: —");
            }

            if (selectedCompany == null) {
                baseHint.setText("Seleccione uma empresa activa.");
            } else if (target == null) {
                baseHint.setText("Informe um ano destino válido.");
            } else if (target <= 2000) {
                baseHint.setText("Para usar o assistente, o ano destino deve ser 2001 ou posterior.");
            } else if (exercicios.stream().anyMatch(ex -> ex.getEmpresa() != null
                    && Objects.equals(ex.getEmpresa().getId(), selectedCompany.getId())
                    && Objects.equals(ex.getAno(), target))) {
                baseHint.setText("Este exercício já existe. Escolha outro ano ou abra o registo existente.");
            } else if (requiredBase == null || !availableYears.contains(requiredBase)) {
                baseHint.setText(
                        "Não existe o exercício-base " + requiredBase + " para esta empresa. "
                                + "Crie/importe esse ano primeiro; não utilize um ano futuro como base."
                );
            } else {
                ExercicioFiscal source = exercicios.stream()
                        .filter(ex -> ex.getEmpresa() != null
                                && Objects.equals(ex.getEmpresa().getId(), selectedCompany.getId())
                                && Objects.equals(ex.getAno(), requiredBase))
                        .findFirst().orElse(null);
                String status = source == null || source.getEstado() == null
                        ? "estado não disponível"
                        : source.getEstado().toString();
                baseHint.setText(
                        "Base válida: exercício " + requiredBase + " (" + status + "). "
                                + (source != null && source.getEstado() != EstadoExercicio.FECHADO
                                ? "Recomendação: confirme as operações e saldos antes de continuar."
                                : "A base é o ano imediatamente anterior.")
                );
            }
        };

        Runnable refreshModuleAvailability = () -> {
            Empresa selectedCompany = empresaField.getValue();
            Integer baseYear = anoBaseField.getValue();
            List<SerieDocumento> allSeries = selectedCompany == null
                    ? List.of()
                    : serieDocumentoService.listarPorEmpresa(selectedCompany.getId());

            for (Map.Entry<String, String> entry : moduleAreas.entrySet()) {
                String code = entry.getKey();
                String area = entry.getValue();
                CheckBox check = moduleChecks.get(code);
                Label countLabel = moduleCounts.get(code);
                long count = allSeries.stream()
                        .filter(serie -> Objects.equals(serie.getExercicio(), baseYear))
                        .filter(serie -> serie.getTipoDocumento() != null
                                && area.equalsIgnoreCase(serie.getTipoDocumento().getArea()))
                        .count();

                boolean available = baseYear != null && count > 0;
                check.setDisable(!available);
                check.setSelected(available);
                countLabel.setText(available
                        ? count + (count == 1 ? " série no exercício-base" : " séries no exercício-base")
                        : baseYear == null
                        ? "Seleccione um exercício-base para verificar a disponibilidade."
                        : "Sem séries de " + moduleLabels.get(code) + " no exercício-base.");
            }
        };

        Runnable updateReview = () -> {
            Empresa selectedCompany = empresaField.getValue();
            Integer target = anoDestinoField.getValue();
            Integer baseYear = anoBaseField.getValue();
            Set<String> selectedModules = moduleChecks.entrySet().stream()
                    .filter(entry -> entry.getValue().isSelected() && !entry.getValue().isDisabled())
                    .map(Map.Entry::getKey)
                    .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

            reviewCompany.setText("Empresa: " + (selectedCompany == null ? "—" : selectedCompany.getNome()));
            reviewYear.setText("Novo exercício: " + (target == null ? "—" : target)
                    + (target == null ? "" : " · 01/01/" + target + " a 31/12/" + target));
            reviewBase.setText("Exercício-base: " + (baseYear == null ? "—" : baseYear));
            reviewModules.setText("Áreas seleccionadas: " + (selectedModules.isEmpty()
                    ? "nenhuma (criar apenas o período fiscal)"
                    : selectedModules.stream().map(moduleLabels::get).collect(java.util.stream.Collectors.joining(", "))));

            long seriesCount = selectedCompany == null || baseYear == null ? 0
                    : serieDocumentoService.listarPorEmpresa(selectedCompany.getId()).stream()
                    .filter(serie -> Objects.equals(serie.getExercicio(), baseYear))
                    .filter(serie -> serie.getTipoDocumento() != null)
                    .filter(serie -> selectedModules.contains(
                            moduleAreas.entrySet().stream()
                                    .filter(entry -> entry.getValue().equalsIgnoreCase(
                                            serie.getTipoDocumento().getArea()))
                                    .map(Map.Entry::getKey)
                                    .findFirst().orElse("")
                    )).count();
            reviewSeries.setText("Séries a preparar: " + seriesCount);

            makeCurrent.setSelected(target != null
                    && target == LocalDate.now().getYear()
                    && (selectedCompany == null || !Objects.equals(
                            selectedCompany.getExercicioActual(), target)));
        };

        java.util.function.IntConsumer showStep = step -> {
            currentStep[0] = step;
            pageHost.getChildren().setAll(step == 0 ? page1 : step == 1 ? page2 : page3);
            stepTitle.setText(switch (step) {
                case 0 -> "Etapa 1 de 3 · Empresa e exercício-base";
                case 1 -> "Etapa 2 de 3 · Preparação dos módulos";
                default -> "Etapa 3 de 3 · Revisão e finalização";
            });
            progress.setProgress((step + 1) / 3.0);
            back.setDisable(step == 0);
            next.setText(step == 2 ? "Criar exercício" : step == 1 ? "Rever criação" : "Seguinte");
            next.setGraphic(IconUtils.icon(step == 2 ? Feather.CHECK_CIRCLE : Feather.ARROW_RIGHT,
                    IconUtils.SIZE_SMALL));
            if (step == 2) {
                updateReview.run();
            }
        };

        empresaField.valueProperty().addListener((obs, old, value) -> {
            updateBaseSelection.run();
            refreshModuleAvailability.run();
            if (currentStep[0] == 2) updateReview.run();
        });
        anoDestinoField.valueProperty().addListener((obs, old, value) -> {
            updateBaseSelection.run();
            refreshModuleAvailability.run();
            if (currentStep[0] == 2) updateReview.run();
        });
        anoBaseField.valueProperty().addListener((obs, old, value) -> {
            refreshModuleAvailability.run();
            if (currentStep[0] == 2) updateReview.run();
        });
        moduleChecks.values().forEach(check ->
                check.selectedProperty().addListener((obs, old, value) -> {
                    if (currentStep[0] == 2) updateReview.run();
                }));

        Runnable commitDestinationYear = () -> {
            String editor = anoDestinoField.getEditor().getText();
            int parsed = Integer.parseInt(editor.trim());
            if (parsed < 2001 || parsed > 2100) {
                throw new IllegalArgumentException("O ano destino deve estar entre 2001 e 2100.");
            }
            anoDestinoField.getValueFactory().setValue(parsed);
        };

        Runnable validateStepOne = () -> {
            Empresa company = empresaField.getValue();
            if (company == null || company.getId() == null || !company.getAtiva()) {
                throw new IllegalArgumentException("Seleccione uma empresa activa.");
            }

            commitDestinationYear.run();
            int target = anoDestinoField.getValue();
            int baseYear = target - 1;

            if (exercicios.stream().anyMatch(ex -> ex.getEmpresa() != null
                    && Objects.equals(ex.getEmpresa().getId(), company.getId())
                    && Objects.equals(ex.getAno(), target))) {
                throw new IllegalArgumentException("O exercício " + target + " já existe para esta empresa.");
            }

            if (anoBaseField.getValue() == null || anoBaseField.getValue() != baseYear) {
                throw new IllegalArgumentException(
                        "Seleccione o exercício " + baseYear + " como base. "
                                + "O Kubata não permite criar o destino a partir de um ano futuro."
                );
            }

            boolean sourceExists = exercicios.stream().anyMatch(ex -> ex.getEmpresa() != null
                    && Objects.equals(ex.getEmpresa().getId(), company.getId())
                    && Objects.equals(ex.getAno(), baseYear)
                    && ex.getEstado() != EstadoExercicio.FUTURO);
            if (!sourceExists) {
                throw new IllegalArgumentException(
                        "O exercício-base " + baseYear + " não existe para esta empresa."
                );
            }
        };

        cancel.setOnAction(e -> modalManager.hideModal());
        back.setOnAction(e -> {
            if (currentStep[0] > 0) showStep.accept(currentStep[0] - 1);
        });
        next.setOnAction(e -> {
            try {
                if (currentStep[0] == 0) {
                    validateStepOne.run();
                    refreshModuleAvailability.run();
                    showStep.accept(1);
                    return;
                }
                if (currentStep[0] == 1) {
                    validateStepOne.run();
                    showStep.accept(2);
                    return;
                }

                validateStepOne.run();
                Empresa selectedCompany = empresaField.getValue();
                int target = anoDestinoField.getValue();
                int baseYear = anoBaseField.getValue();
                Set<String> selectedModules = moduleChecks.entrySet().stream()
                        .filter(entry -> entry.getValue().isSelected() && !entry.getValue().isDisabled())
                        .map(Map.Entry::getKey)
                        .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
                boolean setAsCurrent = makeCurrent.isSelected();

                next.setDisable(true);
                back.setDisable(true);
                cancel.setDisable(true);
                next.setText("A preparar…");
                busy.setVisible(true);
                busy.setManaged(true);
                operationStatus.setText("A validar a base e a preparar as séries sem copiar movimentos…");

                String description = "Criar exercício fiscal " + target + " para "
                        + selectedCompany.getNome() + " com base em " + baseYear;
                Task<ExercicioFiscalService.ResultadoCriacao> task = new Task<>() {
                    @Override
                    protected ExercicioFiscalService.ResultadoCriacao call() {
                        return exercicioService.criarExercicioAssistido(
                                selectedCompany, baseYear, target, selectedModules, setAsCurrent
                        );
                    }
                };

                task.setOnSucceeded(event -> {
                    ExercicioFiscalService.ResultadoCriacao result = task.getValue();
                    try {
                        acessoService.registrarAuditoria(
                                sessionManager.getUser(), "CREATE", "EXERCICIO", "127.0.0.1",
                                description + "; séries criadas: " + result.seriesCriadas(), true
                        );
                    } catch (Exception auditError) {
                        // O registo fiscal foi confirmado; uma falha auxiliar de auditoria
                        // não deve ocultar o resultado nem repetir a operação.
                    }

                    modalManager.hideModal();
                    loadData();

                    String message = "Exercício " + result.exercicio().getAno()
                            + " criado com base em " + result.anoBase() + ".\n"
                            + "Séries documentais preparadas: " + result.seriesCriadas() + ".\n"
                            + (result.definidoComoActual()
                                ? "O novo período foi definido como exercício actual.\n"
                                : "O exercício actual da empresa foi mantido.\n")
                            + "Documentos, movimentos, saldos e existências não foram copiados.";
                    if (!result.avisos().isEmpty()) {
                        message += "\n\nAvisos:\n• " + String.join("\n• ", result.avisos());
                    }
                    modalManager.alert("Exercício criado", message, "success", null);
                });

                task.setOnFailed(event -> {
                    Throwable ex = task.getException();
                    next.setDisable(false);
                    back.setDisable(false);
                    cancel.setDisable(false);
                    next.setText("Criar exercício");
                    next.setGraphic(IconUtils.icon(Feather.CHECK_CIRCLE, IconUtils.SIZE_SMALL));
                    busy.setVisible(false);
                    busy.setManaged(false);
                    operationStatus.setText(
                            "A operação não foi concluída. Nenhum exercício parcialmente preparado deve ser utilizado."
                    );
                    try {
                        acessoService.registrarAuditoria(
                                sessionManager.getUser(), "CREATE_FAILED", "EXERCICIO", "127.0.0.1",
                                description + "; erro: " + rootMessage(ex), false
                        );
                    } catch (Exception ignored) {
                        // Mantém o erro funcional como prioridade.
                    }
                    modalManager.alert(
                            "Não foi possível criar o exercício",
                            rootMessage(ex),
                            "error",
                            ex
                    );
                });

                Thread worker = new Thread(task, "kubata-exercicio-fiscal-wizard");
                worker.setDaemon(true);
                worker.start();
            } catch (Exception ex) {
                modalManager.alert(
                        "Verifique os dados do assistente",
                        rootMessage(ex),
                        "warning",
                        null
                );
            }
        });

        updateBaseSelection.run();
        refreshModuleAvailability.run();
        showStep.accept(0);

        modalManager.showModal(
                root,
                new ModalManager.ModalConfig()
                        .title("Assistente de Exercícios Fiscais")
                        .subtitle("Criação guiada com base no exercício anterior")
                        .icon(Feather.CALENDAR)
                        .tone(ModalManager.ModalTone.DEFAULT)
                        .size(850, 620)
                        .minSize(720, 520)
                        .resizable(true)
                        .scrollable(true)
                        .closeOnOverlayClick(false)
                        .closeOnEscape(false)
                        .windowControls(false)
                        .minimizable(false)
                        .maximizable(false)
                        .footerHint("Por segurança, os movimentos e saldos do exercício-base nunca são duplicados.")
        );
    }

    private String suggestNextYear(Empresa empresa) {
        if (empresa == null) {
            return String.valueOf(LocalDate.now().getYear());
        }

        return exercicios.stream()
                .filter(e -> e.getEmpresa() != null
                        && Objects.equals(e.getEmpresa().getId(), empresa.getId())
                        && e.getAno() != null)
                .mapToInt(ExercicioFiscal::getAno)
                .max()
                .stream()
                .mapToObj(max -> String.valueOf(max + 1))
                .findFirst()
                .orElse(String.valueOf(
                        empresa.getExercicioActual() == null
                                ? LocalDate.now().getYear()
                                : empresa.getExercicioActual() + 1
                ));
    }

    private void definirActual() {
        ExercicioFiscal selected = selected();
        if (selected == null) {
            return;
        }

        confirm(
                "Definir exercício actual",
                "Definir o exercício " + selected.getAno()
                        + " da empresa " + selected.getEmpresa().getNome()
                        + " como exercício actual?",
                "Definir actual",
                () -> runOperation(
                        "Definir exercício actual",
                        () -> exercicioService.definirComoActual(selected.getId())
                )
        );
    }

    private void iniciarEncerramento() {
        ExercicioFiscal selected = selected();
        if (selected == null) {
            return;
        }

        confirm(
                "Iniciar encerramento",
                "O exercício " + selected.getAno()
                        + " passará de Aberto para Em Encerramento.",
                "Iniciar encerramento",
                () -> runOperation(
                        "Iniciar encerramento",
                        () -> exercicioService.iniciarEncerramento(selected.getId())
                )
        );
    }

    private void fecharExercicio() {
        ExercicioFiscal selected = selected();
        if (selected == null) {
            return;
        }

        String user = sessionManager.getUser() == null
                ? "Sistema"
                : sessionManager.getUser().getNome();

        confirm(
                "Fechar exercício",
                "O exercício " + selected.getAno()
                        + " será marcado como Fechado. Esta operação deve ser usada apenas depois de concluir o encerramento.",
                "Fechar exercício",
                () -> runOperation(
                        "Fechar exercício",
                        () -> exercicioService.fecharExercicio(selected.getId(), user)
                )
        );
    }

    private void reabrirExercicio() {
        ExercicioFiscal selected = selected();
        if (selected == null) {
            return;
        }

        confirm(
                "Reabrir exercício",
                "O exercício " + selected.getAno()
                        + " voltará para Aberto e o registo de encerramento será limpo.",
                "Reabrir",
                () -> runOperation(
                        "Reabrir exercício",
                        () -> exercicioService.reabrirExercicio(selected.getId())
                )
        );
    }

    private void confirm(String title,
                         String message,
                         String confirmText,
                         Runnable action) {
        VBox content = new VBox(10);
        content.setPadding(new Insets(4));
        Label messageLabel = new Label(message);
        messageLabel.setWrapText(true);
        messageLabel.getStyleClass().add("exercicios-modal-message");
        content.getChildren().add(messageLabel);

        modalManager.showModal(
                content,
                new ModalManager.ModalConfig()
                        .title(title)
                        .icon(Feather.ALERT_TRIANGLE)
                        .tone(ModalManager.ModalTone.WARNING)
                        .size(500, 260)
                        .resizable(false)
                        .withConfirmButtons(confirmText, "Cancelar")
                        .confirmStyle("button-primary")
                        .cancelStyle("button-outlined")
                        .onConfirm(action)
        );
    }
    private void runOperation(String operation, Runnable action) {
        persistenceService.executeAsync(
                action,
                "UPDATE",
                "EXERCICIO",
                operation,
                this::loadData
        );
    }

    private void saveExercicioInline(ExercicioFiscal ef) {
        persistenceService.saveAsync(
                exercicioRepository,
                ef,
                "EXERCICIO",
                "Atualização das observações do exercício: " + ef.getAno(),
                saved -> loadData()
        );
    }

    private ExercicioFiscal selected() {
        ExercicioFiscal selected = table == null
                ? null
                : table.getSelectionModel().getSelectedItem();

        if (selected == null) {
            modalManager.alert(
                    "Nenhum exercício seleccionado",
                    "Seleccione primeiro o exercício que pretende operar.",
                    "info",
                    null
            );
        }

        return selected;
    }

    private Label label(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("exercicios-modal-label");
        return label;
    }

    private String formatDate(LocalDate date) {
        return date == null ? "—" : DATE_FORMAT.format(date);
    }

    private String value(Object value) {
        return value == null || value.toString().isBlank() ? "—" : value.toString();
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null
                && current.getCause() != current) {
            current = current.getCause();
        }
        return current.getMessage() == null
                ? throwable.getMessage()
                : current.getMessage();
    }
}

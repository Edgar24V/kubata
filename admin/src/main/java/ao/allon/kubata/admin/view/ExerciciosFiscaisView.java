package ao.allon.kubata.admin.view;

import ao.allon.kubata.admin.service.PersistenceService;
import ao.allon.kubata.admin.service.SessionManager;
import ao.allon.kubata.admin.ui.modal.ModalManager;
import ao.allon.kubata.admin.ui.util.IconUtils;
import ao.allon.kubata.core.domain.Empresa;
import ao.allon.kubata.core.domain.ExercicioFiscal;
import ao.allon.kubata.core.domain.ExercicioFiscal.EstadoExercicio;
import ao.allon.kubata.core.repository.EmpresaRepository;
import ao.allon.kubata.core.repository.ExercicioFiscalRepository;
import ao.allon.kubata.core.service.AcessoService;
import ao.allon.kubata.core.service.ExercicioFiscalService;
import ao.allon.kubata.core.ui.table.AdvancedTableView;
import ao.allon.kubata.core.ui.table.TableUtils;
import ao.allon.kubata.core.ui.table.TextTableCell;
import javafx.application.Platform;
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
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Component
public class ExerciciosFiscaisView extends VBox {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ExercicioFiscalService exercicioService;
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
                                ExercicioFiscalRepository exercicioRepository,
                                EmpresaRepository empresaRepository,
                                AcessoService acessoService,
                                SessionManager sessionManager,
                                ModalManager modalManager,
                                PersistenceService persistenceService) {
        this.exercicioService = exercicioService;
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



    public void showNovoExercicioDialog() {
        if (empresas.isEmpty()) {
            loadData();
            modalManager.alert(
                    "Sem empresas",
                    "Não existem empresas carregadas. Crie ou active uma empresa antes de criar um exercício.",
                    "warning",
                    null
            );
            return;
        }

        VBox root = new VBox(12);
        root.setPadding(new Insets(6));
        root.setPrefWidth(620);

        Label intro = new Label(
                "Crie um novo exercício fiscal para uma empresa. "
                        + "O período é criado automaticamente de 1 de Janeiro a 31 de Dezembro."
        );
        intro.setWrapText(true);
        intro.getStyleClass().add("exercicios-modal-intro");

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);

        ComboBox<Empresa> empresaField = new ComboBox<>(FXCollections.observableArrayList(empresas));
        empresaField.setPrefWidth(360);
        empresaField.setPromptText("Seleccione a empresa");

        if (empresaCombo.getValue() != null) {
            empresaField.setValue(empresaCombo.getValue());
        } else {
            empresas.stream()
                    .filter(Empresa::getAtiva)
                    .findFirst()
                    .ifPresent(empresaField::setValue);
        }

        TextField anoField = new TextField();
        anoField.setPrefColumnCount(8);
        anoField.setText(suggestNextYear(empresaField.getValue()));

        empresaField.setOnAction(e -> anoField.setText(suggestNextYear(empresaField.getValue())));

        grid.add(label("Empresa *"), 0, 0);
        grid.add(empresaField, 1, 0);

        grid.add(label("Ano *"), 0, 1);
        grid.add(anoField, 1, 1);

        CheckBox actualCheck = new CheckBox("Definir automaticamente este exercício como actual");
        actualCheck.setSelected(true);
        grid.add(actualCheck, 1, 2);

        Label note = new Label(
                "O ano não pode repetir um exercício já existente para a mesma empresa."
        );
        note.getStyleClass().add("exercicios-modal-note");

        root.getChildren().addAll(intro, grid, note);

        modalManager.showModal(
                root,
                new ModalManager.ModalConfig()
                        .title("Novo exercício fiscal")
                        .subtitle("Criação controlada do período fiscal")
                        .icon(Feather.CALENDAR)
                        .tone(ModalManager.ModalTone.DEFAULT)
                        .size(680, 390)
                        .resizable(false)
                        .closeOnEscape(true)
                        .withConfirmButtons("Criar exercício", "Cancelar")
                        .confirmStyle("button-primary")
                        .cancelStyle("button-outlined")
                        .footerHint("Depois de criado, o exercício pode ser aberto, colocado em encerramento ou fechado no painel lateral.")
                        .onConfirm(() -> {
                            try {
                                Empresa empresa = empresaField.getValue();
                                if (empresa == null) {
                                    throw new IllegalArgumentException("Seleccione a empresa.");
                                }

                                int ano = Integer.parseInt(anoField.getText().trim());
                                if (ano < 2000 || ano > 2100) {
                                    throw new IllegalArgumentException("O ano deve estar entre 2000 e 2100.");
                                }

                                persistenceService.executeAsync(() -> {
                                    ExercicioFiscal created =
                                            exercicioService.criarExercicio(empresa, ano);

                                    if (actualCheck.isSelected()) {
                                        exercicioService.definirComoActual(created.getId());
                                    }
                                }, "CREATE", "EXERCICIO",
                                        "Criação do exercício fiscal " + ano
                                                + " — " + empresa.getNome(),
                                        this::loadData);
                            } catch (Exception ex) {
                                modalManager.alert(
                                        "Dados inválidos",
                                        rootMessage(ex),
                                        "warning",
                                        null
                                );
                            }
                        })
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

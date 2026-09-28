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
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
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
    private Label detailEstado;
    private Label detailActual;
    private Label detailEncerrado;

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

        HBox header = buildHeader();
        HBox kpis = buildKpis();
        HBox filters = buildFilters();

        SplitPane workspace = new SplitPane();
        workspace.getStyleClass().add("exercicios-workspace");
        workspace.setOrientation(Orientation.HORIZONTAL);
        workspace.setDividerPositions(0.76);
        workspace.setMinHeight(0);

        StackPane tablePane = new StackPane(buildTable());
        tablePane.getStyleClass().add("exercicios-table-pane");
        tablePane.setMinWidth(420);

        VBox details = buildDetails();
        details.getStyleClass().add("exercicios-details-pane");

        workspace.getItems().addAll(tablePane, details);
        SplitPane.setResizableWithParent(tablePane, true);
        SplitPane.setResizableWithParent(details, false);

        getChildren().addAll(header, kpis, filters, workspace);
        VBox.setVgrow(workspace, Priority.ALWAYS);
    }

    private HBox buildHeader() {
        HBox header = new HBox(12);
        header.getStyleClass().add("exercicios-header");
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(14, 18, 12, 18));

        Label icon = new Label("", IconUtils.icon(Feather.CALENDAR, 18));
        icon.getStyleClass().add("exercicios-title-icon");

        VBox titles = new VBox(2);
        Label title = new Label("Exercícios Fiscais");
        title.getStyleClass().add("exercicios-title");

        Label subtitle = new Label(
                "Gestão central dos períodos fiscais, abertura, encerramento e exercício actual."
        );
        subtitle.getStyleClass().add("exercicios-subtitle");

        titles.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnNovo = new Button(
                "Novo Exercício",
                IconUtils.icon(Feather.PLUS, IconUtils.SIZE_SMALL)
        );
        btnNovo.getStyleClass().add("button-primary");
        btnNovo.setOnAction(e -> showNovoExercicioDialog());

        Button btnRefresh = new Button(
                "Actualizar",
                IconUtils.icon(Feather.REFRESH_CW, IconUtils.SIZE_SMALL)
        );
        btnRefresh.getStyleClass().add("button-outlined");
        btnRefresh.setOnAction(e -> loadData());

        header.getChildren().addAll(icon, titles, spacer, btnNovo, btnRefresh);
        return header;
    }

    private HBox buildKpis() {
        HBox row = new HBox(10);
        row.getStyleClass().add("exercicios-kpi-row");
        row.setPadding(new Insets(0, 18, 12, 18));

        totalLabel = new Label("0");
        abertosLabel = new Label("0");
        encerramentoLabel = new Label("0");
        fechadosLabel = new Label("0");

        row.getChildren().addAll(
                kpi("Total", "Todos os exercícios carregados", totalLabel, Feather.LAYERS),
                kpi("Abertos", "Operacionais", abertosLabel, Feather.UNLOCK),
                kpi("Em encerramento", "Aguardam fecho", encerramentoLabel, Feather.POWER),
                kpi("Fechados", "Histórico encerrado", fechadosLabel, Feather.LOCK)
        );

        return row;
    }

    private VBox kpi(String title, String caption, Label value, Feather icon) {
        VBox card = new VBox(4);
        card.getStyleClass().add("exercicios-kpi-card");
        HBox.setHgrow(card, Priority.ALWAYS);

        HBox top = new HBox(8);
        top.setAlignment(Pos.CENTER_LEFT);

        Label iconLabel = new Label("", IconUtils.icon(icon, 14));
        iconLabel.getStyleClass().add("exercicios-kpi-icon");

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("exercicios-kpi-title");

        top.getChildren().addAll(iconLabel, titleLabel);

        value.getStyleClass().add("exercicios-kpi-value");
        Label captionLabel = new Label(caption);
        captionLabel.getStyleClass().add("exercicios-kpi-caption");

        card.getChildren().addAll(top, value, captionLabel);
        return card;
    }

    private HBox buildFilters() {
        HBox box = new HBox(10);
        box.getStyleClass().add("exercicios-filters");
        box.setPadding(new Insets(0, 18, 12, 18));
        box.setAlignment(Pos.CENTER_LEFT);

        Label companyLabel = new Label("Empresa");
        companyLabel.getStyleClass().add("exercicios-filter-label");

        empresaCombo = new ComboBox<>(empresas);
        empresaCombo.setPromptText("Todas as empresas");
        empresaCombo.setPrefWidth(280);
        empresaCombo.setMaxWidth(340);
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
        estadoCombo.setPrefWidth(170);
        estadoCombo.setOnAction(e -> applyFilters());

        Label hint = new Label("A pesquisa rápida da tabela também filtra por empresa, ano e observações.");
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

    private AdvancedTableView<ExercicioFiscal> buildTable() {
        table = new AdvancedTableView<>();
        table.setEditable(true);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        TableUtils.standardize(table);
        table.setEntityName("Exercício");

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
        root.getStyleClass().add("exercicios-details");
        root.setPadding(new Insets(16));
        root.setPrefWidth(320);
        root.setMinWidth(300);
        root.setMaxWidth(370);

        HBox heading = new HBox(8);
        heading.setAlignment(Pos.CENTER_LEFT);

        Label icon = new Label("", IconUtils.icon(Feather.CALENDAR, 15));
        icon.getStyleClass().add("exercicios-details-icon");

        VBox titleBox = new VBox(1);
        detailTitle = new Label("Nenhum exercício seleccionado");
        detailTitle.getStyleClass().add("exercicios-details-title");

        Label subtitle = new Label("Detalhes e operações do exercício");
        subtitle.getStyleClass().add("exercicios-details-subtitle");

        titleBox.getChildren().addAll(detailTitle, subtitle);
        heading.getChildren().addAll(icon, titleBox);

        Separator separator = new Separator();

        VBox facts = new VBox(8);
        facts.getStyleClass().add("exercicios-details-facts");

        detailEmpresa = fact("Empresa");
        detailAno = fact("Ano");
        detailPeriodo = fact("Período");
        detailEstado = fact("Estado");
        detailActual = fact("Exercício actual");
        detailEncerrado = fact("Encerrado em");

        facts.getChildren().addAll(
                detailEmpresa, detailAno, detailPeriodo,
                detailEstado, detailActual, detailEncerrado
        );

        Label actionsTitle = new Label("Operações");
        actionsTitle.getStyleClass().add("exercicios-details-section");

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

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        root.getChildren().addAll(
                heading, separator, facts,
                actionsTitle, actions, spacer, help
        );

        clearDetails();
        return root;
    }

    private Label fact(String caption) {
        Label label = new Label(caption + ": —");
        label.getStyleClass().add("exercicios-detail-fact");
        return label;
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

        Empresa selectedEmpresa = empresaCombo.getValue();
        String estado = estadoCombo.getValue();

        exerciciosFiltrados.setPredicate(exercicio -> {
            boolean empresaOk = selectedEmpresa == null
                    || (exercicio.getEmpresa() != null
                    && Objects.equals(exercicio.getEmpresa().getId(), selectedEmpresa.getId()));

            boolean estadoOk = estado == null
                    || "Todos".equals(estado)
                    || (exercicio.getEstado() != null
                    && estado.equals(exercicio.getEstado().toString()));

            return empresaOk && estadoOk;
        });

        updateKpis();
        showDetails(table.getSelectionModel().getSelectedItem());
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
                : exercicio.getEmpresa().getNome();

        boolean actual = exercicio.getEmpresa() != null
                && Objects.equals(
                exercicio.getEmpresa().getExercicioActual(),
                exercicio.getAno()
        );

        detailTitle.setText("Exercício " + exercicio.getAno());
        detailEmpresa.setText("Empresa: " + empresaNome);
        detailAno.setText("Ano: " + value(exercicio.getAno()));
        detailPeriodo.setText(
                "Período: "
                        + formatDate(exercicio.getDataInicio())
                        + " — "
                        + formatDate(exercicio.getDataFim())
        );
        detailEstado.setText(
                "Estado: "
                        + (exercicio.getEstado() == null ? "—" : exercicio.getEstado())
        );
        detailActual.setText("Exercício actual: " + (actual ? "Sim" : "Não"));
        detailEncerrado.setText(
                "Encerrado em: "
                        + (exercicio.getEncerradoEm() == null
                        ? "—"
                        : DATE_FORMAT.format(exercicio.getEncerradoEm().toLocalDate())
                                + " por " + value(exercicio.getEncerradoPor()))
        );

        btnActual.setDisable(actual || exercicio.getEstado() == EstadoExercicio.FECHADO);
        btnEncerramento.setDisable(exercicio.getEstado() != EstadoExercicio.ABERTO);
        btnFechar.setDisable(exercicio.getEstado() != EstadoExercicio.ENCERRAMENTO);
        btnReabrir.setDisable(exercicio.getEstado() != EstadoExercicio.FECHADO);
    }

    private void clearDetails() {
        if (detailTitle != null) detailTitle.setText("Nenhum exercício seleccionado");
        if (detailEmpresa != null) detailEmpresa.setText("Empresa: —");
        if (detailAno != null) detailAno.setText("Ano: —");
        if (detailPeriodo != null) detailPeriodo.setText("Período: —");
        if (detailEstado != null) detailEstado.setText("Estado: —");
        if (detailActual != null) detailActual.setText("Exercício actual: —");
        if (detailEncerrado != null) detailEncerrado.setText("Encerrado em: —");

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

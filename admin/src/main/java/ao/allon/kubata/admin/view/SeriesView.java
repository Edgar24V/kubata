package ao.allon.kubata.admin.view;

import ao.allon.kubata.admin.service.PersistenceService;
import ao.allon.kubata.admin.service.SessionManager;
import ao.allon.kubata.admin.ui.modal.ModalManager;
import ao.allon.kubata.admin.ui.util.IconUtils;
import ao.allon.kubata.core.domain.Empresa;
import ao.allon.kubata.core.domain.Role;
import ao.allon.kubata.core.domain.SerieDocumento;
import ao.allon.kubata.core.domain.SerieDocumento.EstadoSerie;
import ao.allon.kubata.core.domain.SerieDocumento.TipoDocumentoSAFT;
import ao.allon.kubata.core.domain.User;
import ao.allon.kubata.core.repository.EmpresaRepository;
import ao.allon.kubata.core.repository.SerieDocumentoRepository;
import ao.allon.kubata.core.service.AcessoService;
import ao.allon.kubata.core.ui.table.AdvancedTableView;
import ao.allon.kubata.core.ui.table.TableUtils;
import ao.allon.kubata.core.ui.table.TextTableCell;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Central fiscal de séries documentais do Kubata.
 *
 * <p>A vista mantém a lógica actual de numeração e criação, mas fornece
 * contexto operacional, filtros, indicadores fiscais, detalhe da série,
 * controlo de permissões e acesso ao assistente de criação em lote.</p>
 */
@Component
public class SeriesView extends VBox {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final SerieDocumentoRepository serieRepository;
    private final EmpresaRepository empresaRepository;
    private final AcessoService acessoService;
    private final SessionManager sessionManager;
    private final ModalManager modalManager;
    private final SerieDocumentoWizardView wizardView;
    private final PersistenceService persistenceService;

    private final ObservableList<SerieDocumento> series = FXCollections.observableArrayList();

    private AdvancedTableView<SerieDocumento> table;
    private TextField searchField;
    private ComboBox<Empresa> empresaFilter;
    private ComboBox<String> estadoFilter;
    private ComboBox<String> tipoFilter;

    private Label totalValue;
    private Label activeValue;
    private Label pendingValue;
    private Label closedValue;

    private Label detailSerie;
    private Label detailDescription;
    private Label detailCompany;
    private Label detailType;
    private Label detailExercise;
    private Label detailNumber;
    private Label detailFormat;
    private Label detailValidity;
    private Label detailAgT;
    private Label detailDefault;
    private Label detailState;

    private Button btnNovo;
    private Button btnLote;
    private Button btnEditar;
    private Button btnClonar;
    private Button btnRemover;

    private boolean dataLoaded;

    public SeriesView(SerieDocumentoRepository serieRepository,
                      EmpresaRepository empresaRepository,
                      AcessoService acessoService,
                      SessionManager sessionManager,
                      ModalManager modalManager,
                      SerieDocumentoWizardView wizardView,
                      PersistenceService persistenceService) {
        this.serieRepository = serieRepository;
        this.empresaRepository = empresaRepository;
        this.acessoService = acessoService;
        this.sessionManager = sessionManager;
        this.modalManager = modalManager;
        this.wizardView = wizardView;
        this.persistenceService = persistenceService;

        setSpacing(0);
        setPadding(Insets.EMPTY);
        getStyleClass().add("kubata-series-page");
        buildUI();
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        if (!dataLoaded && getScene() != null) {
            dataLoaded = true;
            refreshPermissions();
            loadSeries();
        }
    }

    private void buildUI() {
        VBox header = buildHeader();

        BorderPane workspace = new BorderPane();
        workspace.setTop(buildFilters());
        workspace.setCenter(buildMainArea());

        getChildren().addAll(header, workspace, buildStatusBar());
        VBox.setVgrow(workspace, Priority.ALWAYS);
    }

    private VBox buildHeader() {
        VBox header = new VBox(11);
        header.setPadding(new Insets(18, 22, 14, 22));
        header.getStyleClass().add("kubata-series-header");

        HBox titleLine = new HBox(12);
        titleLine.setAlignment(Pos.CENTER_LEFT);

        Label icon = new Label("", IconUtils.icon(Feather.LAYERS, 24));
        icon.getStyleClass().add("kubata-series-title-icon");

        VBox titleBox = new VBox(2);
        Label title = new Label("Séries Documentais");
        title.getStyleClass().add("kubata-series-title");

        Label subtitle = new Label(
                "Gestão fiscal de séries, numeração, exercícios, prefixos, validade e registo AGT."
        );
        subtitle.getStyleClass().add("kubata-series-subtitle");
        subtitle.setWrapText(true);

        titleBox.getChildren().addAll(title, subtitle);

        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        btnLote = new Button(
                "Assistente por lote",
                IconUtils.icon(Feather.LAYERS, 13)
        );
        btnLote.getStyleClass().add("button-outlined");
        btnLote.setOnAction(e -> startBatchWizard());

        btnNovo = new Button(
                "Nova série",
                IconUtils.icon(Feather.PLUS, 13)
        );
        btnNovo.getStyleClass().add("button-primary");
        btnNovo.setOnAction(e -> showSerieDialog(null));

        Button refresh = new Button(
                "Actualizar",
                IconUtils.icon(Feather.REFRESH_CW, 13)
        );
        refresh.getStyleClass().add("button-outlined");
        refresh.setOnAction(e -> loadSeries());

        titleLine.getChildren().addAll(icon, titleBox, spacer, btnLote, btnNovo, refresh);

        totalValue = new Label("0");
        activeValue = new Label("0");
        pendingValue = new Label("0");
        closedValue = new Label("0");

        HBox kpis = new HBox(
                10,
                kpi("TOTAL", Feather.LAYERS, totalValue),
                kpi("ACTIVAS", Feather.CHECK_CIRCLE, activeValue),
                kpi("PENDENTES AGT", Feather.CLOCK, pendingValue),
                kpi("ENCERRADAS", Feather.LOCK, closedValue)
        );

        header.getChildren().addAll(titleLine, kpis);
        return header;
    }

    private VBox kpi(String title, Feather icon, Label value) {
        VBox card = new VBox(2);
        card.getStyleClass().add("kubata-series-kpi");
        card.setPadding(new Insets(10, 14, 10, 14));
        card.setMinWidth(160);

        HBox line = new HBox(7);
        line.setAlignment(Pos.CENTER_LEFT);

        Label i = new Label("", IconUtils.icon(icon, 14));
        i.getStyleClass().add("kubata-series-kpi-icon");

        Label t = new Label(title);
        t.getStyleClass().add("kubata-series-kpi-title");

        line.getChildren().addAll(i, t);
        value.getStyleClass().add("kubata-series-kpi-value");

        card.getChildren().addAll(line, value);
        return card;
    }

    private HBox buildFilters() {
        HBox bar = new HBox(8);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(9, 14, 9, 14));
        bar.getStyleClass().add("kubata-series-filter-bar");

        searchField = new TextField();
        searchField.setPromptText("Pesquisar série, descrição, prefixo ou tipo...");
        searchField.setPrefWidth(320);
        searchField.textProperty().addListener((obs, old, value) -> applyFilters());

        empresaFilter = new ComboBox<>();
        empresaFilter.setPromptText("Empresa");
        empresaFilter.setPrefWidth(185);
        loadCompanyFilter();

        tipoFilter = new ComboBox<>(FXCollections.observableArrayList(
                "Todos os tipos",
                "Vendas",
                "Compras",
                "Stock",
                "Financeiro"
        ));
        tipoFilter.setValue("Todos os tipos");
        tipoFilter.setPrefWidth(150);

        estadoFilter = new ComboBox<>(FXCollections.observableArrayList(
                "Todos os estados",
                "Activas",
                "Inactivas",
                "Encerradas",
                "Pendentes AGT"
        ));
        estadoFilter.setValue("Todos os estados");
        estadoFilter.setPrefWidth(150);

        empresaFilter.valueProperty().addListener((obs, old, value) -> applyFilters());
        tipoFilter.valueProperty().addListener((obs, old, value) -> applyFilters());
        estadoFilter.valueProperty().addListener((obs, old, value) -> applyFilters());

        Button clear = new Button(
                "Limpar",
                IconUtils.icon(Feather.X, 12)
        );
        clear.getStyleClass().add("button-outlined");
        clear.setOnAction(e -> {
            searchField.clear();
            empresaFilter.setValue(null);
            tipoFilter.setValue("Todos os tipos");
            estadoFilter.setValue("Todos os estados");
        });

        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label hint = new Label("Clique duplo para editar");
        hint.getStyleClass().add("kubata-series-filter-hint");

        bar.getChildren().addAll(
                searchField, empresaFilter, tipoFilter, estadoFilter, clear, spacer, hint
        );
        return bar;
    }

    private void loadCompanyFilter() {
        empresaFilter.getItems().clear();
        empresaFilter.getItems().add(null);
        empresaFilter.getItems().addAll(empresaRepository.findAll());
    }

    private SplitPane buildMainArea() {
        table = buildTable();

        VBox details = buildDetails();
        ScrollPane detailScroll = new ScrollPane(details);
        detailScroll.setFitToWidth(true);
        detailScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        detailScroll.getStyleClass().add("kubata-series-details-wrapper");

        SplitPane split = new SplitPane(
                new StackPane(table),
                detailScroll
        );
        split.setDividerPositions(0.72);
        split.getItems().get(0).getStyleClass().add("kubata-series-table-pane");
        return split;
    }

    private AdvancedTableView<SerieDocumento> buildTable() {
        AdvancedTableView<SerieDocumento> tv = new AdvancedTableView<>();
        tv.setData(series);
        tv.setEntityName("Série");
        tv.setPlaceholder(new Label("Nenhuma série corresponde aos filtros."));
        tv.setEditable(true);
        TableUtils.standardize(tv);

        TableColumn<SerieDocumento, String> colSerie = TableUtils.createTextColumn(
                "Série",
                c -> new SimpleStringProperty(safe(c.getValue().getSerie(), "—"))
        );
        colSerie.setCellFactory(tc -> TextTableCell.create());
        colSerie.setOnEditCommit(event -> {
            if (!hasEditPermission()) return;
            SerieDocumento s = event.getRowValue();
            String value = event.getNewValue() == null ? "" : event.getNewValue().trim().toUpperCase(Locale.ROOT);
            if (value.isBlank()) {
                modalManager.alert("Valor inválido", "A série não pode ficar vazia.", "warning", null);
                loadSeries();
                return;
            }
            s.setSerie(value);
            saveSerieInline(s);
        });
        colSerie.setPrefWidth(95);

        TableColumn<SerieDocumento, String> colDesc = TableUtils.createTextColumn(
                "Descrição",
                c -> new SimpleStringProperty(safe(c.getValue().getDescricao(), "—"))
        );
        colDesc.setCellFactory(tc -> TextTableCell.create());
        colDesc.setOnEditCommit(event -> {
            if (!hasEditPermission()) return;
            SerieDocumento s = event.getRowValue();
            s.setDescricao(event.getNewValue());
            saveSerieInline(s);
        });
        colDesc.setPrefWidth(230);

        TableColumn<SerieDocumento, String> colTipo = TableUtils.createTextColumn(
                "Documento",
                c -> new SimpleStringProperty(
                        c.getValue().getTipoDocumento() == null
                                ? "—"
                                : c.getValue().getTipoDocumento().getDescricao()
                )
        );
        colTipo.setPrefWidth(170);

        TableColumn<SerieDocumento, String> colEmpresa = TableUtils.createTextColumn(
                "Empresa",
                c -> new SimpleStringProperty(
                        c.getValue().getEmpresa() == null ? "—" : c.getValue().getEmpresa().getNome()
                )
        );
        colEmpresa.setPrefWidth(165);

        TableColumn<SerieDocumento, String> colExercicio = TableUtils.createTextColumn(
                "Exercício",
                c -> new SimpleStringProperty(
                        c.getValue().getExercicio() == null ? "—" : String.valueOf(c.getValue().getExercicio())
                )
        );
        colExercicio.setPrefWidth(80);

        TableColumn<SerieDocumento, String> colNumero = TableUtils.createTextColumn(
                "Último Nº",
                c -> new SimpleStringProperty(
                        c.getValue().getUltimoNumero() == null ? "0" : String.valueOf(c.getValue().getUltimoNumero())
                )
        );
        colNumero.setPrefWidth(90);

        TableColumn<SerieDocumento, String> colEstado = new TableColumn<>("Estado");
        colEstado.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getEstado() == null ? "Desconhecido" : c.getValue().getEstado().getDescricao()
        ));
        colEstado.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                Label badge = new Label(item);
                badge.getStyleClass().addAll("kubata-series-badge", seriesStatusClass(item));
                setGraphic(badge);
                setText(null);
                setAlignment(Pos.CENTER);
            }
        });
        colEstado.setPrefWidth(120);

        TableColumn<SerieDocumento, String> colAgT = new TableColumn<>("AGT");
        colAgT.setCellValueFactory(c -> new SimpleStringProperty(
                Boolean.TRUE.equals(c.getValue().getRegistadaAGT()) ? "Registada" : "Não registada"
        ));
        colAgT.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                Label badge = new Label(item);
                badge.getStyleClass().addAll(
                        "kubata-series-badge",
                        "Registada".equals(item)
                                ? "kubata-series-badge-success"
                                : "kubata-series-badge-neutral"
                );
                setGraphic(badge);
                setText(null);
                setAlignment(Pos.CENTER);
            }
        });
        colAgT.setPrefWidth(105);

        tv.getColumns().addAll(
                colSerie, colDesc, colTipo, colEmpresa, colExercicio,
                colNumero, colEstado, colAgT
        );

        tv.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, selected) -> updateDetails(selected));

        tv.setOnEdit(this::editIfAllowed);
        tv.setOnViewDetails(this::showSerieDetails);
        tv.setOnDelete(selected -> removeSerie());
        tv.setOnRefresh(this::loadSeries);

        tv.setRowFactory(view -> {
            TableRow<SerieDocumento> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    editIfAllowed(row.getItem());
                }
            });
            return row;
        });

        return tv;
    }

    private VBox buildDetails() {
        VBox details = new VBox(13);
        details.setPadding(new Insets(16));
        details.getStyleClass().add("kubata-series-details");

        HBox identity = new HBox(10);
        identity.setAlignment(Pos.CENTER_LEFT);

        StackPane iconBox = new StackPane();
        iconBox.getStyleClass().add("kubata-series-detail-icon");
        iconBox.getChildren().add(new Label("", IconUtils.icon(Feather.LAYERS, 17)));

        VBox text = new VBox(2);
        detailSerie = new Label("Nenhuma série seleccionada");
        detailSerie.getStyleClass().add("kubata-series-detail-title");

        detailDescription = new Label("Seleccione uma série para consultar o contexto fiscal.");
        detailDescription.getStyleClass().add("kubata-series-detail-subtitle");
        detailDescription.setWrapText(true);

        text.getChildren().addAll(detailSerie, detailDescription);
        identity.getChildren().addAll(iconBox, text);

        detailCompany = detailValue(details, "EMPRESA", "—");
        detailType = detailValue(details, "TIPO DE DOCUMENTO", "—");
        detailExercise = detailValue(details, "EXERCÍCIO", "—");
        detailNumber = detailValue(details, "ÚLTIMO NÚMERO", "—");
        detailFormat = detailValue(details, "FORMATO", "—");
        detailValidity = detailValue(details, "VALIDADE", "—");
        detailAgT = detailValue(details, "REGISTO AGT", "—");
        detailDefault = detailValue(details, "PREDEFINIDA", "—");
        detailState = detailValue(details, "ESTADO", "—");

        VBox actions = new VBox(7);
        Label actionTitle = new Label("Operações");
        actionTitle.getStyleClass().add("kubata-series-section-title");

        btnEditar = actionButton("Editar série", Feather.EDIT_2);
        btnClonar = actionButton("Clonar série", Feather.COPY);
        btnRemover = actionButton("Remover série", Feather.TRASH_2);
        btnRemover.getStyleClass().add("button-danger-outlined");

        btnEditar.setOnAction(e -> selectedSerie().ifPresent(this::editIfAllowed));
        btnClonar.setOnAction(e -> selectedSerie().ifPresent(this::cloneIfAllowed));
        btnRemover.setOnAction(e -> removeSerie());

        actions.getChildren().addAll(actionTitle, btnEditar, btnClonar, btnRemover);

        VBox governance = new VBox(7);
        Label govTitle = new Label("Controlo fiscal");
        govTitle.getStyleClass().add("kubata-series-section-title");

        Label govText = new Label(
                "A numeração é mantida pela Série. Alterações ao último número devem ser feitas com cuidado, "
                        + "porque afectam a sequência usada pelos documentos."
        );
        govText.setWrapText(true);
        govText.getStyleClass().add("kubata-series-policy-text");

        governance.getChildren().addAll(govTitle, govText);

        details.getChildren().addAll(
                identity,
                new Separator(),
                detailCompany,
                detailType,
                detailExercise,
                detailNumber,
                detailFormat,
                detailValidity,
                detailAgT,
                detailDefault,
                detailState,
                new Separator(),
                actions,
                governance
        );

        updateDetails(null);
        return details;
    }

    private Label detailValue(VBox target, String label, String initial) {
        VBox row = new VBox(2);
        row.getStyleClass().add("kubata-series-detail-row");

        Label title = new Label(label);
        title.getStyleClass().add("kubata-series-detail-label");

        Label value = new Label(initial);
        value.setWrapText(true);
        value.getStyleClass().add("kubata-series-detail-value");

        row.getChildren().addAll(title, value);
        target.getChildren().add(row);
        return value;
    }

    private Button actionButton(String text, Feather icon) {
        Button button = new Button(text, IconUtils.icon(icon, 12));
        button.setMaxWidth(Double.MAX_VALUE);
        button.setMinHeight(32);
        button.getStyleClass().add("button-outlined");
        return button;
    }

    private HBox buildStatusBar() {
        HBox bar = new HBox(10);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(7, 14, 7, 14));
        bar.getStyleClass().add("kubata-series-statusbar");

        Label status = new Label("0 séries carregadas");
        status.getStyleClass().add("kubata-series-status-text");

        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label hint = new Label("Séries devem respeitar o exercício e o tipo de documento.");
        hint.getStyleClass().add("kubata-series-status-hint");

        bar.getChildren().addAll(status, spacer, hint);

        series.addListener((javafx.collections.ListChangeListener<SerieDocumento>) c ->
                Platform.runLater(() ->
                        status.setText(series.size() + " série(s) no catálogo"))
        );

        return bar;
    }

    private void loadSeries() {
        table.setLoading(true);

        persistenceService.executeAsync(
                () -> {
                    List<SerieDocumento> all = serieRepository.findAllWithEmpresa();
                    Platform.runLater(() -> {
                        series.setAll(all);
                        table.setLoading(false);
                        updateKpis();
                        applyFilters();
                        updateDetails(table.getSelectionModel().getSelectedItem());
                        refreshPermissions();
                    });
                },
                "READ",
                "SERIE",
                "Carregamento da central de séries",
                null
        );
    }

    private void loadCompaniesAfterRefresh() {
        Empresa selected = empresaFilter.getValue();
        empresaFilter.getItems().clear();
        empresaFilter.getItems().add(null);
        empresaFilter.getItems().addAll(empresaRepository.findAll());
        empresaFilter.setValue(selected);
    }

    private void applyFilters() {
        if (table == null) return;

        String q = searchField == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        Empresa empresa = empresaFilter == null ? null : empresaFilter.getValue();
        String tipo = tipoFilter == null ? "Todos os tipos" : tipoFilter.getValue();
        String estado = estadoFilter == null ? "Todos os estados" : estadoFilter.getValue();

        table.setFilter(serie -> {
            if (serie == null) return false;

            boolean text = q.isBlank()
                    || contains(serie.getSerie(), q)
                    || contains(serie.getDescricao(), q)
                    || contains(serie.getPrefixo(), q)
                    || (serie.getTipoDocumento() != null
                    && serie.getTipoDocumento().getDescricao().toLowerCase(Locale.ROOT).contains(q));

            boolean company = empresa == null
                    || (serie.getEmpresa() != null
                    && serie.getEmpresa().getId() != null
                    && Objects.equals(serie.getEmpresa().getId(), empresa.getId()));

            boolean type = "Todos os tipos".equals(tipo)
                    || (serie.getTipoDocumento() != null
                    && Objects.equals(serie.getTipoDocumento().getArea(), tipo));

            boolean state = switch (estado) {
                case "Activas" -> serie.getEstado() == EstadoSerie.ACTIVA;
                case "Inactivas" -> serie.getEstado() == EstadoSerie.INACTIVA;
                case "Encerradas" -> serie.getEstado() == EstadoSerie.ENCERRADA;
                case "Pendentes AGT" -> serie.getEstado() == EstadoSerie.PENDENTE_AGT;
                default -> true;
            };

            return text && company && type && state;
        });
    }

    private void updateKpis() {
        totalValue.setText(String.valueOf(series.size()));
        activeValue.setText(String.valueOf(
                series.stream().filter(s -> s.getEstado() == EstadoSerie.ACTIVA).count()
        ));
        pendingValue.setText(String.valueOf(
                series.stream().filter(s ->
                        s.getEstado() == EstadoSerie.PENDENTE_AGT
                                || !Boolean.TRUE.equals(s.getRegistadaAGT())
                ).count()
        ));
        closedValue.setText(String.valueOf(
                series.stream().filter(s -> s.getEstado() == EstadoSerie.ENCERRADA).count()
        ));
    }

    private void updateDetails(SerieDocumento serie) {
        if (detailSerie == null) return;

        if (serie == null) {
            detailSerie.setText("Nenhuma série seleccionada");
            detailDescription.setText("Seleccione uma série para consultar o contexto fiscal.");
            detailCompany.setText("—");
            detailType.setText("—");
            detailExercise.setText("—");
            detailNumber.setText("—");
            detailFormat.setText("—");
            detailValidity.setText("—");
            detailAgT.setText("—");
            detailDefault.setText("—");
            detailState.setText("—");
            refreshPermissions();
            return;
        }

        detailSerie.setText(safe(serie.getSerie(), "SÉRIE"));
        detailDescription.setText(safe(serie.getDescricao(), "Sem descrição"));
        detailCompany.setText(serie.getEmpresa() == null ? "—" : serie.getEmpresa().getNome());
        detailType.setText(serie.getTipoDocumento() == null
                ? "—"
                : serie.getTipoDocumento().getDescricao());
        detailExercise.setText(serie.getExercicio() == null ? "—" : String.valueOf(serie.getExercicio()));
        detailNumber.setText(String.valueOf(serie.getUltimoNumero() == null ? 0 : serie.getUltimoNumero()));
        detailFormat.setText(safe(serie.getFormatoNumero(), "—"));
        detailValidity.setText(formatDateRange(serie));
        detailAgT.setText(Boolean.TRUE.equals(serie.getRegistadaAGT())
                ? "Registada" + (serie.getCodigoValidacaoAGT() == null
                ? "" : " · Código: " + serie.getCodigoValidacaoAGT())
                : "Não registada");
        detailDefault.setText(Boolean.TRUE.equals(serie.getPredefinida()) ? "Sim" : "Não");
        detailState.setText(serie.getEstado() == null ? "—" : serie.getEstado().getDescricao());

        refreshPermissions();
    }

    private void startBatchWizard() {
        if (!hasCreatePermission()) {
            deny("Não possui permissão para criar séries.");
            return;
        }

        if (empresaRepository.findFirstByAtivaTrue().isEmpty()) {
            modalManager.alert(
                    "Empresa activa necessária",
                    "Active uma empresa antes de criar séries.",
                    "warning",
                    null
            );
            return;
        }

        wizardView.start();
    }

    public void showSerieDialog(SerieDocumento serie) {
        boolean isNew = serie == null;

        if (isNew && !hasCreatePermission()) {
            deny("Não possui permissão para criar séries.");
            return;
        }
        if (!isNew && !hasEditPermission()) {
            deny("Não possui permissão para editar séries.");
            return;
        }

        Optional<Empresa> activeCompany = empresaRepository.findFirstByAtivaTrue();
        if (activeCompany.isEmpty()) {
            modalManager.alert(
                    "Empresa activa necessária",
                    "Active uma empresa antes de gerir séries.",
                    "warning",
                    null
            );
            return;
        }

        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(10);
        grid.setPadding(new Insets(6));

        TextField txtSerie = new TextField(isNew ? "" : safe(serie.getSerie(), ""));
        txtSerie.setPromptText("Ex.: A, B, 2026");

        ComboBox<TipoDocumentoSAFT> cmbTipo = new ComboBox<>(
                FXCollections.observableArrayList(TipoDocumentoSAFT.values())
        );
        cmbTipo.setValue(isNew ? TipoDocumentoSAFT.FT : serie.getTipoDocumento());
        cmbTipo.setMaxWidth(Double.MAX_VALUE);

        TextField txtDescricao = new TextField(
                isNew ? "" : safe(serie.getDescricao(), "")
        );
        txtDescricao.setPromptText("Descrição da série");

        TextField txtExercicio = new TextField(
                String.valueOf(isNew
                        ? LocalDate.now().getYear()
                        : serie.getExercicio())
        );

        TextField txtNumeroInicial = new TextField(
                String.valueOf(isNew ? 1L : serie.getNumeroInicial())
        );

        TextField txtUltimoNumero = new TextField(
                String.valueOf(isNew ? 0L : serie.getUltimoNumero())
        );

        TextField txtPrefixo = new TextField(
                isNew ? TipoDocumentoSAFT.FT.getPrefixo() : safe(serie.getPrefixo(), "")
        );

        TextField txtFormato = new TextField(
                isNew ? "{PREFIXO} {SERIE}/{NUMERO}" : safe(serie.getFormatoNumero(), "")
        );

        DatePicker dpInicio = new DatePicker(
                isNew ? LocalDate.now().withDayOfYear(1) : serie.getDataInicio()
        );
        DatePicker dpFim = new DatePicker(
                isNew ? LocalDate.now().withMonth(12).withDayOfMonth(31) : serie.getDataFim()
        );

        CheckBox chkPredefinida = new CheckBox("Série predefinida");
        chkPredefinida.setSelected(!isNew && Boolean.TRUE.equals(serie.getPredefinida()));

        grid.add(new Label("Série:*"), 0, 0);
        grid.add(txtSerie, 1, 0);
        grid.add(new Label("Tipo:*"), 0, 1);
        grid.add(cmbTipo, 1, 1);
        grid.add(new Label("Descrição:"), 0, 2);
        grid.add(txtDescricao, 1, 2);
        grid.add(new Label("Exercício:*"), 0, 3);
        grid.add(txtExercicio, 1, 3);
        grid.add(new Label("N.º inicial:"), 0, 4);
        grid.add(txtNumeroInicial, 1, 4);
        grid.add(new Label("Último N.º:"), 0, 5);
        grid.add(txtUltimoNumero, 1, 5);
        grid.add(new Label("Prefixo:"), 0, 6);
        grid.add(txtPrefixo, 1, 6);
        grid.add(new Label("Formato:"), 0, 7);
        grid.add(txtFormato, 1, 7);
        grid.add(new Label("Data início:"), 0, 8);
        grid.add(dpInicio, 1, 8);
        grid.add(new Label("Data fim:"), 0, 9);
        grid.add(dpFim, 1, 9);
        grid.add(chkPredefinida, 1, 10);

        GridPane.setHgrow(txtSerie, Priority.ALWAYS);
        GridPane.setHgrow(cmbTipo, Priority.ALWAYS);
        GridPane.setHgrow(txtDescricao, Priority.ALWAYS);
        GridPane.setHgrow(txtExercicio, Priority.ALWAYS);
        GridPane.setHgrow(txtNumeroInicial, Priority.ALWAYS);
        GridPane.setHgrow(txtUltimoNumero, Priority.ALWAYS);
        GridPane.setHgrow(txtPrefixo, Priority.ALWAYS);
        GridPane.setHgrow(txtFormato, Priority.ALWAYS);

        cmbTipo.valueProperty().addListener((obs, old, type) -> {
            if (isNew && type != null) {
                txtPrefixo.setText(type.getPrefixo());
                if (txtDescricao.getText().isBlank()) {
                    txtDescricao.setText(type.getDescricao());
                }
            }
        });

        modalManager.showConfirmModal(
                new ScrollPane(grid),
                isNew ? "Nova Série Documental" : "Editar Série Documental",
                () -> saveSerie(
                        isNew, serie, activeCompany.get(), txtSerie, cmbTipo,
                        txtDescricao, txtExercicio, txtNumeroInicial, txtUltimoNumero,
                        txtPrefixo, txtFormato, dpInicio, dpFim, chkPredefinida
                ),
                null
        );
    }

    private void saveSerie(boolean isNew,
                           SerieDocumento existing,
                           Empresa activeCompany,
                           TextField txtSerie,
                           ComboBox<TipoDocumentoSAFT> cmbTipo,
                           TextField txtDescricao,
                           TextField txtExercicio,
                           TextField txtNumeroInicial,
                           TextField txtUltimoNumero,
                           TextField txtPrefixo,
                           TextField txtFormato,
                           DatePicker dpInicio,
                           DatePicker dpFim,
                           CheckBox chkPredefinida) {
        try {
            String serieValue = txtSerie.getText() == null
                    ? ""
                    : txtSerie.getText().trim().toUpperCase(Locale.ROOT);

            if (serieValue.isBlank()) {
                modalManager.alert("Dados incompletos", "A série é obrigatória.", "warning", null);
                return;
            }
            if (cmbTipo.getValue() == null) {
                modalManager.alert("Dados incompletos", "Seleccione o tipo de documento.", "warning", null);
                return;
            }

            int exercicio = Integer.parseInt(txtExercicio.getText().trim());
            long numeroInicial = Long.parseLong(txtNumeroInicial.getText().trim());
            long ultimoNumero = Long.parseLong(txtUltimoNumero.getText().trim());

            if (exercicio < 2000 || exercicio > 2100) {
                throw new IllegalArgumentException("O exercício deve estar entre 2000 e 2100.");
            }
            if (numeroInicial < 1 || ultimoNumero < 0) {
                throw new IllegalArgumentException("A numeração inicial deve ser >= 1 e o último número >= 0.");
            }
            if (ultimoNumero > 0 && ultimoNumero < numeroInicial - 1) {
                throw new IllegalArgumentException(
                        "O último número não pode ser inferior ao início da sequência."
                );
            }
            if (dpInicio.getValue() != null && dpFim.getValue() != null
                    && dpFim.getValue().isBefore(dpInicio.getValue())) {
                throw new IllegalArgumentException("A data final não pode ser anterior à data inicial.");
            }

            SerieDocumento target = isNew ? new SerieDocumento() : existing;
            target.setEmpresa(activeCompany);
            target.setSerie(serieValue);
            target.setTipoDocumento(cmbTipo.getValue());
            target.setDescricao(
                    txtDescricao.getText().isBlank()
                            ? "Série " + serieValue + " - " + exercicio
                            : txtDescricao.getText().trim()
            );
            target.setExercicio(exercicio);
            target.setNumeroInicial(numeroInicial);
            target.setUltimoNumero(ultimoNumero);
            target.setPrefixo(
                    txtPrefixo.getText().isBlank()
                            ? cmbTipo.getValue().getPrefixo()
                            : txtPrefixo.getText().trim().toUpperCase(Locale.ROOT)
            );
            target.setFormatoNumero(
                    txtFormato.getText().isBlank()
                            ? "{PREFIXO} {SERIE}/{NUMERO}"
                            : txtFormato.getText().trim()
            );
            target.setDataInicio(dpInicio.getValue());
            target.setDataFim(dpFim.getValue());
            target.setPredefinida(chkPredefinida.isSelected());

            if (isNew) {
                target.setEstado(EstadoSerie.ACTIVA);
                target.setRegistadaAGT(false);
                target.setCriadoEm(java.time.LocalDateTime.now());
            }

            persistenceService.saveAsync(
                    serieRepository,
                    target,
                    "SERIE",
                    (isNew ? "Criação" : "Edição") + " da série " + target.getSerie(),
                    saved -> Platform.runLater(this::loadSeries)
            );
        } catch (NumberFormatException ex) {
            modalManager.alert(
                    "Dados inválidos",
                    "Exercício e campos de numeração devem conter valores numéricos válidos.",
                    "warning",
                    ex
            );
        } catch (Exception ex) {
            modalManager.alert(
                    "Não foi possível preparar a série",
                    ex.getMessage() == null ? "Verifique os dados introduzidos." : ex.getMessage(),
                    "error",
                    ex
            );
        }
    }

    private void saveSerieInline(SerieDocumento serie) {
        persistenceService.saveAsync(
                serieRepository,
                serie,
                "SERIE",
                "Actualização inline da série: " + serie.getSerie(),
                saved -> Platform.runLater(this::loadSeries)
        );
    }

    private void editIfAllowed(SerieDocumento serie) {
        if (serie == null) return;
        if (!hasEditPermission()) {
            deny("Não possui permissão para editar séries.");
            return;
        }
        showSerieDialog(serie);
    }

    private void cloneIfAllowed(SerieDocumento original) {
        if (original == null) return;
        if (!hasCreatePermission()) {
            deny("Não possui permissão para criar séries.");
            return;
        }

        SerieDocumento clone = new SerieDocumento();
        clone.setSerie(safe(original.getSerie(), "SERIE") + "_COPY");
        clone.setTipoDocumento(original.getTipoDocumento());
        clone.setDescricao(safe(original.getDescricao(), "Série") + " — Cópia");
        clone.setExercicio(original.getExercicio());
        clone.setEmpresa(original.getEmpresa());
        clone.setEstado(EstadoSerie.ACTIVA);
        clone.setNumeroInicial(original.getNumeroInicial());
        clone.setUltimoNumero(0L);
        clone.setFormatoNumero(original.getFormatoNumero());
        clone.setPrefixo(original.getPrefixo());
        clone.setDataInicio(original.getDataInicio());
        clone.setDataFim(original.getDataFim());
        clone.setPredefinida(false);
        clone.setRegistadaAGT(false);
        clone.setCriadoEm(java.time.LocalDateTime.now());

        persistenceService.saveAsync(
                serieRepository,
                clone,
                "SERIE",
                "Clonagem da série " + original.getSerie(),
                saved -> Platform.runLater(this::loadSeries)
        );
    }

    private void removeSerie() {
        SerieDocumento selected = selectedSerie().orElse(null);
        if (selected == null) {
            modalManager.alert(
                    "Série não seleccionada",
                    "Seleccione uma série para remover.",
                    "warning",
                    null
            );
            return;
        }
        if (!hasDeletePermission()) {
            deny("Não possui permissão para remover séries.");
            return;
        }

        modalManager.showConfirm(
                "Remover série",
                "Confirma a remoção da série " + selected.getSerie()
                        + " do exercício " + selected.getExercicio() + "?",
                () -> persistenceService.deleteAsync(
                        serieRepository,
                        selected,
                        null,
                        "SERIE",
                        "Remoção da série " + selected.getSerie(),
                        this::loadSeries
                )
        );
    }

    private void showSerieDetails(SerieDocumento serie) {
        if (serie == null) return;

        VBox content = new VBox(10);
        content.setPadding(new Insets(8));

        content.getChildren().addAll(
                infoCard("Empresa", serie.getEmpresa() == null ? "—" : serie.getEmpresa().getNome()),
                infoCard("Série", safe(serie.getSerie(), "—")),
                infoCard("Documento", serie.getTipoDocumento() == null
                        ? "—" : serie.getTipoDocumento().getDescricao()),
                infoCard("Exercício", String.valueOf(serie.getExercicio())),
                infoCard("Prefixo e formato", safe(serie.getPrefixo(), "—") + " · " + safe(serie.getFormatoNumero(), "—")),
                infoCard("Numeração", "Inicial: " + serie.getNumeroInicial() + " · Último: " + serie.getUltimoNumero()),
                infoCard("Validade", formatDateRange(serie)),
                infoCard("AGT", Boolean.TRUE.equals(serie.getRegistadaAGT())
                        ? "Registada · " + safe(serie.getCodigoValidacaoAGT(), "Sem código")
                        : "Não registada")
        );

        modalManager.showModal(
                content,
                new ModalManager.ModalConfig()
                        .size(640, 600)
                        .minSize(560, 520)
                        .title("Detalhes da série")
                        .icon(Feather.LAYERS)
        );
    }

    private VBox infoCard(String title, String value) {
        VBox card = new VBox(3);
        card.getStyleClass().add("kubata-series-info-card");

        Label t = new Label(title.toUpperCase(Locale.ROOT));
        t.getStyleClass().add("kubata-series-info-label");

        Label v = new Label(value);
        v.setWrapText(true);
        v.getStyleClass().add("kubata-series-info-value");

        card.getChildren().addAll(t, v);
        return card;
    }

    private Optional<SerieDocumento> selectedSerie() {
        return table == null
                ? Optional.empty()
                : Optional.ofNullable(table.getSelectionModel().getSelectedItem());
    }

    private boolean hasPermission(ao.allon.kubata.core.domain.PermissaoPerfil.Operacao operation) {
        User user = sessionManager.getUser();
        if (user == null) return false;
        if (user.isSuperadmin() || user.getRole() == Role.ADMIN) return true;

        return acessoService.temAcesso(
                user,
                "ADMINISTRATOR",
                "SERIES",
                operation
        );
    }

    private boolean hasCreatePermission() {
        return hasPermission(ao.allon.kubata.core.domain.PermissaoPerfil.Operacao.CRIAR);
    }

    private boolean hasEditPermission() {
        return hasPermission(ao.allon.kubata.core.domain.PermissaoPerfil.Operacao.EDITAR);
    }

    private boolean hasDeletePermission() {
        return hasPermission(ao.allon.kubata.core.domain.PermissaoPerfil.Operacao.APAGAR);
    }

    private void refreshPermissions() {
        boolean create = hasCreatePermission();
        boolean edit = hasEditPermission();
        boolean delete = hasDeletePermission();

        if (btnNovo != null) btnNovo.setDisable(!create);
        if (btnLote != null) btnLote.setDisable(!create);
        if (btnEditar != null) btnEditar.setDisable(!edit || selectedSerie().isEmpty());
        if (btnClonar != null) btnClonar.setDisable(!create || selectedSerie().isEmpty());
        if (btnRemover != null) btnRemover.setDisable(!delete || selectedSerie().isEmpty());

        if (table != null) {
            table.setOnEdit(edit ? this::editIfAllowed : this::showSerieDetails);
            table.setOnDelete(delete ? selected -> removeSerie() : null);
        }
    }

    private void deny(String message) {
        modalManager.alert("Acesso negado", message, "warning", null);
    }

    private String seriesStatusClass(String status) {
        return switch (status) {
            case "Activa" -> "kubata-series-badge-success";
            case "Pendente Registo AGT" -> "kubata-series-badge-warning";
            case "Encerrada" -> "kubata-series-badge-neutral";
            default -> "kubata-series-badge-danger";
        };
    }

    private String formatDateRange(SerieDocumento serie) {
        if (serie == null || (serie.getDataInicio() == null && serie.getDataFim() == null)) {
            return "Sem período definido";
        }
        String start = serie.getDataInicio() == null
                ? "—" : serie.getDataInicio().format(DATE_FMT);
        String end = serie.getDataFim() == null
                ? "—" : serie.getDataFim().format(DATE_FMT);
        return start + " → " + end;
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }

    private String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}

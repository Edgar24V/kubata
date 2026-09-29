package ao.allon.kubata.admin.view;

import ao.allon.kubata.admin.service.PersistenceService;
import ao.allon.kubata.admin.service.SessionManager;
import ao.allon.kubata.admin.ui.modal.ModalManager;
import ao.allon.kubata.admin.ui.util.IconUtils;
import ao.allon.kubata.core.domain.ModuloSistema;
import ao.allon.kubata.core.domain.Role;
import ao.allon.kubata.core.domain.User;
import ao.allon.kubata.core.repository.ModuloSistemaRepository;
import ao.allon.kubata.core.repository.UserRepository;
import ao.allon.kubata.core.service.SecurityService;
import ao.allon.kubata.core.ui.table.AdvancedTableView;
import ao.allon.kubata.core.ui.table.TableUtils;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Central de licenciamento do Kubata Administrator.
 *
 * <p>Responsável pela visualização do estado de licenças por módulo,
 * alertas de validade, configuração das chaves e acompanhamento do
 * ambiente licenciado.</p>
 */
@Component
public class LicenciamentoView extends VBox {

    private static final int ALERT_DAYS = 30;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ModuloSistemaRepository moduloRepository;
    private final UserRepository userRepository;
    private final PersistenceService persistenceService;
    private final SessionManager sessionManager;
    private final ModalManager modalManager;
    private final SecurityService securityService;

    private final ObservableList<ModuloSistema> modulos = FXCollections.observableArrayList();

    private AdvancedTableView<ModuloSistema> table;
    private TextField searchField;
    private ComboBox<String> stateFilter;
    private Label totalValue;
    private Label licensedValue;
    private Label expiringValue;
    private Label expiredValue;

    private Label detailModule;
    private Label detailDescription;
    private Label detailState;
    private Label detailVersion;
    private Label detailLicenseState;
    private Label detailValidity;
    private Label detailKey;
    private Label detailInstallation;
    private Label detailRequired;

    private Button btnHeaderConfigure;
    private Button btnConfigure;
    private Button btnClearLicense;

    public LicenciamentoView(ModuloSistemaRepository moduloRepository,
                             UserRepository userRepository,
                             PersistenceService persistenceService,
                             SessionManager sessionManager,
                             ModalManager modalManager,
                             SecurityService securityService) {
        this.moduloRepository = moduloRepository;
        this.userRepository = userRepository;
        this.persistenceService = persistenceService;
        this.sessionManager = sessionManager;
        this.modalManager = modalManager;
        this.securityService = securityService;

        setSpacing(0);
        setPadding(Insets.EMPTY);
        getStyleClass().add("kubata-licenses-page");

        buildUI();
        Platform.runLater(this::reload);
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
        header.getStyleClass().add("kubata-licenses-header");

        HBox titleLine = new HBox(12);
        titleLine.setAlignment(Pos.CENTER_LEFT);

        Label icon = new Label("", IconUtils.icon(Feather.KEY, 24));
        icon.getStyleClass().add("kubata-licenses-title-icon");

        VBox titleBox = new VBox(2);
        Label title = new Label("Licenças e Activação");
        title.getStyleClass().add("kubata-licenses-title");

        Label subtitle = new Label(
                "Centro de controlo das licenças dos módulos, validade, activação e conformidade do ambiente Kubata."
        );
        subtitle.getStyleClass().add("kubata-licenses-subtitle");
        subtitle.setWrapText(true);
        titleBox.getChildren().addAll(title, subtitle);

        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label users = new Label();
        users.getStyleClass().add("kubata-licenses-header-meta");
        users.setText("Utilizadores: " + userRepository.count());

        Button refresh = new Button(
                "Actualizar",
                IconUtils.icon(Feather.REFRESH_CW, 13)
        );
        refresh.getStyleClass().add("button-outlined");
        refresh.setOnAction(e -> reload());

        btnHeaderConfigure = new Button(
                "Configurar licença",
                IconUtils.icon(Feather.KEY, 13)
        );
        btnHeaderConfigure.getStyleClass().add("button-primary");
        btnHeaderConfigure.setOnAction(e ->
                selectedModulo().ifPresent(this::showLicenseWizard)
        );

        titleLine.getChildren().addAll(icon, titleBox, spacer, users, refresh, btnHeaderConfigure);

        totalValue = new Label("0");
        licensedValue = new Label("0");
        expiringValue = new Label("0");
        expiredValue = new Label("0");

        HBox kpis = new HBox(10,
                kpi("MÓDULOS", Feather.PACKAGE, totalValue),
                kpi("COM LICENÇA", Feather.CHECK_CIRCLE, licensedValue),
                kpi("A EXPIRAR", Feather.CLOCK, expiringValue),
                kpi("EXPIRADAS", Feather.ALERT_TRIANGLE, expiredValue)
        );

        header.getChildren().addAll(titleLine, kpis);
        return header;
    }

    private VBox kpi(String title, Feather icon, Label value) {
        VBox card = new VBox(2);
        card.getStyleClass().add("kubata-licenses-kpi");
        card.setPadding(new Insets(10, 14, 10, 14));
        card.setMinWidth(160);

        HBox line = new HBox(7);
        line.setAlignment(Pos.CENTER_LEFT);

        Label i = new Label("", IconUtils.icon(icon, 14));
        i.getStyleClass().add("kubata-licenses-kpi-icon");

        Label t = new Label(title);
        t.getStyleClass().add("kubata-licenses-kpi-title");

        line.getChildren().addAll(i, t);
        value.getStyleClass().add("kubata-licenses-kpi-value");

        card.getChildren().addAll(line, value);
        return card;
    }

    private HBox buildFilters() {
        HBox bar = new HBox(8);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(9, 14, 9, 14));
        bar.getStyleClass().add("kubata-licenses-filter-bar");

        searchField = new TextField();
        searchField.setPromptText("Pesquisar módulo, código ou versão...");
        searchField.setPrefWidth(320);
        searchField.textProperty().addListener((obs, old, value) -> applyFilters());

        stateFilter = new ComboBox<>(FXCollections.observableArrayList(
                "Todos", "Com licença", "Sem licença", "A expirar", "Expirada", "Activos", "Inactivos"
        ));
        stateFilter.setValue("Todos");
        stateFilter.setPrefWidth(150);
        stateFilter.valueProperty().addListener((obs, old, value) -> applyFilters());

        Button clear = new Button(
                "Limpar filtros",
                IconUtils.icon(Feather.X, 12)
        );
        clear.getStyleClass().add("button-outlined");
        clear.setOnAction(e -> {
            searchField.clear();
            stateFilter.setValue("Todos");
        });

        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label hint = new Label("Duplo clique abre o assistente de licenciamento");
        hint.getStyleClass().add("kubata-licenses-filter-hint");

        bar.getChildren().addAll(searchField, stateFilter, clear, spacer, hint);
        return bar;
    }

    private SplitPane buildMainArea() {
        table = buildTable();

        VBox details = buildDetails();
        ScrollPane detailScroll = new ScrollPane(details);
        detailScroll.setFitToWidth(true);
        detailScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        detailScroll.getStyleClass().add("kubata-licenses-details-wrapper");

        SplitPane split = new SplitPane(new StackPane(table), detailScroll);
        split.setDividerPositions(0.72);
        split.getItems().get(0).getStyleClass().add("kubata-licenses-table-pane");
        return split;
    }

    private AdvancedTableView<ModuloSistema> buildTable() {
        AdvancedTableView<ModuloSistema> tv = new AdvancedTableView<>();
        tv.setData(modulos);
        tv.setEntityName("Módulo");
        tv.setPlaceholder(new Label("Nenhum módulo corresponde aos filtros."));
        TableUtils.standardize(tv);

        TableColumn<ModuloSistema, String> module = TableUtils.createTextColumn(
                "Módulo",
                c -> new SimpleStringProperty(safe(c.getValue().getNome(), c.getValue().getCodigo()))
        );
        module.setPrefWidth(205);

        TableColumn<ModuloSistema, String> code = TableUtils.createTextColumn(
                "Código",
                c -> new SimpleStringProperty(safe(c.getValue().getCodigo(), "—"))
        );
        code.setPrefWidth(95);

        TableColumn<ModuloSistema, String> version = TableUtils.createTextColumn(
                "Versão",
                c -> new SimpleStringProperty(safe(c.getValue().getVersao(), "—"))
        );
        version.setPrefWidth(90);

        TableColumn<ModuloSistema, String> runtime = new TableColumn<>("Runtime");
        runtime.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getEstado() == null ? "Desconhecido" : c.getValue().getEstado().toString()
        ));
        runtime.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                Label badge = new Label(item);
                badge.getStyleClass().addAll("kubata-license-badge", runtimeBadgeClass(item));
                setGraphic(badge);
                setText(null);
                setAlignment(Pos.CENTER);
            }
        });
        runtime.setPrefWidth(115);

        TableColumn<ModuloSistema, String> license = new TableColumn<>("Licença");
        license.setCellValueFactory(c ->
                new SimpleStringProperty(licenseStatus(c.getValue()))
        );
        license.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                Label badge = new Label(item);
                badge.getStyleClass().addAll("kubata-license-badge", licenseBadgeClass(item));
                setGraphic(badge);
                setText(null);
                setAlignment(Pos.CENTER);
            }
        });
        license.setPrefWidth(125);

        TableColumn<ModuloSistema, String> validity = TableUtils.createTextColumn(
                "Validade",
                c -> new SimpleStringProperty(formatValidity(c.getValue()))
        );
        validity.setPrefWidth(105);

        TableColumn<ModuloSistema, String> remaining = TableUtils.createTextColumn(
                "Prazo",
                c -> new SimpleStringProperty(daysRemaining(c.getValue()))
        );
        remaining.setPrefWidth(95);

        TableColumn<ModuloSistema, String> action = TableUtils.createTextColumn(
                "Acção",
                c -> new SimpleStringProperty(
                        hasLicense(c.getValue()) ? "Gerir licença" : "Activar licença"
                )
        );
        action.setPrefWidth(120);

        tv.getColumns().addAll(module, code, version, runtime, license, validity, remaining, action);

        tv.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, selected) -> updateDetails(selected));

        tv.setOnEdit(this::showLicenseWizard);
        tv.setOnViewDetails(this::showLicenseDetails);
        tv.setOnRefresh(this::reload);
        tv.setRowFactory(tableView -> {
            TableRow<ModuloSistema> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    showLicenseWizard(row.getItem());
                }
            });
            return row;
        });

        return tv;
    }

    private VBox buildDetails() {
        VBox details = new VBox(13);
        details.setPadding(new Insets(16));
        details.getStyleClass().add("kubata-licenses-details");

        HBox identity = new HBox(10);
        identity.setAlignment(Pos.CENTER_LEFT);

        StackPane iconBox = new StackPane();
        iconBox.getStyleClass().add("kubata-licenses-detail-icon");
        iconBox.getChildren().add(
                new Label("", IconUtils.icon(Feather.KEY, 17))
        );

        VBox text = new VBox(2);
        detailModule = new Label("Nenhum módulo seleccionado");
        detailModule.getStyleClass().add("kubata-licenses-detail-title");

        detailDescription = new Label("Seleccione um módulo para consultar o estado de licenciamento.");
        detailDescription.getStyleClass().add("kubata-licenses-detail-subtitle");
        detailDescription.setWrapText(true);

        text.getChildren().addAll(detailModule, detailDescription);
        identity.getChildren().addAll(iconBox, text);

        detailState = detailValue(details, "ESTADO DO RUNTIME", "—");
        detailVersion = detailValue(details, "VERSÃO", "—");
        detailLicenseState = detailValue(details, "ESTADO DA LICENÇA", "—");
        detailValidity = detailValue(details, "VALIDADE", "—");
        detailKey = detailValue(details, "CHAVE", "—");
        detailInstallation = detailValue(details, "INSTALAÇÃO", "—");
        detailRequired = detailValue(details, "MÓDULO OBRIGATÓRIO", "—");

        VBox actions = new VBox(7);
        Label actionTitle = new Label("Operações");
        actionTitle.getStyleClass().add("kubata-licenses-section-title");

        btnConfigure = profileAction("Configurar licença", Feather.KEY);
        btnClearLicense = profileAction("Limpar licença", Feather.TRASH_2);
        btnClearLicense.getStyleClass().add("button-danger-outlined");

        btnConfigure.setOnAction(e -> selectedModulo().ifPresent(this::showLicenseWizard));
        btnClearLicense.setOnAction(e -> selectedModulo().ifPresent(this::clearLicense));

        actions.getChildren().addAll(actionTitle, btnConfigure, btnClearLicense);
        details.getChildren().addAll(identity, new Separator(), actions);

        VBox governance = new VBox(7);
        Label governanceTitle = new Label("Política");
        governanceTitle.getStyleClass().add("kubata-licenses-section-title");

        Label governanceText = new Label(
                "A licença é guardada no registo do módulo. Uma validade vazia representa uma licença sem data de expiração registada. "
                        + "A configuração não descarrega ficheiros nem altera o catálogo de módulos."
        );
        governanceText.setWrapText(true);
        governanceText.getStyleClass().add("kubata-licenses-policy-text");
        governance.getChildren().addAll(governanceTitle, governanceText);

        details.getChildren().add(governance);
        updateDetails(null);
        return details;
    }

    private Label detailValue(VBox target, String title, String initial) {
        VBox row = new VBox(2);
        row.getStyleClass().add("kubata-licenses-detail-row");

        Label label = new Label(title);
        label.getStyleClass().add("kubata-licenses-detail-label");

        Label value = new Label(initial);
        value.setWrapText(true);
        value.getStyleClass().add("kubata-licenses-detail-value");

        row.getChildren().addAll(label, value);
        target.getChildren().add(row);
        return value;
    }

    private Button profileAction(String text, Feather icon) {
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
        bar.getStyleClass().add("kubata-licenses-statusbar");

        Label status = new Label("Licenciamento");
        status.getStyleClass().add("kubata-licenses-status-text");

        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label hint = new Label("Chaves e validade por módulo");
        hint.getStyleClass().add("kubata-licenses-status-hint");

        bar.getChildren().addAll(status, spacer, hint);
        return bar;
    }

    private void reload() {
        persistenceService.executeAsync(
                () -> moduloRepository.findAll(),
                "LICENSES_REFRESH",
                "LICENCAS",
                "Actualização da central de licenciamento",
                () -> Platform.runLater(() -> {
                    modulos.setAll(moduloRepository.findAll());
                    applyFilters();
                    updateKpis();
                    updateDetails(table == null ? null : table.getSelectionModel().getSelectedItem());
                    refreshPermissions();
                })
        );
    }

    private void applyFilters() {
        if (table == null) {
            return;
        }

        String q = searchField == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        String filter = stateFilter == null ? "Todos" : stateFilter.getValue();

        table.setFilter(module -> {
            if (module == null) return false;

            boolean textMatch = q.isBlank()
                    || contains(module.getNome(), q)
                    || contains(module.getCodigo(), q)
                    || contains(module.getVersao(), q);

            boolean stateMatch = switch (filter) {
                case "Com licença" -> hasLicense(module);
                case "Sem licença" -> !hasLicense(module);
                case "A expirar" -> isExpiring(module);
                case "Expirada" -> isExpired(module);
                case "Activos" -> module.getEstado() == ModuloSistema.EstadoModulo.ACTIVO;
                case "Inactivos" -> module.getEstado() == ModuloSistema.EstadoModulo.INACTIVO;
                default -> true;
            };

            return textMatch && stateMatch;
        });
    }

    private void updateKpis() {
        long total = modulos.size();
        long licensed = modulos.stream().filter(this::hasLicense).count();
        long expiring = modulos.stream().filter(this::isExpiring).count();
        long expired = modulos.stream().filter(this::isExpired).count();

        totalValue.setText(String.valueOf(total));
        licensedValue.setText(String.valueOf(licensed));
        expiringValue.setText(String.valueOf(expiring));
        expiredValue.setText(String.valueOf(expired));
    }

    private void updateDetails(ModuloSistema module) {
        if (detailModule == null) return;

        if (module == null) {
            detailModule.setText("Nenhum módulo seleccionado");
            detailDescription.setText("Seleccione um módulo para consultar o estado de licenciamento.");
            detailState.setText("—");
            detailVersion.setText("—");
            detailLicenseState.setText("—");
            detailValidity.setText("—");
            detailKey.setText("—");
            detailInstallation.setText("—");
            detailRequired.setText("—");
        } else {
            detailModule.setText(safe(module.getNome(), module.getCodigo()));
            detailDescription.setText(safe(module.getDescricao(), "Sem descrição disponível."));
            detailState.setText(module.getEstado() == null ? "Desconhecido" : module.getEstado().toString());
            detailVersion.setText(safe(module.getVersao(), "—"));
            detailLicenseState.setText(licenseStatus(module));
            detailValidity.setText(module.getLicencaValidade() == null
                    ? "Sem expiração registada"
                    : module.getLicencaValidade().format(DATE_TIME_FMT));
            detailKey.setText(maskLicenseKey(module.getLicencaChave()));
            detailInstallation.setText(module.getInstaladoEm() == null
                    ? "Não instalado"
                    : module.getInstaladoEm().format(DATE_TIME_FMT));
            detailRequired.setText(Boolean.TRUE.equals(module.getObrigatorio()) ? "Sim" : "Não");
        }

        refreshPermissions();
    }

    private void refreshPermissions() {
        if (btnConfigure == null) return;

        boolean canEdit = hasEditPermission();
        btnHeaderConfigure.setDisable(!canEdit);
        btnConfigure.setDisable(!canEdit);

        boolean canClear = canEdit && selectedModulo().isPresent() && hasLicense(selectedModulo().orElse(null));
        btnClearLicense.setDisable(!canClear);

        if (table != null) {
            table.setOnEdit(canEdit ? this::showLicenseWizard : this::showLicenseDetails);
        }
    }

    private void showLicenseDetails(ModuloSistema module) {
        if (module == null) return;

        VBox content = new VBox(10);
        content.setPadding(new Insets(8));

        Label intro = new Label(
                safe(module.getNome(), "Módulo") + " · " + licenseStatus(module)
        );
        intro.getStyleClass().add("kubata-licenses-modal-title");

        content.getChildren().addAll(
                infoCard("Código", safe(module.getCodigo(), "—")),
                infoCard("Versão", safe(module.getVersao(), "—")),
                infoCard("Estado", module.getEstado() == null ? "—" : module.getEstado().toString()),
                infoCard("Validade", module.getLicencaValidade() == null
                        ? "Sem expiração registada"
                        : module.getLicencaValidade().format(DATE_TIME_FMT)),
                infoCard("Chave", maskLicenseKey(module.getLicencaChave()))
        );

        modalManager.showModal(
                content,
                new ModalManager.ModalConfig()
                        .size(620, 520)
                        .minSize(560, 460)
                        .title("Detalhes da licença")
                        .icon(Feather.KEY)
        );
    }

    private VBox infoCard(String title, String value) {
        VBox card = new VBox(3);
        card.getStyleClass().add("kubata-licenses-info-card");

        Label t = new Label(title.toUpperCase(Locale.ROOT));
        t.getStyleClass().add("kubata-licenses-info-label");

        Label v = new Label(value);
        v.setWrapText(true);
        v.getStyleClass().add("kubata-licenses-info-value");

        card.getChildren().addAll(t, v);
        return card;
    }

    private void showLicenseWizard(ModuloSistema module) {
        if (module == null) {
            modalManager.alert("Módulo não seleccionado", "Seleccione um módulo para configurar.", "warning", null);
            return;
        }

        if (!hasEditPermission()) {
            modalManager.alert(
                    "Acesso negado",
                    "Não possui permissão para configurar licenças.",
                    "warning",
                    null
            );
            return;
        }

        VBox pageModule = buildLicenseModulePage(module);
        VBox pageLicense = buildLicenseDataPage(module);
        VBox pageReview = buildLicenseReviewPage(module, pageLicense);

        List<Node> pages = List.of(pageModule, pageLicense, pageReview);
        pages.forEach(page -> {
            page.setVisible(false);
            page.setManaged(false);
        });
        pageModule.setVisible(true);
        pageModule.setManaged(true);

        HBox stepper = buildLicenseStepper();

        Label title = new Label();
        title.getStyleClass().add("kubata-license-wizard-title");

        Label hint = new Label();
        hint.getStyleClass().add("kubata-license-wizard-hint");
        hint.setWrapText(true);

        Button cancel = new Button("Cancelar", IconUtils.icon(Feather.X, 12));
        cancel.getStyleClass().add("button-outlined");
        cancel.setOnAction(e -> modalManager.hideModal());

        Button back = new Button("Anterior", IconUtils.icon(Feather.CHEVRON_LEFT, 12));
        back.getStyleClass().add("button-outlined");

        Button next = new Button("Continuar", IconUtils.icon(Feather.CHEVRON_RIGHT, 12));
        next.getStyleClass().add("button-primary");

        Button save = new Button(
                hasLicense(module) ? "Guardar alterações" : "Activar licença",
                IconUtils.icon(Feather.CHECK, 12)
        );
        save.getStyleClass().add("button-primary");
        save.setVisible(false);
        save.setManaged(false);

        int[] step = {0};

        Runnable refresh = () -> {
            for (int i = 0; i < pages.size(); i++) {
                boolean visible = i == step[0];
                pages.get(i).setVisible(visible);
                pages.get(i).setManaged(visible);
            }

            String[] titles = {
                    "1. Módulo e contexto",
                    "2. Chave e validade",
                    "3. Revisão e aplicação"
            };
            String[] hints = {
                    "Confirme o módulo antes de alterar os dados de licenciamento.",
                    "Introduza a chave e defina a política de validade.",
                    "Revise o resultado e só depois grave no ambiente."
            };

            title.setText(titles[step[0]]);
            hint.setText(hints[step[0]]);

            for (int i = 0; i < stepper.getChildren().size(); i++) {
                Node node = stepper.getChildren().get(i);
                node.getStyleClass().remove("kubata-license-wizard-step-active");
                if (i == step[0]) {
                    node.getStyleClass().add("kubata-license-wizard-step-active");
                }
            }

            back.setDisable(step[0] == 0);
            next.setVisible(step[0] < pages.size() - 1);
            next.setManaged(step[0] < pages.size() - 1);
            save.setVisible(step[0] == pages.size() - 1);
            save.setManaged(step[0] == pages.size() - 1);
        };

        next.setOnAction(e -> {
            if (!validateLicenseStep(step[0], pageLicense, module)) {
                return;
            }
            step[0]++;
            refreshLicenseReview(pageReview, module, pageLicense);
            refresh.run();
        });

        back.setOnAction(e -> {
            if (step[0] > 0) {
                step[0]--;
                refresh.run();
            }
        });

        save.setOnAction(e -> {
            if (!validateLicenseStep(1, pageLicense, module)) {
                return;
            }
            saveLicense(module, pageLicense);
        });

        HBox footer = new HBox(8, cancel, new Pane(), back, next, save);
        HBox.setHgrow(footer.getChildren().get(1), Priority.ALWAYS);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.getStyleClass().add("kubata-license-wizard-footer");

        VBox content = new VBox(10, stepper, title, hint);
        StackPane pageContainer = new StackPane();
        pageContainer.setMinHeight(430);
        pageContainer.getChildren().addAll(pages);
        VBox.setVgrow(pageContainer, Priority.ALWAYS);
        content.getChildren().addAll(pageContainer, footer);
        content.setPadding(new Insets(4));

        modalManager.showModal(
                content,
                new ModalManager.ModalConfig()
                        .size(920, 680)
                        .minSize(780, 580)
                        .maxSize(1200, 820)
                        .maximizable(true)
                        .minimizable(true)
                        .windowControls(true)
                        .closeOnOverlayClick(false)
                        .title(hasLicense(module)
                                ? "Assistente de Licenciamento — " + safe(module.getNome(), "")
                                : "Assistente de Activação — " + safe(module.getNome(), ""))
                        .icon(Feather.KEY)
        );

        refresh.run();
    }

    private VBox buildLicenseModulePage(ModuloSistema module) {
        VBox page = wizardPage();

        VBox intro = wizardSection(
                "Módulo seleccionado",
                "Os dados abaixo são informativos. O assistente actua apenas sobre a configuração de licenciamento deste módulo."
        );

        VBox card = new VBox(9);
        card.getStyleClass().add("kubata-license-module-card");

        HBox top = new HBox(10);
        top.setAlignment(Pos.CENTER_LEFT);

        Label icon = new Label("", IconUtils.icon(Feather.PACKAGE, 20));
        icon.getStyleClass().add("kubata-license-module-icon");

        VBox text = new VBox(2);
        Label name = new Label(safe(module.getNome(), module.getCodigo()));
        name.getStyleClass().add("kubata-license-module-name");

        Label code = new Label(
                "Código: " + safe(module.getCodigo(), "—")
                        + "   ·   Versão: " + safe(module.getVersao(), "—")
        );
        code.getStyleClass().add("kubata-license-module-meta");

        text.getChildren().addAll(name, code);
        top.getChildren().addAll(icon, text);

        card.getChildren().addAll(
                top,
                new Separator(),
                reviewRow("Descrição", safe(module.getDescricao(), "Sem descrição")),
                reviewRow("Runtime", module.getEstado() == null ? "Desconhecido" : module.getEstado().toString()),
                reviewRow("Instalado em", module.getInstaladoEm() == null
                        ? "Não instalado"
                        : module.getInstaladoEm().format(DATE_TIME_FMT)),
                reviewRow("Módulo obrigatório", Boolean.TRUE.equals(module.getObrigatorio()) ? "Sim" : "Não")
        );

        page.getChildren().addAll(intro, card);
        return page;
    }

    private VBox buildLicenseDataPage(ModuloSistema module) {
        VBox page = wizardPage();

        VBox section = wizardSection(
                "Dados da licença",
                "Registe a chave fornecida para este módulo. A data de validade é opcional."
        );

        TextField keyField = new TextField(safe(module.getLicencaChave(), ""));
        keyField.setPromptText("Cole aqui a chave de licença...");
        keyField.setId("license-key");

        ToggleGroup validityGroup = new ToggleGroup();
        RadioButton perpetual = new RadioButton("Licença sem data de expiração");
        perpetual.setToggleGroup(validityGroup);

        RadioButton dated = new RadioButton("Licença com data de expiração");
        dated.setToggleGroup(validityGroup);

        DatePicker expiry = new DatePicker(
                module.getLicencaValidade() == null
                        ? null
                        : module.getLicencaValidade().toLocalDate()
        );
        expiry.setDisable(module.getLicencaValidade() == null);
        expiry.setPrefWidth(180);

        if (module.getLicencaValidade() == null) {
            perpetual.setSelected(true);
        } else {
            dated.setSelected(true);
        }

        dated.selectedProperty().addListener((obs, old, selected) -> expiry.setDisable(!selected));

        VBox validity = new VBox(7);
        validity.getStyleClass().add("kubata-license-validity-box");
        validity.getChildren().addAll(
                new Label("Política de validade"),
                perpetual,
                dated,
                expiry
        );

        section.getChildren().addAll(
                fieldLabel("Chave de licença", true),
                keyField,
                fieldHint("A chave é armazenada no registo administrativo do módulo."),
                validity
        );

        VBox security = infoCard(
                "Segurança",
                "A aplicação não presume que a chave é válida perante um servidor externo. "
                        + "Este ecrã regista a licença no Kubata e controla a informação de validade guardada localmente."
        );

        page.getChildren().addAll(section, security);
        page.setUserData(new Object[]{keyField, dated, expiry});
        return page;
    }

    private VBox buildLicenseReviewPage(ModuloSistema module, VBox pageLicense) {
        VBox page = wizardPage();
        refreshLicenseReview(page, module, pageLicense);
        return page;
    }

    private void refreshLicenseReview(VBox page, ModuloSistema module, VBox pageLicense) {
        page.getChildren().clear();
        page.setPadding(new Insets(14));

        Object[] data = (Object[]) pageLicense.getUserData();
        TextField key = (TextField) data[0];
        RadioButton dated = (RadioButton) data[1];
        DatePicker expiry = (DatePicker) data[2];

        VBox summary = wizardSection(
                "Confirmação",
                "Confira a informação que será guardada para este módulo."
        );

        summary.getChildren().addAll(
                reviewRow("Módulo", safe(module.getNome(), module.getCodigo())),
                reviewRow("Código", safe(module.getCodigo(), "—")),
                reviewRow("Versão", safe(module.getVersao(), "—")),
                reviewRow("Chave", maskLicenseKey(key.getText())),
                reviewRow(
                        "Validade",
                        dated.isSelected() && expiry.getValue() != null
                                ? expiry.getValue().format(DATE_FMT)
                                : "Sem expiração registada"
                )
        );

        VBox warning;
        if (!key.getText().trim().isBlank() && dated.isSelected()
                && expiry.getValue() != null && expiry.getValue().isBefore(LocalDate.now())) {
            warning = infoCard(
                    "Data já expirada",
                    "A data escolhida já passou. A informação será guardada como expirada."
            );
            warning.getStyleClass().add("kubata-license-warning-danger");
        } else {
            warning = infoCard(
                    "Pronto para aplicação",
                    "A gravação actualiza apenas a chave e a validade do módulo seleccionado."
            );
        }

        page.getChildren().addAll(summary, warning);
    }

    private boolean validateLicenseStep(int step, VBox pageLicense, ModuloSistema module) {
        if (step != 1) {
            return true;
        }

        Object[] data = (Object[]) pageLicense.getUserData();
        TextField key = (TextField) data[0];
        RadioButton dated = (RadioButton) data[1];
        DatePicker expiry = (DatePicker) data[2];

        if (key.getText() == null || key.getText().trim().isBlank()) {
            modalManager.alert(
                    "Chave obrigatória",
                    "Introduza a chave de licença antes de continuar.",
                    "warning",
                    null
            );
            return false;
        }

        if (dated.isSelected() && expiry.getValue() == null) {
            modalManager.alert(
                    "Validade obrigatória",
                    "Seleccione a data de expiração ou escolha uma licença sem expiração.",
                    "warning",
                    null
            );
            return false;
        }

        if (expiry.getValue() != null && expiry.getValue().isBefore(LocalDate.now().minusYears(20))) {
            modalManager.alert(
                    "Data inválida",
                    "A data de validade indicada não é plausível para uma licença.",
                    "warning",
                    null
            );
            return false;
        }

        return true;
    }

    private void saveLicense(ModuloSistema module, VBox pageLicense) {
        Object[] data = (Object[]) pageLicense.getUserData();
        TextField keyField = (TextField) data[0];
        RadioButton dated = (RadioButton) data[1];
        DatePicker expiry = (DatePicker) data[2];

        String key = keyField.getText().trim();
        LocalDateTime validity = dated.isSelected() && expiry.getValue() != null
                ? expiry.getValue().atTime(23, 59, 59)
                : null;

        module.setLicencaChave(key);
        module.setLicencaValidade(validity);

        persistenceService.saveAsync(
                moduloRepository,
                module,
                "MODULO_SISTEMA",
                "Configuração de licença do módulo " + safe(module.getCodigo(), "—"),
                saved -> Platform.runLater(() -> {
                    modalManager.hideModal();
                    reload();
                    modalManager.success(
                            "Licença guardada",
                            "A licença do módulo " + safe(saved.getNome(), saved.getCodigo())
                                    + " foi actualizada com sucesso."
                    );
                })
        );
    }

    private void clearLicense(ModuloSistema module) {
        if (module == null || !hasLicense(module)) return;

        if (!hasEditPermission()) {
            modalManager.alert("Acesso negado", "Não possui permissão para alterar licenças.", "warning", null);
            return;
        }

        modalManager.showConfirm(
                "Limpar licença",
                "Confirma a remoção da chave e da validade registadas para "
                        + safe(module.getNome(), module.getCodigo()) + "?",
                () -> {
                    module.setLicencaChave(null);
                    module.setLicencaValidade(null);

                    persistenceService.saveAsync(
                            moduloRepository,
                            module,
                            "MODULO_SISTEMA",
                            "Remoção da licença do módulo " + safe(module.getCodigo(), "—"),
                            saved -> Platform.runLater(() -> {
                                reload();
                                modalManager.success(
                                        "Licença removida",
                                        "Os dados de licenciamento foram limpos."
                                );
                            })
                    );
                }
        );
    }

    private HBox reviewRow(String label, String value) {
        HBox row = new HBox(10);
        row.getStyleClass().add("kubata-license-review-row");
        row.setAlignment(Pos.CENTER_LEFT);

        Label l = new Label(label);
        l.getStyleClass().add("kubata-license-review-label");
        l.setMinWidth(125);

        Label v = new Label(value);
        v.setWrapText(true);
        v.getStyleClass().add("kubata-license-review-value");

        row.getChildren().addAll(l, v);
        HBox.setHgrow(v, Priority.ALWAYS);
        return row;
    }

    private HBox fieldLabel(String label, boolean required) {
        Label l = new Label(required ? label + " *" : label);
        l.getStyleClass().add("kubata-license-field-label");
        return new HBox(l);
    }

    private Label fieldHint(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("kubata-license-field-hint");
        label.setWrapText(true);
        return label;
    }

    private VBox wizardPage() {
        VBox page = new VBox(12);
        page.setPadding(new Insets(2));
        page.getStyleClass().add("kubata-license-wizard-page");
        return page;
    }

    private VBox wizardSection(String title, String description) {
        VBox section = new VBox(7);
        section.getStyleClass().add("kubata-license-wizard-section");

        Label t = new Label(title);
        t.getStyleClass().add("kubata-license-wizard-section-title");

        Label d = new Label(description);
        d.setWrapText(true);
        d.getStyleClass().add("kubata-license-wizard-section-description");

        section.getChildren().addAll(t, d);
        return section;
    }

    private HBox buildLicenseStepper() {
        HBox steps = new HBox(7);
        steps.getStyleClass().add("kubata-license-wizard-stepper");

        for (String title : List.of(
                "1 · Módulo",
                "2 · Chave e validade",
                "3 · Revisão"
        )) {
            Label label = new Label(title);
            label.getStyleClass().add("kubata-license-wizard-step");
            steps.getChildren().add(label);
        }
        return steps;
    }

    private String licenseStatus(ModuloSistema module) {
        if (!hasLicense(module)) return "Sem licença";
        if (isExpired(module)) return "Expirada";
        if (isExpiring(module)) return "A expirar";
        return "Válida";
    }

    private String daysRemaining(ModuloSistema module) {
        if (module == null || module.getLicencaValidade() == null) {
            return hasLicense(module) ? "Sem limite" : "—";
        }

        long days = ChronoUnit.DAYS.between(
                LocalDate.now(),
                module.getLicencaValidade().toLocalDate()
        );

        if (days < 0) return "Expirada";
        if (days == 0) return "Hoje";
        return days + " dia(s)";
    }

    private boolean hasLicense(ModuloSistema module) {
        return module != null
                && module.getLicencaChave() != null
                && !module.getLicencaChave().isBlank();
    }

    private boolean isExpiring(ModuloSistema module) {
        if (module == null || module.getLicencaValidade() == null) return false;
        LocalDate expiry = module.getLicencaValidade().toLocalDate();
        LocalDate today = LocalDate.now();
        return !expiry.isBefore(today) && !expiry.isAfter(today.plusDays(ALERT_DAYS));
    }

    private boolean isExpired(ModuloSistema module) {
        return module != null
                && module.getLicencaValidade() != null
                && module.getLicencaValidade().isBefore(LocalDateTime.now());
    }

    private String formatValidity(ModuloSistema module) {
        if (module == null || module.getLicencaValidade() == null) return "—";
        return module.getLicencaValidade().toLocalDate().format(DATE_FMT);
    }

    private String maskLicenseKey(String key) {
        if (key == null || key.isBlank()) return "Não registada";
        String normalized = key.trim();
        if (normalized.length() <= 8) {
            return "••••••••";
        }
        return normalized.substring(0, 4)
                + "••••••••"
                + normalized.substring(normalized.length() - 4);
    }

    private String licenseBadgeClass(String status) {
        return switch (status) {
            case "Válida" -> "kubata-license-badge-success";
            case "A expirar" -> "kubata-license-badge-warning";
            case "Expirada" -> "kubata-license-badge-danger";
            default -> "kubata-license-badge-neutral";
        };
    }

    private String runtimeBadgeClass(String status) {
        return switch (status == null ? "" : status.toLowerCase(Locale.ROOT)) {
            case "activo" -> "kubata-license-badge-success";
            case "inactivo", "erro" -> "kubata-license-badge-danger";
            case "disponível", "actualização pendente" -> "kubata-license-badge-warning";
            default -> "kubata-license-badge-neutral";
        };
    }

    private Optional<ModuloSistema> selectedModulo() {
        return table == null
                ? java.util.Optional.empty()
                : java.util.Optional.ofNullable(table.getSelectionModel().getSelectedItem());
    }

    private boolean hasEditPermission() {
        User user = sessionManager.getUser();
        if (user == null) return false;

        if (user.isSuperadmin() || user.getRole() == Role.ADMIN) {
            return true;
        }

        return securityService.hasPermission(
                user,
                "ADMINISTRATOR",
                "LICENCAS",
                ao.allon.kubata.core.domain.PermissaoPerfil.Operacao.EDITAR
        );
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }

    private String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}

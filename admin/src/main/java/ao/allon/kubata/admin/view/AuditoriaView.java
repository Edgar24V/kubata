package ao.allon.kubata.admin.view;

import ao.allon.kubata.admin.service.SessionManager;
import ao.allon.kubata.admin.ui.modal.ModalManager;
import ao.allon.kubata.admin.ui.util.IconUtils;
import ao.allon.kubata.core.domain.AuditLog;
import ao.allon.kubata.core.domain.Role;
import ao.allon.kubata.core.domain.User;
import ao.allon.kubata.core.repository.AuditLogRepository;
import ao.allon.kubata.core.service.AcessoService;
import ao.allon.kubata.core.ui.table.AdvancedTableView;
import ao.allon.kubata.core.ui.table.TableUtils;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Component
public class AuditoriaView extends VBox {

    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final AuditLogRepository auditLogRepository;
    private final SessionManager sessionManager;
    private final ModalManager modalManager;
    private final AcessoService acessoService;

    private final ObservableList<AuditLog> logs = FXCollections.observableArrayList();

    private AdvancedTableView<AuditLog> table;
    private TextField searchField;
    private TextField moduleField;
    private ComboBox<String> actionFilter;
    private ComboBox<String> entityFilter;
    private ComboBox<String> resultFilter;
    private ComboBox<String> complianceFilter;
    private ComboBox<String> saftFilter;
    private DatePicker fromDate;
    private DatePicker toDate;

    private Label totalValue;
    private Label failedValue;
    private Label saftValue;
    private Label criticalValue;
    private Label todayValue;
    private Label statusText;

    private Button detailsButton;
    private Button exportButton;

    private boolean dataLoaded;

    public AuditoriaView(
            AuditLogRepository auditLogRepository,
            SessionManager sessionManager,
            ModalManager modalManager,
            AcessoService acessoService
    ) {
        this.auditLogRepository = auditLogRepository;
        this.sessionManager = sessionManager;
        this.modalManager = modalManager;
        this.acessoService = acessoService;

        setSpacing(0);
        setPadding(Insets.EMPTY);
        getStyleClass().add("kubata-audit-page");
        buildUi();
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        if (!dataLoaded && getScene() != null) {
            dataLoaded = true;
            refreshPermissions();
            loadLogs();
        }
    }

    private void buildUi() {
        VBox header = buildHeader();
        HBox filters = buildFilters();

        table = buildTable();
        VBox.setVgrow(table, Priority.ALWAYS);

        HBox footer = buildFooter();
        getChildren().addAll(header, filters, table, footer);
    }

    private VBox buildHeader() {
        VBox header = new VBox(12);
        header.setPadding(new Insets(18, 22, 14, 22));
        header.getStyleClass().add("kubata-audit-header");

        HBox top = new HBox(12);
        top.setAlignment(Pos.CENTER_LEFT);

        StackPane iconBox = new StackPane();
        iconBox.getStyleClass().add("kubata-audit-title-icon");
        iconBox.getChildren().add(new Label("", IconUtils.icon(Feather.SHIELD, 22)));

        VBox titleBox = new VBox(2);
        Label title = new Label("Auditoria Geral");
        title.getStyleClass().add("kubata-audit-title");

        Label subtitle = new Label(
                "Trilha transversal de segurança, operações, fiscalidade e alterações realizadas no Kubata."
        );
        subtitle.setWrapText(true);
        subtitle.getStyleClass().add("kubata-audit-subtitle");
        titleBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label readOnly = new Label("TRILHA PROTEGIDA · SOMENTE LEITURA");
        readOnly.getStyleClass().add("kubata-audit-protected-badge");

        detailsButton = new Button(
                "Ver detalhe",
                IconUtils.icon(Feather.SEARCH, 13)
        );
        detailsButton.getStyleClass().add("button-outlined");
        detailsButton.setOnAction(e -> showDetails());

        exportButton = new Button(
                "Exportar CSV",
                IconUtils.icon(Feather.DOWNLOAD, 13)
        );
        exportButton.getStyleClass().add("button-primary");
        exportButton.setOnAction(e -> exportCsv());

        top.getChildren().addAll(
                iconBox, titleBox, spacer, readOnly, detailsButton, exportButton
        );

        HBox kpis = new HBox(
                10,
                kpi("REGISTOS", Feather.ACTIVITY, totalValue = new Label("0")),
                kpi("FALHAS", Feather.ALERT_TRIANGLE, failedValue = new Label("0")),
                kpi("SAF-T / AGT", Feather.FILE_TEXT, saftValue = new Label("0")),
                kpi("CRÍTICOS", Feather.SHIELD, criticalValue = new Label("0")),
                kpi("HOJE", Feather.CALENDAR, todayValue = new Label("0"))
        );

        header.getChildren().addAll(top, kpis);
        return header;
    }

    private VBox kpi(String title, Feather icon, Label value) {
        VBox card = new VBox(2);
        card.setMinWidth(145);
        card.setPadding(new Insets(9, 13, 9, 13));
        card.getStyleClass().add("kubata-audit-kpi");

        HBox row = new HBox(7);
        row.setAlignment(Pos.CENTER_LEFT);

        Label iconLabel = new Label("", IconUtils.icon(icon, 13));
        iconLabel.getStyleClass().add("kubata-audit-kpi-icon");

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("kubata-audit-kpi-label");
        row.getChildren().addAll(iconLabel, titleLabel);

        value.getStyleClass().add("kubata-audit-kpi-value");
        card.getChildren().addAll(row, value);
        return card;
    }

    private HBox buildFilters() {
        HBox bar = new HBox(8);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(9, 14, 9, 14));
        bar.getStyleClass().add("kubata-audit-filterbar");

        searchField = new TextField();
        searchField.setPromptText("Pesquisar utilizador, entidade, descrição, IP...");
        searchField.setPrefWidth(285);

        moduleField = new TextField();
        moduleField.setPromptText("Módulo");
        moduleField.setPrefWidth(130);

        actionFilter = new ComboBox<>(FXCollections.observableArrayList(
                "Todas as operações",
                "CREATE", "UPDATE", "DELETE", "VIEW", "EXPORT", "IMPORT",
                "LOGIN", "LOGOUT", "PRINT", "APPROVE", "REJECT", "CANCEL",
                "BACKUP", "RESTORE", "CONFIG_CHANGE", "PERMISSION_CHANGE",
                "SAFT_EXPORT", "AGT_COMMUNICATION"
        ));
        actionFilter.setValue("Todas as operações");
        actionFilter.setPrefWidth(160);

        entityFilter = new ComboBox<>(FXCollections.observableArrayList(
                "Todas as entidades", "USER", "EMPRESA", "PERMISSION", "AUTH",
                "CONFIG", "SERIE", "FISCAL_AGT", "SAFT", "PARAMETRO_SISTEMA"
        ));
        entityFilter.setValue("Todas as entidades");
        entityFilter.setPrefWidth(155);

        resultFilter = new ComboBox<>(FXCollections.observableArrayList(
                "Todos os resultados", "Sucesso", "Falha"
        ));
        resultFilter.setValue("Todos os resultados");
        resultFilter.setPrefWidth(135);

        complianceFilter = new ComboBox<>(FXCollections.observableArrayList(
                "Todos os níveis", "CRITICAL", "HIGH", "NORMAL", "LOW"
        ));
        complianceFilter.setValue("Todos os níveis");
        complianceFilter.setPrefWidth(135);

        saftFilter = new ComboBox<>(FXCollections.observableArrayList(
                "SAF-T: Todos", "SAF-T: Sim", "SAF-T: Não"
        ));
        saftFilter.setValue("SAF-T: Todos");
        saftFilter.setPrefWidth(120);

        fromDate = new DatePicker();
        fromDate.setPromptText("De");
        fromDate.setPrefWidth(115);

        toDate = new DatePicker();
        toDate.setPromptText("Até");
        toDate.setPrefWidth(115);

        Button clear = new Button("Limpar", IconUtils.icon(Feather.X, 12));
        clear.getStyleClass().add("button-outlined");
        clear.setOnAction(e -> clearFilters());

        Button refresh = new Button("Actualizar", IconUtils.icon(Feather.REFRESH_CW, 12));
        refresh.getStyleClass().add("button-outlined");
        refresh.setOnAction(e -> loadLogs());

        searchField.textProperty().addListener((o, a, b) -> applyFilters());
        moduleField.textProperty().addListener((o, a, b) -> applyFilters());
        actionFilter.valueProperty().addListener((o, a, b) -> applyFilters());
        entityFilter.valueProperty().addListener((o, a, b) -> applyFilters());
        resultFilter.valueProperty().addListener((o, a, b) -> applyFilters());
        complianceFilter.valueProperty().addListener((o, a, b) -> applyFilters());
        saftFilter.valueProperty().addListener((o, a, b) -> applyFilters());
        fromDate.valueProperty().addListener((o, a, b) -> applyFilters());
        toDate.valueProperty().addListener((o, a, b) -> applyFilters());

        bar.getChildren().addAll(
                searchField, moduleField, actionFilter, entityFilter,
                resultFilter, complianceFilter, saftFilter,
                fromDate, toDate, clear, refresh
        );
        return bar;
    }

    private AdvancedTableView<AuditLog> buildTable() {
        AdvancedTableView<AuditLog> tv = new AdvancedTableView<>();
        tv.setData(logs);
        tv.setEntityName("Registo de auditoria");
        tv.setEditable(false);
        tv.setPlaceholder(new Label("Nenhum registo de auditoria corresponde aos filtros."));
        TableUtils.standardize(tv);

        TableColumn<AuditLog, String> date = TableUtils.createTextColumn(
                "Data / Hora",
                c -> new SimpleStringProperty(
                        c.getValue().getTimestamp() == null
                                ? "—"
                                : c.getValue().getTimestamp().format(DATE_TIME)
                )
        );
        date.setPrefWidth(155);

        TableColumn<AuditLog, String> user = TableUtils.createTextColumn(
                "Utilizador",
                c -> new SimpleStringProperty(safe(c.getValue().getUsername()))
        );
        user.setPrefWidth(150);

        TableColumn<AuditLog, String> action = new TableColumn<>("Operação");
        action.setCellValueFactory(c ->
                new SimpleStringProperty(
                        c.getValue().getActionType() == null ? "—" : c.getValue().getActionType().name()
                )
        );
        action.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    getStyleClass().removeAll(
                            "kubata-audit-action-safe",
                            "kubata-audit-action-danger",
                            "kubata-audit-action-warning"
                    );
                    return;
                }
                setGraphic(null);
                setText(item);
                getStyleClass().removeAll(
                        "kubata-audit-action-safe",
                        "kubata-audit-action-danger",
                        "kubata-audit-action-warning"
                );
                if (item.contains("DELETE") || item.contains("CANCEL")
                        || item.contains("REJECT")) {
                    getStyleClass().add("kubata-audit-action-danger");
                } else if (item.contains("CONFIG") || item.contains("PERMISSION")
                        || item.contains("RESTORE")) {
                    getStyleClass().add("kubata-audit-action-warning");
                } else {
                    getStyleClass().add("kubata-audit-action-safe");
                }
            }
        });
        action.setPrefWidth(155);

        TableColumn<AuditLog, String> entity = TableUtils.createTextColumn(
                "Entidade",
                c -> new SimpleStringProperty(safe(c.getValue().getEntityType()))
        );
        entity.setPrefWidth(125);

        TableColumn<AuditLog, String> module = TableUtils.createTextColumn(
                "Módulo",
                c -> new SimpleStringProperty(safe(c.getValue().getModule()))
        );
        module.setPrefWidth(110);

        TableColumn<AuditLog, String> description = TableUtils.createTextColumn(
                "Descrição",
                c -> new SimpleStringProperty(safe(c.getValue().getEntityDescription()))
        );
        description.setPrefWidth(320);

        TableColumn<AuditLog, String> result = new TableColumn<>("Resultado");
        result.setCellValueFactory(c -> new SimpleStringProperty(
                Boolean.TRUE.equals(c.getValue().getSuccess()) ? "Sucesso" : "Falha"
        ));
        result.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                setText(item);
                getStyleClass().removeAll(
                        "kubata-audit-result-success",
                        "kubata-audit-result-failure"
                );
                getStyleClass().add(
                        "Sucesso".equals(item)
                                ? "kubata-audit-result-success"
                                : "kubata-audit-result-failure"
                );
            }
        });
        result.setPrefWidth(90);

        TableColumn<AuditLog, String> compliance = new TableColumn<>("Nível");
        compliance.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getAgtComplianceLevel() == null
                        ? "NORMAL"
                        : c.getValue().getAgtComplianceLevel().name()
        ));
        compliance.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                Label badge = new Label(item);
                badge.getStyleClass().addAll("kubata-audit-compliance-badge", complianceClass(item));
                setGraphic(badge);
                setText(null);
                setAlignment(Pos.CENTER);
            }
        });
        compliance.setPrefWidth(95);

        TableColumn<AuditLog, String> saft = new TableColumn<>("SAF-T");
        saft.setCellValueFactory(c -> new SimpleStringProperty(
                Boolean.TRUE.equals(c.getValue().getSaftRelevant()) ? "Sim" : "Não"
        ));
        saft.setPrefWidth(75);

        TableColumn<AuditLog, String> ip = TableUtils.createTextColumn(
                "IP",
                c -> new SimpleStringProperty(safe(c.getValue().getIpAddress()))
        );
        ip.setPrefWidth(115);

        tv.getColumns().addAll(
                date, user, action, entity, module, description, result, compliance, saft, ip
        );

        tv.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, selected) -> updateActionState(selected)
        );

        tv.setOnViewDetails(selected -> showDetails(selected));
        return tv;
    }

    private HBox buildFooter() {
        HBox footer = new HBox(10);
        footer.setPadding(new Insets(7, 14, 7, 14));
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.getStyleClass().add("kubata-audit-footer");

        statusText = new Label("A carregar auditoria...");
        statusText.getStyleClass().add("kubata-audit-footer-text");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label note = new Label(
                "Auditoria protegida · sem edição/eliminação · exportação sujeita a permissão"
        );
        note.getStyleClass().add("kubata-audit-footer-text");

        footer.getChildren().addAll(statusText, spacer, note);
        return footer;
    }

    private void loadLogs() {
        if (!hasViewPermission()) {
            statusText.setText("Sem permissão para consultar a auditoria.");
            logs.clear();
            return;
        }

        table.setLoading(true);

        Thread worker = new Thread(() -> {
            try {
                List<AuditLog> result = auditLogRepository.findAll(
                        PageRequest.of(
                                0,
                                5000,
                                Sort.by(Sort.Direction.DESC, "timestamp")
                        )
                ).getContent();

                Platform.runLater(() -> {
                    logs.setAll(result);
                    table.setLoading(false);
                    updateKpis();
                    applyFilters();
                    updateActionState(table.getSelectionModel().getSelectedItem());
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    table.setLoading(false);
                    statusText.setText("Erro ao carregar auditoria: " + message(ex));
                    modalManager.alert(
                            "Erro de auditoria",
                            message(ex),
                            "error",
                            ex
                    );
                });
            }
        }, "kubata-audit-loader");

        worker.setDaemon(true);
        worker.start();
    }

    private void applyFilters() {
        if (table == null) return;

        String q = lower(searchField.getText());
        String module = lower(moduleField.getText());
        String action = actionFilter.getValue();
        String entity = entityFilter.getValue();
        String result = resultFilter.getValue();
        String compliance = complianceFilter.getValue();
        String saft = saftFilter.getValue();

        LocalDateTime from = fromDate.getValue() == null
                ? null : fromDate.getValue().atStartOfDay();

        LocalDateTime to = toDate.getValue() == null
                ? null : toDate.getValue().plusDays(1).atStartOfDay();

        table.setFilter(log -> {
            if (log == null) return false;

            String haystack = String.join(" ",
                    safe(log.getUsername()),
                    safe(log.getEntityType()),
                    safe(log.getEntityId()),
                    safe(log.getEntityDescription()),
                    safe(log.getModule()),
                    safe(log.getIpAddress()),
                    safe(log.getSessionId())
            ).toLowerCase(Locale.ROOT);

            if (!q.isBlank() && !haystack.contains(q)) return false;
            if (!module.isBlank() && !safe(log.getModule()).toLowerCase(Locale.ROOT).contains(module)) return false;

            if (!"Todas as operações".equals(action)
                    && (log.getActionType() == null
                    || !log.getActionType().name().equals(action))) {
                return false;
            }

            if (!"Todas as entidades".equals(entity)
                    && !Objects.equals(log.getEntityType(), entity)) {
                return false;
            }

            if ("Sucesso".equals(result) && !Boolean.TRUE.equals(log.getSuccess())) return false;
            if ("Falha".equals(result) && Boolean.TRUE.equals(log.getSuccess())) return false;

            if (!"Todos os níveis".equals(compliance)
                    && !Objects.equals(
                    log.getAgtComplianceLevel() == null ? "NORMAL" : log.getAgtComplianceLevel().name(),
                    compliance)) {
                return false;
            }

            if ("SAF-T: Sim".equals(saft) && !Boolean.TRUE.equals(log.getSaftRelevant())) return false;
            if ("SAF-T: Não".equals(saft) && Boolean.TRUE.equals(log.getSaftRelevant())) return false;

            if (from != null && log.getTimestamp() != null && log.getTimestamp().isBefore(from)) return false;
            if (to != null && log.getTimestamp() != null && !log.getTimestamp().isBefore(to)) return false;

            return true;
        });

        int visible = table.getItems().size();
        statusText.setText(visible + " registo(s) visíveis · " + logs.size() + " carregados");
        updateActionState(table.getSelectionModel().getSelectedItem());
    }

    private void updateKpis() {
        totalValue.setText(String.valueOf(logs.size()));
        failedValue.setText(String.valueOf(
                logs.stream().filter(l -> !Boolean.TRUE.equals(l.getSuccess())).count()
        ));
        saftValue.setText(String.valueOf(
                logs.stream().filter(l -> Boolean.TRUE.equals(l.getSaftRelevant())).count()
        ));
        criticalValue.setText(String.valueOf(
                logs.stream().filter(l ->
                        l.getAgtComplianceLevel() == AuditLog.AGTComplianceLevel.CRITICAL
                ).count()
        ));

        LocalDate today = LocalDate.now();
        todayValue.setText(String.valueOf(
                logs.stream()
                        .filter(l -> l.getTimestamp() != null
                                && l.getTimestamp().toLocalDate().equals(today))
                        .count()
        ));
    }

    private void updateActionState(AuditLog selected) {
        boolean view = hasViewPermission();
        detailsButton.setDisable(!view || selected == null);
        exportButton.setDisable(!hasExportPermission());
    }

    private void showDetails() {
        showDetails(table.getSelectionModel().getSelectedItem());
    }

    private void showDetails(AuditLog log) {
        if (!hasViewPermission()) {
            deny("Não possui permissão para consultar a auditoria.");
            return;
        }
        if (log == null) {
            deny("Seleccione um registo de auditoria.");
            return;
        }

        VBox root = new VBox(12);
        root.setPadding(new Insets(4));

        HBox identity = new HBox(10);
        identity.setAlignment(Pos.CENTER_LEFT);

        StackPane iconBox = new StackPane();
        iconBox.getStyleClass().add("kubata-audit-detail-icon");
        iconBox.getChildren().add(new Label("", IconUtils.icon(Feather.FILE_TEXT, 17)));

        VBox title = new VBox(2);
        Label titleLabel = new Label(
                log.getActionType() == null ? "Registo de auditoria" : log.getActionType().getDescription()
        );
        titleLabel.getStyleClass().add("kubata-audit-detail-title");

        Label subtitle = new Label(
                safe(log.getEntityType()) + " · " + safe(log.getEntityDescription())
        );
        subtitle.setWrapText(true);
        subtitle.getStyleClass().add("kubata-audit-detail-subtitle");
        title.getChildren().addAll(titleLabel, subtitle);

        identity.getChildren().addAll(iconBox, title);

        GridPane meta = new GridPane();
        meta.setHgap(18);
        meta.setVgap(9);
        meta.getStyleClass().add("kubata-audit-detail-grid");

        addDetail(meta, 0, "Data / Hora", formatDate(log.getTimestamp()));
        addDetail(meta, 1, "Utilizador", safe(log.getUsername()));
        addDetail(meta, 2, "Operação", log.getActionType() == null ? "—" : log.getActionType().name());
        addDetail(meta, 3, "Entidade", safe(log.getEntityType()));
        addDetail(meta, 4, "ID entidade", safe(log.getEntityId()));
        addDetail(meta, 5, "Módulo", safe(log.getModule()));
        addDetail(meta, 6, "IP", safe(log.getIpAddress()));
        addDetail(meta, 7, "Sessão", safe(log.getSessionId()));
        addDetail(meta, 8, "Resultado", Boolean.TRUE.equals(log.getSuccess()) ? "Sucesso" : "Falha");
        addDetail(meta, 9, "Nível AGT", log.getAgtComplianceLevel() == null
                ? "NORMAL" : log.getAgtComplianceLevel().name());
        addDetail(meta, 10, "SAF-T relevante", Boolean.TRUE.equals(log.getSaftRelevant()) ? "Sim" : "Não");
        addDetail(meta, 11, "Duração", log.getDuracaoMs() == null ? "—" : log.getDuracaoMs() + " ms");
        addDetail(meta, 12, "Hash de integridade", safe(log.getHashIntegrity()));

        root.getChildren().addAll(identity, meta);

        if (log.getErrorMessage() != null && !log.getErrorMessage().isBlank()) {
            root.getChildren().add(
                    titledArea("Erro registado", log.getErrorMessage())
            );
        }

        if (log.getUserAgent() != null && !log.getUserAgent().isBlank()) {
            root.getChildren().add(
                    titledArea("User Agent", log.getUserAgent())
            );
        }

        if (log.getOldValues() != null && !log.getOldValues().isBlank()) {
            root.getChildren().add(
                    titledArea("Valores anteriores", log.getOldValues())
            );
        }

        if (log.getNewValues() != null && !log.getNewValues().isBlank()) {
            root.getChildren().add(
                    titledArea("Novos valores", log.getNewValues())
            );
        }

        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        VBox wrapper = new VBox(scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        modalManager.showModal(
                wrapper,
                new ModalManager.ModalConfig()
                        .size(760, 680)
                        .minSize(620, 540)
                        .title("Detalhe do registo de auditoria")
                        .icon(Feather.SHIELD)
        );
    }

    private void addDetail(GridPane grid, int row, String label, String value) {
        Label l = new Label(label);
        l.getStyleClass().add("kubata-audit-detail-label");

        Label v = new Label(value);
        v.setWrapText(true);
        v.getStyleClass().add("kubata-audit-detail-value");

        int col = row % 2;
        int visualRow = row / 2;
        grid.add(l, col * 2, visualRow * 2);
        grid.add(v, col * 2, visualRow * 2 + 1);
    }

    private VBox titledArea(String title, String value) {
        VBox box = new VBox(4);
        box.getStyleClass().add("kubata-audit-detail-section");

        Label heading = new Label(title);
        heading.getStyleClass().add("kubata-audit-detail-section-title");

        TextArea area = new TextArea(value);
        area.setEditable(false);
        area.setWrapText(false);
        area.setPrefRowCount(5);

        box.getChildren().addAll(heading, area);
        return box;
    }

    private void exportCsv() {
        if (!hasExportPermission()) {
            deny("Não possui permissão para exportar a auditoria.");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Exportar auditoria");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV UTF-8", "*.csv")
        );
        chooser.setInitialFileName(
                "kubata-auditoria-" + LocalDate.now() + ".csv"
        );

        File target = chooser.showSaveDialog(getScene() == null ? null : getScene().getWindow());
        if (target == null) return;

        List<AuditLog> rows = new ArrayList<>(table.getItems());

        try (BufferedWriter writer = Files.newBufferedWriter(
                target.toPath(),
                StandardCharsets.UTF_8
        )) {
            writer.write("\uFEFF");
            writer.write(
                    "Data/Hora;Utilizador;Operação;Entidade;ID;Módulo;Descrição;"
                            + "Resultado;Nível AGT;SAF-T;IP;Sessão;Duração (ms);Hash;Erro"
            );
            writer.newLine();

            for (AuditLog log : rows) {
                writer.write(csv(formatDate(log.getTimestamp())));
                writer.write(';');
                writer.write(csv(log.getUsername()));
                writer.write(';');
                writer.write(csv(log.getActionType() == null ? "" : log.getActionType().name()));
                writer.write(';');
                writer.write(csv(log.getEntityType()));
                writer.write(';');
                writer.write(csv(log.getEntityId()));
                writer.write(';');
                writer.write(csv(log.getModule()));
                writer.write(';');
                writer.write(csv(log.getEntityDescription()));
                writer.write(';');
                writer.write(csv(Boolean.TRUE.equals(log.getSuccess()) ? "Sucesso" : "Falha"));
                writer.write(';');
                writer.write(csv(log.getAgtComplianceLevel() == null ? "NORMAL" : log.getAgtComplianceLevel().name()));
                writer.write(';');
                writer.write(csv(Boolean.TRUE.equals(log.getSaftRelevant()) ? "Sim" : "Não"));
                writer.write(';');
                writer.write(csv(log.getIpAddress()));
                writer.write(';');
                writer.write(csv(log.getSessionId()));
                writer.write(';');
                writer.write(csv(log.getDuracaoMs() == null ? "" : String.valueOf(log.getDuracaoMs())));
                writer.write(';');
                writer.write(csv(log.getHashIntegrity()));
                writer.write(';');
                writer.write(csv(log.getErrorMessage()));
                writer.newLine();
            }

            registarAuditoria(
                    "EXPORT",
                    "Exportação CSV da auditoria: " + rows.size() + " registos"
            );

            modalManager.alert(
                    "Exportação concluída",
                    "Foram exportados " + rows.size() + " registos para:\n" + target.getAbsolutePath(),
                    "info",
                    null
            );
        } catch (IOException ex) {
            modalManager.alert(
                    "Erro na exportação",
                    "Não foi possível criar o ficheiro CSV: " + ex.getMessage(),
                    "error",
                    ex
            );
        }
    }

    private void clearFilters() {
        searchField.clear();
        moduleField.clear();
        actionFilter.setValue("Todas as operações");
        entityFilter.setValue("Todas as entidades");
        resultFilter.setValue("Todos os resultados");
        complianceFilter.setValue("Todos os níveis");
        saftFilter.setValue("SAF-T: Todos");
        fromDate.setValue(null);
        toDate.setValue(null);
        applyFilters();
    }

    private boolean hasViewPermission() {
        User user = sessionManager.getUser();
        if (user == null) return false;
        if (user.isSuperadmin() || user.getRole() == Role.ADMIN) return true;

        if (acessoService == null) return true;

        return acessoService.temAcesso(
                user,
                "ADMINISTRATOR",
                "AUDITORIA",
                ao.allon.kubata.core.domain.PermissaoPerfil.Operacao.VER
        );
    }

    private boolean hasExportPermission() {
        User user = sessionManager.getUser();
        if (user == null) return false;
        if (user.isSuperadmin() || user.getRole() == Role.ADMIN) return true;
        if (acessoService == null) return false;

        return acessoService.temAcesso(
                user,
                "ADMINISTRATOR",
                "AUDITORIA",
                ao.allon.kubata.core.domain.PermissaoPerfil.Operacao.EXPORTAR
        );
    }

    private void refreshPermissions() {
        updateActionState(table.getSelectionModel().getSelectedItem());
    }

    private void deny(String message) {
        modalManager.alert("Acesso negado", message, "warning", null);
    }

    private void registarAuditoria(String operacao, String descricao) {
        try {
            User user = sessionManager.getUser();
            if (acessoService != null) {
                acessoService.registrarAuditoria(
                        user,
                        operacao,
                        "AUDITORIA",
                        "127.0.0.1",
                        descricao,
                        true
                );
            }
        } catch (Exception ignored) {
            // A auditoria da própria consulta/exportação não deve impedir a operação do utilizador.
        }
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }

    private String lower(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String formatDate(LocalDateTime date) {
        return date == null ? "—" : date.format(DATE_TIME);
    }

    private String message(Exception ex) {
        return ex.getMessage() == null || ex.getMessage().isBlank()
                ? "Operação não concluída."
                : ex.getMessage();
    }

    private String complianceClass(String level) {
        return switch (level) {
            case "CRITICAL" -> "kubata-audit-badge-critical";
            case "HIGH" -> "kubata-audit-badge-high";
            case "LOW" -> "kubata-audit-badge-low";
            default -> "kubata-audit-badge-normal";
        };
    }

    private String csv(String value) {
        if (value == null || "—".equals(value)) return "";
        String normalized = value.replace("\r", " ").replace("\n", " ");
        if (normalized.contains(";") || normalized.contains(""")) {
            return """ + normalized.replace(""", """") + """;
        }
        return normalized;
    }
}

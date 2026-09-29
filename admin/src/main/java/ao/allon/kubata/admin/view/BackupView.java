package ao.allon.kubata.admin.view;

import ao.allon.kubata.core.ui.table.AdvancedTableView;
import ao.allon.kubata.core.ui.table.TableUtils;
import ao.allon.kubata.admin.service.BackupService;
import ao.allon.kubata.core.domain.Role;
import ao.allon.kubata.core.domain.User;
import ao.allon.kubata.admin.service.PersistenceService;
import ao.allon.kubata.admin.service.SessionManager;
import ao.allon.kubata.admin.ui.modal.ModalManager;
import ao.allon.kubata.admin.ui.util.IconUtils;
import ao.allon.kubata.core.domain.BackupConfig;
import ao.allon.kubata.core.domain.BackupRecord;
import ao.allon.kubata.core.repository.BackupConfigRepository;
import ao.allon.kubata.core.repository.BackupRecordRepository;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.File;
import java.awt.Desktop;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Component
public class BackupView extends VBox {

    private final BackupConfigRepository backupConfigRepository;
    private final BackupRecordRepository backupRecordRepository;
    private final SessionManager sessionManager;
    private final ModalManager modalManager;
    private final PersistenceService persistenceService;
    private final BackupService backupService;
    private final ao.allon.kubata.core.service.AcessoService acessoService;
    private final Environment environment;

    private final TabPane tabPane = new TabPane();

    // Listas de Dados
    private final ObservableList<BackupRecord> records = FXCollections.observableArrayList();
    private final ObservableList<BackupConfig> configs = FXCollections.observableArrayList();

    // Tabelas
    private AdvancedTableView<BackupRecord> historyTable;
    private AdvancedTableView<BackupConfig> settingsTable;

    // Dashboard UI
    private Label lblLastBackup;
    private Label lblStorageUsed;
    private Label lblHealthStatus;
    private Label lblBackupCount;
    private Label lblVerifiedCount;
    private Label lblFailureCount;
    private Label lblFreeSpace;

    public BackupView(BackupConfigRepository backupConfigRepository,
                      BackupRecordRepository backupRecordRepository,
                      SessionManager sessionManager, ModalManager modalManager,
                      PersistenceService persistenceService,
                      BackupService backupService,
                      ao.allon.kubata.core.service.AcessoService acessoService,
                      Environment environment) {
        this.backupConfigRepository = backupConfigRepository;
        this.backupRecordRepository = backupRecordRepository;
        this.sessionManager = sessionManager;
        this.modalManager = modalManager;
        this.persistenceService = persistenceService;
        this.backupService = backupService;
        this.acessoService = acessoService;
        this.environment = environment;

        buildUI();
    }

    private boolean dataLoaded = false;

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        if (!dataLoaded && getScene() != null) {
            dataLoaded = true;
            loadData();
        }
    }

    private void buildUI() {
        setSpacing(0);
        getStyleClass().add("backup-view");

        // Toolbar Global
        HBox toolbar = buildToolbar();

        // TabPane setup
        tabPane.getStyleClass().add("office365-tabs");
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab tabDash = new Tab("Visão Geral", buildDashboardTab());
        tabDash.setGraphic(IconUtils.icon(Feather.PIE_CHART, 14));

        Tab tabHistory = new Tab("Histórico de Backups", buildHistoryTab());
        tabHistory.setGraphic(IconUtils.icon(Feather.LIST, 14));

        Tab tabSettings = new Tab("Agendamentos", buildSettingsTab());
        tabSettings.setGraphic(IconUtils.icon(Feather.SETTINGS, 14));

        Tab tabAdvanced = new Tab("Cópia em bruto (SGBD)", buildAdvancedDbTab());
        tabAdvanced.setGraphic(IconUtils.icon(Feather.SERVER, 14));

        tabPane.getTabs().addAll(tabDash, tabHistory, tabSettings, tabAdvanced);

        getChildren().addAll(toolbar, tabPane);
        VBox.setVgrow(tabPane, Priority.ALWAYS);
    }

    private HBox buildToolbar() {
        HBox box = new HBox(12);
        box.getStyleClass().add("header-box");
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(10, 20, 10, 20));

        VBox titleBox = new VBox(2);
        Label title = new Label("Protecção de Dados");
        title.getStyleClass().add("kubata-backup-title");

        Label subtitle = new Label("Centro de backup, recuperação e continuidade do Kubata");
        subtitle.getStyleClass().add("kubata-backup-subtitle");
        titleBox.getChildren().addAll(title, subtitle);

        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnExecutarAgora = new Button("Backup Instantâneo", IconUtils.icon(Feather.ZAP, IconUtils.SIZE_SMALL));
        btnExecutarAgora.getStyleClass().add("button-success");
        btnExecutarAgora.setOnAction(e -> executeInstantBackup());
        btnExecutarAgora.setDisable(!hasCreatePermission());

        Button btnTestarDestino = new Button(
                "Testar destino",
                IconUtils.icon(Feather.CHECK_CIRCLE, IconUtils.SIZE_SMALL)
        );
        btnTestarDestino.getStyleClass().add("button-outlined");
        btnTestarDestino.setOnAction(e -> testBackupDestination());

        Button btnConfigurar = new Button(
                "Configurar política",
                IconUtils.icon(Feather.SETTINGS, IconUtils.SIZE_SMALL)
        );
        btnConfigurar.getStyleClass().add("button-outlined");
        btnConfigurar.setDisable(!hasCreatePermission());
        btnConfigurar.setOnAction(e -> openPrimaryConfig());

        Button btnPasta = new Button(
                "Abrir destino",
                IconUtils.icon(Feather.FOLDER, IconUtils.SIZE_SMALL)
        );
        btnPasta.getStyleClass().add("button-outlined");
        btnPasta.setOnAction(e -> openBackupDirectory());

        Button btnRefresh = new Button(
                "Sincronizar",
                IconUtils.icon(Feather.REFRESH_CW, IconUtils.SIZE_SMALL)
        );
        btnRefresh.getStyleClass().add("button-outlined");
        btnRefresh.setOnAction(e -> loadData());

        box.getChildren().addAll(
                titleBox, spacer, btnPasta, btnTestarDestino, btnConfigurar,
                btnExecutarAgora, btnRefresh
        );
        return box;
    }

    private Node buildDashboardTab() {
        VBox dash = new VBox(18);
        dash.setPadding(new Insets(22));
        dash.getStyleClass().add("kubata-backup-dashboard");

        VBox hero = new VBox(5);
        hero.getStyleClass().add("kubata-backup-hero");

        HBox heroLine = new HBox(12);
        heroLine.setAlignment(Pos.CENTER_LEFT);

        StackPane shield = new StackPane();
        shield.getStyleClass().add("kubata-backup-hero-icon");
        shield.getChildren().add(new Label("", IconUtils.icon(Feather.SHIELD, 22)));

        VBox heroText = new VBox(3);
        Label heroTitle = new Label("Estado da protecção");
        heroTitle.getStyleClass().add("kubata-backup-hero-title");

        Label heroSubtitle = new Label(
                "Uma visão operacional da última cópia, integridade, armazenamento e política de retenção."
        );
        heroSubtitle.setWrapText(true);
        heroSubtitle.getStyleClass().add("kubata-backup-hero-subtitle");

        heroText.getChildren().addAll(heroTitle, heroSubtitle);
        heroLine.getChildren().addAll(shield, heroText);
        hero.getChildren().add(heroLine);

        GridPane metrics = new GridPane();
        metrics.setHgap(12);
        metrics.setVgap(12);

        metrics.add(createMetricCard(
                "Último backup",
                Feather.CLOCK,
                lblLastBackup = new Label("A calcular...")
        ), 0, 0);

        metrics.add(createMetricCard(
                "Backups concluídos",
                Feather.CHECK_CIRCLE,
                lblBackupCount = new Label("0")
        ), 1, 0);

        metrics.add(createMetricCard(
                "Backups verificados",
                Feather.SHIELD,
                lblVerifiedCount = new Label("0")
        ), 2, 0);

        metrics.add(createMetricCard(
                "Falhas registadas",
                Feather.ALERT_TRIANGLE,
                lblFailureCount = new Label("0")
        ), 3, 0);

        metrics.add(createMetricCard(
                "Espaço livre",
                Feather.HARD_DRIVE,
                lblFreeSpace = new Label("A calcular...")
        ), 0, 1);

        metrics.add(createMetricCard(
                "Arquivo protegido",
                Feather.DATABASE,
                lblStorageUsed = new Label("0.00 MB")
        ), 1, 1);

        metrics.add(createMetricCard(
                "Estado operacional",
                Feather.ACTIVITY,
                lblHealthStatus = new Label("A VALIDAR")
        ), 2, 1);

        metrics.add(createMetricCard(
                "Automação",
                Feather.CALENDAR,
                new Label("Consultar política")
        ), 3, 1);

        for (Node node : metrics.getChildren()) {
            GridPane.setHgrow(node, Priority.ALWAYS);
        }

        VBox.setVgrow(metrics, Priority.NEVER);

        HBox lower = new HBox(14);
        lower.setAlignment(Pos.TOP_LEFT);

        VBox quick = panel("Operações rápidas", Feather.ZAP);
        Button instant = actionButton(
                "Executar backup agora",
                "Cria uma cópia manual imediatamente.",
                Feather.CLOUD,
                this::executeInstantBackup,
                hasCreatePermission()
        );
        Button integrity = actionButton(
                "Verificar integridade",
                "Confirma checksum, arquivo e saúde da base.",
                Feather.SHIELD,
                this::verifyAllIntegrity,
                hasViewPermission()
        );
        Button destination = actionButton(
                "Testar destino",
                "Valida criação e escrita no destino configurado.",
                Feather.CHECK,
                this::testBackupDestination,
                hasViewPermission()
        );
        Button retention = actionButton(
                "Executar retenção",
                "Limpa ficheiros que ultrapassaram a retenção técnica.",
                Feather.TRASH_2,
                this::purgeOldBackups,
                hasCreatePermission()
        );
        quick.getChildren().addAll(instant, integrity, destination, retention);

        VBox policy = panel("Política de continuidade", Feather.LOCK);
        Label policySummary = new Label();
        policySummary.getStyleClass().add("kubata-backup-policy-summary");
        policySummary.setWrapText(true);

        Label destinationLabel = new Label();
        destinationLabel.getStyleClass().add("kubata-backup-detail-muted");
        destinationLabel.setWrapText(true);

        Button openFolder = new Button(
                "Abrir pasta de backup",
                IconUtils.icon(Feather.FOLDER, 12)
        );
        openFolder.getStyleClass().add("button-outlined");
        openFolder.setOnAction(e -> openBackupDirectory());

        Button copyPath = new Button(
                "Copiar caminho",
                IconUtils.icon(Feather.COPY, 12)
        );
        copyPath.getStyleClass().add("button-outlined");
        copyPath.setOnAction(e -> copyBackupDirectory());

        policy.getChildren().addAll(policySummary, new Separator(), destinationLabel, openFolder, copyPath);

        VBox compliance = panel("Continuidade fiscal", Feather.SHIELD);
        Label complianceText = new Label(
                "O backup técnico deve ser tratado como mecanismo de recuperação e continuidade. "
                        + "A retenção técnica configurada no Kubata não substitui os prazos legais de "
                        + "conservação, arquivo e disponibilidade dos documentos e dados fiscalmente relevantes."
        );
        complianceText.setWrapText(true);
        complianceText.getStyleClass().add("kubata-backup-detail-text");

        Label readiness = new Label("A verificar...");
        readiness.getStyleClass().add("kubata-backup-readiness");

        Button checkup = new Button(
                "Executar check-up",
                IconUtils.icon(Feather.CHECK_SQUARE, 12)
        );
        checkup.getStyleClass().add("button-outlined");
        checkup.setOnAction(e -> showBackupCheckup());

        compliance.getChildren().addAll(complianceText, readiness, checkup);

        lower.getChildren().addAll(quick, policy, compliance);
        HBox.setHgrow(quick, Priority.ALWAYS);
        HBox.setHgrow(policy, Priority.ALWAYS);
        HBox.setHgrow(compliance, Priority.ALWAYS);

        Runnable refreshPolicy = () -> {
            BackupConfig cfg = configs.stream().findFirst().orElse(null);
            if (cfg == null) {
                policySummary.setText("Nenhuma política configurada.");
            } else {
                String freq = cfg.getFrequency() == null ? "Não definida" : cfg.getFrequency().getDescription();
                String time = cfg.getFormattedTime() == null ? "—" : cfg.getFormattedTime();
                String retentionDays = cfg.getRetentionDays() == null ? "—" : String.valueOf(cfg.getRetentionDays());
                String destinationValue = cfg.getBackupLocation() == null || cfg.getBackupLocation().isBlank()
                        ? "backups"
                        : cfg.getBackupLocation();

                policySummary.setText(
                        "Estado: " + (Boolean.TRUE.equals(cfg.getEnabled()) ? "ACTIVA" : "INACTIVA")
                                + "\nFrequência: " + freq
                                + "\nHorário: " + time
                                + "\nRetenção técnica: " + retentionDays + " dias"
                );
                destinationLabel.setText("Destino actual: " + destinationValue);

                readiness.setText(
                        Boolean.TRUE.equals(cfg.getEnabled())
                                ? "● Política automática activa"
                                : "○ Automação desactivada"
                );
            }
        };

        // Atualização inicial e após carregamento dos dados.
        refreshPolicy.run();
        configs.addListener((javafx.collections.ListChangeListener<BackupConfig>) change ->
                Platform.runLater(refreshPolicy));

        dash.getChildren().addAll(hero, metrics, lower);

        ScrollPane scroll = new ScrollPane(dash);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("kubata-backup-scroll");
        return scroll;
    }

    private VBox createMetricCard(String title, Feather icon, Label value) {
        VBox card = new VBox(5);
        card.setMinWidth(155);
        card.setPrefWidth(190);
        card.setPrefHeight(86);
        card.getStyleClass().add("kubata-backup-metric-card");

        HBox heading = new HBox(7);
        heading.setAlignment(Pos.CENTER_LEFT);

        StackPane iconBox = new StackPane();
        iconBox.getStyleClass().add("kubata-backup-metric-icon");
        iconBox.getChildren().add(new Label("", IconUtils.icon(icon, 14)));

        Label label = new Label(title.toUpperCase());
        label.getStyleClass().add("kubata-backup-metric-label");

        heading.getChildren().addAll(iconBox, label);
        value.getStyleClass().add("kubata-backup-metric-value");
        card.getChildren().addAll(heading, value);

        return card;
    }

    private VBox panel(String title, Feather icon) {
        VBox box = new VBox(10);
        box.setPadding(new Insets(15));
        box.setMinHeight(190);
        box.getStyleClass().add("kubata-backup-panel");

        HBox heading = new HBox(8);
        heading.setAlignment(Pos.CENTER_LEFT);

        Label iconLabel = new Label("", IconUtils.icon(icon, 14));
        iconLabel.getStyleClass().add("kubata-backup-panel-icon");

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("kubata-backup-panel-title");

        heading.getChildren().addAll(iconLabel, titleLabel);
        box.getChildren().add(heading);
        return box;
    }

    private Button actionButton(
            String title,
            String description,
            Feather icon,
            Runnable action,
            boolean enabled
    ) {
        Button button = new Button();
        button.setMaxWidth(Double.MAX_VALUE);
        button.setDisable(!enabled);
        button.getStyleClass().add("kubata-backup-action");

        HBox content = new HBox(9);
        content.setAlignment(Pos.CENTER_LEFT);

        StackPane iconBox = new StackPane();
        iconBox.getStyleClass().add("kubata-backup-action-icon");
        iconBox.getChildren().add(new Label("", IconUtils.icon(icon, 13)));

        VBox text = new VBox(1);
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("kubata-backup-action-title");

        Label desc = new Label(description);
        desc.setWrapText(true);
        desc.getStyleClass().add("kubata-backup-action-text");

        text.getChildren().addAll(titleLabel, desc);
        HBox.setHgrow(text, Priority.ALWAYS);
        content.getChildren().addAll(iconBox, text);

        button.setGraphic(content);
        button.setOnAction(e -> action.run());
        return button;
    }

    private void openBackupDirectory() {
        Path directory = resolveBackupDirectory();
        try {
            Files.createDirectories(directory);
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(directory.toFile());
            } else {
                modalManager.alert(
                        "Destino de backup",
                        directory.toAbsolutePath().toString(),
                        "info",
                        null
                );
            }
        } catch (Exception ex) {
            modalManager.alert(
                    "Não foi possível abrir o destino",
                    directory.toAbsolutePath() + "\n\n"
                            + (ex.getMessage() == null ? "Verifique o caminho e as permissões." : ex.getMessage()),
                    "error",
                    ex
            );
        }
    }

    private void copyBackupDirectory() {
        javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
        content.putString(resolveBackupDirectory().toAbsolutePath().toString());
        javafx.scene.input.Clipboard.getSystemClipboard().setContent(content);
        modalManager.alert("Caminho copiado", "O destino do backup foi copiado para a área de transferência.", "info", null);
    }

    private void showBackupCheckup() {
        Path directory = resolveBackupDirectory();
        long completed = records.stream()
                .filter(r -> r.getStatus() == BackupRecord.BackupStatus.COMPLETED
                        || r.getStatus() == BackupRecord.BackupStatus.VERIFIED)
                .count();
        long failed = records.stream()
                .filter(r -> r.getStatus() == BackupRecord.BackupStatus.FAILED)
                .count();

        String disk;
        try {
            Files.createDirectories(directory);
            var store = Files.getFileStore(directory);
            disk = formatBytes(store.getUsableSpace()) + " livres de " + formatBytes(store.getTotalSpace());
        } catch (Exception ex) {
            disk = "Não foi possível determinar o espaço disponível.";
        }

        VBox content = new VBox(10);
        content.setPadding(new Insets(6));

        Label title = new Label("Check-up de continuidade");
        title.getStyleClass().add("kubata-backup-checkup-title");

        content.getChildren().addAll(
                title,
                checkLine("Destino acessível", Files.isDirectory(directory)
                        ? "OK · " + directory.toAbsolutePath()
                        : "Pendente · será criado na próxima operação"),
                checkLine("Backups válidos no histórico", String.valueOf(completed)),
                checkLine("Falhas registadas", String.valueOf(failed)),
                checkLine("Espaço disponível", disk),
                checkLine("Política automática", configs.stream().anyMatch(c -> Boolean.TRUE.equals(c.getEnabled()))
                        ? "Activa"
                        : "Não configurada/activa"),
                checkLine("Integridade", failed == 0 ? "Sem falhas no histórico" : "Existem falhas para revisão")
        );

        Label note = new Label(
                "Este check-up avalia a prontidão técnica do ambiente local. Não constitui declaração "
                        + "de conformidade, certificação ou validação da AGT."
        );
        note.setWrapText(true);
        note.getStyleClass().add("kubata-backup-detail-muted");
        content.getChildren().add(note);

        modalManager.showModal(
                content,
                new ModalManager.ModalConfig()
                        .size(620, 470)
                        .minSize(520, 400)
                        .title("Check-up de backup")
                        .icon(Feather.CHECK_SQUARE)
        );
    }

    private HBox checkLine(String label, String value) {
        Label left = new Label(label);
        left.getStyleClass().add("kubata-backup-detail-label");
        HBox.setHgrow(left, Priority.ALWAYS);

        Label right = new Label(value);
        right.setWrapText(true);
        right.getStyleClass().add("kubata-backup-detail-value");

        HBox row = new HBox(10, left, right);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("kubata-backup-check-row");
        return row;
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        double kb = bytes / 1024.0;
        if (kb < 1024) return String.format("%.1f KB", kb);
        double mb = kb / 1024.0;
        if (mb < 1024) return String.format("%.1f MB", mb);
        return String.format("%.1f GB", mb / 1024.0);
    }

    private Node buildHistoryTab() {
        historyTable = new AdvancedTableView<>(records);
        TableUtils.standardize(historyTable);

        TableColumn<BackupRecord, LocalDateTime> colData = new TableColumn<>("Data/Hora");
        colData.setCellValueFactory(col -> new SimpleObjectProperty<>(col.getValue().getStartTime()));
        colData.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                if (empty || item == null) setText(null);
                else setText(item.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            }
        });
        colData.setPrefWidth(160);

        TableColumn<BackupRecord, String> colTipo = TableUtils.createTextColumn("Origem", col -> new SimpleStringProperty(col.getValue().getType()));
        TableColumn<BackupRecord, String> colStatus = new TableColumn<>("Estado");
        colStatus.setCellValueFactory(col -> new SimpleObjectProperty<>(col.getValue().getStatus().getDescription()));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label lbl = new Label(item.toUpperCase());
                    lbl.getStyleClass().add("badge");
                    if (item.contains("Concluído") || item.contains("Verificado")) lbl.getStyleClass().add("badge-success");
                    else if (item.contains("Falhou")) lbl.getStyleClass().add("badge-danger");
                    else lbl.getStyleClass().add("badge-warning");
                    setGraphic(lbl);
                }
            }
        });

        TableColumn<BackupRecord, String> colSize = TableUtils.createTextColumn("Tamanho", col -> new SimpleStringProperty(col.getValue().getFormattedFileSize()));
        TableColumn<BackupRecord, String> colFile = TableUtils.createTextColumn("Ficheiro", col -> new SimpleStringProperty(col.getValue().getFilename()));

        // Coluna de Ações
        TableColumn<BackupRecord, Void> colActions = new TableColumn<>("Ações");
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnRestore = new Button("", IconUtils.icon(Feather.ROTATE_CCW, 12));
            private final Button btnVerify = new Button("", IconUtils.icon(Feather.SHIELD, 12));
            private final Button btnTestRestore = new Button("", IconUtils.icon(Feather.CHECK, 12));
            private final HBox group = new HBox(5, btnRestore, btnTestRestore, btnVerify);
            {
                btnRestore.setTooltip(new Tooltip("Restaurar este backup"));
                btnRestore.setOnAction(e -> restoreBackup(getTableView().getItems().get(getIndex())));
                btnTestRestore.setTooltip(new Tooltip("Testar restauração sem alterar a base"));
                btnTestRestore.setOnAction(e -> testRestoreBackup(getTableView().getItems().get(getIndex())));
                btnVerify.setTooltip(new Tooltip("Verificar integridade criptográfica e da base"));
                btnVerify.setOnAction(e -> verifyIntegrity(getTableView().getItems().get(getIndex())));
                group.setAlignment(Pos.CENTER);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : group);
            }
        });

        historyTable.getColumns().addAll(colData, colTipo, colStatus, colSize, colFile, colActions);
        return historyTable.withSearchBar();
    }

    private Node buildSettingsTab() {
        settingsTable = new AdvancedTableView<>(configs);
        TableUtils.standardize(settingsTable);

        TableColumn<BackupConfig, Boolean> colEnabled = TableUtils.createCheckColumn("Ativo", col -> new SimpleBooleanProperty(col.getValue().getEnabled()));
        colEnabled.setPrefWidth(60);

        TableColumn<BackupConfig, String> colFreq = TableUtils.createTextColumn("Frequência", col -> new SimpleStringProperty(col.getValue().getFrequency().getDescription()));
        TableColumn<BackupConfig, String> colTime = TableUtils.createTextColumn("Horário", col -> new SimpleStringProperty(col.getValue().getFormattedTime()));
        TableColumn<BackupConfig, String> colLocation = TableUtils.createTextColumn("Destino", col -> new SimpleStringProperty(col.getValue().getBackupLocation()));

        TableColumn<BackupConfig, LocalDateTime> colLast = new TableColumn<>("Última Execução");
        colLast.setCellValueFactory(col -> new SimpleObjectProperty<>(col.getValue().getLastExecution()));
        colLast.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                if (empty || item == null) setText("Nunca");
                else setText(item.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            }
        });

        // Coluna de Ações de Configuração
        TableColumn<BackupConfig, Void> colActions = new TableColumn<>("Ações");
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit = new Button("", IconUtils.icon(Feather.EDIT, 12));
            private final Button btnRun = new Button("", IconUtils.icon(Feather.PLAY, 12));
            private final HBox group = new HBox(5, btnEdit, btnRun);
            {
                btnEdit.setOnAction(e -> showConfigDialog(getTableView().getItems().get(getIndex())));
                btnRun.setOnAction(e -> executeBackupConfig(getTableView().getItems().get(getIndex())));
                group.setAlignment(Pos.CENTER);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : group);
            }
        });

        settingsTable.getColumns().addAll(colEnabled, colFreq, colTime, colLocation, colLast, colActions);

        VBox content = new VBox(10,
                new HBox(new Button("Novo Agendamento", IconUtils.icon(Feather.PLUS, 14)) {{
                    getStyleClass().add("button-primary");
                    setOnAction(e -> showConfigDialog(new BackupConfig()));
                }}),
                settingsTable
        );
        content.setPadding(new Insets(10));
        VBox.setVgrow(settingsTable, Priority.ALWAYS);

        return content;
    }

    private Node buildAdvancedDbTab() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(20));
        String url = environment.getProperty("spring.datasource.url", "");
        Label intro = new Label("Integração documentada: em SQLite o ficheiro aparece na URL JDBC. Para cópia a frio, feche a aplicação ou use o modo backup integrado acima. Em PostgreSQL/MySQL utilize pg_dump/mysqldump ou o plano de manutenção do ribbon.");
        intro.setWrapText(true);
        intro.getStyleClass().add("text-muted");
        TextArea ta = new TextArea(url.isEmpty() ? "(datasource não definido)" : url);
        ta.setEditable(false);
        ta.setPrefRowCount(3);
        Button copy = new Button("Copiar URL JDBC", IconUtils.icon(Feather.COPY, IconUtils.SIZE_SMALL));
        copy.setOnAction(e -> {
            javafx.scene.input.Clipboard.getSystemClipboard().setContent(
                    new javafx.scene.input.ClipboardContent() {{
                        putString(ta.getText());
                    }});
            modalManager.alert("Área de transferência", "URL copiada.", "info", null);
        });
        box.getChildren().addAll(intro, new Label("Datasource activo:"), ta, copy);
        return new ScrollPane(box) {{
            setFitToWidth(true);
            setStyle("-fx-background-color: transparent;");
        }};
    }

    private void loadData() {
        persistenceService.executeAsync(() -> {
            List<BackupRecord> recordList = backupRecordRepository.findAll();
            List<BackupConfig> configList = backupConfigRepository.findAll();

            Platform.runLater(() -> {
                records.setAll(recordList);
                configs.setAll(configList);
                updateDashboard();
            });
        }, "BACKUP_LOAD", "BACKUP", "Carregamento de dados de backup", null);
    }

    private void updateDashboard() {
        if (records.isEmpty()) {
            lblLastBackup.setText("Nenhum");
            lblStorageUsed.setText("0.00 MB");
            lblBackupCount.setText("0");
            lblVerifiedCount.setText("0");
            lblFailureCount.setText("0");
        } else {
            BackupRecord last = records.stream()
                    .filter(r -> r.getStatus() == BackupRecord.BackupStatus.COMPLETED
                            || r.getStatus() == BackupRecord.BackupStatus.VERIFIED)
                    .filter(r -> r.getStartTime() != null)
                    .max(Comparator.comparing(BackupRecord::getStartTime))
                    .orElse(null);

            lblLastBackup.setText(
                    last != null
                            ? last.getStartTime().format(DateTimeFormatter.ofPattern("dd/MM HH:mm"))
                            : "Nenhum"
            );

            long totalSize = records.stream()
                    .mapToLong(r -> r.getFileSize() != null ? r.getFileSize() : 0L)
                    .sum();
            lblStorageUsed.setText(formatBytes(totalSize));

            lblBackupCount.setText(String.valueOf(
                    records.stream().filter(r -> r.getStatus() == BackupRecord.BackupStatus.COMPLETED
                            || r.getStatus() == BackupRecord.BackupStatus.VERIFIED).count()
            ));
            lblVerifiedCount.setText(String.valueOf(
                    records.stream().filter(r -> r.getStatus() == BackupRecord.BackupStatus.VERIFIED).count()
            ));
            lblFailureCount.setText(String.valueOf(
                    records.stream().filter(r -> r.getStatus() == BackupRecord.BackupStatus.FAILED).count()
            ));
        }

        try {
            Path directory = resolveBackupDirectory();
            Files.createDirectories(directory);
            var store = Files.getFileStore(directory);
            lblFreeSpace.setText(formatBytes(store.getUsableSpace()));
        } catch (Exception ex) {
            lblFreeSpace.setText("Indisponível");
        }

        BackupRecord latest = records.stream()
                .filter(r -> r.getStartTime() != null)
                .max(Comparator.comparing(BackupRecord::getStartTime))
                .orElse(null);

        if (latest == null) {
            lblHealthStatus.setText("SEM DADOS");
        } else {
            switch (latest.getStatus()) {
                case COMPLETED, VERIFIED -> lblHealthStatus.setText("SAUDÁVEL");
                case FAILED -> lblHealthStatus.setText("ATENÇÃO");
                case IN_PROGRESS -> lblHealthStatus.setText("EM CURSO");
            }
        }
        lblHealthStatus.setStyle("-fx-font-weight: bold;");
    }

    private void openPrimaryConfig() {
        BackupConfig config = configs.stream().findFirst().orElseGet(BackupConfig::new);
        showConfigDialog(config);
    }

    private void showConfigDialog(BackupConfig config) {
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(20));

        ComboBox<BackupConfig.BackupFrequency> cmbFreq = new ComboBox<>(FXCollections.observableArrayList(BackupConfig.BackupFrequency.values()));
        cmbFreq.setValue(config.getFrequency() != null ? config.getFrequency() : BackupConfig.BackupFrequency.DAILY);

        TextField txtTime = new TextField(config.getScheduleTime());
        txtTime.setPromptText("HH:mm:ss");

        TextField txtLocation = new TextField(config.getBackupLocation());
        txtLocation.setPrefWidth(360);
        Button btnBrowse = new Button("", IconUtils.icon(Feather.FOLDER, 13));
        btnBrowse.setTooltip(new Tooltip("Escolher pasta de destino"));
        btnBrowse.setOnAction(e -> {
            javafx.stage.DirectoryChooser chooser = new javafx.stage.DirectoryChooser();
            chooser.setTitle("Seleccionar destino de backups");
            try {
                Path current = Paths.get(txtLocation.getText().isBlank() ? "backups" : txtLocation.getText());
                if (Files.isDirectory(current)) {
                    chooser.setInitialDirectory(current.toAbsolutePath().toFile());
                }
            } catch (Exception ignored) {
            }
            java.io.File selected = chooser.showDialog(txtLocation.getScene().getWindow());
            if (selected != null) {
                txtLocation.setText(selected.getAbsolutePath());
            }
        });
        HBox locationBox = new HBox(8, txtLocation, btnBrowse);
        HBox.setHgrow(txtLocation, Priority.ALWAYS);

        Spinner<Integer> spnRetention = new Spinner<>(
                7, 3650,
                config.getRetentionDays() != null ? config.getRetentionDays() : 30
        );
        spnRetention.setEditable(true);

        CheckBox chkCompress = new CheckBox("Comprimir (ZIP)");
        chkCompress.setSelected(!Boolean.FALSE.equals(config.getCompressBackup()));

        CheckBox chkAttachments = new CheckBox("Incluir anexos quando a rotina os suportar");
        chkAttachments.setSelected(!Boolean.FALSE.equals(config.getIncludeAttachments()));

        CheckBox chkNotifySuccess = new CheckBox("Notificar sucesso");
        chkNotifySuccess.setSelected(Boolean.TRUE.equals(config.getNotifyOnSuccess()));

        CheckBox chkNotifyFailure = new CheckBox("Notificar falha");
        chkNotifyFailure.setSelected(!Boolean.FALSE.equals(config.getNotifyOnFailure()));

        TextField txtEmails = new TextField(config.getEmailNotifications());
        txtEmails.setPromptText("emails@empresa.com (separados por vírgula)");

        CheckBox chkEnabled = new CheckBox("Agendamento automático activo");
        chkEnabled.setSelected(!Boolean.FALSE.equals(config.getEnabled()));

        grid.add(new Label("Frequência:"), 0, 0);
        grid.add(cmbFreq, 1, 0);
        grid.add(new Label("Horário:"), 0, 1);
        grid.add(txtTime, 1, 1);
        grid.add(new Label("Destino:"), 0, 2);
        grid.add(locationBox, 1, 2);
        grid.add(new Label("Retenção técnica (dias):"), 0, 3);
        grid.add(spnRetention, 1, 3);
        VBox policy = new VBox(7,
                chkEnabled,
                chkCompress,
                chkAttachments,
                chkNotifySuccess,
                chkNotifyFailure
        );
        grid.add(policy, 1, 4);
        grid.add(new Label("Email de notificações:"), 0, 5);
        grid.add(txtEmails, 1, 5);

        Label retentionNote = new Label(
                "A retenção técnica controla cópias de recuperação. Ela não define nem substitui "
                        + "o prazo legal de conservação do arquivo fiscal."
        );
        retentionNote.setWrapText(true);
        retentionNote.getStyleClass().add("text-muted");
        grid.add(retentionNote, 1, 6);

        modalManager.showConfirmModal(grid, "Configuração de Backup Automático", () -> {
            config.setFrequency(cmbFreq.getValue());
            config.setScheduleTime(txtTime.getText());
            config.setBackupLocation(txtLocation.getText());
            config.setRetentionDays(spnRetention.getValue());
            config.setCompressBackup(chkCompress.isSelected());
            config.setIncludeAttachments(chkAttachments.isSelected());
            config.setNotifyOnSuccess(chkNotifySuccess.isSelected());
            config.setNotifyOnFailure(chkNotifyFailure.isSelected());
            config.setEmailNotifications(txtEmails.getText());
            config.setEnabled(chkEnabled.isSelected());

            if (txtLocation.getText() == null || txtLocation.getText().isBlank()) {
                throw new IllegalArgumentException("Seleccione um destino para os backups.");
            }

            persistenceService.saveAsync(
                    backupConfigRepository,
                    config,
                    "BACKUP_CONFIG",
                    "Actualização da política de backup automático",
                    saved -> Platform.runLater(this::loadData)
            );
        }, null);
    }

    private void testBackupDestination() {
        if (!hasViewPermission()) {
            modalManager.alert(
                    "Acesso negado",
                    "Não possui permissão para consultar a configuração de backup.",
                    "warning",
                    null
            );
            return;
        }

        Path directory = resolveBackupDirectory();
        persistenceService.executeAsync(
                () -> {
                    try {
                        backupService.testDestination(directory);
                        Platform.runLater(() -> modalManager.alert(
                                "Destino validado",
                                "O Kubata conseguiu criar, escrever e remover um ficheiro de teste em:\n"
                                        + directory.toAbsolutePath(),
                                "info",
                                null
                        ));
                    } catch (Exception ex) {
                        Platform.runLater(() -> modalManager.alert(
                                "Destino indisponível",
                                "Não foi possível validar o destino:\n"
                                        + directory.toAbsolutePath()
                                        + "\n\n"
                                        + (ex.getMessage() == null ? "Verifique permissões e espaço em disco." : ex.getMessage()),
                                "error",
                                ex
                        ));
                    }
                },
                "BACKUP_TEST_DESTINATION",
                "BACKUP",
                "Teste do destino de backup: " + directory.toAbsolutePath(),
                null
        );
    }

    private void executeInstantBackup() {
        modalManager.showConfirmModal(new Label("Deseja iniciar um backup completo agora?"), "Backup Instantâneo", () -> {
            runBackupProcess("MANUAL", "Cópia manual iniciada pelo administrador");
        }, null);
    }

    private void executeBackupConfig(BackupConfig config) {
        modalManager.showConfirmModal(new Label("Executar agendamento '" + config.getFrequency().getDescription() + "' agora?"), "Executar Agendamento", () -> {
            runBackupProcess("SCHEDULED", "Execução forçada do agendamento: " + config.getFrequency().getDescription());
        }, null);
    }

    private void runBackupProcess(String type, String notes) {
        persistenceService.executeAsync(() -> {
            BackupRecord record = new BackupRecord();
            record.setStartTime(LocalDateTime.now());
            record.setType(type);
            record.setNotes(notes);
            record.setStatus(BackupRecord.BackupStatus.IN_PROGRESS);
            record.setTriggeredBy(sessionManager.getUser() != null ? sessionManager.getUser().getNome() : "Sistema");
            record.setDbUrl(environment.getProperty("spring.datasource.url", ""));
            backupRecordRepository.save(record);

            try {
                Path backupDir = resolveBackupDirectory();
                boolean compress = resolveCompressDefault();

                BackupService.BackupArtifact artifact = backupService.createDatabaseBackup(backupDir, compress);

                record.setEndTime(LocalDateTime.now());
                record.setStatus(BackupRecord.BackupStatus.COMPLETED);
                record.setFilename(artifact.file().toAbsolutePath().toString());
                record.setFileSize(artifact.sizeBytes());
                record.setChecksum(artifact.sha256());
                record.setCompressed(artifact.compressed());
                record.setErrorMessage(null);
                backupRecordRepository.save(record);
            } catch (Exception ex) {
                record.setEndTime(LocalDateTime.now());
                record.setStatus(BackupRecord.BackupStatus.FAILED);
                record.setErrorMessage(ex.getMessage());
                backupRecordRepository.save(record);
                throw new RuntimeException(ex);
            }
        }, "BACKUP_EXEC", "BACKUP", notes, this::loadData);
    }

    private void testRestoreBackup(BackupRecord record) {
        if (record == null) return;
        if (!hasViewPermission()) {
            modalManager.alert(
                    "Acesso negado",
                    "Não possui permissão para testar backups.",
                    "warning",
                    null
            );
            return;
        }

        persistenceService.executeAsync(() -> {
            Path pending = null;
            try {
                Path backupFile = Paths.get(record.getFilename());
                BackupService.IntegrityCheck integrity =
                        backupService.verifyBackup(backupFile, record.getChecksum());

                if (!integrity.checksumMatches()
                        || !integrity.archiveReadable()
                        || !integrity.databaseHealthy()) {
                    throw new SecurityException(
                            "Teste interrompido: " + integrity.message()
                    );
                }

                pending = backupService.prepareRestoreToPending(backupFile);
                Path prepared = pending;
                Platform.runLater(() -> modalManager.alert(
                        "Teste de restauração concluído",
                        "O backup foi validado e preparado para restauração sem substituir a base actual.\n\n"
                                + "Ficheiro preparado: " + prepared,
                        "info",
                        null
                ));
            } catch (Exception ex) {
                Platform.runLater(() -> modalManager.alert(
                        "Teste de restauração falhou",
                        ex.getMessage() == null
                                ? "O backup não passou no teste de restauração."
                                : ex.getMessage(),
                        "error",
                        ex
                ));
                throw new RuntimeException(ex);
            } finally {
                if (pending != null) {
                    try {
                        Files.deleteIfExists(pending);
                    } catch (Exception ignored) {
                    }
                }
            }
        }, "BACKUP_TEST_RESTORE", "BACKUP",
                "Teste de restauração do backup " + record.getFilename(), null);
    }

    private void restoreBackup(BackupRecord record) {
        modalManager.showConfirmModal(new Label("RESTAURAR BACKUP: " + record.getFilename() + "?\n\nAVISO: Os dados atuais serão substituídos!"),
                "Confirmação de Restauro", () -> {
                    persistenceService.executeAsync(() -> {
                        try {
                            Path backupFile = Paths.get(record.getFilename());
                            BackupService.IntegrityCheck integrity =
                                    backupService.verifyBackup(backupFile, record.getChecksum());

                            if (!integrity.checksumMatches()
                                    || !integrity.archiveReadable()
                                    || !integrity.databaseHealthy()) {
                                throw new SecurityException(
                                        "Restauro bloqueado: a integridade do backup não foi confirmada. "
                                                + integrity.message()
                                );
                            }

                            Path pending = backupService.prepareRestoreToPending(backupFile);

                            record.incrementRestoreCount();
                            record.setRestoredAt(LocalDateTime.now());
                            record.setRestoredBy(sessionManager.getUser() != null ? sessionManager.getUser().getNome() : "Sistema");
                            record.setNotes("Restauro preparado em: " + pending);
                            backupRecordRepository.save(record);

                            Platform.runLater(() -> modalManager.alert(
                                    "Restauro Preparado",
                                    "O restauro foi preparado com sucesso, mas para segurança será aplicado no próximo arranque.\n\nFicheiro: " + pending + "\n\nFeche e volte a abrir o Kubata Administrator para aplicar o restauro.",
                                    "info",
                                    null
                            ));
                        } catch (Exception ex) {
                            throw new RuntimeException(ex);
                        }
                    }, "BACKUP_RESTORE", "BACKUP", "Restauro do ficheiro " + record.getFilename(), this::loadData);
                }, null);
    }

    private void verifyIntegrity(BackupRecord record) {
        persistenceService.executeAsync(() -> {
            try {
                Path file = Paths.get(record.getFilename());
                if (!Files.exists(file)) {
                    throw new IllegalStateException("Ficheiro não existe: " + file);
                }

                BackupService.IntegrityCheck result =
                        backupService.verifyBackup(file, record.getChecksum());

                if (result.checksumMatches()
                        && result.archiveReadable()
                        && result.databaseHealthy()) {
                    record.setStatus(BackupRecord.BackupStatus.VERIFIED);
                    record.setErrorMessage(null);
                    backupRecordRepository.save(record);
                } else {
                    record.setStatus(BackupRecord.BackupStatus.FAILED);
                    record.setErrorMessage(result.message());
                    backupRecordRepository.save(record);
                    throw new SecurityException(
                            "Integridade não confirmada. " + result.message()
                    );
                }
            } catch (Exception ex) {
                record.setStatus(BackupRecord.BackupStatus.FAILED);
                record.setErrorMessage(ex.getMessage());
                backupRecordRepository.save(record);
                throw new RuntimeException(ex);
            }
        }, "BACKUP_VERIFY", "BACKUP", "Verificação de integridade: " + record.getFilename(), this::loadData);
    }

    private void purgeOldBackups() {
        modalManager.showConfirmModal(new Label("Deseja remover backups que excederam o período de retenção?"), "Limpeza de Arquivo", () -> {
            persistenceService.executeAsync(() -> {
                int retentionDays = resolveRetentionDays();
                LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);

                List<BackupRecord> all = backupRecordRepository.findAll();
                for (BackupRecord r : all) {
                    if (r.getStartTime() == null) continue;
                    if (r.getStartTime().isAfter(cutoff)) continue;
                    if (r.getFilename() == null || r.getFilename().isBlank()) continue;

                    try {
                        Path file = Paths.get(r.getFilename());
                        Files.deleteIfExists(file);
                    } catch (Exception ignored) {
                    }
                }
            }, "BACKUP_PURGE", "BACKUP", "Limpeza de backups antigos (retenção)", this::loadData);
        }, null);
    }

    private void verifyAllIntegrity() {
        modalManager.alert("Integridade", "Iniciando verificação em lote de todos os arquivos de backup...", "info", null);
        persistenceService.executeAsync(() -> {
            List<BackupRecord> all = backupRecordRepository.findAll();
            for (BackupRecord r : all) {
                if (r.getFilename() == null || r.getFilename().isBlank()) continue;
                try {
                    Path file = Paths.get(r.getFilename());
                    if (!Files.exists(file)) continue;
                    BackupService.IntegrityCheck result =
                            backupService.verifyBackup(file, r.getChecksum());
                    if (result.checksumMatches()
                            && result.archiveReadable()
                            && result.databaseHealthy()) {
                        r.setStatus(BackupRecord.BackupStatus.VERIFIED);
                        r.setErrorMessage(null);
                    } else {
                        r.setStatus(BackupRecord.BackupStatus.FAILED);
                        r.setErrorMessage(result.message());
                    }
                    backupRecordRepository.save(r);
                } catch (Exception ex) {
                    r.setStatus(BackupRecord.BackupStatus.FAILED);
                    r.setErrorMessage(ex.getMessage());
                    backupRecordRepository.save(r);
                }
            }
        }, "BACKUP_VERIFY_ALL", "BACKUP", "Verificação em lote de integridade", this::loadData);
    }

    private boolean hasViewPermission() {
        User user = sessionManager.getUser();
        if (user == null) return false;
        if (user.isSuperadmin() || user.getRole() == Role.ADMIN) return true;
        return acessoService.temAcesso(
                user, "ADMINISTRATOR", "BACKUP",
                ao.allon.kubata.core.domain.PermissaoPerfil.Operacao.VER
        );
    }

    private boolean hasCreatePermission() {
        User user = sessionManager.getUser();
        if (user == null) return false;
        if (user.isSuperadmin() || user.getRole() == Role.ADMIN) return true;
        return acessoService.temAcesso(
                user, "ADMINISTRATOR", "BACKUP",
                ao.allon.kubata.core.domain.PermissaoPerfil.Operacao.CRIAR
        );
    }

    private Path resolveBackupDirectory() {
        String location = null;
        try {
            location = configs.stream()
                    .filter(c -> Boolean.TRUE.equals(c.getEnabled()))
                    .map(BackupConfig::getBackupLocation)
                    .filter(s -> s != null && !s.isBlank())
                    .findFirst()
                    .orElse(null);
        } catch (Exception ignored) {
        }
        if (location == null || location.isBlank()) {
            location = "backups";
        }

        Path path = Paths.get(location).normalize();
        if (path.toString().isBlank()) {
            return Paths.get("backups");
        }
        return path;
    }

    private boolean resolveCompressDefault() {
        try {
            return configs.stream()
                    .filter(c -> Boolean.TRUE.equals(c.getEnabled()))
                    .map(BackupConfig::getCompressBackup)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(Boolean.TRUE);
        } catch (Exception ignored) {
            return true;
        }
    }

    private int resolveRetentionDays() {
        try {
            return configs.stream()
                    .filter(c -> Boolean.TRUE.equals(c.getEnabled()))
                    .map(BackupConfig::getRetentionDays)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(30);
        } catch (Exception ignored) {
            return 30;
        }
    }
}

package ao.allon.kubata.admin.view;

import ao.allon.kubata.core.ui.table.AdvancedTableView;
import ao.allon.kubata.core.ui.table.TableUtils;
import ao.allon.kubata.admin.service.BackupService;
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

    public BackupView(BackupConfigRepository backupConfigRepository,
                      BackupRecordRepository backupRecordRepository,
                      SessionManager sessionManager, ModalManager modalManager,
                      PersistenceService persistenceService,
                      BackupService backupService,
                      Environment environment) {
        this.backupConfigRepository = backupConfigRepository;
        this.backupRecordRepository = backupRecordRepository;
        this.sessionManager = sessionManager;
        this.modalManager = modalManager;
        this.persistenceService = persistenceService;
        this.backupService = backupService;
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

        Label title = new Label("Centro de Backup Empresarial");
        title.getStyleClass().add("h3");
        title.setStyle("-fx-text-fill: -kubata-green-dark;");

        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnExecutarAgora = new Button("Backup Instantâneo", IconUtils.icon(Feather.ZAP, IconUtils.SIZE_SMALL));
        btnExecutarAgora.getStyleClass().add("button-success");
        btnExecutarAgora.setOnAction(e -> executeInstantBackup());

        Button btnRefresh = new Button("Sincronizar", IconUtils.icon(Feather.REFRESH_CW, IconUtils.SIZE_SMALL));
        btnRefresh.getStyleClass().add("button-outlined");
        btnRefresh.setOnAction(e -> loadData());

        box.getChildren().addAll(title, spacer, btnExecutarAgora, btnRefresh);
        return box;
    }

    private Node buildDashboardTab() {
        VBox dash = new VBox(25);
        dash.setPadding(new Insets(30));
        dash.setAlignment(Pos.TOP_CENTER);
        dash.setStyle("-fx-background-color: #f8f9fa;");

        // Título e Resumo
        VBox header = new VBox(10);
        header.setAlignment(Pos.CENTER);
        Label title = new Label("Estado da Proteção de Dados");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        Label subtitle = new Label("Monitorização em tempo real das cópias de segurança do sistema");
        subtitle.getStyleClass().add("text-muted");
        header.getChildren().addAll(title, subtitle);

        // Grid de Cartões de Status (Estilo Profissional)
        HBox cards = new HBox(20);
        cards.setAlignment(Pos.CENTER);

        cards.getChildren().addAll(
                createStatusCard("Último Backup", Feather.CLOCK, lblLastBackup = new Label("A calcular..."), "#3498db"),
                createStatusCard("Espaço em Disco", Feather.DATABASE, lblStorageUsed = new Label("0.00 MB"), "#9b59b6"),
                createStatusCard("Saúde do Backup", Feather.SHIELD, lblHealthStatus = new Label("OK"), "#27ae60")
        );

        // Painel de Ações e Informações
        HBox contentArea = new HBox(20);
        contentArea.setAlignment(Pos.CENTER);

        // Ações Rápidas
        VBox quickActions = new VBox(15);
        quickActions.setPadding(new Insets(25));
        quickActions.getStyleClass().add("card");
        quickActions.setPrefWidth(350);
        quickActions.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5);");

        Label lblActionsTitle = new Label("Operações de Manutenção");
        lblActionsTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        Button btnPurge = new Button("Limpar Backups Antigos", IconUtils.icon(Feather.TRASH_2, 14));
        btnPurge.setMaxWidth(Double.MAX_VALUE);
        btnPurge.getStyleClass().addAll("button-outlined", "button-danger");
        btnPurge.setOnAction(e -> purgeOldBackups());

        Button btnVerify = new Button("Verificar Integridade Global", IconUtils.icon(Feather.CHECK_CIRCLE, 14));
        btnVerify.setMaxWidth(Double.MAX_VALUE);
        btnVerify.getStyleClass().add("button-outlined");
        btnVerify.setOnAction(e -> verifyAllIntegrity());

        quickActions.getChildren().addAll(lblActionsTitle, new Separator(), btnPurge, btnVerify);

        // Dicas e Status Informativo
        VBox infoPanel = new VBox(15);
        infoPanel.setPadding(new Insets(25));
        infoPanel.getStyleClass().add("card");
        infoPanel.setPrefWidth(350);
        infoPanel.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5);");

        Label lblInfoTitle = new Label("Dicas de Segurança");
        lblInfoTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        
        Label tip1 = new Label("• Mantenha cópias em discos externos.");
        Label tip2 = new Label("• Verifique a integridade mensalmente.");
        Label tip3 = new Label("• Backups comprimidos poupam 70% de espaço.");
        tip1.setWrapText(true); tip2.setWrapText(true); tip3.setWrapText(true);

        infoPanel.getChildren().addAll(lblInfoTitle, new Separator(), tip1, tip2, tip3);

        contentArea.getChildren().addAll(quickActions, infoPanel);

        dash.getChildren().addAll(header, cards, contentArea);
        return new ScrollPane(dash) {{
            setFitToWidth(true);
            setStyle("-fx-background-color: transparent;");
        }};
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
            private final HBox group = new HBox(5, btnRestore, btnVerify);
            {
                btnRestore.setTooltip(new Tooltip("Restaurar este backup"));
                btnRestore.setOnAction(e -> restoreBackup(getTableView().getItems().get(getIndex())));
                btnVerify.setTooltip(new Tooltip("Verificar Integridade (Checksum)"));
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

    private VBox createStatusCard(String title, Feather icon, Label value, String accentColor) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(20));
        card.setPrefSize(220, 130);
        card.setAlignment(Pos.CENTER);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5); -fx-border-color: " + accentColor + "; -fx-border-width: 0 0 4 0;");

        StackPane iconCircle = new StackPane(IconUtils.icon(icon, 24));
        iconCircle.setPrefSize(45, 45);
        iconCircle.setStyle("-fx-background-color: " + accentColor + "22; -fx-background-radius: 50; -fx-text-fill: " + accentColor + ";");

        Label lblTitle = new Label(title);
        lblTitle.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 13px; -fx-font-weight: bold;");

        value.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        card.getChildren().addAll(iconCircle, lblTitle, value);
        return card;
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
        } else {
            BackupRecord last = records.stream()
                    .filter(r -> r.getStatus() == BackupRecord.BackupStatus.COMPLETED)
                    .max(Comparator.comparing(BackupRecord::getStartTime))
                    .orElse(null);

            lblLastBackup.setText(last != null ? last.getStartTime().format(DateTimeFormatter.ofPattern("dd/MM HH:mm")) : "Nenhum");

            long totalSize = records.stream().mapToLong(r -> r.getFileSize() != null ? r.getFileSize() : 0L).sum();
            if (totalSize < 1024 * 1024) lblStorageUsed.setText(String.format("%.2f KB", totalSize / 1024.0));
            else lblStorageUsed.setText(String.format("%.2f MB", totalSize / (1024.0 * 1024.0)));
        }

        boolean hasFailures = records.stream().anyMatch(r -> r.getStatus() == BackupRecord.BackupStatus.FAILED);
        lblHealthStatus.setText(hasFailures ? "AVISO" : "SAUDÁVEL");
        lblHealthStatus.setStyle("-fx-text-fill: " + (hasFailures ? "red" : "green") + "; -fx-font-weight: bold;");
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
        Spinner<Integer> spnRetention = new Spinner<>(7, 365, config.getRetentionDays() != null ? config.getRetentionDays() : 30);

        CheckBox chkCompress = new CheckBox("Comprimir (ZIP)");
        chkCompress.setSelected(config.getCompressBackup());

        TextField txtEmails = new TextField(config.getEmailNotifications());
        txtEmails.setPromptText("emails@empresa.com (separados por vírgula)");

        grid.add(new Label("Frequência:"), 0, 0);
        grid.add(cmbFreq, 1, 0);
        grid.add(new Label("Horário:"), 0, 1);
        grid.add(txtTime, 1, 1);
        grid.add(new Label("Destino:"), 0, 2);
        grid.add(txtLocation, 1, 2);
        grid.add(new Label("Retenção (dias):"), 0, 3);
        grid.add(spnRetention, 1, 3);
        grid.add(chkCompress, 1, 4);
        grid.add(new Label("Notificar por Email:"), 0, 5);
        grid.add(txtEmails, 1, 5);

        modalManager.showConfirmModal(grid, "Configuração de Backup Automático", () -> {
            config.setFrequency(cmbFreq.getValue());
            config.setScheduleTime(txtTime.getText());
            config.setBackupLocation(txtLocation.getText());
            config.setRetentionDays(spnRetention.getValue());
            config.setCompressBackup(chkCompress.isSelected());
            config.setEmailNotifications(txtEmails.getText());
            config.setEnabled(true);

            persistenceService.saveAsync(backupConfigRepository, config, "BACKUP_CONFIG", "Salva configuração de backup", saved -> loadData());
        }, null);
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

    private void restoreBackup(BackupRecord record) {
        modalManager.showConfirmModal(new Label("RESTAURAR BACKUP: " + record.getFilename() + "?\n\nAVISO: Os dados atuais serão substituídos!"),
                "Confirmação de Restauro", () -> {
                    persistenceService.executeAsync(() -> {
                        try {
                            Path backupFile = Paths.get(record.getFilename());
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

                String sha = backupService.sha256Of(file);
                record.setChecksum(sha);
                record.setStatus(BackupRecord.BackupStatus.VERIFIED);
                record.setErrorMessage(null);
                backupRecordRepository.save(record);
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
                    String sha = backupService.sha256Of(file);
                    r.setChecksum(sha);
                    r.setStatus(BackupRecord.BackupStatus.VERIFIED);
                    r.setErrorMessage(null);
                    backupRecordRepository.save(r);
                } catch (Exception ex) {
                    r.setStatus(BackupRecord.BackupStatus.FAILED);
                    r.setErrorMessage(ex.getMessage());
                    backupRecordRepository.save(r);
                }
            }
        }, "BACKUP_VERIFY_ALL", "BACKUP", "Verificação em lote de integridade", this::loadData);
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
        return Paths.get(location);
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

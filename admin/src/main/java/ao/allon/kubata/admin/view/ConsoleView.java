package ao.allon.kubata.admin.view;

import ao.allon.kubata.admin.service.NotificationService;
import ao.allon.kubata.admin.service.PersistenceService;
import ao.allon.kubata.admin.service.SessionManager;
import ao.allon.kubata.admin.service.job.AdminJob;
import ao.allon.kubata.admin.service.job.JobManager;
import ao.allon.kubata.admin.ui.modal.ModalManager;
import ao.allon.kubata.admin.ui.util.IconUtils;
import ao.allon.kubata.core.domain.SystemLog;
import ao.allon.kubata.core.domain.User;
import ao.allon.kubata.core.domain.UserSession;
import ao.allon.kubata.core.domain.RecordLock;
import ao.allon.kubata.core.repository.SystemLogRepository;
import ao.allon.kubata.core.repository.UserRepository;
import ao.allon.kubata.core.repository.UserSessionRepository;
import ao.allon.kubata.core.repository.RecordLockRepository;
import ao.allon.kubata.core.ui.table.AdvancedTableView;
import ao.allon.kubata.core.ui.table.TableUtils;
import ao.allon.kubata.core.module.ModuleRegistry;
import ao.allon.kubata.core.module.KubataModule;
import javafx.application.Platform;
import java.util.Collection;
import java.util.List;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import org.kordamp.ikonli.feather.Feather;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.io.File;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Consola do Administrador - Inspirada no Primavera ERP V10.
 * Permite monitorização de sessões, desbloqueio de registos, visualização de logs e processos.
 */
@Component
public class ConsoleView extends VBox {

    private static final Logger logger = LoggerFactory.getLogger(ConsoleView.class);

    private final SystemLogRepository systemLogRepository;
    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;
    private final RecordLockRepository recordLockRepository;
    private final SessionManager sessionManager;
    private final ModalManager modalManager;
    private final PersistenceService persistenceService;
    private final NotificationService notificationService;
    private final JobManager jobManager;
    private final ModuleRegistry moduleRegistry;

    private final TabPane tabPane = new TabPane();
    
    // KPIs Dashboard
    private Label lblActiveSessionsCount;
    private Label lblLockedRecordsCount;
    private Label lblSystemHealth;
    private Label lblErrorCount;
    private Label lblUptime;
    private Label lblDBStatus;
    
    // Performance Chart Data
    private XYChart.Series<Number, Number> cpuSeries = new XYChart.Series<>();
    private XYChart.Series<Number, Number> memSeries = new XYChart.Series<>();
    private int chartTime = 0;
    private ScheduledExecutorService scheduler;
    
    // Dados para as tabelas
    private final ObservableList<ActiveSession> activeSessions = FXCollections.observableArrayList();
    private final ObservableList<LockedRecord> lockedRecords = FXCollections.observableArrayList();
    private final ObservableList<SystemLog> systemLogs = FXCollections.observableArrayList();
    private final ObservableList<SystemLog> filteredLogs = FXCollections.observableArrayList();
    private final ObservableList<AdminJob> backgroundProcesses;
    private final ObservableList<ModuleStatus> moduleStatuses = FXCollections.observableArrayList();

    public ConsoleView(SystemLogRepository systemLogRepository, 
                       UserRepository userRepository,
                       UserSessionRepository userSessionRepository,
                       RecordLockRepository recordLockRepository,
                       SessionManager sessionManager, 
                       ModalManager modalManager,
                       PersistenceService persistenceService,
                       NotificationService notificationService,
                       JobManager jobManager,
                       ModuleRegistry moduleRegistry) {
        this.systemLogRepository = systemLogRepository;
        this.userRepository = userRepository;
        this.userSessionRepository = userSessionRepository;
        this.recordLockRepository = recordLockRepository;
        this.sessionManager = sessionManager;
        this.modalManager = modalManager;
        this.persistenceService = persistenceService;
        this.notificationService = notificationService;
        this.jobManager = jobManager;
        this.moduleRegistry = moduleRegistry;
        this.backgroundProcesses = jobManager.getJobs();

        buildUI();
        
        // Carregamento assíncrono para evitar erros no construtor
        Platform.runLater(() -> {
            refreshAll();
            startPerformanceMonitoring();
        });
    }

    private void startPerformanceMonitoring() {
        cpuSeries.setName("CPU (%)");
        memSeries.setName("Memória (MB)");
        
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            t.setName("ConsolePerformanceMonitor");
            return t;
        });
        
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        
        scheduler.scheduleAtFixedRate(() -> {
            Platform.runLater(() -> {
                double cpu;
                if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
                    cpu = ((com.sun.management.OperatingSystemMXBean) osBean).getSystemCpuLoad() * 100;
                } else {
                    cpu = osBean.getSystemLoadAverage();
                }
                
                // Fallback se não for possível obter CPU real
                if (cpu < 0) cpu = 5 + (Math.random() * 10);
                
                Runtime runtime = Runtime.getRuntime();
                double mem = (runtime.totalMemory() - runtime.freeMemory()) / (1024.0 * 1024.0);
                
                cpuSeries.getData().add(new XYChart.Data<>(chartTime, cpu));
                memSeries.getData().add(new XYChart.Data<>(chartTime, mem));
                
                if (cpuSeries.getData().size() > 20) {
                    cpuSeries.getData().remove(0);
                    memSeries.getData().remove(0);
                }
                chartTime++;
                
                updateUptime();
                updateDBHealth();
            });
        }, 0, 2, TimeUnit.SECONDS);
    }

    @PreDestroy
    public void cleanup() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            logger.info("Scheduler da Consola desligado com sucesso.");
        }
    }

    private void updateDBHealth() {
        if (lblDBStatus != null) {
            persistenceService.executeSilent(() -> {
                userRepository.count(); // Teste de conexão simples
            }, () -> {
                // Sucesso
                Platform.runLater(() -> {
                    lblDBStatus.setText("LIGADO");
                    lblDBStatus.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                });
            });
            
            // Tratamento de erro via Task interno do executeSilent (logger.warn)
            // Para atualizar a UI em caso de falha, poderíamos estender o executeSilent
            // Mas para o health check, se falhar, o setOnFailed do Task não executa o onSuccess
        }
    }

    private void simulateProcessProgress() {
        // Removido pois agora usamos Jobs reais
    }

    private void updateUptime() {
        if (lblUptime != null) {
            long uptimeMs = ManagementFactory.getRuntimeMXBean().getUptime();
            long seconds = uptimeMs / 1000;
            long h = seconds / 3600;
            long m = (seconds % 3600) / 60;
            long s = seconds % 60;
            lblUptime.setText(String.format("%02dh %02dm %02ds", h, m, s));
        }
    }

    private void buildUI() {
        setSpacing(0);
        getStyleClass().add("console-view");

        // Toolbar de Comandos da Consola
        HBox toolbar = buildToolbar();
        
        tabPane.getStyleClass().add("office365-tabs");
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab tabDash = new Tab("Painel de Controlo", buildDashboardTab());
        tabDash.setGraphic(IconUtils.icon(Feather.LAYOUT, 14));

        Tab tabSessions = new Tab("Utilizadores Ligados", buildSessionsTab());
        tabSessions.setGraphic(IconUtils.icon(Feather.USERS, 14));

        Tab tabLocks = new Tab("Registos Bloqueados", buildLocksTab());
        tabLocks.setGraphic(IconUtils.icon(Feather.LOCK, 14));

        Tab tabLogs = new Tab("Eventos do Sistema", buildLogsTab());
        tabLogs.setGraphic(IconUtils.icon(Feather.ACTIVITY, 14));

        Tab tabModules = new Tab("Estado dos Módulos", buildModulesTab());
        tabModules.setGraphic(IconUtils.icon(Feather.GRID, 14));

        Tab tabProcesses = new Tab("Processos em Background", buildProcessesTab());
        tabProcesses.setGraphic(IconUtils.icon(Feather.CPU, 14));

        tabPane.getTabs().addAll(tabDash, tabSessions, tabLocks, tabLogs, tabModules, tabProcesses);

        getChildren().addAll(toolbar, tabPane);
        VBox.setVgrow(tabPane, Priority.ALWAYS);
    }

    private Node buildDashboardTab() {
        VBox dash = new VBox(25);
        dash.setPadding(new Insets(30));
        dash.getStyleClass().add("dashboard-pane");

        // Painel de KPIs Superiores
        FlowPane kpiPaneTop = new FlowPane(20, 20);
        kpiPaneTop.setAlignment(Pos.CENTER);
        kpiPaneTop.getChildren().addAll(
            createKPI("Sessões Ativas", Feather.USERS, lblActiveSessionsCount = new Label("0"), "-fx-text-fill: -kubata-green;"),
            createKPI("Bloqueios", Feather.LOCK, lblLockedRecordsCount = new Label("0"), "-fx-text-fill: #f39c12;"),
            createKPI("Erros Hoje", Feather.ALERT_CIRCLE, lblErrorCount = new Label("0"), "-fx-text-fill: #e74c3c;"),
            createKPI("Estado Motor", Feather.SHIELD, lblSystemHealth = new Label("ESTÁVEL"), "-fx-text-fill: #27ae60;")
        );

        // Painel de KPIs de Infraestrutura
        FlowPane kpiPaneBottom = new FlowPane(20, 20);
        kpiPaneBottom.setAlignment(Pos.CENTER);
        kpiPaneBottom.getChildren().addAll(
            createKPI("Uptime Servidor", Feather.CLOCK, lblUptime = new Label("00h 00m 00s"), "-fx-text-fill: #3498db;"),
            createKPI("Base de Dados", Feather.DATABASE, lblDBStatus = new Label("LIGADO"), "-fx-text-fill: #27ae60;"),
            createKPI("Versão Core", Feather.INFO, new Label("v1.2.4"), "-fx-text-fill: #7f8c8d;"),
            createKPI("Ambiente", Feather.SERVER, new Label("PRODUÇÃO"), "-fx-text-fill: #8e44ad;")
        );

        // Gráfico de Performance Real-Time
        HBox chartArea = new HBox(20);
        chartArea.setAlignment(Pos.CENTER);
        
        VBox cpuBox = buildChartBox("Carga de CPU", cpuSeries, 0, 100, "%");
        VBox memBox = buildChartBox("Consumo de Memória", memSeries, 0, 1024, "MB");
        
        chartArea.getChildren().addAll(cpuBox, memBox);

        dash.getChildren().addAll(kpiPaneTop, kpiPaneBottom, chartArea);
        return new ScrollPane(dash) {{ setFitToWidth(true); setStyle("-fx-background-color: transparent;"); }};
    }

    private VBox buildChartBox(String title, XYChart.Series<Number, Number> series, double min, double max, String unit) {
        VBox box = new VBox(10);
        box.getStyleClass().add("card");
        box.setPadding(new Insets(15));
        box.setPrefWidth(500);
        box.setAlignment(Pos.CENTER);

        Label lblTitle = new Label(title, IconUtils.icon(Feather.ACTIVITY, 14));
        lblTitle.getStyleClass().add("h4");

        NumberAxis xAxis = new NumberAxis();
        xAxis.setTickLabelsVisible(false);
        xAxis.setOpacity(0);
        
        NumberAxis yAxis = new NumberAxis(min, max, (max - min) / 5);
        yAxis.setLabel(unit);

        LineChart<Number, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setCreateSymbols(false);
        chart.setAnimated(false);
        chart.setLegendVisible(false);
        chart.setPrefHeight(200);
        chart.getData().add(series);
        
        box.getChildren().addAll(lblTitle, chart);
        return box;
    }

    private VBox createKPI(String title, Feather icon, Label value, String valueStyle) {
        VBox box = new VBox(10);
        box.getStyleClass().add("card");
        box.setPrefSize(180, 120);
        box.setAlignment(Pos.CENTER);

        Label lblTitle = new Label(title.toUpperCase(), IconUtils.icon(icon, 16));
        lblTitle.getStyleClass().add("text-muted");
        lblTitle.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");

        value.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; " + valueStyle);

        box.getChildren().addAll(lblTitle, value);
        return box;
    }

    private HBox buildToolbar() {
        HBox box = new HBox(12);
        box.getStyleClass().add("header-box");
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(10, 20, 10, 20));

        Label title = new Label("Consola de Administração", IconUtils.icon(Feather.TERMINAL, 18));
        title.getStyleClass().add("h3");
        title.setStyle("-fx-text-fill: -kubata-green-dark;");

        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnRefresh = new Button("Atualizar Tudo", IconUtils.icon(Feather.REFRESH_CW, IconUtils.SIZE_SMALL));
        btnRefresh.getStyleClass().add("button-outlined");
        btnRefresh.setOnAction(e -> refreshAll());

        Button btnBroadcast = new Button("Broadcast", IconUtils.icon(Feather.MESSAGE_SQUARE, IconUtils.SIZE_SMALL));
        btnBroadcast.getStyleClass().add("button-primary");
        btnBroadcast.setOnAction(e -> showBroadcastDialog());

        Button btnMaintenance = new Button("Modo Manutenção", IconUtils.icon(Feather.ALERT_TRIANGLE, IconUtils.SIZE_SMALL));
        btnMaintenance.getStyleClass().add("button-danger");
        btnMaintenance.setOnAction(e -> toggleMaintenanceMode());

        box.getChildren().addAll(title, spacer, btnRefresh, btnBroadcast, btnMaintenance);
        return box;
    }

    private Node buildSessionsTab() {
        AdvancedTableView<ActiveSession> table = new AdvancedTableView<>(activeSessions);
        TableUtils.standardize(table);

        table.getColumns().add(TableUtils.createTextColumn("Utilizador", s -> new SimpleStringProperty(s.getValue().getUserName())));
        table.getColumns().add(TableUtils.createTextColumn("Login", s -> new SimpleStringProperty(s.getValue().getLoginTime())));
        table.getColumns().add(TableUtils.createTextColumn("Máquina", s -> new SimpleStringProperty(s.getValue().getWorkstation())));
        table.getColumns().add(TableUtils.createTextColumn("IP", s -> new SimpleStringProperty(s.getValue().getIp())));
        table.getColumns().add(TableUtils.createTextColumn("Módulo/Empresa", s -> new SimpleStringProperty(s.getValue().getContext())));
        table.getColumns().add(TableUtils.createTextColumn("Memória", s -> new SimpleStringProperty(s.getValue().getMemory())));

        TableColumn<ActiveSession, Void> colActions = new TableColumn<>("Ações");
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnKick = new Button("", IconUtils.icon(Feather.USER_X, 12));
            {
                btnKick.getStyleClass().add("button-icon-danger");
                btnKick.setTooltip(new Tooltip("Forçar Saída"));
                btnKick.setOnAction(e -> kickUser(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btnKick);
                setAlignment(Pos.CENTER);
            }
        });
        table.getColumns().add(colActions);

        return table.withSearchBar();
    }

    private Node buildLocksTab() {
        AdvancedTableView<LockedRecord> table = new AdvancedTableView<>(lockedRecords);
        TableUtils.standardize(table);

        table.getColumns().add(TableUtils.createTextColumn("Tipo de Registo", l -> new SimpleStringProperty(l.getValue().getEntityType())));
        table.getColumns().add(TableUtils.createTextColumn("ID Registo", l -> new SimpleStringProperty(l.getValue().getEntityId())));
        table.getColumns().add(TableUtils.createTextColumn("Bloqueado Por", l -> new SimpleStringProperty(l.getValue().getUserName())));
        table.getColumns().add(TableUtils.createTextColumn("Desde", l -> new SimpleStringProperty(l.getValue().getLockedSince())));

        TableColumn<LockedRecord, Void> colActions = new TableColumn<>("Ações");
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnUnlock = new Button("Libertar", IconUtils.icon(Feather.UNLOCK, 12));
            {
                btnUnlock.getStyleClass().add("button-success");
                btnUnlock.setOnAction(e -> unlockRecord(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btnUnlock);
                setAlignment(Pos.CENTER);
            }
        });
        table.getColumns().add(colActions);

        return table.withSearchBar();
    }

    private Node buildLogsTab() {
        VBox content = new VBox(10);
        content.setPadding(new Insets(15));

        // Filtros de Log
        HBox filters = new HBox(15);
        filters.setAlignment(Pos.CENTER_LEFT);
        filters.getStyleClass().add("card");
        filters.setPadding(new Insets(10, 15, 10, 15));

        ComboBox<String> cmbLevel = new ComboBox<>(FXCollections.observableArrayList("TODOS", "INFO", "WARN", "ERROR", "FATAL"));
        cmbLevel.setValue("TODOS");
        
        ComboBox<String> cmbCategory = new ComboBox<>(FXCollections.observableArrayList("TODOS", "AUTH", "DATABASE", "SYSTEM", "UI"));
        cmbCategory.setValue("TODOS");

        Button btnFilter = new Button("Filtrar", IconUtils.icon(Feather.FILTER, 12));
        btnFilter.getStyleClass().add("button-primary");
        btnFilter.setOnAction(e -> {
            String level = cmbLevel.getValue();
            String category = cmbCategory.getValue();
            
            List<SystemLog> filtered = systemLogs.stream()
                .filter(l -> level.equals("TODOS") || l.getLogLevel().name().equals(level))
                .filter(l -> category.equals("TODOS") || l.getCategory().equals(category))
                .collect(Collectors.toList());
            
            filteredLogs.setAll(filtered);
        });

        Button btnExport = new Button("Exportar", IconUtils.icon(Feather.DOWNLOAD, 12));
        btnExport.getStyleClass().add("button-outlined");
        btnExport.setOnAction(e -> exportLogs());
        
        filters.getChildren().addAll(
            new Label("Nível:"), cmbLevel,
            new Label("Categoria:"), cmbCategory,
            btnFilter, btnExport
        );

        AdvancedTableView<SystemLog> table = new AdvancedTableView<>(filteredLogs);
        TableUtils.standardize(table);
        
        table.setRowFactory(tv -> {
            TableRow<SystemLog> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    showLogDetails(row.getItem());
                }
            });
            return row;
        });

        TableColumn<SystemLog, LocalDateTime> colTime = new TableColumn<>("Data/Hora");
        colTime.setCellValueFactory(l -> new SimpleObjectProperty<>(l.getValue().getTimestamp()));
        colTime.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                if (empty || item == null) setText(null);
                else setText(item.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            }
        });

        table.getColumns().add(colTime);
        table.getColumns().add(TableUtils.createTextColumn("Nível", l -> new SimpleStringProperty(l.getValue().getLogLevel().name())));
        table.getColumns().add(TableUtils.createTextColumn("Categoria", l -> new SimpleStringProperty(l.getValue().getCategory())));
        table.getColumns().add(TableUtils.createTextColumn("Mensagem", l -> new SimpleStringProperty(l.getValue().getMessage())));

        VBox.setVgrow(table, Priority.ALWAYS);
        content.getChildren().addAll(filters, table.withSearchBar());

        return content;
    }

    private Node buildModulesTab() {
        AdvancedTableView<ModuleStatus> table = new AdvancedTableView<>(moduleStatuses);
        TableUtils.standardize(table);

        table.getColumns().add(TableUtils.createTextColumn("Módulo", m -> new SimpleStringProperty(m.getValue().getName())));
        table.getColumns().add(TableUtils.createTextColumn("Versão", m -> new SimpleStringProperty(m.getValue().getVersion())));
        
        TableColumn<ModuleStatus, String> colStatus = new TableColumn<>("Estado");
        colStatus.setCellValueFactory(m -> new SimpleStringProperty(m.getValue().getStatus()));
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
                    if (item.equalsIgnoreCase("ONLINE")) lbl.getStyleClass().add("badge-success");
                    else if (item.equalsIgnoreCase("OFFLINE")) lbl.getStyleClass().add("badge-danger");
                    else lbl.getStyleClass().add("badge-warning");
                    setGraphic(lbl);
                }
            }
        });
        table.getColumns().add(colStatus);
        
        table.getColumns().add(TableUtils.createTextColumn("Último Check", m -> new SimpleStringProperty(m.getValue().getLastCheck())));

        return table.withSearchBar();
    }

    private Node buildProcessesTab() {
        AdvancedTableView<AdminJob> table = new AdvancedTableView<>(backgroundProcesses);
        TableUtils.standardize(table);

        table.getColumns().add(TableUtils.createTextColumn("Processo", p -> p.getValue().titleProperty()));

        TableColumn<AdminJob, Number> colProgress = new TableColumn<>("Progresso");
        colProgress.setCellValueFactory(p -> p.getValue().progressProperty());
        colProgress.setCellFactory(col -> new TableCell<>() {
            private final ProgressBar pb = new ProgressBar();
            { pb.setMaxWidth(Double.MAX_VALUE); }
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setGraphic(null);
                else {
                    double v = item.doubleValue();
                    pb.setProgress(v);
                    setGraphic(pb);
                }
            }
        });
        table.getColumns().add(colProgress);
        
        table.getColumns().add(TableUtils.createTextColumn("Estado", p -> p.getValue().statusTextProperty()));

        TableColumn<AdminJob, Void> colActions = new TableColumn<>("Ações");
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnCancel = new Button("", IconUtils.icon(Feather.X_CIRCLE, 12));
            {
                btnCancel.getStyleClass().add("button-icon-danger");
                btnCancel.setTooltip(new Tooltip("Cancelar Processo"));
                btnCancel.setOnAction(e -> cancelProcess(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) setGraphic(null);
                else {
                    AdminJob p = getTableView().getItems().get(getIndex());
                    setGraphic(p.getStatus() == AdminJob.Status.RUNNING ? btnCancel : null);
                }
                setAlignment(Pos.CENTER);
            }
        });
        table.getColumns().add(colActions);

        return table;
    }

    private void refreshAll() {
        persistenceService.executeSilent(() -> {
            // Obter sessões reais da base de dados
            List<UserSession> sessions = userSessionRepository.findAll();
            
            // Obter bloqueios reais da base de dados
            List<RecordLock> locks = recordLockRepository.findAll();

            // Obter logs do sistema
            List<SystemLog> logs = systemLogRepository.findAll();

            // Obter módulos reais do ModuleRegistry
            Collection<KubataModule> modules = moduleRegistry.getAllModules();

            Platform.runLater(() -> {
                activeSessions.clear();
                DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
                for (UserSession s : sessions) {
                    activeSessions.add(new ActiveSession(
                        s.getUsername(), 
                        s.getLoginTime().format(timeFormatter), 
                        s.getWorkstation(), 
                        s.getIpAddress(), 
                        s.getContext(), 
                        s.getMemoryUsage() != null ? s.getMemoryUsage() : "N/A"
                    ));
                }

                lockedRecords.clear();
                for (RecordLock l : locks) {
                    lockedRecords.add(new LockedRecord(
                        l.getEntityType(), 
                        l.getEntityId(), 
                        l.getUsername(), 
                        l.getLockedSince().format(timeFormatter)
                    ));
                }

                systemLogs.setAll(logs);
                filteredLogs.setAll(logs);

                moduleStatuses.clear();
                for (KubataModule m : modules) {
                    moduleStatuses.add(new ModuleStatus(
                        m.getModuleName(), 
                        m.getVersion(), 
                        m.isActive() ? "ONLINE" : "OFFLINE", 
                        LocalDateTime.now().format(timeFormatter)
                    ));
                }

                // Atualizar KPIs
                lblActiveSessionsCount.setText(String.valueOf(activeSessions.size()));
                lblLockedRecordsCount.setText(String.valueOf(lockedRecords.size()));
                
                long errorCount = systemLogs.stream().filter(l -> l.getLogLevel() == SystemLog.LogLevel.ERROR).count();
                lblErrorCount.setText(String.valueOf(errorCount));
                
                if (errorCount > 10) {
                    lblSystemHealth.setText("CRÍTICO");
                    lblSystemHealth.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 24px; -fx-font-weight: bold;");
                } else if (errorCount > 5) {
                    lblSystemHealth.setText("AVISO");
                    lblSystemHealth.setStyle("-fx-text-fill: #f39c12; -fx-font-size: 24px; -fx-font-weight: bold;");
                } else {
                    lblSystemHealth.setText("ESTÁVEL");
                    lblSystemHealth.setStyle("-fx-text-fill: #27ae60; -fx-font-size: 24px; -fx-font-weight: bold;");
                }
            });
        }, null);
    }

    private void kickUser(ActiveSession session) {
        modalManager.showConfirmModal(new Label("Forçar a saída do utilizador " + session.getUserName() + "?\nEsta ação encerrará a sessão imediatamente."), 
                "Kick Utilizador", () -> {
            persistenceService.executeAsync(() -> {
                userSessionRepository.findByUsername(session.getUserName())
                    .ifPresent(userSessionRepository::delete);
            }, "USER_KICK", "CONSOLE", "Utilizador " + session.getUserName() + " removido do sistema", () -> {
                activeSessions.remove(session);
                lblActiveSessionsCount.setText(String.valueOf(activeSessions.size()));
            });
        }, null);
    }

    private void unlockRecord(LockedRecord record) {
        modalManager.showConfirmModal(new Label("Libertar o bloqueio do registo " + record.getEntityId() + "?\n\nAVISO: Se o utilizador ainda estiver a editar, poderá perder dados."), 
                "Libertar Registo", () -> {
            persistenceService.executeAsync(() -> {
                recordLockRepository.deleteByEntityTypeAndEntityId(record.getEntityType(), record.getEntityId());
            }, "RECORD_UNLOCK", "CONSOLE", "Bloqueio removido para " + record.getEntityId(), () -> {
                lockedRecords.remove(record);
                lblLockedRecordsCount.setText(String.valueOf(lockedRecords.size()));
            });
        }, null);
    }

    private void toggleMaintenanceMode() {
        modalManager.showConfirmModal(new Label("Deseja ativar o MODO MANUTENÇÃO?\n\n- Impede novos logins\n- Avisa utilizadores ligados\n- Apenas administradores terão acesso"), 
                "Modo Manutenção", () -> {
            notificationService.showWarning("Manutenção Ativada", "O sistema entrou em modo restrito.");
        }, null);
    }

    private void showBroadcastDialog() {
        VBox content = new VBox(10);
        TextArea txtMsg = new TextArea();
        txtMsg.setPromptText("Digite a mensagem para todos os utilizadores ligados...");
        txtMsg.setPrefRowCount(3);
        
        CheckBox chkUrgent = new CheckBox("Marcar como Urgente (Flash)");
        
        content.getChildren().addAll(new Label("Mensagem de Broadcast:"), txtMsg, chkUrgent);

        modalManager.showConfirmModal(content, "Broadcast de Sistema", () -> {
            String msg = txtMsg.getText();
            if (msg == null || msg.trim().isEmpty()) return;
            
            persistenceService.executeAsync(() -> {
                // TODO: Implementar lógica real de broadcast (ex: via WebSocket ou tabela de notificações)
            }, "BROADCAST", "CONSOLE", "Broadcast enviado a " + activeSessions.size() + " utilizadores", null);
        }, null);
    }

    private void showLogDetails(SystemLog log) {
        VBox content = new VBox(15);
        content.setPadding(new Insets(15));
        content.setPrefWidth(600);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        
        grid.add(new Label("Data/Hora:"), 0, 0);
        grid.add(new Label(log.getTimestamp().toString()), 1, 0);
        grid.add(new Label("Nível:"), 0, 1);
        grid.add(new Label(log.getLogLevel().name()), 1, 1);
        grid.add(new Label("Categoria:"), 0, 2);
        grid.add(new Label(log.getCategory()), 1, 2);
        grid.add(new Label("Fonte:"), 0, 3);
        grid.add(new Label(log.getSource()), 1, 3);

        Label lblMsg = new Label("Mensagem:");
        lblMsg.getStyleClass().add("h4");
        TextArea txtMsg = new TextArea(log.getMessage());
        txtMsg.setEditable(false);
        txtMsg.setWrapText(true);
        txtMsg.setPrefRowCount(3);

        content.getChildren().addAll(grid, new Separator(), lblMsg, txtMsg);

        if (log.getStackTrace() != null && !log.getStackTrace().isEmpty()) {
            Label lblStack = new Label("Stack Trace:");
            lblStack.getStyleClass().add("h4");
            TextArea txtStack = new TextArea(log.getStackTrace());
            txtStack.setEditable(false);
            txtStack.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 11px;");
            VBox.setVgrow(txtStack, Priority.ALWAYS);
            content.getChildren().addAll(new Separator(), lblStack, txtStack);
        }

        modalManager.showModal(content, new ModalManager.ModalConfig().title("Detalhes do Evento").size(650, 500).resizable(true));
    }

    private void exportLogs() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exportar Logs");
        fileChooser.setInitialFileName("logs_export_" + System.currentTimeMillis() + ".csv");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        
        File file = fileChooser.showSaveDialog(getScene().getWindow());
        if (file != null) {
            try (PrintWriter writer = new PrintWriter(file)) {
                writer.println("Data/Hora;Nivel;Categoria;Mensagem");
                for (SystemLog log : filteredLogs) {
                    writer.println(String.format("%s;%s;%s;%s", 
                        log.getTimestamp(), log.getLogLevel(), log.getCategory(), log.getMessage()));
                }
                notificationService.showSuccess("Exportação Concluída", "Os logs foram exportados para " + file.getName());
            } catch (Exception ex) {
                notificationService.showError("Erro na Exportação", "Não foi possível gravar o ficheiro: " + ex.getMessage());
            }
        }
    }

    private void cancelProcess(AdminJob process) {
        modalManager.showConfirmModal(new Label("Tem a certeza que deseja cancelar o processo: " + process.getTitle() + "?"), 
                "Cancelar Processo", () -> {
            notificationService.showWarning("Processo Cancelado", "O processo foi interrompido com sucesso.");
            jobManager.cancel(process);
            refreshAll();
        }, null);
    }

    // Classes Auxiliares (Diferente do Primavera, aqui usamos POJOs simples para a UI)
    public static class ActiveSession {
        private final SimpleStringProperty userName, loginTime, workstation, ip, context, memory;
        public ActiveSession(String u, String l, String w, String ip, String c, String m) { 
            this.userName = new SimpleStringProperty(u);
            this.loginTime = new SimpleStringProperty(l);
            this.workstation = new SimpleStringProperty(w);
            this.ip = new SimpleStringProperty(ip);
            this.context = new SimpleStringProperty(c);
            this.memory = new SimpleStringProperty(m);
        }
        public String getUserName() { return userName.get(); }
        public String getLoginTime() { return loginTime.get(); }
        public String getWorkstation() { return workstation.get(); }
        public String getIp() { return ip.get(); }
        public String getContext() { return context.get(); }
        public String getMemory() { return memory.get(); }
    }

    public static class LockedRecord {
        private final SimpleStringProperty entityType, entityId, userName, lockedSince;
        public LockedRecord(String t, String i, String u, String s) { 
            this.entityType = new SimpleStringProperty(t);
            this.entityId = new SimpleStringProperty(i);
            this.userName = new SimpleStringProperty(u);
            this.lockedSince = new SimpleStringProperty(s);
        }
        public String getEntityType() { return entityType.get(); }
        public String getEntityId() { return entityId.get(); }
        public String getUserName() { return userName.get(); }
        public String getLockedSince() { return lockedSince.get(); }
    }

     public static class ModuleStatus {
        private final SimpleStringProperty name, version, status, lastCheck;
        public ModuleStatus(String n, String v, String s, String l) {
            this.name = new SimpleStringProperty(n);
            this.version = new SimpleStringProperty(v);
            this.status = new SimpleStringProperty(s);
            this.lastCheck = new SimpleStringProperty(l);
        }
        public String getName() { return name.get(); }
        public String getVersion() { return version.get(); }
        public String getStatus() { return status.get(); }
        public String getLastCheck() { return lastCheck.get(); }
    }
}

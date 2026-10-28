package ao.allon.kubata.admin.view;

import ao.allon.kubata.admin.service.NotificationService;
import ao.allon.kubata.core.service.MaintenanceModeService;
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
import ao.allon.kubata.core.domain.BackupRecord;
import ao.allon.kubata.core.repository.SystemLogRepository;
import ao.allon.kubata.core.repository.BackupRecordRepository;
import ao.allon.kubata.core.repository.UserRepository;
import ao.allon.kubata.core.repository.UserSessionRepository;
import ao.allon.kubata.core.repository.RecordLockRepository;
import ao.allon.kubata.core.ui.table.AdvancedTableView;
import ao.allon.kubata.core.ui.table.TableUtils;
import ao.allon.kubata.core.module.ModuleRegistry;
import ao.allon.kubata.core.module.KubataModule;
import ao.allon.kubata.core.module.communication.ModuleCommunicationService;
import javafx.application.Platform;
import java.util.Collection;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.SelectionMode;
import javafx.scene.Node;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import org.kordamp.ikonli.feather.Feather;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.io.File;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
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
    private final MaintenanceModeService maintenanceModeService;
    private final Environment environment;
    private final JobManager jobManager;
    private final ModuleRegistry moduleRegistry;
    private final ModuleCommunicationService moduleCommunicationService;
    private final BackupRecordRepository backupRecordRepository;

    /** Últimos eventos carregados na consola (tabela paginada, sem fazer findAll completo). */
    private static final int MAX_LOGS_CARREGADOS = 500;

    /** Blocos de bloqueio considerados "antigos" para alerta na Consola. */
    private static final Duration LIMITE_BLOQUEIO_ANTIGO = Duration.ofMinutes(15);

    private final TabPane tabPane = new TabPane();

    // Referências de abas (navegação a partir dos alertas do Painel de Controlo)
    private Tab tabDash;
    private Tab tabSessions;
    private Tab tabLocks;
    private Tab tabLogs;
    private Tab tabModules;
    private Tab tabProcesses;

    // Barra de estado inferior
    private Label lblStatusSessions;
    private Label lblStatusLocks;
    private Label lblStatusLogs;
    private Label lblStatusModules;
    private Label lblLastRefresh;

    // Actualização automática
    private CheckBox chkAutoRefresh;
    private volatile boolean autoRefreshEnabled;
    private int autoRefreshTicks;

    // Filtros da tab de Eventos
    private ComboBox<String> cmbLevel;
    private ComboBox<String> cmbCategory;
    private TextField txtLogSearch;
    private Label lblLogCount;

    // Tabela de bloqueios (selecção múltipla para "Anular")
    private AdvancedTableView<LockedRecord> locksTable;

    // Painel de alertas do Painel de Controlo
    private VBox alertsContainer;
    
    // KPIs Dashboard
    private Label lblActiveSessionsCount;
    private Label lblLockedRecordsCount;
    private Label lblSystemHealth;
    private Label lblErrorCount;
    private Label lblUptime;
    private Label lblDBStatus;
    private Label maintenanceStatusLabel;
    
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
                       BackupRecordRepository backupRecordRepository,
                       SessionManager sessionManager, 
                       ModalManager modalManager,
                       PersistenceService persistenceService,
                       NotificationService notificationService,
                       MaintenanceModeService maintenanceModeService,
                       Environment environment,
                       JobManager jobManager,
                       ModuleRegistry moduleRegistry,
                       ModuleCommunicationService moduleCommunicationService) {
        this.systemLogRepository = systemLogRepository;
        this.userRepository = userRepository;
        this.userSessionRepository = userSessionRepository;
        this.recordLockRepository = recordLockRepository;
        this.backupRecordRepository = backupRecordRepository;
        this.sessionManager = sessionManager;
        this.modalManager = modalManager;
        this.persistenceService = persistenceService;
        this.notificationService = notificationService;
        this.maintenanceModeService = maintenanceModeService;
        this.environment = environment;
        this.jobManager = jobManager;
        this.moduleRegistry = moduleRegistry;
        this.moduleCommunicationService = moduleCommunicationService;
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
        memSeries.setName("Memória usada (MB)");

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            t.setName("ConsolePerformanceMonitor");
            return t;
        });

        OperatingSystemMXBean osBean =
                ManagementFactory.getOperatingSystemMXBean();

        scheduler.scheduleAtFixedRate(() -> {
            double cpu = 0.0;

            try {
                if (osBean instanceof com.sun.management.OperatingSystemMXBean sunOs) {
                    cpu = sunOs.getSystemCpuLoad() * 100.0;
                } else if (osBean.getSystemLoadAverage() >= 0) {
                    cpu = Math.min(100.0,
                            (osBean.getSystemLoadAverage()
                                    / Math.max(1, osBean.getAvailableProcessors()))
                                    * 100.0);
                }
            } catch (Exception ex) {
                logger.debug("CPU não disponível: {}", ex.getMessage());
            }

            if (Double.isNaN(cpu) || cpu < 0) {
                cpu = 0.0;
            }

            Runtime runtime = Runtime.getRuntime();
            double usedMemory =
                    (runtime.totalMemory() - runtime.freeMemory())
                            / (1024.0 * 1024.0);

            boolean dbOk = true;
            try {
                userRepository.count();
            } catch (Exception ex) {
                dbOk = false;
                logger.warn("Health check da base de dados falhou: {}",
                        ex.getMessage());
            }

            final double finalCpu = Math.min(100.0, cpu);
            final double finalMemory = usedMemory;
            final boolean finalDbOk = dbOk;

            Platform.runLater(() -> {
                cpuSeries.getData().add(
                        new XYChart.Data<>(chartTime, finalCpu));
                memSeries.getData().add(
                        new XYChart.Data<>(chartTime, finalMemory));

                if (cpuSeries.getData().size() > 30) {
                    cpuSeries.getData().remove(0);
                    memSeries.getData().remove(0);
                }

                chartTime++;
                updateUptime();
                tickAutoRefresh();

                if (lblDBStatus != null) {
                    lblDBStatus.setText(finalDbOk ? "LIGADO" : "ERRO");
                    lblDBStatus.setStyle(
                            finalDbOk
                                    ? "-fx-text-fill: #27ae60; -fx-font-weight: bold;"
                                    : "-fx-text-fill: #e74c3c; -fx-font-weight: bold;"
                    );
                }
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
        // O health check é executado pelo scheduler de performance para
        // evitar consultas de base de dados na JavaFX Application Thread.
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
        
        tabPane.getStyleClass().addAll("office365-tabs", "console-tab-content");
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        tabDash = new Tab("Painel de Controlo", buildDashboardTab());
        tabDash.setGraphic(IconUtils.icon(Feather.LAYOUT, 14));

        tabSessions = new Tab("Utilizadores Ligados", buildSessionsTab());
        tabSessions.setGraphic(IconUtils.icon(Feather.USERS, 14));

        tabLocks = new Tab("Registos Bloqueados", buildLocksTab());
        tabLocks.setGraphic(IconUtils.icon(Feather.LOCK, 14));

        tabLogs = new Tab("Eventos do Sistema", buildLogsTab());
        tabLogs.setGraphic(IconUtils.icon(Feather.ACTIVITY, 14));

        tabModules = new Tab("Estado dos Módulos", buildModulesTab());
        tabModules.setGraphic(IconUtils.icon(Feather.GRID, 14));

        tabProcesses = new Tab("Processos em Background", buildProcessesTab());
        tabProcesses.setGraphic(IconUtils.icon(Feather.CPU, 14));

        tabPane.getTabs().addAll(tabDash, tabSessions, tabLocks, tabLogs, tabModules, tabProcesses);

        HBox statusBar = buildStatusBar();

        getChildren().addAll(toolbar, tabPane, statusBar);
        VBox.setVgrow(tabPane, Priority.ALWAYS);
    }

    /**
     * Barra de estado inferior, no estilo da Consola Primavera:
     * resume em tempo real o que está a acontecer no sistema.
     */
    private HBox buildStatusBar() {
        HBox bar = new HBox(18);
        bar.getStyleClass().add("console-statusbar");
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(6, 18, 6, 18));

        lblStatusSessions = statusChip(Feather.USERS, "Sessões: 0");
        lblStatusLocks = statusChip(Feather.LOCK, "Bloqueios: 0");
        lblStatusLogs = statusChip(Feather.ACTIVITY, "Eventos: 0");
        lblStatusModules = statusChip(Feather.GRID, "Módulos: 0/0");

        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        lblLastRefresh = new Label("—");
        lblLastRefresh.getStyleClass().add("text-muted");
        lblLastRefresh.setGraphic(IconUtils.icon(Feather.CLOCK, 12));

        bar.getChildren().addAll(
                lblStatusSessions,
                lblStatusLocks,
                lblStatusLogs,
                lblStatusModules,
                spacer,
                lblLastRefresh
        );
        return bar;
    }

    private Label statusChip(Feather icon, String text) {
        Label label = new Label(text, IconUtils.icon(icon, 12));
        label.getStyleClass().add("console-status-chip");
        return label;
    }

    /** Dispara a actualização automática (de 10 em 10 segundos) quando activa. */
    private void tickAutoRefresh() {
        if (!autoRefreshEnabled) {
            return;
        }

        if (++autoRefreshTicks >= 5) {
            autoRefreshTicks = 0;
            refreshAll();
        }
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
            createKPI("Versão Core", Feather.INFO, new Label(resolveCoreVersion()), "-fx-text-fill: #7f8c8d;"),
            createKPI("Ambiente", Feather.SERVER, new Label(resolveEnvironmentName()), "-fx-text-fill: #8e44ad;")
        );

        // Gráfico de Performance Real-Time
        HBox chartArea = new HBox(20);
        chartArea.setAlignment(Pos.CENTER);
        
        VBox cpuBox = buildChartBox("Carga de CPU", cpuSeries, 0, 100, "%");
        double maxMemoryMb = Math.max(512, Math.ceil(Runtime.getRuntime().maxMemory() / (1024.0 * 1024.0)));
        VBox memBox = buildChartBox("Consumo de Memória", memSeries, 0, maxMemoryMb, "MB");
        
        chartArea.getChildren().addAll(cpuBox, memBox);

        Node alertsCard = buildAlertsCard();

        dash.getChildren().addAll(kpiPaneTop, kpiPaneBottom, chartArea, alertsCard);
        ScrollPane scroll = new ScrollPane(dash);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("transparent-scroll");
        return scroll;
    }

    private VBox buildChartBox(String title, XYChart.Series<Number, Number> series, double min, double max, String unit) {
        VBox box = new VBox(10);
        box.getStyleClass().add("console-chart-card");
        box.setPadding(new Insets(15));
        box.setMinWidth(360);
        HBox.setHgrow(box, Priority.ALWAYS);

        Label lblTitle = new Label(title, IconUtils.icon(Feather.ACTIVITY, 14));
        lblTitle.getStyleClass().add("h4");

        NumberAxis xAxis = new NumberAxis();
        xAxis.setTickLabelsVisible(false);
        xAxis.setOpacity(0);

        double safeMax = Math.max(max, min + 1);
        NumberAxis yAxis = new NumberAxis(min, safeMax, Math.max(1, (safeMax - min) / 5));
        yAxis.setLabel(unit);
        yAxis.setForceZeroInRange(min == 0);

        LineChart<Number, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setCreateSymbols(false);
        chart.setAnimated(false);
        chart.setLegendVisible(false);
        chart.setPrefHeight(210);
        chart.setMinHeight(180);
        chart.getData().add(series);

        box.getChildren().addAll(lblTitle, chart);
        return box;
    }

    /** Painel de alertas de administração, no espírito da Consola Primavera V10. */
    private Node buildAlertsCard() {
        VBox card = new VBox(8);
        card.getStyleClass().addAll("console-chart-card", "console-alerts-card");
        card.setPadding(new Insets(16));

        Label title = new Label("Alertas de Administração", IconUtils.icon(Feather.BELL, 15));
        title.getStyleClass().add("h4");

        Label subtitle = new Label(
                "Condições do sistema que exigem atenção: backups, módulos, bloqueios e eventos"
        );
        subtitle.getStyleClass().add("text-muted");

        alertsContainer = new VBox(6);

        card.getChildren().addAll(title, subtitle, new Separator(), alertsContainer);
        return card;
    }

    private record Alerta(String severidade, String mensagem, String accaoLabel, Runnable accao) {}

    /** Recalcula a lista de alertas apresentada no Painel de Controlo. */
    private void updateAlertas(List<RecordLock> locks, long errosHoje,
                               Collection<KubataModule> modulos, BackupRecord ultimoBackup) {
        if (alertsContainer == null) {
            return;
        }

        LocalDateTime agora = LocalDateTime.now();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        List<Alerta> alertas = new ArrayList<>();

        // 1. Modo de manutenção
        if (maintenanceModeService.isEnabled()) {
            alertas.add(new Alerta(
                    "danger",
                    "Modo de manutenção activo desde " + maintenanceModeService.getSince()
                            + (maintenanceModeService.getReason() == null
                                ? ""
                                : " — " + maintenanceModeService.getReason()),
                    null, null));
        } else {
            alertas.add(new Alerta(
                    "success",
                    "Sistema operacional: modo de manutenção desactivado.",
                    null, null));
        }

        // 2. Backups desactualizados
        if (ultimoBackup == null) {
            alertas.add(new Alerta(
                    "danger",
                    "Não existe nenhum backup concluído registado. Consulte o módulo Backup.",
                    null, null));
        } else {
            LocalDateTime quando = ultimoBackup.getStartTime();
            long horas = quando == null ? 0 : Duration.between(quando, agora).toHours();

            if (quando == null || horas >= 24) {
                alertas.add(new Alerta(
                        "warning",
                        "O último backup concluído tem " + horas + " horas ("
                                + ultimoBackup.getFilename() + ").",
                        null, null));
            } else {
                alertas.add(new Alerta(
                        "success",
                        "Último backup concluído: " + quando.format(fmt)
                                + " — " + ultimoBackup.getFilename(),
                        null, null));
            }
        }

        // 3. Bloqueios antigos
        LocalDateTime limite = agora.minus(LIMITE_BLOQUEIO_ANTIGO);
        long antigos = locks.stream()
                .filter(l -> l.getLockedSince() != null && l.getLockedSince().isBefore(limite))
                .count();

        if (antigos > 0) {
            alertas.add(new Alerta(
                    "warning",
                    antigos + " bloqueio(s) de registos com mais de "
                            + LIMITE_BLOQUEIO_ANTIGO.toMinutes() + " minutos.",
                    "Ver bloqueios",
                    () -> tabPane.getSelectionModel().select(tabLocks)));
        } else {
            alertas.add(new Alerta(
                    "success",
                    "Sem bloqueios antigos de registos.",
                    null, null));
        }

        // 4. Módulos inactivos
        long inactivos = modulos.stream().filter(m -> !m.isActive()).count();
        if (inactivos > 0) {
            alertas.add(new Alerta(
                    "warning",
                    inactivos + " módulo(s) registado(s) inactivo(s) de " + modulos.size() + ".",
                    "Ver módulos",
                    () -> tabPane.getSelectionModel().select(tabModules)));
        } else {
            alertas.add(new Alerta(
                    "success",
                    "Todos os " + modulos.size() + " módulos registados estão activos.",
                    null, null));
        }

        // 5. Erros do dia
        if (errosHoje > 10) {
            alertas.add(new Alerta(
                    "danger",
                    errosHoje + " erros registados hoje no sistema.",
                    "Ver eventos",
                    () -> tabPane.getSelectionModel().select(tabLogs)));
        } else if (errosHoje > 0) {
            alertas.add(new Alerta(
                    "warning",
                    errosHoje + " erro(s) registado(s) hoje no sistema.",
                    "Ver eventos",
                    () -> tabPane.getSelectionModel().select(tabLogs)));
        } else {
            alertas.add(new Alerta(
                    "success",
                    "Sem erros registados hoje no sistema.",
                    null, null));
        }

        alertsContainer.getChildren().clear();
        for (Alerta alerta : alertas) {
            alertsContainer.getChildren().add(createAlertRow(alerta));
        }
    }

    private HBox createAlertRow(Alerta alerta) {
        HBox row = new HBox(10);
        row.getStyleClass().addAll("console-alert-row", "console-alert-" + alerta.severidade());
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(8, 12, 8, 12));

        Feather icon = switch (alerta.severidade()) {
            case "danger" -> Feather.X_CIRCLE;
            case "warning" -> Feather.ALERT_TRIANGLE;
            default -> Feather.CHECK_CIRCLE;
        };

        Label lblIcon = new Label("", IconUtils.icon(icon, 15));
        lblIcon.getStyleClass().add("console-alert-icon-" + alerta.severidade());

        Label lblMsg = new Label(alerta.mensagem());
        lblMsg.setWrapText(true);
        HBox.setHgrow(lblMsg, Priority.ALWAYS);

        row.getChildren().addAll(lblIcon, lblMsg);

        if (alerta.accao() != null && alerta.accaoLabel() != null) {
            Button btn = new Button(alerta.accaoLabel(), IconUtils.icon(Feather.CHEVRON_RIGHT, 12));
            btn.getStyleClass().add("button-flat");
            btn.setOnAction(e -> alerta.accao().run());
            row.getChildren().add(btn);
        }

        return row;
    }

    private VBox createKPI(String title, Feather icon, Label value, String valueStyle) {
        VBox box = new VBox(8);
        box.getStyleClass().add("console-kpi-card");
        box.setPrefSize(190, 118);
        box.setMinWidth(160);
        box.setAlignment(Pos.CENTER_LEFT);

        Label lblTitle = new Label(title.toUpperCase(), IconUtils.icon(icon, 16));
        lblTitle.getStyleClass().add("console-kpi-title");

        value.getStyleClass().removeAll("console-kpi-value");
        value.getStyleClass().add("console-kpi-value");
        value.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; " + valueStyle);

        box.getChildren().addAll(lblTitle, value);
        return box;
    }

    private HBox buildToolbar() {
        HBox box = new HBox(12);
        box.getStyleClass().add("console-toolbar");
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(10, 18, 10, 18));

        VBox heading = new VBox(2);
        Label title = new Label(
                "Consola de Administração",
                IconUtils.icon(Feather.TERMINAL, 18)
        );
        title.getStyleClass().add("h3");

        Label subtitle = new Label(
                "Monitorização operacional, sessões, segurança, logs e processos"
        );
        subtitle.getStyleClass().add("text-muted");

        heading.getChildren().addAll(title, subtitle);

        Label maintenance = new Label();
        maintenance.getStyleClass().add("console-maintenance-badge");
        maintenanceStatusLabel = maintenance;
        updateMaintenanceIndicator();

        chkAutoRefresh = new CheckBox("Actualização automática");
        chkAutoRefresh.setSelected(autoRefreshEnabled);
        chkAutoRefresh.setTooltip(new Tooltip(
                "Actualiza sessões, bloqueios, eventos e módulos de 10 em 10 segundos"));
        chkAutoRefresh.setOnAction(e -> {
            autoRefreshEnabled = chkAutoRefresh.isSelected();
            autoRefreshTicks = 0;
            if (autoRefreshEnabled) {
                refreshAll();
            }
        });

        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnRefresh = new Button(
                "Actualizar",
                IconUtils.icon(Feather.REFRESH_CW, IconUtils.SIZE_SMALL)
        );
        btnRefresh.getStyleClass().add("button-outlined");
        btnRefresh.setTooltip(new Tooltip("Actualizar todas as métricas e tabelas"));
        btnRefresh.setOnAction(e -> refreshAll());

        Button btnBroadcast = new Button(
                "Mensagem",
                IconUtils.icon(Feather.MESSAGE_SQUARE, IconUtils.SIZE_SMALL)
        );
        btnBroadcast.getStyleClass().add("button-primary");
        btnBroadcast.setTooltip(new Tooltip("Registar mensagem operacional"));
        btnBroadcast.setOnAction(e -> showBroadcastDialog());

        Button btnMaintenance = new Button(
                "Manutenção",
                IconUtils.icon(Feather.ALERT_TRIANGLE, IconUtils.SIZE_SMALL)
        );
        btnMaintenance.getStyleClass().add("button-danger");
        btnMaintenance.setTooltip(new Tooltip("Activar ou desactivar o modo de manutenção"));
        btnMaintenance.setOnAction(e -> toggleMaintenanceMode());

        box.getChildren().addAll(
                heading,
                maintenance,
                spacer,
                chkAutoRefresh,
                btnRefresh,
                btnBroadcast,
                btnMaintenance
        );
        return box;
    }

    private Node buildSessionsTab() {
        AdvancedTableView<ActiveSession> table = new AdvancedTableView<>(activeSessions);
        TableUtils.standardize(table);

        table.getColumns().add(TableUtils.createTextColumn("Utilizador", s -> new SimpleStringProperty(s.getValue().getUserName())));
        table.getColumns().add(TableUtils.createTextColumn("Login", s -> new SimpleStringProperty(s.getValue().getLoginTime())));
        table.getColumns().add(TableUtils.createTextColumn("Duração", s -> new SimpleStringProperty(s.getValue().getDuracaoTexto())));
        table.getColumns().add(TableUtils.createTextColumn("Máquina", s -> new SimpleStringProperty(s.getValue().getWorkstation())));
        table.getColumns().add(TableUtils.createTextColumn("IP", s -> new SimpleStringProperty(s.getValue().getIp())));
        table.getColumns().add(TableUtils.createTextColumn("Módulo/Empresa", s -> new SimpleStringProperty(s.getValue().getContext())));
        table.getColumns().add(TableUtils.createTextColumn("Memória", s -> new SimpleStringProperty(s.getValue().getMemory())));

        TableColumn<ActiveSession, Void> colActions = new TableColumn<>("Ações");
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnMsg = new Button("", IconUtils.icon(Feather.MESSAGE_SQUARE, 12));
            private final Button btnKick = new Button("", IconUtils.icon(Feather.USER_X, 12));
            private final HBox actions = new HBox(4, btnMsg, btnKick);
            {
                btnMsg.getStyleClass().add("button-icon");
                btnMsg.setTooltip(new Tooltip("Enviar mensagem ao utilizador"));
                btnMsg.setOnAction(e -> sendMessageToUser(getTableView().getItems().get(getIndex())));

                btnKick.getStyleClass().add("button-icon-danger");
                btnKick.setTooltip(new Tooltip("Forçar Saída"));
                btnKick.setOnAction(e -> kickUser(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : actions);
                setAlignment(Pos.CENTER);
            }
        });
        table.getColumns().add(colActions);

        return table.withSearchBar();
    }

    private Node buildLocksTab() {
        locksTable = new AdvancedTableView<>(lockedRecords);
        TableUtils.standardize(locksTable);
        locksTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        locksTable.getColumns().add(locksTable.createSelectionColumn());

        locksTable.getColumns().add(TableUtils.createTextColumn("Tipo de Registo", l -> new SimpleStringProperty(l.getValue().getEntityType())));
        locksTable.getColumns().add(TableUtils.createTextColumn("ID Registo", l -> new SimpleStringProperty(l.getValue().getEntityId())));
        locksTable.getColumns().add(TableUtils.createTextColumn("Bloqueado Por", l -> new SimpleStringProperty(l.getValue().getUserName())));
        locksTable.getColumns().add(TableUtils.createTextColumn("Desde", l -> new SimpleStringProperty(l.getValue().getLockedSince())));
        locksTable.getColumns().add(TableUtils.createTextColumn("Duração", l -> new SimpleStringProperty(l.getValue().getDuracaoTexto())));

        TableColumn<LockedRecord, Void> colActions = new TableColumn<>("Ações");
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnUnlock = new Button("", IconUtils.icon(Feather.UNLOCK, 12));
            {
                btnUnlock.getStyleClass().add("button-icon-success");
                btnUnlock.setTooltip(new Tooltip("Anular Bloqueio"));
                btnUnlock.setOnAction(e -> unlockRecord(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btnUnlock);
                setAlignment(Pos.CENTER);
            }
        });
        locksTable.getColumns().add(colActions);

        Button btnUnlockSelected = new Button(
                "Anular seleção",
                IconUtils.icon(Feather.UNLOCK, IconUtils.SIZE_SMALL)
        );
        btnUnlockSelected.getStyleClass().add("button-success");
        btnUnlockSelected.setTooltip(new Tooltip(
                "Anula os bloqueios dos registos seleccionados (como no Primavera V10)"));
        btnUnlockSelected.setOnAction(e -> unlockSelectedRecords());

        Button btnRefreshLocks = new Button(
                "Actualizar",
                IconUtils.icon(Feather.REFRESH_CW, IconUtils.SIZE_SMALL)
        );
        btnRefreshLocks.getStyleClass().add("button-outlined");
        btnRefreshLocks.setOnAction(e -> refreshAll());

        Label hint = new Label(
                "Seleccione um ou mais registos e clique em \"Anular seleção\" para libertar os bloqueios.");
        hint.getStyleClass().add("text-muted");

        HBox bar = new HBox(10, btnUnlockSelected, btnRefreshLocks, hint);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("console-tab-toolbar");
        bar.setPadding(new Insets(8, 14, 8, 14));

        Node content = locksTable.withSearchBar();
        VBox.setVgrow(content, Priority.ALWAYS);

        VBox box = new VBox(0, bar, content);
        return box;
    }

    private Node buildLogsTab() {
        VBox content = new VBox(10);
        content.setPadding(new Insets(15));

        // Barra de filtros com aplicação automática (sem botão "Filtrar")
        HBox filters = new HBox(12);
        filters.setAlignment(Pos.CENTER_LEFT);
        filters.getStyleClass().add("console-tab-toolbar");
        filters.setPadding(new Insets(10, 14, 10, 14));

        cmbLevel = new ComboBox<>(FXCollections.observableArrayList("TODOS", "DEBUG", "INFO", "WARN", "ERROR", "FATAL"));
        cmbLevel.setValue("TODOS");
        cmbLevel.setPrefWidth(120);

        cmbCategory = new ComboBox<>(FXCollections.observableArrayList("TODOS"));
        cmbCategory.setValue("TODOS");
        cmbCategory.setPrefWidth(170);

        txtLogSearch = new TextField();
        txtLogSearch.setPromptText("Pesquisar mensagem, fonte ou categoria...");
        txtLogSearch.setPrefWidth(320);
        HBox.setHgrow(txtLogSearch, Priority.ALWAYS);

        lblLogCount = new Label("0 eventos");
        lblLogCount.getStyleClass().add("text-muted");

        cmbLevel.valueProperty().addListener((obs, o, n) -> applyLogFilters());
        cmbCategory.valueProperty().addListener((obs, o, n) -> applyLogFilters());
        txtLogSearch.textProperty().addListener((obs, o, n) -> applyLogFilters());

        Button btnExport = new Button("Exportar", IconUtils.icon(Feather.DOWNLOAD, IconUtils.SIZE_SMALL));
        btnExport.getStyleClass().add("button-outlined");
        btnExport.setTooltip(new Tooltip("Exporta os eventos filtrados para ficheiro CSV"));
        btnExport.setOnAction(e -> exportLogs());

        filters.getChildren().addAll(
            new Label("Nível:"), cmbLevel,
            new Label("Categoria:"), cmbCategory,
            new Label("Pesquisar:"), txtLogSearch,
            lblLogCount,
            btnExport
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
        TableColumn<SystemLog, String> colLevel = new TableColumn<>("Nível");
        colLevel.setCellValueFactory(l -> new SimpleStringProperty(
                l.getValue().getLogLevel() != null
                        ? l.getValue().getLogLevel().name()
                        : "INFO"
        ));
        colLevel.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                Label badge = new Label(item);
                badge.getStyleClass().add("console-log-badge");

                switch (item) {
                    case "ERROR", "FATAL" -> badge.getStyleClass().add("console-log-error");
                    case "WARN" -> badge.getStyleClass().add("console-log-warning");
                    default -> badge.getStyleClass().add("console-log-info");
                }

                setText(null);
                setGraphic(badge);
                setAlignment(Pos.CENTER);
            }
        });

        TableColumn<SystemLog, String> colCategory = new TableColumn<>("Categoria");
        colCategory.setCellValueFactory(l ->
                new SimpleStringProperty(l.getValue().getCategory()));

        TableColumn<SystemLog, String> colMessage = new TableColumn<>("Mensagem");
        colMessage.setCellValueFactory(l ->
                new SimpleStringProperty(l.getValue().getMessage()));

        table.getColumns().addAll(colLevel, colCategory, colMessage);

        VBox.setVgrow(table, Priority.ALWAYS);
        content.getChildren().addAll(filters, table.withSearchBar());

        return content;
    }

    /** Aplica os filtros da tab de Eventos (nível, categoria e texto livre). */
    private void applyLogFilters() {
        if (cmbLevel == null || cmbCategory == null) {
            return;
        }

        String level = cmbLevel.getValue();
        String category = cmbCategory.getValue();
        String termo = txtLogSearch == null || txtLogSearch.getText() == null
                ? ""
                : txtLogSearch.getText().trim().toLowerCase();

        List<SystemLog> filtered = systemLogs.stream()
                .filter(l -> l != null)
                .filter(l -> "TODOS".equals(level)
                        || (l.getLogLevel() != null && l.getLogLevel().name().equals(level)))
                .filter(l -> "TODOS".equals(category)
                        || (l.getCategory() != null && l.getCategory().equals(category)))
                .filter(l -> termo.isEmpty()
                        || containsTerm(l.getMessage(), termo)
                        || containsTerm(l.getSource(), termo)
                        || containsTerm(l.getCategory(), termo))
                .collect(Collectors.toList());

        filteredLogs.setAll(filtered);

        if (lblLogCount != null) {
            lblLogCount.setText(filtered.size() + " de " + systemLogs.size() + " eventos");
        }
    }

    private static boolean containsTerm(String valor, String termoLower) {
        return valor != null && valor.toLowerCase().contains(termoLower);
    }

    private Node buildModulesTab() {
        AdvancedTableView<ModuleStatus> table = new AdvancedTableView<>(moduleStatuses);
        TableUtils.standardize(table);

        table.getColumns().add(TableUtils.createTextColumn("Módulo", m -> new SimpleStringProperty(m.getValue().getName())));
        table.getColumns().add(TableUtils.createTextColumn("Descrição", m -> new SimpleStringProperty(m.getValue().getDescription())));
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
        table.getColumns().add(TableUtils.createTextColumn("Tipo", p -> new SimpleStringProperty(p.getValue().getActionType())));

        TableColumn<AdminJob, String> colCreatedAt = new TableColumn<>("Iniciado");
        colCreatedAt.setCellValueFactory(p -> new SimpleStringProperty(
                p.getValue().getCreatedAt() == null
                        ? ""
                        : p.getValue().getCreatedAt().format(DateTimeFormatter.ofPattern("HH:mm:ss"))));
        table.getColumns().add(colCreatedAt);

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
            List<SystemLog> logs = systemLogRepository
                    .findByOrderByTimestampDesc(PageRequest.of(0, MAX_LOGS_CARREGADOS))
                    .getContent();

            // Obter módulos reais do ModuleRegistry
            Collection<KubataModule> modules = moduleRegistry.getAllModules();

            // Último backup concluído (indicador no estilo da Consola Primavera)
            BackupRecord ultimoBackup = backupRecordRepository
                    .findTopByStatusOrderByStartTimeDesc(BackupRecord.BackupStatus.COMPLETED)
                    .orElse(null);

            Platform.runLater(() -> {
                activeSessions.clear();
                DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
                for (UserSession s : sessions) {
                    activeSessions.add(new ActiveSession(
                        s.getUsername(), 
                        s.getLoginTime() != null ? s.getLoginTime().format(timeFormatter) : "—",
                        s.getLoginTime(), 
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
                        l.getLockedSince() != null ? l.getLockedSince().format(timeFormatter) : "—",
                        l.getLockedSince()
                    ));
                }

                systemLogs.setAll(logs);
                refreshLogCategories();
                applyLogFilters();

                moduleStatuses.clear();
                for (KubataModule m : modules) {
                    moduleStatuses.add(new ModuleStatus(
                        m.getModuleName(), 
                        m.getModuleDescription(),
                        m.getVersion(), 
                        m.isActive() ? "ONLINE" : "OFFLINE", 
                        LocalDateTime.now().format(timeFormatter)
                    ));
                }

                // Atualizar KPIs
                lblActiveSessionsCount.setText(String.valueOf(activeSessions.size()));
                lblLockedRecordsCount.setText(String.valueOf(lockedRecords.size()));
                
                LocalDateTime todayStart = LocalDateTime.now().toLocalDate().atStartOfDay();
                long errorCount = systemLogs.stream()
                        .filter(l -> l.getLogLevel() == SystemLog.LogLevel.ERROR)
                        .filter(l -> l.getTimestamp() != null && !l.getTimestamp().isBefore(todayStart))
                        .count();
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

                // Alertas de administração e barra de estado
                updateAlertas(locks, errorCount, modules, ultimoBackup);
                updateStatusBar(modules);
            });
        }, null);
    }

    /** Actualiza a combo de categorias com as categorias realmente existentes nos eventos. */
    private void refreshLogCategories() {
        if (cmbCategory == null) {
            return;
        }

        String actual = cmbCategory.getValue();

        List<String> opcoes = new ArrayList<>();
        opcoes.add("TODOS");
        systemLogs.stream()
                .map(SystemLog::getCategory)
                .filter(c -> c != null && !c.isBlank())
                .distinct()
                .sorted()
                .forEach(opcoes::add);

        cmbCategory.getItems().setAll(opcoes);
        if (actual == null || !opcoes.contains(actual)) {
            cmbCategory.setValue("TODOS");
        }
    }

    /** Barra de estado inferior: resume o estado operacional da Consola. */
    private void updateStatusBar(Collection<KubataModule> modules) {
        if (lblStatusSessions == null) {
            return;
        }

        lblStatusSessions.setText("Sessões: " + activeSessions.size());
        lblStatusLocks.setText("Bloqueios: " + lockedRecords.size());
        lblStatusLogs.setText("Eventos: " + systemLogs.size());

        long activos = modules == null ? 0 : modules.stream().filter(KubataModule::isActive).count();
        long total = modules == null ? 0 : modules.size();
        lblStatusModules.setText("Módulos: " + activos + "/" + total);

        lblLastRefresh.setText("Última actualização: "
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
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
        boolean enabled = maintenanceModeService.isEnabled();

        String title = enabled
                ? "Desativar Modo de Manutenção"
                : "Ativar Modo de Manutenção";

        String text = enabled
                ? "O sistema está em modo de manutenção desde "
                    + maintenanceModeService.getSince()
                    + ".\n\nPretende libertar o sistema para utilizadores normais?"
                : "Ativar o modo de manutenção?\n\n"
                    + "Novos logins de utilizadores não administrativos serão bloqueados. "
                    + "As operações administrativas continuarão disponíveis.";

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(5));

        Label reasonLabel = new Label("Motivo:");
        TextField reason = new TextField(
                enabled ? maintenanceModeService.getReason() : "Manutenção programada"
        );
        reason.setPrefWidth(420);
        reason.setPromptText("Motivo apresentado aos utilizadores");

        grid.add(new Label(text), 0, 0, 2, 1);

        if (!enabled) {
            grid.add(reasonLabel, 0, 1);
            grid.add(reason, 1, 1);
        }

        String user = sessionManager.getUser() != null
                ? sessionManager.getUser().getNome()
                : "Administrador";

        modalManager.showConfirmModal(
                grid,
                title,
                () -> persistenceService.executeAsync(() -> {
                    if (enabled) {
                        maintenanceModeService.disable(user);
                    } else {
                        maintenanceModeService.enable(reason.getText(), user);
                    }
                },
                "MAINTENANCE",
                (enabled ? "Desativação" : "Ativação")
                        + " do modo de manutenção",
                () -> {
                    boolean nowEnabled = maintenanceModeService.isEnabled();
                    notificationService.showInfo(
                            nowEnabled
                                    ? "Manutenção Ativada"
                                    : "Manutenção Desativada",
                            nowEnabled
                                    ? maintenanceModeService.getReason()
                                    : "O acesso normal foi restaurado."
                    );
                    refreshAll();
                }),
                null
        );
    }

    private void showBroadcastDialog() {
        VBox content = new VBox(10);

        TextArea txtMsg = new TextArea();
        txtMsg.setPromptText(
                "Mensagem operacional para os utilizadores..."
        );
        txtMsg.setPrefRowCount(4);
        txtMsg.setWrapText(true);

        CheckBox chkUrgent = new CheckBox("Marcar como urgente");

        content.getChildren().addAll(
                new Label("Mensagem do sistema:"),
                txtMsg,
                chkUrgent
        );

        modalManager.showConfirmModal(
                content,
                "Mensagem de Sistema",
                () -> {
                    String msg = txtMsg.getText() == null
                            ? ""
                            : txtMsg.getText().trim();
                    boolean urgent = chkUrgent.isSelected();

                    if (msg.isBlank()) {
                        modalManager.alert(
                                "Mensagem inválida",
                                "Introduza uma mensagem antes de enviar.",
                                "warning",
                                null
                        );
                        return;
                    }

                    persistenceService.executeAsync(() -> {
                        SystemLog log = new SystemLog();
                        log.setLogLevel(
                                urgent
                                        ? SystemLog.LogLevel.WARN
                                        : SystemLog.LogLevel.INFO
                        );
                        log.setCategory("BROADCAST");
                        log.setSource("Kubata Administrator");
                        log.setMessage(msg);
                        log.setThreadName(Thread.currentThread().getName());
                        systemLogRepository.save(log);

                        if (moduleCommunicationService != null) {
                            moduleCommunicationService.broadcast(
                                    "ADMIN",
                                    "SYSTEM_BROADCAST",
                                    java.util.Map.of(
                                            "message", msg,
                                            "urgent", urgent,
                                            "source", "Kubata Administrator"
                                    )
                            );
                        }
                    },
                    "BROADCAST",
                    "CONSOLE",
                    "Mensagem operacional emitida para o ecossistema Kubata",
                    () -> {
                        if (urgent) {
                            notificationService.showWarning(
                                    "Mensagem do Sistema",
                                    msg
                            );
                        } else {
                            notificationService.showInfo(
                                    "Mensagem do Sistema",
                                    msg
                            );
                        }
                        refreshAll();
                    });
                },
                null
        );
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
            try (PrintWriter writer = new PrintWriter(file, java.nio.charset.StandardCharsets.UTF_8)) {
                writer.println("\uFEFFData/Hora;Nivel;Categoria;Mensagem");
                for (SystemLog log : filteredLogs) {
                    writer.println(
                            csvValue(log.getTimestamp()) + ";" +
                            csvValue(log.getLogLevel()) + ";" +
                            csvValue(log.getCategory()) + ";" +
                            csvValue(log.getMessage())
                    );
                }
                notificationService.showSuccess(
                        "Exportação Concluída",
                        "Os logs foram exportados para " + file.getName()
                );
            } catch (Exception ex) {
                notificationService.showError(
                        "Erro na Exportação",
                        "Não foi possível gravar o ficheiro: " + ex.getMessage()
                );
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
        private final ObjectProperty<LocalDateTime> loginDateTime;
        public ActiveSession(String u, String l, LocalDateTime loginDateTime, String w, String ip, String c, String m) { 
            this.userName = new SimpleStringProperty(u);
            this.loginTime = new SimpleStringProperty(l);
            this.loginDateTime = new SimpleObjectProperty<>(loginDateTime);
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
        public LocalDateTime getLoginDateTime() { return loginDateTime.get(); }
        public String getDuracaoTexto() { return formatDuracao(loginDateTime.get()); }
    }

    public static class LockedRecord {
        private final SimpleStringProperty entityType, entityId, userName, lockedSince;
        private final ObjectProperty<LocalDateTime> lockedSinceTime;
        public LockedRecord(String t, String i, String u, String s, LocalDateTime lockedSince) { 
            this.entityType = new SimpleStringProperty(t);
            this.entityId = new SimpleStringProperty(i);
            this.userName = new SimpleStringProperty(u);
            this.lockedSince = new SimpleStringProperty(s);
            this.lockedSinceTime = new SimpleObjectProperty<>(lockedSince);
        }
        public String getEntityType() { return entityType.get(); }
        public String getEntityId() { return entityId.get(); }
        public String getUserName() { return userName.get(); }
        public String getLockedSince() { return lockedSince.get(); }
        public LocalDateTime getLockedSinceTime() { return lockedSinceTime.get(); }
        public String getDuracaoTexto() { return formatDuracao(lockedSinceTime.get()); }
    }

     public static class ModuleStatus {
        private final SimpleStringProperty name, description, version, status, lastCheck;
        public ModuleStatus(String n, String d, String v, String s, String l) {
            this.name = new SimpleStringProperty(n);
            this.description = new SimpleStringProperty(d);
            this.version = new SimpleStringProperty(v);
            this.status = new SimpleStringProperty(s);
            this.lastCheck = new SimpleStringProperty(l);
        }
        public String getName() { return name.get(); }
        public String getDescription() { return description.get(); }
        public String getVersion() { return version.get(); }
        public String getStatus() { return status.get(); }
        public String getLastCheck() { return lastCheck.get(); }
    }

    /** Formata a duração decorrida desde uma data/hora (ex.: "2h 05m"). */
    private static String formatDuracao(LocalDateTime desde) {
        if (desde == null) {
            return "—";
        }

        long minutos = Duration.between(desde, LocalDateTime.now()).toMinutes();
        if (minutos < 0) {
            minutos = 0;
        }

        long h = minutos / 60;
        long m = minutos % 60;
        return h > 0 ? h + "h " + String.format("%02dm", m) : m + "m";
    }

    /** Anula em lote os bloqueios dos registos seleccionados (botão "Anular" do Primavera V10). */
    private void unlockSelectedRecords() {
        List<LockedRecord> selection = locksTable == null
                ? new ArrayList<>()
                : new ArrayList<>(locksTable.getSelectionModel().getSelectedItems());

        if (selection.isEmpty()) {
            notificationService.showWarning(
                    "Sem seleção",
                    "Seleccione um ou mais registos bloqueados para anular.");
            return;
        }

        modalManager.showConfirmModal(new Label(
                        "Anular " + selection.size() + " bloqueio(s) de registos?\n\n"
                                + "AVISO: Se os utilizadores ainda estiverem a editar esses registos, "
                                + "podem perder dados."),
                "Anular Registos Bloqueados", () -> {
                    persistenceService.executeAsync(() -> {
                        for (LockedRecord record : selection) {
                            recordLockRepository.deleteByEntityTypeAndEntityId(
                                    record.getEntityType(), record.getEntityId());
                        }
                    }, "RECORD_UNLOCK", "CONSOLE",
                            selection.size() + " bloqueio(s) de registos anulados",
                            () -> {
                                lockedRecords.removeAll(selection);
                                lblLockedRecordsCount.setText(String.valueOf(lockedRecords.size()));
                                updateStatusBar(moduleRegistry.getAllModules());
                                notificationService.showSuccess(
                                        "Bloqueios Anulados",
                                        selection.size() + " bloqueio(s) libertado(s).");
                            });
                }, null);
    }

    /** Envia uma mensagem operacional a um utilizador específico ligado. */
    private void sendMessageToUser(ActiveSession session) {
        VBox content = new VBox(10);

        Label info = new Label("Mensagem dirigida a " + session.getUserName()
                + " (" + session.getWorkstation() + ")");
        info.getStyleClass().add("text-muted");

        TextArea txtMsg = new TextArea();
        txtMsg.setPromptText("Mensagem operacional para o utilizador...");
        txtMsg.setPrefRowCount(4);
        txtMsg.setWrapText(true);

        CheckBox chkUrgent = new CheckBox("Marcar como urgente");

        content.getChildren().addAll(info, txtMsg, chkUrgent);

        modalManager.showConfirmModal(content, "Mensagem ao Utilizador", () -> {
            String msg = txtMsg.getText() == null ? "" : txtMsg.getText().trim();

            if (msg.isBlank()) {
                modalManager.alert(
                        "Mensagem inválida",
                        "Introduza uma mensagem antes de enviar.",
                        "warning",
                        null
                );
                return;
            }

            boolean urgent = chkUrgent.isSelected();

            persistenceService.executeAsync(() -> {
                SystemLog log = new SystemLog();
                log.setLogLevel(urgent ? SystemLog.LogLevel.WARN : SystemLog.LogLevel.INFO);
                log.setCategory("MESSAGE");
                log.setSource("Kubata Administrator");
                log.setMessage("Mensagem para " + session.getUserName() + ": " + msg);
                log.setThreadName(Thread.currentThread().getName());
                systemLogRepository.save(log);

                if (moduleCommunicationService != null) {
                    moduleCommunicationService.broadcast(
                            "ADMIN",
                            "USER_MESSAGE",
                            java.util.Map.of(
                                    "targetUser", session.getUserName(),
                                    "message", msg,
                                    "urgent", urgent,
                                    "source", "Kubata Administrator"
                            )
                    );
                }
            }, "MESSAGE", "CONSOLE",
                    "Mensagem enviada ao utilizador " + session.getUserName(),
                    () -> {
                        notificationService.showSuccess(
                                "Mensagem Enviada",
                                "Mensagem entregue ao utilizador " + session.getUserName() + "."
                        );
                        refreshAll();
                    });
        }, null);
    }
    private void updateMaintenanceIndicator() {
        if (maintenanceStatusLabel == null) {
            return;
        }

        if (maintenanceModeService.isEnabled()) {
            maintenanceStatusLabel.setText("● MANUTENÇÃO");
            maintenanceStatusLabel.getStyleClass().removeAll(
                    "console-maintenance-ok",
                    "console-maintenance-warning"
            );
            if (!maintenanceStatusLabel.getStyleClass().contains("console-maintenance-on")) {
                maintenanceStatusLabel.getStyleClass().add("console-maintenance-on");
            }
        } else {
            maintenanceStatusLabel.setText("● OPERACIONAL");
            maintenanceStatusLabel.getStyleClass().removeAll(
                    "console-maintenance-on",
                    "console-maintenance-warning"
            );
            if (!maintenanceStatusLabel.getStyleClass().contains("console-maintenance-ok")) {
                maintenanceStatusLabel.getStyleClass().add("console-maintenance-ok");
            }
        }
    }

    private String resolveEnvironmentName() {
        String active = environment == null
                ? null
                : String.join(",", environment.getActiveProfiles());

        if (active == null || active.isBlank()) {
            active = environment == null
                    ? null
                    : environment.getProperty("spring.profiles.active");
        }

        if (active == null || active.isBlank()) {
            return "PADRÃO";
        }

        return active.toUpperCase();
    }

    private String resolveCoreVersion() {
        Package pkg = User.class.getPackage();
        String version = pkg != null ? pkg.getImplementationVersion() : null;

        if (version == null || version.isBlank()) {
            version = environment == null
                    ? null
                    : environment.getProperty("kubata.core.version");
        }

        return version == null || version.isBlank()
                ? "DEV"
                : version;
    }

    private static String csvValue(Object value) {
        if (value == null) {
            return "";
        }

        String text = String.valueOf(value)
                .replace("\r", " ")
                .replace("\n", " ")
                .replace("\"", "\"\"");

        return (text.contains(";") || text.contains("\""))
                ? "\"" + text + "\""
                : text;
    }

}

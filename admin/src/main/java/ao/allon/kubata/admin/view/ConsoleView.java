package ao.allon.kubata.admin.view;

import ao.allon.kubata.admin.service.NotificationService;
import ao.allon.kubata.admin.service.MaintenanceModeService;
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
import ao.allon.kubata.core.domain.Role;
import ao.allon.kubata.core.repository.SystemLogRepository;
import ao.allon.kubata.core.repository.UserRepository;
import ao.allon.kubata.core.repository.UserSessionRepository;
import ao.allon.kubata.core.repository.RecordLockRepository;
import ao.allon.kubata.core.service.UserAdministrationService;
import ao.allon.kubata.core.ui.table.AdvancedTableView;
import ao.allon.kubata.core.ui.table.TableUtils;
import ao.allon.kubata.core.module.ModuleRegistry;
import ao.allon.kubata.core.module.KubataModule;
import ao.allon.kubata.core.module.communication.ModuleCommunicationService;
import javafx.application.Platform;
import java.util.Collection;
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
import org.springframework.core.env.Environment;
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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
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
    private final UserAdministrationService userAdministrationService;

    private final TabPane tabPane = new TabPane();
    
    // KPIs Dashboard
    private Label lblActiveSessionsCount;
    private Label lblLockedRecordsCount;
    private Label lblSystemHealth;
    private Label lblErrorCount;
    private Label lblUptime;
    private Label lblDBStatus;
    private Label maintenanceStatusLabel;

    // Dashboard moderno
    private Label lblUsersCount;
    private Label lblActiveUsersCount;
    private Label lblModulesOnlineCount;
    private Label lblRunningJobsCount;
    private Label lblLastRefresh;
    private VBox recentActivityBox;
    
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
                       MaintenanceModeService maintenanceModeService,
                       Environment environment,
                       JobManager jobManager,
                       ModuleRegistry moduleRegistry,
                       ModuleCommunicationService moduleCommunicationService,
                       UserAdministrationService userAdministrationService) {
        this.systemLogRepository = systemLogRepository;
        this.userRepository = userRepository;
        this.userSessionRepository = userSessionRepository;
        this.recordLockRepository = recordLockRepository;
        this.sessionManager = sessionManager;
        this.modalManager = modalManager;
        this.persistenceService = persistenceService;
        this.notificationService = notificationService;
        this.maintenanceModeService = maintenanceModeService;
        this.environment = environment;
        this.jobManager = jobManager;
        this.moduleRegistry = moduleRegistry;
        this.moduleCommunicationService = moduleCommunicationService;
        this.userAdministrationService = userAdministrationService;
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
        VBox dash = new VBox(18);
        dash.setPadding(new Insets(22));
        dash.getStyleClass().add("dashboard-pane");

        // ── Cabeçalho executivo ─────────────────────────────────────────────
        HBox hero = new HBox(18);
        hero.setAlignment(Pos.CENTER_LEFT);
        hero.getStyleClass().add("console-dashboard-hero");

        VBox heroText = new VBox(5);
        Label eyebrow = new Label(
                "CENTRO DE COMANDO · ADMINISTRATOR",
                IconUtils.icon(Feather.SHIELD, 12)
        );
        eyebrow.getStyleClass().add("console-dashboard-eyebrow");

        Label title = new Label("Visão Geral do Kubata");
        title.getStyleClass().add("console-dashboard-title");

        Label subtitle = new Label(
                "Estado operacional, segurança, desempenho e actividade do sistema num único painel."
        );
        subtitle.setWrapText(true);
        subtitle.getStyleClass().add("console-dashboard-subtitle");

        heroText.getChildren().addAll(eyebrow, title, subtitle);

        Pane heroSpacer = new Pane();
        HBox.setHgrow(heroSpacer, Priority.ALWAYS);

        VBox heroStatus = new VBox(4);
        heroStatus.setAlignment(Pos.CENTER_RIGHT);

        Label operational = new Label("SISTEMA OPERACIONAL");
        operational.getStyleClass().add("console-dashboard-operational");

        Label updated = new Label("A monitorizar em tempo real");
        updated.getStyleClass().add("console-dashboard-live");

        heroStatus.getChildren().addAll(operational, updated);
        hero.getChildren().addAll(heroText, heroSpacer, heroStatus);

        // ── KPIs ─────────────────────────────────────────────────────────────
        FlowPane kpis = new FlowPane(12, 12);
        kpis.setPrefWrapLength(1120);

        kpis.getChildren().addAll(
                createDashboardKpi(
                        "Sessões activas",
                        Feather.USERS,
                        lblActiveSessionsCount = new Label("0"),
                        "Utilizadores ligados"
                ),
                createDashboardKpi(
                        "Utilizadores",
                        Feather.USER,
                        lblUsersCount = new Label("0"),
                        "Contas registadas"
                ),
                createDashboardKpi(
                        "Utilizadores activos",
                        Feather.USER_CHECK,
                        lblActiveUsersCount = new Label("0"),
                        "Contas habilitadas"
                ),
                createDashboardKpi(
                        "Bloqueios",
                        Feather.LOCK,
                        lblLockedRecordsCount = new Label("0"),
                        "Registos em edição"
                ),
                createDashboardKpi(
                        "Erros hoje",
                        Feather.ALERT_TRIANGLE,
                        lblErrorCount = new Label("0"),
                        "Eventos críticos"
                ),
                createDashboardKpi(
                        "Módulos online",
                        Feather.GRID,
                        lblModulesOnlineCount = new Label("0"),
                        "Componentes activos"
                )
        );

        // ── Operação + acções rápidas ───────────────────────────────────────
        HBox commandRow = new HBox(14);
        commandRow.setAlignment(Pos.TOP_LEFT);

        VBox healthCard = buildOperationalHealthCard();
        VBox quickActions = buildQuickActionsCard();

        HBox.setHgrow(healthCard, Priority.ALWAYS);
        HBox.setHgrow(quickActions, Priority.ALWAYS);

        commandRow.getChildren().addAll(healthCard, quickActions);

        // ── Gráficos ─────────────────────────────────────────────────────────
        HBox chartArea = new HBox(14);
        chartArea.setAlignment(Pos.CENTER_LEFT);

        VBox cpuBox = buildChartBox(
                "Carga de CPU",
                cpuSeries,
                0,
                100,
                "%"
        );

        double maxMemoryMb = Math.max(
                512,
                Math.ceil(Runtime.getRuntime().maxMemory() / (1024.0 * 1024.0))
        );

        VBox memBox = buildChartBox(
                "Consumo de Memória",
                memSeries,
                0,
                maxMemoryMb,
                "MB"
        );

        HBox.setHgrow(cpuBox, Priority.ALWAYS);
        HBox.setHgrow(memBox, Priority.ALWAYS);
        chartArea.getChildren().addAll(cpuBox, memBox);

        // ── Actividade recente ──────────────────────────────────────────────
        VBox activity = buildRecentActivityCard();

        // ── Infraestrutura ──────────────────────────────────────────────────
        FlowPane infra = new FlowPane(12, 12);
        infra.setPrefWrapLength(1120);
        infra.getChildren().addAll(
                createDashboardInfoCard(
                        "Uptime",
                        Feather.CLOCK,
                        lblUptime = new Label("00h 00m 00s")
                ),
                createDashboardInfoCard(
                        "Base de dados",
                        Feather.DATABASE,
                        lblDBStatus = new Label("LIGADO")
                ),
                createDashboardInfoCard(
                        "Processos em execução",
                        Feather.CPU,
                        lblRunningJobsCount = new Label("0")
                ),
                createDashboardInfoCard(
                        "Versão Core",
                        Feather.INFO,
                        new Label(resolveCoreVersion())
                ),
                createDashboardInfoCard(
                        "Ambiente",
                        Feather.SERVER,
                        new Label(resolveEnvironmentName())
                ),
                createDashboardInfoCard(
                        "Última actualização",
                        Feather.REFRESH_CW,
                        lblLastRefresh = new Label("—")
                )
        );

        VBox section = new VBox(8);
        Label sectionTitle = new Label("Infraestrutura e operação");
        sectionTitle.getStyleClass().add("console-dashboard-section-title");
        Label sectionSubtitle = new Label(
                "Indicadores técnicos para acompanhamento diário do ambiente Kubata."
        );
        sectionSubtitle.getStyleClass().add("console-dashboard-section-subtitle");
        section.getChildren().addAll(sectionTitle, sectionSubtitle, infra);

        dash.getChildren().addAll(
                hero,
                kpis,
                commandRow,
                chartArea,
                activity,
                section
        );

        ScrollPane scroll = new ScrollPane(dash);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("transparent-scroll");
        return scroll;
    }

    private VBox createDashboardKpi(
            String title,
            Feather icon,
            Label value,
            String hint) {

        VBox box = new VBox(6);
        box.getStyleClass().add("console-dashboard-kpi");
        box.setPrefWidth(178);
        box.setMinWidth(155);
        box.setPrefHeight(105);

        HBox top = new HBox(8);
        top.setAlignment(Pos.CENTER_LEFT);

        StackPane iconBox = new StackPane();
        iconBox.getStyleClass().add("console-dashboard-kpi-icon");
        iconBox.setPrefSize(32, 32);
        iconBox.setMinSize(32, 32);
        iconBox.setMaxSize(32, 32);
        iconBox.getChildren().add(IconUtils.icon(icon, 15));

        Label label = new Label(title.toUpperCase());
        label.getStyleClass().add("console-dashboard-kpi-title");

        top.getChildren().addAll(iconBox, label);

        value.getStyleClass().add("console-dashboard-kpi-value");

        Label detail = new Label(hint);
        detail.getStyleClass().add("console-dashboard-kpi-hint");

        box.getChildren().addAll(top, value, detail);
        return box;
    }

    private VBox createDashboardInfoCard(
            String title,
            Feather icon,
            Label value) {

        VBox box = new VBox(5);
        box.getStyleClass().add("console-dashboard-info");
        box.setPrefWidth(175);
        box.setMinWidth(150);
        box.setPrefHeight(82);

        Label titleLabel = new Label(title.toUpperCase(), IconUtils.icon(icon, 12));
        titleLabel.getStyleClass().add("console-dashboard-info-title");

        value.getStyleClass().add("console-dashboard-info-value");
        box.getChildren().addAll(titleLabel, value);
        return box;
    }

    private VBox buildOperationalHealthCard() {
        VBox card = new VBox(10);
        card.getStyleClass().add("console-dashboard-panel");
        card.setPadding(new Insets(14));
        card.setMinHeight(160);

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label(
                "Saúde operacional",
                IconUtils.icon(Feather.HEART, 14)
        );
        title.getStyleClass().add("console-dashboard-panel-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label badge = new Label("MONITORIZAÇÃO ACTIVA");
        badge.getStyleClass().add("console-dashboard-live-badge");

        header.getChildren().addAll(title, spacer, badge);

        HBox body = new HBox(12);
        body.setAlignment(Pos.CENTER_LEFT);

        StackPane shield = new StackPane();
        shield.getStyleClass().add("console-dashboard-health-icon");
        shield.setPrefSize(52, 52);
        shield.setMinSize(52, 52);
        shield.setMaxSize(52, 52);
        shield.getChildren().add(IconUtils.icon(Feather.SHIELD, 24));

        VBox statusBox = new VBox(3);
        lblSystemHealth = new Label("ESTÁVEL");
        lblSystemHealth.getStyleClass().add("console-dashboard-health-value");

        Label description = new Label(
                "CPU, memória, base de dados, sessões, bloqueios e erros estão sob acompanhamento."
        );
        description.setWrapText(true);
        description.getStyleClass().add("console-dashboard-panel-text");

        statusBox.getChildren().addAll(lblSystemHealth, description);
        body.getChildren().addAll(shield, statusBox);

        Button diagnostics = new Button(
                "Executar diagnóstico",
                IconUtils.icon(Feather.ACTIVITY, 12)
        );
        diagnostics.getStyleClass().add("button-outlined");
        diagnostics.setOnAction(e -> showSystemDiagnostics());

        HBox footer = new HBox(diagnostics);
        footer.setAlignment(Pos.CENTER_RIGHT);

        card.getChildren().addAll(header, body, footer);
        return card;
    }

    private VBox buildQuickActionsCard() {
        VBox card = new VBox(10);
        card.getStyleClass().add("console-dashboard-panel");
        card.setPadding(new Insets(14));
        card.setMinHeight(160);

        Label title = new Label(
                "Acções rápidas",
                IconUtils.icon(Feather.ZAP, 14)
        );
        title.getStyleClass().add("console-dashboard-panel-title");

        Label hint = new Label(
                "Aceda directamente às operações mais utilizadas pela administração."
        );
        hint.getStyleClass().add("console-dashboard-panel-text");

        FlowPane actions = new FlowPane(7, 7);
        actions.getChildren().addAll(
                quickAction("Sessões", Feather.USERS, () -> selectConsoleTab("Utilizadores Ligados")),
                quickAction("Bloqueios", Feather.LOCK, () -> selectConsoleTab("Registos Bloqueados")),
                quickAction("Eventos", Feather.ACTIVITY, () -> selectConsoleTab("Eventos do Sistema")),
                quickAction("Módulos", Feather.GRID, () -> selectConsoleTab("Estado dos Módulos")),
                quickAction("Processos", Feather.CPU, () -> selectConsoleTab("Processos em Background")),
                quickAction("Exportar logs", Feather.DOWNLOAD, this::exportLogs)
        );

        card.getChildren().addAll(title, hint, actions);
        return card;
    }

    private Button quickAction(String text, Feather icon, Runnable action) {
        Button button = new Button(text, IconUtils.icon(icon, 12));
        button.getStyleClass().add("console-dashboard-quick-action");
        button.setOnAction(e -> action.run());
        return button;
    }

    private VBox buildRecentActivityCard() {
        VBox card = new VBox(9);
        card.getStyleClass().add("console-dashboard-panel");
        card.setPadding(new Insets(14));

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label(
                "Actividade recente",
                IconUtils.icon(Feather.LIST, 14)
        );
        title.getStyleClass().add("console-dashboard-panel-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button seeAll = new Button(
                "Ver todos",
                IconUtils.icon(Feather.CHEVRON_RIGHT, 11)
        );
        seeAll.getStyleClass().add("button-outlined");
        seeAll.setOnAction(e -> selectConsoleTab("Eventos do Sistema"));

        header.getChildren().addAll(title, spacer, seeAll);

        recentActivityBox = new VBox(0);
        recentActivityBox.getStyleClass().add("console-dashboard-activity-list");

        updateRecentActivity();
        card.getChildren().addAll(header, recentActivityBox);
        return card;
    }

    private void updateRecentActivity() {
        if (recentActivityBox == null) {
            return;
        }

        recentActivityBox.getChildren().clear();

        List<SystemLog> recent = systemLogs.stream()
                .filter(java.util.Objects::nonNull)
                .sorted(java.util.Comparator.comparing(
                        SystemLog::getTimestamp,
                        java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())
                ))
                .limit(5)
                .toList();

        if (recent.isEmpty()) {
            Label empty = new Label("Nenhuma actividade recente registada.");
            empty.getStyleClass().add("console-dashboard-empty");
            recentActivityBox.getChildren().add(empty);
            return;
        }

        for (SystemLog log : recent) {
            HBox row = new HBox(9);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("console-dashboard-activity-row");

            StackPane iconBox = new StackPane();
            iconBox.getStyleClass().add("console-dashboard-activity-icon");
            iconBox.setPrefSize(28, 28);
            iconBox.setMinSize(28, 28);
            iconBox.setMaxSize(28, 28);

            Feather icon = log.getLogLevel() == SystemLog.LogLevel.ERROR
                    ? Feather.ALERT_CIRCLE
                    : log.getLogLevel() == SystemLog.LogLevel.WARN
                            ? Feather.ALERT_TRIANGLE
                            : Feather.CHECK_CIRCLE;
            iconBox.getChildren().add(IconUtils.icon(icon, 13));

            VBox text = new VBox(2);
            String category = log.getCategory() == null || log.getCategory().isBlank()
                    ? "EVENTO"
                    : log.getCategory();

            Label line = new Label(
                    category + " · "
                            + (log.getMessage() == null || log.getMessage().isBlank()
                            ? "Sem descrição"
                            : log.getMessage())
            );
            line.setWrapText(true);
            line.getStyleClass().add("console-dashboard-activity-title");

            String time = log.getTimestamp() == null
                    ? "—"
                    : log.getTimestamp().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));

            Label detail = new Label(
                    time + " · " + (log.getSource() == null ? "Kubata" : log.getSource())
            );
            detail.getStyleClass().add("console-dashboard-activity-detail");

            text.getChildren().addAll(line, detail);
            HBox.setHgrow(text, Priority.ALWAYS);
            row.getChildren().addAll(iconBox, text);
            recentActivityBox.getChildren().add(row);
        }
    }

    private void selectConsoleTab(String title) {
        tabPane.getTabs().stream()
                .filter(tab -> title.equals(tab.getText()))
                .findFirst()
                .ifPresent(tab -> tabPane.getSelectionModel().select(tab));
    }

    private void showSystemDiagnostics() {
        StringBuilder report = new StringBuilder();

        report.append("Estado geral: ")
                .append(lblSystemHealth == null ? "—" : lblSystemHealth.getText())
                .append("\n\n");

        report.append("Base de dados: ")
                .append(lblDBStatus == null ? "—" : lblDBStatus.getText())
                .append("\n");

        report.append("Sessões activas: ")
                .append(lblActiveSessionsCount == null ? "—" : lblActiveSessionsCount.getText())
                .append("\n");

        report.append("Utilizadores activos: ")
                .append(lblActiveUsersCount == null ? "—" : lblActiveUsersCount.getText())
                .append("\n");

        report.append("Bloqueios: ")
                .append(lblLockedRecordsCount == null ? "—" : lblLockedRecordsCount.getText())
                .append("\n");

        report.append("Erros hoje: ")
                .append(lblErrorCount == null ? "—" : lblErrorCount.getText())
                .append("\n");

        report.append("Módulos online: ")
                .append(lblModulesOnlineCount == null ? "—" : lblModulesOnlineCount.getText())
                .append("\n");

        report.append("Processos em execução: ")
                .append(lblRunningJobsCount == null ? "—" : lblRunningJobsCount.getText());

        Label content = new Label(report.toString());
        content.setWrapText(true);
        content.setMaxWidth(520);
        content.getStyleClass().add("console-dashboard-diagnostic-text");

        modalManager.showModal(
                content,
                new ModalManager.ModalConfig()
                        .title("Diagnóstico rápido do Kubata")
                        .subtitle("Resumo do estado operacional actual")
                        .icon(Feather.ACTIVITY)
                        .tone(ModalManager.ModalTone.INFO)
                        .singleButton("Fechar")
                        .size(560, 380)
                        .minSize(500, 330)
        );
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

        Button btnTerminateAllSessions = new Button(
                "Terminar todas",
                IconUtils.icon(Feather.LOG_OUT, IconUtils.SIZE_SMALL)
        );
        btnTerminateAllSessions.getStyleClass().add("button-outlined");
        btnTerminateAllSessions.setTooltip(
                new Tooltip("Terminar todas as sessões de outros utilizadores")
        );
        btnTerminateAllSessions.setOnAction(e -> confirmTerminateAllSessions());
        btnTerminateAllSessions.setDisable(!isAdministrativeActor());

        box.getChildren().addAll(
                heading,
                maintenance,
                spacer,
                btnRefresh,
                btnBroadcast,
                btnTerminateAllSessions,
                btnMaintenance
        );
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
                    .filter(l -> l != null)
                    .filter(l -> "TODOS".equals(level)
                            || (l.getLogLevel() != null
                            && l.getLogLevel().name().equals(level)))
                    .filter(l -> "TODOS".equals(category)
                            || (l.getCategory() != null
                            && l.getCategory().equals(category)))
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
            // Obter dados principais da base de dados
            List<UserSession> sessions = userSessionRepository.findAll();
            List<User> users = userRepository.findAll();
            
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
                        s.getId(),
                        s.getUsername(),
                        s.getLoginTime() == null ? "—" : s.getLoginTime().format(timeFormatter),
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
                updateRecentActivity();

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

                long totalUsers = users.size();
                long activeUsers = users.stream()
                        .filter(java.util.Objects::nonNull)
                        .filter(u -> Boolean.TRUE.equals(u.getActive()))
                        .count();
                long onlineModules = moduleStatuses.stream()
                        .filter(m -> "ONLINE".equalsIgnoreCase(m.getStatus()))
                        .count();
                long runningJobs = backgroundProcesses.stream()
                        .filter(java.util.Objects::nonNull)
                        .filter(j -> j.getStatus() == AdminJob.Status.RUNNING)
                        .count();

                if (lblUsersCount != null) lblUsersCount.setText(String.valueOf(totalUsers));
                if (lblActiveUsersCount != null) lblActiveUsersCount.setText(String.valueOf(activeUsers));
                if (lblModulesOnlineCount != null) lblModulesOnlineCount.setText(String.valueOf(onlineModules));
                if (lblRunningJobsCount != null) lblRunningJobsCount.setText(String.valueOf(runningJobs));
                if (lblLastRefresh != null) {
                    lblLastRefresh.setText(LocalDateTime.now().format(
                            DateTimeFormatter.ofPattern("HH:mm:ss")
                    ));
                }
                
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
            });
        }, null);
    }

    private void kickUser(ActiveSession session) {
        modalManager.showConfirmModal(new Label("Forçar a saída do utilizador " + session.getUserName() + "?\nEsta ação encerrará a sessão imediatamente."), 
                "Kick Utilizador", () -> {
            persistenceService.executeAsync(() -> {
                if (session.getId() == null) {
                    throw new IllegalArgumentException("Sessão inválida: identificador não encontrado.");
                }
                userSessionRepository.findById(session.getId())
                    .ifPresent(userSessionRepository::delete);
            }, "USER_KICK", "CONSOLE",
                    "Sessão " + session.getId() + " do utilizador " + session.getUserName()
                            + " removida do sistema",
                    () -> {
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

    private boolean isAdministrativeActor() {
        User actor = sessionManager.getUser();
        return actor != null
                && (actor.isSuperadmin() || actor.getRole() == Role.ADMIN);
    }

    private void confirmTerminateAllSessions() {
        if (!isAdministrativeActor()) {
            modalManager.alert(
                    "Acesso negado",
                    "Apenas Administradores e Superadministradores podem terminar sessões em massa.",
                    "warning",
                    null
            );
            return;
        }

        User actor = sessionManager.getUser();
        Label warning = new Label(
                "Esta operação irá terminar todas as sessões dos outros utilizadores actualmente registadas no Kubata. "
                        + "A sessão do administrador que está a executar a operação será preservada para manter a consola operacional."
        );
        warning.setWrapText(true);

        VBox caution = new VBox(7);
        caution.setPadding(new Insets(12));
        caution.getStyleClass().add("console-danger-card");

        Label cautionTitle = new Label(
                "ATENÇÃO · operação global"
        );
        cautionTitle.getStyleClass().add("h4");

        Label cautionText = new Label(
                "Utilizadores que estejam a trabalhar noutros computadores poderão perder a sessão imediatamente. "
                        + "A operação será registada na auditoria administrativa."
        );
        cautionText.setWrapText(true);
        cautionText.getStyleClass().add("text-muted");
        caution.getChildren().addAll(cautionTitle, cautionText);

        VBox content = new VBox(12, warning, caution);
        content.setPadding(new Insets(6));

        modalManager.showConfirmModal(
                content,
                "Terminar todas as sessões",
                () -> terminateAllSessions(actor),
                null
        );
    }

    private void terminateAllSessions(User actor) {
        AtomicLong terminatedCount = new AtomicLong();
        AtomicReference<Exception> failure = new AtomicReference<>();

        persistenceService.executeSilent(
                () -> {
                    try {
                        terminatedCount.set(
                                userAdministrationService.terminarTodasSessoesDeUtilizadores(
                                        actor,
                                        "127.0.0.1"
                                )
                        );
                    } catch (Exception ex) {
                        failure.set(ex);
                    }
                },
                () -> {
                    Exception ex = failure.get();
                    if (ex != null) {
                        modalManager.alert(
                                "Falha ao terminar sessões",
                                ex.getMessage() == null || ex.getMessage().isBlank()
                                        ? "Não foi possível concluir a operação."
                                        : ex.getMessage(),
                                "error",
                                ex
                        );
                        return;
                    }

                    long count = terminatedCount.get();
                    notificationService.showSuccess(
                            "Sessões terminadas",
                            count == 0
                                    ? "Não foram encontradas sessões de outros utilizadores para terminar."
                                    : count + " sessão(ões) de outros utilizadores foram terminadas."
                    );
                    refreshAll();
                }
        );
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
        private final Long id;
        private final SimpleStringProperty userName, loginTime, workstation, ip, context, memory;

        public ActiveSession(Long id, String u, String l, String w, String ip, String c, String m) {
            this.id = id;
            this.userName = new SimpleStringProperty(u);
            this.loginTime = new SimpleStringProperty(l);
            this.workstation = new SimpleStringProperty(w);
            this.ip = new SimpleStringProperty(ip);
            this.context = new SimpleStringProperty(c);
            this.memory = new SimpleStringProperty(m);
        }

        public Long getId() { return id; }
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

package ao.allon.kubata.admin.view;

import ao.allon.kubata.admin.service.ModuleInstallationService;
import ao.allon.kubata.admin.service.NotificationService;
import ao.allon.kubata.admin.ui.util.IconUtils;
import ao.allon.kubata.core.domain.ModuloSistema;
import ao.allon.kubata.core.domain.ParametroSistema;
import ao.allon.kubata.core.module.KubataModule;
import ao.allon.kubata.core.module.ModuleRegistry;
import ao.allon.kubata.core.repository.ModuloSistemaRepository;
import ao.allon.kubata.core.repository.ParametroSistemaRepository;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import org.flywaydb.core.Flyway;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Centro avançado do Administrator.
 *
 * <p>Concentra capacidades administrativas que complementam Empresa,
 * Utilizadores, Perfis, Fiscal, Backup, Auditoria, API e Infraestrutura:
 * operações, scheduler, alertas, segurança, certificados, documentos,
 * comunicações, preferências e personalização.</p>
 */
@Component
public class PlataformaCentroView extends BorderPane {

    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final ParametroSistemaRepository parametroRepository;
    private final ModuloSistemaRepository moduloRepository;
    private final ModuleRegistry moduleRegistry;
    private final ModuleInstallationService moduleInstallationService;
    private final NotificationService notificationService;
    private final ObjectProvider<Flyway> flywayProvider;

    private final TabPane tabs = new TabPane();

    private final Label dashboardModules = new Label("0");
    private final Label dashboardMemory = new Label("0 MB");
    private final Label dashboardThreads = new Label("0");
    private final Label dashboardAlerts = new Label("0");

    private final Label alertMemory = new Label();
    private final Label alertDisk = new Label();
    private final Label alertModules = new Label();
    private final Label alertMigrations = new Label();

    private final TableView<JobRow> jobsTable = new TableView<>();
    private final ObservableList<JobRow> jobs = FXCollections.observableArrayList();
    private final Map<String, ScheduledFuture<?>> scheduledJobs = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(2, r -> {
                Thread t = new Thread(r, "kubata-platform-scheduler");
                t.setDaemon(true);
                return t;
            });

    private TextField smtpHost;
    private TextField smtpPort;
    private TextField smtpUser;
    private PasswordField smtpPassword;
    private TextField documentsDirectory;
    private ComboBox<String> globalLanguage;
    private ComboBox<String> globalTheme;
    private ComboBox<String> globalDensity;
    private Spinner<Integer> maxLoginAttempts;
    private Spinner<Integer> lockMinutes;
    private Spinner<Integer> passwordDays;
    private TextArea personalizationCatalog;

    public PlataformaCentroView(
            ParametroSistemaRepository parametroRepository,
            ModuloSistemaRepository moduloRepository,
            ModuleRegistry moduleRegistry,
            ModuleInstallationService moduleInstallationService,
            NotificationService notificationService,
            ObjectProvider<Flyway> flywayProvider) {

        this.parametroRepository = parametroRepository;
        this.moduloRepository = moduloRepository;
        this.moduleRegistry = moduleRegistry;
        this.moduleInstallationService = moduleInstallationService;
        this.notificationService = notificationService;
        this.flywayProvider = flywayProvider;

        setPadding(Insets.EMPTY);
        getStyleClass().add("application-view");

        buildUi();
        loadState();
    }

    private void buildUi() {
        VBox header = buildHeader();

        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getStyleClass().add("office365-tabs");

        tabs.getTabs().addAll(
                tab("Dashboard", Feather.HOME, buildDashboard()),
                tab("Operações & Scheduler", Feather.CLOCK, buildOperations()),
                tab("Alertas", Feather.ALERT_TRIANGLE, buildAlerts()),
                tab("Segurança", Feather.SHIELD, buildSecurity()),
                tab("Certificados", Feather.AWARD, buildCertificates()),
                tab("Gestão Documental", Feather.FOLDER, buildDocuments()),
                tab("Comunicações", Feather.MAIL, buildCommunications()),
                tab("Preferências", Feather.SLIDERS, buildPreferences()),
                tab("Personalização", Feather.CPU, buildPersonalization())
        );

        setTop(header);
        setCenter(tabs);
    }

    private VBox buildHeader() {
        VBox header = new VBox(10);
        header.setPadding(new Insets(16, 18, 14, 18));
        header.getStyleClass().add("header-box");

        HBox line = new HBox(12);
        line.setAlignment(Pos.CENTER_LEFT);

        HBox iconBox = new HBox();
        iconBox.setAlignment(Pos.CENTER);
        iconBox.setMinSize(42, 42);
        iconBox.setPrefSize(42, 42);
        iconBox.setMaxSize(42, 42);
        iconBox.setStyle(
                "-fx-background-color: rgba(33,115,70,0.10);" +
                "-fx-background-radius: 12px;"
        );
        iconBox.getChildren().add(IconUtils.icon(Feather.CPU, 21));

        VBox titleBox = new VBox(2);
        Label title = new Label("Centro da Plataforma");
        title.setStyle("-fx-font-size: 19px; -fx-font-weight: 800; -fx-text-fill: #24292f;");

        Label subtitle = new Label(
                "Administração avançada do Kubata: operações, automação, segurança, armazenamento e extensibilidade."
        );
        subtitle.setWrapText(true);
        subtitle.getStyleClass().add("text-muted");

        titleBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button refresh = new Button(
                "Actualizar estado",
                IconUtils.icon(Feather.REFRESH_CW, 13)
        );
        refresh.getStyleClass().add("button-primary");
        refresh.setOnAction(e -> refreshAll());

        line.getChildren().addAll(iconBox, titleBox, spacer, refresh);

        Label status = new Label(
                "CENTRAL ADMINISTRATIVA • operações acompanháveis e configurações persistentes"
        );
        status.setStyle(
                "-fx-background-color:#f6f8fa;" +
                "-fx-border-color:#eaeef2;" +
                "-fx-background-radius:8px;" +
                "-fx-border-radius:8px;" +
                "-fx-padding:8px 10px;" +
                "-fx-font-size:10px;" +
                "-fx-font-weight:800;" +
                "-fx-text-fill:#57606a;"
        );

        header.getChildren().addAll(line, status);
        return header;
    }

    private Tab tab(String title, Feather icon, Node content) {
        Tab tab = new Tab(title, content);
        tab.setGraphic(IconUtils.icon(icon, 13));
        return tab;
    }

    private Node buildDashboard() {
        VBox root = page();

        root.getChildren().addAll(
                sectionHeading(
                        Feather.LAYOUT,
                        "Visão geral da plataforma",
                        "Indicadores operacionais e acesso rápido às novas áreas administrativas."
                ),
                metricGrid(),
                dashboardActions(),
                infoCard(
                        Feather.INFO,
                        "Centro unificado",
                        "Use as abas desta área para controlar tarefas, políticas de segurança, certificados, armazenamento documental, comunicações e preferências."
                )
        );

        return scroll(root);
    }

    private GridPane metricGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);

        grid.add(metric("MÓDULOS REGISTADOS", dashboardModules, Feather.PACKAGE), 0, 0);
        grid.add(metric("MEMÓRIA HEAP", dashboardMemory, Feather.HARD_DRIVE), 1, 0);
        grid.add(metric("THREADS", dashboardThreads, Feather.ACTIVITY), 2, 0);
        grid.add(metric("ALERTAS", dashboardAlerts, Feather.ALERT_TRIANGLE), 3, 0);

        for (int i = 0; i < 4; i++) {
            ColumnConstraints c = new ColumnConstraints();
            c.setPercentWidth(25);
            c.setHgrow(Priority.ALWAYS);
            grid.getColumnConstraints().add(c);
        }

        return grid;
    }

    private VBox metric(String label, Label value, Feather icon) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(14));
        card.getStyleClass().add("card");

        HBox top = new HBox(8);
        top.setAlignment(Pos.CENTER_LEFT);
        top.getChildren().addAll(
                new Label("", IconUtils.icon(icon, 14)),
                new Label(label)
        );

        ((Label) top.getChildren().get(1)).setStyle(
                "-fx-font-size:9px;-fx-font-weight:800;-fx-text-fill:#6e7781;"
        );
        value.setStyle("-fx-font-size:22px;-fx-font-weight:800;-fx-text-fill:#24292f;");

        card.getChildren().addAll(top, value);
        return card;
    }

    private HBox dashboardActions() {
        HBox box = new HBox(8);
        box.setAlignment(Pos.CENTER_LEFT);

        Button apps = actionButton(
                "Aplicações Instaladas",
                Feather.PACKAGE,
                () -> notificationService.showInfo(
                        "Aplicações Instaladas",
                        "Abra «Aplicações Instaladas» no Ribbon da Infraestrutura para gerir módulos."
                )
        );

        Button currencies = actionButton(
                "Moedas",
                Feather.DOLLAR_SIGN,
                () -> notificationService.showInfo(
                        "Moedas e Câmbios",
                        "Use o botão «Moedas» no Ribbon para abrir a administração dedicada."
                )
        );

        Button alerts = actionButton(
                "Ver Alertas",
                Feather.ALERT_TRIANGLE,
                () -> tabs.getSelectionModel().select(findTab("Alertas"))
        );

        Button schedulerButton = actionButton(
                "Scheduler",
                Feather.CLOCK,
                () -> tabs.getSelectionModel().select(findTab("Operações & Scheduler"))
        );

        box.getChildren().addAll(apps, currencies, alerts, schedulerButton);
        return box;
    }

    private Tab findTab(String title) {
        return tabs.getTabs().stream()
                .filter(t -> Objects.equals(t.getText(), title))
                .findFirst()
                .orElse(tabs.getTabs().get(0));
    }

    private Button actionButton(String text, Feather icon, Runnable action) {
        Button button = new Button(text, IconUtils.icon(icon, 12));
        button.getStyleClass().add("button-outlined");
        button.setOnAction(e -> action.run());
        return button;
    }

    private Node buildOperations() {
        VBox root = page();

        Label hint = new Label(
                "O Scheduler executa tarefas administrativas seguras. As configurações são guardadas nos parâmetros do sistema."
        );
        hint.setWrapText(true);
        hint.getStyleClass().add("text-muted");

        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_LEFT);

        Button add = actionButton("Nova tarefa", Feather.PLUS, this::addJobDialog);
        Button runNow = actionButton("Executar agora", Feather.PLAY, this::runSelectedJob);
        Button toggle = actionButton("Activar / Pausar", Feather.POWER, this::toggleSelectedJob);
        Button refresh = actionButton("Actualizar", Feather.REFRESH_CW, this::loadJobs);

        actions.getChildren().addAll(add, runNow, toggle, refresh);

        jobsTable.setItems(jobs);
        jobsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        jobsTable.setPlaceholder(new Label("Nenhuma tarefa configurada."));

        TableColumn<JobRow, String> name = column("Tarefa", JobRow::getName);
        TableColumn<JobRow, String> interval = column("Intervalo", j -> j.getIntervalSeconds() + " s");
        TableColumn<JobRow, String> state = column("Estado", j -> j.isActive() ? "Activa" : "Pausada");
        TableColumn<JobRow, String> last = column("Última execução", JobRow::getLastRun);
        TableColumn<JobRow, String> result = column("Resultado", JobRow::getResult);

        jobsTable.getColumns().setAll(name, interval, state, last, result);

        root.getChildren().addAll(
                sectionHeading(
                        Feather.CLOCK,
                        "Operações assíncronas e Scheduler",
                        "Acompanhe tarefas administrativas e execute rotinas sem bloquear a interface."
                ),
                hint,
                actions,
                jobsTable,
                infoCard(
                        Feather.LIST,
                        "Tarefas nativas",
                        "Incluem verificação de alertas, diagnóstico da JVM, sincronização do catálogo de módulos e verificação de migrações Flyway."
                )
        );

        VBox.setVgrow(jobsTable, Priority.ALWAYS);
        return root;
    }

    private <T> TableColumn<JobRow, String> column(
            String title,
            java.util.function.Function<JobRow, String> mapper) {
        TableColumn<JobRow, String> column = new TableColumn<>(title);
        column.setCellValueFactory(data -> new SimpleStringProperty(
                Optional.ofNullable(mapper.apply(data.getValue())).orElse("—")
        ));
        return column;
    }

    private void addJobDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Nova tarefa agendada");
        dialog.setHeaderText("Criar uma operação administrativa");

        ComboBox<String> type = new ComboBox<>(FXCollections.observableArrayList(
                "VERIFICAR_ALERTAS",
                "DIAGNOSTICO_JVM",
                "SINCRONIZAR_MODULOS",
                "VERIFICAR_MIGRACOES"
        ));
        type.getSelectionModel().selectFirst();

        Spinner<Integer> seconds = new Spinner<>(10, 86400, 300, 10);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(9);
        grid.add(new Label("Rotina"), 0, 0);
        grid.add(type, 1, 0);
        grid.add(new Label("Intervalo (seg.)"), 0, 1);
        grid.add(seconds, 1, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

        dialog.setResultConverter(button -> {
            if (button != ButtonType.OK) {
                return null;
            }

            String id = type.getValue();
            JobRow row = jobs.stream()
                    .filter(j -> j.getId().equals(id))
                    .findFirst()
                    .orElse(null);

            if (row == null) {
                row = new JobRow(id, id.replace('_', ' '), seconds.getValue(), true);
                jobs.add(row);
            } else {
                row.setIntervalSeconds(seconds.getValue());
                row.setActive(true);
            }

            persistJobs();
            scheduleJob(row);
            jobsTable.refresh();
            return null;
        });

        dialog.showAndWait();
    }

    private void runSelectedJob() {
        JobRow selected = jobsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            notificationService.showWarning("Scheduler", "Seleccione uma tarefa.");
            return;
        }
        executeJob(selected);
    }

    private void toggleSelectedJob() {
        JobRow selected = jobsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            notificationService.showWarning("Scheduler", "Seleccione uma tarefa.");
            return;
        }

        selected.setActive(!selected.isActive());

        ScheduledFuture<?> future = scheduledJobs.remove(selected.getId());
        if (future != null) {
            future.cancel(false);
        }
        if (selected.isActive()) {
            scheduleJob(selected);
        }

        persistJobs();
        jobsTable.refresh();
    }

    private void scheduleJob(JobRow row) {
        ScheduledFuture<?> previous = scheduledJobs.remove(row.getId());
        if (previous != null) {
            previous.cancel(false);
        }

        if (!row.isActive()) {
            return;
        }

        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(
                () -> executeJob(row),
                row.getIntervalSeconds(),
                row.getIntervalSeconds(),
                TimeUnit.SECONDS
        );
        scheduledJobs.put(row.getId(), future);
    }

    private void executeJob(JobRow row) {
        try {
            String result;

            switch (row.getId()) {
                case "VERIFICAR_ALERTAS" -> {
                    Platform.runLater(this::refreshAlerts);
                    result = "Pedido de actualização enviado para a UI";
                }
                case "DIAGNOSTICO_JVM" -> {
                    MemoryMXBean bean = ManagementFactory.getMemoryMXBean();
                    long used = bean.getHeapMemoryUsage().getUsed() / (1024 * 1024);
                    result = "Heap usada: " + used + " MB";
                }
                case "SINCRONIZAR_MODULOS" -> {
                    moduleInstallationService.synchronizeCatalog();
                    result = "Catálogo sincronizado";
                }
                case "VERIFICAR_MIGRACOES" -> {
                    Flyway flyway = flywayProvider.getIfAvailable();
                    result = flyway == null
                            ? "Flyway indisponível"
                            : flyway.info().pending().length + " migração(ões) pendente(s)";
                }
                default -> result = "Rotina não reconhecida";
            }

            String finalResult = result;
            row.setLastRun(LocalDateTime.now().format(DATE_TIME));
            row.setResult(finalResult);
            Platform.runLater(jobsTable::refresh);
        } catch (Exception ex) {
            row.setLastRun(LocalDateTime.now().format(DATE_TIME));
            row.setResult("Erro: " + safe(ex.getMessage()));
            Platform.runLater(jobsTable::refresh);
        }
    }

    private Node buildAlerts() {
        VBox root = page();

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);

        grid.add(alertCard("Memória JVM", alertMemory, Feather.HARD_DRIVE), 0, 0);
        grid.add(alertCard("Armazenamento", alertDisk, Feather.DATABASE), 1, 0);
        grid.add(alertCard("Módulos", alertModules, Feather.PACKAGE), 0, 1);
        grid.add(alertCard("Migrações", alertMigrations, Feather.REFRESH_CW), 1, 1);

        ColumnConstraints left = new ColumnConstraints();
        left.setPercentWidth(50);
        left.setHgrow(Priority.ALWAYS);
        ColumnConstraints right = new ColumnConstraints();
        right.setPercentWidth(50);
        right.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(left, right);

        Button refresh = actionButton("Verificar agora", Feather.REFRESH_CW, this::refreshAlerts);

        root.getChildren().addAll(
                sectionHeading(
                        Feather.ALERT_TRIANGLE,
                        "Central de Alertas",
                        "Indicadores derivados do estado actual da JVM, disco, módulos e Flyway."
                ),
                grid,
                refresh,
                infoCard(
                        Feather.BELL,
                        "Expansível",
                        "A central pode receber posteriormente regras adicionais de séries, licenças, webhooks, backups e conectividade sem alterar o modelo do Administrator."
                )
        );

        return root;
    }

    private VBox alertCard(String title, Label status, Feather icon) {
        VBox card = new VBox(7);
        card.setPadding(new Insets(14));
        card.getStyleClass().add("card");

        HBox titleLine = new HBox(8);
        titleLine.setAlignment(Pos.CENTER_LEFT);
        titleLine.getChildren().addAll(
                new Label("", IconUtils.icon(icon, 14)),
                new Label(title)
        );

        ((Label) titleLine.getChildren().get(1))
                .setStyle("-fx-font-weight:800;-fx-font-size:13px;");

        status.setWrapText(true);
        status.setStyle("-fx-font-size:12px;-fx-font-weight:700;");

        card.getChildren().addAll(titleLine, status);
        return card;
    }

    private void refreshAlerts() {
        try {
            MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
            long used = memory.getHeapMemoryUsage().getUsed();
            long max = memory.getHeapMemoryUsage().getMax();
            double ratio = max > 0 ? (double) used / max : 0;

            alertMemory.setText(
                    String.format(Locale.ROOT, "%.0f%% da heap em utilização", ratio * 100)
            );
            alertMemory.setStyle(ratio >= 0.85
                    ? "-fx-text-fill:#b42318;-fx-font-weight:800;"
                    : ratio >= 0.70
                        ? "-fx-text-fill:#9a6700;-fx-font-weight:800;"
                        : "-fx-text-fill:#17663f;-fx-font-weight:800;");

            Path rootPath = Paths.get(System.getProperty("user.dir", ".")).toAbsolutePath();
            File file = rootPath.toFile();
            long free = file.getFreeSpace();
            long total = file.getTotalSpace();
            double diskRatio = total > 0 ? (double) free / total : 1;
            alertDisk.setText(
                    String.format(Locale.ROOT, "%.1f GB livres", free / (1024d * 1024d * 1024d))
            );
            alertDisk.setStyle(diskRatio <= 0.10
                    ? "-fx-text-fill:#b42318;-fx-font-weight:800;"
                    : diskRatio <= 0.20
                        ? "-fx-text-fill:#9a6700;-fx-font-weight:800;"
                        : "-fx-text-fill:#17663f;-fx-font-weight:800;");

            long unavailable = moduleRegistry.getAllModules().stream()
                    .filter(m -> !m.isActive())
                    .count();
            alertModules.setText(unavailable == 0
                    ? "Todos os módulos registados estão activos."
                    : unavailable + " módulo(s) não activo(s).");
            alertModules.setStyle(unavailable == 0
                    ? "-fx-text-fill:#17663f;-fx-font-weight:800;"
                    : "-fx-text-fill:#9a6700;-fx-font-weight:800;");

            Flyway flyway = flywayProvider.getIfAvailable();
            int pending = flyway == null ? -1 : flyway.info().pending().length;
            alertMigrations.setText(
                    pending < 0
                            ? "Flyway indisponível"
                            : pending == 0
                                ? "Base de dados sem migrações pendentes."
                                : pending + " migração(ões) pendente(s)."
            );
            alertMigrations.setStyle(
                    pending <= 0
                            ? "-fx-text-fill:#17663f;-fx-font-weight:800;"
                            : "-fx-text-fill:#9a6700;-fx-font-weight:800;"
            );

            updateDashboardAlertCount(ratio, diskRatio, unavailable, pending);
        } catch (Exception ex) {
            alertMemory.setText("Não foi possível calcular o estado.");
            alertDisk.setText("Não foi possível calcular o estado.");
            alertModules.setText("Não foi possível calcular o estado.");
            alertMigrations.setText("Não foi possível calcular o estado.");
        }
    }

    private void updateDashboardAlertCount(
            double memoryRatio,
            double diskFreeRatio,
            long unavailableModules,
            int pendingMigrations) {

        int count = 0;
        if (memoryRatio >= 0.85) count++;
        if (diskFreeRatio <= 0.10) count++;
        if (unavailableModules > 0) count++;
        if (pendingMigrations > 0) count++;

        dashboardAlerts.setText(Integer.toString(count));
    }

    private Node buildSecurity() {
        VBox root = page();

        maxLoginAttempts = new Spinner<>(1, 20, integer("SEGURANCA.MAX_TENTATIVAS", 5));
        lockMinutes = new Spinner<>(1, 1440, integer("SEGURANCA.MINUTOS_BLOQUEIO", 30));
        passwordDays = new Spinner<>(1, 3650, integer("SEGURANCA.DIAS_VALIDADE_PW", 90));

        CheckBox requireMfa = new CheckBox("Exigir MFA para administradores");
        requireMfa.setSelected(booleanValue("SEGURANCA.MFA_ADMIN", false));

        CheckBox singleSession = new CheckBox("Limitar a uma sessão por utilizador");
        singleSession.setSelected(booleanValue("SEGURANCA.SESSAO_UNICA", false));

        Button save = actionButton("Guardar política", Feather.SAVE, () -> {
            saveGlobal("SEGURANCA.MAX_TENTATIVAS", Integer.toString(maxLoginAttempts.getValue()), "INTEGER", "Tentativas de login antes do bloqueio", "SEGURANCA");
            saveGlobal("SEGURANCA.MINUTOS_BLOQUEIO", Integer.toString(lockMinutes.getValue()), "INTEGER", "Minutos de bloqueio", "SEGURANCA");
            saveGlobal("SEGURANCA.DIAS_VALIDADE_PW", Integer.toString(passwordDays.getValue()), "INTEGER", "Dias de validade da password", "SEGURANCA");
            saveGlobal("SEGURANCA.MFA_ADMIN", Boolean.toString(requireMfa.isSelected()), "BOOLEAN", "Exigir MFA a administradores", "SEGURANCA");
            saveGlobal("SEGURANCA.SESSAO_UNICA", Boolean.toString(singleSession.isSelected()), "BOOLEAN", "Uma sessão por utilizador", "SEGURANCA");
            notificationService.showSuccess("Segurança", "Política de segurança guardada.");
        });

        VBox policy = formCard(
                "Política de Segurança",
                "Parâmetros centralizados para autenticação e controlo de sessões.",
                field("Máximo de tentativas", maxLoginAttempts),
                field("Minutos de bloqueio", lockMinutes),
                field("Dias de validade da password", passwordDays),
                requireMfa,
                singleSession,
                save
        );

        root.getChildren().addAll(
                sectionHeading(
                        Feather.SHIELD,
                        "Segurança Avançada",
                        "Complementa Utilizadores e Perfis com políticas administrativas globais."
                ),
                policy,
                infoCard(
                        Feather.KEY,
                        "Segurança por empresa",
                        "A autorização granular continua a ser definida pelos perfis existentes; estes parâmetros acrescentam políticas de autenticação global."
                )
        );

        return scroll(root);
    }

    private Node buildCertificates() {
        VBox root = page();

        TextField path = new TextField(defaultKeystorePath());
        path.setEditable(false);

        TextArea certificateList = new TextArea();
        certificateList.setEditable(false);
        certificateList.setWrapText(false);
        certificateList.setPrefRowCount(12);
        certificateList.setStyle("-fx-font-family:'Consolas';");

        Button inspect = actionButton(
                "Ler certificados",
                Feather.SEARCH,
                () -> certificateList.setText(readKeystore(path.getText()))
        );

        Button choose = actionButton(
                "Escolher keystore",
                Feather.FOLDER,
                () -> {
                    FileChooser chooser = new FileChooser();
                    chooser.setTitle("Seleccionar keystore/certificado");
                    chooser.getExtensionFilters().addAll(
                            new FileChooser.ExtensionFilter("Keystores", "*.jks", "*.p12", "*.pfx"),
                            new FileChooser.ExtensionFilter("Todos os ficheiros", "*.*")
                    );
                    File file = chooser.showOpenDialog(getScene() == null ? null : getScene().getWindow());
                    if (file != null) {
                        path.setText(file.getAbsolutePath());
                        certificateList.setText(readKeystore(file.getAbsolutePath()));
                    }
                }
        );

        HBox actions = new HBox(8, inspect, choose);

        root.getChildren().addAll(
                sectionHeading(
                        Feather.AWARD,
                        "Gestão de Certificados",
                        "Consulta dos certificados disponíveis no keystore do ambiente."
                ),
                formCard(
                        "Keystore activo",
                        "Use o keystore da JVM ou escolha um JKS/PKCS12 para inspecção.",
                        new Label("Ficheiro"),
                        path,
                        actions
                ),
                certificateList,
                infoCard(
                        Feather.INFO,
                        "Boas práticas",
                        "A visualização não expõe chaves privadas. A instalação definitiva de certificados deve ser feita no keystore/serviço de destino."
                )
        );

        VBox.setVgrow(certificateList, Priority.ALWAYS);
        return scroll(root);
    }

    private String defaultKeystorePath() {
        String javaHome = System.getProperty("java.home");
        Path candidate = Paths.get(javaHome, "lib", "security", "cacerts");
        if (Files.exists(candidate)) {
            return candidate.toString();
        }
        return candidate.toString();
    }

    private String readKeystore(String path) {
        if (path == null || path.isBlank()) {
            return "Nenhum keystore seleccionado.";
        }

        try {
            KeyStore store = KeyStore.getInstance(
                    path.toLowerCase(Locale.ROOT).endsWith(".p12")
                            || path.toLowerCase(Locale.ROOT).endsWith(".pfx")
                            ? "PKCS12"
                            : "JKS"
            );

            IOException last = null;
            for (char[] password : new char[][]{
                    new char[0],
                    "changeit".toCharArray()
            }) {
                try {
                    try (var in = Files.newInputStream(Paths.get(path))) {
                        store.load(in, password);
                    }
                    last = null;
                    break;
                } catch (IOException ex) {
                    last = ex;
                }
            }

            if (last != null) {
                return "Não foi possível abrir o keystore: " + safe(last.getMessage());
            }

            StringBuilder sb = new StringBuilder();
            Enumeration<String> aliases = store.aliases();

            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                Certificate certificate = store.getCertificate(alias);

                sb.append("Alias: ").append(alias).append('\n');
                sb.append("Tipo: ").append(certificate == null ? "—" : certificate.getType()).append('\n');

                if (certificate instanceof java.security.cert.X509Certificate x509) {
                    sb.append("Subject: ").append(x509.getSubjectX500Principal()).append('\n');
                    sb.append("Emissor: ").append(x509.getIssuerX500Principal()).append('\n');
                    sb.append("Válido desde: ").append(x509.getNotBefore()).append('\n');
                    sb.append("Válido até: ").append(x509.getNotAfter()).append('\n');
                }

                sb.append("────────────────────────────────────\n");
            }

            return sb.isEmpty()
                    ? "Nenhum certificado encontrado."
                    : sb.toString();
        } catch (Exception ex) {
            return "Erro ao ler o keystore: " + safe(ex.getMessage());
        }
    }

    private Node buildDocuments() {
        VBox root = page();

        documentsDirectory = new TextField(stringValue(
                "DOCUMENTOS.DIRETORIO",
                Paths.get(System.getProperty("user.home", "."), "Kubata", "Documentos").toString()
        ));

        Button choose = actionButton(
                "Escolher pasta",
                Feather.FOLDER,
                this::chooseDocumentsDirectory
        );

        Button create = actionButton(
                "Criar pasta",
                Feather.PLUS,
                () -> {
                    try {
                        Files.createDirectories(Paths.get(documentsDirectory.getText().trim()));
                        notificationService.showSuccess("Documentos", "Pasta criada/verificada.");
                    } catch (Exception ex) {
                        notificationService.showWarning("Documentos", safe(ex.getMessage()));
                    }
                }
        );

        Button save = actionButton(
                "Guardar",
                Feather.SAVE,
                () -> {
                    saveGlobal(
                            "DOCUMENTOS.DIRETORIO",
                            documentsDirectory.getText().trim(),
                            "STRING",
                            "Diretório central de armazenamento documental",
                            "DOCUMENTOS"
                    );
                    notificationService.showSuccess("Documentos", "Diretório documental guardado.");
                }
        );

        root.getChildren().addAll(
                sectionHeading(
                        Feather.FOLDER,
                        "Gestão Documental",
                        "Configuração central do armazenamento de anexos e documentos."
                ),
                formCard(
                        "Armazenamento",
                        "Defina a pasta onde o Administrator pode centralizar documentos."
                        ,
                        new Label("Directório"),
                        documentsDirectory,
                        new HBox(8, choose, create, save)
                ),
                infoCard(
                        Feather.SHIELD,
                        "Controlo de acesso",
                        "O armazenamento deve permanecer protegido pelo sistema operativo e pelas permissões dos utilizadores. O caminho não concede acesso por si só."
                )
        );

        return scroll(root);
    }

    private void chooseDocumentsDirectory() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Escolher directório documental");
        File dir = chooser.showDialog(getScene() == null ? null : getScene().getWindow());
        if (dir != null) {
            documentsDirectory.setText(dir.getAbsolutePath());
        }
    }

    private Node buildCommunications() {
        VBox root = page();

        smtpHost = new TextField(stringValue("COMUNICACAO.SMTP_HOST", ""));
        smtpPort = new TextField(stringValue("COMUNICACAO.SMTP_PORT", "587"));
        smtpUser = new TextField(stringValue("COMUNICACAO.SMTP_USER", ""));
        smtpPassword = new PasswordField();
        smtpPassword.setPromptText("Password SMTP");
        smtpPassword.setText(stringValue("COMUNICACAO.SMTP_PASSWORD", ""));

        CheckBox tls = new CheckBox("STARTTLS");
        tls.setSelected(booleanValue("COMUNICACAO.SMTP_TLS", true));

        Button save = actionButton("Guardar SMTP", Feather.SAVE, () -> {
            saveGlobal("COMUNICACAO.SMTP_HOST", smtpHost.getText().trim(), "STRING", "Host SMTP", "COMUNICACAO");
            saveGlobal("COMUNICACAO.SMTP_PORT", smtpPort.getText().trim(), "STRING", "Porta SMTP", "COMUNICACAO");
            saveGlobal("COMUNICACAO.SMTP_USER", smtpUser.getText().trim(), "STRING", "Utilizador SMTP", "COMUNICACAO");
            saveGlobal("COMUNICACAO.SMTP_PASSWORD", smtpPassword.getText(), "STRING", "Password SMTP", "COMUNICACAO");
            saveGlobal("COMUNICACAO.SMTP_TLS", Boolean.toString(tls.isSelected()), "BOOLEAN", "STARTTLS SMTP", "COMUNICACAO");
            notificationService.showSuccess("Comunicações", "Configuração SMTP guardada.");
        });

        Button test = actionButton(
                "Validar configuração",
                Feather.SEND,
                () -> notificationService.showInfo(
                        "Comunicações",
                        smtpHost.getText().isBlank()
                                ? "Informe primeiro o servidor SMTP."
                                : "Parâmetros SMTP preenchidos. O envio real pode ser ligado ao serviço de correio do módulo."
                )
        );

        root.getChildren().addAll(
                sectionHeading(
                        Feather.MAIL,
                        "Comunicações",
                        "Centralize SMTP e parâmetros de envio usados por notificações e relatórios."
                ),
                formCard(
                        "Servidor SMTP",
                        "Configuração base para correio do sistema.",
                        field("Servidor", smtpHost),
                        field("Porta", smtpPort),
                        field("Utilizador", smtpUser),
                        field("Password", smtpPassword),
                        tls,
                        new HBox(8, save, test)
                ),
                infoCard(
                        Feather.INFO,
                        "Extensão futura",
                        "A mesma área pode acolher SMS, templates de email, filas e histórico de entregas sem alterar a configuração actual."
                )
        );

        return scroll(root);
    }

    private Node buildPreferences() {
        VBox root = page();

        globalLanguage = new ComboBox<>(FXCollections.observableArrayList("pt-AO", "pt-PT", "en"));
        globalLanguage.setValue(stringValue("PREFERENCIAS.IDIOMA", "pt-AO"));

        globalTheme = new ComboBox<>(FXCollections.observableArrayList(
                "VERDE_ADMIN", "CLARO", "ESCURO", "ALTO_CONTRASTE"
        ));
        globalTheme.setValue(stringValue("PREFERENCIAS.TEMA", "VERDE_ADMIN"));

        globalDensity = new ComboBox<>(FXCollections.observableArrayList(
                "COMPACTA", "NORMAL", "CONFORTO"
        ));
        globalDensity.setValue(stringValue("PREFERENCIAS.DENSIDADE", "NORMAL"));

        Button save = actionButton("Guardar preferências", Feather.SAVE, () -> {
            saveGlobal("PREFERENCIAS.IDIOMA", globalLanguage.getValue(), "STRING", "Idioma global", "PREFERENCIAS");
            saveGlobal("PREFERENCIAS.TEMA", globalTheme.getValue(), "STRING", "Tema global", "PREFERENCIAS");
            saveGlobal("PREFERENCIAS.DENSIDADE", globalDensity.getValue(), "STRING", "Densidade de informação", "PREFERENCIAS");
            notificationService.showSuccess("Preferências", "Preferências globais guardadas.");
        });

        root.getChildren().addAll(
                sectionHeading(
                        Feather.SLIDERS,
                        "Preferências da Plataforma",
                        "Valores globais que podem servir de base para os módulos do Kubata."
                ),
                formCard(
                        "Interface",
                        "Os utilizadores podem continuar a manter preferências pessoais; estes valores servem como padrão.",
                        field("Idioma", globalLanguage),
                        field("Tema", globalTheme),
                        field("Densidade", globalDensity),
                        save
                ),
                infoCard(
                        Feather.USER,
                        "Preferências do utilizador",
                        "Idioma, tema e linhas por página já existem na conta de utilizador e podem sobrepor os valores globais."
                )
        );

        return scroll(root);
    }

    private Node buildPersonalization() {
        VBox root = page();

        personalizationCatalog = new TextArea();
        personalizationCatalog.setWrapText(false);
        personalizationCatalog.setPromptText(
                "TIPO|NOME|DESCRIÇÃO\n" +
                "CAMPO|N.º Cliente|Campo personalizado para clientes\n" +
                "FORMULARIO|Ficha Cliente|Secção adicional"
        );
        personalizationCatalog.setText(
                stringValue("PERSONALIZACAO.CATALOGO", "")
        );
        personalizationCatalog.setStyle("-fx-font-family:'Consolas';");

        Button save = actionButton(
                "Guardar catálogo",
                Feather.SAVE,
                () -> {
                    saveGlobal(
                            "PERSONALIZACAO.CATALOGO",
                            personalizationCatalog.getText(),
                            "STRING",
                            "Catálogo de personalizações administrativas",
                            "PERSONALIZACAO"
                    );
                    notificationService.showSuccess(
                            "Personalização",
                            "Catálogo guardado nos parâmetros do sistema."
                    );
                }
        );

        Label explanation = new Label(
                "Registe metadados de campos, tabelas, formulários, separadores, menus, mapas, funções e processos personalizados. " +
                "A execução dinâmica destas extensões deve ser ligada aos módulos que as consumirem."
        );
        explanation.setWrapText(true);
        explanation.getStyleClass().add("text-muted");

        root.getChildren().addAll(
                sectionHeading(
                        Feather.CPU,
                        "Studio de Personalização",
                        "Base administrativa para CDU/XDU/PDU/RDU/FDU/SDU/MDU."
                ),
                explanation,
                personalizationCatalog,
                save,
                infoCard(
                        Feather.CODE,
                        "Catálogo extensível",
                        "Esta camada deixa a informação configurada e versionável; cada módulo pode interpretar os tipos que suporta."
                )
        );

        VBox.setVgrow(personalizationCatalog, Priority.ALWAYS);
        return root;
    }

    private VBox page() {
        VBox box = new VBox(14);
        box.setPadding(new Insets(18, 20, 22, 20));
        box.setFillWidth(true);
        return box;
    }

    private ScrollPane scroll(Node node) {
        ScrollPane scroll = new ScrollPane(node);
        scroll.setFitToWidth(true);
        scroll.setPannable(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.getStyleClass().add("application-scroll");
        return scroll;
    }

    private VBox sectionHeading(Feather icon, String title, String description) {
        VBox box = new VBox(3);

        HBox line = new HBox(8);
        line.setAlignment(Pos.CENTER_LEFT);

        Label iconLabel = new Label("", IconUtils.icon(icon, 16));
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size:15px;-fx-font-weight:800;");

        line.getChildren().addAll(iconLabel, titleLabel);

        Label desc = new Label(description);
        desc.setWrapText(true);
        desc.getStyleClass().add("text-muted");

        box.getChildren().addAll(line, desc);
        return box;
    }

    private VBox formCard(String title, String description, Node... nodes) {
        VBox card = new VBox(11);
        card.setPadding(new Insets(16));
        card.getStyleClass().add("card");

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size:13px;-fx-font-weight:800;");

        Label desc = new Label(description);
        desc.setWrapText(true);
        desc.getStyleClass().add("text-muted");

        card.getChildren().addAll(titleLabel, desc);
        Collections.addAll(card.getChildren(), nodes);
        return card;
    }

    private HBox field(String labelText, Node control) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);

        Label label = new Label(labelText);
        label.setMinWidth(170);
        label.setStyle("-fx-font-size:11px;-fx-font-weight:700;");

        HBox.setHgrow(control, Priority.ALWAYS);
        row.getChildren().addAll(label, control);
        return row;
    }

    private HBox infoCard(Feather icon, String title, String description) {
        HBox card = new HBox(10);
        card.setPadding(new Insets(12, 14, 12, 14));
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().add("card");

        VBox text = new VBox(2);
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size:12px;-fx-font-weight:800;");
        Label desc = new Label(description);
        desc.setWrapText(true);
        desc.getStyleClass().add("text-muted");

        text.getChildren().addAll(titleLabel, desc);
        HBox.setHgrow(text, Priority.ALWAYS);

        card.getChildren().addAll(
                new Label("", IconUtils.icon(icon, 14)),
                text
        );
        return card;
    }

    private void loadState() {
        loadJobs();
        refreshAll();
    }

    private void refreshAll() {
        int moduleCount = moduleRegistry.getAllModules().size();
        dashboardModules.setText(Integer.toString(moduleCount));

        MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
        long heap = memory.getHeapMemoryUsage().getUsed() / (1024 * 1024);
        dashboardMemory.setText(heap + " MB");
        dashboardThreads.setText(Integer.toString(
                ManagementFactory.getThreadMXBean().getThreadCount()
        ));

        refreshAlerts();
    }

    private void loadJobs() {
        jobs.clear();

        String stored = stringValue("PLATAFORMA.SCHEDULER", "");
        if (!stored.isBlank()) {
            for (String line : stored.split("\\R")) {
                String[] parts = line.split("\\|", -1);
                if (parts.length >= 3) {
                    try {
                        JobRow row = new JobRow(
                                parts[0],
                                parts.length > 1 ? parts[1] : parts[0],
                                Integer.parseInt(parts[2]),
                                parts.length < 4 || Boolean.parseBoolean(parts[3])
                        );
                        jobs.add(row);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }

        if (jobs.isEmpty()) {
            jobs.add(new JobRow("VERIFICAR_ALERTAS", "Verificar alertas", 300, true));
            jobs.add(new JobRow("DIAGNOSTICO_JVM", "Diagnóstico JVM", 600, false));
        }

        scheduledJobs.values().forEach(f -> f.cancel(false));
        scheduledJobs.clear();

        jobs.forEach(this::scheduleJob);
        jobsTable.refresh();
    }

    private void persistJobs() {
        String value = jobs.stream()
                .map(j -> String.join("|",
                        j.getId(),
                        j.getName(),
                        Integer.toString(j.getIntervalSeconds()),
                        Boolean.toString(j.isActive())
                ))
                .collect(Collectors.joining(System.lineSeparator()));

        saveGlobal(
                "PLATAFORMA.SCHEDULER",
                value,
                "STRING",
                "Configuração do Scheduler do Administrator",
                "PLATAFORMA"
        );
    }

    private int integer(String key, int fallback) {
        try {
            return Integer.parseInt(stringValue(key, Integer.toString(fallback)));
        } catch (Exception ex) {
            return fallback;
        }
    }

    private boolean booleanValue(String key, boolean fallback) {
        return Boolean.parseBoolean(
                stringValue(key, Boolean.toString(fallback))
        );
    }

    private String stringValue(String key, String fallback) {
        try {
            return parametroRepository.findByChaveAndEmpresaIdIsNull(key)
                    .map(ParametroSistema::getValor)
                    .filter(v -> v != null && !v.isBlank())
                    .orElse(fallback);
        } catch (Exception ex) {
            return fallback;
        }
    }

    private void saveGlobal(
            String key,
            String value,
            String type,
            String description,
            String group) {

        ParametroSistema p = parametroRepository
                .findByChaveAndEmpresaIdIsNull(key)
                .orElseGet(ParametroSistema::new);

        p.setEmpresa(null);
        p.setChave(key);
        p.setValor(value == null ? "" : value);
        p.setTipoValor(type);
        p.setDescricao(description);
        p.setGrupo(group);
        p.setEditavel(true);
        p.setAtualizadoEm(LocalDateTime.now());
        p.setAtualizadoPor("Kubata Administrator");
        parametroRepository.save(p);
    }

    private String safe(String message) {
        return message == null || message.isBlank()
                ? "erro desconhecido"
                : message;
    }

    @Override
    protected void finalize() throws Throwable {
        scheduledJobs.values().forEach(f -> f.cancel(true));
        scheduler.shutdownNow();
        super.finalize();
    }

    public static class JobRow {
        private final String id;
        private final String name;
        private int intervalSeconds;
        private boolean active;
        private String lastRun = "—";
        private String result = "—";

        public JobRow(String id, String name, int intervalSeconds, boolean active) {
            this.id = id;
            this.name = name;
            this.intervalSeconds = intervalSeconds;
            this.active = active;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public int getIntervalSeconds() { return intervalSeconds; }
        public void setIntervalSeconds(int intervalSeconds) { this.intervalSeconds = intervalSeconds; }
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
        public String getLastRun() { return lastRun; }
        public void setLastRun(String lastRun) { this.lastRun = lastRun; }
        public String getResult() { return result; }
        public void setResult(String result) { this.result = result; }
    }
}

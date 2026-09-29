package ao.allon.kubata.admin.view;

import ao.allon.kubata.admin.service.PersistenceService;
import ao.allon.kubata.admin.service.SessionManager;
import ao.allon.kubata.admin.ui.modal.ModalManager;
import ao.allon.kubata.admin.ui.util.IconUtils;
import ao.allon.kubata.admin.ui.util.ThemeManager;
import ao.allon.kubata.core.domain.AuditLog;
import ao.allon.kubata.core.domain.BackupRecord;
import ao.allon.kubata.core.domain.Empresa;
import ao.allon.kubata.admin.ui.reports.JasperViewerPane;
import ao.allon.kubata.core.domain.User;
import ao.allon.kubata.core.repository.AuditLogRepository;
import ao.allon.kubata.core.repository.BackupRecordRepository;
import ao.allon.kubata.core.repository.EmpresaRepository;
import ao.allon.kubata.core.repository.UserRepository;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class RelatoriosView extends VBox {

    private final UserRepository userRepository;
    private final EmpresaRepository empresaRepository;
    private final AuditLogRepository auditLogRepository;
    private final BackupRecordRepository backupRecordRepository;
    private final SessionManager sessionManager;
    private final ModalManager modalManager;
    private final PersistenceService persistenceService;
    private final ResourceLoader resourceLoader;

    // Filtros
    private DatePicker dpInicio;
    private DatePicker dpFim;
    private ComboBox<String> cbCategoria;

    // Gráficos
    private PieChart userStatusChart;
    private BarChart<String, Number> auditActivityChart;

    public RelatoriosView(UserRepository userRepository, EmpresaRepository empresaRepository,
                          AuditLogRepository auditLogRepository, BackupRecordRepository backupRecordRepository,
                          SessionManager sessionManager, ModalManager modalManager,
                          PersistenceService persistenceService, ResourceLoader resourceLoader) {
        this.userRepository = userRepository;
        this.empresaRepository = empresaRepository;
        this.auditLogRepository = auditLogRepository;
        this.backupRecordRepository = backupRecordRepository;
        this.sessionManager = sessionManager;
        this.modalManager = modalManager;
        this.persistenceService = persistenceService;
        this.resourceLoader = resourceLoader;

        // Carregamento assíncrono para evitar erros de banco de dados no construtor
        Platform.runLater(this::buildUI);
    }

    private void buildUI() {
        getChildren().clear();
        setSpacing(0);
        setPadding(Insets.EMPTY);
        getStyleClass().add("relatorios-view");

        HBox toolbar = buildToolbar();
        
        // Área de Filtros
        HBox filtersBox = buildFiltersBox();

        // ScrollPane para o conteúdo principal
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("transparent-scroll");
        
        VBox mainContent = new VBox(25);
        mainContent.setPadding(new Insets(20));
        
        // Seção de KPIs
        VBox kpiSection = buildKPISection();
        
        // Seção de Gráficos
        HBox chartSection = buildChartSection();
        
        // Seção de Listagem de Relatórios
        VBox reportsSection = buildReportsSection();

        mainContent.getChildren().addAll(kpiSection, new Separator(), chartSection, new Separator(), reportsSection);
        scrollPane.setContent(mainContent);

        getChildren().addAll(toolbar, filtersBox, scrollPane);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        
        // Carregar dados iniciais
        refreshData();
    }

    private HBox buildToolbar() {
        HBox box = new HBox(12);
        box.getStyleClass().add("kubata-reports-toolbar");
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(14, 20, 12, 20));

        StackPane icon = new StackPane();
        icon.getStyleClass().add("kubata-reports-toolbar-icon");
        icon.getChildren().add(new Label("", IconUtils.icon(Feather.BAR_CHART_2, 20)));

        VBox titleBox = new VBox(2);
        Label title = new Label("Central de Relatórios");
        title.getStyleClass().add("kubata-reports-title");

        Label subtitle = new Label(
                "Indicadores administrativos, auditoria, utilizadores, empresas, acessos e continuidade."
        );
        subtitle.setWrapText(true);
        subtitle.getStyleClass().add("kubata-reports-subtitle");

        titleBox.getChildren().addAll(title, subtitle);

        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnExportar = new Button(
                "Exportar auditoria",
                IconUtils.icon(Feather.DOWNLOAD, IconUtils.SIZE_SMALL)
        );
        btnExportar.getStyleClass().add("button-primary");
        btnExportar.setOnAction(e -> exportReport());

        Button btnRefresh = new Button(
                "Actualizar",
                IconUtils.icon(Feather.REFRESH_CW, IconUtils.SIZE_SMALL)
        );
        btnRefresh.getStyleClass().add("button-outlined");
        btnRefresh.setOnAction(e -> refreshData());

        box.getChildren().addAll(icon, titleBox, spacer, btnExportar, btnRefresh);
        return box;
    }

    private HBox buildFiltersBox() {
        HBox box = new HBox(10);
        box.getStyleClass().add("kubata-reports-filterbar");
        box.setPadding(new Insets(10, 20, 12, 20));
        box.setAlignment(Pos.CENTER_LEFT);

        Label filterIcon = new Label("", IconUtils.icon(Feather.FILTER, 13));
        filterIcon.getStyleClass().add("kubata-reports-filter-icon");

        Label periodo = new Label("Período");
        periodo.getStyleClass().add("kubata-reports-filter-label");

        dpInicio = new DatePicker(LocalDate.now().minusMonths(1));
        dpInicio.setPrefWidth(145);

        Label arrow = new Label("→");
        arrow.getStyleClass().add("kubata-reports-filter-arrow");

        dpFim = new DatePicker(LocalDate.now());
        dpFim.setPrefWidth(145);

        Label categoria = new Label("Categoria");
        categoria.getStyleClass().add("kubata-reports-filter-label");

        cbCategoria = new ComboBox<>(FXCollections.observableArrayList(
                "Todos",
                "Utilizadores",
                "Audit",
                "Segurança",
                "Sistema"
        ));
        cbCategoria.setValue("Todos");
        cbCategoria.setPrefWidth(155);

        Button btnHoje = new Button("Hoje");
        btnHoje.getStyleClass().add("button-outlined");
        btnHoje.setOnAction(e -> {
            LocalDate today = LocalDate.now();
            dpInicio.setValue(today);
            dpFim.setValue(today);
            refreshData();
        });

        Button btnMes = new Button("Este mês");
        btnMes.getStyleClass().add("button-outlined");
        btnMes.setOnAction(e -> {
            LocalDate today = LocalDate.now();
            dpInicio.setValue(today.withDayOfMonth(1));
            dpFim.setValue(today);
            refreshData();
        });

        Button btnAplicar = new Button(
                "Aplicar filtros",
                IconUtils.icon(Feather.CHECK, 13)
        );
        btnAplicar.getStyleClass().add("button-primary");
        btnAplicar.setOnAction(e -> refreshData());

        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label scope = new Label("Dados do Kubata Administrator");
        scope.getStyleClass().add("kubata-reports-filter-scope");

        box.getChildren().addAll(
                filterIcon, periodo, dpInicio, arrow, dpFim,
                categoria, cbCategoria, btnHoje, btnMes, btnAplicar,
                spacer, scope
        );
        return box;
    }

    private VBox buildKPISection() {
        VBox section = new VBox(10);

        HBox heading = new HBox(8);
        heading.setAlignment(Pos.CENTER_LEFT);

        Label icon = new Label("", IconUtils.icon(Feather.ACTIVITY, 16));
        icon.getStyleClass().add("kubata-reports-section-icon");

        VBox titleBox = new VBox(2);
        Label title = new Label("Visão executiva");
        title.getStyleClass().add("kubata-reports-section-title");

        Label subtitle = new Label(
                "Estado actual do ambiente administrativo e actividade registada no período seleccionado."
        );
        subtitle.getStyleClass().add("kubata-reports-section-subtitle");

        titleBox.getChildren().addAll(title, subtitle);
        heading.getChildren().addAll(icon, titleBox);

        lblTotalUsers = new Label("—");
        lblActiveUsers = new Label("—");
        lblTotalEmpresas = new Label("—");
        lblTotalLogs = new Label("—");

        FlowPane kpiPane = new FlowPane(12, 12);
        kpiPane.getStyleClass().add("kubata-reports-kpi-grid");
        kpiPane.getChildren().addAll(
                createStatCard("Utilizadores", "Total registado", lblTotalUsers, Feather.USERS),
                createStatCard("Utilizadores activos", "Contas activas", lblActiveUsers, Feather.USER_CHECK),
                createStatCard("Empresas", "Entidades registadas", lblTotalEmpresas, Feather.BRIEFCASE),
                createStatCard("Eventos de auditoria", "No período seleccionado", lblTotalLogs, Feather.ACTIVITY)
        );

        section.getChildren().addAll(heading, kpiPane);
        return section;
    }

    private HBox buildChartSection() {
        HBox box = new HBox(20);
        box.setPrefHeight(350);

        // Gráfico de Pizza - Status de Utilizadores
        VBox userChartBox = new VBox(10);
        userChartBox.getStyleClass().add("kubata-reports-chart-card");
        HBox.setHgrow(userChartBox, Priority.ALWAYS);
        
        Label lblUserChart = new Label("Status dos Utilizadores", IconUtils.icon(Feather.PIE_CHART, 14));
        lblUserChart.getStyleClass().add("text-bold");
        
        userStatusChart = new PieChart();
        userStatusChart.setLegendSide(javafx.geometry.Side.BOTTOM);
        userStatusChart.setLabelsVisible(true);
        userChartBox.getChildren().addAll(lblUserChart, userStatusChart);

        // Gráfico de Barras - Atividade de Auditoria (Últimos 7 dias)
        VBox auditChartBox = new VBox(10);
        auditChartBox.getStyleClass().add("kubata-reports-chart-card");
        HBox.setHgrow(auditChartBox, Priority.ALWAYS);

        Label lblAuditChart = new Label("Atividade de Auditoria (Frequência)", IconUtils.icon(Feather.BAR_CHART_2, 14));
        lblAuditChart.getStyleClass().add("text-bold");

        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Período");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Ocorrências");

        auditActivityChart = new BarChart<>(xAxis, yAxis);
        auditActivityChart.setLegendVisible(false);
        auditActivityChart.setAnimated(false);
        auditChartBox.getChildren().addAll(lblAuditChart, auditActivityChart);

        box.getChildren().addAll(userChartBox, auditChartBox);
        return box;
    }

    private void refreshData() {
        if (dpInicio != null && dpFim != null
                && dpInicio.getValue() != null
                && dpFim.getValue() != null
                && dpFim.getValue().isBefore(dpInicio.getValue())) {
            modalManager.alert(
                    "Período inválido",
                    "A data final não pode ser anterior à data inicial.",
                    "warning",
                    null
            );
            return;
        }

        lblTotalUsers.setText("...");
        lblActiveUsers.setText("...");
        lblTotalEmpresas.setText("...");
        lblTotalLogs.setText("...");

        persistenceService.executeSilent(() -> {
            try {
                long totalUtilizadores = userRepository.count();
                long totalEmpresas = empresaRepository.count();

                LocalDateTime startDate = dpInicio.getValue() == null
                        ? LocalDateTime.now().minusMonths(1)
                        : dpInicio.getValue().atStartOfDay();
                LocalDateTime endDate = dpFim.getValue() == null
                        ? LocalDateTime.now()
                        : dpFim.getValue().plusDays(1).atStartOfDay().minusNanos(1);

                String category = cbCategoria == null ? "Todos" : cbCategoria.getValue();
                List<AuditLog> logs = auditLogRepository
                        .findByTimestampBetweenOrderByTimestampDesc(startDate, endDate)
                        .stream()
                        .filter(log -> category == null
                                || category.equalsIgnoreCase("Todos")
                                || category.equalsIgnoreCase("Audit")
                                || category.equalsIgnoreCase(log.getModule())
                                || category.equalsIgnoreCase(log.getEntityType()))
                        .toList();

                long totalLogs = logs.size();
                long utilizadoresAtivos = userRepository.findAll().stream()
                        .filter(User::getActive)
                        .count();
                long utilizadoresInativos = Math.max(0, totalUtilizadores - utilizadoresAtivos);

                List<Object[]> dailyCounts = auditLogRepository.countByDay(
                        LocalDateTime.now().minusDays(7),
                        LocalDateTime.now()
                );

                Map<String, Long> countMap = new java.util.LinkedHashMap<>();
                DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                for (int day = 6; day >= 0; day--) {
                    countMap.put(
                            LocalDate.now().minusDays(day).format(dayFormatter),
                            0L
                    );
                }

                for (Object[] row : dailyCounts) {
                    if (row != null && row.length >= 2 && row[0] != null) {
                        countMap.put(
                                row[0].toString(),
                                ((Number) row[1]).longValue()
                        );
                    }
                }

                Platform.runLater(() -> {
                    lblTotalUsers.setText(String.valueOf(totalUtilizadores));
                    lblActiveUsers.setText(String.valueOf(utilizadoresAtivos));
                    lblTotalEmpresas.setText(String.valueOf(totalEmpresas));
                    lblTotalLogs.setText(String.valueOf(totalLogs));

                    userStatusChart.getData().clear();
                    if (utilizadoresAtivos > 0) {
                        userStatusChart.getData().add(
                                new PieChart.Data(
                                        "Activos (" + utilizadoresAtivos + ")",
                                        utilizadoresAtivos
                                )
                        );
                    }
                    if (utilizadoresInativos > 0) {
                        userStatusChart.getData().add(
                                new PieChart.Data(
                                        "Inactivos (" + utilizadoresInativos + ")",
                                        utilizadoresInativos
                                )
                        );
                    }

                    auditActivityChart.getData().clear();
                    XYChart.Series<String, Number> activitySeries = new XYChart.Series<>();
                    countMap.forEach((day, count) ->
                            activitySeries.getData().add(
                                    new XYChart.Data<>(day.substring(8), count)
                            )
                    );
                    auditActivityChart.getData().add(activitySeries);
                });
            } catch (Exception ex) {
                Platform.runLater(() -> modalManager.alert(
                        "Erro nos indicadores",
                        ex.getMessage() == null
                                ? "Não foi possível carregar os indicadores."
                                : ex.getMessage(),
                        "error",
                        ex
                ));
            }
        }, null);
    }

    private HBox createStatCard(
            String title,
            String hint,
            Label valueLbl,
            Feather icon
    ) {
        HBox card = new HBox(12);
        card.getStyleClass().add("kubata-reports-stat-card");
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPrefWidth(245);
        card.setPrefHeight(86);

        StackPane iconPane = new StackPane();
        iconPane.getStyleClass().add("kubata-reports-stat-icon");
        iconPane.getChildren().add(new Label("", IconUtils.icon(icon, 19)));

        VBox textBox = new VBox(2);
        Label label = new Label(title.toUpperCase());
        label.getStyleClass().add("kubata-reports-stat-label");

        valueLbl.getStyleClass().add("kubata-reports-stat-value");

        Label helper = new Label(hint);
        helper.getStyleClass().add("kubata-reports-stat-hint");

        textBox.getChildren().addAll(label, valueLbl, helper);
        HBox.setHgrow(textBox, Priority.ALWAYS);
        card.getChildren().addAll(iconPane, textBox);
        return card;
    }

    private VBox buildReportsSection() {
        VBox section = new VBox(10);

        HBox heading = new HBox(8);
        heading.setAlignment(Pos.CENTER_LEFT);

        Label icon = new Label("", IconUtils.icon(Feather.FILE_TEXT, 16));
        icon.getStyleClass().add("kubata-reports-section-icon");

        VBox titleBox = new VBox(2);
        Label title = new Label("Catálogo de relatórios");
        title.getStyleClass().add("kubata-reports-section-title");

        Label subtitle = new Label(
                "Modelos preparados para consulta, controlo e análise administrativa."
        );
        subtitle.getStyleClass().add("kubata-reports-section-subtitle");

        titleBox.getChildren().addAll(title, subtitle);
        heading.getChildren().addAll(icon, titleBox);

        VBox reportsBox = new VBox(8);
        reportsBox.getStyleClass().add("kubata-reports-catalog");

        reportsBox.getChildren().addAll(
                createReportItem(
                        "Utilizadores por Perfil",
                        "Lista detalhada de utilizadores, perfil e estado da conta.",
                        Feather.SHIELD
                ),
                createReportItem(
                        "Empresas Activas",
                        "Relação das empresas registadas e respectiva parametrização.",
                        Feather.BRIEFCASE
                ),
                createReportItem(
                        "Histórico de Auditoria",
                        "Trilha de eventos do sistema para revisão e controlo.",
                        Feather.EYE
                ),
                createReportItem(
                        "Estatísticas de Acesso",
                        "Sessões, duração e actividade do utilizador por período.",
                        Feather.BAR_CHART_2
                ),
                createReportItem(
                        "Backup e Restore",
                        "Histórico de cópias, tamanhos, estados e restaurações.",
                        Feather.ARCHIVE
                )
        );

        section.getChildren().addAll(heading, reportsBox);
        return section;
    }

    private HBox createReportItem(String title, String description, Feather icon) {
        HBox item = new HBox(12);
        item.getStyleClass().add("kubata-reports-report-card");
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPrefHeight(76);

        StackPane iconPane = new StackPane();
        iconPane.getStyleClass().add("kubata-reports-report-icon");
        iconPane.getChildren().add(new Label("", IconUtils.icon(icon, 17)));

        VBox textBox = new VBox(3);
        Label lblTitle = new Label(title);
        lblTitle.getStyleClass().add("kubata-reports-report-title");

        Label lblDesc = new Label(description);
        lblDesc.setWrapText(true);
        lblDesc.getStyleClass().add("kubata-reports-report-desc");

        textBox.getChildren().addAll(lblTitle, lblDesc);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        Label format = new Label("JASPER");
        format.getStyleClass().add("kubata-reports-report-format");

        Button btnGerar = new Button(
                "Gerar",
                IconUtils.icon(Feather.PLAY, 11)
        );
        btnGerar.getStyleClass().add("button-outlined");
        btnGerar.setOnAction(e -> gerarRelatorioReal(title, btnGerar));

        item.getChildren().addAll(iconPane, textBox, format, btnGerar);
        return item;
    }

    private void gerarRelatorioReal(String reportTitle, Button btnGerar) {
        // Feedback visual de carregamento
        ProgressIndicator loading = new ProgressIndicator();
        loading.setPrefSize(16, 16);
        loading.setStyle("-fx-accent: #4CAF50;"); // COR VERDE
        javafx.scene.Node originalGraphic = btnGerar.getGraphic();
        btnGerar.setGraphic(loading);
        btnGerar.setDisable(true);

        persistenceService.executeAsync(() -> {
            try {
                // Carregar parâmetros da empresa ativa
                Empresa empresa = empresaRepository.findFirstByAtivaTrue().orElse(new Empresa());
                Map<String, Object> params = new HashMap<>();
                params.put("EMPRESA_NOME", empresa.getNome());
                params.put("EMPRESA_NIF", empresa.getNif());
                params.put("PERIODO_INICIO", dpInicio.getValue() != null ? dpInicio.getValue().toString() : "N/A");
                params.put("PERIODO_FIM", dpFim.getValue() != null ? dpFim.getValue().toString() : "N/A");

                JasperPrint jasperPrint = null;
                String reportFile = "";
                JRDataSource dataSource = null;

                if (reportTitle.contains("Utilizadores")) {
                    reportFile = "classpath:reports/users_list.jrxml";
                    dataSource = new JRBeanCollectionDataSource(userRepository.findAll());
                } else if (reportTitle.contains("Empresas")) {
                    reportFile = "classpath:reports/empresas_list.jrxml";
                    dataSource = new JRBeanCollectionDataSource(empresaRepository.findAll());
                } else if (reportTitle.contains("Auditoria")) {
                    reportFile = "classpath:reports/audit_log.jrxml";
                    // Converter AuditLog para DTO para compatibilidade com JasperReports
                    List<AuditLogReportDTO> auditLogDTOs = auditLogRepository.findAll()
                            .stream()
                            .map(AuditLogReportDTO::new)
                            .collect(java.util.stream.Collectors.toList());
                    dataSource = new JRBeanCollectionDataSource(auditLogDTOs);
                } else if (reportTitle.contains("Estatísticas")) {
                    reportFile = "classpath:reports/access_statistics.jrxml";
                    // Dados reais derivados do histórico de auditoria.
                    dataSource = new JRBeanCollectionDataSource(generateAccessStatisticsData());
                } else if (reportTitle.contains("Backup")) {
                    reportFile = "classpath:reports/backup_restore.jrxml";
                    // Dados reais do histórico de backups.
                    dataSource = new JRBeanCollectionDataSource(generateBackupRestoreData());
                } else {
                    Platform.runLater(() -> {
                        btnGerar.setGraphic(originalGraphic);
                        btnGerar.setDisable(false);
                        modalManager.alert("Informação", "O relatório '" + reportTitle + "' não está disponível nesta instalação.", "info", null);
                    });
                    return;
                }

                InputStream jrxml = resourceLoader.getResource(reportFile).getInputStream();
                JasperReport report = JasperCompileManager.compileReport(jrxml);
                jasperPrint = JasperFillManager.fillReport(report, params, dataSource);

                if (jasperPrint != null) {
                    final JasperPrint jp = jasperPrint;
                    Platform.runLater(() -> {
                        btnGerar.setGraphic(originalGraphic);
                        btnGerar.setDisable(false);
                        
                        JasperViewerPane viewer = new JasperViewerPane(jp);
                        Stage stage = new Stage();
                        stage.setTitle("Kubata Admin - Visualizador: " + reportTitle);
                        stage.setScene(new Scene(viewer, 1000, 750));
                        
                        // Tenta carregar o ícone de forma segura
                        try {
                            InputStream iconStream = getClass().getResourceAsStream("/images/logo.png");
                            if (iconStream != null) {
                                stage.getIcons().add(new javafx.scene.image.Image(iconStream));
                            }
                        } catch (Exception ignore) {}
                        
                        stage.show();
                    });
                }

            } catch (Exception ex) {
                Platform.runLater(() -> {
                    btnGerar.setGraphic(originalGraphic);
                    btnGerar.setDisable(false);
                    modalManager.alert("Erro", "Falha ao gerar relatório: " + ex.getMessage(), "error", ex);
                });
            }
        }, "REPORT_GEN", "RELATORIOS", "Geração de relatório: " + reportTitle, null);
    }

    private void exportReport() {
        VBox content = new VBox(12);
        content.setPadding(new Insets(10));

        Label lbl = new Label(
                "Exporte o histórico de auditoria do período selecionado."
        );
        lbl.setWrapText(true);
        lbl.getStyleClass().add("text-muted");

        HBox formats = new HBox(10);
        formats.setAlignment(Pos.CENTER);

        Button btnPdf = new Button(
                "PDF",
                IconUtils.icon(Feather.FILE_TEXT, IconUtils.SIZE_SMALL)
        );
        btnPdf.getStyleClass().add("button-primary");
        btnPdf.setOnAction(e -> {
            modalManager.hideModal();
            exportAuditPdf();
        });

        Button btnCsv = new Button(
                "CSV",
                IconUtils.icon(Feather.DOWNLOAD, IconUtils.SIZE_SMALL)
        );
        btnCsv.getStyleClass().add("button-outlined");
        btnCsv.setOnAction(e -> {
            modalManager.hideModal();
            exportAuditCsv();
        });

        formats.getChildren().addAll(btnPdf, btnCsv);
        content.getChildren().addAll(lbl, formats);

        modalManager.showModalSimple(content, "Exportar Auditoria");
    }

    private List<Map<String, Object>> generateAccessStatisticsData() {
        List<Map<String, Object>> data = new ArrayList<>();

        LocalDateTime start = dpInicio.getValue() == null
                ? LocalDateTime.now().minusMonths(1)
                : dpInicio.getValue().atStartOfDay();
        LocalDateTime end = dpFim.getValue() == null
                ? LocalDateTime.now()
                : dpFim.getValue().plusDays(1).atStartOfDay().minusNanos(1);

        List<AuditLog> logs = auditLogRepository
                .findByTimestampBetweenOrderByTimestampDesc(start, end);

        Map<String, AuditLog> openSessions = new HashMap<>();
        Map<String, Integer> actionsBySession = new HashMap<>();

        for (AuditLog log : logs) {
            if (log == null || log.getTimestamp() == null) {
                continue;
            }

            String sessionId = log.getSessionId();
            String key = sessionId != null && !sessionId.isBlank()
                    ? sessionId
                    : (log.getUsername() == null ? "UNKNOWN" : log.getUsername());

            actionsBySession.merge(key, 1, Integer::sum);

            if (log.getActionType() == AuditLog.AuditActionType.LOGIN) {
                openSessions.put(key, log);
                continue;
            }

            if (log.getActionType() == AuditLog.AuditActionType.LOGOUT) {
                AuditLog login = openSessions.remove(key);

                Map<String, Object> record = new HashMap<>();
                record.put("username", log.getUsername());
                record.put("loginTime", login != null ? login.getTimestamp() : log.getTimestamp());
                record.put("logoutTime", log.getTimestamp());

                long duration = login != null
                        ? Math.max(0, java.time.Duration.between(
                                login.getTimestamp(), log.getTimestamp()).toMinutes())
                        : 0L;

                record.put("durationMinutes", duration);
                record.put("ipAddress", login != null && login.getIpAddress() != null
                        ? login.getIpAddress()
                        : log.getIpAddress());
                record.put("module", login != null && login.getModule() != null
                        ? login.getModule()
                        : (log.getModule() != null ? log.getModule() : "Sistema"));
                record.put("actionsCount", actionsBySession.getOrDefault(key, 1));

                data.add(record);
            }
        }

        // Sessões que continuam abertas são igualmente úteis no relatório.
        for (Map.Entry<String, AuditLog> entry : openSessions.entrySet()) {
            AuditLog login = entry.getValue();
            if (login == null || login.getTimestamp() == null) {
                continue;
            }

            Map<String, Object> record = new HashMap<>();
            record.put("username", login.getUsername());
            record.put("loginTime", login.getTimestamp());
            record.put("logoutTime", null);
            record.put("durationMinutes",
                    Math.max(0, java.time.Duration.between(
                            login.getTimestamp(), LocalDateTime.now()).toMinutes()));
            record.put("ipAddress", login.getIpAddress());
            record.put("module", login.getModule() != null ? login.getModule() : "Sistema");
            record.put("actionsCount", actionsBySession.getOrDefault(entry.getKey(), 1));

            data.add(record);
        }

        data.sort(java.util.Comparator.comparing(
                row -> (LocalDateTime) row.get("loginTime"),
                java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())
        ));

        return data;
    }

    private List<Map<String, Object>> generateBackupRestoreData() {
        List<Map<String, Object>> data = new ArrayList<>();

        List<BackupRecord> records = backupRecordRepository.findAll().stream()
                .filter(r -> r != null && r.getStartTime() != null)
                .sorted(java.util.Comparator.comparing(
                        BackupRecord::getStartTime,
                        java.util.Comparator.reverseOrder()
                ))
                .toList();

        for (BackupRecord record : records) {
            Map<String, Object> row = new HashMap<>();
            row.put("backupId", String.valueOf(record.getId()));
            row.put("backupDate", record.getStartTime());
            row.put("backupType", formatBackupType(record.getType()));
            row.put("backupSize", record.getFileSize() != null ? record.getFileSize() : 0L);
            row.put("backupPath", record.getFilename());
            row.put("status", record.getStatus() != null
                    ? record.getStatus().getDescription()
                    : "Desconhecido");
            row.put("createdBy", record.getTriggeredBy() != null
                    ? record.getTriggeredBy() : "Sistema");
            row.put("description", record.getNotes() != null
                    ? record.getNotes()
                    : record.getErrorMessage());
            row.put("restoreDate", record.getRestoredAt());
            row.put("restoredBy", record.getRestoredBy());
            data.add(row);
        }

        return data;
    }

    private String formatBackupType(String type) {
        if (type == null || type.isBlank()) {
            return "Não especificado";
        }

        return switch (type.toUpperCase()) {
            case "AUTOMATIC" -> "Automático";
            case "MANUAL" -> "Manual";
            case "SCHEDULED" -> "Agendado";
            default -> type;
        };
    }

    private List<AuditLogReportDTO> getFilteredAuditReports() {
        LocalDateTime start = dpInicio.getValue() == null
                ? LocalDateTime.now().minusMonths(1)
                : dpInicio.getValue().atStartOfDay();
        LocalDateTime end = dpFim.getValue() == null
                ? LocalDateTime.now()
                : dpFim.getValue().plusDays(1).atStartOfDay().minusNanos(1);

        String category = cbCategoria.getValue();
        List<AuditLog> logs = auditLogRepository
                .findByTimestampBetweenOrderByTimestampDesc(start, end);

        return logs.stream()
                .filter(log -> category == null
                        || category.equalsIgnoreCase("Todos")
                        || category.equalsIgnoreCase("Audit")
                        || category.equalsIgnoreCase(log.getModule())
                        || category.equalsIgnoreCase(log.getEntityType()))
                .map(AuditLogReportDTO::new)
                .toList();
    }

    private void exportAuditCsv() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Guardar auditoria");
        chooser.setInitialFileName("kubata-auditoria-" +
                LocalDate.now().format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE) +
                ".csv");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV", "*.csv"));

        java.io.File file = chooser.showSaveDialog(getScene() != null
                ? getScene().getWindow()
                : null);

        if (file == null) {
            return;
        }

        persistenceService.executeAsync(() -> {
            List<AuditLogReportDTO> rows = getFilteredAuditReports();

            try (java.io.BufferedWriter writer = java.nio.file.Files.newBufferedWriter(
                    file.toPath(), java.nio.charset.StandardCharsets.UTF_8)) {

                writer.write("\uFEFF");
                writer.write("Data/Hora;Utilizador;Acção;Entidade;Alterações;Duração (ms)");
                writer.newLine();

                for (AuditLogReportDTO row : rows) {
                    writer.write(csv(row.getTimestamp()));
                    writer.write(";");
                    writer.write(csv(row.getUsername()));
                    writer.write(";");
                    writer.write(csv(row.getActionType()));
                    writer.write(";");
                    writer.write(csv(row.getEntityType()));
                    writer.write(";");
                    writer.write(csv(row.getNewValues()));
                    writer.write(";");
                    writer.write(csv(row.getDuracaoMs()));
                    writer.newLine();
                }
            } catch (java.io.IOException ex) {
                throw new IllegalStateException(
                        "Não foi possível escrever o ficheiro CSV: " + ex.getMessage(), ex);
            }
        }, "REPORT_EXPORT", "RELATORIOS",
                "Exportação CSV do histórico de auditoria", () ->
                        modalManager.alert("Exportação concluída",
                                "O ficheiro foi guardado em " + file.getAbsolutePath(),
                                "success", null));
    }

    private void exportAuditPdf() {
        persistenceService.executeAsync(() -> {
            try {
                List<AuditLogReportDTO> rows = getFilteredAuditReports();

                InputStream jrxml = resourceLoader
                        .getResource("classpath:reports/audit_log.jrxml")
                        .getInputStream();
                JasperReport report = JasperCompileManager.compileReport(jrxml);

                Map<String, Object> params = new HashMap<>();
                params.put("PERIODO_INICIO",
                        dpInicio.getValue() != null ? dpInicio.getValue().toString() : "");
                params.put("PERIODO_FIM",
                        dpFim.getValue() != null ? dpFim.getValue().toString() : "");

                JasperPrint print = JasperFillManager.fillReport(
                        report, params, new JRBeanCollectionDataSource(rows));

                Platform.runLater(() -> {
                    JasperViewerPane viewer = new JasperViewerPane(print);
                    Stage stage = new Stage();
                    stage.setTitle("Kubata Admin - Auditoria");
                    stage.setScene(new Scene(viewer, 1100, 780));
                    stage.show();
                });
            } catch (Exception ex) {
                Platform.runLater(() -> modalManager.alert(
                        "Erro",
                        "Não foi possível exportar a auditoria: " + ex.getMessage(),
                        "error", ex));
            }
        }, "REPORT_PDF", "RELATORIOS",
                "Geração PDF do histórico de auditoria", null);
    }

    private static String csv(Object value) {
        if (value == null) {
            return "";
        }

        String text = String.valueOf(value)
                .replace("\r", " ")
                .replace("\n", " ")
                .replace("\"", "\"\"");

        if (text.indexOf(';') >= 0 || text.indexOf('"') >= 0) {
            return "\"" + text + "\"";
        }

        return text;
    }

}

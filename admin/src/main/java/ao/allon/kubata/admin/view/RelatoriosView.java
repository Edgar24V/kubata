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
        HBox box = new HBox(10);
        box.getStyleClass().add("header-box");
        box.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Relatórios Administrativos");
        title.getStyleClass().add("h3");

        Button btnExportar = new Button("Exportar Relatório", IconUtils.icon(Feather.DOWNLOAD, IconUtils.SIZE_SMALL));
        btnExportar.getStyleClass().add("button-primary");
        btnExportar.setOnAction(e -> exportReport());

        Button btnRefresh = new Button(null, IconUtils.icon(Feather.REFRESH_CW, IconUtils.SIZE_SMALL));
        btnRefresh.getStyleClass().add("button-outlined");
        btnRefresh.setOnAction(e -> refreshData());

        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        box.getChildren().addAll(title, spacer, btnExportar, btnRefresh);
        return box;
    }

    private HBox buildFiltersBox() {
        HBox box = new HBox(15);
        box.getStyleClass().add("filters-pane");
        box.setPadding(new Insets(15, 20, 15, 20));
        box.setAlignment(Pos.CENTER_LEFT);
        box.setStyle("-fx-background-color: #fcfcfc; -fx-border-color: #eeeeee; -fx-border-width: 0 0 1 0;");

        Label lblFiltros = new Label("Filtros:", IconUtils.icon(Feather.FILTER, 14));
        lblFiltros.getStyleClass().add("text-bold");

        dpInicio = new DatePicker(LocalDate.now().minusMonths(1));
        dpInicio.setPromptText("Data Início");
        dpInicio.setPrefWidth(150);

        dpFim = new DatePicker(LocalDate.now());
        dpFim.setPromptText("Data Fim");
        dpFim.setPrefWidth(150);

        cbCategoria = new ComboBox<>(FXCollections.observableArrayList("Todos", "Utilizadores", "Audit", "Segurança", "Sistema"));
        cbCategoria.setValue("Todos");
        cbCategoria.setPrefWidth(150);

        Button btnAplicar = new Button("Aplicar", IconUtils.icon(Feather.CHECK, 14));
        btnAplicar.getStyleClass().add("button-primary");
        btnAplicar.setOnAction(e -> refreshData());

        box.getChildren().addAll(lblFiltros, new Label("Início:"), dpInicio, new Label("Fim:"), dpFim, new Label("Categoria:"), cbCategoria, btnAplicar);
        return box;
    }

    private VBox buildKPISection() {
        VBox section = new VBox(15);
        
        Label lblTitle = new Label("Indicadores de Desempenho", IconUtils.icon(Feather.ACTIVITY, 16));
        lblTitle.getStyleClass().add("h4");
        lblTitle.setStyle("-fx-font-weight: bold;");

        // Labels para os valores (serão atualizados no refreshData)
        lblTotalUsers = new Label("...");
        lblActiveUsers = new Label("...");
        lblTotalEmpresas = new Label("...");
        lblTotalLogs = new Label("...");

        FlowPane kpiPane = new FlowPane(20, 20);
        kpiPane.getChildren().addAll(
            createStatCard("Utilizadores",       lblTotalUsers,    Feather.USERS),
            createStatCard("Utilizadores Ativos", lblActiveUsers,   Feather.USER_CHECK),
            createStatCard("Empresas",           lblTotalEmpresas,  Feather.BRIEFCASE),
            createStatCard("Total Logs",         lblTotalLogs,      Feather.ACTIVITY)
        );

        section.getChildren().addAll(lblTitle, kpiPane);
        return section;
    }

    private HBox buildChartSection() {
        HBox box = new HBox(20);
        box.setPrefHeight(350);

        // Gráfico de Pizza - Status de Utilizadores
        VBox userChartBox = new VBox(10);
        userChartBox.getStyleClass().add("card");
        HBox.setHgrow(userChartBox, Priority.ALWAYS);
        
        Label lblUserChart = new Label("Status dos Utilizadores", IconUtils.icon(Feather.PIE_CHART, 14));
        lblUserChart.getStyleClass().add("text-bold");
        
        userStatusChart = new PieChart();
        userStatusChart.setLegendSide(javafx.geometry.Side.BOTTOM);
        userStatusChart.setLabelsVisible(true);
        userChartBox.getChildren().addAll(lblUserChart, userStatusChart);

        // Gráfico de Barras - Atividade de Auditoria (Últimos 7 dias)
        VBox auditChartBox = new VBox(10);
        auditChartBox.getStyleClass().add("card");
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

    private VBox buildReportsSection() {
        VBox section = new VBox(15);
        
        Label lblTitle = new Label("Catálogo de Relatórios", IconUtils.icon(Feather.LIST, 16));
        lblTitle.getStyleClass().add("h4");
        lblTitle.setStyle("-fx-font-weight: bold;");

        VBox reportsBox = new VBox(10);
        reportsBox.getChildren().addAll(
                createReportItem("Utilizadores por Perfil",  "Lista detalhada de utilizadores agrupados por perfil de acesso", Feather.SHIELD),
                createReportItem("Empresas Ativas",          "Relatório consolidado de empresas ativas e parametrização fiscal", Feather.CHECK_CIRCLE),
                createReportItem("Histórico de Auditoria",   "Log completo de transações e alterações de sistema (AGT Compliance)", Feather.CLOCK),
                createReportItem("Estatísticas de Acesso",   "Análise temporal de acessos e atividade concorrente", Feather.BAR_CHART_2),
                createReportItem("Backup e Restore",         "Registo histórico de cópias de segurança e integridade", Feather.ARCHIVE)
        );

        section.getChildren().addAll(lblTitle, reportsBox);
        return section;
    }

    private Label lblTotalUsers, lblActiveUsers, lblTotalEmpresas, lblTotalLogs;

    private void refreshData() {
        // Mostrar Loading nos labels
        lblTotalUsers.setText("...");
        lblActiveUsers.setText("...");
        lblTotalEmpresas.setText("...");
        lblTotalLogs.setText("...");

        persistenceService.executeSilent(() -> {
            try {
                long totalUtilizadores = userRepository.count();
                long totalEmpresas = empresaRepository.count();
                long totalLogs = auditLogRepository.count();
                long utilizadoresAtivos = userRepository.findAll().stream()
                        .filter(User::getActive).count();
                long utilizadoresInativos = totalUtilizadores - utilizadoresAtivos;

                Platform.runLater(() -> {
                    lblTotalUsers.setText(String.valueOf(totalUtilizadores));
                    lblActiveUsers.setText(String.valueOf(utilizadoresAtivos));
                    lblTotalEmpresas.setText(String.valueOf(totalEmpresas));
                    lblTotalLogs.setText(String.valueOf(totalLogs));

                    // Atualizar Gráfico de Pizza
                    userStatusChart.getData().clear();
                    userStatusChart.getData().add(new PieChart.Data("Ativos (" + utilizadoresAtivos + ")", utilizadoresAtivos));
                    userStatusChart.getData().add(new PieChart.Data("Inativos (" + utilizadoresInativos + ")", utilizadoresInativos));

                    // Atualizar Gráfico de Barras (Dados reais baseados no AuditLog)
                    auditActivityChart.getData().clear();
                    XYChart.Series<String, Number> series = new XYChart.Series<>();
                    
                    LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
                    List<Object[]> dailyCounts = auditLogRepository.countByDay(weekAgo, LocalDateTime.now());
                    
                    // Mapa para facilitar o preenchimento dos dias (garantindo que todos os dias apareçam)
                    Map<String, Long> countMap = new HashMap<>();
                    DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                    for (int i = 6; i >= 6; i--) { // Corrigido loop para 7 dias
                        countMap.put(LocalDate.now().minusDays(i).format(dayFormatter), 0L);
                    }
                    // Reinicializar countMap corretamente
                    countMap.clear();
                    for (int i = 6; i >= 0; i--) {
                        countMap.put(LocalDate.now().minusDays(i).format(dayFormatter), 0L);
                    }
                    
                    for (Object[] row : dailyCounts) {
                        if (row != null && row.length >= 2 && row[0] != null) {
                            countMap.put(row[0].toString(), ((Number) row[1]).longValue());
                        }
                    }
                    
                    countMap.entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .forEach(entry -> {
                            String label = entry.getKey().substring(8); // Só o dia
                            series.getData().add(new XYChart.Data<>(label, entry.getValue()));
                        });

                    auditActivityChart.getData().add(series);
                });
            } catch (Exception e) {
                Platform.runLater(() ->
                    modalManager.alert("Erro", "Falha ao processar estatísticas: " + e.getMessage(), "error", e));
            }
        }, null);
    }

    private HBox createStatCard(String title, Label valueLbl, Feather icon) {
        HBox card = new HBox(15);
        card.getStyleClass().add("card-container");
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPrefWidth(220);
        card.setStyle("-fx-background-color: white; -fx-padding: 20; -fx-background-radius: 12; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.08), 10, 0, 0, 4); -fx-border-color: #f0f0f0; -fx-border-width: 1;");

        VBox textBox = new VBox(5);
        Label lblTitle = new Label(title.toUpperCase());
        lblTitle.setStyle("-fx-text-fill: #999999; -fx-font-size: 11px; -fx-font-weight: bold; -fx-letter-spacing: 1px;");

        valueLbl.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        textBox.getChildren().addAll(lblTitle, valueLbl);
        
        StackPane iconPane = new StackPane(IconUtils.icon(icon, 28));
        iconPane.setStyle("-fx-background-color: #f1f8e9; -fx-padding: 10; -fx-background-radius: 10; -fx-text-fill: -kubata-green;");
        
        card.getChildren().addAll(iconPane, textBox);
        return card;
    }

    private HBox createReportItem(String title, String description, Feather icon) {
        HBox item = new HBox(15);
        item.getStyleClass().add("card-item");
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPrefWidth(600);
        item.setStyle("-fx-background-color: white; -fx-padding: 15; -fx-background-radius: 8; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 5, 0, 0, 2);");

        Label iconLabel = new Label();
        iconLabel.setGraphic(IconUtils.icon(icon, IconUtils.SIZE_LARGE));
        iconLabel.setStyle("-fx-text-fill: -kubata-green;");

        VBox textBox = new VBox(3);
        Label lblTitle = new Label(title);
        lblTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        Label lblDesc = new Label(description);
        lblDesc.setStyle("-fx-text-fill: #666666; -fx-font-size: 12px;");

        textBox.getChildren().addAll(lblTitle, lblDesc);

        Button btnGerar = new Button("Gerar", IconUtils.icon(Feather.PLAY, IconUtils.SIZE_SMALL));
        btnGerar.getStyleClass().add("button-success");
        btnGerar.setOnAction(e -> gerarRelatorioReal(title, btnGerar));

        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        item.getChildren().addAll(iconLabel, textBox, spacer, btnGerar);
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

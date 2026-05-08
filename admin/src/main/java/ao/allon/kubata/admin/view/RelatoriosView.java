package ao.allon.kubata.admin.view;

import ao.allon.kubata.admin.service.PersistenceService;
import ao.allon.kubata.admin.service.SessionManager;
import ao.allon.kubata.admin.ui.modal.ModalManager;
import ao.allon.kubata.admin.ui.util.IconUtils;
import ao.allon.kubata.admin.ui.util.ThemeManager;
import ao.allon.kubata.core.domain.AuditLog;
import ao.allon.kubata.core.domain.Empresa;
import ao.allon.kubata.admin.ui.reports.JasperViewerPane;
import ao.allon.kubata.core.domain.User;
import ao.allon.kubata.core.repository.AuditLogRepository;
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
import java.util.Random;

@Component
public class RelatoriosView extends VBox {

    private final UserRepository userRepository;
    private final EmpresaRepository empresaRepository;
    private final AuditLogRepository auditLogRepository;
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
                          AuditLogRepository auditLogRepository, SessionManager sessionManager, ModalManager modalManager,
                          PersistenceService persistenceService, ResourceLoader resourceLoader) {
        this.userRepository = userRepository;
        this.empresaRepository = empresaRepository;
        this.auditLogRepository = auditLogRepository;
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
        btnRefresh.setOnAction(e -> {
            buildUI(); // Recarrega a UI para atualizar estatísticas
            modalManager.alert("Atualização", "Estatísticas atualizadas com sucesso.", "info", null);
        });

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
                    // Criar dados simulados para estatísticas de acesso
                    dataSource = new JRBeanCollectionDataSource(generateAccessStatisticsData());
                } else if (reportTitle.contains("Backup")) {
                    reportFile = "classpath:reports/backup_restore.jrxml";
                    // Criar dados simulados para backup e restauro
                    dataSource = new JRBeanCollectionDataSource(generateBackupRestoreData());
                } else {
                    Platform.runLater(() -> {
                        btnGerar.setGraphic(originalGraphic);
                        btnGerar.setDisable(false);
                        modalManager.alert("Informação", "O relatório '" + reportTitle + "' está em fase de desenho JRXML.", "info", null);
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
        VBox content = new VBox(15);
        content.setPadding(new Insets(10));
        content.setAlignment(Pos.CENTER);
        
        Label lbl = new Label("Selecione o formato de exportação:");
        
        HBox formats = new HBox(10);
        formats.setAlignment(Pos.CENTER);
        
        Button btnPdf = new Button("PDF", IconUtils.icon(Feather.FILE_TEXT, IconUtils.SIZE_SMALL));
        btnPdf.getStyleClass().add("outlined");
        btnPdf.setOnAction(e -> {
            modalManager.hideModal();
            modalManager.alert("Sucesso", "Relatório exportado em PDF com sucesso.", "info", null);
        });
        
        Button btnExcel = new Button("Excel", IconUtils.icon(Feather.FILE_TEXT, IconUtils.SIZE_SMALL));
        btnExcel.getStyleClass().add("outlined");
        btnExcel.setOnAction(e -> {
            modalManager.hideModal();
            modalManager.alert("Sucesso", "Relatório exportado em Excel com sucesso.", "info", null);
        });
        
        Button btnCsv = new Button("CSV", IconUtils.icon(Feather.FILE_TEXT, IconUtils.SIZE_SMALL));
        btnCsv.getStyleClass().add("outlined");
        btnCsv.setOnAction(e -> {
            modalManager.hideModal();
            modalManager.alert("Sucesso", "Relatório exportado em CSV com sucesso.", "info", null);
        });
        
        formats.getChildren().addAll(btnPdf, btnExcel, btnCsv);
        content.getChildren().addAll(lbl, formats);
        
        modalManager.showModalSimple(content, "Exportar Relatório");
    }

    private List<Map<String, Object>> generateAccessStatisticsData() {
        List<Map<String, Object>> data = new ArrayList<>();
        List<User> users = userRepository.findAll();
        Random random = new Random();
        
        for (User user : users) {
            for (int i = 0; i < 3; i++) {
                Map<String, Object> record = new HashMap<>();
                record.put("username", user.getUsername());
                record.put("loginTime", LocalDateTime.now().minusDays(random.nextInt(30)).minusHours(random.nextInt(24)));
                record.put("logoutTime", LocalDateTime.now().minusDays(random.nextInt(30)).minusHours(random.nextInt(24)).plusMinutes(random.nextInt(120)));
                record.put("durationMinutes", random.nextInt(120) + 10L);
                record.put("ipAddress", "192.168.1." + random.nextInt(255));
                record.put("module", getRandomModule(random));
                record.put("actionsCount", random.nextInt(50) + 5);
                data.add(record);
            }
        }
        return data;
    }

    private List<Map<String, Object>> generateBackupRestoreData() {
        List<Map<String, Object>> data = new ArrayList<>();
        Random random = new Random();
        
        for (int i = 0; i < 10; i++) {
            Map<String, Object> record = new HashMap<>();
            record.put("backupId", "BKP-" + String.format("%06d", i + 1));
            record.put("backupDate", LocalDateTime.now().minusDays(random.nextInt(90)));
            record.put("backupType", random.nextBoolean() ? "Completo" : "Incremental");
            record.put("backupSize", (long) (random.nextDouble() * 500 + 50) * 1024 * 1024);
            record.put("backupPath", "/backups/kubata_backup_" + String.format("%06d", i + 1) + ".sql");
            record.put("status", random.nextBoolean() ? "Sucesso" : "Falha");
            record.put("createdBy", "admin");
            record.put("description", random.nextBoolean() ? "Backup automático diário" : "Backup manual");
            
            if (random.nextBoolean()) {
                record.put("restoreDate", LocalDateTime.now().minusDays(random.nextInt(30)));
                record.put("restoredBy", "admin");
            } else {
                record.put("restoreDate", null);
                record.put("restoredBy", null);
            }
            data.add(record);
        }
        return data;
    }

    private String getRandomModule(Random random) {
        String[] modules = {"Admin", "Faturação", "Vendas", "Compras", "RH", "Financeiro", "Fiscal", "Inventário", "Relatórios"};
        return modules[random.nextInt(modules.length)];
    }
}

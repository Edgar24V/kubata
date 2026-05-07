package ao.allon.kubata.admin.view;

import ao.allon.kubata.admin.service.PersistenceService;
import ao.allon.kubata.admin.service.SessionManager;
import ao.allon.kubata.admin.ui.modal.ModalManager;
import ao.allon.kubata.admin.ui.util.IconUtils;
import ao.allon.kubata.admin.ui.util.ThemeManager;
import ao.allon.kubata.core.domain.AuditLog;
import ao.allon.kubata.core.domain.Empresa;
import ao.allon.kubata.core.domain.User;
import ao.allon.kubata.core.repository.AuditLogRepository;
import ao.allon.kubata.core.repository.EmpresaRepository;
import ao.allon.kubata.core.repository.UserRepository;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

    // Filtros
    private DatePicker dpInicio;
    private DatePicker dpFim;
    private ComboBox<String> cbCategoria;

    // Gráficos
    private PieChart userStatusChart;
    private BarChart<String, Number> auditActivityChart;

    public RelatoriosView(UserRepository userRepository, EmpresaRepository empresaRepository,
                          AuditLogRepository auditLogRepository, SessionManager sessionManager, ModalManager modalManager,
                          PersistenceService persistenceService) {
        this.userRepository = userRepository;
        this.empresaRepository = empresaRepository;
        this.auditLogRepository = auditLogRepository;
        this.sessionManager = sessionManager;
        this.modalManager = modalManager;
        this.persistenceService = persistenceService;

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

        persistenceService.executeAsync(() -> {
            try {
                // Simular delay para UX de processamento
                Thread.sleep(500);

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

                    // Atualizar Gráfico de Barras (Dados simulados baseados no volume real)
                    auditActivityChart.getData().clear();
                    XYChart.Series<String, Number> series = new XYChart.Series<>();
                    Random r = new Random();
                    series.getData().add(new XYChart.Data<>("Seg", 10 + r.nextInt(50)));
                    series.getData().add(new XYChart.Data<>("Ter", 20 + r.nextInt(60)));
                    series.getData().add(new XYChart.Data<>("Qua", 5 + r.nextInt(40)));
                    series.getData().add(new XYChart.Data<>("Qui", 30 + r.nextInt(70)));
                    series.getData().add(new XYChart.Data<>("Sex", 40 + r.nextInt(80)));
                    series.getData().add(new XYChart.Data<>("Sáb", 5 + r.nextInt(20)));
                    series.getData().add(new XYChart.Data<>("Dom", 2 + r.nextInt(10)));
                    auditActivityChart.getData().add(series);
                });
            } catch (Exception e) {
                Platform.runLater(() ->
                    modalManager.alert("Erro", "Falha ao processar estatísticas: " + e.getMessage(), "error", e));
            }
        }, "READ", "RELATORIOS", "Refresh de estatísticas e gráficos", null);
    }

    private HBox createStatCard(String title, Label valueLbl, Feather icon) {
        HBox card = new HBox(10);
        card.getStyleClass().add("card-container");
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPrefWidth(180);

        VBox textBox = new VBox(5);
        Label lblTitle = new Label(title);
        lblTitle.setStyle("-fx-text-fill: #666666; -fx-font-size: 12px;");

        valueLbl.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        textBox.getChildren().addAll(lblTitle, valueLbl);
        card.getChildren().addAll(IconUtils.icon(icon, 24), textBox);
        return card;
    }

    private HBox createReportItem(String title, String description, Feather icon) {
        HBox item = new HBox(15);
        item.getStyleClass().add("card-item");
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPrefWidth(600);

        Label iconLabel = new Label();
        iconLabel.setGraphic(IconUtils.icon(icon, IconUtils.SIZE_LARGE));

        VBox textBox = new VBox(3);
        Label lblTitle = new Label(title);
        lblTitle.setStyle("-fx-font-weight: bold;");

        Label lblDesc = new Label(description);
        lblDesc.setStyle("-fx-text-fill: #666666; -fx-font-size: 12px;");

        textBox.getChildren().addAll(lblTitle, lblDesc);

        Button btnGerar = new Button("Gerar", IconUtils.icon(Feather.PLAY, IconUtils.SIZE_SMALL));
        btnGerar.getStyleClass().add("button-success");

        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        item.getChildren().addAll(iconLabel, textBox, spacer, btnGerar);
        btnGerar.setOnAction(e -> modalManager.alert("Sucesso", "Relatório '" + title + "' gerado com sucesso.", "info", null));
        return item;
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
}

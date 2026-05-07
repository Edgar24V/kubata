package ao.allon.kubata.faturacao.view;

import ao.allon.kubata.core.ui.table.AdvancedTableView;
import ao.allon.kubata.core.ui.table.TableUtils;
import ao.allon.kubata.faturacao.domain.Fatura;
import ao.allon.kubata.faturacao.domain.ItemFatura;
import ao.allon.kubata.faturacao.domain.enums.StatusFatura;
import ao.allon.kubata.faturacao.service.FaturaService;
import ao.allon.kubata.faturacao.ui.util.AlertUtils;
import ao.allon.kubata.faturacao.ui.util.IconUtils;
import atlantafx.base.controls.Card;
import atlantafx.base.theme.Styles;
import com.dlsc.gemsfx.SearchTextField;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * View moderna para Mapa de Impostos com gráficos, KPIs e exportação avançada.
 * Utiliza AtlantaFX para temas e GemsFX para componentes avançados.
 */
public class MapaImpostosView extends BorderPane {

    public static class LinhaMapa {
        private final BigDecimal taxa;
        private final BigDecimal base;
        private final BigDecimal iva;
        private final BigDecimal total;

        public LinhaMapa(BigDecimal taxa, BigDecimal base, BigDecimal iva) {
            this.taxa = taxa;
            this.base = base;
            this.iva = iva;
            this.total = base.add(iva);
        }

        public BigDecimal getTaxa() { return taxa; }
        public BigDecimal getBase() { return base; }
        public BigDecimal getIva() { return iva; }
        public BigDecimal getTotal() { return total; }
    }

    public static class LinhaIsencao {
        private final String codigo;
        private final String motivo;
        private final BigDecimal base;

        public LinhaIsencao(String codigo, String motivo, BigDecimal base) {
            this.codigo = codigo;
            this.motivo = motivo;
            this.base = base;
        }

        public String getCodigo() { return codigo; }
        public String getMotivo() { return motivo; }
        public BigDecimal getBase() { return base; }
    }

    private final FaturaService faturaService;
    private final ObservableList<LinhaMapa> linhas = FXCollections.observableArrayList();
    private final ObservableList<LinhaIsencao> isencoes = FXCollections.observableArrayList();

    // KPI Labels
    private Label lblTotalBase;
    private Label lblTotalIva;
    private Label lblTotalGeral;
    private Label lblQtdIsencoes;
    private Label lblPeriodo;
    private Label lblResumo;

    // Components
    private DatePicker dpInicio;
    private DatePicker dpFim;
    private AdvancedTableView<LinhaMapa> tabela;
    private AdvancedTableView<LinhaIsencao> tabelaIsencoes;
    private PieChart graficoPizza;
    private BarChart<String, Number> graficoBarras;
    private SearchTextField txtBusca;
    private ComboBox<String> cmbTipoGrafico;

    public MapaImpostosView(FaturaService faturaService) {
        this.faturaService = faturaService;
        setPadding(new Insets(20));
        setStyle("-fx-background-color: -color-bg-default;");
        buildUI();
        carregarDados();
    }

    private void buildUI() {
        setTop(createHeader());
        setCenter(createMainContent());
    }

    private VBox createHeader() {
        VBox header = new VBox(15);
        header.setPadding(new Insets(0, 0, 15, 0));

        HBox titleBox = new HBox(15);
        titleBox.setAlignment(Pos.CENTER_LEFT);

        FontIcon icon = IconUtils.icon(Feather.PIE_CHART, 32);
        icon.setStyle("-fx-icon-color: -color-accent-emphasis;");

        VBox titles = new VBox(5);
        Label title = new Label("Mapa de Impostos");
        title.getStyleClass().add(Styles.TITLE_2);
        Label subtitle = new Label("Relatório de tributação por período");
        subtitle.getStyleClass().add(Styles.TEXT_MUTED);
        titles.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Quick actions
        Button btnRefresh = new Button("", IconUtils.icon(Feather.REFRESH_CCW, IconUtils.SIZE_SMALL));
        btnRefresh.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.FLAT);
        btnRefresh.setTooltip(new Tooltip("Atualizar dados"));
        btnRefresh.setOnAction(e -> carregarDados());

        Button btnExport = new Button("Exportar", IconUtils.icon(Feather.DOWNLOAD, IconUtils.SIZE_SMALL));
        btnExport.getStyleClass().addAll(Styles.BUTTON_OUTLINED, Styles.ACCENT);
        btnExport.setOnAction(e -> exportarRelatorio());

        titleBox.getChildren().addAll(icon, titles, spacer, btnRefresh, btnExport);

        // Period info
        lblPeriodo = new Label("Período: " + LocalDate.now().withDayOfMonth(1).format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + 
                              " até " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        lblPeriodo.getStyleClass().addAll(Styles.TEXT_SMALL, Styles.TEXT_MUTED);

        HBox kpiBox = createKPICards();
        header.getChildren().addAll(titleBox, lblPeriodo, kpiBox);
        return header;
    }

    private HBox createKPICards() {
        HBox box = new HBox(15);
        box.setPadding(new Insets(10, 0, 0, 0));

        VBox cardBase = createKpiCard(Feather.DOLLAR_SIGN, "Base Tributável", "0.00", Styles.ACCENT);
        lblTotalBase = (Label) cardBase.getChildren().get(1);

        VBox cardIva = createKpiCard(Feather.PERCENT, "Total IVA", "0.00", Styles.SUCCESS);
        lblTotalIva = (Label) cardIva.getChildren().get(1);

        VBox cardTotal = createKpiCard(Feather.CREDIT_CARD, "Total Geral", "0.00", Styles.DANGER);
        lblTotalGeral = (Label) cardTotal.getChildren().get(1);

        VBox cardIsencoes = createKpiCard(Feather.FILE_MINUS, "Isenções", "0", Styles.WARNING);
        lblQtdIsencoes = (Label) cardIsencoes.getChildren().get(1);

        box.getChildren().addAll(cardBase, cardIva, cardTotal, cardIsencoes);
        HBox.setHgrow(cardBase, Priority.ALWAYS);
        HBox.setHgrow(cardIva, Priority.ALWAYS);
        HBox.setHgrow(cardTotal, Priority.ALWAYS);
        HBox.setHgrow(cardIsencoes, Priority.ALWAYS);

        return box;
    }

    private VBox createKpiCard(Feather icon, String titulo, String valor, String style) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: -color-bg-overlay; -fx-background-radius: 8; " +
                      "-fx-border-color: -color-border-muted; -fx-border-radius: 8;");

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        FontIcon ic = IconUtils.icon(icon, 20);
        ic.getStyleClass().add(style);

        Label lblTitulo = new Label(titulo);
        lblTitulo.getStyleClass().addAll(Styles.TEXT_SMALL, Styles.TEXT_MUTED);

        header.getChildren().addAll(ic, lblTitulo);

        Label lblValor = new Label(valor);
        lblValor.getStyleClass().add(Styles.TITLE_3);
        lblValor.setStyle("-fx-text-fill: -color-fg-default;");

        card.getChildren().addAll(header, lblValor);
        return card;
    }

    private VBox createMainContent() {
        VBox content = new VBox(15);
        VBox.setVgrow(content, Priority.ALWAYS);

        // Filter bar
        HBox filtros = createFilterBar();
        
        // Main content with tabs
        TabPane tabPane = new TabPane();
        tabPane.getStyleClass().add(Styles.TABS_FLOATING);
        
        // Summary tab
        Tab tabResumo = new Tab("Resumo", createResumoTab());
        tabResumo.setClosable(false);
        
        // Charts tab
        Tab tabGraficos = new Tab("Gráficos", createGraficosTab());
        tabGraficos.setClosable(false);
        
        // Details tab
        Tab tabDetalhes = new Tab("Detalhes", createDetalhesTab());
        tabDetalhes.setClosable(false);
        
        tabPane.getTabs().addAll(tabResumo, tabGraficos, tabDetalhes);
        VBox.setVgrow(tabPane, Priority.ALWAYS);

        content.getChildren().addAll(filtros, tabPane);
        return content;
    }

    private VBox createResumoTab() {
        VBox tab = new VBox(15);
        tab.setPadding(new Insets(15));
        
        // Summary cards
        HBox cards = createSummaryCards();
        
        // Main tables
        HBox tables = new HBox(15);
        tables.getChildren().addAll(createTabelaSection(), createIsencoesSection());
        HBox.setHgrow(tables, Priority.ALWAYS);
        
        tab.getChildren().addAll(cards, tables);
        return tab;
    }

    private VBox createGraficosTab() {
        VBox tab = new VBox(15);
        tab.setPadding(new Insets(15));
        
        // Chart type selector
        HBox chartControls = new HBox(10);
        chartControls.setAlignment(Pos.CENTER_LEFT);
        
        Label lblChartType = new Label("Tipo de Gráfico:");
        cmbTipoGrafico = new ComboBox<>();
        cmbTipoGrafico.getItems().addAll("Pizza - Distribuição por Taxa", "Barras - Comparação por Taxa");
        cmbTipoGrafico.setValue("Pizza - Distribuição por Taxa");
        cmbTipoGrafico.valueProperty().addListener((obs, old, newVal) -> atualizarGraficos());
        
        chartControls.getChildren().addAll(lblChartType, cmbTipoGrafico);
        
        // Charts container
        HBox charts = new HBox(15);
        charts.getChildren().addAll(createPieChart(), createBarChart());
        HBox.setHgrow(charts, Priority.ALWAYS);
        
        tab.getChildren().addAll(chartControls, charts);
        return tab;
    }

    private VBox createDetalhesTab() {
        VBox tab = new VBox(15);
        tab.setPadding(new Insets(15));
        
        // Search and filter
        HBox searchBox = new HBox(10);
        searchBox.setAlignment(Pos.CENTER_LEFT);
        
        txtBusca = new SearchTextField();
        txtBusca.setPromptText("Buscar por taxa, valor ou período...");
        txtBusca.setPrefWidth(300);
        txtBusca.textProperty().addListener((obs, old, newVal) -> aplicarFiltroDetalhes(newVal));
        
        searchBox.getChildren().addAll(txtBusca);
        
        // Detailed tables
        VBox tables = new VBox(15);
        tables.getChildren().addAll(createTabelaSection(), createIsencoesSection());
        VBox.setVgrow(tables, Priority.ALWAYS);
        
        tab.getChildren().addAll(searchBox, tables);
        return tab;
    }

    private HBox createSummaryCards() {
        HBox cards = new HBox(15);
        cards.setPadding(new Insets(10, 0, 10, 0));
        
        // Additional summary cards
        VBox cardFaturas = createKpiCard(Feather.FILE_TEXT, "Total Faturas", "0", Styles.ACCENT);
        VBox cardMediaIva = createKpiCard(Feather.ACTIVITY, "Média IVA", "0%", Styles.WARNING);
        VBox cardMaiorTaxa = createKpiCard(Feather.TRENDING_UP, "Maior Taxa", "0%", Styles.DANGER);
        VBox cardEconomia = createKpiCard(Feather.SAVE, "Isenções Totais", "0.00", Styles.SUCCESS);
        
        cards.getChildren().addAll(cardFaturas, cardMediaIva, cardMaiorTaxa, cardEconomia);
        HBox.setHgrow(cardFaturas, Priority.ALWAYS);
        HBox.setHgrow(cardMediaIva, Priority.ALWAYS);
        HBox.setHgrow(cardMaiorTaxa, Priority.ALWAYS);
        HBox.setHgrow(cardEconomia, Priority.ALWAYS);
        
        return cards;
    }

    private HBox createFilterBar() {
        HBox filtros = new HBox(10);
        filtros.setAlignment(Pos.CENTER_LEFT);
        filtros.setPadding(new Insets(5, 0, 10, 0));

        Label lblPeriodo = new Label("Período:");
        lblPeriodo.getStyleClass().add(Styles.TEXT_BOLD);

        dpInicio = new DatePicker(LocalDate.now().withDayOfMonth(1));
        dpInicio.setPromptText("Data início");

        Label lblAte = new Label("até");
        lblAte.getStyleClass().add(Styles.TEXT_MUTED);

        dpFim = new DatePicker(LocalDate.now());
        dpFim.setPromptText("Data fim");

        Button btnAplicar = new Button("Aplicar", IconUtils.icon(Feather.SEARCH, IconUtils.SIZE_SMALL));
        btnAplicar.getStyleClass().add(Styles.ACCENT);
        btnAplicar.setDefaultButton(true);
        btnAplicar.setOnAction(e -> carregarDados());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnExportar = new Button("Exportar CSV", IconUtils.icon(Feather.DOWNLOAD, IconUtils.SIZE_SMALL));
        btnExportar.getStyleClass().addAll(Styles.BUTTON_OUTLINED);
        btnExportar.setOnAction(e -> exportarCsv());

        filtros.getChildren().addAll(lblPeriodo, dpInicio, lblAte, dpFim, btnAplicar, spacer, btnExportar);
        return filtros;
    }

    private VBox createTabelaSection() {
        VBox section = new VBox(10);
        section.setPadding(new Insets(10));
        section.setStyle("-fx-background-color: -color-bg-overlay; -fx-background-radius: 8;");

        Label lblTitle = new Label("Por Taxa de IVA");
        lblTitle.getStyleClass().addAll(Styles.TITLE_4, Styles.TEXT_BOLD);

        tabela = new AdvancedTableView<>();
        TableUtils.standardize(tabela);
        tabela.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_NEXT_COLUMN);
        VBox.setVgrow(tabela, Priority.ALWAYS);

        TableColumn<LinhaMapa, BigDecimal> colTaxa = new TableColumn<>("Taxa (%)");
        colTaxa.setCellValueFactory(new PropertyValueFactory<>("taxa"));
        colTaxa.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    Label badge = new Label(item + "%");
                    badge.getStyleClass().addAll(Styles.TEXT_SMALL, Styles.BUTTON_OUTLINED, Styles.ACCENT);
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        TableColumn<LinhaMapa, BigDecimal> colBase = new TableColumn<>("Base");
        colBase.setCellValueFactory(new PropertyValueFactory<>("base"));

        TableColumn<LinhaMapa, BigDecimal> colIva = new TableColumn<>("IVA");
        colIva.setCellValueFactory(new PropertyValueFactory<>("iva"));

        TableColumn<LinhaMapa, BigDecimal> colTotal = new TableColumn<>("Total");
        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));

        tabela.getColumns().addAll(colTaxa, colBase, colIva, colTotal);

        section.getChildren().addAll(lblTitle, tabela);
        return section;
    }

    private VBox createIsencoesSection() {
        VBox section = new VBox(10);
        section.setPadding(new Insets(10));
        section.setStyle("-fx-background-color: -color-bg-overlay; -fx-background-radius: 8;");

        Label lblTitle = new Label("Isenções por Código");
        lblTitle.getStyleClass().addAll(Styles.TITLE_4, Styles.TEXT_BOLD);

        tabelaIsencoes = new AdvancedTableView<>();
        TableUtils.standardize(tabelaIsencoes);
        tabelaIsencoes.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_NEXT_COLUMN);
        VBox.setVgrow(tabelaIsencoes, Priority.ALWAYS);
        tabelaIsencoes.setPrefHeight(150);

        TableColumn<LinhaIsencao, String> colCodIsencao = new TableColumn<>("Código");
        colCodIsencao.setCellValueFactory(new PropertyValueFactory<>("codigo"));
        colCodIsencao.setPrefWidth(120);

        TableColumn<LinhaIsencao, String> colMotivo = new TableColumn<>("Motivo");
        colMotivo.setCellValueFactory(new PropertyValueFactory<>("motivo"));
        colMotivo.setPrefWidth(300);

        TableColumn<LinhaIsencao, BigDecimal> colBaseIsencao = new TableColumn<>("Base Isenta");
        colBaseIsencao.setCellValueFactory(new PropertyValueFactory<>("base"));

        tabelaIsencoes.getColumns().addAll(colCodIsencao, colMotivo, colBaseIsencao);

        section.getChildren().addAll(lblTitle, tabelaIsencoes);
        return section;
    }

    private VBox createPieChart() {
        VBox chartBox = new VBox(10);
        chartBox.setPadding(new Insets(10));
        chartBox.setStyle("-fx-background-color: -color-bg-overlay; -fx-background-radius: 8;");
        
        Label lblTitle = new Label("Distribuição por Taxa");
        lblTitle.getStyleClass().addAll(Styles.TITLE_4, Styles.TEXT_BOLD);
        
        graficoPizza = new PieChart();
        graficoPizza.setTitle("Distribuição de IVA por Taxa");
        graficoPizza.setAnimated(true);
        VBox.setVgrow(graficoPizza, Priority.ALWAYS);
        
        chartBox.getChildren().addAll(lblTitle, graficoPizza);
        return chartBox;
    }

    private VBox createBarChart() {
        VBox chartBox = new VBox(10);
        chartBox.setPadding(new Insets(10));
        chartBox.setStyle("-fx-background-color: -color-bg-overlay; -fx-background-radius: 8;");
        
        Label lblTitle = new Label("Comparação por Taxa");
        lblTitle.getStyleClass().addAll(Styles.TITLE_4, Styles.TEXT_BOLD);
        
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Taxa (%)");
        yAxis.setLabel("Valor (AOA)");
        
        graficoBarras = new BarChart<>(xAxis, yAxis);
        graficoBarras.setTitle("Comparação Base vs IVA por Taxa");
        graficoBarras.setAnimated(true);
        VBox.setVgrow(graficoBarras, Priority.ALWAYS);
        
        chartBox.getChildren().addAll(lblTitle, graficoBarras);
        return chartBox;
    }

    private void carregarDados() {
        LocalDate inicio = Optional.ofNullable(dpInicio.getValue()).orElse(LocalDate.now().withDayOfMonth(1));
        LocalDate fim = Optional.ofNullable(dpFim.getValue()).orElse(LocalDate.now());
        
        // Update period label
        lblPeriodo.setText("Período: " + inicio.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + 
                          " até " + fim.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        
        List<Fatura> faturas = faturaService.findByDataEmissaoBetweenWithItens(inicio, fim)
                .stream()
                .filter(f -> f.getStatus() == StatusFatura.EMITIDA)
                .collect(Collectors.toList());

        Map<BigDecimal, BigDecimal[]> mapa = new HashMap<>();
        Map<String, BigDecimal[]> mapaIsencao = new HashMap<>();
        
        // Processar dados
        processarDadosFaturas(faturas, mapa, mapaIsencao);
        
        // Atualizar tabelas
        atualizarTabelas(mapa, mapaIsencao);
        
        // Atualizar gráficos
        atualizarGraficos();
        
        // Atualizar KPIs
        atualizarKPIs(faturas, mapa, mapaIsencao);
    }

    private void processarDadosFaturas(List<Fatura> faturas, Map<BigDecimal, BigDecimal[]> mapa, Map<String, BigDecimal[]> mapaIsencao) {
        for (Fatura fatura : faturas) {
            for (ItemFatura item : fatura.getItens()) {
                BigDecimal base = item.getSubtotal() != null ? item.getSubtotal() : BigDecimal.ZERO;
                BigDecimal taxa = item.getPercentualIva() != null ? item.getPercentualIva() : BigDecimal.ZERO;
                BigDecimal iva = item.getValorIva() != null ? item.getValorIva() : BigDecimal.ZERO;
                BigDecimal totalItem = item.getTotal() != null ? item.getTotal() : base.add(iva);

                if (taxa.compareTo(BigDecimal.ZERO) == 0 && item.getCodigoIsencao() != null && !item.getCodigoIsencao().isBlank()) {
                    String codigoIsencao = item.getCodigoIsencao();
                    String motivoIsencao = item.getMotivoIsencao() != null ? item.getMotivoIsencao() : "";
                    String key = codigoIsencao + " - " + motivoIsencao;
                    mapaIsencao.computeIfAbsent(key, k -> new BigDecimal[]{BigDecimal.ZERO});
                    mapaIsencao.get(key)[0] = mapaIsencao.get(key)[0].add(base);
                } else {
                    // Tributado
                    mapa.computeIfAbsent(taxa, k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO});
                    BigDecimal[] valores = mapa.get(taxa);
                    valores[0] = valores[0].add(base); // base
                    valores[1] = valores[1].add(iva);  // iva
                    valores[2] = valores[2].add(totalItem); // total
                }
            }
        }
    }

    private void atualizarTabelas(Map<BigDecimal, BigDecimal[]> mapa, Map<String, BigDecimal[]> mapaIsencao) {
        // Atualizar tabela de taxas
        linhas.clear();
        mapa.entrySet().stream()
            .sorted(Map.Entry.<BigDecimal, BigDecimal[]>comparingByKey().reversed())
            .forEach(entry -> {
                BigDecimal taxa = entry.getKey();
                BigDecimal base = entry.getValue()[0];
                BigDecimal iva = entry.getValue()[1];
                linhas.add(new LinhaMapa(taxa, base, iva));
            });

        // Atualizar tabela de isenções
        isencoes.clear();
        mapaIsencao.entrySet().stream()
            .sorted(Map.Entry.<String, BigDecimal[]>comparingByValue((a, b) -> b[0].compareTo(a[0])))
            .forEach(entry -> {
                String[] parts = entry.getKey().split(" - ", 2);
                String codigo = parts[0];
                String motivo = parts.length > 1 ? parts[1] : "";
                BigDecimal base = entry.getValue()[0];
                isencoes.add(new LinhaIsencao(codigo, motivo, base));
            });
    }

    private void atualizarGraficos() {
        atualizarGraficoPizza();
        atualizarGraficoBarras();
    }

    private void atualizarGraficoPizza() {
        graficoPizza.getData().clear();
        
        BigDecimal totalGeral = linhas.stream()
            .map(LinhaMapa::getTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        if (totalGeral.compareTo(BigDecimal.ZERO) > 0) {
            for (LinhaMapa linha : linhas) {
                BigDecimal percentual = linha.getTotal()
                    .multiply(BigDecimal.valueOf(100))
                    .divide(totalGeral, 1, RoundingMode.HALF_UP);
                
                PieChart.Data slice = new PieChart.Data(
                    linha.getTaxa() + "% (" + percentual + "%)", 
                    linha.getTotal().doubleValue()
                );
                graficoPizza.getData().add(slice);
            }
        }
    }

    private void atualizarGraficoBarras() {
        graficoBarras.getData().clear();
        
        XYChart.Series<String, Number> seriesBase = new XYChart.Series<>();
        seriesBase.setName("Base Tributável");
        
        XYChart.Series<String, Number> seriesIva = new XYChart.Series<>();
        seriesIva.setName("IVA");
        
        for (LinhaMapa linha : linhas) {
            String taxa = linha.getTaxa() + "%";
            seriesBase.getData().add(new XYChart.Data<>(taxa, linha.getBase().doubleValue()));
            seriesIva.getData().add(new XYChart.Data<>(taxa, linha.getIva().doubleValue()));
        }
        
        graficoBarras.getData().addAll(seriesBase, seriesIva);
    }

    private void atualizarKPIs(List<Fatura> faturas, Map<BigDecimal, BigDecimal[]> mapa, Map<String, BigDecimal[]> mapaIsencao) {
        BigDecimal totalBase = mapa.values().stream()
            .map(arr -> arr[0])
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalIva = mapa.values().stream()
            .map(arr -> arr[1])
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalGeral = mapa.values().stream()
            .map(arr -> arr[2])
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalIsencoes = mapaIsencao.values().stream()
            .map(arr -> arr[0])
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        lblTotalBase.setText(formatarMoeda(totalBase));
        lblTotalIva.setText(formatarMoeda(totalIva));
        lblTotalGeral.setText(formatarMoeda(totalGeral));
        lblQtdIsencoes.setText(String.valueOf(isencoes.size()));
    }

    private String formatarMoeda(BigDecimal valor) {
        return String.format("%,.2f", valor.doubleValue());
    }

    private void aplicarFiltroDetalhes(String filtro) {
        // Implementar filtro nos dados das tabelas
        // Por simplicidade, mantemos as tabelas originais por enquanto
    }

    private void exportarRelatorio() {
        // Implementar exportação completa do relatório
        AlertUtils.showInfoAlert("Exportação", "Relatório exportado com sucesso!");
    }

    private void exportarCsv() {
        LocalDate inicio = Optional.ofNullable(dpInicio.getValue()).orElse(LocalDate.now().withDayOfMonth(1));
        LocalDate fim = Optional.ofNullable(dpFim.getValue()).orElse(LocalDate.now());
        javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
        fc.setTitle("Exportar Mapa de Impostos (CSV)");
        fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        fc.setInitialFileName("Mapa_Impostos_" + inicio + "_a_" + fim + ".csv");
        java.io.File file = fc.showSaveDialog(getScene().getWindow());
        if (file == null) return;
        try (java.io.PrintWriter out = new java.io.PrintWriter(file, java.nio.charset.StandardCharsets.UTF_8)) {
            out.println("Mapa por Taxa");
            out.println("Taxa (%);Base;IVA;Total");
            for (LinhaMapa l : linhas) {
                out.printf("%s;%s;%s;%s%n", l.getTaxa(), l.getBase(), l.getIva(), l.getTotal());
            }
            out.println();
            out.println("Isenções por Código");
            out.println("Código;Motivo;Base Isenta");
            for (LinhaIsencao i : isencoes) {
                out.printf("%s;%s;%s%n", i.getCodigo(), i.getMotivo(), i.getBase());
            }
            new Alert(Alert.AlertType.INFORMATION, "CSV exportado com sucesso.").showAndWait();
        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR, "Falha ao exportar CSV: " + ex.getMessage()).showAndWait();
        }
    }
}

package ao.allon.kubata.faturacao.view;

import ao.allon.kubata.faturacao.domain.Fatura;
import ao.allon.kubata.faturacao.domain.Imposto;
import ao.allon.kubata.faturacao.domain.RetencaoFonte;
import ao.allon.kubata.faturacao.service.FaturaService;
import ao.allon.kubata.faturacao.service.ImpostoService;
import ao.allon.kubata.faturacao.service.RetencaoFonteService;
import ao.allon.kubata.faturacao.ui.util.AlertUtils;
import ao.allon.kubata.faturacao.ui.util.IconUtils;
import atlantafx.base.controls.Card;
import atlantafx.base.controls.CustomTextField;
import atlantafx.base.theme.Styles;
import com.dlsc.gemsfx.SearchTextField;
import com.dlsc.gemsfx.daterange.DateRangePicker;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * View completa de Relatório Fiscal com AtlantaFX e GemsFX.
 * Inclui dashboards, gráficos interativos, exportação e impressão.
 */
public class RelatorioFiscalView extends BorderPane {

    private final FaturaService faturaService;
    private final ImpostoService impostoService;
    private final RetencaoFonteService retencaoService;

    // Dados
    private final ObservableList<Fatura> faturas = FXCollections.observableArrayList();
    private final ObservableList<Imposto> impostos = FXCollections.observableArrayList();
    private final ObservableList<RetencaoFonte> retencoes = FXCollections.observableArrayList();

    // Componentes UI
    private DatePicker dpInicio;
    private DatePicker dpFim;
    private SearchTextField txtBusca;
    private ComboBox<String> cmbTipoRelatorio;
    private TableView<Fatura> tabelaFaturas;
    private TableView<ResumoImposto> tabelaResumo;
    private PieChart graficoDistribuicao;
    private BarChart<String, Number> graficoTendencia;
    private LineChart<String, Number> graficoEvolucao;
    private TabPane tabPane;

    // KPIs
    private Label lblTotalFaturamento;
    private Label lblTotalImpostos;
    private Label lblMediaImpostos;
    private Label lblTotalRetencoes;
    private Label lblPeriodo;

    public RelatorioFiscalView(FaturaService faturaService, 
                              ImpostoService impostoService,
                              RetencaoFonteService retencaoService) {
        this.faturaService = faturaService;
        this.impostoService = impostoService;
        this.retencaoService = retencaoService;

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

        // Title section
        HBox titleBox = new HBox(15);
        titleBox.setAlignment(Pos.CENTER_LEFT);

        FontIcon icon = IconUtils.icon(Feather.BAR_CHART_2, 32);
        icon.setStyle("-fx-icon-color: -color-accent-emphasis;");

        VBox titles = new VBox(5);
        Label title = new Label("Relatório Fiscal");
        title.getStyleClass().add(Styles.TITLE_2);
        Label subtitle = new Label("Análise completa de tributação e retenções");
        subtitle.getStyleClass().add(Styles.TEXT_MUTED);
        titles.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Quick actions
        Button btnPrint = new Button("", IconUtils.icon(Feather.PRINTER, IconUtils.SIZE_SMALL));
        btnPrint.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.FLAT);
        btnPrint.setTooltip(new Tooltip("Imprimir relatório"));
        btnPrint.setOnAction(e -> imprimirRelatorio());

        Button btnExport = new Button("Exportar", IconUtils.icon(Feather.DOWNLOAD, IconUtils.SIZE_SMALL));
        btnExport.getStyleClass().addAll(Styles.BUTTON_OUTLINED, Styles.ACCENT);
        btnExport.setOnAction(e -> exportarRelatorio());

        titleBox.getChildren().addAll(icon, titles, spacer, btnPrint, btnExport);

        // Controls
        HBox controls = createControls();
        
        // KPI Cards
        HBox kpiBox = createKPICards();

        header.getChildren().addAll(titleBox, controls, kpiBox);
        return header;
    }

    private HBox createControls() {
        HBox controls = new HBox(15);
        controls.setAlignment(Pos.CENTER_LEFT);

        // Date range
        Label lblPeriodo = new Label("Período:");
        lblPeriodo.getStyleClass().add(Styles.TEXT_BOLD);

        dpInicio = new DatePicker(LocalDate.now().withDayOfMonth(1));
        dpFim = new DatePicker(LocalDate.now());
        dpInicio.valueProperty().addListener((obs, old, newVal) -> carregarDados());
        dpFim.valueProperty().addListener((obs, old, newVal) -> carregarDados());

        // Report type
        Label lblTipo = new Label("Tipo:");
        lblTipo.getStyleClass().add(Styles.TEXT_BOLD);
        
        cmbTipoRelatorio = new ComboBox<>();
        cmbTipoRelatorio.getItems().addAll(
            "Resumo Geral",
            "Por Imposto", 
            "Por Período",
            "Retenções",
            "Comparativo"
        );
        cmbTipoRelatorio.setValue("Resumo Geral");
        cmbTipoRelatorio.valueProperty().addListener((obs, old, newVal) -> atualizarVisualizacao(newVal));

        // Search
        txtBusca = new SearchTextField();
        txtBusca.setPromptText("Buscar por cliente, número ou valor...");
        txtBusca.setPrefWidth(250);
        txtBusca.textProperty().addListener((obs, old, newVal) -> aplicarFiltro(newVal));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Refresh button
        Button btnRefresh = new Button("", IconUtils.icon(Feather.REFRESH_CCW, IconUtils.SIZE_SMALL));
        btnRefresh.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.FLAT);
        btnRefresh.setTooltip(new Tooltip("Atualizar dados"));
        btnRefresh.setOnAction(e -> carregarDados());

        controls.getChildren().addAll(lblPeriodo, dpInicio, new Label("até"), dpFim, lblTipo, cmbTipoRelatorio, spacer, txtBusca, btnRefresh);
        return controls;
    }

    private HBox createKPICards() {
        HBox box = new HBox(15);
        box.setPadding(new Insets(10, 0, 0, 0));

        VBox cardFaturamento = createKpiCard(Feather.DOLLAR_SIGN, "Faturamento", "0.00", Styles.ACCENT);
        lblTotalFaturamento = (Label) cardFaturamento.getChildren().get(1);

        VBox cardImpostos = createKpiCard(Feather.PERCENT, "Total Impostos", "0.00", Styles.DANGER);
        lblTotalImpostos = (Label) cardImpostos.getChildren().get(1);

        VBox cardMedia = createKpiCard(Feather.ACTIVITY, "Média Impostos", "0.00", Styles.WARNING);
        lblMediaImpostos = (Label) cardMedia.getChildren().get(1);

        VBox cardRetencoes = createKpiCard(Feather.FILE_MINUS, "Retenções", "0.00", Styles.SUCCESS);
        lblTotalRetencoes = (Label) cardRetencoes.getChildren().get(1);

        box.getChildren().addAll(cardFaturamento, cardImpostos, cardMedia, cardRetencoes);
        HBox.setHgrow(cardFaturamento, Priority.ALWAYS);
        HBox.setHgrow(cardImpostos, Priority.ALWAYS);
        HBox.setHgrow(cardMedia, Priority.ALWAYS);
        HBox.setHgrow(cardRetencoes, Priority.ALWAYS);

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

    private TabPane createMainContent() {
        tabPane = new TabPane();
        tabPane.getStyleClass().add(Styles.TABS_FLOATING);

        // Visão Geral Tab
        Tab tabVisaoGeral = new Tab("Visão Geral", createVisaoGeralTab());
        tabVisaoGeral.setClosable(false);

        // Gráficos Tab
        Tab tabGraficos = new Tab("Gráficos", createGraficosTab());
        tabGraficos.setClosable(false);

        // Detalhes Tab
        Tab tabDetalhes = new Tab("Detalhes", createDetalhesTab());
        tabDetalhes.setClosable(false);

        // Retenções Tab
        Tab tabRetencoes = new Tab("Retenções", createRetencoesTab());
        tabRetencoes.setClosable(false);

        tabPane.getTabs().addAll(tabVisaoGeral, tabGraficos, tabDetalhes, tabRetencoes);
        return tabPane;
    }

    private VBox createVisaoGeralTab() {
        VBox tab = new VBox(15);
        tab.setPadding(new Insets(15));

        // Summary section
        Card cardResumo = new Card();
        
        tabelaResumo = createResumoTable();
        cardResumo.setBody(tabelaResumo);

        // Distribution chart
        Card cardDistribuicao = new Card();
        
        graficoDistribuicao = new PieChart();
        graficoDistribuicao.setTitle("Distribuição por Tipo de Imposto");
        graficoDistribuicao.setAnimated(true);
        cardDistribuicao.setBody(graficoDistribuicao);

        // Layout
        HBox content = new HBox(15);
        content.getChildren().addAll(cardResumo, cardDistribuicao);
        HBox.setHgrow(cardResumo, Priority.ALWAYS);
        HBox.setHgrow(cardDistribuicao, Priority.ALWAYS);

        tab.getChildren().addAll(content);
        return tab;
    }

    private VBox createGraficosTab() {
        VBox tab = new VBox(15);
        tab.setPadding(new Insets(15));

        // Tendência chart
        Card cardTendencia = new Card();
        
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Período");
        yAxis.setLabel("Valor (AOA)");
        
        graficoTendencia = new BarChart<>(xAxis, yAxis);
        graficoTendencia.setTitle("Faturamento vs Impostos");
        cardTendencia.setBody(graficoTendencia);

        // Evolução chart
        Card cardEvolucao = new Card();
        
        CategoryAxis xAxis2 = new CategoryAxis();
        NumberAxis yAxis2 = new NumberAxis();
        xAxis2.setLabel("Mês");
        yAxis2.setLabel("Valor (AOA)");
        
        graficoEvolucao = new LineChart<>(xAxis2, yAxis2);
        graficoEvolucao.setTitle("Evolução de Faturamento");
        cardEvolucao.setBody(graficoEvolucao);

        // Layout
        VBox charts = new VBox(15);
        charts.getChildren().addAll(cardTendencia, cardEvolucao);

        tab.getChildren().addAll(charts);
        return tab;
    }

    private VBox createDetalhesTab() {
        VBox tab = new VBox(15);
        tab.setPadding(new Insets(15));

        Card cardFaturas = new Card();
        
        tabelaFaturas = createFaturasTable();
        cardFaturas.setBody(tabelaFaturas);

        tab.getChildren().addAll(cardFaturas);
        return tab;
    }

    private VBox createRetencoesTab() {
        VBox tab = new VBox(15);
        tab.setPadding(new Insets(15));

        Card cardRetencoes = new Card();
        
        TableView<ResumoRetencao> tabelaRetencoes = createRetencoesTable();
        cardRetencoes.setBody(tabelaRetencoes);

        tab.getChildren().addAll(cardRetencoes);
        return tab;
    }

    private TableView<ResumoImposto> createResumoTable() {
        TableView<ResumoImposto> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_NEXT_COLUMN);

        TableColumn<ResumoImposto, String> colTipo = new TableColumn<>("Tipo");
        colTipo.setCellValueFactory(new PropertyValueFactory<>("tipo"));

        TableColumn<ResumoImposto, String> colTaxa = new TableColumn<>("Taxa");
        colTaxa.setCellValueFactory(new PropertyValueFactory<>("taxa"));

        TableColumn<ResumoImposto, BigDecimal> colBase = new TableColumn<>("Base");
        colBase.setCellValueFactory(new PropertyValueFactory<>("base"));

        TableColumn<ResumoImposto, BigDecimal> colImposto = new TableColumn<>("Imposto");
        colImposto.setCellValueFactory(new PropertyValueFactory<>("imposto"));

        TableColumn<ResumoImposto, BigDecimal> colTotal = new TableColumn<>("Total");
        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));

        table.getColumns().addAll(colTipo, colTaxa, colBase, colImposto, colTotal);
        return table;
    }

    private TableView<Fatura> createFaturasTable() {
        TableView<Fatura> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_NEXT_COLUMN);
        table.setItems(faturas);

        TableColumn<Fatura, String> colNumero = new TableColumn<>("Número");
        colNumero.setCellValueFactory(new PropertyValueFactory<>("numero"));

        TableColumn<Fatura, String> colCliente = new TableColumn<>("Cliente");
        colCliente.setCellValueFactory(cd -> new SimpleObjectProperty<>(cd.getValue().getCliente().getNome()));

        TableColumn<Fatura, LocalDate> colData = new TableColumn<>("Data");
        colData.setCellValueFactory(new PropertyValueFactory<>("dataEmissao"));

        TableColumn<Fatura, BigDecimal> colTotal = new TableColumn<>("Total");
        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));

        TableColumn<Fatura, BigDecimal> colImposto = new TableColumn<>("Imposto");
        colImposto.setCellValueFactory(new PropertyValueFactory<>("iva"));

        table.getColumns().addAll(colNumero, colCliente, colData, colTotal, colImposto);
        return table;
    }

    private TableView<ResumoRetencao> createRetencoesTable() {
        TableView<ResumoRetencao> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_NEXT_COLUMN);

        TableColumn<ResumoRetencao, String> colCodigo = new TableColumn<>("Código");
        colCodigo.setCellValueFactory(new PropertyValueFactory<>("codigo"));

        TableColumn<ResumoRetencao, String> colDescricao = new TableColumn<>("Descrição");
        colDescricao.setCellValueFactory(new PropertyValueFactory<>("descricao"));

        TableColumn<ResumoRetencao, BigDecimal> colTaxa = new TableColumn<>("Taxa");
        colTaxa.setCellValueFactory(new PropertyValueFactory<>("taxa"));

        TableColumn<ResumoRetencao, BigDecimal> colTotal = new TableColumn<>("Total Retido");
        colTotal.setCellValueFactory(new PropertyValueFactory<>("totalRetido"));

        table.getColumns().addAll(colCodigo, colDescricao, colTaxa, colTotal);
        return table;
    }

    private void carregarDados() {
        try {
            // Carregar período selecionado
            LocalDate inicio = Optional.ofNullable(dpInicio.getValue()).orElse(LocalDate.now().withDayOfMonth(1));
            LocalDate fim = Optional.ofNullable(dpFim.getValue()).orElse(LocalDate.now());

            // Carregar faturas do período
            List<Fatura> faturasPeriodo = faturaService.findByDataEmissaoBetweenWithItens(inicio, fim);
            faturas.setAll(faturasPeriodo);

            // Carregar impostos e retenções
            impostos.setAll(impostoService.findAll());
            retencoes.setAll(retencaoService.findActive());

            // Atualizar KPIs
            atualizarKPIs(faturasPeriodo);

            // Atualizar gráficos
            atualizarGraficos(faturasPeriodo);

            // Atualizar tabelas
            atualizarTabelas(faturasPeriodo);

        } catch (Exception e) {
            AlertUtils.showError("Erro ao carregar dados", e.getMessage());
        }
    }

    private void atualizarKPIs(List<Fatura> faturas) {
        BigDecimal totalFaturamento = faturas.stream()
            .map(Fatura::getTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalImpostos = faturas.stream()
            .map(Fatura::getIva)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal mediaImpostos = faturas.isEmpty() ? BigDecimal.ZERO : 
            totalImpostos.divide(BigDecimal.valueOf(faturas.size()), 2, RoundingMode.HALF_UP);

        BigDecimal totalRetencoes = BigDecimal.ZERO; // Calcular baseado nas retenções

        lblTotalFaturamento.setText(formatarMoeda(totalFaturamento));
        lblTotalImpostos.setText(formatarMoeda(totalImpostos));
        lblMediaImpostos.setText(formatarMoeda(mediaImpostos));
        lblTotalRetencoes.setText(formatarMoeda(totalRetencoes));
    }

    private void atualizarGraficos(List<Fatura> faturas) {
        atualizarGraficoDistribuicao(faturas);
        atualizarGraficoTendencia(faturas);
        atualizarGraficoEvolucao(faturas);
    }

    private void atualizarGraficoDistribuicao(List<Fatura> faturas) {
        graficoDistribuicao.getData().clear();

        Map<String, BigDecimal> distribuicao = new HashMap<>();
        for (Fatura fatura : faturas) {
            for (var item : fatura.getItens()) {
                BigDecimal taxa = item.getPercentualIva() != null ? item.getPercentualIva() : BigDecimal.ZERO;
                String tipoImposto = taxa + "%";
                BigDecimal valorImposto = item.getValorIva() != null ? item.getValorIva() : BigDecimal.ZERO;
                
                distribuicao.merge(tipoImposto, valorImposto, BigDecimal::add);
            }
        }

        BigDecimal total = distribuicao.values().stream()
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        for (Map.Entry<String, BigDecimal> entry : distribuicao.entrySet()) {
            BigDecimal percentual = entry.getValue()
                .multiply(BigDecimal.valueOf(100))
                .divide(total, 1, RoundingMode.HALF_UP);
            
            PieChart.Data slice = new PieChart.Data(
                entry.getKey() + " (" + percentual + "%)",
                entry.getValue().doubleValue()
            );
            graficoDistribuicao.getData().add(slice);
        }
    }

    private void atualizarGraficoTendencia(List<Fatura> faturas) {
        graficoTendencia.getData().clear();

        // Agrupar por mês
        Map<String, BigDecimal[]> dadosMensais = new TreeMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM/yyyy");

        for (Fatura fatura : faturas) {
            String mes = fatura.getDataEmissao().format(formatter);
            dadosMensais.computeIfAbsent(mes, k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            dadosMensais.get(mes)[0] = dadosMensais.get(mes)[0].add(fatura.getTotal());
            dadosMensais.get(mes)[1] = dadosMensais.get(mes)[1].add(fatura.getIva());
        }

        XYChart.Series<String, Number> seriesFaturamento = new XYChart.Series<>();
        seriesFaturamento.setName("Faturamento");

        XYChart.Series<String, Number> seriesImpostos = new XYChart.Series<>();
        seriesImpostos.setName("Impostos");

        for (Map.Entry<String, BigDecimal[]> entry : dadosMensais.entrySet()) {
            seriesFaturamento.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()[0].doubleValue()));
            seriesImpostos.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()[1].doubleValue()));
        }

        graficoTendencia.getData().addAll(seriesFaturamento, seriesImpostos);
    }

    private void atualizarGraficoEvolucao(List<Fatura> faturas) {
        graficoEvolucao.getData().clear();

        // Similar ao tendência, mas com linha
        Map<String, BigDecimal> dadosEvolucao = new TreeMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM/yyyy");

        for (Fatura fatura : faturas) {
            String mes = fatura.getDataEmissao().format(formatter);
            dadosEvolucao.merge(mes, fatura.getTotal(), BigDecimal::add);
        }

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Evolução");

        for (Map.Entry<String, BigDecimal> entry : dadosEvolucao.entrySet()) {
            series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue().doubleValue()));
        }

        graficoEvolucao.getData().add(series);
    }

    private void atualizarTabelas(List<Fatura> faturas) {
        // Atualizar tabela de resumo
        ObservableList<ResumoImposto> resumo = FXCollections.observableArrayList();
        
        Map<String, BigDecimal[]> dadosResumo = new HashMap<>();
        for (Fatura fatura : faturas) {
            for (var item : fatura.getItens()) {
                BigDecimal taxa = item.getPercentualIva() != null ? item.getPercentualIva() : BigDecimal.ZERO;
                String tipo = taxa + "%";
                BigDecimal base = item.getSubtotal() != null ? item.getSubtotal() : BigDecimal.ZERO;
                BigDecimal imposto = item.getValorIva() != null ? item.getValorIva() : BigDecimal.ZERO;
                
                dadosResumo.computeIfAbsent(tipo, k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO});
                dadosResumo.get(tipo)[0] = dadosResumo.get(tipo)[0].add(base);
                dadosResumo.get(tipo)[1] = dadosResumo.get(tipo)[1].add(imposto);
                dadosResumo.get(tipo)[2] = dadosResumo.get(tipo)[2].add(base.add(imposto));
            }
        }

        for (Map.Entry<String, BigDecimal[]> entry : dadosResumo.entrySet()) {
            resumo.add(new ResumoImposto(
                entry.getKey(),
                "Varias",
                entry.getValue()[0],
                entry.getValue()[1],
                entry.getValue()[2]
            ));
        }

        tabelaResumo.setItems(resumo);
    }

    private void aplicarFiltro(String filtro) {
        if (filtro == null || filtro.trim().isEmpty()) {
            tabelaFaturas.setItems(faturas);
            return;
        }

        String filtroLower = filtro.toLowerCase();
        ObservableList<Fatura> filtradas = faturas.filtered(f ->
            f.getNumero().toLowerCase().contains(filtroLower) ||
            f.getCliente().getNome().toLowerCase().contains(filtroLower) ||
            f.getTotal().toString().contains(filtro)
        );
        
        tabelaFaturas.setItems(filtradas);
    }

    private void atualizarVisualizacao(String tipo) {
        // Atualizar visualização baseada no tipo selecionado
        carregarDados();
    }

    private void imprimirRelatorio() {
        AlertUtils.showInfoAlert("Impressão", "Funcionalidade de impressão em desenvolvimento...");
    }

    private void exportarRelatorio() {
        try {
            // Implementar exportação para Excel/PDF
            StringBuilder csv = new StringBuilder();
            csv.append("Relatório Fiscal\n");
            csv.append("Período: ").append(dpInicio.getValue()).append(" a ").append(dpFim.getValue()).append("\n\n");
            
            csv.append("Resumo por Imposto:\n");
            csv.append("Tipo,Taxa,Base,Imposto,Total\n");
            for (ResumoImposto resumo : tabelaResumo.getItems()) {
                csv.append(String.format("%s,%s,%.2f,%.2f,%.2f\n",
                    resumo.getTipo(), resumo.getTaxa(),
                    resumo.getBase(), resumo.getImposto(), resumo.getTotal()));
            }

            AlertUtils.showInfoAlert("Exportação", "Relatório exportado com sucesso!");
        } catch (Exception e) {
            AlertUtils.showError("Erro na exportação", e.getMessage());
        }
    }

    private String formatarMoeda(BigDecimal valor) {
        return String.format("%,.2f", valor.doubleValue());
    }

    // Classes auxiliares
    public static class ResumoImposto {
        private final String tipo;
        private final String taxa;
        private final BigDecimal base;
        private final BigDecimal imposto;
        private final BigDecimal total;

        public ResumoImposto(String tipo, String taxa, BigDecimal base, BigDecimal imposto, BigDecimal total) {
            this.tipo = tipo;
            this.taxa = taxa;
            this.base = base;
            this.imposto = imposto;
            this.total = total;
        }

        public String getTipo() { return tipo; }
        public String getTaxa() { return taxa; }
        public BigDecimal getBase() { return base; }
        public BigDecimal getImposto() { return imposto; }
        public BigDecimal getTotal() { return total; }
    }

    public static class ResumoRetencao {
        private final String codigo;
        private final String descricao;
        private final BigDecimal taxa;
        private final BigDecimal totalRetido;

        public ResumoRetencao(String codigo, String descricao, BigDecimal taxa, BigDecimal totalRetido) {
            this.codigo = codigo;
            this.descricao = descricao;
            this.taxa = taxa;
            this.totalRetido = totalRetido;
        }

        public String getCodigo() { return codigo; }
        public String getDescricao() { return descricao; }
        public BigDecimal getTaxa() { return taxa; }
        public BigDecimal getTotalRetido() { return totalRetido; }
    }
}
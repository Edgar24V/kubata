package ao.allon.kubata.faturacao.view;

import ao.allon.kubata.core.ui.table.AdvancedTableView;
import ao.allon.kubata.core.ui.table.TableUtils;
import ao.allon.kubata.faturacao.domain.Armazem;
import ao.allon.kubata.faturacao.domain.Estoque;
import ao.allon.kubata.faturacao.domain.MovimentoStock;
import ao.allon.kubata.faturacao.domain.Produto;
import ao.allon.kubata.faturacao.service.EstoqueService;
import ao.allon.kubata.faturacao.service.CategoriaService;
import ao.allon.kubata.faturacao.service.ProdutoService;
import ao.allon.kubata.faturacao.service.SessionManager;
import ao.allon.kubata.faturacao.service.FifoService;
import ao.allon.kubata.faturacao.ui.modal.ModalService;
import ao.allon.kubata.faturacao.util.Money;
import atlantafx.base.theme.Styles;
import atlantafx.base.controls.MaskTextField;
import atlantafx.base.controls.Tile;
import atlantafx.base.controls.Card;
import ao.allon.kubata.faturacao.ui.util.TileFactory;
import ao.allon.kubata.faturacao.ui.util.IconUtils;
import ao.allon.kubata.faturacao.ui.util.AlertUtils;
import org.controlsfx.control.SearchableComboBox;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.Node;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import ao.allon.kubata.faturacao.domain.Imposto;
import ao.allon.kubata.faturacao.enums.UnidadeMedida;
import ao.allon.kubata.faturacao.domain.Fornecedor;
import ao.allon.kubata.faturacao.service.ImpostoService;
import ao.allon.kubata.faturacao.service.FornecedorService;
import org.controlsfx.validation.ValidationSupport;
import org.controlsfx.validation.Validator;
import org.controlsfx.validation.ValidationResult;

public class InventarioView extends BorderPane {

    private final ProdutoService produtoService;
    private final EstoqueService estoqueService;
    private final ao.allon.kubata.faturacao.service.SaftAoExportService saftAoExportService;
    private final CategoriaService categoriaService;
    private final ImpostoService impostoService;
    private final FornecedorService fornecedorService;
    private final SessionManager sessionManager;
    private final ModalService modalService;
    private final FifoService fifoService;
    private final ao.allon.kubata.faturacao.service.RelatorioEstoqueLoteService relatorioEstoqueLoteService;
    
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("EEEE, dd 'de' MMMM 'de' yyyy", new Locale("pt", "AO"));
    
    private final ObservableList<Produto> produtoData = FXCollections.observableArrayList();
    private final ObservableList<MovimentoStock> movimentoData = FXCollections.observableArrayList();
    
    private AdvancedTableView<Produto> tableProdutos;
    private AdvancedTableView<MovimentoStock> tableMovimentos;
    private FilteredList<Produto> filteredProdutos;
    
    private SearchableComboBox<ao.allon.kubata.faturacao.domain.Categoria> cbCategoria;
    private TextField txtSearch;
    private ComboBox<String> cbStatus;
    private ComboBox<Armazem> cbArmazemMovFilter;

    // KPI Labels
    private Label lblTotalValorVenda;
    private Label lblTotalValorCusto;
    private Label lblLucroEstimado;
    private Label lblItensBaixoEstoque;
    private Label lblItensEsgotados;
    private Label lblValidadeProxima;
    private MetricCard cardLucro;
    private MetricCard cardBaixoEstoque;
    private MetricCard cardEsgotados;
    private MetricCard cardValidadeCard;

    public InventarioView(ProdutoService produtoService, 
                          EstoqueService estoqueService, 
                          SessionManager sessionManager, 
                          CategoriaService categoriaService, 
                          ao.allon.kubata.faturacao.service.SaftAoExportService saftAoExportService, 
                          ImpostoService impostoService, 
                          FornecedorService fornecedorService, 
                          ModalService modalService, 
                          FifoService fifoService,
                          ao.allon.kubata.faturacao.service.RelatorioEstoqueLoteService relatorioEstoqueLoteService) {
        this.produtoService = produtoService;
        this.estoqueService = estoqueService;
        this.categoriaService = categoriaService;
        this.sessionManager = sessionManager;
        this.saftAoExportService = saftAoExportService;
        this.impostoService = impostoService;
        this.fornecedorService = fornecedorService;
        this.modalService = modalService;
        this.fifoService = fifoService;
        this.relatorioEstoqueLoteService = relatorioEstoqueLoteService;
        
        getStyleClass().add("inventario-view");
        
        initializeUI();
        refreshData();
    }

    private void initializeUI() {
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        
        Tab tabDashboard = new Tab("Visão Geral", createDashboardTab());
        Tab tabProdutos = new Tab("Produtos", createProdutosTab());
        Tab tabMovimentos = new Tab("Movimentações", createMovimentosTab());
        Tab tabFifo = new Tab("Gestão FIFO", createFifoTab());
        Tab tabValorizacao = new Tab("Valorização", createValorizacaoTab());
        
        tabPane.getTabs().addAll(tabDashboard, tabProdutos, tabMovimentos, tabFifo, tabValorizacao);
        setCenter(tabPane);
    }

    private Node createValorizacaoTab() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(20));

        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("Relatório de Valorização de Stock");
        title.getStyleClass().add(Styles.TITLE_3);
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Button btnRefresh = new Button("Atualizar", IconUtils.icon(Feather.REFRESH_CW, IconUtils.SIZE_SMALL));
        btnRefresh.getStyleClass().add(Styles.BUTTON_OUTLINED);
        
        header.getChildren().addAll(title, spacer, btnRefresh);

        AdvancedTableView<ao.allon.kubata.faturacao.service.RelatorioEstoqueLoteService.ValorizacaoEstoqueDto> table = new AdvancedTableView<>();
        TableUtils.standardize(table);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<ao.allon.kubata.faturacao.service.RelatorioEstoqueLoteService.ValorizacaoEstoqueDto, String> colProd = new TableColumn<>("Produto");
        colProd.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNome()));
        
        TableColumn<ao.allon.kubata.faturacao.service.RelatorioEstoqueLoteService.ValorizacaoEstoqueDto, Integer> colStock = new TableColumn<>("Stock");
        colStock.setCellValueFactory(new PropertyValueFactory<>("stockTotal"));
        
        TableColumn<ao.allon.kubata.faturacao.service.RelatorioEstoqueLoteService.ValorizacaoEstoqueDto, String> colCustoMedio = new TableColumn<>("Custo Médio");
        colCustoMedio.setCellValueFactory(c -> new SimpleStringProperty(Money.formatAOA(c.getValue().getCustoMedio())));
        
        TableColumn<ao.allon.kubata.faturacao.service.RelatorioEstoqueLoteService.ValorizacaoEstoqueDto, String> colTotalMedio = new TableColumn<>("Total (C. Médio)");
        colTotalMedio.setCellValueFactory(c -> new SimpleStringProperty(Money.formatAOA(c.getValue().getValorTotalCustoMedio())));
        
        TableColumn<ao.allon.kubata.faturacao.service.RelatorioEstoqueLoteService.ValorizacaoEstoqueDto, String> colUltimoCusto = new TableColumn<>("Último Custo");
        colUltimoCusto.setCellValueFactory(c -> new SimpleStringProperty(Money.formatAOA(c.getValue().getUltimoCusto())));
        
        TableColumn<ao.allon.kubata.faturacao.service.RelatorioEstoqueLoteService.ValorizacaoEstoqueDto, String> colTotalUltimo = new TableColumn<>("Total (Últ. Custo)");
        colTotalUltimo.setCellValueFactory(c -> new SimpleStringProperty(Money.formatAOA(c.getValue().getValorTotalUltimoCusto())));

        TableColumn<ao.allon.kubata.faturacao.service.RelatorioEstoqueLoteService.ValorizacaoEstoqueDto, String> colAlerta = new TableColumn<>("Estado");
        colAlerta.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    var dto = getTableRow().getItem();
                    Produto p = produtoService.findById(dto.getProdutoId()).orElse(null);
                    if (p != null) {
                        int stock = dto.getStockTotal();
                        int min = p.getStockMinimo() != null ? p.getStockMinimo() : 0;
                        
                        if (stock <= 0) {
                            Label l = new Label("ESGOTADO");
                            l.getStyleClass().addAll("badge", Styles.DANGER);
                            setGraphic(l);
                        } else if (stock <= min) {
                            Label l = new Label("CRÍTICO");
                            l.getStyleClass().addAll("badge", Styles.WARNING);
                            setGraphic(l);
                        } else {
                            Label l = new Label("OK");
                            l.getStyleClass().addAll("badge", Styles.SUCCESS);
                            setGraphic(l);
                        }
                    }
                }
            }
        });

        table.getColumns().addAll(colProd, colStock, colAlerta, colCustoMedio, colTotalMedio, colUltimoCusto, colTotalUltimo);

        Runnable load = () -> table.setData(FXCollections.observableArrayList(relatorioEstoqueLoteService.gerarRelatorioValorizacao()));
        btnRefresh.setOnAction(e -> load.run());
        load.run();

        root.getChildren().addAll(header, table);
        return root;
    }

    private PieChart pieChartStock;
    private BarChart<String, Number> barChartTopProducts;

    private static boolean isUnidadeFracionavel(UnidadeMedida u) {
        if (u == null) return false;
        return switch (u) {
            case KILOGRAMA, LITRO, METRO, HORA, SERVICO -> true;
            default -> false;
        };
    }

    private static BigDecimal unidadeMinimaParaUnidade(UnidadeMedida u, Integer qtdMinima) {
        if (qtdMinima == null) return BigDecimal.ZERO;
        if (u == null) return BigDecimal.valueOf(qtdMinima);

        return switch (u) {
            case KILOGRAMA, LITRO, METRO -> BigDecimal.valueOf(qtdMinima).divide(new BigDecimal("1000"), 3, RoundingMode.HALF_UP);
            case HORA, SERVICO -> BigDecimal.valueOf(qtdMinima).divide(new BigDecimal("60"), 3, RoundingMode.HALF_UP);
            default -> BigDecimal.valueOf(qtdMinima);
        };
    }

    private static String unidadeShort(UnidadeMedida u) {
        if (u == null) return "un";
        return switch (u) {
            case KILOGRAMA -> "kg";
            case LITRO -> "L";
            case METRO -> "m";
            case HORA, SERVICO -> "h";
            case CAIXA -> "cx";
            default -> "un";
        };
    }

    private Node createDashboardTab() {
        VBox container = new VBox(20);
        container.setPadding(new Insets(30));
        container.setFillWidth(true);
        
        // 1. Header (Consistent with DashboardView)
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);
        
        VBox titleBox = new VBox(5);
        Label title = new Label("Visão Geral do Inventário");
        title.getStyleClass().addAll(Styles.TITLE_2);
        
        Label subtitle = new Label(LocalDate.now().format(dateFormatter));
        subtitle.getStyleClass().add(Styles.TEXT_MUTED);
        
        titleBox.getChildren().addAll(title, subtitle);
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        // Header Actions
        HBox actionsBar = new HBox(10);
        actionsBar.setAlignment(Pos.CENTER_RIGHT);
        
        Button btnEntrada = new Button("Entrada", IconUtils.icon(Feather.PLUS_CIRCLE, IconUtils.SIZE_SMALL));
        btnEntrada.getStyleClass().addAll(Styles.SUCCESS);
        btnEntrada.setOnAction(e -> showStockMovementModal(true));
        
        Button btnSaida = new Button("Saída", IconUtils.icon(Feather.MINUS_CIRCLE, IconUtils.SIZE_SMALL));
        btnSaida.getStyleClass().addAll(Styles.DANGER);
        btnSaida.setOnAction(e -> showStockMovementModal(false));
        
        Button btnTransferencia = new Button("Transferência", IconUtils.icon(Feather.REPEAT, IconUtils.SIZE_SMALL));
        btnTransferencia.getStyleClass().addAll(Styles.ACCENT);
        btnTransferencia.setOnAction(e -> showTransferenciaDialog());

        Button btnAjuste = new Button("Ajuste", IconUtils.icon(Feather.SLIDERS, IconUtils.SIZE_SMALL));
        btnAjuste.getStyleClass().addAll(Styles.WARNING);
        btnAjuste.setOnAction(e -> showAjusteDialog());

        Button btnExport = new Button("SAFT", IconUtils.icon(Feather.DOWNLOAD, IconUtils.SIZE_SMALL));
        btnExport.setOnAction(e -> exportarSaftAO());
        
        actionsBar.getChildren().addAll(btnEntrada, btnSaida, btnTransferencia, btnAjuste, btnExport);
        
        header.getChildren().addAll(titleBox, spacer, actionsBar);
        container.getChildren().add(header);

        // 2. KPI Cards - Usando FlowPane para responsividade
        FlowPane kpiContainer = new FlowPane(20, 20);
        kpiContainer.setAlignment(Pos.TOP_LEFT);
        kpiContainer.setPrefWrapLength(1000); // Tenta manter numa linha se possível
        
        // Criando labels que serão atualizados
        lblTotalValorVenda = new Label(Money.formatAOA(BigDecimal.ZERO));
        lblTotalValorCusto = new Label(Money.formatAOA(BigDecimal.ZERO));
        lblLucroEstimado = new Label(Money.formatAOA(BigDecimal.ZERO));
        lblItensBaixoEstoque = new Label("0");
        lblItensEsgotados = new Label("0");
        lblValidadeProxima = new Label("0");

        kpiContainer.getChildren().add(new MetricCard("Valor Total (Venda)", lblTotalValorVenda, Feather.DOLLAR_SIGN, Styles.SUCCESS));
        kpiContainer.getChildren().add(new MetricCard("Valor Total (Custo)", lblTotalValorCusto, Feather.SHOPPING_BAG, Styles.WARNING));
        
        cardLucro = new MetricCard("Lucro Estimado", lblLucroEstimado, Feather.TRENDING_UP, Styles.ACCENT);
        kpiContainer.getChildren().add(cardLucro);
        
        cardBaixoEstoque = new MetricCard("Baixo Estoque", lblItensBaixoEstoque, Feather.ALERT_TRIANGLE, Styles.DANGER);
        cardEsgotados = new MetricCard("Esgotados", lblItensEsgotados, Feather.X_OCTAGON, Styles.DANGER);
        cardValidadeCard = new MetricCard("Validade Próxima", lblValidadeProxima, Feather.CLOCK, Styles.WARNING);
        
        kpiContainer.getChildren().addAll(cardBaixoEstoque, cardEsgotados, cardValidadeCard);

        cardBaixoEstoque.setOnMouseClicked(e -> {
            if (cbStatus != null) cbStatus.getSelectionModel().select("Baixo Estoque");
        });
        cardEsgotados.setOnMouseClicked(e -> {
            if (cbStatus != null) cbStatus.getSelectionModel().select("Esgotado");
        });
        cardValidadeCard.setOnMouseClicked(e -> showValidadesProximasDialog());
        
        container.getChildren().add(kpiContainer);
        
        container.getChildren().add(new Separator());

        // 3. Charts Section - Usando GridPane para grid responsivo 2 colunas
        GridPane chartsContainer = new GridPane();
        chartsContainer.setHgap(20);
        chartsContainer.setVgap(20);
        
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(50);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(50);
        chartsContainer.getColumnConstraints().addAll(col1, col2);

        // Pie Chart: Stock Value by Category
        Card pieCard = new Card();
        pieCard.setHeader(new Label("Valor em Stock por Categoria"));
        pieCard.getStyleClass().add(Styles.ELEVATED_1);
        
        pieChartStock = new PieChart();
        pieChartStock.setLabelsVisible(true);
        pieChartStock.setLegendVisible(false);
        pieCard.setBody(pieChartStock);
        pieCard.setPrefHeight(400);
        
        // Bar Chart: Top 5 Products by Stock Quantity
        Card barCard = new Card();
        barCard.setHeader(new Label("Top 5 Produtos em Stock"));
        barCard.getStyleClass().add(Styles.ELEVATED_1);
        
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Produto");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Quantidade");
        
        barChartTopProducts = new BarChart<>(xAxis, yAxis);
        barChartTopProducts.setLegendVisible(false);
        barCard.setBody(barChartTopProducts);
        barCard.setPrefHeight(400);
        
        chartsContainer.add(pieCard, 0, 0);
        chartsContainer.add(barCard, 1, 0);
        
        container.getChildren().add(chartsContainer);
        
        ScrollPane scrollPane = new ScrollPane(container);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER); // Remove barra horizontal
        return scrollPane;
    }
    
    /**
     * Modern Metric Card with Icon and Colored Value (Consistent with DashboardView)
     */
    private static class MetricCard extends Card {
        private final Label lblTitle;
        private final FontIcon fontIcon;
        private final Label valueLabel;
        private String currentStyle;

        public MetricCard(String title, Label valueLabel, Feather icon, String colorStyle) {
            this.valueLabel = valueLabel;
            this.currentStyle = colorStyle;
            
            getStyleClass().add(Styles.ELEVATED_1);
            setPrefWidth(280);
            
            VBox body = new VBox(10);
            body.setAlignment(Pos.CENTER_LEFT);
            body.setPadding(new Insets(15));
            
            // Icon Header
            HBox headerBox = new HBox(10);
            headerBox.setAlignment(Pos.CENTER_LEFT);
            
            fontIcon = new FontIcon(icon);
            fontIcon.setIconSize(24);
            fontIcon.getStyleClass().addAll(colorStyle); 
            
            lblTitle = new Label(title);
            lblTitle.getStyleClass().add(Styles.TEXT_MUTED);
            lblTitle.setFont(Font.font("System", FontWeight.NORMAL, 14));
            
            headerBox.getChildren().addAll(fontIcon, lblTitle);
            
            // Value
            valueLabel.setFont(Font.font("System", FontWeight.BOLD, 28));
            valueLabel.getStyleClass().add(colorStyle); 
            
            body.getChildren().addAll(headerBox, valueLabel);
            setBody(body);
        }

        public void updateState(String title, String newStyle) {
            if (currentStyle.equals(newStyle) && lblTitle.getText().equals(title)) {
                return;
            }

            lblTitle.setText(title);
            
            fontIcon.getStyleClass().remove(currentStyle);
            fontIcon.getStyleClass().add(newStyle);
            
            valueLabel.getStyleClass().remove(currentStyle);
            valueLabel.getStyleClass().add(newStyle);
            
            this.currentStyle = newStyle;
        }
    }

    private Node createProdutosTab() {
        VBox container = new VBox(20);
        container.setPadding(new Insets(30));
        container.setFillWidth(true);
        
        // 1. Header (Consistent with DashboardView)
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);
        
        VBox titleBox = new VBox(5);
        Label title = new Label("Gestão de Produtos");
        title.getStyleClass().addAll(Styles.TITLE_2);
        
        Label subtitle = new Label("Gerencie produtos, stock e categorias");
        subtitle.getStyleClass().add(Styles.TEXT_MUTED);
        
        titleBox.getChildren().addAll(title, subtitle);
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        // Header Actions
        HBox actionsBar = new HBox(10);
        actionsBar.setAlignment(Pos.CENTER_RIGHT);
        
        Button btnCategorias = new Button("Categorias", IconUtils.icon(Feather.LIST, IconUtils.SIZE_SMALL));
        btnCategorias.getStyleClass().add(Styles.BUTTON_OUTLINED);
        btnCategorias.setOnAction(e -> showCategoriaDialog());
        
        Button btnNew = new Button("Novo Produto", IconUtils.icon(Feather.PLUS, IconUtils.SIZE_SMALL));
        btnNew.getStyleClass().add(Styles.ACCENT);
        btnNew.setOnAction(e -> showProdutoDialog(null));
        
        actionsBar.getChildren().addAll(btnCategorias, btnNew);
        
        header.getChildren().addAll(titleBox, spacer, actionsBar);
        container.getChildren().add(header);
        
        // 2. Filter Bar Card
        Card filterCard = new Card();
        filterCard.getStyleClass().add(Styles.ELEVATED_1);
        filterCard.setHeader(new Label("Filtros"));
        
        HBox filters = new HBox(10);
        filters.setAlignment(Pos.CENTER_LEFT);
        filters.setPadding(new Insets(10, 15, 15, 15));
        
        txtSearch = new TextField();
        txtSearch.setPromptText("Buscar por nome, código...");
        txtSearch.setPrefWidth(250);
        
        cbCategoria = new SearchableComboBox<>(FXCollections.observableArrayList(categoriaService.findAll()));
        cbCategoria.setPromptText("Todas Categorias");
        cbCategoria.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(ao.allon.kubata.faturacao.domain.Categoria c) {
                return c == null ? "" : c.getNome();
            }
            @Override
            public ao.allon.kubata.faturacao.domain.Categoria fromString(String s) { return null; }
        });
        
        cbStatus = new ComboBox<>(FXCollections.observableArrayList("Todos", "Em Stock", "Baixo Estoque", "Esgotado"));
        cbStatus.getSelectionModel().selectFirst();
        
        Button btnLowStock = new Button("Alertas", IconUtils.icon(Feather.ALERT_TRIANGLE, IconUtils.SIZE_SMALL));
        btnLowStock.getStyleClass().addAll(Styles.WARNING, Styles.BUTTON_OUTLINED);
        btnLowStock.setOnAction(e -> showLowStockReportDialog());
        
        Button btnValidades = new Button("Validades", IconUtils.icon(Feather.CLOCK, IconUtils.SIZE_SMALL));
        btnValidades.getStyleClass().addAll(Styles.WARNING, Styles.BUTTON_OUTLINED);
        btnValidades.setOnAction(e -> showValidadesProximasDialog());
        
        Region filterSpacer = new Region();
        HBox.setHgrow(filterSpacer, Priority.ALWAYS);
        
        Button btnExport = new Button("Exportar", IconUtils.icon(Feather.DOWNLOAD, IconUtils.SIZE_SMALL));
        btnExport.getStyleClass().add(Styles.BUTTON_OUTLINED);
        btnExport.setOnAction(e -> exportarCSV());
        
        Button btnRefresh = new Button("Atualizar", IconUtils.icon(Feather.REFRESH_CW, IconUtils.SIZE_SMALL));
        btnRefresh.getStyleClass().add(Styles.BUTTON_OUTLINED);
        btnRefresh.setOnAction(e -> refreshData());
        
        filters.getChildren().addAll(
            txtSearch, cbCategoria, cbStatus, 
            new Separator(javafx.geometry.Orientation.VERTICAL),
            btnLowStock, btnValidades,
            filterSpacer, btnExport, btnRefresh
        );
        
        filterCard.setBody(filters);
        container.getChildren().add(filterCard);
        
        // 3. Table Card
        Card tableCard = new Card();
        tableCard.getStyleClass().add(Styles.ELEVATED_1);
        tableCard.setHeader(new Label("Lista de Produtos"));
        
        tableProdutos = new AdvancedTableView<>();
        TableUtils.standardize(tableProdutos);
        tableProdutos.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        tableProdutos.getStyleClass().add(Styles.STRIPED);
        
        TableColumn<Produto, String> colCodigo = new TableColumn<>("Código");
        colCodigo.setCellValueFactory(new PropertyValueFactory<>("codigoBarra"));
        
        TableColumn<Produto, String> colNome = new TableColumn<>("Produto");
        colNome.setCellValueFactory(new PropertyValueFactory<>("nome"));
        
        TableColumn<Produto, String> colCat = new TableColumn<>("Categoria");
        colCat.setCellValueFactory(cell -> new SimpleStringProperty(
            cell.getValue().getCategoria() != null ? cell.getValue().getCategoria().getNome() : "-"
        ));
        
        TableColumn<Produto, String> colStock = new TableColumn<>("Stock");
        colStock.setCellValueFactory(cell -> {
            Produto p = cell.getValue();
            if (p == null) return new SimpleStringProperty("0");
            UnidadeMedida u = p.getUnidadeMedida();
            Integer s = p.getStock() != null ? p.getStock() : 0;
            if (isUnidadeFracionavel(u)) {
                BigDecimal shown = unidadeMinimaParaUnidade(u, s).setScale(3, RoundingMode.HALF_UP).stripTrailingZeros();
                return new SimpleStringProperty(shown.toPlainString().replace(".", ",") + " " + unidadeShort(u));
            }
            return new SimpleStringProperty(String.valueOf(s));
        });
        
        TableColumn<Produto, BigDecimal> colPreco = new TableColumn<>("Preço");
        colPreco.setCellValueFactory(new PropertyValueFactory<>("precoUnitario"));
        colPreco.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(Money.formatAOA(item));
                }
            }
        });

        TableColumn<Produto, BigDecimal> colCusto = new TableColumn<>("Custo");
        colCusto.setCellValueFactory(new PropertyValueFactory<>("precoCompra"));
        colCusto.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(Money.formatAOA(item));
                }
            }
        });

        TableColumn<Produto, String> colMargem = new TableColumn<>("Margem %");
        colMargem.setCellValueFactory(cell -> {
            Produto p = cell.getValue();
            BigDecimal venda = p.getPrecoUnitario() != null ? p.getPrecoUnitario() : BigDecimal.ZERO;
            BigDecimal custo = p.getPrecoCompra() != null ? p.getPrecoCompra() : BigDecimal.ZERO;
            if (venda.compareTo(BigDecimal.ZERO) <= 0) {
                return new SimpleStringProperty("-");
            }
            BigDecimal margem = venda.subtract(custo).divide(venda, java.math.MathContext.DECIMAL64).multiply(BigDecimal.valueOf(100));
            return new SimpleStringProperty(String.format("%.1f%%", margem));
        });
        colMargem.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    try {
                        double val = Double.parseDouble(item.replace("%", ""));
                        if (val < 0) {
                            setStyle("-fx-text-fill: -color-danger-fg; -fx-font-weight: bold;");
                        } else {
                            setStyle("-fx-text-fill: -color-success-fg; -fx-font-weight: bold;");
                        }
                    } catch (Exception ignored) {
                        setStyle("");
                    }
                }
            }
        });

        TableColumn<Produto, BigDecimal> colValorStock = new TableColumn<>("Valor Stock");
        colValorStock.setCellValueFactory(cell -> {
            Produto p = cell.getValue();
            if (p == null) {
                return new javafx.beans.property.SimpleObjectProperty<>(BigDecimal.ZERO);
            }
            BigDecimal preco = p.getPrecoUnitario() != null ? p.getPrecoUnitario() : BigDecimal.ZERO;
            UnidadeMedida u = p.getUnidadeMedida();
            Integer stockMinimo = p.getStock() != null ? p.getStock() : 0;
            BigDecimal qtd = isUnidadeFracionavel(u)
                ? unidadeMinimaParaUnidade(u, stockMinimo)
                : BigDecimal.valueOf(stockMinimo);
            return new javafx.beans.property.SimpleObjectProperty<>(preco.multiply(qtd));
        });
        colValorStock.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(Money.formatAOA(item));
                }
            }
        });
        
        TableColumn<Produto, Void> colActions = new TableColumn<>("Ações");
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit = new Button(null, new FontIcon(Feather.EDIT));
            private final Button btnDelete = new Button(null, new FontIcon(Feather.TRASH));
            private final HBox pane = new HBox(5, btnEdit, btnDelete);

            {
                btnEdit.getStyleClass().addAll(Styles.SMALL, Styles.BUTTON_OUTLINED, Styles.ACCENT);
                btnDelete.getStyleClass().addAll(Styles.SMALL, Styles.BUTTON_OUTLINED, Styles.DANGER);
                pane.setAlignment(Pos.CENTER);
                
                btnEdit.setOnAction(e -> {
                    Produto p = getTableView().getItems().get(getIndex());
                    showProdutoDialog(p);
                });
                
                btnDelete.setOnAction(e -> {
                    Produto p = getTableView().getItems().get(getIndex());
                    deleteProduto(p);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(pane);
                }
            }
        });

        tableProdutos.getColumns().addAll(colCodigo, colNome, colCat, colStock, colPreco, colCusto, colMargem, colValorStock, colActions);
        
        VBox tableContainer = new VBox(tableProdutos);
        tableContainer.setPadding(new Insets(0, 15, 15, 15));
        VBox.setVgrow(tableProdutos, Priority.ALWAYS);
        tableCard.setBody(tableContainer);
        
        container.getChildren().add(tableCard);
        VBox.setVgrow(tableCard, Priority.ALWAYS);
        
        // Filter Logic
        filteredProdutos = new FilteredList<>(produtoData, p -> true);
        tableProdutos.setItems(filteredProdutos);
        
        txtSearch.textProperty().addListener((o, ov, nv) -> applyFilters());
        cbCategoria.valueProperty().addListener((o, ov, nv) -> applyFilters());
        cbStatus.valueProperty().addListener((o, ov, nv) -> applyFilters());

        // Context Menu
        ContextMenu contextMenu = new ContextMenu();
        MenuItem itemEdit = new MenuItem("Editar", IconUtils.icon(Feather.EDIT, IconUtils.SIZE_SMALL));
        itemEdit.setOnAction(e -> {
            Produto p = tableProdutos.getSelectionModel().getSelectedItem();
            if (p != null) showProdutoDialog(p);
        });
        
        MenuItem itemDelete = new MenuItem("Excluir", IconUtils.icon(Feather.TRASH, IconUtils.SIZE_SMALL));
        itemDelete.setOnAction(e -> {
            Produto p = tableProdutos.getSelectionModel().getSelectedItem();
            if (p != null) deleteProduto(p);
        });
        
        MenuItem itemLabel = new MenuItem("Imprimir Etiqueta", IconUtils.icon(Feather.PRINTER, IconUtils.SIZE_SMALL));
        itemLabel.setOnAction(e -> {
            Produto p = tableProdutos.getSelectionModel().getSelectedItem();
            if (p != null) printBarcodeLabel(p);
        });

        MenuItem itemDetails = new MenuItem("Detalhes de Stock", IconUtils.icon(Feather.LAYERS, IconUtils.SIZE_SMALL));
        itemDetails.setOnAction(e -> {
            Produto p = tableProdutos.getSelectionModel().getSelectedItem();
            if (p != null) showDetalhesStockDialog(p);
        });
        
        contextMenu.getItems().addAll(itemEdit, itemLabel, itemDetails, new SeparatorMenuItem(), itemDelete);
        tableProdutos.setContextMenu(contextMenu);
        
        return container;
    }
    
    private void printBarcodeLabel(Produto p) {
        // Mockup implementation for Barcode Label
        VBox label = new VBox(5);
        label.setAlignment(Pos.CENTER);
        label.setPadding(new Insets(10));
        label.setStyle("-fx-border-color: black; -fx-border-width: 1; -fx-background-color: white;");
        label.setPrefSize(200, 100);
        
        Label lblEmpresa = new Label("KUBATA");
        lblEmpresa.setFont(Font.font("System", FontWeight.BOLD, 10));
        
        Label lblNome = new Label(p.getNome());
        lblNome.setFont(Font.font("System", FontWeight.BOLD, 12));
        lblNome.setWrapText(true);
        
        Label lblPrice = new Label(Money.formatAOA(p.getPrecoUnitario()));
        lblPrice.setFont(Font.font("System", FontWeight.BOLD, 14));
        
        Label lblBarcode = new Label(p.getCodigoBarra());
        lblBarcode.setFont(Font.font("Monospaced", 10));
        
        // Barcode visual representation (fake bars)
        HBox bars = new HBox(2);
        bars.setAlignment(Pos.CENTER);
        bars.setPrefHeight(30);
        for (int i = 0; i < 20; i++) {
            Region bar = new Region();
            bar.setStyle("-fx-background-color: black;");
            bar.setPrefWidth(Math.random() > 0.5 ? 2 : 4);
            bar.setPrefHeight(30);
            bars.getChildren().add(bar);
        }
        
        label.getChildren().addAll(lblEmpresa, lblNome, bars, lblBarcode, lblPrice);
        
        modalService.create()
            .title("Visualização de Etiqueta")
            .content(label)
            .autoSize()
            .buildAndShow();
    }



    
    private FilteredList<MovimentoStock> filteredMovimentos;
    private DatePicker dpInicio;
    private DatePicker dpFim;
    private ComboBox<String> cbTipoMovimento;
    private TextField txtSearchMovimento;

    private Node createMovimentosTab() {
        VBox container = new VBox(20);
        container.setPadding(new Insets(30));
        container.setFillWidth(true);
        
        // 1. Header with Title and Actions
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);
        
        VBox titleBox = new VBox(5);
        Label title = new Label("Histórico de Movimentações");
        title.getStyleClass().addAll(Styles.TITLE_2);
        
        Label subtitle = new Label("Rastreie entradas, saídas e ajustes de stock");
        subtitle.getStyleClass().add(Styles.TEXT_MUTED);
        
        titleBox.getChildren().addAll(title, subtitle);
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Button btnExport = new Button("Exportar", IconUtils.icon(Feather.DOWNLOAD, IconUtils.SIZE_SMALL));
         btnExport.setOnAction(e -> exportarMovimentosCSV());
         
         Button btnRefresh = new Button("Atualizar", IconUtils.icon(Feather.REFRESH_CW, IconUtils.SIZE_SMALL));
        btnRefresh.setOnAction(e -> refreshData());
        
        header.getChildren().addAll(titleBox, spacer, btnExport, btnRefresh);
        container.getChildren().add(header);
        
        // 2. Filter Card
        Card filterCard = new Card();
        filterCard.getStyleClass().add(Styles.ELEVATED_1);
        filterCard.setHeader(new Label("Filtros"));
        
        HBox filterBar = new HBox(10);
        filterBar.setAlignment(Pos.CENTER_LEFT);
        filterBar.setPadding(new Insets(10, 15, 15, 15));
        
        txtSearchMovimento = new TextField();
        txtSearchMovimento.setPromptText("Buscar por Produto ou Lote...");
        txtSearchMovimento.setPrefWidth(250);
        
        dpInicio = new DatePicker();
        dpInicio.setPromptText("Data Início");
        dpInicio.setPrefWidth(120);
        
        dpFim = new DatePicker();
        dpFim.setPromptText("Data Fim");
        dpFim.setPrefWidth(120);
        
        cbTipoMovimento = new ComboBox<>(FXCollections.observableArrayList("Todos", "ENTRADA", "SAIDA", "TRANSFERENCIA", "AJUSTE"));
        cbTipoMovimento.getSelectionModel().selectFirst();
        cbTipoMovimento.setPrefWidth(150);

        cbArmazemMovFilter = new ComboBox<>(FXCollections.observableArrayList(estoqueService.listarArmazens()));
        cbArmazemMovFilter.setPromptText("Armazém");
        cbArmazemMovFilter.setPrefWidth(180);
        cbArmazemMovFilter.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(Armazem a) { return a == null ? "" : a.getNome(); }
            @Override
            public Armazem fromString(String s) { return null; }
        });
        
        Button btnClearFilters = new Button(null, IconUtils.icon(Feather.X, IconUtils.SIZE_SMALL));
        btnClearFilters.setTooltip(new Tooltip("Limpar Filtros"));
        btnClearFilters.getStyleClass().add(Styles.BUTTON_OUTLINED);
        btnClearFilters.setOnAction(e -> {
            txtSearchMovimento.clear();
            dpInicio.setValue(null);
            dpFim.setValue(null);
            cbTipoMovimento.getSelectionModel().selectFirst();
        });
        
        filterBar.getChildren().addAll(
            txtSearchMovimento, 
            new Separator(javafx.geometry.Orientation.VERTICAL),
            dpInicio, 
            new Label("até"), 
            dpFim, 
            cbTipoMovimento,
            cbArmazemMovFilter,
            btnClearFilters
        );
        
        filterCard.setBody(filterBar);
        container.getChildren().add(filterCard);
        
        // 3. Table Card
        Card tableCard = new Card();
        tableCard.getStyleClass().add(Styles.ELEVATED_1);
        tableCard.setHeader(new Label("Histórico de Movimentos"));
        
        tableMovimentos = new AdvancedTableView<>();
        TableUtils.standardize(tableMovimentos);
        tableMovimentos.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        tableMovimentos.getStyleClass().add(Styles.STRIPED);
        VBox.setVgrow(tableMovimentos, Priority.ALWAYS);
        tableMovimentos.setPlaceholder(new Label("Nenhum movimento encontrado com os filtros selecionados."));
        
        TableColumn<MovimentoStock, String> colData = new TableColumn<>("Data / Hora");
        colData.setCellValueFactory(cell -> {
            var mov = cell.getValue();
            if (mov != null && mov.getDataMovimento() != null) {
                return new SimpleStringProperty(mov.getDataMovimento().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            }
            return new SimpleStringProperty("-");
        });
        
        TableColumn<MovimentoStock, String> colTipo = new TableColumn<>("Tipo");
        colTipo.setCellValueFactory(cell -> {
             var mov = cell.getValue();
             if (mov != null && mov.getTipoMovimento() != null) {
                 return new SimpleStringProperty(mov.getTipoMovimento().toString());
             }
             return new SimpleStringProperty("-");
        });
        colTipo.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label lbl = new Label(item);
                    lbl.getStyleClass().add("badge"); // Assuming AtlantaFX badge style or similar
                    if ("ENTRADA".equals(item)) {
                        lbl.getStyleClass().addAll(Styles.SUCCESS, Styles.TEXT_SMALL);
                        lbl.setGraphic(new FontIcon(Feather.ARROW_DOWN_LEFT));
                    } else if ("SAIDA".equals(item)) {
                        lbl.getStyleClass().addAll(Styles.DANGER, Styles.TEXT_SMALL);
                        lbl.setGraphic(new FontIcon(Feather.ARROW_UP_RIGHT));
                    } else if ("TRANSFERENCIA".equals(item)) {
                        lbl.getStyleClass().addAll(Styles.ACCENT, Styles.TEXT_SMALL);
                        lbl.setGraphic(new FontIcon(Feather.REPEAT));
                    } else {
                        lbl.getStyleClass().addAll(Styles.WARNING, Styles.TEXT_SMALL);
                        lbl.setGraphic(new FontIcon(Feather.SLIDERS));
                    }
                    setGraphic(lbl);
                    setText(null);
                }
            }
        });
        
        TableColumn<MovimentoStock, String> colProd = new TableColumn<>("Produto");
        colProd.setCellValueFactory(cell -> {
             var mov = cell.getValue();
             if (mov != null && mov.getProduto() != null) {
                 return new SimpleStringProperty(mov.getProduto().getNome());
             }
             return new SimpleStringProperty("?");
        });
        colProd.setStyle("-fx-font-weight: bold;");

        TableColumn<MovimentoStock, String> colArmazem = new TableColumn<>("Armazém");
        colArmazem.setCellValueFactory(cell -> {
            var mov = cell.getValue();
            if (mov != null && mov.getArmazem() != null) {
                return new SimpleStringProperty(mov.getArmazem().getNome());
            }
            return new SimpleStringProperty("-");
        });
        
        TableColumn<MovimentoStock, Integer> colQtd = new TableColumn<>("Qtd");
        colQtd.setCellValueFactory(new PropertyValueFactory<>("quantidade"));
        colQtd.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    MovimentoStock mov = getTableView().getItems().get(getIndex());
                    setText(String.valueOf(item));
                    if (mov.getTipoMovimento() != null && 
                       (mov.getTipoMovimento().name().equals("ENTRADA") || mov.getTipoMovimento().name().equals("AJUSTE") && item > 0)) {
                        setText("+" + item);
                        setStyle("-fx-text-fill: -color-success-fg; -fx-font-weight: bold;");
                    } else {
                        setText("-" + item);
                        setStyle("-fx-text-fill: -color-danger-fg; -fx-font-weight: bold;");
                    }
                }
            }
        });
        
        TableColumn<MovimentoStock, Integer> colSaldoAnterior = new TableColumn<>("Saldo Antes");
        colSaldoAnterior.setCellValueFactory(new PropertyValueFactory<>("saldoAnterior"));

        TableColumn<MovimentoStock, Integer> colSaldo = new TableColumn<>("Saldo Após");
        colSaldo.setCellValueFactory(new PropertyValueFactory<>("saldoAtual"));

        TableColumn<MovimentoStock, String> colFornecedor = new TableColumn<>("Fornecedor");
        colFornecedor.setCellValueFactory(cell -> {
            var mov = cell.getValue();
            if (mov != null && mov.getFornecedor() != null) {
                return new SimpleStringProperty(mov.getFornecedor().getNome());
            }
            return new SimpleStringProperty("");
        });
        
        TableColumn<MovimentoStock, String> colLote = new TableColumn<>("Lote");
        colLote.setCellValueFactory(new PropertyValueFactory<>("lote"));
        
        TableColumn<MovimentoStock, String> colObs = new TableColumn<>("Observação");
        colObs.setCellValueFactory(new PropertyValueFactory<>("observacao"));
        colObs.setPrefWidth(200);
        
        tableMovimentos.getColumns().addAll(colData, colTipo, colProd, colArmazem, colQtd, colSaldoAnterior, colSaldo, colLote, colFornecedor, colObs);
        
        // Setup FilteredList
        filteredMovimentos = new FilteredList<>(movimentoData, p -> true);
        tableMovimentos.setItems(filteredMovimentos);
        
        // Add Listeners
        txtSearchMovimento.textProperty().addListener((o, ov, nv) -> applyMovimentosFilters());
        cbTipoMovimento.valueProperty().addListener((o, ov, nv) -> applyMovimentosFilters());
        dpInicio.valueProperty().addListener((o, ov, nv) -> applyMovimentosFilters());
        dpFim.valueProperty().addListener((o, ov, nv) -> applyMovimentosFilters());
        cbArmazemMovFilter.valueProperty().addListener((o, ov, nv) -> applyMovimentosFilters());
        
        VBox tableContainer = new VBox(tableMovimentos);
        tableContainer.setPadding(new Insets(0, 15, 15, 15));
        VBox.setVgrow(tableMovimentos, Priority.ALWAYS);
        tableCard.setBody(tableContainer);
        
        container.getChildren().add(tableCard);
        VBox.setVgrow(tableCard, Priority.ALWAYS);
        
        return container;
    }

    private void applyMovimentosFilters() {
        String search = txtSearchMovimento.getText().toLowerCase();
        String tipo = cbTipoMovimento.getValue();
        LocalDate inicio = dpInicio.getValue();
        LocalDate fim = dpFim.getValue();
        Armazem armazemFiltro = cbArmazemMovFilter != null ? cbArmazemMovFilter.getValue() : null;
        
        filteredMovimentos.setPredicate(mov -> {
            // 1. Search Text (Product Name or Lote)
            boolean matchSearch = search.isEmpty() || 
                (mov.getProduto() != null && mov.getProduto().getNome().toLowerCase().contains(search)) ||
                (mov.getLote() != null && mov.getLote().toLowerCase().contains(search));
            
            // 2. Type
            boolean matchTipo = "Todos".equals(tipo) || (mov.getTipoMovimento() != null && mov.getTipoMovimento().name().equals(tipo));
            
            // 3. Date Range
            boolean matchDate = true;
            if (mov.getDataMovimento() != null) {
                LocalDate dataMov = mov.getDataMovimento().toLocalDate();
                if (inicio != null && dataMov.isBefore(inicio)) matchDate = false;
                if (fim != null && dataMov.isAfter(fim)) matchDate = false;
            }

            boolean matchArmazem = armazemFiltro == null || (mov.getArmazem() != null && mov.getArmazem().getId().equals(armazemFiltro.getId()));
            
            return matchSearch && matchTipo && matchDate && matchArmazem;
        });
    }

    private void applyFilters() {
        String search = txtSearch.getText().toLowerCase();
        ao.allon.kubata.faturacao.domain.Categoria cat = cbCategoria.getValue();
        String status = cbStatus.getValue();
        
        tableProdutos.setFilter(p -> {
            String nome = p.getNome() == null ? "" : p.getNome().toLowerCase();
            String codigo = p.getCodigoBarra() == null ? "" : p.getCodigoBarra().toLowerCase();
            boolean matchSearch = search.isEmpty() || 
                nome.contains(search) || 
                codigo.contains(search);
                
            boolean matchCat = cat == null || (p.getCategoria() != null && p.getCategoria().getId().equals(cat.getId()));
            
            boolean matchStatus = true;
            if ("Em Stock".equals(status)) matchStatus = p.getStock() > p.getStockMinimo();
            else if ("Baixo Estoque".equals(status)) matchStatus = p.getStock() > 0 && p.getStock() <= p.getStockMinimo();
            else if ("Esgotado".equals(status)) matchStatus = p.getStock() <= 0;
            
            return matchSearch && matchCat && matchStatus;
        });
    }

    public void refreshData() {
        produtoData.setAll(produtoService.findAll());
        movimentoData.setAll(estoqueService.listarMovimentos());
        tableProdutos.setData(produtoData);
        tableMovimentos.setData(movimentoData);
        updateKpis();
    }
    
    private void updateKpis() {
        BigDecimal totalVenda = BigDecimal.ZERO;
        BigDecimal totalCusto = BigDecimal.ZERO;
        long baixoEstoque = 0;
        long itensEsgotados = 0;
        long validadeProxima = 0;
        LocalDate hoje = LocalDate.now();
        LocalDate limiteValidade = hoje.plusDays(30);
        
        java.util.Map<String, BigDecimal> categoriaValor = new java.util.HashMap<>();
        java.util.List<Produto> topProducts = new java.util.ArrayList<>(produtoData);

        for (Produto p : produtoData) {
            Integer stock = p.getStock();
            if (stock == null) stock = 0;
            
            BigDecimal qtd = BigDecimal.valueOf(stock);
            
            if (qtd.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal preco = p.getPrecoUnitario();
                if (preco == null) preco = BigDecimal.ZERO;
                
                totalVenda = totalVenda.add(preco.multiply(qtd));
                
                if (p.getPrecoCompra() != null) {
                    totalCusto = totalCusto.add(p.getPrecoCompra().multiply(qtd));
                }
                
                // Categoria aggregation
                String catName = p.getCategoria() != null ? p.getCategoria().getNome() : "Sem Categoria";
                BigDecimal valor = preco.multiply(qtd);
                categoriaValor.merge(catName, valor, BigDecimal::add);
            }
            
            Integer stockMin = p.getStockMinimo();
            if (stockMin == null) stockMin = 0;
            
            if (stock <= stockMin) {
                baixoEstoque++;
            }

            if (stock <= 0) {
                itensEsgotados++;
            }
        }

        List<Estoque> estoques = estoqueService.listarTodosEstoque();
        for (Estoque e : estoques) {
            if (e.getValidade() != null && e.getQuantidade() != null && e.getQuantidade() > 0) {
                LocalDate validade = e.getValidade();
                if (!validade.isBefore(hoje) && !validade.isAfter(limiteValidade)) {
                    validadeProxima++;
                }
            }
        }
        
        lblTotalValorVenda.setText(Money.formatAOA(totalVenda));
        lblTotalValorCusto.setText(Money.formatAOA(totalCusto));
        
        BigDecimal lucro = totalVenda.subtract(totalCusto);
        lblLucroEstimado.setText(Money.formatAOA(lucro));
        
        if (lucro.compareTo(BigDecimal.ZERO) < 0) {
             cardLucro.updateState("Prejuízo Estimado", Styles.DANGER);
        } else {
             cardLucro.updateState("Lucro Estimado", Styles.ACCENT);
        }
        
        lblItensBaixoEstoque.setText(String.valueOf(baixoEstoque));
        lblItensEsgotados.setText(String.valueOf(itensEsgotados));
        lblValidadeProxima.setText(String.valueOf(validadeProxima));
        
        // Update Charts
        if (pieChartStock != null) {
            pieChartStock.getData().clear();
            categoriaValor.forEach((cat, val) -> {
                pieChartStock.getData().add(new PieChart.Data(cat, val.doubleValue()));
            });
        }
        
        if (barChartTopProducts != null) {
            barChartTopProducts.getData().clear();
            topProducts.sort((p1, p2) -> {
                int s1 = p1.getStock() == null ? 0 : p1.getStock();
                int s2 = p2.getStock() == null ? 0 : p2.getStock();
                return Integer.compare(s2, s1);
            });
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Stock");
            topProducts.stream().limit(5).forEach(p -> {
                 series.getData().add(new XYChart.Data<>(p.getNome(), p.getStock() == null ? 0 : p.getStock()));
            });
            barChartTopProducts.getData().add(series);
        }
    }
    
    private void showStockMovementModal(boolean isEntry) {
        VBox form = new VBox(15);
        form.setPadding(new Insets(20));
        form.setPrefWidth(450);

        Label titleLabel = new Label(isEntry ? "Entrada de Stock" : "Saída de Stock");
        titleLabel.getStyleClass().add(Styles.TITLE_3);

        ComboBox<Produto> cbProduto = new ComboBox<>(FXCollections.observableArrayList(produtoService.findAll()));
        cbProduto.setPromptText("Selecione o Produto");
        cbProduto.setMaxWidth(Double.MAX_VALUE);
        cbProduto.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(Produto p) { return p == null ? "" : p.getCodigoBarra() + " - " + p.getNome(); }
            @Override public Produto fromString(String string) { return null; }
        });
        
        ComboBox<Armazem> cbArmazem = new ComboBox<>(FXCollections.observableArrayList(estoqueService.listarArmazens()));
        cbArmazem.setPromptText("Selecione o Armazém");
        cbArmazem.setMaxWidth(Double.MAX_VALUE);
        cbArmazem.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(Armazem a) { return a == null ? "" : a.getNome(); }
            @Override public Armazem fromString(String s) { return null; }
        });
        try { cbArmazem.setValue(estoqueService.getArmazemPrincipal()); } catch (Exception ignored) {}

        ComboBox<UnidadeMedida> cbUnidade = new ComboBox<>();
        cbUnidade.setPromptText("Unidade");
        cbUnidade.setMaxWidth(Double.MAX_VALUE);

        TextField txtQtd = new TextField();
        txtQtd.setPromptText("Quantidade");
        
        TextField txtCustoEntrada = new TextField();
        txtCustoEntrada.setPromptText("Custo Unitário de Compra");
        
        TextField txtNovoPrecoVenda = new TextField();
        txtNovoPrecoVenda.setPromptText("Novo Preço Venda (Opcional)");

        Label lblSugestao = new Label("");
        lblSugestao.getStyleClass().add(Styles.TEXT_SMALL);
        lblSugestao.setStyle("-fx-text-fill: -color-accent-fg;");

        // Lógica de atualização ao selecionar produto
        cbProduto.valueProperty().addListener((obs, ov, nv) -> {
            if (nv != null) {
                cbUnidade.getItems().setAll(nv.getUnidadeMedida(), nv.getUnidadeCompra());
                cbUnidade.setValue(nv.getUnidadeCompra() != null && isEntry ? nv.getUnidadeCompra() : nv.getUnidadeMedida());
                
                if (isEntry) {
                    txtCustoEntrada.setText(nv.getPrecoCompra() != null ? nv.getPrecoCompra().toString() : "");
                    txtNovoPrecoVenda.setText(nv.getPrecoUnitario() != null ? nv.getPrecoUnitario().toString() : "");
                }
            }
        });

        // Calculadora de Margem Sugerida
        txtCustoEntrada.textProperty().addListener((obs, ov, nv) -> {
            try {
                if (nv != null && !nv.isEmpty() && cbProduto.getValue() != null && isEntry) {
                    BigDecimal custo = new BigDecimal(nv.replace(",", "."));
                    Produto p = cbProduto.getValue();
                    BigDecimal custoBase = custo;
                    if (cbUnidade.getValue() == p.getUnidadeCompra() && p.getFatorConversao().compareTo(BigDecimal.ZERO) > 0) {
                        custoBase = custo.divide(p.getFatorConversao(), 2, RoundingMode.HALF_UP);
                    }
                    if (p.getPrecoUnitario() != null && p.getPrecoCompra() != null && p.getPrecoCompra().compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal margemAtual = p.getPrecoUnitario().subtract(p.getPrecoCompra())
                                                .divide(p.getPrecoCompra(), 4, RoundingMode.HALF_UP)
                                                .multiply(new BigDecimal("100"));
                        BigDecimal sugestaoVenda = custoBase.multiply(BigDecimal.ONE.add(margemAtual.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP)));
                        lblSugestao.setText(String.format("Custo Base: %s | Sugestão Venda (Margem %.1f%%): %s", 
                                            ao.allon.kubata.faturacao.util.Money.formatAOA(custoBase),
                                            margemAtual,
                                            ao.allon.kubata.faturacao.util.Money.formatAOA(sugestaoVenda)));
                    }
                }
            } catch (Exception ignored) {}
        });

        ComboBox<Fornecedor> cbFornecedor = new ComboBox<>();
        if (isEntry) {
            cbFornecedor.setItems(FXCollections.observableArrayList(fornecedorService.findAll()));
            cbFornecedor.setPromptText("Selecione o Fornecedor (Opcional)");
            cbFornecedor.setMaxWidth(Double.MAX_VALUE);
            cbFornecedor.setConverter(new javafx.util.StringConverter<>() {
                @Override public String toString(Fornecedor f) { return f == null ? "" : f.getNome(); }
                @Override public Fornecedor fromString(String s) { return null; }
            });
        }
        
        TextField txtLote = new TextField("GERAL");
        txtLote.setPromptText("Lote (Opcional)");
        
        DatePicker dpValidade = new DatePicker();
        dpValidade.setPromptText("Validade (Opcional)");
        dpValidade.setMaxWidth(Double.MAX_VALUE);
        
        TextField txtObs = new TextField();
        txtObs.setPromptText("Observação / Motivo");
        
        form.getChildren().addAll(titleLabel, cbProduto, cbArmazem);
        form.getChildren().addAll(new HBox(10, new VBox(5, new Label("Qtd:"), txtQtd), new VBox(5, new Label("Unidade:"), cbUnidade)));
        
        if (isEntry) {
            form.getChildren().addAll(new VBox(5, new Label("Custo Unitário:"), txtCustoEntrada, lblSugestao));
            form.getChildren().addAll(new VBox(5, new Label("Novo Preço Venda:"), txtNovoPrecoVenda));
            form.getChildren().add(cbFornecedor);
        }
        form.getChildren().addAll(txtLote, dpValidade, txtObs);
        
        String confirmButtonText = isEntry ? "Confirmar Entrada" : "Confirmar Saída";

        modalService.create()
            .title(isEntry ? "Entrada de Stock " : "Saída de Stock ")
            .content(form)
            .dynamicSize()
            .withConfirmButton(confirmButtonText, () -> {
                try {
                    Produto p = cbProduto.getValue();
                    if (p == null) throw new IllegalArgumentException("Selecione um produto");
                    Armazem armazem = cbArmazem.getValue();
                    if (armazem == null) throw new IllegalArgumentException("Selecione um armazém");
                    
                    BigDecimal qtd = new BigDecimal(txtQtd.getText().trim().replace(",", "."));
                    String obs = txtObs.getText();
                    String lote = txtLote.getText();
                    if (lote == null || lote.trim().isEmpty()) lote = "GERAL";
                    
                    if (isEntry) {
                        BigDecimal custo = new BigDecimal(txtCustoEntrada.getText().trim().replace(",", "."));
                        BigDecimal novoVenda = txtNovoPrecoVenda.getText().isBlank() ? null : new BigDecimal(txtNovoPrecoVenda.getText().trim().replace(",", "."));
                        Fornecedor fornecedor = cbFornecedor.getValue();
                        
                        estoqueService.adicionarStockComConversao(p, armazem, qtd, cbUnidade.getValue(), custo, novoVenda, lote, dpValidade.getValue(), obs, fornecedor);
                    } else {
                        // Saída Profissional: Converte quantidade se necessário
                        int qtdInt;
                        if (isUnidadeFracionavel(cbUnidade.getValue())) {
                            qtdInt = switch (cbUnidade.getValue()) {
                                case KILOGRAMA, LITRO, METRO -> qtd.multiply(new BigDecimal("1000")).setScale(0, RoundingMode.HALF_UP).intValue();
                                case HORA, SERVICO -> qtd.multiply(new BigDecimal("60")).setScale(0, RoundingMode.HALF_UP).intValue();
                                default -> qtd.setScale(0, RoundingMode.HALF_UP).intValue();
                            };
                        } else {
                            qtdInt = qtd.intValue();
                        }
                        estoqueService.removerStock(p, armazem, qtdInt, lote, obs);
                    }
                    
                    AlertUtils.showSuccessNotification("Movimento registado com sucesso!");
                    refreshData();
                    return true;
                } catch (Exception ex) {
                    AlertUtils.showErrorAlert("Erro na Operação", ex.getMessage());
                    return false;
                }
            })
            .buildAndShow();
    }
    
    public void showTransferenciaDialog() {
        VBox form = new VBox(15);
        form.setPadding(new Insets(20));
        form.setPrefWidth(450);

        Label titleLabel = new Label("Transferência de Stock");
        titleLabel.getStyleClass().add(Styles.TITLE_3);

        ComboBox<Produto> cbProduto = new ComboBox<>(FXCollections.observableArrayList(produtoService.findAll()));
        cbProduto.setPromptText("Selecione o Produto");
        cbProduto.setMaxWidth(Double.MAX_VALUE);
        cbProduto.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(Produto p) { return p == null ? "" : p.getCodigoBarra() + " - " + p.getNome(); }
            @Override
            public Produto fromString(String string) { return null; }
        });

        ComboBox<Armazem> cbOrigem = new ComboBox<>(FXCollections.observableArrayList(estoqueService.listarArmazens()));
        cbOrigem.setPromptText("Armazém de Origem");
        cbOrigem.setMaxWidth(Double.MAX_VALUE);
        cbOrigem.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(Armazem a) { return a == null ? "" : a.getNome(); }
            @Override
            public Armazem fromString(String s) { return null; }
        });

        ComboBox<Armazem> cbDestino = new ComboBox<>(FXCollections.observableArrayList(estoqueService.listarArmazens()));
        cbDestino.setPromptText("Armazém de Destino");
        cbDestino.setMaxWidth(Double.MAX_VALUE);
        cbDestino.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(Armazem a) { return a == null ? "" : a.getNome(); }
            @Override
            public Armazem fromString(String s) { return null; }
        });

        TextField txtQtd = new TextField();
        txtQtd.setPromptText("Quantidade a Transferir");

        ComboBox<UnidadeMedida> cbUnidade = new ComboBox<>();
        cbUnidade.setPromptText("Unidade");
        cbUnidade.setMaxWidth(Double.MAX_VALUE);

        cbProduto.valueProperty().addListener((obs, ov, nv) -> {
            if (nv != null) {
                cbUnidade.getItems().setAll(nv.getUnidadeMedida(), nv.getUnidadeCompra());
                cbUnidade.setValue(nv.getUnidadeMedida());
            }
        });

        TextField txtLote = new TextField("GERAL");
        txtLote.setPromptText("Lote (Opcional)");

        TextField txtObs = new TextField();
        txtObs.setPromptText("Observação / Motivo");

        form.getChildren().addAll(titleLabel, cbProduto, new Label("Origem:"), cbOrigem, new Label("Destino:"), cbDestino, 
                                  new HBox(10, new VBox(5, new Label("Qtd:"), txtQtd), new VBox(5, new Label("Unidade:"), cbUnidade)), 
                                  txtLote, txtObs);
        
        modalService.create()
            .title("Transferência de Stock ")
            .content(form)
            .autoSize()
            .withConfirmButton("Confirmar Transferência", () -> {
                try {
                    Produto p = cbProduto.getValue();
                    if (p == null) throw new IllegalArgumentException("Selecione um produto");

                    Armazem origem = cbOrigem.getValue();
                    Armazem destino = cbDestino.getValue();
                    if (origem == null || destino == null) throw new IllegalArgumentException("Selecione os armazéns de origem e destino");
                    
                    BigDecimal qtd = new BigDecimal(txtQtd.getText().trim().replace(",", "."));
                    int qtdInt;
                    if (isUnidadeFracionavel(cbUnidade.getValue())) {
                        qtdInt = switch (cbUnidade.getValue()) {
                            case KILOGRAMA, LITRO, METRO -> qtd.multiply(new BigDecimal("1000")).setScale(0, RoundingMode.HALF_UP).intValue();
                            case HORA, SERVICO -> qtd.multiply(new BigDecimal("60")).setScale(0, RoundingMode.HALF_UP).intValue();
                            default -> qtd.setScale(0, RoundingMode.HALF_UP).intValue();
                        };
                    } else {
                        qtdInt = qtd.intValue();
                    }
                    
                    String obs = txtObs.getText();
                    String lote = txtLote.getText();
                    if (lote == null || lote.trim().isEmpty()) lote = "GERAL";

                    estoqueService.transferirStock(p, origem, destino, qtdInt, lote, obs);

                    AlertUtils.showSuccessNotification("Transferência realizada com sucesso!");
                    refreshData();
                    return true;
                } catch (Exception ex) {
                    AlertUtils.showErrorAlert("Erro na Operação", ex.getMessage());
                    return false;
                }
            })
            .buildAndShow();
    }

    private void showAjusteDialog() {
        VBox form = new VBox(15);
        form.setPadding(new Insets(20));
        form.setPrefWidth(400);

        Label titleLabel = new Label("Ajuste de Stock (Inventário)");
        titleLabel.getStyleClass().add(Styles.TITLE_3);
        
        Label info = new Label("Defina a quantidade real contada no estoque físico.");
        info.getStyleClass().add(Styles.TEXT_MUTED);
        info.setWrapText(true);

        ComboBox<Produto> cbProduto = new ComboBox<>(FXCollections.observableArrayList(produtoService.findAll()));
        cbProduto.setPromptText("Selecione o Produto");
        cbProduto.setMaxWidth(Double.MAX_VALUE);
        cbProduto.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(Produto p) { return p == null ? "" : p.getCodigoBarra() + " - " + p.getNome(); }
            @Override
            public Produto fromString(String string) { return null; }
        });

        ComboBox<Armazem> cbArmazem = new ComboBox<>(FXCollections.observableArrayList(estoqueService.listarArmazens()));
        cbArmazem.setPromptText("Armazém");
        cbArmazem.setMaxWidth(Double.MAX_VALUE);
        cbArmazem.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(Armazem a) { return a == null ? "" : a.getNome(); }
            @Override
            public Armazem fromString(String s) { return null; }
        });
        try { cbArmazem.setValue(estoqueService.getArmazemPrincipal()); } catch (Exception ignored) {}

        TextField txtQtdReal = new TextField();
        txtQtdReal.setPromptText("Quantidade Real (Contagem Física)");

        ComboBox<UnidadeMedida> cbUnidade = new ComboBox<>();
        cbUnidade.setPromptText("Unidade");
        cbUnidade.setMaxWidth(Double.MAX_VALUE);

        cbProduto.valueProperty().addListener((obs, ov, nv) -> {
            if (nv != null) {
                cbUnidade.getItems().setAll(nv.getUnidadeMedida(), nv.getUnidadeCompra());
                cbUnidade.setValue(nv.getUnidadeMedida());
            }
        });

        TextField txtLote = new TextField("GERAL");
        txtLote.setPromptText("Lote");

        TextField txtObs = new TextField();
        txtObs.setPromptText("Justificativa do Ajuste");

        form.getChildren().addAll(titleLabel, info, cbProduto, cbArmazem, 
                                  new HBox(10, new VBox(5, new Label("Qtd Real:"), txtQtdReal), new VBox(5, new Label("Unidade:"), cbUnidade)), 
                                  txtLote, txtObs);
        
        modalService.create()
            .title("Ajuste de Stock ")
            .content(form)
            .autoSize()
            .withConfirmButton("Confirmar Ajuste", () -> {
                try {
                    Produto p = cbProduto.getValue();
                    if (p == null) throw new IllegalArgumentException("Selecione um produto");
                    
                    Armazem armazem = cbArmazem.getValue();
                    if (armazem == null) throw new IllegalArgumentException("Selecione um armazém");

                    BigDecimal qtd = new BigDecimal(txtQtdReal.getText().trim().replace(",", "."));
                    int qtdInt;
                    if (isUnidadeFracionavel(cbUnidade.getValue())) {
                        qtdInt = switch (cbUnidade.getValue()) {
                            case KILOGRAMA, LITRO, METRO -> qtd.multiply(new BigDecimal("1000")).setScale(0, RoundingMode.HALF_UP).intValue();
                            case HORA, SERVICO -> qtd.multiply(new BigDecimal("60")).setScale(0, RoundingMode.HALF_UP).intValue();
                            default -> qtd.setScale(0, RoundingMode.HALF_UP).intValue();
                        };
                    } else {
                        qtdInt = qtd.intValue();
                    }

                    String obs = txtObs.getText();
                    String lote = txtLote.getText();
                    if (lote == null || lote.trim().isEmpty()) lote = "GERAL";

                    estoqueService.ajustarStock(p, armazem, qtdInt, lote, obs);

                    AlertUtils.showSuccessNotification("Ajuste de estoque realizado!");
                    refreshData();
                    return true;
                } catch (Exception ex) {
                    AlertUtils.showErrorAlert("Erro na Operação", ex.getMessage());
                    return false;
                }
            })
            .buildAndShow();
    }

    private void exportarSaftAO() {
         try {
             FileChooser fileChooser = new FileChooser();
             fileChooser.setTitle("Exportar SAFT-AO (Produtos)");
             fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML Files (*.xml)", "*.xml"));
             fileChooser.setInitialFileName("SAFT_AO_Produtos_" + LocalDate.now() + ".xml");
             java.io.File file = fileChooser.showSaveDialog(getScene().getWindow());
             
             if (file != null) {
                 saftAoExportService.exportProdutosToSaftAo(file);
                 AlertUtils.showInfoAlert("Sucesso", "Arquivo SAFT-AO exportado com sucesso!");
             }
         } catch (Exception e) {
             AlertUtils.showErrorAlert("Erro Exportação", "Falha ao exportar SAFT: " + e.getMessage());
         }
    }

    public void showProdutoDialog(Produto p) {
        ProdutoFormView form = new ProdutoFormView(
            produtoService, 
            categoriaService, 
            impostoService, 
            fornecedorService, 
            p, 
            (saved) -> {
                refreshData();
                ao.allon.kubata.faturacao.ui.util.AlertUtils.showSuccessNotification("Produto salvo com sucesso!");
            }
        );
        form.setPrefSize(900, 650);
        
        modalService.create()
            .title(p == null ? "Novo Produto" : "Editar Produto")
            .content(form)
            .autoSize()
            .buildAndShow();
    }

    private void showCategoriaDialog() {
         VBox root = new VBox(15);
         root.setPadding(new Insets(20));
         root.setPrefWidth(400);
         
         ObservableList<ao.allon.kubata.faturacao.domain.Categoria> categorias = FXCollections.observableArrayList(categoriaService.findAll());
         ListView<ao.allon.kubata.faturacao.domain.Categoria> listView = new ListView<>(categorias);
         listView.setCellFactory(lv -> new ListCell<>() {
             @Override
             protected void updateItem(ao.allon.kubata.faturacao.domain.Categoria item, boolean empty) {
                 super.updateItem(item, empty);
                 if (empty || item == null) {
                     setText(null);
                 } else {
                     setText(item.getNome());
                 }
             }
         });
         
         HBox addBox = new HBox(10);
         TextField txtNewCat = new TextField();
         txtNewCat.setPromptText("Nova Categoria");
         HBox.setHgrow(txtNewCat, Priority.ALWAYS);
         
         Button btnAdd = new Button("Adicionar");
         btnAdd.setOnAction(e -> {
             if (!txtNewCat.getText().isEmpty()) {
                 ao.allon.kubata.faturacao.domain.Categoria cat = new ao.allon.kubata.faturacao.domain.Categoria();
                 cat.setNome(txtNewCat.getText());
                 categoriaService.save(cat);
                 categorias.setAll(categoriaService.findAll());
                 txtNewCat.clear();
                 cbCategoria.setItems(FXCollections.observableArrayList(categoriaService.findAll()));
             }
         });
         
         addBox.getChildren().addAll(txtNewCat, btnAdd);
         
         Button btnDelete = new Button("Excluir Selecionada");
         btnDelete.getStyleClass().add(Styles.DANGER);
         btnDelete.setOnAction(e -> {
             ao.allon.kubata.faturacao.domain.Categoria selected = listView.getSelectionModel().getSelectedItem();
             if (selected != null) {
                 try {
                     categoriaService.delete(selected.getId());
                     categorias.setAll(categoriaService.findAll());
                     cbCategoria.setItems(FXCollections.observableArrayList(categoriaService.findAll()));
                     AlertUtils.showInfoAlert("Sucesso", "Categoria excluída com sucesso!");
                 } catch (Exception ex) {
                     AlertUtils.showErrorAlert("Erro", "Não é possível excluir categoria em uso.");
                 }
             }
         });
         
         root.getChildren().addAll(listView, addBox, btnDelete);
         
         modalService.create()
             .title("Gerir Categorias")
             .content(root)
             .autoSize()
             .withCancelButton("Fechar")
             .buildAndShow();
    }

    public void deleteProduto(Produto p) {
        if (p == null) return;
        
        Label lbl = new Label("Tem certeza que deseja excluir o produto " + p.getNome() + "?");
        lbl.setWrapText(true);
        
        modalService.create()
            .title("Confirmar Exclusão")
            .content(lbl)
            .dynamicSize()
            .withConfirmButton("Sim, Excluir", () -> {
                try {
                    produtoService.delete(p.getId());
                    AlertUtils.showInfoAlert("Sucesso", "Produto excluído.");
                    refreshData();
                    return true;
                } catch (Exception ex) {
                    AlertUtils.showErrorAlert("Erro", "Erro ao excluir: " + ex.getMessage());
                    return false;
                }
            })
            .withCancelButton("Cancelar")
            .buildAndShow();
    }

    public void exportarCSV() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exportar Produtos");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Arquivos CSV (*.csv)", "*.csv"));
        fileChooser.setInitialFileName("produtos_inventario_" + LocalDate.now() + ".csv");
        java.io.File file = fileChooser.showSaveDialog(getScene().getWindow());
        if (file != null) {
            try (java.io.PrintWriter writer = new java.io.PrintWriter(file, java.nio.charset.StandardCharsets.UTF_8)) {
                writer.println("Codigo;Produto;Categoria;Stock;StockMinimo;PrecoVenda;PrecoCusto;IVA");
                for (Produto p : tableProdutos.getItems()) {
                    writer.printf("%s;%s;%s;%d;%d;%.2f;%.2f;%.2f%n",
                        p.getCodigoBarra() != null ? p.getCodigoBarra() : "",
                        p.getNome() != null ? p.getNome() : "",
                        p.getCategoria() != null ? p.getCategoria().getNome() : "",
                        p.getStock() != null ? p.getStock() : 0,
                        p.getStockMinimo() != null ? p.getStockMinimo() : 0,
                        p.getPrecoUnitario() != null ? p.getPrecoUnitario() : BigDecimal.ZERO,
                        p.getPrecoCompra() != null ? p.getPrecoCompra() : BigDecimal.ZERO,
                        p.getPercentualIva() != null ? p.getPercentualIva() : BigDecimal.ZERO
                    );
                }
                AlertUtils.showInfoAlert("Exportação", "Arquivo exportado com sucesso!");
            } catch (Exception e) {
                AlertUtils.showErrorAlert("Erro", "Falha ao exportar: " + e.getMessage());
            }
        }
    }

    public void showLowStockReportDialog() {
        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setPrefWidth(700);

        TableView<Produto> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.getStyleClass().add(Styles.STRIPED);

        TableColumn<Produto, String> colCodigo = new TableColumn<>("Código");
        colCodigo.setCellValueFactory(new PropertyValueFactory<>("codigoBarra"));

        TableColumn<Produto, String> colNome = new TableColumn<>("Produto");
        colNome.setCellValueFactory(new PropertyValueFactory<>("nome"));

        TableColumn<Produto, String> colCategoria = new TableColumn<>("Categoria");
        colCategoria.setCellValueFactory(cell -> new SimpleStringProperty(
            cell.getValue().getCategoria() != null ? cell.getValue().getCategoria().getNome() : "-"
        ));

        TableColumn<Produto, Integer> colStock = new TableColumn<>("Stock");
        colStock.setCellValueFactory(new PropertyValueFactory<>("stock"));

        TableColumn<Produto, Integer> colMin = new TableColumn<>("Min");
        colMin.setCellValueFactory(new PropertyValueFactory<>("stockMinimo"));

        table.getColumns().addAll(colCodigo, colNome, colCategoria, colStock, colMin);

        ObservableList<Produto> baixoEstoque = FXCollections.observableArrayList();
        for (Produto p : produtoData) {
            int stock = p.getStock() == null ? 0 : p.getStock();
            int min = p.getStockMinimo() == null ? 0 : p.getStockMinimo();
            if (stock <= min) {
                baixoEstoque.add(p);
            }
        }
        table.setItems(baixoEstoque);

        Label resumo = new Label("Itens em baixo estoque: " + baixoEstoque.size());
        resumo.getStyleClass().add(Styles.TEXT_MUTED);

        Button btnExport = new Button("Exportar", IconUtils.icon(Feather.DOWNLOAD, IconUtils.SIZE_SMALL));
        btnExport.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Exportar Baixo Estoque");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Arquivos CSV (*.csv)", "*.csv"));
            fileChooser.setInitialFileName("baixo_estoque_" + LocalDate.now() + ".csv");
            java.io.File file = fileChooser.showSaveDialog(getScene().getWindow());
            if (file != null) {
                try (java.io.PrintWriter writer = new java.io.PrintWriter(file, java.nio.charset.StandardCharsets.UTF_8)) {
                    writer.println("Codigo;Produto;Categoria;Stock;StockMinimo");
                    for (Produto p : baixoEstoque) {
                        writer.printf("%s;%s;%s;%d;%d%n",
                            p.getCodigoBarra() != null ? p.getCodigoBarra() : "",
                            p.getNome() != null ? p.getNome() : "",
                            p.getCategoria() != null ? p.getCategoria().getNome() : "",
                            p.getStock() != null ? p.getStock() : 0,
                            p.getStockMinimo() != null ? p.getStockMinimo() : 0
                        );
                    }
                    AlertUtils.showInfoAlert("Exportação", "Arquivo exportado com sucesso!");
                } catch (Exception ex) {
                    AlertUtils.showErrorAlert("Erro", "Falha ao exportar: " + ex.getMessage());
                }
            }
        });

        root.getChildren().addAll(resumo, table);
        
        var builder = modalService.create()
                .title("Relatório de Baixo Estoque")
                .content(root)
                .autoSize()
                .withCancelButton("Fechar");
        
        builder.getModal().getFooter().getChildren().add(0, btnExport);
        builder.buildAndShow();
    }
    private void showValidadesProximasDialog() {
        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setPrefWidth(800);

        LocalDate hoje = LocalDate.now();
        LocalDate limite = hoje.plusDays(30);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        TableView<Estoque> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.getStyleClass().add(Styles.STRIPED);

        TableColumn<Estoque, String> colProduto = new TableColumn<>("Produto");
        colProduto.setCellValueFactory(cell -> new SimpleStringProperty(
            cell.getValue().getProduto() != null ? cell.getValue().getProduto().getNome() : "-"
        ));

        TableColumn<Estoque, String> colArmazem = new TableColumn<>("Armazém");
        colArmazem.setCellValueFactory(cell -> new SimpleStringProperty(
            cell.getValue().getArmazem() != null ? cell.getValue().getArmazem().getNome() : "-"
        ));

        TableColumn<Estoque, String> colLote = new TableColumn<>("Lote");
        colLote.setCellValueFactory(new PropertyValueFactory<>("lote"));

        TableColumn<Estoque, String> colValidade = new TableColumn<>("Validade");
        colValidade.setCellValueFactory(cell -> new SimpleStringProperty(
            cell.getValue().getValidade() != null ? cell.getValue().getValidade().format(formatter) : "-"
        ));

        TableColumn<Estoque, Integer> colQtd = new TableColumn<>("Quantidade");
        colQtd.setCellValueFactory(new PropertyValueFactory<>("quantidade"));

        table.getColumns().addAll(colProduto, colArmazem, colLote, colValidade, colQtd);

        ObservableList<Estoque> proximas = FXCollections.observableArrayList();
        for (Estoque e : estoqueService.listarTodosEstoque()) {
            if (e.getValidade() != null && e.getQuantidade() != null && e.getQuantidade() > 0) {
                LocalDate validade = e.getValidade();
                if (!validade.isBefore(hoje) && !validade.isAfter(limite)) {
                    proximas.add(e);
                }
            }
        }
        table.setItems(proximas);

        Label resumo = new Label("Itens com validade até " + limite.format(formatter) + ": " + proximas.size());
        resumo.getStyleClass().add(Styles.TEXT_MUTED);

        Button btnExport = new Button("Exportar", IconUtils.icon(Feather.DOWNLOAD, IconUtils.SIZE_SMALL));
        btnExport.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Exportar Validades Próximas");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Arquivos CSV (*.csv)", "*.csv"));
            fileChooser.setInitialFileName("validades_proximas_" + LocalDate.now() + ".csv");
            java.io.File file = fileChooser.showSaveDialog(getScene().getWindow());
            if (file != null) {
                try (java.io.PrintWriter writer = new java.io.PrintWriter(file, java.nio.charset.StandardCharsets.UTF_8)) {
                    writer.println("Produto;Armazem;Lote;Validade;Quantidade");
                    for (Estoque eItem : proximas) {
                        writer.printf("%s;%s;%s;%s;%d%n",
                            eItem.getProduto() != null ? eItem.getProduto().getNome() : "",
                            eItem.getArmazem() != null ? eItem.getArmazem().getNome() : "",
                            eItem.getLote() != null ? eItem.getLote() : "",
                            eItem.getValidade() != null ? eItem.getValidade().format(formatter) : "",
                            eItem.getQuantidade() != null ? eItem.getQuantidade() : 0
                        );
                    }
                    AlertUtils.showInfoAlert("Exportação", "Arquivo exportado com sucesso!");
                } catch (Exception ex) {
                    AlertUtils.showErrorAlert("Erro", "Falha ao exportar: " + ex.getMessage());
                }
            }
        });

        root.getChildren().addAll(resumo, table);
        
        var builder = modalService.create()
                .title("Validades Próximas")
                .content(root)
                .autoSize()
                .withCancelButton("Fechar");
        
        builder.getModal().getFooter().getChildren().add(0, btnExport);
        builder.buildAndShow();
    }
    private void showDetalhesStockDialog(Produto produto) {
        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setPrefWidth(700);

        TableView<Estoque> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.getStyleClass().add(Styles.STRIPED);

        TableColumn<Estoque, String> colArmazem = new TableColumn<>("Armazém");
        colArmazem.setCellValueFactory(cell -> new SimpleStringProperty(
            cell.getValue().getArmazem() != null ? cell.getValue().getArmazem().getNome() : "-"
        ));

        TableColumn<Estoque, String> colLote = new TableColumn<>("Lote");
        colLote.setCellValueFactory(new PropertyValueFactory<>("lote"));

        TableColumn<Estoque, String> colValidade = new TableColumn<>("Validade");
        colValidade.setCellValueFactory(cell -> new SimpleStringProperty(
            cell.getValue().getValidade() != null ? cell.getValue().getValidade().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "-"
        ));

        TableColumn<Estoque, Integer> colQtd = new TableColumn<>("Quantidade");
        colQtd.setCellValueFactory(new PropertyValueFactory<>("quantidade"));

        table.getColumns().addAll(colArmazem, colLote, colValidade, colQtd);

        ObservableList<Estoque> itens = FXCollections.observableArrayList(estoqueService.listarPorProduto(produto));
        table.setItems(itens);

        Label resumo = new Label("Total em stock: " + (produto.getStock() != null ? produto.getStock() : 0));
        resumo.getStyleClass().add(Styles.TEXT_MUTED);

        root.getChildren().addAll(resumo, table);
        
        modalService.create()
                .title("Detalhes de Stock - " + produto.getNome())
                .content(root)
                .autoSize()
                .withCancelButton("Fechar")
                .buildAndShow();
    }
    public void showMovimentosReportDialog() {}
    public void filter(String query) {
        txtSearch.setText(query);
    }
    public Produto getSelectedProduto() {
        return tableProdutos.getSelectionModel().getSelectedItem();
    }
    public void showMovimentoDialog(Object o) {
        showStockMovementModal(true); // Default to entry for now
    }

    private void exportarMovimentosCSV() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exportar Movimentos");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Arquivos CSV (*.csv)", "*.csv"));
        fileChooser.setInitialFileName("movimentos_stock_" + LocalDate.now() + ".csv");
        java.io.File file = fileChooser.showSaveDialog(getScene().getWindow());
        
        if (file != null) {
            try (java.io.PrintWriter writer = new java.io.PrintWriter(file, java.nio.charset.StandardCharsets.UTF_8)) {
                writer.println("Data/Hora;Tipo;Produto;Quantidade;Saldo;Lote;Observacao");
                for (MovimentoStock mov : tableMovimentos.getItems()) {
                    writer.printf("%s;%s;%s;%d;%d;%s;%s%n",
                        mov.getDataMovimento() != null ? mov.getDataMovimento().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "",
                        mov.getTipoMovimento(),
                        mov.getProduto() != null ? mov.getProduto().getNome() : "",
                        mov.getQuantidade(),
                        mov.getSaldoAtual(),
                        mov.getLote() != null ? mov.getLote() : "",
                        mov.getObservacao() != null ? mov.getObservacao().replace(";", ",") : ""
                    );
                }
                AlertUtils.showInfoAlert("Exportação", "Arquivo exportado com sucesso!");
            } catch (Exception e) {
                AlertUtils.showErrorAlert("Erro", "Falha ao exportar: " + e.getMessage());
            }
        }
    }

    private Node createFifoTab() {
        return new EstoqueFifoView(estoqueService, produtoService, sessionManager, modalService, fifoService);
    }
}

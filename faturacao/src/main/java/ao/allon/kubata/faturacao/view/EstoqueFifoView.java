package ao.allon.kubata.faturacao.view;

import ao.allon.kubata.core.ui.table.AdvancedTableView;
import ao.allon.kubata.core.ui.table.TableUtils;
import ao.allon.kubata.faturacao.domain.Armazem;
import ao.allon.kubata.faturacao.domain.Produto;
import ao.allon.kubata.faturacao.service.EstoqueService;
import ao.allon.kubata.faturacao.service.FifoService;
import ao.allon.kubata.faturacao.service.ProdutoService;
import ao.allon.kubata.faturacao.service.SessionManager;
import ao.allon.kubata.faturacao.ui.modal.ModalService;
import ao.allon.kubata.faturacao.ui.util.AlertUtils;
import ao.allon.kubata.faturacao.ui.util.IconUtils;
import ao.allon.kubata.faturacao.util.Money;
import atlantafx.base.controls.Card;
import atlantafx.base.controls.CustomTextField;
import atlantafx.base.controls.Tile;
import atlantafx.base.theme.Styles;
import atlantafx.base.theme.Tweaks;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.io.FileWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * View profissional e moderna para gestão FIFO de estoque.
 * Redesenhada com AtlantaFX e GemsFX.
 */
public class EstoqueFifoView extends BorderPane {

    private final EstoqueService estoqueService;
    private final ProdutoService produtoService;
    private final SessionManager sessionManager;
    private final ModalService modalService;
    private final FifoService fifoService;

    private final AdvancedTableView<FifoLoteRow> table = new AdvancedTableView<>();
    private final ObservableList<FifoLoteRow> masterData = FXCollections.observableArrayList();
    private final DateTimeFormatter df = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // UI Components
    private ComboBox<Armazem> cbArmazemFilter;
    private Label lblTotalItens;
    private Label lblValorTotal;
    private Label lblLotesVencidos;
    private Label lblLotesProximos;
    private PieChart stockChart;
    private VBox notificationPanel;

    public EstoqueFifoView(EstoqueService estoqueService,
                           ProdutoService produtoService,
                           SessionManager sessionManager,
                           ModalService modalService,
                           FifoService fifoService) {
        this.estoqueService = estoqueService;
        this.produtoService = produtoService;
        this.sessionManager = sessionManager;
        this.modalService = modalService;
        this.fifoService = fifoService;

        getStyleClass().add("estoque-fifo-view");
        setPadding(new Insets(20));

        // Layout Principal
        setTop(createTopDashboard());
        setCenter(createMainContent());
        
        // Carregar dados
        loadData();
    }

    private Node createTopDashboard() {
        HBox dashboard = new HBox(20);
        dashboard.setAlignment(Pos.CENTER_LEFT);
        dashboard.setPadding(new Insets(0, 0, 20, 0));

        // KPI Cards
        Card kpiTotal = createKpiTile("Total em Estoque", "0 Unidades", Feather.PACKAGE, Styles.ACCENT);
        lblTotalItens = (Label) ((HBox) kpiTotal.getBody()).getChildren().get(1);
        
        Card kpiValor = createKpiTile("Valor Estimado", Money.formatAOA(BigDecimal.ZERO), Feather.DOLLAR_SIGN, Styles.SUCCESS);
        lblValorTotal = (Label) ((HBox) kpiValor.getBody()).getChildren().get(1);
        
        Card kpiVencidos = createKpiTile("Lotes Vencidos", "0 Lotes", Feather.ALERT_TRIANGLE, Styles.DANGER);
        lblLotesVencidos = (Label) ((HBox) kpiVencidos.getBody()).getChildren().get(1);
        
        Card kpiProximos = createKpiTile("A Vencer (30d)", "0 Lotes", Feather.CLOCK, Styles.WARNING);
        lblLotesProximos = (Label) ((HBox) kpiProximos.getBody()).getChildren().get(1);

        // Chart (Resumo Visual)
        stockChart = new PieChart();
        stockChart.setLabelsVisible(false);
        stockChart.setMaxSize(200, 100);
        
        VBox chartBox = new VBox(new Label("Status Estoque"), stockChart);
        chartBox.setAlignment(Pos.CENTER);
        chartBox.getStyleClass().add(Styles.ELEVATED_1);
        chartBox.setStyle("-fx-background-color: -color-bg-default; -fx-background-radius: 8; -fx-padding: 10;");

        HBox.setHgrow(kpiTotal, Priority.ALWAYS);
        HBox.setHgrow(kpiValor, Priority.ALWAYS);
        HBox.setHgrow(kpiVencidos, Priority.ALWAYS);
        HBox.setHgrow(kpiProximos, Priority.ALWAYS);

        dashboard.getChildren().addAll(kpiTotal, kpiValor, kpiVencidos, kpiProximos, chartBox);
        return dashboard;
    }

    private Card createKpiTile(String title, String value, Feather icon, String colorStyle) {
        Card card = new Card();
        card.getStyleClass().add(Styles.ELEVATED_1);
        card.setHeader(new Label(title));
        
        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().addAll(Styles.TITLE_3, colorStyle);
        
        FontIcon iconNode = new FontIcon(icon);
        iconNode.setIconSize(32);
        iconNode.getStyleClass().add(colorStyle);
        
        HBox content = new HBox(15, iconNode, valueLabel);
        content.setAlignment(Pos.CENTER_LEFT);
        content.setPadding(new Insets(10));
        
        card.setBody(content);
        card.setPrefWidth(200);
        return card;
    }

    private Node createMainContent() {
        VBox container = new VBox(15);
        VBox.setVgrow(container, Priority.ALWAYS);

        // Toolbar
        HBox toolbar = new HBox(15);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        // Filtros
        cbArmazemFilter = new ComboBox<>();
        cbArmazemFilter.setPromptText("Todos os Armazéns");
        cbArmazemFilter.getItems().addAll(estoqueService.listarArmazens());
        cbArmazemFilter.setPrefWidth(200);
        cbArmazemFilter.setOnAction(e -> updateFilter());
        
        // Search Field (GemsFX or Custom)
        // Usando CustomTextField simulando search, pois GemsFX SearchField requer SuggestionProvider complexo
        CustomTextField txtSearch = new CustomTextField();
        txtSearch.setPromptText("Buscar produto, lote ou código...");
        txtSearch.setLeft(new FontIcon(Feather.SEARCH));
        txtSearch.setPrefWidth(300);
        txtSearch.textProperty().addListener((obs, old, newVal) -> {
            updateFilter(newVal);
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Actions
        Button btnRefresh = new Button("", new FontIcon(Feather.REFRESH_CW));
        btnRefresh.getStyleClass().addAll(Styles.BUTTON_ICON);
        btnRefresh.setTooltip(new Tooltip("Atualizar Dados"));
        btnRefresh.setOnAction(e -> loadData());

        Button btnExport = new Button("Exportar", new FontIcon(Feather.DOWNLOAD));
        btnExport.getStyleClass().addAll(Styles.BUTTON_OUTLINED);
        btnExport.setOnAction(e -> exportData());

        Button btnBarcode = new Button("Scan", new FontIcon(Feather.MAXIMIZE));
        btnBarcode.getStyleClass().addAll(Styles.BUTTON_OUTLINED);
        btnBarcode.setTooltip(new Tooltip("Ler Código de Barras"));
        btnBarcode.setOnAction(e -> {
            AlertUtils.showInfoAlert("Leitor", "Aguardando leitura do scanner... (Foco no campo de busca)");
            txtSearch.requestFocus();
        });

        Button btnConsume = new Button("Consumir", new FontIcon(Feather.MINUS_CIRCLE));
        btnConsume.getStyleClass().addAll(Styles.DANGER);
        btnConsume.setOnAction(e -> showConsumirModal());
        
        Button btnAddStock = new Button("Entrada de Lote", new FontIcon(Feather.PLUS_CIRCLE));
        btnAddStock.getStyleClass().addAll(Styles.SUCCESS);
        btnAddStock.setOnAction(e -> showEntradaLoteModal());
        
        boolean canEdit = sessionManager.hasAccess("ESTOQUE", "Editar");
        btnConsume.setDisable(!canEdit);
        btnAddStock.setDisable(!canEdit);

        toolbar.getChildren().addAll(
            new Label("Filtros:"), cbArmazemFilter, txtSearch, 
            spacer, 
            btnRefresh, btnBarcode, btnExport, btnAddStock, btnConsume
        );

        // Table
        setupTable();
        
        // Notification Panel (Simulated)
        notificationPanel = new VBox(5);
        notificationPanel.setPadding(new Insets(10));
        notificationPanel.setStyle("-fx-background-color: -color-bg-subtle; -fx-background-radius: 4;");
        Label lblNotif = new Label("Notificações do Sistema");
        lblNotif.getStyleClass().add(Styles.TEXT_BOLD);
        notificationPanel.getChildren().add(lblNotif);
        notificationPanel.setVisible(false);
        notificationPanel.setManaged(false);

        container.getChildren().addAll(toolbar, table, notificationPanel);
        return container;
    }

    private void setupTable() {
        TableUtils.standardize(table);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        // Styles.HOVER removed if not available in this version
        table.getStyleClass().addAll(Styles.STRIPED, Tweaks.EDGE_TO_EDGE);

        TableColumn<FifoLoteRow, String> colProduto = new TableColumn<>("Produto");
        colProduto.setCellValueFactory(c -> c.getValue().produtoProperty());
        colProduto.setMinWidth(200);

        TableColumn<FifoLoteRow, String> colArmazem = new TableColumn<>("Armazém");
        colArmazem.setCellValueFactory(c -> c.getValue().armazemProperty());

        TableColumn<FifoLoteRow, String> colLote = new TableColumn<>("Lote");
        colLote.setCellValueFactory(c -> c.getValue().loteProperty());
        colLote.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item);
                    setGraphic(new FontIcon(Feather.TAG));
                    setContentDisplay(ContentDisplay.LEFT);
                    setGraphicTextGap(8);
                }
            }
        });

        TableColumn<FifoLoteRow, Integer> colQtd = new TableColumn<>("Qtd.");
        colQtd.setCellValueFactory(c -> c.getValue().quantidadeProperty());
        colQtd.getStyleClass().add(Styles.RIGHT);

        TableColumn<FifoLoteRow, String> colEntrada = new TableColumn<>("Entrada");
        colEntrada.setCellValueFactory(c -> c.getValue().dataEntradaProperty());

        TableColumn<FifoLoteRow, String> colValidade = new TableColumn<>("Validade");
        colValidade.setCellValueFactory(c -> c.getValue().validadeProperty());

        TableColumn<FifoLoteRow, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(c -> c.getValue().validadeProperty());
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    try {
                        LocalDate v = LocalDate.parse(item, df);
                        Label badge = new Label();
                        badge.getStyleClass().addAll("badge", Styles.TEXT_SMALL);
                        
                        if (v.isBefore(LocalDate.now())) {
                            badge.setText("VENCIDO");
                            badge.getStyleClass().add(Styles.DANGER);
                        } else if (v.isBefore(LocalDate.now().plusDays(30))) {
                            badge.setText("CRÍTICO");
                            badge.getStyleClass().add(Styles.WARNING);
                        } else {
                            badge.setText("OK");
                            badge.getStyleClass().add(Styles.SUCCESS);
                        }
                        setGraphic(badge);
                        setText(null);
                    } catch (Exception e) {
                        setText("-");
                    }
                }
            }
        });

        TableColumn<FifoLoteRow, String> colPreco = new TableColumn<>("Preço Venda");
        colPreco.setCellValueFactory(c -> c.getValue().precoVendaProperty());
        colPreco.getStyleClass().add(Styles.RIGHT);

        table.getColumns().addAll(colProduto, colArmazem, colLote, colQtd, colEntrada, colValidade, colStatus, colPreco);
        
        // Sorting is handled by AdvancedTableView.setData()
    }

    private void loadData() {
        masterData.clear();
        List<Produto> produtos = produtoService.findAll();
        for (Produto p : produtos) {
            List<FifoService.ResumoLoteFifoDto> lotes = fifoService.resumoPorProduto(p);
            for (FifoService.ResumoLoteFifoDto dto : lotes) {
                masterData.add(new FifoLoteRow(p, null, dto));
            }
        }
        table.setData(masterData);
        updateDashboard();
    }

    private void updateFilter() {
        updateFilter(null);
    }

    private void updateFilter(String search) {
        Armazem armazem = cbArmazemFilter.getValue();

        table.setFilter(row -> {
            if (armazem != null && (row.getArmazemId() == null || !row.getArmazemId().equals(armazem.getId()))) {
                return false;
            }
            if (search != null && !search.isEmpty()) {
                String q = search.toLowerCase();
                boolean match = row.getProduto().getNome().toLowerCase().contains(q) ||
                                row.getLote().toLowerCase().contains(q) ||
                                row.getProduto().getCodigoBarra().toLowerCase().contains(q);
                if (!match) return false;
            }
            return true;
        });

        updateDashboard();
    }

    private void updateDashboard() {
        // Calculate KPIs based on filtered data (or master data if preferred)
        var data = table.getItems();
        
        int totalUnidades = data.stream().mapToInt(FifoLoteRow::getQuantidade).sum();
        BigDecimal totalValor = data.stream()
            .map(r -> r.dto.precoVenda() != null ? r.dto.precoVenda().multiply(BigDecimal.valueOf(r.getQuantidade())) : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        long vencidos = data.stream().filter(r -> {
            try { return LocalDate.parse(r.getValidade(), df).isBefore(LocalDate.now()); } catch(Exception e) { return false; }
        }).count();
        
        long proximos = data.stream().filter(r -> {
            try {
                LocalDate d = LocalDate.parse(r.getValidade(), df);
                return !d.isBefore(LocalDate.now()) && d.isBefore(LocalDate.now().plusDays(30));
            } catch(Exception e) { return false; }
        }).count();

        lblTotalItens.setText(totalUnidades + " Unidades");
        lblValorTotal.setText(Money.formatAOA(totalValor));
        lblLotesVencidos.setText(vencidos + " Lotes");
        lblLotesProximos.setText(proximos + " Lotes");

        // Update Chart
        ObservableList<PieChart.Data> chartData = FXCollections.observableArrayList(
            new PieChart.Data("OK", data.size() - vencidos - proximos),
            new PieChart.Data("Crítico", proximos),
            new PieChart.Data("Vencido", vencidos)
        );
        stockChart.setData(chartData);
        
        // Show notification if critical
        if (vencidos > 0) {
            notificationPanel.setVisible(true);
            notificationPanel.setManaged(true);
            if (notificationPanel.getChildren().size() > 1) notificationPanel.getChildren().remove(1); // clear old
            Label msg = new Label("Atenção: Existem " + vencidos + " lotes vencidos que não devem ser vendidos.");
            msg.getStyleClass().add(Styles.DANGER);
            notificationPanel.getChildren().add(msg);
        } else {
            notificationPanel.setVisible(false);
            notificationPanel.setManaged(false);
        }
    }

    private void showConsumirModal() {
        FifoLoteRow selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtils.showWarningAlert("Seleção Necessária", "Selecione um lote na tabela para consumir.");
            return;
        }

        TextField txtQtd = new TextField();
        txtQtd.setPromptText("Qtd.");

        ComboBox<ao.allon.kubata.faturacao.enums.UnidadeMedida> cbUnidade = new ComboBox<>();
        cbUnidade.setPromptText("Unidade");
        cbUnidade.getItems().setAll(selected.getProduto().getUnidadeMedida(), selected.getProduto().getUnidadeCompra());
        cbUnidade.setValue(selected.getProduto().getUnidadeMedida());
        
        TextArea txtObs = new TextArea();
        txtObs.setPromptText("Motivo / Observação");
        txtObs.setPrefRowCount(3);

        VBox form = new VBox(10,
            new Label("Produto: " + selected.getProduto().getNome()),
            new Label("Lote: " + selected.getLote()),
            new Label("Disponível: " + selected.getQuantidade()),
            new Separator(),
            new HBox(10, new VBox(5, new Label("Quantidade a Consumir:"), txtQtd), new VBox(5, new Label("Unidade:"), cbUnidade)),
            new Label("Observação:"), txtObs
        );
        form.setPadding(new Insets(10));

        modalService.create()
                .title("Consumo Manual (FIFO) Profissional")
                .content(form)
                .autoSize()
                .withConfirmButton("Confirmar", () -> {
                    try {
                        BigDecimal qtd = new BigDecimal(txtQtd.getText().trim().replace(",", "."));
                        int qtdInt;
                        
                        // Lógica de conversão profissional
                        ao.allon.kubata.faturacao.enums.UnidadeMedida u = cbUnidade.getValue();
                        Produto p = selected.getProduto();
                        
                        if (u != null) {
                            qtdInt = switch (u) {
                                case KILOGRAMA, LITRO, METRO -> qtd.multiply(new BigDecimal("1000")).setScale(0, RoundingMode.HALF_UP).intValue();
                                case HORA, SERVICO -> qtd.multiply(new BigDecimal("60")).setScale(0, RoundingMode.HALF_UP).intValue();
                                default -> qtd.setScale(0, RoundingMode.HALF_UP).intValue();
                            };
                        } else {
                            qtdInt = qtd.intValue();
                        }

                        String obs = txtObs.getText();
                        fifoService.consumirPorFIFO(p, qtdInt, obs);
                        AlertUtils.showInfoAlert("Sucesso", "Estoque consumido com sucesso.");
                        loadData();
                        return true;
                    } catch (NumberFormatException e) {
                        AlertUtils.showWarningAlert("Erro", "Quantidade inválida.");
                        return false;
                    } catch (Exception e) {
                        AlertUtils.showExceptionAlert("Erro", "Falha ao consumir estoque.", e);
                        return false;
                    }
                })
                .withCancelButton("Cancelar")
                .buildAndShow();
    }

    private void showEntradaLoteModal() {
        ComboBox<Produto> cbProd = new ComboBox<>();
        cbProd.setPromptText("Selecione o Produto");
        cbProd.getItems().addAll(produtoService.findAll());
        cbProd.setPrefWidth(350);
        
        ComboBox<Armazem> cbArm = new ComboBox<>();
        cbArm.setPromptText("Armazém de Entrada");
        cbArm.getItems().addAll(estoqueService.listarArmazens());
        cbArm.setPrefWidth(350);
        // Selecionar principal por padrão
        cbArm.getItems().stream().filter(Armazem::getIsPrincipal).findFirst().ifPresent(cbArm::setValue);

        ComboBox<ao.allon.kubata.faturacao.enums.UnidadeMedida> cbUnidade = new ComboBox<>();
        cbUnidade.setPromptText("Unidade");
        cbUnidade.setPrefWidth(150);

        TextField txtQtd = new TextField();
        txtQtd.setPromptText("Quantidade");
        
        TextField txtCustoEntrada = new TextField();
        txtCustoEntrada.setPromptText("Custo Unitário de Compra");
        
        TextField txtNovoPrecoVenda = new TextField();
        txtNovoPrecoVenda.setPromptText("Novo Preço Venda (Opcional)");

        Label lblSugestao = new Label("");
        lblSugestao.getStyleClass().add(Styles.TEXT_SMALL);
        lblSugestao.setStyle("-fx-text-fill: -color-accent-fg;");

        cbProd.valueProperty().addListener((obs, ov, nv) -> {
            if (nv != null) {
                cbUnidade.getItems().setAll(nv.getUnidadeMedida(), nv.getUnidadeCompra());
                cbUnidade.setValue(nv.getUnidadeCompra() != null ? nv.getUnidadeCompra() : nv.getUnidadeMedida());
                txtCustoEntrada.setText(nv.getPrecoCompra() != null ? nv.getPrecoCompra().toString() : "");
                txtNovoPrecoVenda.setText(nv.getPrecoUnitario() != null ? nv.getPrecoUnitario().toString() : "");
            }
        });

        // Calculadora de Margem Sugerida
        txtCustoEntrada.textProperty().addListener((obs, ov, nv) -> {
            try {
                if (nv != null && !nv.isEmpty() && cbProd.getValue() != null) {
                    BigDecimal custo = new BigDecimal(nv.replace(",", "."));
                    Produto p = cbProd.getValue();
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

        TextField txtLote = new TextField();
        txtLote.setPromptText("Número do Lote (Opcional)");
        
        DatePicker dpValidade = new DatePicker();
        dpValidade.setPromptText("Data de Validade");
        
        TextArea txtObs = new TextArea();
        txtObs.setPromptText("Observações (Ex: Compra ref. FT 123)");
        txtObs.setPrefRowCount(2);

        VBox form = new VBox(10,
            new Label("Produto:*"), cbProd,
            new Label("Armazém:*"), cbArm,
            new HBox(10, new VBox(5, new Label("Quantidade:*"), txtQtd), new VBox(5, new Label("Unidade:"), cbUnidade)),
            new VBox(5, new Label("Custo Unitário de Entrada:"), txtCustoEntrada, lblSugestao),
            new VBox(5, new Label("Novo Preço de Venda:"), txtNovoPrecoVenda),
            new HBox(10, new VBox(5, new Label("Lote:"), txtLote), new VBox(5, new Label("Validade:"), dpValidade)),
            new Label("Observação:"), txtObs
        );
        form.setPadding(new Insets(10));

        modalService.create()
                .title("Entrada de Lote ")
                .content(form)
                .autoSize()
                .withConfirmButton("Registrar Entrada", () -> {
                    try {
                        Produto p = cbProd.getValue();
                        Armazem a = cbArm.getValue();
                        if (p == null || a == null) {
                            AlertUtils.showWarningAlert("Dados Incompletos", "Selecione o produto e o armazém.");
                            return false;
                        }
                        
                        BigDecimal qtd = new BigDecimal(txtQtd.getText().trim().replace(",", "."));
                        BigDecimal custo = new BigDecimal(txtCustoEntrada.getText().trim().replace(",", "."));
                        BigDecimal novoVenda = txtNovoPrecoVenda.getText().isBlank() ? null : new BigDecimal(txtNovoPrecoVenda.getText().trim().replace(",", "."));
                        
                        String lote = txtLote.getText();
                        if (lote == null || lote.trim().isEmpty()) {
                            lote = "LOTE-" + java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd").format(java.time.LocalDate.now());
                        }
                        
                        estoqueService.adicionarStockComConversao(p, a, qtd, cbUnidade.getValue(), custo, novoVenda, lote, dpValidade.getValue(), txtObs.getText(), null);
                        
                        AlertUtils.showInfoAlert("Sucesso", "Lote registrado com sucesso.");
                        loadData();
                        return true;
                    } catch (NumberFormatException e) {
                        AlertUtils.showWarningAlert("Erro", "Quantidade ou valores inválidos.");
                        return false;
                    } catch (Exception e) {
                        AlertUtils.showExceptionAlert("Erro", "Falha ao registrar entrada.", e);
                        return false;
                    }
                })
                .withCancelButton("Cancelar")
                .buildAndShow();
    }

    private void exportData() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exportar Relatório FIFO");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File file = fileChooser.showSaveDialog(getScene().getWindow());
        
        if (file != null) {
            try (FileWriter writer = new FileWriter(file)) {
                writer.write("Produto;Armazem;Lote;Quantidade;Validade;PrecoVenda\n");
                for (FifoLoteRow row : table.getItems()) {
                    writer.write(String.format("%s;%s;%s;%d;%s;%s\n",
                        row.getProduto().getNome(),
                        row.getArmazemNome() != null ? row.getArmazemNome() : "-",
                        row.getLote(),
                        row.getQuantidade(),
                        row.getValidade(),
                        row.getPrecoVenda()
                    ));
                }
                AlertUtils.showInfoAlert("Exportação", "Dados exportados com sucesso para " + file.getName());
            } catch (Exception e) {
                AlertUtils.showExceptionAlert("Erro", "Falha na exportação.", e);
            }
        }
    }

    /**
     * Modelo de Linha para Tabela
     */
    public static class FifoLoteRow {
        private final Produto produto;
        private final FifoService.ResumoLoteFifoDto dto;

        public FifoLoteRow(Produto produto, Armazem armazem, FifoService.ResumoLoteFifoDto dto) {
            this.produto = produto;
            this.dto = dto;
        }

        public javafx.beans.property.StringProperty produtoProperty() {
            return new SimpleStringProperty(produto != null ? produto.getNome() : "-");
        }

        public javafx.beans.property.StringProperty armazemProperty() {
            return new SimpleStringProperty(dto.nomeArmazem() != null ? dto.nomeArmazem() : "-");
        }

        public javafx.beans.property.StringProperty loteProperty() {
            return new SimpleStringProperty(dto.lote() != null ? dto.lote() : "-");
        }

        public javafx.beans.property.ObjectProperty<Integer> quantidadeProperty() {
            return new SimpleObjectProperty<>(dto.quantidade() != null ? dto.quantidade() : 0);
        }

        public javafx.beans.property.StringProperty dataEntradaProperty() {
            return new SimpleStringProperty(dto.dataEntrada() != null ? dto.dataEntrada().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "-");
        }

        public javafx.beans.property.StringProperty validadeProperty() {
            return new SimpleStringProperty(dto.validade() != null ? dto.validade().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "-");
        }

        public javafx.beans.property.StringProperty precoCompraProperty() {
            return new SimpleStringProperty(Money.formatAOA(dto.precoCompra()));
        }

        public javafx.beans.property.StringProperty precoVendaProperty() {
            return new SimpleStringProperty(Money.formatAOA(dto.precoVenda()));
        }

        public Produto getProduto() { return produto; }
        public String getArmazemNome() { return dto.nomeArmazem(); }
        public Long getArmazemId() { return dto.armazemId(); }
        public Integer getQuantidade() { return dto.quantidade(); }
        public String getLote() { return dto.lote(); }
        public String getValidade() { return dto.validade() != null ? dto.validade().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : ""; }
        public String getPrecoVenda() { return Money.formatAOA(dto.precoVenda()); }
    }
}

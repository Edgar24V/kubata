package ao.allon.kubata.faturacao.view;

import ao.allon.kubata.core.ui.table.AdvancedTableView;
import ao.allon.kubata.core.ui.table.TableUtils;
import ao.allon.kubata.faturacao.domain.Despesa;
import ao.allon.kubata.faturacao.domain.Fatura;
import ao.allon.kubata.core.service.ContabilidadeService;
import ao.allon.kubata.faturacao.service.DespesaService;
import ao.allon.kubata.faturacao.service.FaturaService;
import ao.allon.kubata.faturacao.service.JasperReportService;
import ao.allon.kubata.faturacao.service.ProdutoService;
import ao.allon.kubata.faturacao.service.ReciboService;
import ao.allon.kubata.faturacao.service.SaftAoExportService;
import ao.allon.kubata.faturacao.service.SessionManager;
import ao.allon.kubata.faturacao.service.report.InvoiceReportData;
import ao.allon.kubata.faturacao.service.report.InvoiceItemData;
import ao.allon.kubata.faturacao.service.report.InvoiceReportService;
import ao.allon.kubata.faturacao.service.report.FluxoCaixaReportService;
import ao.allon.kubata.faturacao.service.report.SalesByUnitReportService;
import ao.allon.kubata.faturacao.repository.FaturaRepository;
import ao.allon.kubata.faturacao.ui.util.AlertUtils;
import ao.allon.kubata.faturacao.ui.util.IconUtils;
import ao.allon.kubata.faturacao.ui.util.NotificationUtils;
import atlantafx.base.controls.Card;
import atlantafx.base.controls.CustomTextField;
import atlantafx.base.theme.Styles;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import net.sf.jasperreports.engine.JasperPrint;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.context.ApplicationContext;

import java.io.File;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.prefs.Preferences;
import java.text.NumberFormat;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RelatoriosView extends VBox {

    private final FaturaService faturaService;
    private final ProdutoService produtoService;
    private final SaftAoExportService saftService;
    private final JasperReportService jasperReportService;
    private final DespesaService despesaService;
    private final ContabilidadeService contabilidadeService;
    private final SessionManager sessionManager;
    private final InvoiceReportService invoiceReportService;
    private final FluxoCaixaReportService fluxoCaixaReportService;
    private final ApplicationContext applicationContext;

    private final Map<String, ReportItem> allItems = new LinkedHashMap<>();
    private final Set<String> favorites = new LinkedHashSet<>();
    private final List<String> recent = new ArrayList<>();

    private FlowPane itemsPane;
    private CustomTextField searchField;
    private ComboBox<ReportCategory> categoryFilter;
    private CheckBox onlyFavorites;
    private HBox recentsBar;

    @Autowired
    public RelatoriosView(FaturaService faturaService, ProdutoService produtoService, SaftAoExportService saftService, JasperReportService jasperReportService, DespesaService despesaService, ContabilidadeService contabilidadeService, ReciboService reciboService, SessionManager sessionManager, InvoiceReportService invoiceReportService, FluxoCaixaReportService fluxoCaixaReportService, ApplicationContext applicationContext) {
        this.faturaService = faturaService;
        this.produtoService = produtoService;
        this.saftService = saftService;
        this.jasperReportService = jasperReportService;
        this.despesaService = despesaService;
        this.contabilidadeService = contabilidadeService;
        this.sessionManager = sessionManager;
        this.invoiceReportService = invoiceReportService;
        this.fluxoCaixaReportService = fluxoCaixaReportService;
        this.applicationContext = applicationContext;

        setSpacing(20);
        setPadding(new Insets(20));
        getStyleClass().add("relatorios-view");

        VBox header = new VBox(6);
        HBox headerTop = new HBox(12);
        headerTop.setAlignment(Pos.CENTER_LEFT);

        VBox titles = new VBox(4);
        Label title = new Label("Relatórios e Análises");
        title.getStyleClass().add(Styles.TITLE_2);
        Label subtitle = new Label("Fiscais, comerciais, contabilidade e impressão");
        subtitle.getStyleClass().add(Styles.TEXT_MUTED);
        titles.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnAtualizar = new Button("Atualizar", IconUtils.icon(Feather.REFRESH_CW, IconUtils.SIZE_SMALL));
        btnAtualizar.getStyleClass().addAll(Styles.BUTTON_OUTLINED);
        btnAtualizar.setOnAction(e -> {
            renderItems();
            notifyInfo("Relatórios", "Lista de relatórios atualizada.");
        });

        Button btnSaft = new Button("SAFT-AO", IconUtils.icon(Feather.DOWNLOAD, IconUtils.SIZE_SMALL));
        btnSaft.getStyleClass().addAll(Styles.BUTTON_OUTLINED, Styles.ACCENT);
        btnSaft.setOnAction(e -> exportSaft());

        headerTop.getChildren().addAll(titles, spacer, btnSaft, btnAtualizar);

        HBox tools = buildToolsBar();
        recentsBar = buildRecentsBar();
        header.getChildren().addAll(headerTop, tools, recentsBar);

        itemsPane = new FlowPane();
        itemsPane.setHgap(16);
        itemsPane.setVgap(16);
        itemsPane.setPrefWrapLength(1100);
        itemsPane.setAlignment(Pos.TOP_LEFT);

        seedItems();
        loadFavorites();
        renderItems();

        getChildren().addAll(header, itemsPane);
    }

    private HBox buildRecentsBar() {
        Label lbl = new Label("Recentes:");
        lbl.getStyleClass().addAll(Styles.TEXT_MUTED);

        HBox box = new HBox(8);
        box.setAlignment(Pos.CENTER_LEFT);
        box.getChildren().add(lbl);

        renderRecents(box);
        return box;
    }

    private void renderRecents(HBox box) {
        if (box == null) return;
        // Mantém o label (posição 0)
        while (box.getChildren().size() > 1) {
            box.getChildren().remove(1);
        }

        if (recent.isEmpty()) {
            Label empty = new Label("(nenhum)");
            empty.getStyleClass().addAll(Styles.TEXT_MUTED);
            box.getChildren().add(empty);
            return;
        }

        int max = Math.min(6, recent.size());
        for (int i = 0; i < max; i++) {
            String key = recent.get(i);
            ReportItem item = allItems.get(key);
            if (item == null) continue;

            Button b = new Button(item.title(), IconUtils.icon(item.icon(), IconUtils.SIZE_SMALL));
            b.getStyleClass().addAll(Styles.BUTTON_OUTLINED);
            b.setOnAction(e -> {
                trackRecent(item.key());
                item.action().run();
            });
            box.getChildren().add(b);
        }

        Button clear = new Button("Limpar", IconUtils.icon(Feather.X, IconUtils.SIZE_SMALL));
        clear.getStyleClass().addAll(Styles.BUTTON_OUTLINED);
        clear.setOnAction(e -> {
            recent.clear();
            if (recentsBar != null) {
                renderRecents(recentsBar);
            }
        });
        box.getChildren().add(clear);
    }

    private enum ReportCategory {
        TODOS("Todos"),
        COMERCIAL("Comercial"),
        FISCAL("Fiscal"),
        CONTABILIDADE("Contabilidade"),
        IMPRESSAO("Impressão"),
        UTILITARIOS("Utilitários");

        private final String label;

        ReportCategory(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private record ReportItem(String key, String title, String description, Feather icon, ReportCategory category, Runnable action) {
    }

    private HBox buildToolsBar() {
        searchField = new CustomTextField();
        searchField.setPromptText("Pesquisar relatórios...");
        searchField.setLeft(IconUtils.icon(Feather.SEARCH, IconUtils.SIZE_SMALL));
        searchField.setPrefWidth(360);

        categoryFilter = new ComboBox<>();
        categoryFilter.getItems().addAll(ReportCategory.TODOS, ReportCategory.COMERCIAL, ReportCategory.FISCAL, ReportCategory.CONTABILIDADE, ReportCategory.IMPRESSAO, ReportCategory.UTILITARIOS);
        categoryFilter.getSelectionModel().select(ReportCategory.TODOS);
        categoryFilter.setPrefWidth(200);

        onlyFavorites = new CheckBox("Favoritos");

        searchField.textProperty().addListener((o, ov, nv) -> renderItems());
        categoryFilter.valueProperty().addListener((o, ov, nv) -> renderItems());
        onlyFavorites.selectedProperty().addListener((o, ov, nv) -> renderItems());

        Button btnLimpar = new Button("Limpar", IconUtils.icon(Feather.X, IconUtils.SIZE_SMALL));
        btnLimpar.getStyleClass().addAll(Styles.BUTTON_OUTLINED);
        btnLimpar.setOnAction(e -> {
            searchField.setText("");
            categoryFilter.getSelectionModel().select(ReportCategory.TODOS);
            onlyFavorites.setSelected(false);
            renderItems();
        });

        HBox tools = new HBox(10, searchField, categoryFilter, onlyFavorites, btnLimpar);
        tools.setAlignment(Pos.CENTER_LEFT);
        return tools;
    }

    private void renderItems() {
        if (itemsPane == null) return;
        itemsPane.getChildren().clear();

        String q = searchField != null && searchField.getText() != null ? searchField.getText().trim().toLowerCase() : "";
        ReportCategory cat = categoryFilter != null ? categoryFilter.getValue() : ReportCategory.TODOS;
        boolean favOnly = onlyFavorites != null && onlyFavorites.isSelected();

        // Favoritos em primeiro lugar
        for (ReportItem item : allItems.values()) {
            if (!favorites.contains(item.key())) continue;
            if (!matches(item, q, cat, favOnly)) continue;
            itemsPane.getChildren().add(createReportCard(item));
        }

        // Depois o restante
        for (ReportItem item : allItems.values()) {
            if (favorites.contains(item.key())) continue;
            if (!matches(item, q, cat, favOnly)) continue;
            itemsPane.getChildren().add(createReportCard(item));
        }
    }

    private boolean matches(ReportItem item, String q, ReportCategory cat, boolean favOnly) {
        if (favOnly && !favorites.contains(item.key())) return false;
        if (cat != null && cat != ReportCategory.TODOS && item.category() != cat) return false;
        if (q == null || q.isBlank()) return true;
        return (item.title() != null && item.title().toLowerCase().contains(q))
                || (item.description() != null && item.description().toLowerCase().contains(q));
    }

    private void loadFavorites() {
        try {
            Preferences prefs = Preferences.userRoot().node("kubata/reports/favorites");
            String userKey = getUserKey();
            String stored = prefs.get(userKey, "");
            if (stored != null && !stored.isBlank()) {
                String[] parts = stored.split(",");
                for (String p : parts) {
                    if (!p.isBlank()) {
                        favorites.add(p.trim());
                    }
                }
            }
        } catch (Exception e) {
            // Silencioso: fallback para sem persistência
        }
    }

    private void saveFavorites() {
        try {
            Preferences prefs = Preferences.userRoot().node("kubata/reports/favorites");
            String userKey = getUserKey();
            String value = String.join(",", favorites);
            prefs.put(userKey, value);
            prefs.flush();
        } catch (Exception e) {
            // Silencioso: falha ao persistir não deve quebrar a UI
        }
    }

    private String getUserKey() {
        try {
            if (sessionManager != null && sessionManager.getUsuario() != null) {
                String u = sessionManager.getUsuario().getUsername();
                if (u != null && !u.isBlank()) {
                    return u;
                }
                u = sessionManager.getUsuario().getNome();
                if (u != null && !u.isBlank()) {
                    return u;
                }
            }
        } catch (Exception ignore) {
        }
        return "default";
    }

    private void seedItems() {
        allItems.clear();

        addItem(new ReportItem("extrato_clientes", "Extrato de Clientes", "Relatório de vendas agrupado por cliente.", Feather.USERS, ReportCategory.COMERCIAL, this::showVendasReport));
        addItem(new ReportItem("contas_receber", "Contas a Receber (Clientes)", "Em aberto por cliente (total, recebido, aberto).", Feather.CREDIT_CARD, ReportCategory.COMERCIAL, this::showContasReceberClientesReport));
        addItem(new ReportItem("vendas_dia", "Vendas por Dia", "Totais diários de vendas e IVA por período.", Feather.CALENDAR, ReportCategory.COMERCIAL, this::showVendasPorDiaReport));
        addItem(new ReportItem("top_produtos", "Produtos Mais Vendidos", "Ranking de produtos com maior saída.", Feather.TRENDING_UP, ReportCategory.COMERCIAL, this::showTopProductsReport));
        addItem(new ReportItem("vendas_unidade", "Vendas por Unidade", "Total por produto/unidade (kg/L/m) com margem estimada.", Feather.PACKAGE, ReportCategory.COMERCIAL, this::showVendasPorUnidadeReport));
        addItem(new ReportItem("desempenho_caixa", "Desempenho por Caixa", "Vendas agrupadas por ponto de venda.", Feather.ACTIVITY, ReportCategory.COMERCIAL, this::showCashFlowReport));

        addItem(new ReportItem("mapa_impostos", "Mapa de Impostos", "Resumo de IVA liquidado por período.", Feather.PERCENT, ReportCategory.FISCAL, this::showTaxReport));
        addItem(new ReportItem("relatorio_impostos", "Relatório de Impostos", "Análise detalhada de todos os impostos.", Feather.BAR_CHART_2, ReportCategory.FISCAL, this::showImpostosReport));
        addItem(new ReportItem("retencoes", "Retenções na Fonte", "Mapa de retenções por tipo de rendimento.", Feather.DOLLAR_SIGN, ReportCategory.FISCAL, this::showRetencoesReport));
        addItem(new ReportItem("mapa_impostos_completo", "Mapa de Impostos Completo", "Análise visual com gráficos e KPIs.", Feather.PIE_CHART, ReportCategory.FISCAL, this::showMapaImpostosCompleto));
        addItem(new ReportItem("fiscal_integrado", "Relatório Fiscal Integrado", "Dashboard completo de tributação.", Feather.ACTIVITY, ReportCategory.FISCAL, this::showRelatorioFiscalIntegrado));
        addItem(new ReportItem("faturas_fiscais", "Faturas com Detalhes Fiscais", "Faturas com análise tributária completa.", Feather.FILE_TEXT, ReportCategory.FISCAL, this::showFaturasFiscaisReport));

        addItem(new ReportItem("balancete", "Balancete", "Balancete geral por data de corte.", Feather.BOOK_OPEN, ReportCategory.CONTABILIDADE, this::showBalanceteReport));
        addItem(new ReportItem("dre", "DRE", "Demonstração de Resultados por período.", Feather.BAR_CHART, ReportCategory.CONTABILIDADE, this::showDreReport));
        addItem(new ReportItem("fluxo_caixa", "Fluxo de Caixa", "Entradas e saídas diárias (recebimentos x pagamentos).", Feather.BAR_CHART_2, ReportCategory.CONTABILIDADE, this::showFluxoCaixaReport));

        addItem(new ReportItem("imprimir_a4", "Imprimir Documento (A4)", "Imprime por número: FT/FR/RC/NC/ND/GR/GT/OR/EC/PP.", Feather.PRINTER, ReportCategory.IMPRESSAO, this::showImprimirDocumentoA4));
        addItem(new ReportItem("imprimir_termica", "Imprimir Térmica (80mm)", "Impressão térmica por número e escolha de impressora.", Feather.CPU, ReportCategory.IMPRESSAO, this::showImprimirDocumentoTermica));

        addItem(new ReportItem("estoque_critico", "Estoque Crítico", "Produtos com estoque abaixo do mínimo.", Feather.ALERT_TRIANGLE, ReportCategory.UTILITARIOS, this::showLowStockReport));
        addItem(new ReportItem("extrato_fornecedores", "Extrato de Fornecedores", "Relatório de despesas agrupado por fornecedor.", Feather.TRUCK, ReportCategory.UTILITARIOS, this::showFornecedoresReport));
        addItem(new ReportItem("saft", "Exportar SAFT-AO", "Arquivo padrão AGT para contabilidade.", Feather.DOWNLOAD, ReportCategory.UTILITARIOS, this::exportSaft));
    }

    private void addItem(ReportItem item) {
        if (item == null || item.key() == null) return;
        allItems.put(item.key(), item);
    }

    private Card createReportCard(ReportItem item) {
        Card card = new Card();
        card.getStyleClass().addAll(Styles.ELEVATED_1);
        card.setPrefWidth(340);
        card.setMinWidth(280);

        HBox head = new HBox(10);
        head.setAlignment(Pos.CENTER_LEFT);

        Label lblTitle = new Label(item.title());
        lblTitle.getStyleClass().add(Styles.TITLE_4);
        lblTitle.setGraphic(IconUtils.icon(item.icon(), IconUtils.SIZE_MEDIUM));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        ToggleButton favBtn = new ToggleButton("", IconUtils.icon(Feather.STAR, IconUtils.SIZE_SMALL));
        favBtn.getStyleClass().addAll(Styles.BUTTON_ICON);
        favBtn.setSelected(favorites.contains(item.key()));
        favBtn.setOnAction(e -> {
            if (favBtn.isSelected()) {
                favorites.add(item.key());
            } else {
                favorites.remove(item.key());
            }
            saveFavorites();
            renderItems();
        });
        favBtn.setTooltip(new Tooltip("Favoritar"));

        head.getChildren().addAll(lblTitle, spacer, favBtn);

        Label lblDesc = new Label(item.description());
        lblDesc.setWrapText(true);
        lblDesc.getStyleClass().add(Styles.TEXT_MUTED);
        lblDesc.setMinHeight(44);

        Button btnGerar = new Button("Gerar", IconUtils.icon(Feather.PLAY, IconUtils.SIZE_SMALL));
        btnGerar.getStyleClass().addAll(Styles.ACCENT);
        btnGerar.setMaxWidth(Double.MAX_VALUE);
        btnGerar.setOnAction(e -> {
            trackRecent(item.key());
            item.action().run();
        });

        VBox body = new VBox(10, head, lblDesc, btnGerar);
        body.setPadding(new Insets(14));
        card.setBody(body);
        return card;
    }

    private void trackRecent(String key) {
        if (key == null) return;
        recent.remove(key);
        recent.add(0, key);
        if (recent.size() > 8) {
            recent.subList(8, recent.size()).clear();
        }

        if (recentsBar != null) {
            renderRecents(recentsBar);
        }
    }

    private void notifyInfo(String title, String msg) {
        try {
            NotificationUtils.showInfo(msg);
        } catch (Exception e) {
            AlertUtils.showInfoAlert(title, msg);
        }
    }

    @Deprecated
    private void addReportCard(Object grid, String title, String desc, Feather icon, int col, int row, Runnable action) {
        // Método antigo removido do layout. Mantido apenas para evitar quebra durante refatorações futuras.
    }

    private void showVendasReport() {
        showDateRangeDialog("Extrato de Clientes", (inicio, fim) -> {
            try {
                List<Fatura> faturas = faturaService.findAll();
                Map<String, Object> params = new HashMap<>();
                params.put("periodoInicio", inicio);
                params.put("periodoFim", fim);

                JasperPrint jasperPrint = jasperReportService.prepararExtratoClientes(faturas, params);
                jasperReportService.showReport(jasperPrint);

            } catch (Exception e) {
                AlertUtils.showExceptionAlert("Erro ao Gerar Relatório", "Não foi possível gerar o extrato de clientes.", e);
            }
        });
    }

    private void showFornecedoresReport() {
        showDateRangeDialog("Extrato de Fornecedores", (inicio, fim) -> {
            try {
                List<Despesa> despesas = despesaService.findAll();
                Map<String, Object> params = new HashMap<>();
                params.put("periodoInicio", inicio);
                params.put("periodoFim", fim);

                JasperPrint jasperPrint = jasperReportService.prepararExtratoFornecedores(despesas, params);
                jasperReportService.showReport(jasperPrint);

            } catch (Exception e) {
                AlertUtils.showExceptionAlert("Erro ao Gerar Relatório", "Não foi possível gerar o extrato de fornecedores.", e);
            }
        });
    }


    private void showTopProductsReport() {
        showDateRangeDialog("Ranking de Produtos", (inicio, fim) -> {
            try {
                List<Object[]> data = faturaService.findTopSellingProducts(inicio, fim);
                JasperPrint jp = jasperReportService.prepararTopProducts(data, inicio, fim);
                jasperReportService.showReport(jp);
            } catch (Exception e) {
                AlertUtils.showExceptionAlert("Erro ao Gerar Relatório", "Falha ao gerar ranking de produtos.", e);
            }
        });
    }

    private void showVendasPorUnidadeReport() {
        showDateRangeDialog("Vendas por Unidade", (inicio, fim) -> {
            try {
                SalesByUnitReportService svc = applicationContext.getBean(SalesByUnitReportService.class);
                List<SalesByUnitReportService.Row> rows = svc.gerar(inicio, fim);
                showVendasPorUnidadeTable(inicio, fim, rows);
            } catch (Exception e) {
                AlertUtils.showExceptionAlert("Erro ao Gerar Relatório", "Falha ao gerar vendas por unidade.", e);
            }
        });
    }

    private void showVendasPorUnidadeTable(LocalDate inicio, LocalDate fim, List<SalesByUnitReportService.Row> rows) {
        NumberFormat currency = NumberFormat.getCurrencyInstance(new Locale("pt", "AO"));

        AdvancedTableView<SalesByUnitReportService.Row> table = new AdvancedTableView<>();
        TableUtils.standardize(table);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        TableColumn<SalesByUnitReportService.Row, String> colProduto = new TableColumn<>("Produto");
        colProduto.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().produto()));

        TableColumn<SalesByUnitReportService.Row, String> colCategoria = new TableColumn<>("Categoria");
        colCategoria.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().categoria()));

        TableColumn<SalesByUnitReportService.Row, String> colUn = new TableColumn<>("Unidade");
        colUn.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().unidade()));

        TableColumn<SalesByUnitReportService.Row, String> colQtd = new TableColumn<>("Quantidade");
        colQtd.setCellValueFactory(c -> {
            java.math.BigDecimal q = c.getValue().quantidade() != null ? c.getValue().quantidade() : java.math.BigDecimal.ZERO;
            String s = q.stripTrailingZeros().toPlainString().replace(".", ",");
            return new javafx.beans.property.SimpleStringProperty(s);
        });

        TableColumn<SalesByUnitReportService.Row, String> colReceita = new TableColumn<>("Receita");
        colReceita.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(currency.format(c.getValue().receita())));

        TableColumn<SalesByUnitReportService.Row, String> colCusto = new TableColumn<>("Custo");
        colCusto.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(currency.format(c.getValue().custo())));

        TableColumn<SalesByUnitReportService.Row, String> colMargem = new TableColumn<>("Margem");
        colMargem.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(currency.format(c.getValue().margem())));

        table.getColumns().addAll(colProduto, colCategoria, colUn, colQtd, colReceita, colCusto, colMargem);
        table.setData(FXCollections.observableArrayList(rows != null ? rows : List.of()));

        Label header = new Label("Período: " + inicio + " a " + fim + " | Linhas: " + (rows != null ? rows.size() : 0));
        header.getStyleClass().addAll(Styles.TEXT_MUTED);

        VBox content = new VBox(10, header, table);
        content.setPadding(new Insets(10));
        VBox.setVgrow(table, Priority.ALWAYS);

        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle("Vendas por Unidade");
        ButtonType btExport = new ButtonType("Exportar CSV", ButtonBar.ButtonData.LEFT);
        dlg.getDialogPane().getButtonTypes().addAll(btExport, ButtonType.CLOSE);
        dlg.getDialogPane().setContent(content);

        javafx.scene.control.Button exportBtn = (javafx.scene.control.Button) dlg.getDialogPane().lookupButton(btExport);
        exportBtn.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
            ev.consume();
            exportVendasPorUnidadeCsv(inicio, fim, rows);
        });

        dlg.getDialogPane().setPrefSize(1000, 600);
        dlg.showAndWait();
    }

    private void exportVendasPorUnidadeCsv(LocalDate inicio, LocalDate fim, List<SalesByUnitReportService.Row> rows) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Exportar CSV - Vendas por Unidade");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
        fc.setInitialFileName("Vendas_por_Unidade_" + inicio + "_a_" + fim + ".csv");
        File file = fc.showSaveDialog(getScene().getWindow());
        if (file == null) return;

        StringBuilder sb = new StringBuilder();
        sb.append("produto;categoria;unidade;quantidade;receita;custo;margem").append(System.lineSeparator());
        if (rows != null) {
            for (var r : rows) {
                String produto = r.produto() != null ? r.produto().replace(";", " ") : "";
                String categoria = r.categoria() != null ? r.categoria().replace(";", " ") : "";
                String unidade = r.unidade() != null ? r.unidade().replace(";", " ") : "";
                String qtd = r.quantidade() != null ? r.quantidade().stripTrailingZeros().toPlainString().replace(".", ",") : "0";
                String receita = r.receita() != null ? r.receita().stripTrailingZeros().toPlainString().replace(".", ",") : "0";
                String custo = r.custo() != null ? r.custo().stripTrailingZeros().toPlainString().replace(".", ",") : "0";
                String margem = r.margem() != null ? r.margem().stripTrailingZeros().toPlainString().replace(".", ",") : "0";
                sb.append(produto).append(';')
                        .append(categoria).append(';')
                        .append(unidade).append(';')
                        .append(qtd).append(';')
                        .append(receita).append(';')
                        .append(custo).append(';')
                        .append(margem)
                        .append(System.lineSeparator());
            }
        }

        try {
            Files.writeString(file.toPath(), sb.toString(), StandardCharsets.UTF_8);
            notifyInfo("CSV", "Exportado com sucesso: " + file.getAbsolutePath());
        } catch (Exception e) {
            AlertUtils.showExceptionAlert("Erro", "Falha ao exportar CSV.", e);
        }
    }

    private void showLowStockReport() {
        try {
            List<ao.allon.kubata.faturacao.domain.Produto> produtos = produtoService.buscarProdutosComStockBaixo();
            JasperPrint jp = jasperReportService.prepararListaInventario(produtos);
            jp.setName("Estoque_Critico");
            jasperReportService.showReport(jp);
        } catch (Exception e) {
            AlertUtils.showExceptionAlert("Erro ao Gerar Relatório", "Falha ao gerar relatório de estoque crítico.", e);
        }
    }

    private void showCashFlowReport() {
        showDateRangeDialog("Desempenho por Caixa", (inicio, fim) -> {
            try {
                List<Object[]> data = faturaService.findPerformanceByCashier(inicio, fim);
                JasperPrint jp = jasperReportService.prepararPerformanceReport(data, inicio, fim);
                jasperReportService.showReport(jp);
            } catch (Exception e) {
                AlertUtils.showExceptionAlert("Erro ao Gerar Relatório", "Falha ao gerar desempenho por caixa.", e);
            }
        });
    }

    private void showFluxoCaixaReport() {
        showDateRangeDialog("Fluxo de Caixa", (inicio, fim) -> {
            try {
                JasperPrint jp = fluxoCaixaReportService.prepararFluxoCaixa(inicio, fim);
                jasperReportService.showReport(jp);
            } catch (Exception e) {
                AlertUtils.showExceptionAlert("Erro ao Gerar Relatório", "Falha ao gerar fluxo de caixa.", e);
            }
        });
    }

    private void showTaxReport() {
        showDateRangeDialog("Mapa de Impostos", (inicio, fim) -> {
            try {
                List<Object[]> data = faturaService.findTaxReportData(inicio, fim);
                JasperPrint jp = jasperReportService.prepararTaxReport(data, inicio, fim);
                jasperReportService.showReport(jp);
            } catch (Exception e) {
                AlertUtils.showExceptionAlert("Erro ao Gerar Relatório", "Falha ao gerar mapa de impostos.", e);
            }
        });
    }

    private void exportSaft() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Exportar SAFT-AO");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML Files", "*.xml"));
        fc.setInitialFileName("SAFT_AO_" + java.time.LocalDate.now() + ".xml");
        File file = fc.showSaveDialog(getScene().getWindow());
        if (file != null) {
            try {
                saftService.exportProdutosToSaftAo(file);
                AlertUtils.showInfoAlert("SAFT-AO", "Arquivo exportado com sucesso para:\n" + file.getAbsolutePath());
            } catch (Exception e) {
                AlertUtils.showErrorAlert("Erro Exportação", "Falha ao exportar SAFT-AO: " + e.getMessage());
            }
        }
    }
    
    private void showDateRangeDialog(String title, java.util.function.BiConsumer<LocalDate, LocalDate> onConfirm) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText("Selecione o período");
        
        ButtonType typeGerar = new ButtonType("Gerar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(typeGerar, ButtonType.CANCEL);
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        
        DatePicker dpInicio = new DatePicker(LocalDate.now().withDayOfMonth(1));
        DatePicker dpFim = new DatePicker(LocalDate.now());
        
        grid.add(new Label("Início:"), 0, 0);
        grid.add(dpInicio, 1, 0);
        grid.add(new Label("Fim:"), 0, 1);
        grid.add(dpFim, 1, 1);
        
        dialog.getDialogPane().setContent(grid);
        
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == typeGerar) {
            onConfirm.accept(dpInicio.getValue(), dpFim.getValue());
        }
    }

    // NOVOS RELATÓRIOS FISCAIS

    private void showImpostosReport() {
        showDateRangeDialog("Relatório de Impostos", (inicio, fim) -> {
            try {
                List<Fatura> faturas = faturaService.findByDataEmissaoBetweenWithItens(inicio, fim);
                String usuario = "";
                try {
                    ao.allon.kubata.faturacao.service.SessionManager sm = applicationContext.getBean(ao.allon.kubata.faturacao.service.SessionManager.class);
                    if (sm != null && sm.getUsuario() != null) {
                        usuario = sm.getUsuario().getNome() != null ? sm.getUsuario().getNome() : sm.getUsuario().getUsername();
                    }
                } catch (Exception ignore) {
                }
                JasperPrint jp = jasperReportService.prepararRelatorioImpostosProfissional(faturas, inicio, fim, usuario);
                jasperReportService.showReport(jp);

            } catch (Exception e) {
                AlertUtils.showExceptionAlert("Erro ao Gerar Relatório", "Não foi possível gerar o relatório de impostos.", e);
            }
        });
    }

    private void showRetencoesReport() {
        showDateRangeDialog("Relatório de Retenções", (inicio, fim) -> {
            try {
                List<Fatura> faturas = faturaService.findByDataEmissaoBetweenWithItens(inicio, fim);
                String usuario = "";
                try {
                    ao.allon.kubata.faturacao.service.SessionManager sm = applicationContext.getBean(ao.allon.kubata.faturacao.service.SessionManager.class);
                    if (sm != null && sm.getUsuario() != null) {
                        usuario = sm.getUsuario().getNome() != null ? sm.getUsuario().getNome() : sm.getUsuario().getUsername();
                    }
                } catch (Exception ignore) {
                }
                JasperPrint jp = jasperReportService.prepararRelatorioRetencoesFonteProfissional(faturas, inicio, fim, usuario);
                jasperReportService.showReport(jp);

            } catch (Exception e) {
                AlertUtils.showExceptionAlert("Erro ao Gerar Relatório", "Não foi possível gerar o relatório de retenções.", e);
            }
        });
    }

    private void showFaturasFiscaisReport() {
        showDateRangeDialog("Faturas com Detalhes Fiscais", (inicio, fim) -> {
            try {
                Map<String, Object> params = new HashMap<>();
                params.put("NUMERO_FATURA", "FT-2024-001");
                params.put("DATA_EMISSAO", new java.util.Date());
                params.put("CLIENTE_NOME", "Cliente Exemplo");
                params.put("CLIENTE_NIF", "5000000000");
                params.put("CLIENTE_ENDERECO", "Rua Exemplo, 123, Luanda");
                params.put("EMPRESA_NOME", "Kubata Sistemas");
                params.put("EMPRESA_NIF", "5000000000");
                params.put("EMPRESA_ENDERECO", "Luanda, Angola");
                params.put("EMPRESA_TELEFONE", "+244 999 999 999");
                params.put("EMPRESA_EMAIL", "info@kubata.ao");
                params.put("LOGO_PATH", getClass().getResource("/images/logo.png"));
                params.put("USUARIO", "Administrador");
                params.put("CONDICAO_PAGAMENTO", "30 dias");
                params.put("OBSERVACOES", "Fatura com detalhamento fiscal conforme legislação angolana.");

                AlertUtils.showInfoAlert("Faturas com Detalhes Fiscais", "Modelo de relatório ainda não integrado.");

            } catch (Exception e) {
                AlertUtils.showExceptionAlert("Erro ao Gerar Relatório", "Não foi possível gerar a fatura com detalhes fiscais.", e);
            }
        });
    }

    private void showMapaImpostosCompleto() {
        showDateRangeDialog("Mapa de Impostos Completo", (inicio, fim) -> {
            try {
                List<Fatura> faturas = faturaService.findByDataEmissaoBetweenWithItens(inicio, fim);
                String usuario = "";
                try {
                    ao.allon.kubata.faturacao.service.SessionManager sm = applicationContext.getBean(ao.allon.kubata.faturacao.service.SessionManager.class);
                    if (sm != null && sm.getUsuario() != null) {
                        usuario = sm.getUsuario().getNome() != null ? sm.getUsuario().getNome() : sm.getUsuario().getUsername();
                    }
                } catch (Exception ignore) {
                }
                JasperPrint jp = jasperReportService.prepararMapaImpostosProfissional(faturas, inicio, fim, usuario);
                jasperReportService.showReport(jp);

            } catch (Exception e) {
                AlertUtils.showExceptionAlert("Erro ao Gerar Relatório", "Não foi possível gerar o mapa de impostos completo.", e);
            }
        });
    }

    private void showRelatorioFiscalIntegrado() {
        AlertUtils.showInfoAlert("Relatório Fiscal Integrado", "Funcionalidade ainda não integrada.");
    }

    private void showBalanceteReport() {
        showSingleDateDialog("Balancete", "Selecione a data de corte", dataCorte -> {
            try {
                List<ContabilidadeService.BalanceteItemDTO> dados = contabilidadeService.gerarBalancete(dataCorte);
                JasperPrint jp = jasperReportService.prepararBalancete(dados, dataCorte);
                jp.setName("Balancete_" + dataCorte);
                jasperReportService.showReport(jp);
            } catch (Exception e) {
                AlertUtils.showExceptionAlert("Erro ao Gerar Relatório", "Não foi possível gerar o balancete.", e);
            }
        });
    }

    private void showDreReport() {
        showDateRangeDialog("DRE", (inicio, fim) -> {
            try {
                List<ContabilidadeService.BalanceteItemDTO> dados = contabilidadeService.gerarDRE(inicio, fim);
                JasperPrint jp = jasperReportService.prepararDRE(dados, inicio, fim);
                jp.setName("DRE_" + inicio + "_a_" + fim);
                jasperReportService.showReport(jp);
            } catch (Exception e) {
                AlertUtils.showExceptionAlert("Erro ao Gerar Relatório", "Não foi possível gerar a DRE.", e);
            }
        });
    }

    private void showSingleDateDialog(String title, String header, java.util.function.Consumer<LocalDate> onConfirm) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(header);

        ButtonType typeGerar = new ButtonType("Gerar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(typeGerar, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        DatePicker dp = new DatePicker(LocalDate.now());
        grid.add(new Label("Data:"), 0, 0);
        grid.add(dp, 1, 0);
        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == typeGerar) {
            onConfirm.accept(dp.getValue());
        }
    }

    private void showImprimirDocumentoA4() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Imprimir Documento (A4)");
        dialog.setHeaderText("Informe o número do documento (ex: FT A/1)");
        dialog.setContentText("Número:");
        Optional<String> res = dialog.showAndWait();
        if (res.isEmpty() || res.get().isBlank()) return;
        String numero = res.get().trim();
        try {
            FaturaRepository repo = applicationContext.getBean(FaturaRepository.class);
            Fatura faturaBase = repo.findByNumero(numero)
                    .orElseThrow(() -> new IllegalArgumentException("Documento não encontrado: " + numero));
            Fatura fatura = faturaService.findFaturaParaImpressao(faturaBase.getId())
                    .orElseThrow(() -> new IllegalStateException("Documento não encontrado para impressão. ID: " + faturaBase.getId()));
            JasperPrint jp = "Recibo".equalsIgnoreCase(fatura.getTipoDocumento() != null ? fatura.getTipoDocumento().getDescricao() : "")
                    ? jasperReportService.prepararRecibo(fatura)
                    : jasperReportService.prepararFatura(fatura);
            jasperReportService.showReport(jp);
        } catch (Exception e) {
            AlertUtils.showExceptionAlert("Erro ao Imprimir", "Não foi possível preparar o documento.", e);
        }
    }

    private void showImprimirDocumentoTermica() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Imprimir Térmica (80mm)");
        dialog.setHeaderText("Informe o número do documento (ex: FT A/1)");
        dialog.setContentText("Número:");
        Optional<String> res = dialog.showAndWait();
        if (res.isEmpty() || res.get().isBlank()) return;
        String numero = res.get().trim();
        try {
            FaturaRepository repo = applicationContext.getBean(FaturaRepository.class);
            Fatura fatura = repo.findByNumero(numero)
                    .orElseThrow(() -> new IllegalArgumentException("Documento não encontrado: " + numero));

            String operador = "";
            try {
                if (sessionManager != null && sessionManager.getUsuario() != null) {
                    operador = sessionManager.getUsuario().getNome() != null ? sessionManager.getUsuario().getNome() : sessionManager.getUsuario().getUsername();
                }
            } catch (Exception ignore) {
            }

            InvoiceReportData data = new InvoiceReportData(
                    fatura.getNumero(),
                    fatura.getDataEmissao() != null ? fatura.getDataEmissao().atStartOfDay() : java.time.LocalDateTime.now(),
                    fatura.getSystemEntryDate() != null ? fatura.getSystemEntryDate() : java.time.LocalDateTime.now(),
                    operador,
                    (fatura.getCliente() != null && fatura.getCliente().getNome() != null) ? fatura.getCliente().getNome() : "Consumidor Final",
                    (fatura.getCliente() != null && fatura.getCliente().getNif() != null) ? fatura.getCliente().getNif() : "999999999",
                    fatura.getSubtotal() != null ? fatura.getSubtotal() : java.math.BigDecimal.ZERO,
                    fatura.getIva() != null ? fatura.getIva() : java.math.BigDecimal.ZERO,
                    fatura.getTotal() != null ? fatura.getTotal() : java.math.BigDecimal.ZERO,
                    BigDecimal.ZERO, // totalDiscount
                    fatura.getTotalRetencao() != null ? fatura.getTotalRetencao() : java.math.BigDecimal.ZERO,
                    fatura.getHash() != null ? fatura.getHash() : "",
                    fatura.getHashAnterior() != null ? fatura.getHashAnterior() : ""
            );

            java.util.List<InvoiceItemData> items = new java.util.ArrayList<>();
            try {
                Fatura completa = faturaService.findFaturaParaImpressao(fatura.getId()).orElse(fatura);
                if (completa.getItens() != null) {
                    for (var it : completa.getItens()) {
                        String desc = it.getDescricao();
                        if (it.getProduto() != null && it.getProduto().getUnidadeMedida() != null) {
                            String u = switch (it.getProduto().getUnidadeMedida()) {
                                case KILOGRAMA -> "kg";
                                case LITRO -> "L";
                                case METRO -> "m";
                                case HORA, SERVICO -> "h";
                                case CAIXA -> "cx";
                                default -> "un";
                            };
                            desc = desc + " (" + u + ")";
                        }
                        java.math.BigDecimal q = it.getQuantidadeDecimal() != null
                                ? it.getQuantidadeDecimal()
                                : (it.getQuantidade() != null ? new java.math.BigDecimal(it.getQuantidade()) : java.math.BigDecimal.ZERO);
                        items.add(new InvoiceItemData(
                                "",
                                desc,
                                q,
                                "un",
                                it.getPrecoUnitario() != null ? it.getPrecoUnitario() : java.math.BigDecimal.ZERO,
                                it.getPercentualIva() != null ? it.getPercentualIva() : java.math.BigDecimal.ZERO,
                                it.getTotal() != null ? it.getTotal() : java.math.BigDecimal.ZERO,
                                it.getCodigoIsencao(),
                                it.getMotivoIsencao()
                        ));
                    }
                }
            } catch (Exception ignore) {
            }

            java.util.List<String> printers = invoiceReportService.getAvailablePrinters();
            ChoiceDialog<String> printerDialog = new ChoiceDialog<>(invoiceReportService.getDefaultPrinterName(), printers);
            printerDialog.setTitle("Escolher Impressora");
            printerDialog.setHeaderText("Selecione a impressora térmica");
            printerDialog.setContentText("Impressora:");
            Optional<String> printer = printerDialog.showAndWait();
            if (printer.isEmpty()) return;
            invoiceReportService.printInvoiceThermal(data, items, printer.get());
        } catch (Exception e) {
            AlertUtils.showExceptionAlert("Erro ao Imprimir", "Não foi possível imprimir na térmica.", e);
        }
    }

    private void showVendasPorDiaReport() {
        showDateRangeDialog("Vendas por Dia", (inicio, fim) -> {
            try {
                List<Object[]> data = faturaService.findTaxReportData(inicio, fim);
                JasperPrint jp = jasperReportService.prepararVendasPorDia(data, inicio, fim);
                jasperReportService.showReport(jp);
            } catch (Exception e) {
                AlertUtils.showExceptionAlert("Erro ao Gerar Relatório", "Não foi possível gerar vendas por dia.", e);
            }
        });
    }

    private void showContasReceberClientesReport() {
        showDateRangeDialog("Contas a Receber (Clientes)", (inicio, fim) -> {
            try {
                List<Fatura> faturas = faturaService.findByDataEmissaoBetweenWithItens(inicio, fim);
                // filtrar apenas documentos de venda e status relevantes
                List<Fatura> base = faturas.stream()
                        .filter(f -> f.getTipoDocumento() == ao.allon.kubata.faturacao.domain.enums.TipoDocumento.FATURA
                                || f.getTipoDocumento() == ao.allon.kubata.faturacao.domain.enums.TipoDocumento.FATURA_RECIBO)
                        .filter(f -> f.getStatus() == ao.allon.kubata.faturacao.domain.enums.StatusFatura.EMITIDA
                                || f.getStatus() == ao.allon.kubata.faturacao.domain.enums.StatusFatura.PAGA)
                        .toList();
                JasperPrint jp = jasperReportService.prepararContasReceberPorCliente(base, inicio, fim);
                jasperReportService.showReport(jp);
            } catch (Exception e) {
                AlertUtils.showExceptionAlert("Erro ao Gerar Relatório", "Não foi possível gerar contas a receber por cliente.", e);
            }
        });
    }
}

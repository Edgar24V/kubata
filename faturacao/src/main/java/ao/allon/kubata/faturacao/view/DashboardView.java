package ao.allon.kubata.faturacao.view;

import ao.allon.kubata.faturacao.controller.DashboardController;
import atlantafx.base.controls.Card;
import atlantafx.base.theme.Styles;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.*;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
@Scope("prototype")
public class DashboardView extends VBox {

    private static final int MESES_TENDENCIA = 6;
    private static final int DIAS_CATEGORIAS = 30;
    private static final int MAX_FATIAS = 5;
    private static final int MAX_PRODUTOS = 5;

    private final DashboardController controller;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("EEEE, dd 'de' MMMM 'de' yyyy", new Locale("pt", "AO"));
    private final DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("MMM/yy", new Locale("pt", "AO"));
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("pt", "AO"));

    private final MetricCard cardVendasDia = new MetricCard("Vendas Hoje", "—", Feather.DOLLAR_SIGN, Styles.SUCCESS);
    private final MetricCard cardVendasMes = new MetricCard("Vendas Mês", "—", Feather.TRENDING_UP, Styles.ACCENT);
    private final MetricCard cardFaturasHoje = new MetricCard("Faturas Hoje", "—", Feather.FILE_PLUS, Styles.SUCCESS);
    private final MetricCard cardFaturasPendentes = new MetricCard("Faturas Pendentes", "—", Feather.ALERT_CIRCLE, Styles.WARNING);
    private final MetricCard cardClientes = new MetricCard("Total Clientes", "—", Feather.USERS, Styles.ACCENT);
    private final MetricCard cardClientesNovos = new MetricCard("Novos Clientes (Mês)", "—", Feather.USER_PLUS, Styles.SUCCESS);
    private final MetricCard cardProdutos = new MetricCard("Produtos em Stock", "—", Feather.PACKAGE, Styles.ACCENT);
    private final MetricCard cardBaixoStock = new MetricCard("Baixo Stock", "—", Feather.ALERT_TRIANGLE, Styles.DANGER);

    private AreaChart<String, Number> salesChart;
    private PieChart pieChart;
    private BarChart<String, Number> topChart;

    public DashboardView(DashboardController controller) {
        this.controller = controller;

        setPadding(new Insets(0));
        setSpacing(0);
        setFillWidth(true);
        getStyleClass().add("dashboard-view");

        VBox content = new VBox(20);
        content.setPadding(new Insets(30));
        content.setFillWidth(true);
        content.getChildren().add(createHeader());
        content.getChildren().add(createMetricsSection());
        content.getChildren().add(new Separator());
        content.getChildren().add(createChartsSection());

        getChildren().add(content);

        carregarDados();
    }

    private HBox createHeader() {
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(5);
        Label title = new Label("Visão Geral");
        title.getStyleClass().addAll(Styles.TITLE_2);

        Label subtitle = new Label(LocalDate.now().format(dateFormatter));
        subtitle.getStyleClass().add(Styles.TEXT_MUTED);

        titleBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        header.getChildren().addAll(titleBox, spacer);
        return header;
    }

    private FlowPane createMetricsSection() {
        FlowPane pane = new FlowPane();
        pane.setHgap(20);
        pane.setVgap(20);
        pane.setPrefWrapLength(1200);

        pane.getChildren().addAll(
                cardVendasDia, cardVendasMes, cardFaturasHoje, cardFaturasPendentes,
                cardClientes, cardClientesNovos, cardProdutos, cardBaixoStock);

        return pane;
    }

    private GridPane createChartsSection() {
        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(20);

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(60);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(40);
        grid.getColumnConstraints().addAll(col1, col2);

        Card salesCard = new Card();
        salesCard.setHeader(new Label("Tendência de Vendas (Últimos " + MESES_TENDENCIA + " Meses)"));
        salesCard.getStyleClass().add(Styles.ELEVATED_1);

        salesChart = new AreaChart<>(new CategoryAxis(), new NumberAxis());
        salesChart.getYAxis().setLabel("Valor (Kz)");
        salesChart.setLegendVisible(false);
        salesChart.setCreateSymbols(false);
        salesCard.setBody(salesChart);
        salesCard.setPrefHeight(400);

        Card categoriesCard = new Card();
        categoriesCard.setHeader(new Label("Vendas por Categoria (Últimos " + DIAS_CATEGORIAS + " Dias)"));
        categoriesCard.getStyleClass().add(Styles.ELEVATED_1);

        pieChart = new PieChart();
        pieChart.setLabelsVisible(false);
        pieChart.setLegendSide(javafx.geometry.Side.RIGHT);
        categoriesCard.setBody(pieChart);
        categoriesCard.setPrefHeight(400);

        grid.add(salesCard, 0, 0);
        grid.add(categoriesCard, 1, 0);

        Card topProductsCard = new Card();
        topProductsCard.setHeader(new Label("Top " + MAX_PRODUTOS + " Produtos Mais Vendidos (Valor, Último Mês)"));
        topProductsCard.getStyleClass().add(Styles.ELEVATED_1);

        topChart = new BarChart<>(new CategoryAxis(), new NumberAxis());
        topChart.setLegendVisible(false);
        topProductsCard.setBody(topChart);
        topProductsCard.setPrefHeight(400);

        grid.add(topProductsCard, 0, 1, 2, 1);

        return grid;
    }

    private void carregarDados() {
        Thread worker = new Thread(() -> {
            try {
                Snapshot snapshot = Snapshot.carregar(controller, monthFormatter, MESES_TENDENCIA, DIAS_CATEGORIAS);
                Platform.runLater(() -> aplicar(snapshot));
            } catch (Exception ex) {
                System.err.println("Erro ao carregar dados do dashboard: " + ex.getMessage());
            }
        }, "dashboard-loader");
        worker.setDaemon(true);
        worker.start();
    }

    private void aplicar(Snapshot snapshot) {
        cardVendasDia.setValue(currencyFormat.format(snapshot.vendasDia()));
        cardVendasMes.setValue(currencyFormat.format(snapshot.vendasMes()));
        cardFaturasHoje.setValue(String.valueOf(snapshot.faturasHoje()));
        cardFaturasPendentes.setValue(String.valueOf(snapshot.faturasPendentes()));
        cardClientes.setValue(String.valueOf(snapshot.totalClientes()));
        cardClientesNovos.setValue(String.valueOf(snapshot.clientesNovos()));
        cardProdutos.setValue(String.valueOf(snapshot.totalProdutos()));
        cardBaixoStock.setValue(String.valueOf(snapshot.baixoStock()));

        salesChart.getData().setAll(new XYChart.Series<>("Vendas",
                FXCollections.observableArrayList(snapshot.tendencia())));
        pieChart.setData(FXCollections.observableArrayList(snapshot.porCategoria()));
        topChart.getData().setAll(new XYChart.Series<>(
                FXCollections.observableArrayList(snapshot.topProdutos())));
    }

    private record Snapshot(BigDecimal vendasDia, BigDecimal vendasMes, long faturasHoje, long faturasPendentes,
                            long totalClientes, long clientesNovos, long totalProdutos, long baixoStock,
                            List<XYChart.Data<String, Number>> tendencia, List<PieChart.Data> porCategoria,
                            List<XYChart.Data<String, Number>> topProdutos) {

        static Snapshot carregar(DashboardController controller, DateTimeFormatter monthFormatter,
                                 int meses, int diasCategorias) {
            LocalDate hoje = LocalDate.now();

            List<XYChart.Data<String, Number>> tendencia = new ArrayList<>();
            for (Map.Entry<YearMonth, BigDecimal> entrada : controller.getVendasPorMes(meses).entrySet()) {
                tendencia.add(new XYChart.Data<>(entrada.getKey().atDay(1).format(monthFormatter),
                        entrada.getValue().doubleValue()));
            }

            List<PieChart.Data> porCategoria = new ArrayList<>();
            double outras = 0;
            List<Object[]> categorias = controller.getVendasPorCategoria(hoje.minusDays(diasCategorias - 1L), hoje);
            for (int i = 0; i < categorias.size(); i++) {
                Object[] linha = categorias.get(i);
                double valor = valor(linha[1]);
                if (i < MAX_FATIAS) {
                    porCategoria.add(new PieChart.Data(String.valueOf(linha[0]), valor));
                } else {
                    outras += valor;
                }
            }
            if (outras > 0) {
                porCategoria.add(new PieChart.Data("Outras", outras));
            }

            List<XYChart.Data<String, Number>> topProdutos = new ArrayList<>();
            List<Object[]> maisVendidos = controller.findTopSellingProducts(hoje.minusMonths(1), hoje);
            for (int i = 0; i < maisVendidos.size() && i < MAX_PRODUTOS; i++) {
                Object[] linha = maisVendidos.get(i);
                topProdutos.add(new XYChart.Data<>(String.valueOf(linha[0]), valor(linha[2])));
            }

            return new Snapshot(
                    controller.getVendasDiaAtual(),
                    controller.getVendasMesAtual(),
                    controller.getFaturasHojeCount(),
                    controller.getFaturasPendentesCount(),
                    controller.getTotalClientesCount(),
                    controller.getClientesNovosMesCount(),
                    controller.getTotalProdutosCount(),
                    controller.getProdutosBaixoStockCount(),
                    tendencia, porCategoria, topProdutos);
        }

        private static double valor(Object bruto) {
            return bruto instanceof Number numero ? numero.doubleValue() : 0d;
        }
    }

    private static class MetricCard extends Card {

        private final Label lblValue;

        MetricCard(String title, String value, Feather icon, String colorStyle) {
            getStyleClass().add(Styles.ELEVATED_1);
            setPrefWidth(280);

            VBox body = new VBox(10);
            body.setAlignment(Pos.CENTER_LEFT);
            body.setPadding(new Insets(15));

            HBox headerBox = new HBox(10);
            headerBox.setAlignment(Pos.CENTER_LEFT);

            FontIcon fontIcon = new FontIcon(icon);
            fontIcon.setIconSize(24);
            fontIcon.getStyleClass().addAll(colorStyle);

            Label lblTitle = new Label(title);
            lblTitle.getStyleClass().add(Styles.TEXT_MUTED);
            lblTitle.setFont(Font.font("System", FontWeight.NORMAL, 14));

            headerBox.getChildren().addAll(fontIcon, lblTitle);

            lblValue = new Label(value);
            lblValue.setFont(Font.font("System", FontWeight.BOLD, 28));
            lblValue.getStyleClass().add(colorStyle);

            body.getChildren().addAll(headerBox, lblValue);
            setBody(body);
        }

        void setValue(String value) {
            lblValue.setText(value);
        }
    }
}

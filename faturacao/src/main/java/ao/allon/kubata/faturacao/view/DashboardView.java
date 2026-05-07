package ao.allon.kubata.faturacao.view;

import ao.allon.kubata.faturacao.controller.DashboardController;
import ao.allon.kubata.faturacao.domain.Cliente;
import ao.allon.kubata.faturacao.domain.Fatura;
import ao.allon.kubata.faturacao.domain.Produto;
import atlantafx.base.controls.Card;
import atlantafx.base.theme.Styles;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.*;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

@Component
public class DashboardView extends VBox {

    private final DashboardController controller;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("EEEE, dd 'de' MMMM 'de' yyyy", new Locale("pt", "AO"));

    public DashboardView(DashboardController controller) {
        this.controller = controller;
        
        setPadding(new Insets(0));
        setSpacing(0);
        setFillWidth(true);
        getStyleClass().add("dashboard-view");

        // Main Scroll Container
        VBox content = new VBox(20);
        content.setPadding(new Insets(30));
        content.setFillWidth(true);

        // 1. Header Section
        HBox header = createHeader();
        content.getChildren().add(header);

        // 2. Metrics Cards Section
        FlowPane metricsPane = createMetricsSection();
        content.getChildren().add(metricsPane);

        content.getChildren().add(new Separator());

        // 3. Charts Section
        GridPane chartsGrid = createChartsSection();
        content.getChildren().add(chartsGrid);

        getChildren().add(content);
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

        // Data fetching
        BigDecimal vendasMes = controller.getVendasMesAtual();
        BigDecimal vendasDia = BigDecimal.ZERO; // Método não implementado
        long faturasPendentes = controller.getFaturasPendentesCount();
        long faturasHoje = 0L; // Método não implementado
        int totalClientes = controller.getTotalClientesCount();
        int clientesNovosMes = 0; // Método não implementado
        int totalProdutos = controller.getTotalProdutosCount();
        int produtosBaixoStock = 0; // Método não implementado
        
        java.text.NumberFormat currencyFormat = java.text.NumberFormat.getCurrencyInstance(new Locale("pt", "AO"));

        // Cards - Primeira linha
        pane.getChildren().add(new MetricCard("Vendas Hoje", currencyFormat.format(vendasDia), Feather.DOLLAR_SIGN, Styles.SUCCESS));
        pane.getChildren().add(new MetricCard("Vendas Mês", currencyFormat.format(vendasMes), Feather.TRENDING_UP, Styles.ACCENT));
        pane.getChildren().add(new MetricCard("Faturas Hoje", String.valueOf(faturasHoje), Feather.FILE_PLUS, Styles.SUCCESS));
        pane.getChildren().add(new MetricCard("Faturas Pendentes", String.valueOf(faturasPendentes), Feather.ALERT_CIRCLE, Styles.WARNING));
        
        // Cards - Segunda linha
        pane.getChildren().add(new MetricCard("Total Clientes", String.valueOf(totalClientes), Feather.USERS, Styles.ACCENT));
        pane.getChildren().add(new MetricCard("Novos Clientes (Mês)", String.valueOf(clientesNovosMes), Feather.USER_PLUS, Styles.SUCCESS));
        pane.getChildren().add(new MetricCard("Produtos em Stock", String.valueOf(totalProdutos), Feather.PACKAGE, Styles.DANGER));
        pane.getChildren().add(new MetricCard("Baixo Stock", String.valueOf(produtosBaixoStock), Feather.ALERT_TRIANGLE, Styles.DANGER));

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

        // Chart 1: Sales Trend (Line/Area Chart)
        Card salesCard = new Card();
        salesCard.setHeader(new Label("Tendência de Vendas (Últimos 6 Meses)"));
        salesCard.getStyleClass().add(Styles.ELEVATED_1);
        
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Valor (Kz)");
        
        AreaChart<String, Number> salesChart = new AreaChart<>(xAxis, yAxis);
        salesChart.setLegendVisible(false);
        salesChart.getData().add(getSalesSeries());
        salesCard.setBody(salesChart);
        salesCard.setPrefHeight(400);

        // Chart 2: Top Categories (Pie Chart)
        Card categoriesCard = new Card();
        categoriesCard.setHeader(new Label("Distribuição por Categoria"));
        categoriesCard.getStyleClass().add(Styles.ELEVATED_1);
        
        PieChart pieChart = new PieChart();
        pieChart.setData(getCategoryData());
        pieChart.setLabelsVisible(false);
        pieChart.setLegendSide(javafx.geometry.Side.RIGHT);
        categoriesCard.setBody(pieChart);
        categoriesCard.setPrefHeight(400);

        grid.add(salesCard, 0, 0);
        grid.add(categoriesCard, 1, 0);

        // Chart 3: Top Products (Bar Chart)
        Card topProductsCard = new Card();
        topProductsCard.setHeader(new Label("Top 5 Produtos Mais Vendidos (Valor)"));
        topProductsCard.getStyleClass().add(Styles.ELEVATED_1);
        
        CategoryAxis pxAxis = new CategoryAxis();
        NumberAxis pyAxis = new NumberAxis();
        BarChart<String, Number> topChart = new BarChart<>(pxAxis, pyAxis);
        topChart.setLegendVisible(false);
        topChart.getData().add(getTopProductsSeries());
        topProductsCard.setBody(topChart);
        topProductsCard.setPrefHeight(400);
        
        grid.add(topProductsCard, 0, 1, 2, 1); // Span both columns

        return grid;
    }

    private XYChart.Series<String, Number> getTopProductsSeries() {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        List<Object[]> data = controller.findTopSellingProducts(LocalDate.now().minusMonths(1), LocalDate.now());
        data.stream().limit(5).forEach(row -> {
            series.getData().add(new XYChart.Data<>((String)row[0], (BigDecimal)row[2]));
        });
        return series;
    }

    private XYChart.Series<String, Number> getSalesSeries() {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Vendas");
        
        List<Fatura> faturas = controller.getFaturasRecentes();
        LocalDate now = LocalDate.now();
        
        // Group by month for the last 6 months
        for (int i = 5; i >= 0; i--) {
            LocalDate monthDate = now.minusMonths(i);
            Month month = monthDate.getMonth();
            int year = monthDate.getYear();
            
            String monthLabel = month.getDisplayName(TextStyle.SHORT, new Locale("pt", "AO"));
            
            BigDecimal total = faturas.stream()
                .filter(f -> f.getDataEmissao().getMonth() == month && f.getDataEmissao().getYear() == year)
                .map(Fatura::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
                
            series.getData().add(new XYChart.Data<>(monthLabel, total));
        }
        
        return series;
    }

    private javafx.collections.ObservableList<PieChart.Data> getCategoryData() {
        // O ideal é que o controller já forneça esses dados agregados.
        // Por enquanto, vamos usar o que temos.
        List<Produto> produtos = controller.getProdutosComStockBaixo();
        
        // Count products per category
        Map<String, Long> counts = produtos.stream()
            .collect(Collectors.groupingBy(
                p -> p.getCategoria() != null ? p.getCategoria().getNome() : "Sem Categoria",
                Collectors.counting()
            ));
            
        return FXCollections.observableArrayList(
            counts.entrySet().stream()
                .sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue())) // Sort descending
                .limit(5) // Top 5
                .map(e -> new PieChart.Data(e.getKey(), e.getValue()))
                .collect(Collectors.toList())
        );
    }

    /**
     * Modern Metric Card with Icon and Colored Value
     */
    private static class MetricCard extends Card {
        public MetricCard(String title, String value, Feather icon, String colorStyle) {
            getStyleClass().add(Styles.ELEVATED_1);
            setPrefWidth(280);
            
            VBox body = new VBox(10);
            body.setAlignment(Pos.CENTER_LEFT);
            body.setPadding(new Insets(15));
            
            // Icon Header
            HBox headerBox = new HBox(10);
            headerBox.setAlignment(Pos.CENTER_LEFT);
            
            FontIcon fontIcon = new FontIcon(icon);
            fontIcon.setIconSize(24);
            fontIcon.getStyleClass().addAll(colorStyle); // Apply color class to icon
            
            Label lblTitle = new Label(title);
            lblTitle.getStyleClass().add(Styles.TEXT_MUTED);
            lblTitle.setFont(Font.font("System", FontWeight.NORMAL, 14));
            
            headerBox.getChildren().addAll(fontIcon, lblTitle);
            
            // Value
            Label lblValue = new Label(value);
            lblValue.setFont(Font.font("System", FontWeight.BOLD, 28));
            // Apply the color style to the value text as well for impact, or keep it standard
            // User asked for "LETRAS COLORIDAS DE VALIDACAO", let's color the value
            lblValue.getStyleClass().add(colorStyle); 
            
            body.getChildren().addAll(headerBox, lblValue);
            setBody(body);
        }
    }
}

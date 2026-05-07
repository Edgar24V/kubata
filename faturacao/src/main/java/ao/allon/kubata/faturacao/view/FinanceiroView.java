package ao.allon.kubata.faturacao.view;

import ao.allon.kubata.faturacao.domain.Fatura;
import ao.allon.kubata.faturacao.domain.Recibo;
import ao.allon.kubata.faturacao.domain.Despesa;
import ao.allon.kubata.faturacao.service.FaturaService;
import ao.allon.kubata.faturacao.service.ReciboService;
import ao.allon.kubata.faturacao.service.DespesaService;
import atlantafx.base.controls.Card;
import atlantafx.base.theme.Styles;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class FinanceiroView extends BorderPane {

    private final FaturaService faturaService;
    private final ReciboService reciboService;
    private final DespesaService despesaService;

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("EEEE, dd 'de' MMMM 'de' yyyy", new Locale("pt", "AO"));

    private Label lblARAberto;
    private Label lblARVencido;
    private Label lblARRecebidoHoje;
    private Label lblAPAberto;
    private Label lblAPVencido;
    private Label lblAPPagoHoje;

    public FinanceiroView(FaturaService faturaService, ReciboService reciboService, DespesaService despesaService) {
        this.faturaService = faturaService;
        this.reciboService = reciboService;
        this.despesaService = despesaService;
        getStyleClass().add("financeiro-view");
        initializeUI();
        refreshData();
    }

    private void initializeUI() {
        VBox content = new VBox(20);
        content.setPadding(new Insets(30));
        content.setFillWidth(true);

        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);
        VBox titleBox = new VBox(5);
        Label title = new Label("Resumo Financeiro");
        title.getStyleClass().addAll(Styles.TITLE_2);
        Label subtitle = new Label(LocalDate.now().format(dateFormatter));
        subtitle.getStyleClass().add(Styles.TEXT_MUTED);
        titleBox.getChildren().addAll(title, subtitle);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        header.getChildren().addAll(titleBox, spacer);
        content.getChildren().add(header);

        FlowPane metrics = new FlowPane(20, 20);
        metrics.setAlignment(Pos.TOP_LEFT);
        lblARAberto = new Label("Kz 0,00");
        lblARVencido = new Label("Kz 0,00");
        lblARRecebidoHoje = new Label("Kz 0,00");
        lblAPAberto = new Label("Kz 0,00");
        lblAPVencido = new Label("Kz 0,00");
        lblAPPagoHoje = new Label("Kz 0,00");
        metrics.getChildren().add(new MetricCard("Receber - Aberto", lblARAberto, Feather.ARROW_DOWN_LEFT, Styles.WARNING));
        metrics.getChildren().add(new MetricCard("Receber - Vencido", lblARVencido, Feather.ALERT_TRIANGLE, Styles.DANGER));
        metrics.getChildren().add(new MetricCard("Recebido Hoje", lblARRecebidoHoje, Feather.CHECK_CIRCLE, Styles.SUCCESS));
        metrics.getChildren().add(new MetricCard("Pagar - Aberto", lblAPAberto, Feather.ARROW_UP_RIGHT, Styles.WARNING));
        metrics.getChildren().add(new MetricCard("Pagar - Vencido", lblAPVencido, Feather.ALERT_TRIANGLE, Styles.DANGER));
        metrics.getChildren().add(new MetricCard("Pago Hoje", lblAPPagoHoje, Feather.CHECK_CIRCLE, Styles.SUCCESS));
        content.getChildren().add(metrics);

        content.getChildren().add(new Separator());

        GridPane charts = new GridPane();
        charts.setHgap(20);
        charts.setVgap(20);
        ColumnConstraints c1 = new ColumnConstraints(); c1.setPercentWidth(50);
        ColumnConstraints c2 = new ColumnConstraints(); c2.setPercentWidth(50);
        charts.getColumnConstraints().addAll(c1, c2);

        Card cashFlowCard = new Card();
        cashFlowCard.setHeader(new Label("Fluxo de Caixa (Últimos 14 dias)"));
        cashFlowCard.getStyleClass().add(Styles.ELEVATED_1);
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Valor (Kz)");
        BarChart<String, Number> cashFlowChart = new BarChart<>(xAxis, yAxis);
        cashFlowChart.setLegendVisible(true);
        cashFlowChart.getData().addAll(getSeriesRecebimentos(14), getSeriesPagamentos(14));
        cashFlowCard.setBody(cashFlowChart);
        cashFlowCard.setPrefHeight(420);

        Card ageingCard = new Card();
        ageingCard.setHeader(new Label("Ageing Recebíveis (0–30 / 31–60 / 61–90 / >90)"));
        ageingCard.getStyleClass().add(Styles.ELEVATED_1);
        BarChart<String, Number> ageingChart = new BarChart<>(new CategoryAxis(), new NumberAxis());
        ageingChart.setLegendVisible(false);
        ageingChart.getData().add(getAgeingSeries());
        ageingCard.setBody(ageingChart);
        ageingCard.setPrefHeight(420);

        charts.add(cashFlowCard, 0, 0);
        charts.add(ageingCard, 1, 0);
        content.getChildren().add(charts);

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        setCenter(scroll);
    }

    private XYChart.Series<String, Number> getSeriesRecebimentos(int dias) {
        LocalDate hoje = LocalDate.now();
        XYChart.Series<String, Number> s = new XYChart.Series<>();
        s.setName("Recebimentos");
        for (int i = dias - 1; i >= 0; i--) {
            LocalDate d = hoje.minusDays(i);
            BigDecimal total = reciboService.findByPeriodo(d, d).stream().map(Recibo::getValor).reduce(BigDecimal.ZERO, BigDecimal::add);
            s.getData().add(new XYChart.Data<>(d.toString(), total));
        }
        return s;
    }

    private XYChart.Series<String, Number> getSeriesPagamentos(int dias) {
        LocalDate hoje = LocalDate.now();
        XYChart.Series<String, Number> s = new XYChart.Series<>();
        s.setName("Pagamentos");
        for (int i = dias - 1; i >= 0; i--) {
            LocalDate d = hoje.minusDays(i);
            BigDecimal total = despesaService.sumPagoPeriodo(d, d);
            s.getData().add(new XYChart.Data<>(d.toString(), total));
        }
        return s;
    }

    private XYChart.Series<String, Number> getAgeingSeries() {
        ObservableList<Fatura> faturas = FXCollections.observableArrayList(faturaService.findAll());
        LocalDate hoje = LocalDate.now();
        Map<String, BigDecimal> buckets = FXCollections.observableHashMap();
        buckets.put("0-30", BigDecimal.ZERO);
        buckets.put("31-60", BigDecimal.ZERO);
        buckets.put("61-90", BigDecimal.ZERO);
        buckets.put(">90", BigDecimal.ZERO);
        for (Fatura f : faturas) {
            BigDecimal total = f.getTotal() != null ? f.getTotal() : BigDecimal.ZERO;
            BigDecimal recebido = reciboService.totalRecebidoFatura(f.getId());
            BigDecimal aberto = total.subtract(recebido);
            if (aberto.compareTo(BigDecimal.ZERO) > 0 && f.getDataVencimento() != null) {
                long dias = java.time.temporal.ChronoUnit.DAYS.between(f.getDataVencimento(), hoje);
                if (dias <= 30) buckets.put("0-30", buckets.get("0-30").add(aberto));
                else if (dias <= 60) buckets.put("31-60", buckets.get("31-60").add(aberto));
                else if (dias <= 90) buckets.put("61-90", buckets.get("61-90").add(aberto));
                else buckets.put(">90", buckets.get(">90").add(aberto));
            }
        }
        XYChart.Series<String, Number> s = new XYChart.Series<>();
        s.setName("Receber");
        for (String k : buckets.keySet().stream().sorted().collect(Collectors.toList())) {
            s.getData().add(new XYChart.Data<>(k, buckets.get(k)));
        }
        return s;
    }

    public void refreshData() {
        updateKpis();
    }

    private void updateKpis() {
        BigDecimal arAberto = BigDecimal.ZERO;
        BigDecimal arVencido = BigDecimal.ZERO;
        BigDecimal apAberto = BigDecimal.ZERO;
        BigDecimal apVencido = BigDecimal.ZERO;
        LocalDate hoje = LocalDate.now();
        for (Fatura f : faturaService.findAll()) {
            BigDecimal total = f.getTotal() != null ? f.getTotal() : BigDecimal.ZERO;
            BigDecimal recebido = reciboService.totalRecebidoFatura(f.getId());
            BigDecimal aberto = total.subtract(recebido);
            if (aberto.compareTo(BigDecimal.ZERO) > 0) {
                arAberto = arAberto.add(aberto);
                if (f.getDataVencimento() != null && f.getDataVencimento().isBefore(hoje)) {
                    arVencido = arVencido.add(aberto);
                }
            }
        }
        for (Despesa d : despesaService.findAll()) {
            BigDecimal valor = d.getValor() != null ? d.getValor() : BigDecimal.ZERO;
            BigDecimal pago = d.getValorPago() != null ? d.getValorPago() : BigDecimal.ZERO;
            BigDecimal aberto = valor.subtract(pago);
            if (aberto.compareTo(BigDecimal.ZERO) > 0) {
                apAberto = apAberto.add(aberto);
                if (d.getDataVencimento() != null && d.getDataVencimento().isBefore(hoje)) {
                    apVencido = apVencido.add(aberto);
                }
            }
        }
        BigDecimal recebidoHoje = reciboService.findByPeriodo(hoje, hoje).stream().map(Recibo::getValor).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal pagoHoje = despesaService.sumPagoPeriodo(hoje, hoje);
        lblARAberto.setText(String.format("Kz %.2f", arAberto));
        lblARVencido.setText(String.format("Kz %.2f", arVencido));
        lblARRecebidoHoje.setText(String.format("Kz %.2f", recebidoHoje));
        lblAPAberto.setText(String.format("Kz %.2f", apAberto));
        lblAPVencido.setText(String.format("Kz %.2f", apVencido));
        lblAPPagoHoje.setText(String.format("Kz %.2f", pagoHoje));
    }

    private static class MetricCard extends Card {
        public MetricCard(String title, Label valueLabel, Feather icon, String colorStyle) {
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
            valueLabel.setFont(Font.font("System", FontWeight.BOLD, 28));
            valueLabel.getStyleClass().add(colorStyle);
            body.getChildren().addAll(headerBox, valueLabel);
            setBody(body);
        }
    }
}


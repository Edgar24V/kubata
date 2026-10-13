package ao.allon.kubata.financeiro.view;

import ao.allon.kubata.financeiro.domain.Caixa;
import ao.allon.kubata.financeiro.domain.ContaBancaria;
import ao.allon.kubata.financeiro.domain.MovimentoBancario;
import ao.allon.kubata.financeiro.domain.MovimentoCaixa;
import ao.allon.kubata.financeiro.enums.TipoTituloFinanceiro;
import ao.allon.kubata.financeiro.service.CaixaService;
import ao.allon.kubata.financeiro.service.ContaBancariaService;
import ao.allon.kubata.financeiro.service.MovimentoCaixaService;
import ao.allon.kubata.financeiro.service.TituloFinanceiroService;
import ao.allon.kubata.financeiro.ui.FinanceiroUiSupport;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Painel de controlo do Financeiro com saldos e últimas operações reais. */
@Component
public class FinanceiroDashboardView extends BorderPane {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private final CaixaService caixaService;
    private final MovimentoCaixaService movimentoCaixaService;
    private final ContaBancariaService contaBancariaService;
    private final TituloFinanceiroService tituloService;
    private final ObservableList<MovimentoLinha> movimentos = FXCollections.observableArrayList();

    private final Label liquidityValue = new Label("—");
    private final Label cashValue = new Label("—");
    private final Label bankValue = new Label("—");
    private final Label payableValue = new Label("—");
    private final Label receivableValue = new Label("—");
    private final TableView<MovimentoLinha> table = new TableView<>(movimentos);
    private final Label summaryLabel = new Label();

    public FinanceiroDashboardView(CaixaService caixaService,
                                   MovimentoCaixaService movimentoCaixaService,
                                   ContaBancariaService contaBancariaService,
                                   TituloFinanceiroService tituloService) {
        this.caixaService = caixaService;
        this.movimentoCaixaService = movimentoCaixaService;
        this.contaBancariaService = contaBancariaService;
        this.tituloService = tituloService;
        buildUi();
        refresh();
    }

    private void buildUi() {
        Button refreshButton = FinanceiroUiSupport.secondary("Actualizar dados");
        refreshButton.setOnAction(event -> refresh());

        VBox header = FinanceiroUiSupport.page(
                "Visão Geral Financeira",
                "Tesouraria, saldos disponíveis, compromissos e movimentos recentes.",
                refreshButton);
        setTop(header);

        HBox cards = new HBox(12,
                FinanceiroUiSupport.metric("LIQUIDEZ TOTAL", "—", "Caixas + contas bancárias", FinanceiroUiSupport.GREEN),
                FinanceiroUiSupport.metric("SALDO EM CAIXA", "—", "Numerário registado", "#2563EB"),
                FinanceiroUiSupport.metric("SALDO BANCÁRIO", "—", "Contas activas", "#0F766E"),
                FinanceiroUiSupport.metric("CONTAS A PAGAR", "—", "Saldo ainda por liquidar", "#B45309"),
                FinanceiroUiSupport.metric("CONTAS A RECEBER", "—", "Saldo ainda por receber", "#7C3AED"));
        cards.setPadding(new Insets(18, 22, 6, 22));
        cards.setAlignment(Pos.CENTER_LEFT);
        ((Region) cards.getChildren().get(0)).setMinWidth(160);
        ((Region) cards.getChildren().get(1)).setMinWidth(150);
        ((Region) cards.getChildren().get(2)).setMinWidth(150);
        ((Region) cards.getChildren().get(3)).setMinWidth(150);
        ((Region) cards.getChildren().get(4)).setMinWidth(150);
        cards.getChildren().forEach(node -> HBox.setHgrow(node, Priority.ALWAYS));

        // As referências aos cartões tornam possível actualizar os números sem
        // recriar os componentes nem perder o estado da página.
        updateMetricLabel(cards, 0, liquidityValue);
        updateMetricLabel(cards, 1, cashValue);
        updateMetricLabel(cards, 2, bankValue);
        updateMetricLabel(cards, 3, payableValue);
        updateMetricLabel(cards, 4, receivableValue);

        configureTable();
        summaryLabel.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11px;");
        VBox panel = FinanceiroUiSupport.whitePanel("Movimentos mais recentes", table);
        panel.setPadding(new Insets(15));
        VBox.setVgrow(panel, Priority.ALWAYS);

        VBox content = new VBox(12, cards, panel, summaryLabel);
        content.setPadding(new Insets(0, 22, 18, 22));
        VBox.setVgrow(panel, Priority.ALWAYS);
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        setCenter(scroll);
    }

    private void updateMetricLabel(HBox cards, int index, Label valueLabel) {
        VBox card = (VBox) cards.getChildren().get(index);
        card.getChildren().set(1, valueLabel);
        valueLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: 700; -fx-text-fill: #17352B;");
    }

    private void configureTable() {
        FinanceiroUiSupport.styleTable(table);
        table.setPrefHeight(360);
        TableColumn<MovimentoLinha, String> date = new TableColumn<>("Data");
        date.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().data() == null ? "—" : DATE_FORMAT.format(cell.getValue().data())));
        date.setPrefWidth(140);
        TableColumn<MovimentoLinha, String> type = new TableColumn<>("Movimento");
        type.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().tipo()));
        type.setPrefWidth(125);
        TableColumn<MovimentoLinha, String> origin = new TableColumn<>("Origem");
        origin.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().origem()));
        origin.setPrefWidth(165);
        TableColumn<MovimentoLinha, String> description = new TableColumn<>("Descrição");
        description.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().descricao()));
        TableColumn<MovimentoLinha, String> amount = new TableColumn<>("Valor");
        amount.setCellValueFactory(cell -> new SimpleStringProperty(
                FinanceiroUiSupport.money(cell.getValue().valor())));
        amount.setPrefWidth(155);
        table.getColumns().setAll(date, type, origin, description, amount);
    }

    public void refresh() {
        try {
            List<Caixa> caixas = caixaService.findAllAtivos();
            List<ContaBancaria> contas = contaBancariaService.findAllAtivas();
            BigDecimal saldoCaixa = caixas.stream().map(Caixa::getSaldo)
                    .filter(value -> value != null).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal saldoBanco = contas.stream().map(ContaBancaria::getSaldo)
                    .filter(value -> value != null).reduce(BigDecimal.ZERO, BigDecimal::add);

            cashValue.setText(FinanceiroUiSupport.money(saldoCaixa));
            bankValue.setText(FinanceiroUiSupport.money(saldoBanco));
            liquidityValue.setText(FinanceiroUiSupport.money(saldoCaixa.add(saldoBanco)));
            payableValue.setText(FinanceiroUiSupport.money(tituloService.totalPendente(TipoTituloFinanceiro.PAGAR)));
            receivableValue.setText(FinanceiroUiSupport.money(tituloService.totalPendente(TipoTituloFinanceiro.RECEBER)));

            List<MovimentoLinha> rows = new ArrayList<>();
            for (Caixa caixa : caixas) {
                for (MovimentoCaixa movimento : movimentoCaixaService.listarMovimentos(caixa.getId())) {
                    rows.add(new MovimentoLinha(movimento.getDataMovimento(),
                            movimento.getTipo() == null ? "Movimento" : movimento.getTipo().toString(),
                            "Caixa · " + caixa.getNome(),
                            movimento.getDescricao(),
                            sinalCaixa(movimento)));
                }
            }
            for (ContaBancaria conta : contas) {
                for (MovimentoBancario movimento : contaBancariaService.getUltimosMovimentos(conta.getId())) {
                    boolean credito = movimento.getTipo() != null
                            && movimento.getTipo().name().equals("CREDITO");
                    rows.add(new MovimentoLinha(movimento.getDataMovimento(),
                            credito ? "Crédito" : "Débito",
                            "Banco · " + conta.getBanco(),
                            movimento.getDescricao(),
                            credito ? movimento.getValor() : movimento.getValor().negate()));
                }
            }
            rows.sort(Comparator.comparing(MovimentoLinha::data,
                    Comparator.nullsLast(Comparator.reverseOrder())));
            movimentos.setAll(rows.stream().limit(25).toList());
            summaryLabel.setText(caixas.size() + " caixa(s) activo(s) · " + contas.size()
                    + " conta(s) bancária(s) activa(s) · " + rows.size() + " movimento(s) consultado(s).");
        } catch (Exception ex) {
            FinanceiroUiSupport.error("Não foi possível actualizar o painel financeiro", ex);
        }
    }

    private BigDecimal sinalCaixa(MovimentoCaixa movimento) {
        BigDecimal valor = movimento.getValor() == null ? BigDecimal.ZERO : movimento.getValor();
        return movimento.getTipo() != null && movimento.getTipo().name().equals("SAIDA")
                ? valor.negate() : valor;
    }

    private record MovimentoLinha(LocalDateTime data, String tipo, String origem,
                                  String descricao, BigDecimal valor) { }
}

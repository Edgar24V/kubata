package ao.allon.kubata.faturacao.pdv.flow;

import ao.allon.kubata.faturacao.domain.MovimentoCaixa;
import ao.allon.kubata.faturacao.domain.enums.MetodoPagamento;
import ao.allon.kubata.faturacao.domain.enums.TipoMovimento;
import ao.allon.kubata.faturacao.ui.util.IconUtils;
import ao.allon.kubata.faturacao.util.Money;
import atlantafx.base.theme.Styles;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.kordamp.ikonli.feather.Feather;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class FluxoCaixaView extends BorderPane {

    private final FluxoCaixaController controller;
    private final NumberFormat currencyFormat = Money.getCurrencyFormat();
    private final DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("HH:mm:ss");

    // Metric Labels
    private Label lblSaldoAtual;
    private Label lblTotalVendas;
    private Label lblTotalSangria;
    private Label lblTotalSuprimento;
    private Label lblStatus;

    // Table
    private TableView<MovimentoCaixa> tableMovimentos;
    
    // Callback para notificar outras telas (ex.: PDV) sobre mudança de status do caixa
    private Runnable onStatusChanged;
    public void setOnStatusChanged(Runnable cb) { this.onStatusChanged = cb; }

    public FluxoCaixaView(FluxoCaixaController controller) {
        this.controller = controller;
        getStyleClass().add("fluxo-caixa-view");
        setPadding(new Insets(20));

        // Header
        VBox header = buildHeader();
        setTop(header);

        // Center (Metrics + Table)
        VBox center = new VBox(20);
        center.getChildren().addAll(buildMetricsCards(), buildMovimentosTable());
        setCenter(center);

        // Refresh Data
        refreshData();
    }

    private VBox buildHeader() {
        VBox header = new VBox(10);
        header.setPadding(new Insets(0, 0, 20, 0));

        Label title = new Label("Fluxo de Caixa ");
        title.getStyleClass().addAll(Styles.TITLE_3);

        HBox toolbar = new HBox(10);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        Button btnAbrir = new Button("Abrir Caixa", IconUtils.icon(Feather.UNLOCK, IconUtils.SIZE_MEDIUM));
        btnAbrir.getStyleClass().addAll(Styles.SUCCESS);
        btnAbrir.setOnAction(e -> showAbrirCaixaDialog());

        Button btnFechar = new Button("Fechar Caixa", IconUtils.icon(Feather.LOCK, IconUtils.SIZE_MEDIUM));
        btnFechar.getStyleClass().addAll(Styles.DANGER);
        btnFechar.setOnAction(e -> showFecharCaixaDialog());

        Button btnSangria = new Button("Sangria", IconUtils.icon(Feather.MINUS_CIRCLE, IconUtils.SIZE_MEDIUM));
        btnSangria.getStyleClass().addAll(Styles.WARNING);
        btnSangria.setOnAction(e -> showSangriaDialog());

        Button btnSuprimento = new Button("Suprimento", IconUtils.icon(Feather.PLUS_CIRCLE, IconUtils.SIZE_MEDIUM));
        btnSuprimento.getStyleClass().addAll(Styles.ACCENT);
        btnSuprimento.setOnAction(e -> showSuprimentoDialog());
        
        Button btnRefresh = new Button("Atualizar", IconUtils.icon(Feather.REFRESH_CW, IconUtils.SIZE_MEDIUM));
        btnRefresh.setOnAction(e -> refreshData());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        lblStatus = new Label("Status: Carregando...");
        lblStatus.getStyleClass().add(Styles.TEXT_BOLD);

        toolbar.getChildren().addAll(btnAbrir, btnFechar, new Separator(), btnSangria, btnSuprimento, spacer, lblStatus, btnRefresh);

        header.getChildren().addAll(title, toolbar);
        return header;
    }

    private HBox buildMetricsCards() {
        HBox cards = new HBox(20);
        cards.setAlignment(Pos.CENTER_LEFT);

        lblSaldoAtual = new Label("KZ 0,00");
        lblTotalVendas = new Label("KZ 0,00");
        lblTotalSangria = new Label("KZ 0,00");
        lblTotalSuprimento = new Label("KZ 0,00");

        cards.getChildren().addAll(
            createMetricCard("Saldo em Caixa", lblSaldoAtual, Styles.ACCENT, Feather.DOLLAR_SIGN),
            createMetricCard("Total Vendas", lblTotalVendas, Styles.SUCCESS, Feather.TRENDING_UP),
            createMetricCard("Total Sangria", lblTotalSangria, Styles.DANGER, Feather.TRENDING_DOWN),
            createMetricCard("Total Suprimento", lblTotalSuprimento, Styles.WARNING, Feather.PLUS_SQUARE)
        );

        return cards;
    }

    private VBox createMetricCard(String title, Label valueLabel, String colorStyle, Feather icon) {
        VBox card = new VBox(5);
        card.setPadding(new Insets(15));
        card.setPrefWidth(200);
        card.setStyle("-fx-background-color: -color-bg-default; -fx-background-radius: 8; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 0);");
        
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        Label iconLabel = new Label("", IconUtils.icon(icon, 20));
        iconLabel.getStyleClass().addAll(colorStyle);
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add(Styles.TEXT_MUTED);
        header.getChildren().addAll(iconLabel, titleLabel);

        valueLabel.getStyleClass().addAll(Styles.TITLE_4);

        card.getChildren().addAll(header, valueLabel);
        return card;
    }

    private VBox buildMovimentosTable() {
        VBox container = new VBox(10);
        VBox.setVgrow(container, Priority.ALWAYS);

        Label lblTitle = new Label("Movimentações Recentes");
        lblTitle.getStyleClass().add(Styles.TITLE_4);

        tableMovimentos = new TableView<>();
        tableMovimentos.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        VBox.setVgrow(tableMovimentos, Priority.ALWAYS);

        TableColumn<MovimentoCaixa, String> colHora = new TableColumn<>("Hora");
        colHora.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDataHora().format(timeFormat)));
        
        TableColumn<MovimentoCaixa, String> colTipo = new TableColumn<>("Tipo");
        colTipo.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getTipo().toString()));
        colTipo.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item);
                    if (item.equals("VENDA") || item.equals("SUPRIMENTO") || item.equals("ABERTURA")) {
                        getStyleClass().add(Styles.SUCCESS);
                    } else {
                        getStyleClass().add(Styles.DANGER);
                    }
                }
            }
        });

        TableColumn<MovimentoCaixa, String> colDesc = new TableColumn<>("Descrição");
        colDesc.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDescricao()));

        TableColumn<MovimentoCaixa, String> colMetodo = new TableColumn<>("Método");
        colMetodo.setCellValueFactory(cell -> {
            MetodoPagamento m = cell.getValue().getMetodoPagamento();
            return new SimpleStringProperty(m != null ? m.toString() : "-");
        });

        TableColumn<MovimentoCaixa, String> colValor = new TableColumn<>("Valor");
        colValor.setCellValueFactory(cell -> new SimpleStringProperty(currencyFormat.format(cell.getValue().getValor())));
        colValor.setStyle("-fx-alignment: CENTER-RIGHT;");

        tableMovimentos.getColumns().addAll(colHora, colTipo, colDesc, colMetodo, colValor);

        container.getChildren().addAll(lblTitle, tableMovimentos);
        return container;
    }

    public void refreshData() {
        FluxoCaixaController.ResumoCaixa resumo = controller.calcularResumo();
        
        lblSaldoAtual.setText(currencyFormat.format(resumo.getSaldoAtual()));
        lblTotalVendas.setText(currencyFormat.format(resumo.getTotalVendas()));
        lblTotalSangria.setText(currencyFormat.format(resumo.getTotalSangria()));
        lblTotalSuprimento.setText(currencyFormat.format(resumo.getTotalSuprimento()));

        boolean aberto = controller.getCaixaAberto().isPresent();
        if (aberto) {
            lblStatus.setText("CAIXA ABERTO");
            lblStatus.setStyle("-fx-text-fill: -color-success-fg;");
        } else {
            lblStatus.setText("CAIXA FECHADO");
            lblStatus.setStyle("-fx-text-fill: -color-danger-fg;");
        }

        tableMovimentos.setItems(javafx.collections.FXCollections.observableArrayList(controller.getMovimentosAtuais()));
    }

    // Dialog Helpers
    private void showAbrirCaixaDialog() {
        if (controller.getCaixaAberto().isPresent()) {
            ao.allon.kubata.faturacao.ui.util.AlertUtils.showWarningAlert("Aviso", "Caixa já está aberto!");
            return;
        }
        
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        TextField txtSaldo = new TextField("0");
        content.getChildren().addAll(new Label("Saldo Inicial (KZ):"), txtSaldo);
        
        controller.getModalService().create()
            .title("Abrir Caixa")
            .content(content)
            .autoSize()
            .withConfirmButton("Abrir", () -> {
                try {
                    BigDecimal valor = new BigDecimal(txtSaldo.getText().replace(",", "."));
                    controller.abrirCaixa(valor);
                    refreshData();
                    ao.allon.kubata.faturacao.ui.util.AlertUtils.showInfoAlert("Sucesso", "Caixa aberto com sucesso!");
                    if (onStatusChanged != null) onStatusChanged.run();
                    return true;
                } catch (Exception e) {
                    ao.allon.kubata.faturacao.ui.util.AlertUtils.showErrorAlert("Erro", e.getMessage());
                    return false;
                }
            })
            .withCancelButton()
            .buildAndShow();
    }

    private void showFecharCaixaDialog() {
        if (controller.getCaixaAberto().isEmpty()) {
            ao.allon.kubata.faturacao.ui.util.AlertUtils.showWarningAlert("Aviso", "Caixa já está fechado!");
            return;
        }

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        TextField txtSaldo = new TextField("0");
        TextArea txtObs = new TextArea();
        content.getChildren().addAll(new Label("Saldo em Mãos (KZ):"), txtSaldo, new Label("Observações:"), txtObs);

        controller.getModalService().create()
            .title("Fechar Caixa")
            .content(content)
            .autoSize()
            .withConfirmButton("Fechar", () -> {
                try {
                    BigDecimal valor = new BigDecimal(txtSaldo.getText().replace(",", "."));
                    controller.fecharCaixa(valor, txtObs.getText());
                    refreshData();
                    ao.allon.kubata.faturacao.ui.util.AlertUtils.showInfoAlert("Sucesso", "Caixa fechado com sucesso!");
                    if (onStatusChanged != null) onStatusChanged.run();
                    return true;
                } catch (Exception e) {
                    ao.allon.kubata.faturacao.ui.util.AlertUtils.showErrorAlert("Erro", e.getMessage());
                    return false;
                }
            })
            .withCancelButton()
            .buildAndShow();
    }

    private void showSangriaDialog() {
        if (controller.getCaixaAberto().isEmpty()) {
            ao.allon.kubata.faturacao.ui.util.AlertUtils.showWarningAlert("Aviso", "Caixa fechado!");
            return;
        }

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        TextField txtValor = new TextField("0");
        TextField txtMotivo = new TextField();
        content.getChildren().addAll(new Label("Valor (KZ):"), txtValor, new Label("Motivo:"), txtMotivo);

        controller.getModalService().create()
            .title("Realizar Sangria")
            .content(content)
            .autoSize()
            .withConfirmButton("Confirmar", () -> {
                try {
                    BigDecimal valor = new BigDecimal(txtValor.getText().replace(",", "."));
                    controller.realizarSangria(valor, txtMotivo.getText());
                    refreshData();
                    ao.allon.kubata.faturacao.ui.util.AlertUtils.showInfoAlert("Sucesso", "Sangria realizada!");
                    return true;
                } catch (Exception e) {
                    ao.allon.kubata.faturacao.ui.util.AlertUtils.showErrorAlert("Erro", e.getMessage());
                    return false;
                }
            })
            .withCancelButton()
            .buildAndShow();
    }

    private void showSuprimentoDialog() {
        if (controller.getCaixaAberto().isEmpty()) {
            ao.allon.kubata.faturacao.ui.util.AlertUtils.showWarningAlert("Aviso", "Caixa fechado!");
            return;
        }

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        TextField txtValor = new TextField("0");
        TextField txtMotivo = new TextField();
        content.getChildren().addAll(new Label("Valor (KZ):"), txtValor, new Label("Motivo:"), txtMotivo);

        controller.getModalService().create()
            .title("Realizar Suprimento")
            .content(content)
            .autoSize()
            .withConfirmButton("Confirmar", () -> {
                try {
                    BigDecimal valor = new BigDecimal(txtValor.getText().replace(",", "."));
                    controller.realizarSuprimento(valor, txtMotivo.getText());
                    refreshData();
                    ao.allon.kubata.faturacao.ui.util.AlertUtils.showInfoAlert("Sucesso", "Suprimento realizado!");
                    return true;
                } catch (Exception e) {
                    ao.allon.kubata.faturacao.ui.util.AlertUtils.showErrorAlert("Erro", e.getMessage());
                    return false;
                }
            })
            .withCancelButton()
            .buildAndShow();
    }
}

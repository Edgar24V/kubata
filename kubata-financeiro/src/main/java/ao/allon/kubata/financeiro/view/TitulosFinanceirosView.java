package ao.allon.kubata.financeiro.view;

import ao.allon.kubata.financeiro.domain.Caixa;
import ao.allon.kubata.financeiro.domain.ContaBancaria;
import ao.allon.kubata.financeiro.domain.LiquidacaoTitulo;
import ao.allon.kubata.financeiro.domain.TituloFinanceiro;
import ao.allon.kubata.financeiro.enums.EstadoTituloFinanceiro;
import ao.allon.kubata.financeiro.enums.MeioPagamentoFinanceiro;
import ao.allon.kubata.financeiro.enums.TipoTituloFinanceiro;
import ao.allon.kubata.financeiro.service.CaixaService;
import ao.allon.kubata.financeiro.service.ContaBancariaService;
import ao.allon.kubata.financeiro.service.TituloFinanceiroService;
import ao.allon.kubata.financeiro.ui.FinanceiroUiSupport;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Contas a pagar/receber com liquidação parcial, histórico e integração com caixa/banco. */
@Component
public class TitulosFinanceirosView extends BorderPane {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final TituloFinanceiroService tituloService;
    private final CaixaService caixaService;
    private final ContaBancariaService contaService;

    private final ObservableList<TituloFinanceiro> pagar = FXCollections.observableArrayList();
    private final ObservableList<TituloFinanceiro> receber = FXCollections.observableArrayList();
    private final TableView<TituloFinanceiro> pagarTable = new TableView<>(pagar);
    private final TableView<TituloFinanceiro> receberTable = new TableView<>(receber);
    private final Label pagarSummary = new Label();
    private final Label receberSummary = new Label();

    public TitulosFinanceirosView(TituloFinanceiroService tituloService,
                                  CaixaService caixaService,
                                  ContaBancariaService contaService) {
        this.tituloService = tituloService;
        this.caixaService = caixaService;
        this.contaService = contaService;
        buildUi();
        refresh();
    }

    private void buildUi() {
        Button refresh = FinanceiroUiSupport.secondary("Actualizar");
        refresh.setOnAction(event -> refresh());
        setTop(FinanceiroUiSupport.page("Contas a Pagar e a Receber",
                "Gestão de compromissos, vencimentos e liquidações parciais ou totais ligadas ao livro-caixa e ao banco.",
                refresh));

        TabPane pane = new TabPane();
        pane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        pane.getStyleClass().add("financeiro-titulos-tabs");
        pane.getTabs().addAll(
                new Tab("Contas a Pagar", buildTitleTab(TipoTituloFinanceiro.PAGAR, pagarTable, pagarSummary)),
                new Tab("Contas a Receber", buildTitleTab(TipoTituloFinanceiro.RECEBER, receberTable, receberSummary))
        );
        setCenter(pane);
        setStyle("-fx-background-color: #F5F7F6;");
    }

    private VBox buildTitleTab(TipoTituloFinanceiro tipo,
                               TableView<TituloFinanceiro> table,
                               Label summary) {
        Button create = FinanceiroUiSupport.primary("＋ Novo título");
        Button edit = FinanceiroUiSupport.secondary("Editar");
        Button settle = FinanceiroUiSupport.secondary("Liquidar");
        Button history = FinanceiroUiSupport.secondary("Histórico");
        Button cancel = FinanceiroUiSupport.danger("Anular");
        create.setOnAction(event -> openTitleForm(tipo, null));
        edit.setOnAction(event -> selected(table).ifPresent(title -> openTitleForm(tipo, title)));
        settle.setOnAction(event -> selected(table).ifPresent(this::settle));
        history.setOnAction(event -> selected(table).ifPresent(this::showHistory));
        cancel.setOnAction(event -> selected(table).ifPresent(this::cancel));
        HBox toolbar = new HBox(8, create, edit, settle, history, cancel);
        toolbar.setPadding(new Insets(14, 18, 10, 18));
        toolbar.setAlignment(Pos.CENTER_LEFT);

        configureTitleTable(table);
        summary.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B;");
        HBox footer = new HBox(summary);
        footer.setPadding(new Insets(10, 18, 12, 18));
        VBox content = new VBox(0, toolbar, table, footer);
        VBox.setVgrow(table, Priority.ALWAYS);
        content.setStyle("-fx-background-color: #F5F7F6;");
        return content;
    }

    private void configureTitleTable(TableView<TituloFinanceiro> table) {
        FinanceiroUiSupport.styleTable(table);
        table.setMinHeight(240);
        TableColumn<TituloFinanceiro, String> code = new TableColumn<>("Código");
        code.setCellValueFactory(cell -> new SimpleStringProperty(safe(cell.getValue().getCodigo())));
        code.setPrefWidth(110);
        TableColumn<TituloFinanceiro, String> entity = new TableColumn<>("Cliente / Fornecedor");
        entity.setCellValueFactory(cell -> new SimpleStringProperty(safe(cell.getValue().getEntidade())));
        entity.setPrefWidth(190);
        TableColumn<TituloFinanceiro, String> description = new TableColumn<>("Descrição");
        description.setCellValueFactory(cell -> new SimpleStringProperty(safe(cell.getValue().getDescricao())));
        TableColumn<TituloFinanceiro, String> due = new TableColumn<>("Vencimento");
        due.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getDataVencimento() == null ? "—" : DATE_FORMAT.format(cell.getValue().getDataVencimento())));
        due.setPrefWidth(110);
        TableColumn<TituloFinanceiro, String> original = new TableColumn<>("Valor original");
        original.setCellValueFactory(cell -> new SimpleStringProperty(FinanceiroUiSupport.money(cell.getValue().getValorOriginal())));
        original.setPrefWidth(145);
        TableColumn<TituloFinanceiro, String> pending = new TableColumn<>("Saldo pendente");
        pending.setCellValueFactory(cell -> new SimpleStringProperty(FinanceiroUiSupport.money(cell.getValue().getSaldoPendente())));
        pending.setPrefWidth(145);
        TableColumn<TituloFinanceiro, String> state = new TableColumn<>("Estado");
        state.setCellValueFactory(cell -> {
            TituloFinanceiro title = cell.getValue();
            String value = title.isVencido() ? "VENCIDO" :
                    title.getEstado() == null ? "—" : title.getEstado().getDescricao();
            return new SimpleStringProperty(value);
        });
        state.setPrefWidth(150);
        table.getColumns().setAll(code, entity, description, due, original, pending, state);
        table.setRowFactory(view -> {
            TableRow<TituloFinanceiro> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    showHistory(row.getItem());
                }
            });
            return row;
        });
    }

    private void refresh() {
        try {
            pagar.setAll(tituloService.listar(TipoTituloFinanceiro.PAGAR));
            receber.setAll(tituloService.listar(TipoTituloFinanceiro.RECEBER));
            pagarSummary.setText(pagar.size() + " título(s) · pendente "
                    + FinanceiroUiSupport.money(tituloService.totalPendente(TipoTituloFinanceiro.PAGAR))
                    + " · vencidos " + tituloService.contarVencidos(TipoTituloFinanceiro.PAGAR));
            receberSummary.setText(receber.size() + " título(s) · pendente "
                    + FinanceiroUiSupport.money(tituloService.totalPendente(TipoTituloFinanceiro.RECEBER))
                    + " · vencidos " + tituloService.contarVencidos(TipoTituloFinanceiro.RECEBER));
        } catch (Exception ex) {
            FinanceiroUiSupport.error("Não foi possível carregar os títulos financeiros", ex);
        }
    }

    private java.util.Optional<TituloFinanceiro> selected(TableView<TituloFinanceiro> table) {
        TituloFinanceiro title = table.getSelectionModel().getSelectedItem();
        if (title == null) {
            FinanceiroUiSupport.info("Seleccione um título", "Seleccione primeiro o título financeiro que pretende operar.");
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(title);
    }

    private void openTitleForm(TipoTituloFinanceiro tipo, TituloFinanceiro selected) {
        TextField code = new TextField(selected == null ? "" : safe(selected.getCodigo()));
        TextField entity = new TextField(selected == null ? "" : safe(selected.getEntidade()));
        TextField description = new TextField(selected == null ? "" : safe(selected.getDescricao()));
        TextField document = new TextField(selected == null ? "" : safe(selected.getNumeroDocumento()));
        DatePicker issueDate = new DatePicker(selected == null || selected.getDataEmissao() == null
                ? LocalDate.now() : selected.getDataEmissao());
        DatePicker dueDate = new DatePicker(selected == null || selected.getDataVencimento() == null
                ? LocalDate.now().plusDays(30) : selected.getDataVencimento());
        TextField amount = new TextField(selected == null ? "" :
                selected.getValorOriginal() == null ? "" : selected.getValorOriginal().toPlainString());
        TextArea notes = new TextArea(selected == null ? "" : safe(selected.getObservacoes()));
        notes.setPromptText("Notas internas (opcional)");
        notes.setPrefRowCount(2);
        notes.setWrapText(true);

        GridPane form = formGrid();
        form.addRow(0, new Label("Natureza"), new Label(tipo.getDescricao()));
        form.addRow(1, new Label("Código *"), code);
        form.addRow(2, new Label(tipo == TipoTituloFinanceiro.PAGAR ? "Fornecedor *" : "Cliente *"), entity);
        form.addRow(3, new Label("Descrição *"), description);
        form.addRow(4, new Label("N.º documento"), document);
        form.addRow(5, new Label("Data emissão *"), issueDate);
        form.addRow(6, new Label("Data vencimento *"), dueDate);
        form.addRow(7, new Label("Valor original *"), amount);
        form.addRow(8, new Label("Observações"), notes);
        for (int i = 1; i <= 8; i++) {
            GridPane.setHgrow(form.getChildren().get(i * 2 + 1), Priority.ALWAYS);
        }
        if (selected != null) {
            code.setEditable(false);
            amount.setEditable(selected.getValorLiquidado() == null || selected.getValorLiquidado().signum() == 0);
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(selected == null ? "Novo título · " + tipo.getDescricao()
                : "Editar título · " + selected.getCodigo());
        dialog.setHeaderText(selected == null
                ? "Registe os dados do compromisso. O título ficará em aberto."
                : "Títulos com liquidações não permitem alteração dos dados financeiros.");
        dialog.getDialogPane().getButtonTypes().addAll(
                new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE), ButtonType.CANCEL);
        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().setPrefWidth(580);

        if (dialog.showAndWait().filter(button -> button.getButtonData() == ButtonBar.ButtonData.OK_DONE).isPresent()) {
            try {
                TituloFinanceiro data = new TituloFinanceiro();
                if (selected != null) data.setId(selected.getId());
                data.setTipo(tipo);
                data.setCodigo(code.getText());
                data.setEntidade(entity.getText());
                data.setDescricao(description.getText());
                data.setNumeroDocumento(document.getText());
                data.setDataEmissao(issueDate.getValue());
                data.setDataVencimento(dueDate.getValue());
                data.setValorOriginal(parseAmount(amount.getText()));
                data.setObservacoes(notes.getText());
                tituloService.salvar(data);
                refresh();
                FinanceiroUiSupport.info("Títulos financeiros", selected == null
                        ? "Título registado com sucesso." : "Título actualizado.");
            } catch (Exception ex) {
                FinanceiroUiSupport.error("Não foi possível guardar o título", ex);
            }
        }
    }

    private void settle(TituloFinanceiro title) {
        if (title.getEstado() == EstadoTituloFinanceiro.LIQUIDADO
                || title.getEstado() == EstadoTituloFinanceiro.ANULADO) {
            FinanceiroUiSupport.info("Liquidação não disponível",
                    "O título está " + (title.getEstado() == EstadoTituloFinanceiro.LIQUIDADO ? "liquidado" : "anulado") + ".");
            return;
        }

        TextField amount = new TextField(title.getSaldoPendente().toPlainString());
        amount.setPromptText("Valor desta liquidação");
        ComboBox<MeioPagamentoFinanceiro> method = new ComboBox<>(
                FXCollections.observableArrayList(MeioPagamentoFinanceiro.values()));
        method.setValue(MeioPagamentoFinanceiro.BANCO);
        method.setMaxWidth(Double.MAX_VALUE);

        ComboBox<Caixa> cash = new ComboBox<>(FXCollections.observableArrayList(caixaService.findAllAtivos()));
        cash.setPromptText("Seleccione o caixa");
        cash.setMaxWidth(Double.MAX_VALUE);
        ComboBox<ContaBancaria> bank = new ComboBox<>(FXCollections.observableArrayList(contaService.findAllAtivas()));
        bank.setPromptText("Seleccione a conta bancária");
        bank.setMaxWidth(Double.MAX_VALUE);
        TextField reference = new TextField();
        reference.setPromptText("Referência do pagamento/recebimento");

        Label sourceLabel = new Label("Conta bancária *");
        method.valueProperty().addListener((obs, old, value) -> {
            boolean cashSelected = value == MeioPagamentoFinanceiro.CAIXA;
            cash.setVisible(cashSelected);
            cash.setManaged(cashSelected);
            sourceLabel.setText(cashSelected ? "Caixa *" : "Conta bancária *");
            bank.setVisible(!cashSelected);
            bank.setManaged(!cashSelected);
        });
        cash.setVisible(false);
        cash.setManaged(false);

        GridPane form = formGrid();
        form.addRow(0, new Label("Título"), new Label(title.getCodigo() + " · " + title.getEntidade()));
        form.addRow(1, new Label("Saldo pendente"), new Label(FinanceiroUiSupport.money(title.getSaldoPendente())));
        form.addRow(2, new Label("Meio *"), method);
        form.addRow(3, sourceLabel, bank);
        form.addRow(4, new Label(""), cash);
        form.addRow(5, new Label("Valor *"), amount);
        form.addRow(6, new Label("Referência"), reference);
        GridPane.setHgrow(method, Priority.ALWAYS);
        GridPane.setHgrow(bank, Priority.ALWAYS);
        GridPane.setHgrow(cash, Priority.ALWAYS);
        GridPane.setHgrow(amount, Priority.ALWAYS);
        GridPane.setHgrow(reference, Priority.ALWAYS);

        String operation = title.getTipo() == TipoTituloFinanceiro.PAGAR ? "pagamento" : "recebimento";
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Liquidar título");
        dialog.setHeaderText("Registar " + operation + " parcial ou total. Será criado um movimento de caixa/banco.");
        dialog.getDialogPane().getButtonTypes().addAll(
                new ButtonType("Confirmar liquidação", ButtonBar.ButtonData.OK_DONE), ButtonType.CANCEL);
        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().setPrefWidth(585);

        if (dialog.showAndWait().filter(button -> button.getButtonData() == ButtonBar.ButtonData.OK_DONE).isPresent()) {
            try {
                Long sourceId;
                if (method.getValue() == MeioPagamentoFinanceiro.CAIXA) {
                    if (cash.getValue() == null) throw new IllegalArgumentException("Seleccione o caixa.");
                    sourceId = cash.getValue().getId();
                } else {
                    if (bank.getValue() == null) throw new IllegalArgumentException("Seleccione a conta bancária.");
                    sourceId = bank.getValue().getId();
                }
                tituloService.liquidar(title.getId(), parseAmount(amount.getText()),
                        method.getValue(), sourceId, reference.getText());
                refresh();
                FinanceiroUiSupport.info("Liquidação registada",
                        "O título foi actualizado e o movimento correspondente foi registado no livro financeiro.");
            } catch (Exception ex) {
                FinanceiroUiSupport.error("Não foi possível liquidar o título", ex);
            }
        }
    }

    private void showHistory(TituloFinanceiro title) {
        try {
            List<LiquidacaoTitulo> history = tituloService.listarLiquidacoes(title.getId());
            ObservableList<LiquidacaoTitulo> data = FXCollections.observableArrayList(history);
            TableView<LiquidacaoTitulo> historyTable = new TableView<>(data);
            FinanceiroUiSupport.styleTable(historyTable);

            TableColumn<LiquidacaoTitulo, String> date = new TableColumn<>("Data");
            date.setCellValueFactory(cell -> new SimpleStringProperty(formatDateTime(cell.getValue().getDataLiquidacao())));
            TableColumn<LiquidacaoTitulo, String> value = new TableColumn<>("Valor");
            value.setCellValueFactory(cell -> new SimpleStringProperty(FinanceiroUiSupport.money(cell.getValue().getValor())));
            TableColumn<LiquidacaoTitulo, String> method = new TableColumn<>("Meio");
            method.setCellValueFactory(cell -> new SimpleStringProperty(
                    cell.getValue().getMeioPagamento() == null ? "" : cell.getValue().getMeioPagamento().getDescricao()));
            TableColumn<LiquidacaoTitulo, String> account = new TableColumn<>("Caixa / conta bancária");
            account.setCellValueFactory(cell -> {
                LiquidacaoTitulo item = cell.getValue();
                String source = item.getCaixa() != null ? "Caixa · " + item.getCaixa().getNome()
                        : item.getContaBancaria() != null ? "Banco · " + item.getContaBancaria().getBanco()
                        : "—";
                return new SimpleStringProperty(source);
            });
            TableColumn<LiquidacaoTitulo, String> reference = new TableColumn<>("Referência");
            reference.setCellValueFactory(cell -> new SimpleStringProperty(safe(cell.getValue().getReferencia())));
            historyTable.getColumns().setAll(date, value, method, account, reference);
            historyTable.setPrefHeight(350);

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Histórico de liquidações");
            dialog.setHeaderText(title.getCodigo() + " · original " + FinanceiroUiSupport.money(title.getValorOriginal())
                    + " · pendente " + FinanceiroUiSupport.money(title.getSaldoPendente()));
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
            dialog.getDialogPane().setContent(historyTable);
            dialog.getDialogPane().setPrefSize(790, 475);
            dialog.showAndWait();
        } catch (Exception ex) {
            FinanceiroUiSupport.error("Não foi possível consultar o histórico", ex);
        }
    }

    private void cancel(TituloFinanceiro title) {
        if (!FinanceiroUiSupport.confirm("Anular título",
                "Pretende anular o título " + title.getCodigo() + "? A operação só é permitida antes de qualquer liquidação.")) {
            return;
        }
        try {
            tituloService.anular(title.getId());
            refresh();
        } catch (Exception ex) {
            FinanceiroUiSupport.error("Não foi possível anular o título", ex);
        }
    }

    private GridPane formGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));
        return grid;
    }

    private BigDecimal parseAmount(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Informe o valor.");
        String normalized = value.trim().replace(" ", "");
        if (normalized.contains(",") && normalized.contains(".")) {
            normalized = normalized.replace(".", "").replace(",", ".");
        } else {
            normalized = normalized.replace(",", ".");
        }
        try {
            return new BigDecimal(normalized);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Valor inválido. Utilize, por exemplo, 1500,00.");
        }
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? "—" : DATETIME_FORMAT.format(value);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}

package ao.allon.kubata.financeiro.view;

import ao.allon.kubata.financeiro.domain.Caixa;
import ao.allon.kubata.financeiro.domain.MovimentoCaixa;
import ao.allon.kubata.financeiro.enums.TipoMovimentoCaixa;
import ao.allon.kubata.financeiro.service.CaixaService;
import ao.allon.kubata.financeiro.service.MovimentoCaixaService;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Gestão operacional de caixas, aberturas, entradas, saídas e consulta de movimentos. */
@Component
public class CaixasView extends BorderPane {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final CaixaService caixaService;
    private final MovimentoCaixaService movimentoService;
    private final ObservableList<Caixa> caixas = FXCollections.observableArrayList();
    private final TableView<Caixa> table = new TableView<>(caixas);
    private final Label status = new Label("Seleccione um caixa para operar.");

    public CaixasView(CaixaService caixaService, MovimentoCaixaService movimentoService) {
        this.caixaService = caixaService;
        this.movimentoService = movimentoService;
        buildUi();
        refresh();
    }

    private void buildUi() {
        Button create = FinanceiroUiSupport.primary("＋ Novo caixa");
        Button edit = FinanceiroUiSupport.secondary("Editar");
        Button open = FinanceiroUiSupport.secondary("Abertura");
        Button in = FinanceiroUiSupport.secondary("＋ Entrada");
        Button out = FinanceiroUiSupport.secondary("− Saída");
        Button statement = FinanceiroUiSupport.secondary("Movimentos");
        Button deactivate = FinanceiroUiSupport.danger("Desactivar");
        Button refresh = FinanceiroUiSupport.secondary("Actualizar");
        create.setOnAction(event -> editCaixa(null));
        edit.setOnAction(event -> selectedCaixa().ifPresent(this::editCaixa));
        open.setOnAction(event -> selectedCaixa().ifPresent(c -> movement(c, TipoMovimentoCaixa.ABERTURA)));
        in.setOnAction(event -> selectedCaixa().ifPresent(c -> movement(c, TipoMovimentoCaixa.ENTRADA)));
        out.setOnAction(event -> selectedCaixa().ifPresent(c -> movement(c, TipoMovimentoCaixa.SAIDA)));
        statement.setOnAction(event -> selectedCaixa().ifPresent(this::showMovements));
        deactivate.setOnAction(event -> selectedCaixa().ifPresent(this::deactivate));
        refresh.setOnAction(event -> refresh());

        VBox header = FinanceiroUiSupport.page("Caixas e Livro de Caixa",
                "Registe aberturas, entradas e saídas com actualização automática do saldo e trilho de movimentos.",
                FinanceiroUiSupport.actions(create, edit, refresh));
        setTop(header);

        HBox operations = new HBox(8, open, in, out, statement, deactivate);
        operations.setAlignment(Pos.CENTER_LEFT);
        operations.setPadding(new Insets(12, 20, 12, 20));
        operations.setStyle("-fx-background-color: #F5F7F6;");

        configureTable();
        status.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11px;");
        HBox footer = new HBox(status);
        footer.setPadding(new Insets(10, 20, 10, 20));
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setStyle("-fx-background-color: white; -fx-border-color: #E3E9E5 transparent transparent transparent;");

        VBox body = new VBox(0, operations, table, footer);
        VBox.setVgrow(table, Priority.ALWAYS);
        setCenter(body);
        setStyle("-fx-background-color: #F5F7F6;");
    }

    private void configureTable() {
        FinanceiroUiSupport.styleTable(table);
        table.setMinHeight(220);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        TableColumn<Caixa, String> nome = new TableColumn<>("Caixa");
        nome.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getNome()));
        nome.setPrefWidth(200);

        TableColumn<Caixa, String> responsavel = new TableColumn<>("Responsável");
        responsavel.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getResponsavel()));
        responsavel.setPrefWidth(175);

        TableColumn<Caixa, String> saldo = new TableColumn<>("Saldo actual");
        saldo.setCellValueFactory(cell -> new SimpleStringProperty(FinanceiroUiSupport.money(cell.getValue().getSaldo())));
        saldo.setPrefWidth(155);

        TableColumn<Caixa, String> principal = new TableColumn<>("Principal");
        principal.setCellValueFactory(cell -> new SimpleStringProperty(
                Boolean.TRUE.equals(cell.getValue().getIsPrincipal()) ? "Sim" : "Não"));
        principal.setPrefWidth(95);

        TableColumn<Caixa, String> estado = new TableColumn<>("Estado");
        estado.setCellValueFactory(cell -> new SimpleStringProperty(
                Boolean.TRUE.equals(cell.getValue().getIsAtivo()) ? "Activo" : "Inactivo"));
        estado.setPrefWidth(95);

        TableColumn<Caixa, String> observacoes = new TableColumn<>("Observações");
        observacoes.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getObservacoes() == null ? "" : cell.getValue().getObservacoes()));

        table.getColumns().setAll(nome, responsavel, saldo, principal, estado, observacoes);
        table.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            status.setText(selected == null
                    ? "Seleccione um caixa para operar."
                    : selected.getNome() + " · saldo " + FinanceiroUiSupport.money(selected.getSaldo())
                    + (Boolean.TRUE.equals(selected.getIsAtivo()) ? " · activo" : " · inactivo"));
        });
        table.setRowFactory(view -> {
            TableRow<Caixa> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    editCaixa(row.getItem());
                }
            });
            return row;
        });
    }

    private void refresh() {
        try {
            caixas.setAll(caixaService.findAll());
        } catch (Exception ex) {
            FinanceiroUiSupport.error("Falha ao carregar os caixas", ex);
        }
    }

    private java.util.Optional<Caixa> selectedCaixa() {
        Caixa selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            FinanceiroUiSupport.info("Seleccione um caixa", "Seleccione primeiro o caixa que pretende operar.");
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(selected);
    }

    private void editCaixa(Caixa selected) {
        TextField nome = new TextField(selected == null ? "" : safe(selected.getNome()));
        nome.setPromptText("Ex.: Caixa principal");
        TextField responsavel = new TextField(selected == null ? "" : safe(selected.getResponsavel()));
        responsavel.setPromptText("Nome do responsável");
        CheckBox principal = new CheckBox("Definir como caixa principal");
        principal.setSelected(selected != null && Boolean.TRUE.equals(selected.getIsPrincipal()));
        CheckBox activo = new CheckBox("Caixa activo");
        activo.setSelected(selected == null || Boolean.TRUE.equals(selected.getIsAtivo()));
        TextArea observacoes = new TextArea(selected == null ? "" : safe(selected.getObservacoes()));
        observacoes.setPromptText("Notas internas (opcional)");
        observacoes.setPrefRowCount(3);
        observacoes.setWrapText(true);

        GridPane form = formGrid();
        form.addRow(0, new Label("Nome *"), nome);
        form.addRow(1, new Label("Responsável *"), responsavel);
        form.addRow(2, new Label("Opções"), principal);
        form.addRow(3, new Label("Estado"), activo);
        form.addRow(4, new Label("Observações"), observacoes);
        GridPane.setHgrow(nome, Priority.ALWAYS);
        GridPane.setHgrow(responsavel, Priority.ALWAYS);
        GridPane.setHgrow(observacoes, Priority.ALWAYS);

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(selected == null ? "Criar caixa" : "Editar caixa");
        dialog.setHeaderText(selected == null
                ? "Defina o caixa. O saldo inicial é registado na operação de Abertura."
                : "Actualize os dados cadastrais do caixa.");
        dialog.getDialogPane().getButtonTypes().addAll(
                new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE), ButtonType.CANCEL);
        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().setPrefWidth(510);

        if (dialog.showAndWait().filter(button -> button.getButtonData() == ButtonBar.ButtonData.OK_DONE).isPresent()) {
            try {
                Caixa data = new Caixa();
                if (selected != null) data.setId(selected.getId());
                data.setNome(nome.getText());
                data.setResponsavel(responsavel.getText());
                data.setIsPrincipal(principal.isSelected());
                data.setIsAtivo(activo.isSelected());
                data.setObservacoes(observacoes.getText());
                data.setSaldo(selected == null ? BigDecimal.ZERO : selected.getSaldo());
                caixaService.save(data);
                refresh();
                FinanceiroUiSupport.info("Caixas", selected == null
                        ? "Caixa criado com sucesso." : "Dados do caixa actualizados.");
            } catch (Exception ex) {
                FinanceiroUiSupport.error("Não foi possível guardar o caixa", ex);
            }
        }
    }

    private void movement(Caixa caixa, TipoMovimentoCaixa tipo) {
        if (!Boolean.TRUE.equals(caixa.getIsAtivo())) {
            FinanceiroUiSupport.info("Caixa inactivo", "Active o caixa antes de registar movimentos.");
            return;
        }
        if (tipo == TipoMovimentoCaixa.ABERTURA && caixa.getSaldo() != null
                && caixa.getSaldo().signum() != 0) {
            FinanceiroUiSupport.info("Abertura não permitida",
                    "O caixa já tem saldo. Utilize Entrada ou Saída para movimentar o caixa.");
            return;
        }

        TextField amount = new TextField();
        amount.setPromptText("0,00");
        TextField description = new TextField();
        description.setPromptText(tipo == TipoMovimentoCaixa.ABERTURA
                ? "Saldo de abertura do turno" : "Motivo do movimento");
        TextField reference = new TextField();
        reference.setPromptText("Referência do documento (opcional)");

        GridPane form = formGrid();
        form.addRow(0, new Label("Caixa"), new Label(caixa.getNome()));
        form.addRow(1, new Label("Saldo actual"), new Label(FinanceiroUiSupport.money(caixa.getSaldo())));
        form.addRow(2, new Label("Valor *"), amount);
        form.addRow(3, new Label("Descrição *"), description);
        form.addRow(4, new Label("Documento"), reference);
        GridPane.setHgrow(amount, Priority.ALWAYS);
        GridPane.setHgrow(description, Priority.ALWAYS);
        GridPane.setHgrow(reference, Priority.ALWAYS);

        String title = switch (tipo) {
            case ABERTURA -> "Abertura de caixa";
            case ENTRADA -> "Entrada de caixa";
            case SAIDA -> "Saída de caixa";
            default -> "Movimento de caixa";
        };
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(title + " · " + caixa.getNome());
        dialog.getDialogPane().getButtonTypes().addAll(
                new ButtonType("Registar", ButtonBar.ButtonData.OK_DONE), ButtonType.CANCEL);
        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().setPrefWidth(480);

        if (dialog.showAndWait().filter(button -> button.getButtonData() == ButtonBar.ButtonData.OK_DONE).isPresent()) {
            try {
                BigDecimal value = parseAmount(amount.getText());
                switch (tipo) {
                    case ABERTURA -> movimentoService.abrirCaixa(caixa.getId(), value, description.getText());
                    case ENTRADA -> movimentoService.registarEntrada(caixa.getId(), value, description.getText(), reference.getText());
                    case SAIDA -> movimentoService.registarSaida(caixa.getId(), value, description.getText(), reference.getText());
                    default -> throw new IllegalArgumentException("Tipo de movimento não suportado.");
                }
                refresh();
                FinanceiroUiSupport.info(title, "Movimento registado. Saldo actualizado automaticamente.");
            } catch (Exception ex) {
                FinanceiroUiSupport.error("Não foi possível registar o movimento", ex);
            }
        }
    }

    private void showMovements(Caixa caixa) {
        try {
            ObservableList<MovimentoCaixa> data = FXCollections.observableArrayList(
                    movimentoService.listarMovimentos(caixa.getId()));
            TableView<MovimentoCaixa> movements = new TableView<>(data);
            FinanceiroUiSupport.styleTable(movements);

            TableColumn<MovimentoCaixa, String> date = new TableColumn<>("Data");
            date.setCellValueFactory(cell -> new SimpleStringProperty(formatDate(cell.getValue().getDataMovimento())));
            TableColumn<MovimentoCaixa, String> type = new TableColumn<>("Tipo");
            type.setCellValueFactory(cell -> new SimpleStringProperty(
                    cell.getValue().getTipo() == null ? "" : cell.getValue().getTipo().name()));
            TableColumn<MovimentoCaixa, String> description = new TableColumn<>("Descrição");
            description.setCellValueFactory(cell -> new SimpleStringProperty(safe(cell.getValue().getDescricao())));
            TableColumn<MovimentoCaixa, String> document = new TableColumn<>("Documento");
            document.setCellValueFactory(cell -> new SimpleStringProperty(safe(cell.getValue().getDocumentoRef())));
            TableColumn<MovimentoCaixa, String> amount = new TableColumn<>("Valor");
            amount.setCellValueFactory(cell -> new SimpleStringProperty(FinanceiroUiSupport.money(cell.getValue().getValor())));
            TableColumn<MovimentoCaixa, String> balance = new TableColumn<>("Saldo após");
            balance.setCellValueFactory(cell -> new SimpleStringProperty(FinanceiroUiSupport.money(cell.getValue().getSaldoAtual())));
            movements.getColumns().setAll(date, type, description, document, amount, balance);
            movements.setPrefHeight(390);

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Livro de caixa");
            dialog.setHeaderText(caixa.getNome() + " · saldo actual " + FinanceiroUiSupport.money(caixa.getSaldo()));
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
            dialog.getDialogPane().setContent(movements);
            dialog.getDialogPane().setPrefSize(900, 520);
            dialog.showAndWait();
        } catch (Exception ex) {
            FinanceiroUiSupport.error("Não foi possível consultar os movimentos", ex);
        }
    }

    private void deactivate(Caixa caixa) {
        if (!FinanceiroUiSupport.confirm("Desactivar caixa",
                "O caixa " + caixa.getNome() + " ficará indisponível para novos movimentos. O histórico será preservado.")) {
            return;
        }
        try {
            caixaService.delete(caixa.getId());
            refresh();
        } catch (Exception ex) {
            FinanceiroUiSupport.error("Não foi possível desactivar o caixa", ex);
        }
    }

    private GridPane formGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(11);
        grid.setPadding(new Insets(10));
        return grid;
    }

    private BigDecimal parseAmount(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Informe o valor do movimento.");
        }
        String normalized = value.trim().replace(" ", "");
        if (normalized.contains(",") && normalized.contains(".")) {
            normalized = normalized.replace(".", "").replace(",", ".");
        } else {
            normalized = normalized.replace(",", ".");
        }
        try {
            return new BigDecimal(normalized);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("O valor informado não é válido. Utilize, por exemplo, 1500,00.");
        }
    }

    private String formatDate(LocalDateTime value) {
        return value == null ? "—" : DATE_FORMAT.format(value);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}

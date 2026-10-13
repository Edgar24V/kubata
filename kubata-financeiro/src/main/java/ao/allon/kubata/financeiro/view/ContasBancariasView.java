package ao.allon.kubata.financeiro.view;

import ao.allon.kubata.financeiro.domain.ContaBancaria;
import ao.allon.kubata.financeiro.domain.MovimentoBancario;
import ao.allon.kubata.financeiro.enums.TipoMovimentoBancario;
import ao.allon.kubata.financeiro.service.ContaBancariaService;
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
import java.util.List;

/** Cadastro de contas e movimentos bancários, incluindo transferências entre contas. */
@Component
public class ContasBancariasView extends BorderPane {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ContaBancariaService contaService;
    private final ObservableList<ContaBancaria> contas = FXCollections.observableArrayList();
    private final TableView<ContaBancaria> table = new TableView<>(contas);
    private final Label status = new Label("Seleccione uma conta bancária.");

    public ContasBancariasView(ContaBancariaService contaService) {
        this.contaService = contaService;
        buildUi();
        refresh();
    }

    private void buildUi() {
        Button create = FinanceiroUiSupport.primary("＋ Nova conta");
        Button edit = FinanceiroUiSupport.secondary("Editar");
        Button credit = FinanceiroUiSupport.secondary("＋ Crédito");
        Button debit = FinanceiroUiSupport.secondary("− Débito");
        Button transfer = FinanceiroUiSupport.secondary("⇄ Transferir");
        Button statement = FinanceiroUiSupport.secondary("Extrato");
        Button deactivate = FinanceiroUiSupport.danger("Desactivar");
        Button refresh = FinanceiroUiSupport.secondary("Actualizar");

        create.setOnAction(event -> editConta(null));
        edit.setOnAction(event -> selectedConta().ifPresent(this::editConta));
        credit.setOnAction(event -> selectedConta().ifPresent(c -> movement(c, TipoMovimentoBancario.CREDITO)));
        debit.setOnAction(event -> selectedConta().ifPresent(c -> movement(c, TipoMovimentoBancario.DEBITO)));
        transfer.setOnAction(event -> transferir());
        statement.setOnAction(event -> selectedConta().ifPresent(this::showStatement));
        deactivate.setOnAction(event -> selectedConta().ifPresent(this::deactivate));
        refresh.setOnAction(event -> refresh());

        setTop(FinanceiroUiSupport.page("Contas Bancárias",
                "Cadastro bancário, controlo de saldo, créditos, débitos, transferências e extractos.",
                FinanceiroUiSupport.actions(create, edit, refresh)));

        HBox operations = new HBox(8, credit, debit, transfer, statement, deactivate);
        operations.setAlignment(Pos.CENTER_LEFT);
        operations.setPadding(new Insets(12, 20, 12, 20));
        operations.setStyle("-fx-background-color: #F5F7F6;");

        configureTable();
        status.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11px;");
        HBox footer = new HBox(status);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setPadding(new Insets(10, 20, 10, 20));
        footer.setStyle("-fx-background-color: white; -fx-border-color: #E3E9E5 transparent transparent transparent;");

        VBox body = new VBox(0, operations, table, footer);
        VBox.setVgrow(table, Priority.ALWAYS);
        setCenter(body);
        setStyle("-fx-background-color: #F5F7F6;");
    }

    private void configureTable() {
        FinanceiroUiSupport.styleTable(table);
        table.setMinHeight(220);

        TableColumn<ContaBancaria, String> banco = new TableColumn<>("Banco");
        banco.setCellValueFactory(cell -> new SimpleStringProperty(safe(cell.getValue().getBanco())));
        banco.setPrefWidth(150);
        TableColumn<ContaBancaria, String> numero = new TableColumn<>("N.º de conta");
        numero.setCellValueFactory(cell -> new SimpleStringProperty(safe(cell.getValue().getNumeroConta())));
        numero.setPrefWidth(130);
        TableColumn<ContaBancaria, String> titular = new TableColumn<>("Titular");
        titular.setCellValueFactory(cell -> new SimpleStringProperty(safe(cell.getValue().getTitular())));
        titular.setPrefWidth(180);
        TableColumn<ContaBancaria, String> iban = new TableColumn<>("IBAN");
        iban.setCellValueFactory(cell -> new SimpleStringProperty(safe(cell.getValue().getIban())));
        iban.setPrefWidth(220);
        TableColumn<ContaBancaria, String> saldo = new TableColumn<>("Saldo disponível");
        saldo.setCellValueFactory(cell -> new SimpleStringProperty(FinanceiroUiSupport.money(cell.getValue().getSaldo())));
        saldo.setPrefWidth(155);
        TableColumn<ContaBancaria, String> principal = new TableColumn<>("Principal");
        principal.setCellValueFactory(cell -> new SimpleStringProperty(
                Boolean.TRUE.equals(cell.getValue().getIsPrincipal()) ? "Sim" : "Não"));
        principal.setPrefWidth(85);
        TableColumn<ContaBancaria, String> estado = new TableColumn<>("Estado");
        estado.setCellValueFactory(cell -> new SimpleStringProperty(
                Boolean.TRUE.equals(cell.getValue().getIsAtivo()) ? "Activa" : "Inactiva"));
        estado.setPrefWidth(85);
        table.getColumns().setAll(banco, numero, titular, iban, saldo, principal, estado);
        table.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) ->
                status.setText(selected == null
                        ? "Seleccione uma conta bancária."
                        : selected.getBanco() + " · " + selected.getNumeroConta()
                        + " · " + FinanceiroUiSupport.money(selected.getSaldo())
                        + (Boolean.TRUE.equals(selected.getIsAtivo()) ? " · activa" : " · inactiva")));
        table.setRowFactory(view -> {
            TableRow<ContaBancaria> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) editConta(row.getItem());
            });
            return row;
        });
    }

    private void refresh() {
        try {
            contas.setAll(contaService.findAll());
        } catch (Exception ex) {
            FinanceiroUiSupport.error("Falha ao carregar as contas bancárias", ex);
        }
    }

    private java.util.Optional<ContaBancaria> selectedConta() {
        ContaBancaria selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            FinanceiroUiSupport.info("Seleccione uma conta", "Seleccione primeiro a conta bancária que pretende operar.");
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(selected);
    }

    private void editConta(ContaBancaria selected) {
        TextField banco = new TextField(selected == null ? "" : safe(selected.getBanco()));
        TextField numero = new TextField(selected == null ? "" : safe(selected.getNumeroConta()));
        TextField titular = new TextField(selected == null ? "" : safe(selected.getTitular()));
        TextField iban = new TextField(selected == null ? "" : safe(selected.getIban()));
        TextField swift = new TextField(selected == null ? "" : safe(selected.getSwiftCode()));
        TextField descricao = new TextField(selected == null ? "" : safe(selected.getDescricao()));
        TextField initialBalance = new TextField();
        initialBalance.setPromptText("0,00");
        if (selected == null) initialBalance.setText("0,00");
        else initialBalance.setEditable(false);
        CheckBox principal = new CheckBox("Definir como conta principal");
        principal.setSelected(selected != null && Boolean.TRUE.equals(selected.getIsPrincipal()));
        CheckBox active = new CheckBox("Conta activa");
        active.setSelected(selected == null || Boolean.TRUE.equals(selected.getIsAtivo()));

        GridPane form = formGrid();
        form.addRow(0, new Label("Banco *"), banco);
        form.addRow(1, new Label("N.º da conta *"), numero);
        form.addRow(2, new Label("Titular *"), titular);
        form.addRow(3, new Label("IBAN *"), iban);
        form.addRow(4, new Label("SWIFT/BIC"), swift);
        form.addRow(5, new Label("Descrição"), descricao);
        form.addRow(6, new Label(selected == null ? "Saldo de abertura" : "Saldo actual"),
                initialBalance);
        form.addRow(7, new Label("Opções"), principal);
        form.addRow(8, new Label("Estado"), active);
        for (int i = 0; i <= 6; i++) {
            if (form.getChildren().get(i * 2 + 1) instanceof Control control) {
                GridPane.setHgrow(control, Priority.ALWAYS);
            }
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(selected == null ? "Nova conta bancária" : "Editar conta bancária");
        dialog.setHeaderText(selected == null
                ? "O saldo de abertura gera automaticamente um movimento de crédito."
                : "O saldo actual não pode ser alterado pelo formulário de cadastro.");
        dialog.getDialogPane().getButtonTypes().addAll(
                new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE), ButtonType.CANCEL);
        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().setPrefWidth(555);

        if (dialog.showAndWait().filter(button -> button.getButtonData() == ButtonBar.ButtonData.OK_DONE).isPresent()) {
            try {
                ContaBancaria data = new ContaBancaria();
                if (selected != null) data.setId(selected.getId());
                data.setBanco(banco.getText());
                data.setNumeroConta(numero.getText());
                data.setTitular(titular.getText());
                data.setIban(iban.getText());
                data.setSwiftCode(swift.getText());
                data.setDescricao(descricao.getText());
                data.setIsPrincipal(principal.isSelected());
                data.setIsAtivo(active.isSelected());
                data.setSaldo(selected == null ? parseAmount(initialBalance.getText()) : selected.getSaldo());
                contaService.salvar(data);
                refresh();
                FinanceiroUiSupport.info("Contas bancárias", selected == null
                        ? "Conta bancária criada." : "Cadastro actualizado.");
            } catch (Exception ex) {
                FinanceiroUiSupport.error("Não foi possível guardar a conta", ex);
            }
        }
    }

    private void movement(ContaBancaria conta, TipoMovimentoBancario tipo) {
        if (!Boolean.TRUE.equals(conta.getIsAtivo())) {
            FinanceiroUiSupport.info("Conta inactiva", "Active a conta antes de registar movimentos.");
            return;
        }
        TextField amount = new TextField();
        amount.setPromptText("0,00");
        TextField description = new TextField();
        description.setPromptText("Descrição do movimento");
        TextField category = new TextField();
        category.setPromptText("Categoria (opcional)");
        TextField reference = new TextField();
        reference.setPromptText("Documento (opcional)");

        GridPane form = formGrid();
        form.addRow(0, new Label("Conta"), new Label(conta.toString()));
        form.addRow(1, new Label("Saldo actual"), new Label(FinanceiroUiSupport.money(conta.getSaldo())));
        form.addRow(2, new Label("Valor *"), amount);
        form.addRow(3, new Label("Descrição *"), description);
        form.addRow(4, new Label("Categoria"), category);
        form.addRow(5, new Label("Referência"), reference);
        for (int i = 2; i < 6; i++) {
            GridPane.setHgrow(form.getChildren().get(i * 2 + 1), Priority.ALWAYS);
        }

        boolean credit = tipo == TipoMovimentoBancario.CREDITO;
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(credit ? "Registar crédito bancário" : "Registar débito bancário");
        dialog.setHeaderText((credit ? "Entrada de fundos · " : "Saída de fundos · ") + conta.toString());
        dialog.getDialogPane().getButtonTypes().addAll(
                new ButtonType("Registar", ButtonBar.ButtonData.OK_DONE), ButtonType.CANCEL);
        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().setPrefWidth(525);

        if (dialog.showAndWait().filter(button -> button.getButtonData() == ButtonBar.ButtonData.OK_DONE).isPresent()) {
            try {
                BigDecimal value = parseAmount(amount.getText());
                if (credit) {
                    contaService.registarCredito(conta.getId(), value, description.getText(), category.getText(), reference.getText());
                } else {
                    contaService.registarDebito(conta.getId(), value, description.getText(), category.getText(), reference.getText());
                }
                refresh();
                FinanceiroUiSupport.info("Movimento bancário", "Movimento registado e saldo actualizado.");
            } catch (Exception ex) {
                FinanceiroUiSupport.error("Não foi possível registar o movimento", ex);
            }
        }
    }

    private void transferir() {
        List<ContaBancaria> activeAccounts = contaService.findAllAtivas();
        if (activeAccounts.size() < 2) {
            FinanceiroUiSupport.info("Transferência não disponível",
                    "Cadastre pelo menos duas contas bancárias activas para efectuar uma transferência.");
            return;
        }
        ComboBox<ContaBancaria> origin = new ComboBox<>(FXCollections.observableArrayList(activeAccounts));
        ComboBox<ContaBancaria> destination = new ComboBox<>(FXCollections.observableArrayList(activeAccounts));
        origin.setPromptText("Conta de origem");
        destination.setPromptText("Conta de destino");
        origin.setMaxWidth(Double.MAX_VALUE);
        destination.setMaxWidth(Double.MAX_VALUE);
        TextField amount = new TextField();
        amount.setPromptText("0,00");
        TextField description = new TextField();
        description.setPromptText("Motivo da transferência");
        GridPane form = formGrid();
        form.addRow(0, new Label("Origem *"), origin);
        form.addRow(1, new Label("Destino *"), destination);
        form.addRow(2, new Label("Valor *"), amount);
        form.addRow(3, new Label("Descrição *"), description);
        GridPane.setHgrow(origin, Priority.ALWAYS);
        GridPane.setHgrow(destination, Priority.ALWAYS);
        GridPane.setHgrow(amount, Priority.ALWAYS);
        GridPane.setHgrow(description, Priority.ALWAYS);

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Transferência entre contas");
        dialog.setHeaderText("A transferência gera um débito e um crédito na mesma transacção.");
        dialog.getDialogPane().getButtonTypes().addAll(
                new ButtonType("Transferir", ButtonBar.ButtonData.OK_DONE), ButtonType.CANCEL);
        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().setPrefWidth(555);

        if (dialog.showAndWait().filter(button -> button.getButtonData() == ButtonBar.ButtonData.OK_DONE).isPresent()) {
            try {
                if (origin.getValue() == null || destination.getValue() == null) {
                    throw new IllegalArgumentException("Seleccione as contas de origem e destino.");
                }
                contaService.transferir(origin.getValue().getId(), destination.getValue().getId(),
                        parseAmount(amount.getText()), description.getText());
                refresh();
                FinanceiroUiSupport.info("Transferência", "Transferência concluída com os dois movimentos registados.");
            } catch (Exception ex) {
                FinanceiroUiSupport.error("Não foi possível efectuar a transferência", ex);
            }
        }
    }

    private void showStatement(ContaBancaria conta) {
        try {
            List<MovimentoBancario> history = contaService.getUltimosMovimentos(conta.getId());
            ObservableList<MovimentoBancario> data = FXCollections.observableArrayList(history);
            TableView<MovimentoBancario> movements = new TableView<>(data);
            FinanceiroUiSupport.styleTable(movements);

            TableColumn<MovimentoBancario, String> date = new TableColumn<>("Data");
            date.setCellValueFactory(cell -> new SimpleStringProperty(formatDate(cell.getValue().getDataMovimento())));
            TableColumn<MovimentoBancario, String> type = new TableColumn<>("Tipo");
            type.setCellValueFactory(cell -> new SimpleStringProperty(
                    cell.getValue().getTipo() == null ? "" : cell.getValue().getTipo().getDescricao()));
            TableColumn<MovimentoBancario, String> description = new TableColumn<>("Descrição");
            description.setCellValueFactory(cell -> new SimpleStringProperty(safe(cell.getValue().getDescricao())));
            TableColumn<MovimentoBancario, String> reference = new TableColumn<>("Documento");
            reference.setCellValueFactory(cell -> new SimpleStringProperty(safe(cell.getValue().getReferenciaDocumento())));
            TableColumn<MovimentoBancario, String> value = new TableColumn<>("Valor");
            value.setCellValueFactory(cell -> new SimpleStringProperty(FinanceiroUiSupport.money(cell.getValue().getValor())));
            TableColumn<MovimentoBancario, String> balance = new TableColumn<>("Saldo após");
            balance.setCellValueFactory(cell -> new SimpleStringProperty(FinanceiroUiSupport.money(cell.getValue().getSaldoAtual())));
            movements.getColumns().setAll(date, type, description, reference, value, balance);
            movements.setPrefHeight(390);

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Extracto bancário");
            dialog.setHeaderText(conta.getBanco() + " · " + conta.getNumeroConta()
                    + " · saldo " + FinanceiroUiSupport.money(conta.getSaldo())
                    + " · últimos " + history.size() + " movimentos");
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
            dialog.getDialogPane().setContent(movements);
            dialog.getDialogPane().setPrefSize(930, 520);
            dialog.showAndWait();
        } catch (Exception ex) {
            FinanceiroUiSupport.error("Não foi possível consultar o extracto", ex);
        }
    }

    private void deactivate(ContaBancaria conta) {
        if (!FinanceiroUiSupport.confirm("Desactivar conta bancária",
                "A conta " + conta.getBanco() + " · " + conta.getNumeroConta()
                        + " deixará de aceitar movimentos. O histórico será preservado.")) {
            return;
        }
        try {
            ContaBancaria data = new ContaBancaria();
            data.setId(conta.getId());
            data.setBanco(conta.getBanco());
            data.setNumeroConta(conta.getNumeroConta());
            data.setTitular(conta.getTitular());
            data.setDescricao(conta.getDescricao());
            data.setIban(conta.getIban());
            data.setSwiftCode(conta.getSwiftCode());
            data.setIsAtivo(false);
            data.setIsPrincipal(false);
            contaService.salvar(data);
            refresh();
        } catch (Exception ex) {
            FinanceiroUiSupport.error("Não foi possível desactivar a conta", ex);
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
            throw new IllegalArgumentException("Informe o valor.");
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
            throw new IllegalArgumentException("Valor inválido. Utilize, por exemplo, 1500,00.");
        }
    }

    private String formatDate(LocalDateTime value) {
        return value == null ? "—" : DATE_FORMAT.format(value);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}

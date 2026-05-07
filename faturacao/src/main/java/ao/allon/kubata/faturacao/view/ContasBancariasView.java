package ao.allon.kubata.faturacao.view;

import ao.allon.kubata.faturacao.domain.ContaBancaria;
import ao.allon.kubata.faturacao.domain.MovimentoBancario;
import ao.allon.kubata.faturacao.domain.enums.BancoAngola;
import ao.allon.kubata.faturacao.domain.enums.TipoMovimentoBancario;
import ao.allon.kubata.faturacao.service.ContaBancariaService;
import ao.allon.kubata.faturacao.ui.modal.ModalService;
import ao.allon.kubata.faturacao.ui.util.AlertUtils;
import ao.allon.kubata.faturacao.ui.util.IconUtils;
import atlantafx.base.controls.Card;
import atlantafx.base.controls.Tile;
import atlantafx.base.theme.Styles;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

public class ContasBancariasView extends BorderPane {

    private final ContaBancariaService contaService;
    private final ModalService modalService;

    private VBox listaContasBox;
    private TableView<MovimentoBancario> tableMovimentos;
    private Label lblSaldoTotal;
    private Label lblContaSelecionada;
    private ContaBancaria contaSelecionada;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public ContasBancariasView(ContaBancariaService contaService, ModalService modalService) {
        this.contaService = contaService;
        this.modalService = modalService;
        
        getStyleClass().add("bancos-view");
        setPadding(new Insets(20));
        
        setLeft(buildSidebar());
        setCenter(buildMainContent());
        
        refreshContas();
    }

    private VBox buildSidebar() {
        VBox sidebar = new VBox(15);
        sidebar.setPrefWidth(320);
        sidebar.setPadding(new Insets(0, 20, 0, 0));
        sidebar.setStyle("-fx-border-color: -color-border-default; -fx-border-width: 0 1 0 0;");

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("Contas Bancárias");
        title.getStyleClass().add(Styles.TITLE_4);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button btnNova = new Button(null, new FontIcon(Feather.PLUS));
        btnNova.getStyleClass().addAll(Styles.BUTTON_CIRCLE, Styles.ACCENT);
        btnNova.setTooltip(new Tooltip("Nova Conta"));
        btnNova.setOnAction(e -> showNovaContaDialog(null));
        header.getChildren().addAll(title, spacer, btnNova);

        lblSaldoTotal = new Label("Total: Kz 0,00");
        lblSaldoTotal.getStyleClass().addAll(Styles.TEXT_MUTED, Styles.TEXT_SMALL);

        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        listaContasBox = new VBox(10);
        scroll.setContent(listaContasBox);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        sidebar.getChildren().addAll(header, lblSaldoTotal, scroll);
        return sidebar;
    }

    private VBox buildMainContent() {
        VBox main = new VBox(15);
        main.setPadding(new Insets(0, 0, 0, 20));

        // Header
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);
        
        VBox titleBox = new VBox(5);
        lblContaSelecionada = new Label("Selecione uma conta");
        lblContaSelecionada.getStyleClass().add(Styles.TITLE_3);
        Label lblSub = new Label("Extrato e Movimentos");
        lblSub.getStyleClass().add(Styles.TEXT_MUTED);
        titleBox.getChildren().addAll(lblContaSelecionada, lblSub);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnDepositar = new Button("Depositar", new FontIcon(Feather.ARROW_DOWN_LEFT));
        btnDepositar.getStyleClass().add(Styles.SUCCESS);
        btnDepositar.setOnAction(e -> showTransacaoDialog(TipoMovimentoBancario.CREDITO));

        Button btnLevantar = new Button("Levantar", new FontIcon(Feather.ARROW_UP_RIGHT));
        btnLevantar.getStyleClass().add(Styles.DANGER);
        btnLevantar.setOnAction(e -> showTransacaoDialog(TipoMovimentoBancario.DEBITO));
        
        Button btnTransferir = new Button("Transferir", new FontIcon(Feather.REPEAT));
        btnTransferir.setOnAction(e -> showTransferenciaDialog());

        header.getChildren().addAll(titleBox, spacer, btnDepositar, btnLevantar, btnTransferir);

        // Table
        tableMovimentos = new TableView<>();
        tableMovimentos.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(tableMovimentos, Priority.ALWAYS);

        TableColumn<MovimentoBancario, String> colData = new TableColumn<>("Data");
        colData.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDataMovimento().format(DATE_FMT)));
        
        TableColumn<MovimentoBancario, String> colDesc = new TableColumn<>("Descrição");
        colDesc.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDescricao()));
        
        TableColumn<MovimentoBancario, String> colCat = new TableColumn<>("Categoria");
        colCat.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCategoria()));

        TableColumn<MovimentoBancario, String> colValor = new TableColumn<>("Valor");
        colValor.setCellValueFactory(c -> new SimpleStringProperty(String.format("Kz %.2f", c.getValue().getValor())));
        colValor.setStyle("-fx-alignment: CENTER-RIGHT;");
        colValor.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    MovimentoBancario m = getTableView().getItems().get(getIndex());
                    if (m.getTipo() == TipoMovimentoBancario.CREDITO) {
                        setStyle("-fx-text-fill: -color-success-fg; -fx-alignment: CENTER-RIGHT;");
                    } else {
                        setStyle("-fx-text-fill: -color-danger-fg; -fx-alignment: CENTER-RIGHT;");
                    }
                }
            }
        });

        TableColumn<MovimentoBancario, String> colSaldo = new TableColumn<>("Saldo");
        colSaldo.setCellValueFactory(c -> new SimpleStringProperty(String.format("Kz %.2f", c.getValue().getSaldoAtual())));
        colSaldo.setStyle("-fx-alignment: CENTER-RIGHT; -fx-font-weight: bold;");

        tableMovimentos.getColumns().addAll(colData, colDesc, colCat, colValor, colSaldo);
        tableMovimentos.setPlaceholder(new Label("Selecione uma conta para ver o extrato"));

        main.getChildren().addAll(header, tableMovimentos);
        return main;
    }

    private void refreshContas() {
        listaContasBox.getChildren().clear();
        List<ContaBancaria> contas = contaService.findAll();
        
        BigDecimal total = BigDecimal.ZERO;
        for (ContaBancaria c : contas) {
            total = total.add(c.getSaldo());
            listaContasBox.getChildren().add(createContaCard(c));
        }
        lblSaldoTotal.setText(String.format("Total Global: Kz %.2f", total));
        
        if (contaSelecionada != null) {
            // Re-select current account to refresh balance
            contaSelecionada = contaService.findById(contaSelecionada.getId()).orElse(null);
            if (contaSelecionada != null) {
                selectConta(contaSelecionada);
            } else {
                tableMovimentos.getItems().clear();
                lblContaSelecionada.setText("Selecione uma conta");
            }
        } else if (!contas.isEmpty()) {
            selectConta(contas.get(0));
        }
    }

    private Tile createContaCard(ContaBancaria c) {
        Tile tile = new Tile(
            c.getBanco().getNome(),
            c.getDescricao() + "\n" + c.getIban()
        );
        tile.setPrefWidth(280);
        tile.setAction(new Label(String.format("Kz %.2f", c.getSaldo())));
        ((Label) tile.getAction()).setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        
        if (contaSelecionada != null && contaSelecionada.getId().equals(c.getId())) {
            tile.getStyleClass().add(Styles.ACCENT);
        }
        
        tile.setOnMouseClicked(e -> selectConta(c));
        return tile;
    }

    private void selectConta(ContaBancaria c) {
        this.contaSelecionada = c;
        lblContaSelecionada.setText(c.getDescricao() + " (" + c.getBanco() + ")");
        refreshContasUIOnly(); // Update selection style without clearing list
        
        List<MovimentoBancario> movimentos = contaService.getUltimosMovimentos(c.getId());
        tableMovimentos.setItems(FXCollections.observableArrayList(movimentos));
    }
    
    private void refreshContasUIOnly() {
        // Simple refresh to update selection state
        int idx = 0;
        for (javafx.scene.Node n : listaContasBox.getChildren()) {
            if (n instanceof Tile t) {
                t.getStyleClass().remove(Styles.ACCENT);
                if (contaSelecionada != null) {
                    // Check if this tile corresponds to selected account (dirty check by title/desc or tag)
                    // Better would be to store ID in userData
                    // But here we iterate list which is in sync
                }
            }
            idx++;
        }
        // Rebuilding list is safer for now
        listaContasBox.getChildren().clear();
        List<ContaBancaria> contas = contaService.findAll();
        for (ContaBancaria c : contas) {
            Tile t = createContaCard(c);
            listaContasBox.getChildren().add(t);
        }
    }

    private void showNovaContaDialog(ContaBancaria editar) {
        VBox form = new VBox(15);
        
        ComboBox<BancoAngola> cbBanco = new ComboBox<>(FXCollections.observableArrayList(BancoAngola.values()));
        cbBanco.setPromptText("Selecione o Banco");
        cbBanco.setMaxWidth(Double.MAX_VALUE);
        
        TextField txtDesc = new TextField();
        txtDesc.setPromptText("Descrição (ex: Conta Principal)");
        
        TextField txtNum = new TextField();
        txtNum.setPromptText("Número da Conta");
        
        TextField txtIban = new TextField();
        txtIban.setPromptText("IBAN (AO06...)");
        
        TextField txtSwift = new TextField();
        txtSwift.setPromptText("SWIFT/BIC");
        
        TextField txtSaldo = new TextField("0");
        txtSaldo.setPromptText("Saldo Inicial");

        form.getChildren().addAll(
            new Label("Banco"), cbBanco,
            new Label("Descrição"), txtDesc,
            new Label("Número da Conta"), txtNum,
            new Label("IBAN"), txtIban,
            new Label("SWIFT"), txtSwift,
            new Label("Saldo Inicial"), txtSaldo
        );

        if (editar != null) {
            cbBanco.setValue(editar.getBanco());
            txtDesc.setText(editar.getDescricao());
            txtNum.setText(editar.getNumeroConta());
            txtIban.setText(editar.getIban());
            txtSwift.setText(editar.getSwift());
            txtSaldo.setText(editar.getSaldo().toString());
            txtSaldo.setDisable(true); // Don't edit balance directly
        }

        modalService.create()
            .title(editar == null ? "Nova Conta Bancária" : "Editar Conta")
            .content(form)
            .width(400)
            .withConfirmButton("Salvar", () -> {
                try {
                    if (cbBanco.getValue() == null) throw new IllegalArgumentException("Selecione o banco");
                    if (txtIban.getText().isEmpty()) throw new IllegalArgumentException("IBAN é obrigatório");
                    
                    ContaBancaria c = editar != null ? editar : new ContaBancaria();
                    c.setBanco(cbBanco.getValue());
                    c.setDescricao(txtDesc.getText());
                    c.setNumeroConta(txtNum.getText());
                    c.setIban(txtIban.getText());
                    c.setSwift(txtSwift.getText());
                    if (editar == null) {
                        c.setSaldo(new BigDecimal(txtSaldo.getText().replace(",", ".")));
                    }
                    
                    contaService.salvar(c);
                    refreshContas();
                    return true;
                } catch (Exception ex) {
                    AlertUtils.showWarningAlert("Erro", ex.getMessage());
                    return false;
                }
            })
            .withCancelButton()
            .buildAndShow();
    }

    private void showTransacaoDialog(TipoMovimentoBancario tipo) {
        if (contaSelecionada == null) {
            AlertUtils.showWarningAlert("Aviso", "Selecione uma conta primeiro.");
            return;
        }

        VBox form = new VBox(15);
        
        TextField txtValor = new TextField();
        txtValor.setPromptText("0,00");
        
        TextField txtDesc = new TextField();
        txtDesc.setPromptText("Descrição da operação");
        
        ComboBox<String> cbCat = new ComboBox<>(FXCollections.observableArrayList(
            "Depósito", "Levantamento", "Pagamento Fornecedor", "Recebimento Cliente", "Taxas", "Outros"
        ));
        cbCat.setEditable(true);
        cbCat.setMaxWidth(Double.MAX_VALUE);
        cbCat.setValue(tipo == TipoMovimentoBancario.CREDITO ? "Depósito" : "Levantamento");

        TextField txtRef = new TextField();
        txtRef.setPromptText("Referência Doc. (Opcional)");

        form.getChildren().addAll(
            new Label("Valor (Kz)"), txtValor,
            new Label("Descrição"), txtDesc,
            new Label("Categoria"), cbCat,
            new Label("Referência"), txtRef
        );

        modalService.create()
            .title(tipo == TipoMovimentoBancario.CREDITO ? "Novo Depósito" : "Novo Levantamento")
            .content(form)
            .width(350)
            .withConfirmButton("Confirmar", () -> {
                try {
                    BigDecimal valor = new BigDecimal(txtValor.getText().replace(",", "."));
                    String desc = txtDesc.getText();
                    String cat = cbCat.getValue();
                    String ref = txtRef.getText();
                    
                    if (tipo == TipoMovimentoBancario.CREDITO) {
                        contaService.registrarCredito(contaSelecionada.getId(), valor, desc, cat, ref);
                    } else {
                        contaService.registrarDebito(contaSelecionada.getId(), valor, desc, cat, ref);
                    }
                    
                    refreshContas();
                    return true;
                } catch (Exception ex) {
                    AlertUtils.showExceptionAlert("Erro", "Falha na operação", ex);
                    return false;
                }
            })
            .withCancelButton()
            .buildAndShow();
    }

    private void showTransferenciaDialog() {
        if (contaSelecionada == null) {
            AlertUtils.showWarningAlert("Aviso", "Selecione a conta de origem.");
            return;
        }

        VBox form = new VBox(15);
        
        ComboBox<ContaBancaria> cbDestino = new ComboBox<>();
        List<ContaBancaria> contas = contaService.findAll();
        contas.removeIf(c -> c.getId().equals(contaSelecionada.getId()));
        cbDestino.setItems(FXCollections.observableArrayList(contas));
        cbDestino.setMaxWidth(Double.MAX_VALUE);
        
        TextField txtValor = new TextField();
        txtValor.setPromptText("0,00");
        
        TextField txtDesc = new TextField();
        txtDesc.setPromptText("Motivo da transferência");

        form.getChildren().addAll(
            new Label("Conta Destino"), cbDestino,
            new Label("Valor (Kz)"), txtValor,
            new Label("Descrição"), txtDesc
        );

        modalService.create()
            .title("Transferência entre Contas")
            .content(form)
            .width(400)
            .withConfirmButton("Transferir", () -> {
                try {
                    ContaBancaria destino = cbDestino.getValue();
                    if (destino == null) throw new IllegalArgumentException("Selecione a conta de destino");
                    
                    BigDecimal valor = new BigDecimal(txtValor.getText().replace(",", "."));
                    String desc = txtDesc.getText();
                    
                    contaService.transferir(contaSelecionada.getId(), destino.getId(), valor, desc);
                    
                    refreshContas();
                    return true;
                } catch (Exception ex) {
                    AlertUtils.showExceptionAlert("Erro", "Falha na transferência", ex);
                    return false;
                }
            })
            .withCancelButton()
            .buildAndShow();
    }
}

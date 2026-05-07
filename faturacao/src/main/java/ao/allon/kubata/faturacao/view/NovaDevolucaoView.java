package ao.allon.kubata.faturacao.view;

import ao.allon.kubata.faturacao.domain.*;
import ao.allon.kubata.faturacao.service.ClienteService;
import ao.allon.kubata.faturacao.service.DevolucaoService;
import ao.allon.kubata.faturacao.service.FaturaService;
import ao.allon.kubata.faturacao.service.ProdutoService;
import ao.allon.kubata.faturacao.ui.modal.ModalService;
import ao.allon.kubata.faturacao.ui.util.AlertUtils;
import ao.allon.kubata.faturacao.ui.util.IconUtils;
import atlantafx.base.theme.Styles;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import org.kordamp.ikonli.feather.Feather;

import java.math.BigDecimal;
import java.util.List;

public class NovaDevolucaoView extends BorderPane {

    private final DevolucaoService devolucaoService;
    private final FaturaService faturaService;
    private final ClienteService clienteService;
    private final ProdutoService produtoService;
    private final ModalService modalService;

    private ComboBox<Cliente> cbCliente;
    private ComboBox<Fatura> cbFaturaOrigem;
    private TextArea txtMotivo;
    
    private ComboBox<Produto> cbProduto;
    private TextField txtQuantidade;
    private TextArea txtMotivoItem;
    private TableView<ItemDevolucao> tabelaItens;
    
    private final Devolucao devolucaoAtual = new Devolucao();
    private final ObservableList<ItemDevolucao> itensObservable = FXCollections.observableArrayList();

    public NovaDevolucaoView(DevolucaoService devolucaoService, FaturaService faturaService, ClienteService clienteService, ProdutoService produtoService, ModalService modalService) {
        this.devolucaoService = devolucaoService;
        this.faturaService = faturaService;
        this.clienteService = clienteService;
        this.produtoService = produtoService;
        this.modalService = modalService;
        
        initialize();
    }

    private void initialize() {
        setPadding(new Insets(16));
        setCenter(buildForm());
        setBottom(buildFooter());
    }

    private VBox buildForm() {
        VBox root = new VBox(15);
        
        // Cabeçalho
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        
        cbCliente = new ComboBox<>();
        cbCliente.setMaxWidth(Double.MAX_VALUE);
        cbCliente.setItems(FXCollections.observableArrayList(clienteService.findAll()));
        cbCliente.setConverter(new StringConverter<>() {
            @Override public String toString(Cliente c) { return c != null ? c.getNome() : ""; }
            @Override public Cliente fromString(String s) { return null; }
        });
        cbCliente.setOnAction(e -> carregarFaturasDoCliente());

        cbFaturaOrigem = new ComboBox<>();
        cbFaturaOrigem.setMaxWidth(Double.MAX_VALUE);
        cbFaturaOrigem.setDisable(true);
        cbFaturaOrigem.setConverter(new StringConverter<>() {
            @Override public String toString(Fatura f) { return f != null ? f.getNumero() + " (" + f.getDataEmissao() + ")" : ""; }
            @Override public Fatura fromString(String s) { return null; }
        });
        
        txtMotivo = new TextArea();
        txtMotivo.setPromptText("Descreva o motivo geral da devolução...");
        txtMotivo.setPrefRowCount(2);

        grid.addRow(0, new Label("Cliente:"), cbCliente);
        grid.addRow(1, new Label("Fatura Origem:"), cbFaturaOrigem);
        grid.addRow(2, new Label("Motivo Geral:"), txtMotivo);
        
        // Adição de Itens
        VBox itemBox = new VBox(10);
        itemBox.setStyle("-fx-border-color: -color-border-default; -fx-border-width: 1; -fx-padding: 10;");
        itemBox.getChildren().add(new Label("Adicionar Itens"));
        
        HBox inputLine = new HBox(10);
        inputLine.setAlignment(Pos.BOTTOM_LEFT);
        
        cbProduto = new ComboBox<>();
        cbProduto.setPromptText("Selecione o Produto");
        cbProduto.setPrefWidth(200);
        cbProduto.setItems(FXCollections.observableArrayList(produtoService.findAll())); // Idealmente filtrar pelos itens da fatura selecionada
        cbProduto.setConverter(new StringConverter<>() {
            @Override public String toString(Produto p) { return p != null ? p.getNome() : ""; }
            @Override public Produto fromString(String s) { return null; }
        });
        
        txtQuantidade = new TextField("1");
        txtQuantidade.setPrefWidth(60);
        
        Button btnAdd = new Button("", IconUtils.icon(Feather.PLUS, IconUtils.SIZE_SMALL));
        btnAdd.setOnAction(e -> adicionarItem());
        
        inputLine.getChildren().addAll(new Label("Produto"), cbProduto, new Label("Qtd"), txtQuantidade, btnAdd);
        
        txtMotivoItem = new TextArea();
        txtMotivoItem.setPromptText("Motivo específico do item (opcional)");
        txtMotivoItem.setPrefRowCount(1);
        
        itemBox.getChildren().addAll(inputLine, txtMotivoItem);
        
        // Tabela
        tabelaItens = new TableView<>();
        tabelaItens.setItems(itensObservable);
        tabelaItens.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        tabelaItens.setPrefHeight(200);
        
        TableColumn<ItemDevolucao, String> colProd = new TableColumn<>("Produto");
        colProd.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getProduto().getNome()));
        
        TableColumn<ItemDevolucao, Integer> colQtd = new TableColumn<>("Qtd");
        colQtd.setCellValueFactory(new PropertyValueFactory<>("quantidade"));
        
        TableColumn<ItemDevolucao, String> colMotivo = new TableColumn<>("Motivo");
        colMotivo.setCellValueFactory(new PropertyValueFactory<>("motivoItem"));
        
        TableColumn<ItemDevolucao, Void> colAcao = new TableColumn<>("Ação");
        colAcao.setCellFactory(param -> new TableCell<>() {
            private final Button btnRemover = new Button("", IconUtils.icon(Feather.TRASH, IconUtils.SIZE_SMALL));
            {
                btnRemover.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.DANGER);
                btnRemover.setOnAction(e -> {
                    ItemDevolucao item = getTableView().getItems().get(getIndex());
                    devolucaoAtual.removeItem(item);
                    itensObservable.remove(item);
                });
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btnRemover);
            }
        });
        
        tabelaItens.getColumns().addAll(colProd, colQtd, colMotivo, colAcao);
        
        root.getChildren().addAll(grid, new Separator(), itemBox, tabelaItens);
        return root;
    }
    
    private HBox buildFooter() {
        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER_RIGHT);
        
        Button btnSalvar = new Button("Salvar Solicitação");
        btnSalvar.getStyleClass().add(Styles.SUCCESS);
        btnSalvar.setOnAction(e -> salvar());
        
        box.getChildren().add(btnSalvar);
        return box;
    }
    
    private void carregarFaturasDoCliente() {
        Cliente c = cbCliente.getValue();
        if (c != null) {
            // Buscar faturas emitidas do cliente (simplificado)
            // Idealmente: faturaService.findByClienteAndStatus(c.getId(), StatusFatura.EMITIDA)
            List<Fatura> faturas = faturaService.findAll().stream()
                    .filter(f -> f.getCliente().getId().equals(c.getId()) && f.getStatus() == ao.allon.kubata.faturacao.domain.enums.StatusFatura.EMITIDA)
                    .toList();
            cbFaturaOrigem.setItems(FXCollections.observableArrayList(faturas));
            cbFaturaOrigem.setDisable(false);
        } else {
            cbFaturaOrigem.getItems().clear();
            cbFaturaOrigem.setDisable(true);
        }
    }
    
    private void adicionarItem() {
        Produto p = cbProduto.getValue();
        if (p == null) return;
        
        int qtd;
        try {
            qtd = Integer.parseInt(txtQuantidade.getText());
            if (qtd <= 0) throw new NumberFormatException();
        } catch (Exception e) {
            AlertUtils.showWarningAlert("Qtd", "Quantidade inválida");
            return;
        }
        
        ItemDevolucao item = new ItemDevolucao();
        item.setProduto(p);
        item.setQuantidade(qtd);
        item.setMotivoItem(txtMotivoItem.getText());
        item.setValorUnitario(p.getPrecoUnitario()); // Simplificação: usar preço atual. Ideal: preço da fatura.
        item.calculateTotal();
        
        devolucaoAtual.addItem(item);
        itensObservable.add(item);
        
        cbProduto.setValue(null);
        txtQuantidade.setText("1");
        txtMotivoItem.clear();
    }
    
    private void salvar() {
        if (cbCliente.getValue() == null) {
            AlertUtils.showWarningAlert("Erro", "Selecione o cliente.");
            return;
        }
        if (itensObservable.isEmpty()) {
            AlertUtils.showWarningAlert("Erro", "Adicione pelo menos um item.");
            return;
        }
        
        devolucaoAtual.setCliente(cbCliente.getValue());
        devolucaoAtual.setFaturaOrigem(cbFaturaOrigem.getValue());
        devolucaoAtual.setMotivoSolicitacao(txtMotivo.getText());
        
        try {
            devolucaoService.criarSolicitacao(devolucaoAtual);
            AlertUtils.showInfoAlert("Sucesso", "Solicitação criada com sucesso.");
            // Fechar modal - lógica no controller pai ou callback
        } catch (Exception e) {
            AlertUtils.showExceptionAlert("Erro", "Falha ao criar solicitação.", e);
        }
    }
}

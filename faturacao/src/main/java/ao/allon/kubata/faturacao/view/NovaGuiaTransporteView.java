package ao.allon.kubata.faturacao.view;

import ao.allon.kubata.faturacao.domain.Cliente;
import ao.allon.kubata.faturacao.domain.Fatura;
import ao.allon.kubata.faturacao.domain.ItemFatura;
import ao.allon.kubata.faturacao.domain.Produto;
import ao.allon.kubata.faturacao.domain.enums.TipoDocumento;
import ao.allon.kubata.faturacao.service.ClienteService;
import ao.allon.kubata.faturacao.service.FaturaService;
import ao.allon.kubata.faturacao.service.ProdutoService;
import ao.allon.kubata.faturacao.ui.modal.ModalService;
import ao.allon.kubata.faturacao.util.AngolaValidationUtils;
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

import java.math.BigDecimal;
import java.time.LocalDate;

public class NovaGuiaTransporteView extends BorderPane {

    private final FaturaService faturaService;
    private final ClienteService clienteService;
    private final ProdutoService produtoService;
    private final ModalService modalService;

    private final ComboBox<Cliente> cbCliente = new ComboBox<>();
    private final DatePicker dpEmissao = new DatePicker();
    private final TextField txtOrigem = new TextField();
    private final TextField txtDestino = new TextField();
    private final TextField txtTipoTransporte = new TextField();
    private final TextField txtMatricula = new TextField();
    private final TextField txtMotorista = new TextField();

    private final ComboBox<Produto> cbProduto = new ComboBox<>();
    private final TextField txtQuantidade = new TextField("1");
    private final TextField txtPesoUnitario = new TextField("0.00");
    private final Button btnAdicionar = new Button("Adicionar");

    private final TableView<ItemFatura> tabelaItens = new TableView<>();

    private final Button btnSalvar = new Button("Emitir Guia");
    private final Button btnRascunho = new Button("Salvar Rascunho");

    private Fatura guiaAtual = new Fatura();
    private final ObservableList<ItemFatura> itensObservable = FXCollections.observableArrayList();

    public NovaGuiaTransporteView(FaturaService faturaService, ClienteService clienteService, ProdutoService produtoService, ModalService modalService) {
        this.faturaService = faturaService;
        this.clienteService = clienteService;
        this.produtoService = produtoService;
        this.modalService = modalService;
        initialize();
    }

    private void initialize() {
        guiaAtual.setTipoDocumento(TipoDocumento.GUIA_TRANSPORTE);
        guiaAtual.setDataEmissao(LocalDate.now());
        setPadding(new Insets(16));
        setCenter(buildBody());
        setBottom(buildFooter());
    }

    private VBox buildBody() {
        VBox root = new VBox(12);
        root.setPadding(new Insets(8));

        Label title = new Label("Nova Guia de Transporte");
        title.getStyleClass().add(Styles.TITLE_3);
        root.getChildren().add(title);

        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(10);

        cbCliente.setItems(FXCollections.observableArrayList(clienteService.findAll()));
        cbCliente.setConverter(new StringConverter<>() {
            @Override
            public String toString(Cliente c) { return c != null ? c.getNome() + " (" + c.getNif() + ")" : ""; }
            @Override
            public Cliente fromString(String s) { return null; }
        });

        dpEmissao.setValue(guiaAtual.getDataEmissao());
        txtOrigem.setPromptText("Endereço de origem");
        txtDestino.setPromptText("Endereço de destino");
        txtTipoTransporte.setPromptText("Ex.: Rodoviário, Próprio, Terceirizado");
        txtMatricula.setPromptText("Matrícula da viatura");
        txtMotorista.setPromptText("Nome do motorista");

        form.addRow(0, new Label("Destinatário"), cbCliente);
        form.addRow(1, new Label("Emissão"), dpEmissao);
        form.addRow(2, new Label("Origem"), txtOrigem);
        form.addRow(3, new Label("Destino"), txtDestino);
        form.addRow(4, new Label("Tipo Transporte"), txtTipoTransporte);
        form.addRow(5, new Label("Matrícula"), txtMatricula);
        form.addRow(6, new Label("Motorista"), txtMotorista);

        HBox addBox = new HBox(8);
        addBox.setAlignment(Pos.CENTER_LEFT);
        cbProduto.setItems(FXCollections.observableArrayList(produtoService.findAll()));
        cbProduto.setConverter(new StringConverter<>() {
            @Override
            public String toString(Produto p) { return p != null ? p.getNome() + " | Stock: " + p.getStock() : ""; }
            @Override
            public Produto fromString(String s) { return null; }
        });
        txtQuantidade.setPrefWidth(80);
        txtPesoUnitario.setPrefWidth(100);
        btnAdicionar.getStyleClass().addAll(Styles.BUTTON_OUTLINED);
        btnAdicionar.setOnAction(e -> handleAdicionarItem());
        addBox.getChildren().addAll(new Label("Produto"), cbProduto, new Label("Qtd"), txtQuantidade, new Label("Peso (kg)"), txtPesoUnitario, btnAdicionar);

        tabelaItens.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        tabelaItens.setMinHeight(200);
        tabelaItens.setMaxHeight(400);
        TableColumn<ItemFatura, String> colItemProduto = new TableColumn<>("Descrição");
        colItemProduto.setCellValueFactory(new PropertyValueFactory<>("descricao"));
        TableColumn<ItemFatura, Integer> colItemQtd = new TableColumn<>("Qtd");
        colItemQtd.setCellValueFactory(new PropertyValueFactory<>("quantidade"));
        TableColumn<ItemFatura, String> colPeso = new TableColumn<>("Peso Total (kg)");
        colPeso.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getPesoTotal() != null ? c.getValue().getPesoTotal().toPlainString() : "0.00"));
        TableColumn<ItemFatura, Void> colItemAcao = new TableColumn<>("Ação");
        colItemAcao.setCellFactory(param -> new TableCell<>() {
            private final Button btnRemover = new Button("Remover");
            {
                btnRemover.getStyleClass().addAll(Styles.DANGER);
                btnRemover.setOnAction(e -> {
                    ItemFatura it = getTableView().getItems().get(getIndex());
                    removerItem(it);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btnRemover);
            }
        });
        tabelaItens.getColumns().setAll(colItemProduto, colItemQtd, colPeso, colItemAcao);
        tabelaItens.setItems(itensObservable);
        VBox.setVgrow(tabelaItens, Priority.ALWAYS);

        root.getChildren().addAll(form, new Separator(), addBox, tabelaItens);
        return root;
    }

    private HBox buildFooter() {
        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER_RIGHT);
        btnRascunho.getStyleClass().addAll(Styles.BUTTON_OUTLINED);
        btnRascunho.setOnAction(e -> handleSalvarRascunho());
        btnSalvar.getStyleClass().addAll(Styles.SUCCESS);
        btnSalvar.setText("Emitir Guia");
        btnSalvar.setOnAction(e -> handleEmitir());
        box.getChildren().addAll(btnRascunho, btnSalvar);
        return box;
    }

    private void handleAdicionarItem() {
        Produto produto = cbProduto.getValue();
        if (produto == null) {
            modalService.create().title("Erro").content(new Label("Selecione um produto.")).autoSize().withCancelButton("Fechar").buildAndShow();
            return;
        }
        int qtd;
        try {
            qtd = Integer.parseInt(txtQuantidade.getText());
            if (qtd <= 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            modalService.create().title("Erro").content(new Label("Quantidade inválida.")).autoSize().withCancelButton("Fechar").buildAndShow();
            return;
        }
        BigDecimal pesoUni;
        try {
            pesoUni = new BigDecimal(txtPesoUnitario.getText());
            if (pesoUni.compareTo(BigDecimal.ZERO) < 0) throw new NumberFormatException();
        } catch (Exception ex) {
            modalService.create().title("Erro").content(new Label("Peso unitário inválido.")).autoSize().withCancelButton("Fechar").buildAndShow();
            return;
        }
        if (produto.getStock() != null && produto.getStock() < qtd) {
            modalService.create().title("Erro").content(new Label("Stock insuficiente. Disponível: " + produto.getStock())).autoSize().withCancelButton("Fechar").buildAndShow();
            return;
        }
        ItemFatura item = new ItemFatura();
        item.setDescricao(produto.getNome());
        item.setQuantidade(qtd);
        item.setPrecoUnitario(produto.getPrecoUnitario());
        item.setPercentualIva(BigDecimal.ZERO);
        item.setProduto(produto);
        item.setPesoUnitario(pesoUni);
        item.calculateTotals();
        guiaAtual.addItem(item);
        itensObservable.add(item);
        cbProduto.setValue(null);
        txtQuantidade.setText("1");
        txtPesoUnitario.setText("0.00");
        cbProduto.requestFocus();
    }

    private void removerItem(ItemFatura item) {
        guiaAtual.removeItem(item);
        itensObservable.remove(item);
    }

    private boolean validar() {
        if (cbCliente.getValue() == null) {
            modalService.create().title("Erro").content(new Label("Selecione o destinatário.")).autoSize().withCancelButton("Fechar").buildAndShow();
            return false;
        }
        String nif = cbCliente.getValue().getNif();
        if (!AngolaValidationUtils.isValidNif(nif)) {
            modalService.create().title("Erro").content(new Label("NIF inválido segundo as regras de Angola.")).autoSize().withCancelButton("Fechar").buildAndShow();
            return false;
        }
        if (dpEmissao.getValue() == null) {
            modalService.create().title("Erro").content(new Label("Data de emissão é obrigatória.")).autoSize().withCancelButton("Fechar").buildAndShow();
            return false;
        }
        if (txtOrigem.getText().isBlank() || txtDestino.getText().isBlank()) {
            modalService.create().title("Erro").content(new Label("Origem e destino são obrigatórios.")).autoSize().withCancelButton("Fechar").buildAndShow();
            return false;
        }
        if (txtMatricula.getText().isBlank() || txtMotorista.getText().isBlank()) {
            modalService.create().title("Erro").content(new Label("Matrícula e motorista são obrigatórios.")).autoSize().withCancelButton("Fechar").buildAndShow();
            return false;
        }
        if (itensObservable.isEmpty()) {
            modalService.create().title("Erro").content(new Label("Adicione pelo menos um produto.")).autoSize().withCancelButton("Fechar").buildAndShow();
            return false;
        }
        return true;
    }

    private void prepararGuia() {
        guiaAtual.setCliente(cbCliente.getValue());
        guiaAtual.setDataEmissao(dpEmissao.getValue());
        guiaAtual.setDataVencimento(dpEmissao.getValue());
        guiaAtual.setLocalCarga(txtOrigem.getText());
        guiaAtual.setLocalDescarga(txtDestino.getText());
        guiaAtual.setMatriculaViatura(txtMatricula.getText());
        guiaAtual.setMotorista(txtMotorista.getText());
        guiaAtual.setTipoTransporte(txtTipoTransporte.getText());
        guiaAtual.setTipoDocumento(TipoDocumento.GUIA_TRANSPORTE);
    }

    private void handleSalvarRascunho() {
        if (!validar()) return;
        try {
            prepararGuia();
            faturaService.salvar(guiaAtual);
            modalService.create().title("Sucesso").content(new Label("Rascunho salvo com sucesso!")).autoSize().withCancelButton("Fechar").buildAndShow();
            limpar();
        } catch (Exception e) {
            modalService.create().title("Erro").content(new Label("Erro ao salvar: " + e.getMessage())).autoSize().withCancelButton("Fechar").buildAndShow();
        }
    }

    private void handleEmitir() {
        if (!validar()) return;
        try {
            prepararGuia();
            guiaAtual = faturaService.salvarEEmitir(guiaAtual);
            modalService.create().title("Sucesso").content(new Label("Guia emitida com sucesso!")).autoSize().withCancelButton("Fechar").buildAndShow();
            limpar();
        } catch (Exception e) {
            modalService.create().title("Erro").content(new Label("Erro ao emitir: " + e.getMessage())).autoSize().withCancelButton("Fechar").buildAndShow();
        }
    }

    private void limpar() {
        cbCliente.setValue(null);
        dpEmissao.setValue(LocalDate.now());
        txtOrigem.clear();
        txtDestino.clear();
        txtTipoTransporte.clear();
        txtMatricula.clear();
        txtMotorista.clear();
        cbProduto.setValue(null);
        txtQuantidade.setText("1");
        txtPesoUnitario.setText("0.00");
        itensObservable.clear();
        guiaAtual = new Fatura();
        guiaAtual.setTipoDocumento(TipoDocumento.GUIA_TRANSPORTE);
        guiaAtual.setDataEmissao(LocalDate.now());
    }
}

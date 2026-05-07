package ao.allon.kubata.faturacao.view;

import ao.allon.kubata.core.ui.table.AdvancedTableView;
import ao.allon.kubata.core.ui.table.TableUtils;
import ao.allon.kubata.faturacao.domain.Cliente;
import ao.allon.kubata.faturacao.domain.Fatura;
import ao.allon.kubata.faturacao.domain.ItemFatura;
import ao.allon.kubata.faturacao.domain.Produto;
import ao.allon.kubata.faturacao.service.ClienteService;
import ao.allon.kubata.faturacao.service.FaturaService;
import ao.allon.kubata.faturacao.service.ProdutoService;
import ao.allon.kubata.faturacao.ui.modal.ModalService;
import ao.allon.kubata.faturacao.domain.enums.TipoDocumento;
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
import org.kordamp.ikonli.javafx.FontIcon;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.Locale;

public class NovaFaturaView extends BorderPane {

    private final FaturaService faturaService;
    private final ClienteService clienteService;
    private final ProdutoService produtoService;
    private final ModalService modalService;

    private final ComboBox<Cliente> cbCliente = new ComboBox<>();
    private final DatePicker dpEmissao = new DatePicker();
    private final DatePicker dpVencimento = new DatePicker();

    private final ComboBox<Produto> cbProduto = new ComboBox<>();
    private final TextField txtQuantidade = new TextField("1");
    private final TextField txtCodigoIsencao = new TextField();
    private final TextField txtMotivoIsencao = new TextField();
    private final Button btnAdicionar = new Button("Adicionar");
    private final CheckBox chkModoFormacao = new CheckBox("Modo de Formação (treinamento)");

    private final AdvancedTableView<ItemFatura> tabelaItens = new AdvancedTableView<>();
    private final Label lblSubtotal = new Label();
    private final Label lblTotalIva = new Label();
    private final Label lblTotalGeral = new Label();

    private final Button btnSalvar = new Button("Emitir");
    private final Button btnRascunho = new Button("Salvar Rascunho");
    private final Button btnNovoCliente = new Button("", new FontIcon(Feather.USER_PLUS));

    private Fatura faturaAtual = new Fatura();
    private final ObservableList<ItemFatura> itensObservable = FXCollections.observableArrayList();
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(new Locale("pt", "AO"));

    private TipoDocumento tipoDocumento = TipoDocumento.FATURA;

    public NovaFaturaView(FaturaService faturaService, ClienteService clienteService, ProdutoService produtoService, ModalService modalService) {
        this.faturaService = faturaService;
        this.clienteService = clienteService;
        this.produtoService = produtoService;
        this.modalService = modalService;
        initialize();
    }

    public NovaFaturaView(FaturaService faturaService, ClienteService clienteService, ProdutoService produtoService, ModalService modalService, TipoDocumento tipoDocumento) {
        this.faturaService = faturaService;
        this.clienteService = clienteService;
        this.produtoService = produtoService;
        this.modalService = modalService;
        this.tipoDocumento = tipoDocumento != null ? tipoDocumento : TipoDocumento.FATURA;
        initialize();
    }

    public NovaFaturaView(FaturaService faturaService, ClienteService clienteService, ProdutoService produtoService, ModalService modalService, TipoDocumento tipoDocumento, Fatura existente) {
        this.faturaService = faturaService;
        this.clienteService = clienteService;
        this.produtoService = produtoService;
        this.modalService = modalService;
        this.tipoDocumento = tipoDocumento != null ? tipoDocumento : TipoDocumento.FATURA;
        initialize();
        if (existente != null) carregarFaturaExistente(existente);
    }

    private void initialize() {
        faturaAtual.setTipoDocumento(tipoDocumento);
        faturaAtual.setDataEmissao(LocalDate.now());
        faturaAtual.setDataVencimento(LocalDate.now().plusDays(30));
        setPadding(new Insets(16));
        setCenter(buildBody());
        setBottom(buildFooter());
        atualizarTotais();
    }

    private VBox buildBody() {
        VBox root = new VBox(12);
        root.setPadding(new Insets(8));

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

        dpEmissao.setValue(faturaAtual.getDataEmissao());
        dpVencimento.setValue(faturaAtual.getDataVencimento());

        btnNovoCliente.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.ACCENT);
        btnNovoCliente.setTooltip(new Tooltip("Novo Cliente"));
        btnNovoCliente.setOnAction(e -> showNovoClienteModal());

        form.addRow(0, new Label("Cliente"), new HBox(5, cbCliente, btnNovoCliente));
        form.addRow(1, new Label("Emissão"), dpEmissao);
        form.addRow(2, new Label("Vencimento"), dpVencimento);
        form.addRow(3, chkModoFormacao);

        HBox addBox = new HBox(8);
        addBox.setAlignment(Pos.CENTER_LEFT);
        cbProduto.setItems(FXCollections.observableArrayList(produtoService.findAll()));
        cbProduto.setConverter(new StringConverter<>() {
            @Override
            public String toString(Produto p) { return p != null ? p.getNome() + " | Stock: " + p.getStock() : ""; }
            @Override
            public Produto fromString(String s) { return null; }
        });
        cbProduto.valueProperty().addListener((obs, o, n) -> updateIsencaoFieldsState(n));
        txtQuantidade.setPrefWidth(80);

        txtCodigoIsencao.setPromptText("Código Isenção (Mxx)");
        txtCodigoIsencao.setPrefWidth(160);
        txtMotivoIsencao.setPromptText("Motivo Isenção");
        txtMotivoIsencao.setPrefWidth(220);

        updateIsencaoFieldsState(cbProduto.getValue());

        btnAdicionar.getStyleClass().addAll(Styles.BUTTON_OUTLINED);
        btnAdicionar.setOnAction(e -> handleAdicionarItem());
        addBox.getChildren().addAll(
            new Label("Produto"), cbProduto,
            new Label("Qtd"), txtQuantidade,
            txtCodigoIsencao, txtMotivoIsencao,
            btnAdicionar
        );

        TableUtils.standardize(tabelaItens);
        tabelaItens.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        tabelaItens.setMinHeight(200);
        tabelaItens.setMaxHeight(400);
        TableColumn<ItemFatura, String> colItemProduto = new TableColumn<>("Descrição");
        colItemProduto.setCellValueFactory(new PropertyValueFactory<>("descricao"));
        TableColumn<ItemFatura, Integer> colItemQtd = new TableColumn<>("Qtd");
        colItemQtd.setCellValueFactory(new PropertyValueFactory<>("quantidade"));
        TableColumn<ItemFatura, String> colItemPreco = new TableColumn<>("Preço");
        colItemPreco.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(CURRENCY_FORMAT.format(c.getValue().getPrecoUnitario())));
        TableColumn<ItemFatura, String> colItemIva = new TableColumn<>("IVA");
        colItemIva.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getPercentualIva() + "%"));
        TableColumn<ItemFatura, String> colItemTotal = new TableColumn<>("Total");
        colItemTotal.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(CURRENCY_FORMAT.format(c.getValue().getTotal())));
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
        tabelaItens.getColumns().setAll(colItemProduto, colItemQtd, colItemPreco, colItemIva, colItemTotal, colItemAcao);
        tabelaItens.setData(itensObservable);
        VBox.setVgrow(tabelaItens, Priority.ALWAYS);

        HBox totals = new HBox(20, new Label("Subtotal"), lblSubtotal, new Label("IVA"), lblTotalIva, new Label("Total"), lblTotalGeral);
        totals.setAlignment(Pos.CENTER_RIGHT);
        totals.setPadding(new Insets(8, 0, 0, 0));

        root.getChildren().addAll(form, new Separator(), addBox, tabelaItens, totals);
        return root;
    }

    private HBox buildFooter() {
        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER_RIGHT);
        
        btnRascunho.getStyleClass().addAll(Styles.BUTTON_OUTLINED);
        btnRascunho.setOnAction(e -> handleSalvarRascunho());

        btnSalvar.getStyleClass().addAll(Styles.SUCCESS);
        btnSalvar.setText(tipoDocumento == TipoDocumento.PRO_FORMA ? "Emitir Pró-Forma" : "Emitir");
        btnSalvar.setOnAction(e -> handleEmitir());
        
        box.getChildren().addAll(btnRascunho, btnSalvar);
        return box;
    }

    private void showNovoClienteModal() {
        ClienteFormView form = new ClienteFormView(clienteService, null, (novo) -> {
            cbCliente.setItems(FXCollections.observableArrayList(clienteService.findAll()));
            cbCliente.setValue(novo);
        });
        form.setPrefSize(800, 600);
        modalService.create()
                .title("Novo Cliente (Rápido)")
                .content(form)
                .buildAndShow();
    }

    private void handleAdicionarItem() {
        Produto produto = cbProduto.getValue();
        if (produto == null) {
            modalService.create()
                    .title("Erro")
                    .content(new Label("Selecione um produto."))
                    .autoSize()
                    .withCancelButton("Fechar")
                    .buildAndShow();
            return;
        }

        if (produto.getPercentualIva() != null && produto.getPercentualIva().compareTo(BigDecimal.ZERO) == 0) {
            String codigo = txtCodigoIsencao.getText() != null ? txtCodigoIsencao.getText().trim() : "";
            if (codigo.isBlank()) {
                modalService.create()
                    .title("Conformidade AGT")
                    .content(new Label("O produto selecionado está isento de IVA.\nInforme o Código de Isenção (formato Mxx, ex: M00)."))
                    .autoSize()
                    .withCancelButton("Corrigir")
                    .buildAndShow();
                txtCodigoIsencao.requestFocus();
                return;
            }
            if (!codigo.matches("M\\d{2}")) {
                modalService.create()
                    .title("Conformidade AGT")
                    .content(new Label("Código de Isenção inválido ('" + codigo + "').\nUse o formato Mxx (ex: M00, M02)."))
                    .autoSize()
                    .withCancelButton("Corrigir")
                    .buildAndShow();
                txtCodigoIsencao.requestFocus();
                return;
            }
        }
        int qtd;
        try {
            qtd = Integer.parseInt(txtQuantidade.getText());
            if (qtd <= 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            modalService.create()
                    .title("Erro")
                    .content(new Label("Quantidade inválida."))
                    .autoSize()
                    .withCancelButton("Fechar")
                    .buildAndShow();
            return;
        }
        if (produto.getStock() != null && produto.getStock() < qtd) {
            modalService.create()
                    .title("Erro")
                    .content(new Label("Stock insuficiente. Disponível: " + produto.getStock()))
                    .autoSize()
                    .withCancelButton("Fechar")
                    .buildAndShow();
            return;
        }
        ItemFatura item = new ItemFatura();
        item.setDescricao(produto.getNome());
        item.setQuantidade(qtd);
        item.setPrecoUnitario(produto.getPrecoUnitario());
        item.setPercentualIva(produto.getPercentualIva());
        item.setProduto(produto);

        if (produto.getPercentualIva() != null && produto.getPercentualIva().compareTo(BigDecimal.ZERO) == 0) {
            String codigo = txtCodigoIsencao.getText() != null ? txtCodigoIsencao.getText().trim() : null;
            String motivo = txtMotivoIsencao.getText() != null ? txtMotivoIsencao.getText().trim() : null;
            item.setCodigoIsencao(codigo);
            item.setMotivoIsencao(motivo);
        }

        item.calculateTotals();
        faturaAtual.addItem(item);
        itensObservable.add(item);
        atualizarTotais();
        cbProduto.setValue(null);
        txtQuantidade.setText("1");
        txtCodigoIsencao.clear();
        txtMotivoIsencao.clear();
        cbProduto.requestFocus();
    }

    private void updateIsencaoFieldsState(Produto produto) {
        boolean isIsento = produto != null && produto.getPercentualIva() != null && produto.getPercentualIva().compareTo(BigDecimal.ZERO) == 0;
        txtCodigoIsencao.setDisable(!isIsento);
        txtMotivoIsencao.setDisable(!isIsento);
        if (!isIsento) {
            txtCodigoIsencao.clear();
            txtMotivoIsencao.clear();
        }
    }

    private void removerItem(ItemFatura item) {
        faturaAtual.removeItem(item);
        itensObservable.remove(item);
        atualizarTotais();
    }

    private void atualizarTotais() {
        faturaAtual.recalculateTotals();
        lblSubtotal.setText(CURRENCY_FORMAT.format(faturaAtual.getSubtotal()));
        lblTotalIva.setText(CURRENCY_FORMAT.format(faturaAtual.getIva()));
        lblTotalGeral.setText(CURRENCY_FORMAT.format(faturaAtual.getTotal()));
    }

    private boolean validarFatura() {
        if (cbCliente.getValue() == null) {
            modalService.create()
                    .title("Erro")
                    .content(new Label("Selecione o cliente."))
                    .autoSize()
                    .withCancelButton("Fechar")
                    .buildAndShow();
            return false;
        }
        
        if (dpEmissao.getValue() != null && dpEmissao.getValue().isAfter(LocalDate.now())) {
            modalService.create()
                    .title("Conformidade AGT")
                    .content(new Label("A data de emissão não pode ser futura (Norma AGT)."))
                    .autoSize()
                    .withCancelButton("Corrigir")
                    .buildAndShow();
            return false;
        }

        String nif = cbCliente.getValue().getNif();
        String nifMsg = ao.allon.kubata.faturacao.util.AngolaValidationUtils.validateNifMessage(nif);
        if (nifMsg != null) {
            modalService.create()
                    .title("Erro")
                    .content(new Label(nifMsg))
                    .autoSize()
                    .withCancelButton("Fechar")
                    .buildAndShow();
            return false;
        }
        if (dpEmissao.getValue() == null || dpVencimento.getValue() == null) {
            modalService.create()
                    .title("Erro")
                    .content(new Label("Datas são obrigatórias."))
                    .autoSize()
                    .withCancelButton("Fechar")
                    .buildAndShow();
            return false;
        }
        if (tabelaItens.getItems().stream().anyMatch(it -> it.getDescricao() == null || it.getDescricao().isBlank())) {
            modalService.create()
                    .title("Erro")
                    .content(new Label("Descrição dos itens é obrigatória."))
                    .autoSize()
                    .withCancelButton("Fechar")
                    .buildAndShow();
            return false;
        }
        return true;
    }

    private void prepararFatura() {
        faturaAtual.setCliente(cbCliente.getValue());
        faturaAtual.setDataEmissao(dpEmissao.getValue());
        faturaAtual.setDataVencimento(dpVencimento.getValue());
        faturaAtual.setModoFormacao(chkModoFormacao.isSelected());
    }

    private void handleSalvarRascunho() {
        if (!validarFatura()) return;
        try {
            prepararFatura();
            faturaService.salvar(faturaAtual);
            modalService.create()
                    .title("Sucesso")
                    .content(new Label("Rascunho salvo com sucesso!"))
                    .autoSize()
                    .withCancelButton("Fechar")
                    .buildAndShow();
            limparFormulario();
        } catch (Exception e) {
            modalService.create()
                    .title("Erro")
                    .content(new Label("Erro ao salvar: " + e.getMessage()))
                    .autoSize()
                    .withCancelButton("Fechar")
                    .buildAndShow();
        }
    }

    private void handleEmitir() {
        if (!validarFatura()) return;
        if (faturaAtual.getItens().isEmpty()) {
            modalService.create()
                    .title("Erro")
                    .content(new Label("Adicione pelo menos um item para emitir."))
                    .autoSize()
                    .withCancelButton("Fechar")
                    .buildAndShow();
            return;
        }

        // Validação Rigorosa de Motivos de Isenção (Norma AGT)
        for (ItemFatura item : faturaAtual.getItens()) {
            if (item.getPercentualIva().compareTo(BigDecimal.ZERO) == 0) {
                if (item.getCodigoIsencao() == null || item.getCodigoIsencao().isBlank()) {
                    modalService.create()
                            .title("Conformidade AGT")
                            .content(new Label("O item '" + item.getDescricao() + "' está isento de IVA.\nÉ obrigatório selecionar um motivo de isenção válido conforme a tabela da AGT."))
                            .autoSize()
                            .withCancelButton("Corrigir")
                            .buildAndShow();
                    return;
                }
            }
        }

        try {
            prepararFatura();
            faturaAtual = faturaService.salvarEEmitir(faturaAtual);
            modalService.create()
                    .title("Sucesso")
                    .content(new Label("Fatura emitida com sucesso!"))
                    .autoSize()
                    .withCancelButton("Fechar")
                    .buildAndShow();
            limparFormulario();
        } catch (Exception e) {
            modalService.create()
                    .title("Erro")
                    .content(new Label("Erro ao emitir: " + e.getMessage()))
                    .autoSize()
                    .withCancelButton("Fechar")
                    .buildAndShow();
        }
    }

    private void limparFormulario() {
        cbCliente.setValue(null);
        dpEmissao.setValue(LocalDate.now());
        dpVencimento.setValue(LocalDate.now().plusDays(30));
        cbProduto.setValue(null);
        txtQuantidade.setText("1");
        itensObservable.clear();
        faturaAtual = new Fatura();
        faturaAtual.setTipoDocumento(tipoDocumento);
        faturaAtual.setDataEmissao(LocalDate.now());
        faturaAtual.setDataVencimento(LocalDate.now().plusDays(30));
        atualizarTotais();
    }

    private void carregarFaturaExistente(Fatura existente) {
        faturaAtual = existente;
        tipoDocumento = existente.getTipoDocumento() != null ? existente.getTipoDocumento() : tipoDocumento;
        cbCliente.setItems(FXCollections.observableArrayList(clienteService.findAll()));
        cbCliente.setValue(existente.getCliente());
        dpEmissao.setValue(existente.getDataEmissao());
        dpVencimento.setValue(existente.getDataVencimento());
        itensObservable.setAll(existente.getItens());
        tabelaItens.setItems(itensObservable);
        atualizarTotais();
    }
}

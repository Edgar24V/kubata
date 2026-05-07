package ao.allon.kubata.faturacao.view;

import ao.allon.kubata.core.ui.table.AdvancedTableView;
import ao.allon.kubata.core.ui.table.TableUtils;
import ao.allon.kubata.faturacao.domain.Armazem;
import ao.allon.kubata.faturacao.domain.Estoque;
import ao.allon.kubata.faturacao.domain.MovimentoStock;
import ao.allon.kubata.faturacao.domain.Produto;
import ao.allon.kubata.faturacao.enums.TipoMovimento;
import ao.allon.kubata.faturacao.service.EstoqueService;
import ao.allon.kubata.faturacao.service.ProdutoService;
import ao.allon.kubata.faturacao.service.SessionManager;
import ao.allon.kubata.faturacao.ui.modal.ModalService;
import ao.allon.kubata.faturacao.ui.util.AlertUtils;
import ao.allon.kubata.faturacao.ui.util.IconUtils;
import atlantafx.base.controls.CustomTextField;
import atlantafx.base.theme.Styles;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.kordamp.ikonli.feather.Feather;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * View para gestão de estoque e movimentações de stock.
 */
public class EstoqueView extends VBox {

    private final EstoqueService estoqueService;
    private final ProdutoService produtoService;
    private final SessionManager sessionManager;
    private final ModalService modalService;

    private final AdvancedTableView<Estoque> table = new AdvancedTableView<>();
    private final ObservableList<Estoque> masterData = FXCollections.observableArrayList();
    private final DateTimeFormatter df = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private ComboBox<Armazem> cbArmazem;
    private ComboBox<Produto> cbProduto;
    private CustomTextField search;

    public EstoqueView(EstoqueService estoqueService,
                       ProdutoService produtoService,
                       SessionManager sessionManager,
                       ModalService modalService) {
        this.estoqueService = estoqueService;
        this.produtoService = produtoService;
        this.sessionManager = sessionManager;
        this.modalService = modalService;

        setSpacing(20);
        setPadding(new Insets(20));

        setupHeader();
        setupFilters();
        setupTable();
        loadData();
    }

    private void setupHeader() {
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 10, 0));

        VBox titleBox = new VBox(5);
        Label title = new Label("Gestão de Estoque");
        title.getStyleClass().add(Styles.TITLE_3);
        Label subtitle = new Label("Consulta e movimentações de stock por armazém");
        subtitle.getStyleClass().add(Styles.TEXT_MUTED);
        titleBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnEntrada = new Button("Entrada", IconUtils.icon(Feather.LOG_IN, IconUtils.SIZE_SMALL));
        btnEntrada.getStyleClass().addAll(Styles.BUTTON_OUTLINED, Styles.SUCCESS);
        btnEntrada.setOnAction(e -> showEntradaModal());

        Button btnSaida = new Button("Saída", IconUtils.icon(Feather.LOG_OUT, IconUtils.SIZE_SMALL));
        btnSaida.getStyleClass().addAll(Styles.BUTTON_OUTLINED, Styles.DANGER);
        btnSaida.setOnAction(e -> showSaidaModal());

        Button btnTransferir = new Button("Transferir", IconUtils.icon(Feather.TRUCK, IconUtils.SIZE_SMALL));
        btnTransferir.getStyleClass().add(Styles.BUTTON_OUTLINED);
        btnTransferir.setOnAction(e -> showTransferenciaModal());

        Button btnAjuste = new Button("Ajuste", IconUtils.icon(Feather.SLIDERS, IconUtils.SIZE_SMALL));
        btnAjuste.getStyleClass().add(Styles.BUTTON_OUTLINED);
        btnAjuste.setOnAction(e -> showAjusteModal());

        Button btnAtualizar = new Button("Atualizar", IconUtils.icon(Feather.REFRESH_CW, IconUtils.SIZE_SMALL));
        btnAtualizar.getStyleClass().add(Styles.BUTTON_OUTLINED);
        btnAtualizar.setOnAction(e -> loadData());

        boolean canEdit = sessionManager.hasAccess("ESTOQUE", "Editar");
        btnEntrada.setDisable(!canEdit);
        btnSaida.setDisable(!canEdit);
        btnTransferir.setDisable(!canEdit);
        btnAjuste.setDisable(!canEdit);

        header.getChildren().addAll(titleBox, spacer, btnAtualizar, btnAjuste, btnTransferir, btnSaida, btnEntrada);
        getChildren().add(header);
    }

    private void setupFilters() {
        HBox filters = new HBox(15);
        filters.setAlignment(Pos.CENTER_LEFT);
        filters.setPadding(new Insets(0, 0, 10, 0));

        cbArmazem = new ComboBox<>();
        cbArmazem.setPromptText("Selecionar Armazém");
        cbArmazem.setPrefWidth(200);
        cbArmazem.getItems().addAll(estoqueService.listarArmazens());
        cbArmazem.setOnAction(e -> applyFilters());

        cbProduto = new ComboBox<>();
        cbProduto.setPromptText("Selecionar Produto");
        cbProduto.setPrefWidth(250);
        cbProduto.getItems().addAll(produtoService.findAll());
        cbProduto.setOnAction(e -> applyFilters());

        search = new CustomTextField();
        search.setPromptText("Pesquisar produto...");
        search.setLeft(IconUtils.icon(Feather.SEARCH, IconUtils.SIZE_SMALL));
        search.setPrefWidth(250);
        search.textProperty().addListener((obs, ov, nv) -> applyFilters());

        filters.getChildren().addAll(
            new Label("Armazém:"), cbArmazem,
            new Label("Produto:"), cbProduto,
            search
        );

        getChildren().add(filters);
    }

    private void setupTable() {
        TableUtils.standardize(table);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        TableColumn<Estoque, String> colProduto = new TableColumn<>("Produto");
        colProduto.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                } else {
                    Estoque e = getTableRow().getItem();
                    setText(e.getProduto() != null ? e.getProduto().getNome() : "-");
                }
            }
        });
        colProduto.setMinWidth(250);

        TableColumn<Estoque, String> colCodigo = new TableColumn<>("Código");
        colCodigo.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                } else {
                    Estoque e = getTableRow().getItem();
                    setText(e.getProduto() != null ? e.getProduto().getCodigo() : "-");
                }
            }
        });
        colCodigo.setMinWidth(100);

        TableColumn<Estoque, String> colArmazem = new TableColumn<>("Armazém");
        colArmazem.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                } else {
                    Estoque e = getTableRow().getItem();
                    setText(e.getArmazem() != null ? e.getArmazem().getNome() : "-");
                }
            }
        });
        colArmazem.setMinWidth(150);

        TableColumn<Estoque, String> colLote = new TableColumn<>("Lote");
        colLote.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("lote"));
        colLote.setMinWidth(100);

        TableColumn<Estoque, Integer> colQuantidade = new TableColumn<>("Quantidade");
        colQuantidade.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("quantidade"));
        colQuantidade.setMinWidth(100);

        TableColumn<Estoque, String> colValidade = new TableColumn<>("Validade");
        colValidade.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                } else {
                    Estoque e = getTableRow().getItem();
                    if (e.getValidade() != null) {
                        setText(e.getValidade().format(df));
                        if (e.getValidade().isBefore(LocalDate.now())) {
                            setTextFill(javafx.scene.paint.Color.RED);
                            setStyle("-fx-font-weight: bold;");
                        } else if (e.getValidade().isBefore(LocalDate.now().plusDays(30))) {
                            setTextFill(javafx.scene.paint.Color.ORANGE);
                        } else {
                            setTextFill(javafx.scene.paint.Color.BLACK);
                            setStyle("");
                        }
                    } else {
                        setText("-");
                    }
                }
            }
        });
        colValidade.setMinWidth(100);

        table.getColumns().addAll(colProduto, colCodigo, colArmazem, colLote, colQuantidade, colValidade);

        VBox.setVgrow(table, Priority.ALWAYS);
        getChildren().add(table);
    }

    private void loadData() {
        masterData.setAll(estoqueService.listarTodosEstoque());
        table.setData(masterData);
    }

    private void applyFilters() {
        Armazem armazem = cbArmazem.getValue();
        Produto produto = cbProduto.getValue();
        String q = search.getText() != null ? search.getText().trim().toLowerCase() : "";

        table.setFilter(e -> {
            if (armazem != null && (e.getArmazem() == null || !e.getArmazem().getId().equals(armazem.getId()))) {
                return false;
            }
            if (produto != null && (e.getProduto() == null || !e.getProduto().getId().equals(produto.getId()))) {
                return false;
            }
            if (!q.isEmpty()) {
                String nome = e.getProduto() != null ? e.getProduto().getNome().toLowerCase() : "";
                String cod = e.getProduto() != null ? e.getProduto().getCodigo().toLowerCase() : "";
                if (!nome.contains(q) && !cod.contains(q)) {
                    return false;
                }
            }
            return true;
        });
    }

    private void showEntradaModal() {
        ComboBox<Produto> cbProd = new ComboBox<>();
        cbProd.setPromptText("Selecionar Produto");
        cbProd.getItems().addAll(produtoService.findAll());
        cbProd.setPrefWidth(350);

        ComboBox<Armazem> cbArm = new ComboBox<>();
        cbArm.setPromptText("Selecionar Armazém");
        cbArm.getItems().addAll(estoqueService.listarArmazens());
        cbArm.setPrefWidth(350);

        ComboBox<ao.allon.kubata.faturacao.enums.UnidadeMedida> cbUnidade = new ComboBox<>();
        cbUnidade.setPromptText("Unidade de Entrada");
        cbUnidade.setPrefWidth(150);

        TextField txtQtd = new TextField();
        txtQtd.setPromptText("Qtd");
        txtQtd.setPrefWidth(100);

        TextField txtCustoEntrada = new TextField();
        txtCustoEntrada.setPromptText("Custo Unitário (Compra)");
        
        TextField txtNovoPrecoVenda = new TextField();
        txtNovoPrecoVenda.setPromptText("Novo Preço Venda (Opcional)");

        Label lblSugestao = new Label("");
        lblSugestao.getStyleClass().add(Styles.TEXT_SMALL);
        lblSugestao.setStyle("-fx-text-fill: -color-accent-fg;");

        cbProd.valueProperty().addListener((obs, ov, nv) -> {
            if (nv != null) {
                cbUnidade.getItems().setAll(nv.getUnidadeMedida(), nv.getUnidadeCompra());
                cbUnidade.setValue(nv.getUnidadeCompra() != null ? nv.getUnidadeCompra() : nv.getUnidadeMedida());
                txtCustoEntrada.setText(nv.getPrecoCompra() != null ? nv.getPrecoCompra().toString() : "");
                txtNovoPrecoVenda.setText(nv.getPrecoUnitario() != null ? nv.getPrecoUnitario().toString() : "");
            }
        });

        // Calculadora de Margem Sugerida
        txtCustoEntrada.textProperty().addListener((obs, ov, nv) -> {
            try {
                if (nv != null && !nv.isEmpty() && cbProd.getValue() != null) {
                    BigDecimal custo = new BigDecimal(nv.replace(",", "."));
                    Produto p = cbProd.getValue();
                    
                    // Se for unidade de compra, converter para base para sugerir
                    BigDecimal custoBase = custo;
                    if (cbUnidade.getValue() == p.getUnidadeCompra() && p.getFatorConversao().compareTo(BigDecimal.ZERO) > 0) {
                        custoBase = custo.divide(p.getFatorConversao(), 2, RoundingMode.HALF_UP);
                    }

                    // Calcular margem atual
                    if (p.getPrecoUnitario() != null && p.getPrecoCompra() != null && p.getPrecoCompra().compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal margemAtual = p.getPrecoUnitario().subtract(p.getPrecoCompra())
                                                .divide(p.getPrecoCompra(), 4, RoundingMode.HALF_UP)
                                                .multiply(new BigDecimal("100"));
                        
                        BigDecimal sugestaoVenda = custoBase.multiply(BigDecimal.ONE.add(margemAtual.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP)));
                        lblSugestao.setText(String.format("Custo Base: %s | Sugestão Venda (Margem %.1f%%): %s", 
                                            ao.allon.kubata.faturacao.util.Money.formatAOA(custoBase),
                                            margemAtual,
                                            ao.allon.kubata.faturacao.util.Money.formatAOA(sugestaoVenda)));
                    }
                }
            } catch (Exception ignored) {}
        });

        TextField txtLote = new TextField();
        txtLote.setPromptText("Lote (opcional)");

        DatePicker dpValidade = new DatePicker();
        dpValidade.setPromptText("Validade (opcional)");

        TextArea txtObs = new TextArea();
        txtObs.setPromptText("Observações");
        txtObs.setPrefRowCount(2);

        VBox content = new VBox(10,
            new Label("Produto:*"), cbProd,
            new Label("Armazém:*"), cbArm,
            new HBox(10, new VBox(5, new Label("Quantidade:*"), txtQtd), new VBox(5, new Label("Unidade:*"), cbUnidade)),
            new VBox(5, new Label("Custo Unitário de Entrada (Compra):"), txtCustoEntrada, lblSugestao),
            new VBox(5, new Label("Novo Preço de Venda (Base):"), txtNovoPrecoVenda),
            new HBox(10, new VBox(5, new Label("Lote:"), txtLote), new VBox(5, new Label("Validade:"), dpValidade)),
            new Label("Observações:"), txtObs
        );
        content.setPadding(new Insets(10));

        modalService.create()
            .title("Entrada de Stock")
            .content(content)
            .dynamicSize()
            .withConfirmButton("Confirmar Entrada", () -> {
                try {
                    Produto p = cbProd.getValue();
                    Armazem a = cbArm.getValue();
                    BigDecimal qtd = new BigDecimal(txtQtd.getText().trim().replace(",", "."));
                    BigDecimal custo = new BigDecimal(txtCustoEntrada.getText().trim().replace(",", "."));
                    BigDecimal novoVenda = txtNovoPrecoVenda.getText().isBlank() ? null : new BigDecimal(txtNovoPrecoVenda.getText().trim().replace(",", "."));

                    if (p == null || a == null) {
                        AlertUtils.showWarningAlert("Campos Obrigatórios", "Selecione produto e armazém.");
                        return false;
                    }

                    estoqueService.adicionarStockComConversao(p, a, qtd, cbUnidade.getValue(), custo, novoVenda,
                        txtLote.getText().trim(), dpValidade.getValue(), txtObs.getText().trim(), null);
                    
                    loadData();
                    return true;
                } catch (Exception ex) {
                    AlertUtils.showExceptionAlert("Erro", "Não foi possível registrar a entrada.", ex);
                    return false;
                }
            })
            .withCancelButton("Cancelar")
            .buildAndShow();
    }

    private void showSaidaModal() {
        ComboBox<Produto> cbProd = new ComboBox<>();
        cbProd.setPromptText("Selecionar Produto");
        cbProd.getItems().addAll(produtoService.findAll());
        cbProd.setPrefWidth(300);

        ComboBox<Armazem> cbArm = new ComboBox<>();
        cbArm.setPromptText("Selecionar Armazém");
        cbArm.getItems().addAll(estoqueService.listarArmazens());
        cbArm.setPrefWidth(300);

        TextField txtQtd = new TextField();
        txtQtd.setPromptText("Quantidade");

        TextField txtLote = new TextField();
        txtLote.setPromptText("Lote (opcional)");

        TextArea txtObs = new TextArea();
        txtObs.setPromptText("Motivo da saída");
        txtObs.setPrefRowCount(2);

        VBox content = new VBox(10,
            new Label("Produto:*"), cbProd,
            new Label("Armazém:*"), cbArm,
            new Label("Quantidade:*"), txtQtd,
            new Label("Lote:"), txtLote,
            new Label("Observações:*"), txtObs
        );
        content.setPadding(new Insets(10));

        modalService.create()
            .title("Saída de Stock")
            .content(content)
            .autoSize()
            .withConfirmButton("Confirmar Saída", () -> {
                try {
                    Produto p = cbProd.getValue();
                    Armazem a = cbArm.getValue();
                    int qtd = Integer.parseInt(txtQtd.getText().trim());

                    if (p == null || a == null) {
                        AlertUtils.showWarningAlert("Campos Obrigatórios", "Selecione produto e armazém.");
                        return false;
                    }

                    estoqueService.removerStock(p, a, qtd, txtLote.getText().trim(), txtObs.getText().trim());
                    loadData();
                    return true;
                } catch (Exception ex) {
                    AlertUtils.showExceptionAlert("Erro", "Não foi possível registrar a saída.", ex);
                    return false;
                }
            })
            .withCancelButton("Cancelar")
            .buildAndShow();
    }

    private void showTransferenciaModal() {
        ComboBox<Produto> cbProd = new ComboBox<>();
        cbProd.setPromptText("Selecionar Produto");
        cbProd.getItems().addAll(produtoService.findAll());
        cbProd.setPrefWidth(300);

        ComboBox<Armazem> cbOrigem = new ComboBox<>();
        cbOrigem.setPromptText("Armazém Origem");
        cbOrigem.getItems().addAll(estoqueService.listarArmazens());
        cbOrigem.setPrefWidth(300);

        ComboBox<Armazem> cbDestino = new ComboBox<>();
        cbDestino.setPromptText("Armazém Destino");
        cbDestino.getItems().addAll(estoqueService.listarArmazens());
        cbDestino.setPrefWidth(300);

        TextField txtQtd = new TextField();
        txtQtd.setPromptText("Quantidade");

        TextField txtLote = new TextField();
        txtLote.setPromptText("Lote (opcional)");

        TextArea txtObs = new TextArea();
        txtObs.setPromptText("Motivo da transferência");
        txtObs.setPrefRowCount(2);

        VBox content = new VBox(10,
            new Label("Produto:*"), cbProd,
            new Label("Armazém Origem:*"), cbOrigem,
            new Label("Armazém Destino:*"), cbDestino,
            new Label("Quantidade:*"), txtQtd,
            new Label("Lote:"), txtLote,
            new Label("Observações:*"), txtObs
        );
        content.setPadding(new Insets(10));

        modalService.create()
            .title("Transferência de Stock")
            .content(content)
            .autoSize()
            .withConfirmButton("Confirmar Transferência", () -> {
                try {
                    Produto p = cbProd.getValue();
                    Armazem origem = cbOrigem.getValue();
                    Armazem destino = cbDestino.getValue();
                    int qtd = Integer.parseInt(txtQtd.getText().trim());

                    if (p == null || origem == null || destino == null) {
                        AlertUtils.showWarningAlert("Campos Obrigatórios", "Preencha todos os campos obrigatórios.");
                        return false;
                    }

                    if (origem.getId().equals(destino.getId())) {
                        AlertUtils.showWarningAlert("Erro", "Armazém de origem e destino devem ser diferentes.");
                        return false;
                    }

                    estoqueService.transferirStock(p, origem, destino, qtd, txtLote.getText().trim(), txtObs.getText().trim());
                    loadData();
                    return true;
                } catch (Exception ex) {
                    AlertUtils.showExceptionAlert("Erro", "Não foi possível realizar a transferência.", ex);
                    return false;
                }
            })
            .withCancelButton("Cancelar")
            .buildAndShow();
    }

    private void showAjusteModal() {
        ComboBox<Produto> cbProd = new ComboBox<>();
        cbProd.setPromptText("Selecionar Produto");
        cbProd.getItems().addAll(produtoService.findAll());
        cbProd.setPrefWidth(300);

        ComboBox<Armazem> cbArm = new ComboBox<>();
        cbArm.setPromptText("Selecionar Armazém");
        cbArm.getItems().addAll(estoqueService.listarArmazens());
        cbArm.setPrefWidth(300);

        TextField txtQtd = new TextField();
        txtQtd.setPromptText("Quantidade Real (contada)");

        TextField txtLote = new TextField();
        txtLote.setPromptText("Lote (opcional)");

        TextArea txtObs = new TextArea();
        txtObs.setPromptText("Motivo do ajuste");
        txtObs.setPrefRowCount(2);

        VBox content = new VBox(10,
            new Label("Produto:*"), cbProd,
            new Label("Armazém:*"), cbArm,
            new Label("Quantidade Real:*"), txtQtd,
            new Label("Lote:"), txtLote,
            new Label("Motivo:*"), txtObs
        );
        content.setPadding(new Insets(10));

        modalService.create()
            .title("Ajuste de Stock (Inventário)")
            .content(content)
            .autoSize()
            .withConfirmButton("Confirmar Ajuste", () -> {
                try {
                    Produto p = cbProd.getValue();
                    Armazem a = cbArm.getValue();
                    int qtd = Integer.parseInt(txtQtd.getText().trim());

                    if (p == null || a == null) {
                        AlertUtils.showWarningAlert("Campos Obrigatórios", "Selecione produto e armazém.");
                        return false;
                    }

                    estoqueService.ajustarStock(p, a, qtd, txtLote.getText().trim(), txtObs.getText().trim());
                    loadData();
                    return true;
                } catch (Exception ex) {
                    AlertUtils.showExceptionAlert("Erro", "Não foi possível realizar o ajuste.", ex);
                    return false;
                }
            })
            .withCancelButton("Cancelar")
            .buildAndShow();
    }
}

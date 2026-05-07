package ao.allon.kubata.faturacao.pdv.view;

import ao.allon.kubata.faturacao.domain.Fatura;
import ao.allon.kubata.faturacao.domain.ItemFatura;
import ao.allon.kubata.faturacao.domain.Produto;
import ao.allon.kubata.faturacao.domain.enums.StatusFatura;
import ao.allon.kubata.faturacao.pdv.controller.PdvController;
import ao.allon.kubata.faturacao.ui.components.VirtualKeyboardPopup;
import ao.allon.kubata.faturacao.ui.modal.ModalService;
import ao.allon.kubata.faturacao.ui.util.AlertUtils;
import ao.allon.kubata.faturacao.ui.util.IconUtils;
import ao.allon.kubata.faturacao.integration.scale.ScaleSerialService;
import ao.allon.kubata.faturacao.integration.scale.WeightParser;
import ao.allon.kubata.faturacao.util.Money;
import atlantafx.base.controls.Breadcrumbs;
import atlantafx.base.controls.Card;
import atlantafx.base.theme.Styles;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.Node;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;
import javafx.util.converter.IntegerStringConverter;
import org.kordamp.ikonli.feather.Feather;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;
import java.math.RoundingMode;
import java.math.BigDecimal;

public class PdvView extends BorderPane {

    private final PdvController controller;
    private final NumberFormat currencyFormat = Money.getCurrencyFormat();
    private SplitPane splitPaneRef;
    private Produto lastSelectedProduct;

    // Left Panel (Cart) components
    private final ListView<ItemFatura> listCarrinho = new ListView<>();
    private final Label lblTotalValue = new Label(Money.format(BigDecimal.ZERO));
    private final ComboBox<ao.allon.kubata.faturacao.domain.Cliente> cbCliente = new ComboBox<>();
    private final TextField txtSearch = new TextField();
    private final TextField txtDescTotal = new TextField("0");
    private final Label lblSearchCount = new Label();
    private final Label lblSearchError = new Label();
    
    // Right Panel (Products) components
    private final TilePane productsGrid = new TilePane();
    private final ScrollPane productsScroll = new ScrollPane(productsGrid);
    private final ComboBox<String> cbCategoria = new ComboBox<>();
    private final Label lblResultsFooter = new Label();
    
    // Metrics
    private final Label lblVendasDia = new Label(Money.format(BigDecimal.ZERO));
    private final Label lblTicketMedio = new Label(Money.format(BigDecimal.ZERO));
    private final ComboBox<String> cbOrdenar = new ComboBox<>();
    private final CheckBox chkEmStock = new CheckBox("Apenas em stock");
    
    // Status Bar
    private final Label lblStatusCaixa = new Label();
    private final HBox statusPanel = new HBox(15);
    private final Label lblPrinter = new Label();

    public PdvView(PdvController controller) {
        this.controller = controller;
        getStyleClass().add("pdv-view");
        setPadding(new Insets(0));

        controller.init();
        controller.setOnStatusChanged(this::updateStatusPanel);
        controller.setOnMetricsChanged(this::refreshMetrics);

        splitPaneRef = new SplitPane();
        splitPaneRef.setDividerPositions(0.35); // 35% Cart, 65% Products
        
        VBox cartPanel = buildCartPanel();
        VBox productsPanel = buildProductsPanel();
        
        splitPaneRef.getItems().addAll(cartPanel, productsPanel);
        setCenter(splitPaneRef);
        
        buildStatusPanel();
        setTop(new VBox(buildMenuBar(), statusPanel));

        bindData();
        installAccelerators();
        installResponsiveness();
        refreshMetrics();
        updateStatusPanel();

        VirtualKeyboardPopup.attach(txtSearch, VirtualKeyboardPopup.KeyboardType.ALPHANUMERIC);
        VirtualKeyboardPopup.attach(txtDescTotal, VirtualKeyboardPopup.KeyboardType.NUMERIC);
        
        // Setup focus
        javafx.application.Platform.runLater(txtSearch::requestFocus);
    }
    
    private void buildStatusPanel() {
        statusPanel.setPadding(new Insets(5, 10, 5, 10));
        statusPanel.setAlignment(Pos.CENTER_LEFT);
        statusPanel.setStyle("-fx-background-color: -color-bg-subtle; -fx-border-color: -color-border-default; -fx-border-width: 0 0 1 0;");
        
        lblStatusCaixa.setStyle("-fx-font-weight: bold; -fx-font-size: 1.1em;");
        lblPrinter.getStyleClass().addAll(Styles.TEXT_MUTED);
        updatePrinterLabel();
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        HBox legends = new HBox(15);
        legends.setAlignment(Pos.CENTER_RIGHT);
        legends.getChildren().addAll(
            createLegend("F2", "Abrir/Fechar"),
            createLegend("F3", "Buscar"),
            createLegend("F4", "Finalizar"),
            createLegend("QTD*PROD", "Multiplicador"),
            createLegend("Ctrl+H", "Histórico")
        );
        
        Button btnHelp = new Button("Ajuda", IconUtils.icon(Feather.HELP_CIRCLE, IconUtils.SIZE_SMALL));
        btnHelp.getStyleClass().addAll(Styles.SMALL, Styles.BUTTON_OUTLINED);
        btnHelp.setOnAction(e -> showHelpDialog());
        
        statusPanel.getChildren().addAll(lblStatusCaixa, spacer, legends, new Separator(), new Label("Impressora:"), lblPrinter, btnHelp);
    }
    
    private Label createLegend(String key, String desc) {
        Label l = new Label(key + ": " + desc);
        l.getStyleClass().addAll(Styles.TEXT_SMALL, Styles.TEXT_MUTED);
        return l;
    }
    
    private void updateStatusPanel() {
        String status = controller.getStatusCaixaTexto();
        boolean isOpen = controller.isCaixaAberto();
        
        lblStatusCaixa.setText(status);
        if (isOpen) {
            lblStatusCaixa.setStyle("-fx-text-fill: -color-success-fg; -fx-font-weight: bold; -fx-font-size: 1.1em;");
        } else {
            lblStatusCaixa.setStyle("-fx-text-fill: -color-danger-fg; -fx-font-weight: bold; -fx-font-size: 1.1em;");
        }
    }
    
    public void refreshStatusCaixa() {
        updateStatusPanel();
    }
    
    private void showHelpDialog() {
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        
        content.getChildren().addAll(
            new Label("Atalhos do Sistema:"),
            new Label("• F2: Abre ou Fecha o Caixa (Menu Caixa)"),
            new Label("• F3: Foca na barra de pesquisa de produtos"),
            new Label("• F4: Finaliza a venda atual (Tela de Pagamento)"),
            new Label("• F5: Recarrega a lista de produtos"),
            new Label("• F6: Abre o Dashboard de Fluxo de Caixa"),
            new Label("• Ctrl+H: Exibe o histórico de vendas do dia"),
            new Label("• Enter (na busca): Adiciona o primeiro produto encontrado ao carrinho"),
            new Label("• Delete (no carrinho): Remove o item selecionado"),
            new Label(""),
            new Label("Fluxo de Caixa:"),
            new Label("1. Abra o caixa no início do turno (F2)."),
            new Label("2. Realize as vendas."),
            new Label("3. Use Sangria/Suprimento para ajustes de valor."),
            new Label("4. Feche o caixa ao final do turno (F2).")
        );
        
        controller.getModalService().create()
            .title("Ajuda e Atalhos")
            .content(content)
            .autoSize()
            .withCloseButton()
            .buildAndShow();
    }

    private void handleF2Action() {
        if (controller.isCaixaAberto()) {
            controller.fecharCaixa();
        } else {
            controller.abrirCaixa();
        }
    }

    private MenuBar buildMenuBar() {
        MenuBar menuBar = new MenuBar();
        
        Menu menuVendas = new Menu("Vendas");
        
        // Importar Documento
        MenuItem itemImportar = new MenuItem("Importar Documento...");
        itemImportar.setAccelerator(new KeyCodeCombination(KeyCode.I, KeyCombination.CONTROL_DOWN));
        itemImportar.setOnAction(e -> showImportarDocumentoDialog());
        
        MenuItem itemHistorico = new MenuItem("Histórico do Dia");
        itemHistorico.setAccelerator(new KeyCodeCombination(KeyCode.H, KeyCombination.CONTROL_DOWN));
        itemHistorico.setOnAction(e -> showHistoricoVendas());
        
        menuVendas.getItems().addAll(itemImportar, itemHistorico, new SeparatorMenuItem());
        MenuItem itemReprint = new MenuItem("Reimprimir Última");
        itemReprint.setAccelerator(new KeyCodeCombination(KeyCode.F4, KeyCombination.SHIFT_DOWN));
        itemReprint.setOnAction(e -> {
            if (!controller.isReprintConfirmEnabled()) {
                controller.reimprimirUltima();
                return;
            }
            controller.getModalService().create()
                    .title("Reimprimir Última Fatura")
                    .content(new Label("Confirmar reimpressão da última fatura emitida?"))
                    .autoSize()
                    .withConfirmButton("Reimprimir", () -> {
                        controller.reimprimirUltima();
                        return true;
                    })
                    .withCancelButton()
                    .buildAndShow();
        });
        CheckMenuItem confirmReprintItem = new CheckMenuItem("Confirmar reimpressão");
        confirmReprintItem.setSelected(controller.isReprintConfirmEnabled());
        confirmReprintItem.selectedProperty().addListener((o, ov, nv) -> controller.setReprintConfirmEnabled(nv));
        
        MenuItem itemRelatorioClientes = new MenuItem("Relatório por Tipo de Cliente");
        itemRelatorioClientes.setOnAction(e -> controller.exibirRelatorioVendasPorCliente());
        
        menuVendas.getItems().addAll(itemReprint, new SeparatorMenuItem(), confirmReprintItem, itemRelatorioClientes);
        
        CheckMenuItem offlineItem = new CheckMenuItem("Modo Offline");
        offlineItem.setSelected(controller.isOfflineMode());
        offlineItem.selectedProperty().addListener((o, ov, nv) -> controller.setOfflineMode(nv));
        
        CheckMenuItem vkItem = new CheckMenuItem("Teclado Virtual");
        vkItem.setSelected(controller.isVirtualKeyboardEnabled());
        vkItem.selectedProperty().addListener((o, ov, nv) -> controller.setVirtualKeyboardEnabled(nv));
        
        MenuItem itemAtalhos = new MenuItem("Configurar Atalhos...");
        itemAtalhos.setOnAction(e -> showShortcutsDialog());
        menuVendas.getItems().addAll(new SeparatorMenuItem(), offlineItem, vkItem, itemAtalhos);

        Menu menuCaixa = new Menu("Caixa");
        
        MenuItem itemAbrirFechar = new MenuItem("Abrir/Fechar Caixa");
        itemAbrirFechar.setAccelerator(new KeyCodeCombination(KeyCode.F2));
        itemAbrirFechar.setOnAction(e -> handleF2Action());
        
        MenuItem itemSangria = new MenuItem("Sangria");
        itemSangria.setOnAction(e -> controller.realizarSangria());
        
        MenuItem itemSuprimento = new MenuItem("Suprimento");
        itemSuprimento.setOnAction(e -> controller.realizarSuprimento());
        
        MenuItem itemRelatorio = new MenuItem("Fluxo de Caixa (Dashboard)");
        itemRelatorio.setAccelerator(new KeyCodeCombination(KeyCode.F6));
        itemRelatorio.setOnAction(e -> controller.exibirFluxoCaixa());
        
        MenuItem itemRelX = new MenuItem("Relatório X (Parcial)");
        itemRelX.setOnAction(e -> controller.gerarRelatorioX());
        MenuItem itemRelZ = new MenuItem("Relatório Z (Fecho)");
        itemRelZ.setOnAction(e -> controller.gerarRelatorioZ());
        
        CheckMenuItem autoPrintItem = new CheckMenuItem("Imprimir automaticamente");
        autoPrintItem.setSelected(controller.isAutoPrintEnabled());
        autoPrintItem.selectedProperty().addListener((o, ov, nv) -> controller.setAutoPrintEnabled(nv));

        CheckMenuItem useViewerItem = new CheckMenuItem("Usar visualizador de relatório");
        useViewerItem.setSelected(controller.isUseReportViewerEnabled());
        useViewerItem.selectedProperty().addListener((o, ov, nv) -> controller.setUseReportViewerEnabled(nv));

        MenuItem selectPrinterItem = new MenuItem("Selecionar Impressora...");
        selectPrinterItem.setOnAction(e -> showPrinterSelectionDialog());

        menuCaixa.getItems().addAll(
                itemAbrirFechar,
                new SeparatorMenuItem(),
                itemSangria,
                itemSuprimento,
                new SeparatorMenuItem(),
                itemRelatorio,
                itemRelX,
                itemRelZ,
                new SeparatorMenuItem(),
                autoPrintItem,
                useViewerItem,
                selectPrinterItem
        );
        
        menuBar.getMenus().addAll(menuVendas, menuCaixa);
        
        return menuBar;
    }
    
    private void showShortcutsDialog() {
        java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userRoot().node("ao/allon/kubata/pdv");
        TextField tfRemove = new TextField(prefs.get("shortcutRemove", "DELETE"));
        TextField tfQty = new TextField(prefs.get("shortcutQty", "Q"));
        TextField tfDisc = new TextField(prefs.get("shortcutDiscount", "D"));
        GridPane g = new GridPane();
        g.setHgap(10); g.setVgap(10);
        g.addRow(0, new Label("Remover Item:"), tfRemove);
        g.addRow(1, new Label("Editar Quantidade:"), tfQty);
        g.addRow(2, new Label("Aplicar Desconto:"), tfDisc);
        controller.getModalService().create()
            .title("Configurar Atalhos")
            .content(new VBox(10, g))
            .autoSize()
            .withConfirmButton("Salvar", () -> {
                prefs.put("shortcutRemove", tfRemove.getText().trim().toUpperCase());
                prefs.put("shortcutQty", tfQty.getText().trim().toUpperCase());
                prefs.put("shortcutDiscount", tfDisc.getText().trim().toUpperCase());
                return true;
            })
            .withCancelButton()
            .buildAndShow();
    }

    private void showPrinterSelectionDialog() {
        java.util.List<String> printers = controller.getAvailablePrinters();
        javafx.scene.control.ComboBox<String> cb = new javafx.scene.control.ComboBox<>();
        cb.getItems().addAll(printers);
        cb.setEditable(false);
        String current = controller.getSelectedPrinterName();
        if (current != null) cb.setValue(current);
        if (cb.getValue() == null) {
            String def = controller.getDefaultPrinterName();
            if (def != null) cb.setValue(def);
        }

        VBox content = new VBox(10, new Label("Selecione a impressora térmica"), cb);
        content.setPadding(new Insets(15));

        controller.getModalService().create()
                .title("Configurar Impressora")
                .content(content)
                .autoSize()
                .withConfirmButton("Salvar", () -> {
                    controller.setSelectedPrinterName(cb.getValue());
                    updatePrinterLabel();
                    return true;
                })
                .withCancelButton()
                .buildAndShow();
    }

    private void updatePrinterLabel() {
        String name = controller.getSelectedPrinterName();
        if (name == null || name.isBlank()) name = controller.getDefaultPrinterName();
        lblPrinter.setText(name != null ? name : "(padrão não definido)");
    }

    private void showHistoricoVendas() {
        TableView<Fatura> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        
        TableColumn<Fatura, String> colNum = new TableColumn<>("Número");
        colNum.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getNumero()));
        
        TableColumn<Fatura, String> colHora = new TableColumn<>("Hora");
        colHora.setCellValueFactory(cell -> {
            if (cell.getValue().getCreatedAt() != null)
                return new SimpleStringProperty(cell.getValue().getCreatedAt().toLocalTime().toString().substring(0, 8));
            return new SimpleStringProperty("-");
        });
        
        TableColumn<Fatura, String> colCliente = new TableColumn<>("Cliente");
        colCliente.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getCliente().getNome()));
        
        TableColumn<Fatura, String> colValor = new TableColumn<>("Valor");
        colValor.setCellValueFactory(cell -> new SimpleStringProperty(currencyFormat.format(cell.getValue().getTotal())));
        colValor.setStyle("-fx-alignment: CENTER-RIGHT;");
        
        TableColumn<Fatura, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getStatus().toString()));
        
        TableColumn<Fatura, Void> colAction = new TableColumn<>("Ações");
        colAction.setCellFactory(param -> new TableCell<>() {
            private final Button btnCancel = new Button("Cancelar");
            private final Button btnReprint = new Button("Reimprimir");
            private final HBox box = new HBox(5, btnReprint, btnCancel);
            {
                btnCancel.getStyleClass().addAll(Styles.SMALL, Styles.DANGER);
                btnReprint.getStyleClass().addAll(Styles.SMALL, Styles.ACCENT);
                btnReprint.setOnAction(e -> {
                    Fatura f = getTableView().getItems().get(getIndex());
                    if (!controller.isReprintConfirmEnabled()) {
                        controller.reimprimir(f);
                        return;
                    }
                    controller.getModalService().create()
                            .title("Reimprimir Fatura")
                            .content(new Label("Confirmar reimpressão da fatura " + f.getNumero() + "?"))
                            .autoSize()
                            .withConfirmButton("Reimprimir", () -> {
                                controller.reimprimir(f);
                                return true;
                            })
                            .withCancelButton()
                            .buildAndShow();
                });
                btnCancel.setOnAction(e -> {
                    Fatura f = getTableView().getItems().get(getIndex());
                    controller.getModalService().create()
                        .title("Cancelar Venda")
                        .content(new Label("Tem certeza que deseja cancelar a venda " + f.getNumero() + "?\nEsta ação reverterá o estoque e o caixa."))
                        .autoSize()
                        .withConfirmButton("Cancelar Venda", () -> {
                            controller.cancelarVenda(f, "Cancelamento solicitado pelo usuário");
                            table.setItems(javafx.collections.FXCollections.observableArrayList(controller.getHistoricoVendasDia()));
                            return true;
                        })
                        .withCancelButton()
                        .buildAndShow();
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Fatura f = getTableView().getItems().get(getIndex());
                    if (f.getStatus() == StatusFatura.EMITIDA) {
                        setGraphic(box);
                    } else {
                        setGraphic(null);
                    }
                }
            }
        });
        
        table.getColumns().addAll(colNum, colHora, colCliente, colValor, colStatus, colAction);
        table.setItems(javafx.collections.FXCollections.observableArrayList(controller.getHistoricoVendasDia()));
        table.setPrefWidth(680);
        table.setMaxWidth(Double.MAX_VALUE);

        VBox content = new VBox(10, table);
        content.setPadding(new Insets(10));
        VBox.setVgrow(table, Priority.ALWAYS);
        
        controller.getModalService().create()
            .title("Histórico de Vendas")
            .content(content)
            .autoSize()
            .withCloseButton()
            .buildAndShow();
    }

    private VBox buildCartPanel() {
        VBox root = new VBox(10);
        root.setPadding(new Insets(10));
        root.setStyle("-fx-background-color: -color-bg-default;");

        // Header: Customer & Actions
        HBox header = new HBox(10);
        cbCliente.setMaxWidth(Double.MAX_VALUE);
        cbCliente.setPromptText("Selecione o Cliente");
        HBox.setHgrow(cbCliente, Priority.ALWAYS);
        
        Button btnAddClient = new Button("", IconUtils.icon(Feather.USER_PLUS, IconUtils.SIZE_SMALL));
        btnAddClient.getStyleClass().addAll(Styles.BUTTON_OUTLINED);
        btnAddClient.setTooltip(new Tooltip("Novo Cliente"));
        btnAddClient.setOnAction(e -> showNovoClienteDialog());

        header.getChildren().addAll(cbCliente, btnAddClient);

        // Cart List
        listCarrinho.setPlaceholder(new Label("Carrinho vazio"));
        VBox.setVgrow(listCarrinho, Priority.ALWAYS);

        // Totals Section
        VBox totalsBox = new VBox(5);
        totalsBox.setPadding(new Insets(10));
        totalsBox.setStyle("-fx-background-color: -color-bg-subtle; -fx-background-radius: 4;");
        
        HBox subRow = new HBox(10, new Label("Desconto Global (%):"), txtDescTotal);
        subRow.setAlignment(Pos.CENTER_RIGHT);
        txtDescTotal.setPrefWidth(70);
        txtDescTotal.textProperty().addListener((obs, ov, nv) -> {
            try {
                BigDecimal p = new BigDecimal(nv.replace(",", "."));
                BigDecimal maxG = computeMaxGlobalDiscount();
                if (p.compareTo(maxG) <= 0 && p.compareTo(BigDecimal.ZERO) >= 0) {
                    controller.setDescontoTotalPercent(p);
                    txtDescTotal.getStyleClass().remove(Styles.DANGER);
                } else {
                    txtDescTotal.getStyleClass().add(Styles.DANGER);
                    AlertUtils.showWarningAlert("Desconto", "Máximo global permitido: " + maxG + "%");
                    txtDescTotal.setText(ov);
                }
            } catch (Exception ex) {
                txtDescTotal.getStyleClass().add(Styles.DANGER);
            }
        });

        lblTotalValue.setStyle("-fx-font-size: 2em; -fx-font-weight: bold; -fx-text-fill: -color-accent-fg;");
        HBox totalRow = new HBox(lblTotalValue);
        totalRow.setAlignment(Pos.CENTER_RIGHT);
        
        HBox qtyActions = new HBox(8);
        qtyActions.setAlignment(Pos.CENTER_RIGHT);
        Button btnMinus = new Button("", IconUtils.icon(Feather.MINUS, IconUtils.SIZE_SMALL));
        Button btnPlus = new Button("", IconUtils.icon(Feather.PLUS, IconUtils.SIZE_SMALL));
        btnMinus.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.BUTTON_OUTLINED);
        btnPlus.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.BUTTON_OUTLINED);
        btnMinus.setOnAction(e -> {
            ItemFatura sel = listCarrinho.getSelectionModel().getSelectedItem();
            if (sel != null && sel.getQuantidade() != null && sel.getQuantidade() > 1) {
                controller.atualizarQuantidade(sel, sel.getQuantidade() - 1);
            }
        });
        btnPlus.setOnAction(e -> {
            ItemFatura sel = listCarrinho.getSelectionModel().getSelectedItem();
            if (sel != null && sel.getQuantidade() != null) {
                controller.atualizarQuantidade(sel, sel.getQuantidade() + 1);
            }
        });
        qtyActions.getChildren().addAll(new Label("Qtd:"), btnMinus, btnPlus);

        totalsBox.getChildren().addAll(subRow, qtyActions, new Separator(), totalRow);

        // Action Buttons
        Button btnPay = new Button("FINALIZAR (F4)", IconUtils.icon(Feather.CHECK_CIRCLE, IconUtils.SIZE_MEDIUM));
        btnPay.setMaxWidth(Double.MAX_VALUE);
        btnPay.setPrefHeight(50);
        btnPay.getStyleClass().addAll(Styles.SUCCESS, Styles.LARGE);
        btnPay.setOnAction(e -> controller.finalizarVenda());

        Button btnCancel = new Button("Cancelar", IconUtils.icon(Feather.X_CIRCLE, IconUtils.SIZE_MEDIUM));
        btnCancel.setMaxWidth(Double.MAX_VALUE);
        btnCancel.getStyleClass().addAll(Styles.DANGER, Styles.BUTTON_OUTLINED);
        btnCancel.setOnAction(e -> {
             controller.getModalService().create()
                 .title("Cancelar")
                 .content(new Label("Limpar todo o carrinho?"))
                 .autoSize()
                 .withConfirmButton("Sim", () -> {
                     controller.getCarrinho().clear();
                     return true;
                 })
                 .withCancelButton()
                 .buildAndShow();
        });

        Button btnScale = new Button("Balança", IconUtils.icon(Feather.SQUARE, IconUtils.SIZE_MEDIUM));
        btnScale.setMaxWidth(Double.MAX_VALUE);
        btnScale.getStyleClass().addAll(Styles.ACCENT, Styles.BUTTON_OUTLINED);
        btnScale.setOnAction(e -> {
            if (!controller.canUseScale()) {
                AlertUtils.showWarningAlert("Permissão", "Sem permissão para usar a balança.");
                return;
            }
            showScaleDialog(null);
        });
        btnScale.setDisable(!controller.canUseScale());

        HBox actions = new HBox(10, btnCancel, btnScale, btnPay);
        HBox.setHgrow(btnPay, Priority.ALWAYS);
        HBox.setHgrow(btnCancel, Priority.ALWAYS);
        HBox.setHgrow(btnScale, Priority.ALWAYS);

        root.getChildren().addAll(header, listCarrinho, totalsBox, actions);
        setupCartListView();
        return root;
    }

    private void setupCartListView() {
        listCarrinho.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(ItemFatura item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    // Create the custom card layout for each item
                    HBox card = new HBox(10);
                    card.getStyleClass().add("cart-item-card");
                    card.setPadding(new Insets(5));
                    card.setAlignment(Pos.CENTER_LEFT);

                    // Product Code and Description
                    VBox productInfo = new VBox(2);
                    Label code = new Label(item.getProduto().getCodigo());
                    code.getStyleClass().add("cart-item-code");
                    Label description = new Label(item.getProduto().getNome());
                    description.getStyleClass().add("cart-item-description");
                    productInfo.getChildren().addAll(code, description);

                    // Quantity controls
                    HBox qtyControls = new HBox(5);
                    qtyControls.setAlignment(Pos.CENTER);
                    Button btnMinus = new Button("", IconUtils.icon(Feather.MINUS, IconUtils.SIZE_SMALL));
                    btnMinus.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.BUTTON_OUTLINED, Styles.FLAT);
                    btnMinus.setOnAction(e -> {
                        if (isFracionavel(item)) {
                            updateQuantidadeDecimal(item, getStepFracionavel(item).negate());
                        } else {
                            if (item.getQuantidade() > 1) {
                                controller.atualizarQuantidade(item, item.getQuantidade() - 1);
                            }
                        }
                    });
                    Label quantity = new Label(formatQuantidade(item));
                    quantity.getStyleClass().add("cart-item-quantity");
                    Button btnPlus = new Button("", IconUtils.icon(Feather.PLUS, IconUtils.SIZE_SMALL));
                    btnPlus.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.BUTTON_OUTLINED, Styles.FLAT);
                    btnPlus.setOnAction(e -> {
                        if (isFracionavel(item)) {
                            updateQuantidadeDecimal(item, getStepFracionavel(item));
                        } else {
                            controller.atualizarQuantidade(item, item.getQuantidade() + 1);
                        }
                    });
                    qtyControls.getChildren().addAll(btnMinus, quantity, btnPlus);

                    // Unit Price
                    String unitPriceText = currencyFormat.format(item.getPrecoUnitario());
                    if (isFracionavel(item)) {
                        unitPriceText = unitPriceText + " / " + unidadeLabel(item);
                    }
                    Label unitPrice = new Label(unitPriceText);
                    unitPrice.getStyleClass().add("cart-item-unit-price");

                    // Discount
                    Label discount = new Label();
                    if (item.getDescontoPercentual() != null && item.getDescontoPercentual().compareTo(BigDecimal.ZERO) > 0) {
                        discount.setText("-" + item.getDescontoPercentual().stripTrailingZeros().toPlainString() + "%");
                        discount.getStyleClass().add("cart-item-discount");
                    }

                    // Subtotal
                    Label subtotal = new Label(currencyFormat.format(item.getTotal()));
                    subtotal.getStyleClass().add("cart-item-subtotal");

                    // Remove button
                    Button btnRemove = new Button("", IconUtils.icon(Feather.TRASH_2, IconUtils.SIZE_SMALL));
                    btnRemove.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.DANGER, Styles.FLAT);
                    btnRemove.setOnAction(e -> controller.removerItemDoCarrinho(item));

                    // Layout arrangement
                    HBox.setHgrow(productInfo, Priority.ALWAYS);
                    card.getChildren().addAll(productInfo, qtyControls, unitPrice, discount, subtotal, btnRemove);
                    setGraphic(card);

                    // Animation
                    if (item.isNew()) { // Assuming a 'isNew' flag in ItemFatura for animation
                        FadeTransition ft = new FadeTransition(Duration.millis(300), card);
                        ft.setFromValue(0.0);
                        ft.setToValue(1.0);
                        ft.play();
                        item.setNew(false); // Reset flag after animation
                    }
                }
            }
        });
        listCarrinho.setOnKeyPressed(e -> {
            String rem = java.util.prefs.Preferences.userRoot().node("ao/allon/kubata/pdv").get("shortcutRemove", "DELETE");
            String qty = java.util.prefs.Preferences.userRoot().node("ao/allon/kubata/pdv").get("shortcutQty", "Q");
            String disc = java.util.prefs.Preferences.userRoot().node("ao/allon/kubata/pdv").get("shortcutDiscount", "D");
            ItemFatura sel = listCarrinho.getSelectionModel().getSelectedItem();
            if (sel == null) return;
            if (e.getCode().getName().equalsIgnoreCase(rem) || e.getCode() == KeyCode.DELETE) {
                controller.removerItemDoCarrinho(sel);
                e.consume();
            } else if (e.getCode().getName().equalsIgnoreCase(qty)) {
                showQuantityDialog(sel);
                e.consume();
            } else if (e.getCode().getName().equalsIgnoreCase(disc)) {
                showDiscountDialog(sel);
                e.consume();
            }
        });
        MenuItem miScale = new MenuItem("Pesar com Balança...");
        miScale.setOnAction(e -> {
            if (!controller.canUseScale()) {
                AlertUtils.showWarningAlert("Permissão", "Sem permissão para usar a balança.");
                return;
            }
            ItemFatura sel = listCarrinho.getSelectionModel().getSelectedItem();
            Produto p = sel != null ? sel.getProduto() : null;
            showScaleDialog(p);
        });
        MenuItem miPrice = new MenuItem("Alterar Preço...");
        miPrice.setOnAction(e -> {
            if (!controller.canEditPdvPrice()) {
                AlertUtils.showWarningAlert("Permissão", "Sem permissão para alterar preço.");
                return;
            }
            ItemFatura sel = listCarrinho.getSelectionModel().getSelectedItem();
            if (sel == null) return;
            showPriceDialog(sel);
        });
        listCarrinho.setContextMenu(new ContextMenu(miScale, miPrice));
    }


    
    private void showDiscountDialog(ItemFatura item) {
        Dialog<java.math.BigDecimal> dlg = new Dialog<>();
        dlg.setTitle("Desconto no Item");
        TextField tf = new TextField("0");
        tf.setPrefWidth(240);
        VBox box = new VBox(10, new Label("Percentual de desconto (%)"), tf);
        BigDecimal max = getMaxDiscountForItem(item);
        Label warn = new Label();
        warn.getStyleClass().addAll(Styles.TEXT_SMALL, Styles.DANGER);
        warn.setVisible(false);
        UnaryOperator<TextFormatter.Change> percFilter = change -> {
            String nt = change.getControlNewText();
            if (nt.matches("\\d{0,3}([\\.,]\\d{0,2})?")) {
                return change;
            }
            return null;
        };
        tf.setTextFormatter(new TextFormatter<>(percFilter));
        javafx.scene.control.Button btnPad = new javafx.scene.control.Button("Teclado");
        btnPad.getStyleClass().add(atlantafx.base.theme.Styles.BUTTON_OUTLINED);
        btnPad.setOnAction(e -> VirtualKeyboardPopup.show(tf, VirtualKeyboardPopup.KeyboardType.NUMERIC, () -> {
            dlg.setResult(new java.math.BigDecimal(tf.getText().replace(",", ".")));
            dlg.close();
        }));
        tf.focusedProperty().addListener((o, ov, nv) -> {
            if (nv) VirtualKeyboardPopup.show(tf, VirtualKeyboardPopup.KeyboardType.NUMERIC, () -> {
                dlg.setResult(new java.math.BigDecimal(tf.getText().replace(",", ".")));
                dlg.close();
            });
        });
        VBox root = new VBox(12, box, warn, btnPad);
        root.setPadding(new Insets(10));
        dlg.getDialogPane().setContent(root);
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
        Node okBtn = dlg.getDialogPane().lookupButton(ButtonType.OK);
        tf.textProperty().addListener((obs, ov, nv) -> {
            try {
                BigDecimal v = new BigDecimal(nv.replace(",", "."));
                boolean invalid = v.compareTo(max) > 0 || v.compareTo(BigDecimal.ZERO) < 0;
                warn.setText(invalid ? ("Máximo permitido: " + max + "%") : "");
                warn.setVisible(invalid);
                okBtn.setDisable(invalid);
            } catch (Exception ex) {
                okBtn.setDisable(true);
                warn.setText("Valor inválido");
                warn.setVisible(true);
            }
        });
        dlg.setResultConverter(bt -> bt == ButtonType.OK ? new java.math.BigDecimal(tf.getText().replace(",", ".")) : null);
        dlg.showAndWait().ifPresent(p -> controller.atualizarDesconto(item, p));
    }

    private void showPriceDialog(ItemFatura item) {
        if (item == null) return;
        Dialog<BigDecimal> dlg = new Dialog<>();
        dlg.setTitle("Preço do Item");
        BigDecimal current = item.getPrecoUnitario() != null ? item.getPrecoUnitario() : BigDecimal.ZERO;
        TextField tf = new TextField(current.stripTrailingZeros().toPlainString());
        tf.setPrefWidth(240);
        VBox box = new VBox(10, new Label("Preço Unitário"), tf);
        UnaryOperator<TextFormatter.Change> priceFilter = change -> {
            String nt = change.getControlNewText();
            if (nt.matches("\\d{0,9}([\\.,]\\d{0,2})?")) return change;
            return null;
        };
        tf.setTextFormatter(new TextFormatter<>(priceFilter));

        javafx.scene.control.Button btnPad = new javafx.scene.control.Button("Teclado");
        btnPad.getStyleClass().add(atlantafx.base.theme.Styles.BUTTON_OUTLINED);
        Runnable ok = () -> {
            try {
                dlg.setResult(new BigDecimal(tf.getText().replace(",", ".")));
                dlg.close();
            } catch (Exception ignored) {}
        };
        btnPad.setOnAction(e -> VirtualKeyboardPopup.show(tf, VirtualKeyboardPopup.KeyboardType.NUMERIC, ok));
        tf.focusedProperty().addListener((o, ov, nv) -> {
            if (nv) VirtualKeyboardPopup.show(tf, VirtualKeyboardPopup.KeyboardType.NUMERIC, ok);
        });

        VBox root = new VBox(12, box, btnPad);
        root.setPadding(new Insets(10));
        dlg.getDialogPane().setContent(root);
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
        dlg.setResultConverter(bt -> {
            if (bt == ButtonType.OK) {
                try {
                    return new BigDecimal(tf.getText().replace(",", "."));
                } catch (Exception ex) {
                    return null;
                }
            }
            return null;
        });
        dlg.showAndWait().ifPresent(v -> controller.atualizarPrecoUnitario(item, v));
    }
    
    private void showQuantityDialog(ItemFatura item) {
        if (item == null) return;

        if (isFracionavel(item)) {
            Dialog<BigDecimal> dlg = new Dialog<>();
            dlg.setTitle("Quantidade");
            BigDecimal current = item.getQuantidadeDecimal() != null
                ? item.getQuantidadeDecimal()
                : BigDecimal.valueOf(item.getQuantidade() != null ? item.getQuantidade() : 1);

            TextField tf = new TextField(current.stripTrailingZeros().toPlainString());
            tf.setPrefWidth(240);
            String unit = unidadeLabel(item);
            VBox box = new VBox(10, new Label("Quantidade (" + unit + ")"), tf);

            UnaryOperator<TextFormatter.Change> decFilter = change -> {
                String nt = change.getControlNewText();
                int casas = getCasasDecimais(item);
                if (nt.matches("\\d{0,6}([\\.,]\\d{0," + casas + "})?")) return change;
                return null;
            };
            tf.setTextFormatter(new TextFormatter<>(decFilter));

            javafx.scene.control.Button btnPad = new javafx.scene.control.Button("Teclado");
            btnPad.getStyleClass().add(atlantafx.base.theme.Styles.BUTTON_OUTLINED);

            Runnable ok = () -> {
                try {
                    dlg.setResult(new BigDecimal(tf.getText().replace(",", ".")));
                    dlg.close();
                } catch (Exception ignored) {}
            };
            btnPad.setOnAction(e -> VirtualKeyboardPopup.show(tf, VirtualKeyboardPopup.KeyboardType.NUMERIC, ok));
            tf.focusedProperty().addListener((o, ov, nv) -> {
                if (nv) VirtualKeyboardPopup.show(tf, VirtualKeyboardPopup.KeyboardType.NUMERIC, ok);
            });

            VBox root = new VBox(12, box, btnPad);
            root.setPadding(new Insets(10));
            dlg.getDialogPane().setContent(root);
            dlg.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

            dlg.setResultConverter(bt -> {
                if (bt == ButtonType.OK) {
                    try {
                        return new BigDecimal(tf.getText().replace(",", "."));
                    } catch (Exception ex) {
                        return null;
                    }
                }
                return null;
            });

        dlg.showAndWait().ifPresent(q -> controller.atualizarQuantidadeDecimal(item, q));
            return;
        }

        Dialog<Integer> dlg = new Dialog<>();
        dlg.setTitle("Quantidade");
        TextField tf = new TextField(item.getQuantidade() != null ? item.getQuantidade().toString() : "1");
        tf.setPrefWidth(240);
        VBox box = new VBox(10, new Label("Quantidade"), tf);
        javafx.scene.control.Button btnPad = new javafx.scene.control.Button("Teclado");
        btnPad.getStyleClass().add(atlantafx.base.theme.Styles.BUTTON_OUTLINED);
        Runnable ok = () -> {
            try {
                dlg.setResult(Integer.parseInt(tf.getText().replace(",", ".")));
                dlg.close();
            } catch (NumberFormatException ignored) {}
        };
        btnPad.setOnAction(e -> VirtualKeyboardPopup.show(tf, VirtualKeyboardPopup.KeyboardType.NUMERIC, ok));
        tf.focusedProperty().addListener((o, ov, nv) -> {
            if (nv) VirtualKeyboardPopup.show(tf, VirtualKeyboardPopup.KeyboardType.NUMERIC, ok);
        });
        VBox root = new VBox(12, box, btnPad);
        root.setPadding(new Insets(10));
        dlg.getDialogPane().setContent(root);
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
        dlg.setResultConverter(bt -> {
            if (bt == ButtonType.OK) {
                try {
                    return Integer.parseInt(tf.getText().replace(",", "."));
                } catch (NumberFormatException ex) {
                    return null;
                }
            }
            return null;
        });
        dlg.showAndWait().ifPresent(q -> controller.atualizarQuantidade(item, q));
    }

    private void showQuantityDialogForProduct(Produto p, BigDecimal initialQty) {
        if (p == null) return;
        Dialog<BigDecimal> dlg = new Dialog<>();
        dlg.setTitle("Pesagem/Quantidade: " + p.getNome());
        
        TextField tf = new TextField(initialQty.compareTo(BigDecimal.ONE) == 0 ? "" : initialQty.stripTrailingZeros().toPlainString());
        tf.setPromptText("0.000");
        tf.setPrefWidth(240);
        tf.getStyleClass().add(Styles.LARGE);
        
        String unit = unidadeShort(p.getUnidadeMedida());
        VBox box = new VBox(10, new Label("Informe o peso ou quantidade (" + unit + "):"), tf);
        
        // Exibe preço por KG se for o caso
        if (isFracionavel(p)) {
            Label lblPreco = new Label("Preço: " + currencyFormat.format(p.getPrecoUnitario()) + " / " + unit);
            lblPreco.getStyleClass().add(Styles.TEXT_BOLD);
            box.getChildren().add(1, lblPreco);
        }

        UnaryOperator<TextFormatter.Change> decFilter = change -> {
            String nt = change.getControlNewText();
            int casas = isFracionavel(p) ? 3 : 0;
            if (nt.matches("\\d{0,6}([\\.,]\\d{0," + casas + "})?")) return change;
            return null;
        };
        tf.setTextFormatter(new TextFormatter<>(decFilter));

        // Atalhos de teclado no diálogo
        tf.setOnKeyPressed(ke -> {
            if (ke.getCode() == KeyCode.ENTER) {
                try {
                    BigDecimal val = new BigDecimal(tf.getText().replace(",", "."));
                    controller.adicionarProdutoAoCarrinho(p, val);
                    dlg.setResult(val);
                    dlg.close();
                } catch (Exception ignore) {}
            } else if (ke.getCode() == KeyCode.ESCAPE) {
                dlg.close();
            }
        });

        VBox root = new VBox(12, box);
        root.setPadding(new Insets(15));
        dlg.getDialogPane().setContent(root);
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
        
        dlg.setResultConverter(bt -> {
            if (bt == ButtonType.OK) {
                try {
                    return new BigDecimal(tf.getText().replace(",", "."));
                } catch (Exception ex) {
                    return null;
                }
            }
            return null;
        });
        
        javafx.application.Platform.runLater(tf::requestFocus);
        dlg.showAndWait().ifPresent(val -> controller.adicionarProdutoAoCarrinho(p, val));
    }

    private String unidadeShort(ao.allon.kubata.faturacao.enums.UnidadeMedida u) {
        if (u == null) return "un";
        return switch (u) {
            case KILOGRAMA -> "kg";
            case LITRO -> "L";
            case METRO -> "m";
            case HORA, SERVICO -> "h";
            default -> "un";
        };
    }

    private boolean isFracionavel(Produto p) {
        if (p == null || p.getUnidadeMedida() == null) return false;
        return switch (p.getUnidadeMedida()) {
            case KILOGRAMA, LITRO, METRO, HORA, SERVICO -> true;
            default -> false;
        };
    }

    private boolean isFracionavel(ItemFatura item) {
        if (item == null || item.getProduto() == null || item.getProduto().getUnidadeMedida() == null) return false;
        return switch (item.getProduto().getUnidadeMedida()) {
            case KILOGRAMA, LITRO, METRO, HORA, SERVICO -> true;
            default -> false;
        };
    }

    private String unidadeLabel(ItemFatura item) {
        if (item == null || item.getProduto() == null || item.getProduto().getUnidadeMedida() == null) return "un";
        return switch (item.getProduto().getUnidadeMedida()) {
            case KILOGRAMA -> "kg";
            case LITRO -> "L";
            case METRO -> "m";
            case HORA -> "h";
            case SERVICO -> "h";
            case CAIXA -> "cx";
            default -> "un";
        };
    }

    private String formatQuantidade(ItemFatura item) {
        if (item == null) return "";
        if (isFracionavel(item)) {
            BigDecimal q = item.getQuantidadeDecimal();
            if (q == null) q = BigDecimal.valueOf(item.getQuantidade() != null ? item.getQuantidade() : 1);
            int casas = getCasasDecimais(item);
            q = q.setScale(casas, RoundingMode.HALF_UP).stripTrailingZeros();
            return q.toPlainString().replace(".", ",") + " " + unidadeLabel(item);
        }
        return String.valueOf(item.getQuantidade() != null ? item.getQuantidade() : 1);
    }

    private int getCasasDecimais(ItemFatura item) {
        if (item == null || item.getProduto() == null) return 3;
        Integer c = item.getProduto().getCasasDecimaisQuantidade();
        if (c == null) return 3;
        return Math.max(0, Math.min(c, 6));
    }

    private BigDecimal getStepFracionavel(ItemFatura item) {
        if (item == null || item.getProduto() == null) return new BigDecimal("0.100");
        if (item.getProduto().getStepVenda() != null && item.getProduto().getStepVenda().compareTo(BigDecimal.ZERO) > 0) {
            return item.getProduto().getStepVenda();
        }
        if (item.getProduto().getUnidadeMedida() == null) return new BigDecimal("0.100");
        return switch (item.getProduto().getUnidadeMedida()) {
            case KILOGRAMA, LITRO, METRO -> new BigDecimal("0.100");
            case HORA, SERVICO -> new BigDecimal("0.250");
            default -> BigDecimal.ONE;
        };
    }

    private void updateQuantidadeDecimal(ItemFatura item, BigDecimal delta) {
        if (item == null) return;
        BigDecimal current = item.getQuantidadeDecimal() != null
            ? item.getQuantidadeDecimal()
            : BigDecimal.valueOf(item.getQuantidade() != null ? item.getQuantidade() : 1);
        controller.atualizarQuantidadeDecimal(item, current.add(delta));
    }

    private void setQuantidadeDecimal(ItemFatura item, BigDecimal value) {
        if (item == null || value == null) return;
        int casas = getCasasDecimais(item);
        BigDecimal q = value.setScale(casas, RoundingMode.HALF_UP);
        controller.atualizarQuantidadeDecimal(item, q);
    }
    
    private BigDecimal getMaxDiscountForItem(ItemFatura item) {
        return controller.getMaxAllowedDiscount(item);
    }
    
    private BigDecimal computeMaxGlobalDiscount() {
        BigDecimal max = new BigDecimal("50");
        for (ItemFatura it : controller.getCarrinho()) {
            BigDecimal m = getMaxDiscountForItem(it);
            if (m.compareTo(max) < 0) max = m;
        }
        return max;
    }

    private VBox buildProductsPanel() {
        VBox root = new VBox(10);
        root.setPadding(new Insets(10));
        root.setStyle("-fx-background-color: -color-bg-default;");

        // Top Bar: Search & Category
        HBox topBar = new HBox(10);
        topBar.setAlignment(Pos.CENTER_LEFT);
        
        txtSearch.setPromptText("Pesquisar produto (F3)...");
        txtSearch.setPrefHeight(36);
        HBox.setHgrow(txtSearch, Priority.ALWAYS);
        UnaryOperator<TextFormatter.Change> alnumFilter = change -> {
            String nt = change.getControlNewText();
            // Permite letras, números, ponto, vírgula e asterisco (*)
            if (nt.matches("[A-Za-z0-9\\.,\\*]*")) {
                lblSearchError.setVisible(false);
                return change;
            } else {
                lblSearchError.setText("Caracteres inválidos");
                lblSearchError.getStyleClass().setAll(Styles.TEXT_SMALL, Styles.DANGER);
                lblSearchError.setVisible(true);
                return null;
            }
        };
        txtSearch.setTextFormatter(new TextFormatter<>(alnumFilter));
        txtSearch.textProperty().addListener((o, ov, nv) -> {
            if (!nv.contains("*")) {
                controller.filtrarProdutos(nv);
            }
        });
        txtSearch.setOnAction(e -> {
            String input = txtSearch.getText().trim();
            if (input.isEmpty()) return;

            BigDecimal quantidade = BigDecimal.ONE;
            String termoPesquisa = input;

            // Suporte a multiplicador: QTD*PRODUTO (ex: 1.5*BANANA ou 2*123456)
            if (input.contains("*")) {
                String[] partes = input.split("\\*", 2);
                try {
                    quantidade = new BigDecimal(partes[0].replace(",", "."));
                    termoPesquisa = partes.length > 1 ? partes[1] : "";
                } catch (Exception ex) {
                    AlertUtils.showWarningAlert("Erro", "Quantidade inválida no comando '*' ");
                    return;
                }
            }

            if (termoPesquisa.isEmpty()) {
                txtSearch.selectAll();
                return;
            }

            boolean handled = controller.tentarAdicionarPorLeituraCodigo(termoPesquisa, quantidade);
            if (!handled) {
                // Se não achou pelo código exato, tenta pelo primeiro da lista filtrada
                var lista = controller.getProdutosDisponiveis();
                if (!lista.isEmpty()) {
                    Produto p = lista.get(0);
                    if (isFracionavel(p)) {
                        showQuantityDialogForProduct(p, quantidade);
                    } else {
                        controller.adicionarProdutoAoCarrinho(p, quantidade);
                    }
                } else {
                    AlertUtils.showWarningAlert("Não encontrado", "Nenhum produto encontrado para: " + termoPesquisa);
                }
            }
            txtSearch.clear();
        });
        txtSearch.focusedProperty().addListener((o, ov, nv) -> {
            boolean show = java.util.prefs.Preferences.userRoot().node("ao/allon/kubata/pdv").getBoolean("numpad.showOnFocus", true);
            if (nv && show && controller.isVirtualKeyboardEnabled()) {
                VirtualKeyboardPopup.show(txtSearch, VirtualKeyboardPopup.KeyboardType.ALPHANUMERIC);
            }
        });
        lblSearchCount.getStyleClass().addAll(Styles.TEXT_SMALL, Styles.TEXT_MUTED);
        lblSearchCount.setMinWidth(180);
        lblSearchError.setVisible(false);

        cbCategoria.setPromptText("Todas Categorias");
        cbCategoria.setPrefWidth(150);
        cbCategoria.valueProperty().addListener((o, ov, nv) -> controller.filtrarPorCategoria(nv));
        
        cbOrdenar.getItems().setAll("Nome (A-Z)", "Preço (↑)", "Preço (↓)");
        cbOrdenar.setPromptText("Ordenar");
        cbOrdenar.valueProperty().addListener((o, ov, nv) -> rebuildProductGrid());
        
        chkEmStock.selectedProperty().addListener((o, ov, nv) -> rebuildProductGrid());
        
        ToggleButton tgPopulares = new ToggleButton("Populares");
        tgPopulares.getStyleClass().add(Styles.BUTTON_OUTLINED);
        tgPopulares.selectedProperty().addListener((obs, ov, nv) -> {
            if (nv) controller.carregarPopulares();
            else controller.carregarProdutos();
        });
        
        Button btnReload = new Button("", IconUtils.icon(Feather.REFRESH_CW, IconUtils.SIZE_MEDIUM));
        btnReload.getStyleClass().add(Styles.BUTTON_ICON);
        btnReload.setTooltip(new Tooltip("Recarregar Produtos (F5)"));
        btnReload.setOnAction(e -> controller.carregarProdutos());

        Button btnPadSearch = new Button("Teclado", IconUtils.icon(Feather.TYPE, IconUtils.SIZE_SMALL));
        btnPadSearch.getStyleClass().add(Styles.BUTTON_OUTLINED);
        btnPadSearch.setOnAction(e -> VirtualKeyboardPopup.show(txtSearch, VirtualKeyboardPopup.KeyboardType.ALPHANUMERIC));
        // Oculta botão se teclado desativado (opcional, ou mantem como override manual)
        btnPadSearch.visibleProperty().bind(javafx.beans.binding.Bindings.createBooleanBinding(() -> controller.isVirtualKeyboardEnabled(), controller.getCarrinho())); 
        btnPadSearch.managedProperty().bind(btnPadSearch.visibleProperty());
        
        ToggleButton btnFull = new ToggleButton("", IconUtils.icon(Feather.ZOOM_IN, IconUtils.SIZE_SMALL));
        btnFull.setTooltip(new Tooltip("Tela cheia (F11)"));
        btnFull.getStyleClass().add(Styles.BUTTON_ICON);
        btnFull.setOnAction(e -> toggleFullscreen(btnFull));
        
        topBar.getChildren().addAll(
            txtSearch, lblSearchError, btnPadSearch,
            cbCategoria, cbOrdenar, chkEmStock, tgPopulares, btnReload, btnFull
        );

        // Products Grid
        productsGrid.setHgap(10);
        productsGrid.setVgap(10);
        productsGrid.setPadding(new Insets(10));
        productsGrid.setPrefColumns(3); // Responsive preference
        
        productsScroll.setFitToWidth(true);
        productsScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(productsScroll, Priority.ALWAYS);

        // Metrics Footer
        HBox resultsFooter = new HBox();
        resultsFooter.setAlignment(Pos.CENTER_RIGHT);
        resultsFooter.setPadding(new Insets(6,10,6,10));
        resultsFooter.setStyle("-fx-background-color: -color-bg-subtle; -fx-border-color: -color-border-default; -fx-border-width: 1 0 0 0;");
        lblResultsFooter.getStyleClass().addAll(Styles.TEXT_SMALL, Styles.TEXT_MUTED);
        resultsFooter.getChildren().add(lblResultsFooter);

        HBox metrics = new HBox(20);
        metrics.setAlignment(Pos.CENTER_LEFT);
        metrics.setPadding(new Insets(5));
        metrics.setStyle("-fx-background-color: -color-bg-subtle; -fx-background-radius: 4;");
        
        metrics.getChildren().addAll(
            createMetric("Vendas Hoje", lblVendasDia, Feather.DOLLAR_SIGN),
            createMetric("Ticket Médio", lblTicketMedio, Feather.TRENDING_UP)
        );

        root.getChildren().addAll(topBar, productsScroll, resultsFooter, metrics);
        return root;
    }
    
    private HBox createMetric(String title, Label valueLabel, Feather icon) {
        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER_LEFT);
        Label iconLbl = new Label(null, IconUtils.icon(icon, IconUtils.SIZE_MEDIUM));
        VBox texts = new VBox(2, new Label(title), valueLabel);
        ((Label)texts.getChildren().get(0)).getStyleClass().add(Styles.TEXT_MUTED);
        valueLabel.getStyleClass().add(Styles.TEXT_BOLD);
        box.getChildren().addAll(iconLbl, texts);
        return box;
    }

    private void bindData() {
        controller.getProdutosDisponiveis().addListener((javafx.collections.ListChangeListener<Produto>) c -> {
            rebuildProductGrid();
            updateSearchCount();
        });
        rebuildProductGrid(); // Initial build
        updateSearchCount();

        listCarrinho.setItems(controller.getCarrinho());
        controller.getCarrinho().addListener((javafx.collections.ListChangeListener<ItemFatura>) c -> updateTotals());
        updateTotals();

        cbCliente.setItems(javafx.collections.FXCollections.observableArrayList(controller.getClientesDisponiveis()));
        cbCliente.valueProperty().addListener((o, ov, nv) -> controller.setCliente(nv));
        if (controller.getCliente() != null) cbCliente.setValue(controller.getCliente());

        // Categories
        java.util.Set<String> cats = new java.util.HashSet<>();
        cats.add("Todas");
        for (Produto p : controller.getProdutosDisponiveis()) {
            if (p.getCategoria() != null && p.getCategoria().getNome() != null) 
                cats.add(p.getCategoria().getNome());
        }
        cbCategoria.setItems(javafx.collections.FXCollections.observableArrayList(cats));
    }

    private void rebuildProductGrid() {
        productsGrid.getChildren().clear();
        java.util.List<Produto> items = new java.util.ArrayList<>(controller.getProdutosDisponiveis());
        if (chkEmStock.isSelected()) {
            items.removeIf(p -> p.getStock() == null || p.getStock() <= 0);
        }
        String ord = cbOrdenar.getValue();
        if ("Nome (A-Z)".equals(ord)) {
            items.sort(java.util.Comparator.comparing(p -> p.getNome() == null ? "" : p.getNome().toLowerCase()));
        } else if ("Preço (↑)".equals(ord)) {
            items.sort(java.util.Comparator.comparing(p -> p.getPrecoUnitario() == null ? java.math.BigDecimal.ZERO : p.getPrecoUnitario()));
        } else if ("Preço (↓)".equals(ord)) {
            items.sort(java.util.Comparator.comparing((Produto p) -> p.getPrecoUnitario() == null ? java.math.BigDecimal.ZERO : p.getPrecoUnitario()).reversed());
        }
        for (Produto p : items) {
            productsGrid.getChildren().add(createProductCard(p));
        }
        lblResultsFooter.setText("Itens encontrados: " + items.size());
    }

    private Button createProductCard(Produto p) {
        Button card = new Button();
        card.getStyleClass().addAll("product-card");
        card.setPrefSize(160, 120);
        card.setMaxSize(160, 120);
        
        VBox content = new VBox(2);
        content.setAlignment(Pos.CENTER);
        
        Label name = new Label(p.getNome());
        name.setWrapText(true);
        name.setTextAlignment(TextAlignment.CENTER);
        name.setStyle("-fx-font-weight: bold; -fx-font-size: 1.1em;");
        
        Label price = new Label(currencyFormat.format(p.getPrecoUnitario()));
        price.getStyleClass().add(Styles.ACCENT);
        price.setStyle("-fx-font-weight: bold;");
        
        Label stock = new Label("Stock: " + formatStock(p));
        stock.getStyleClass().add(Styles.TEXT_SMALL);

        VBox fifoInfo = new VBox(1);
        fifoInfo.setAlignment(Pos.CENTER);
        
        try {
            var lotes = controller.getResumoFifo(p);
            if (lotes != null && lotes.size() > 1) {
                // Se houver mais de um lote, mostrar detalhamento antigo vs novo
                var loteAntigo = lotes.get(0);
                var loteNovo = lotes.get(lotes.size() - 1);
                
                Label lblAntigo = new Label(String.format("Antigo: %d (%s)", 
                        loteAntigo.quantidade(), 
                        Money.format(loteAntigo.precoVenda())));
                lblAntigo.setStyle("-fx-font-size: 0.8em; -fx-text-fill: -color-warning-fg;");
                
                Label lblNovo = new Label(String.format("Novo: %d (%s)", 
                        loteNovo.quantidade(), 
                        Money.format(loteNovo.precoVenda())));
                lblNovo.setStyle("-fx-font-size: 0.8em; -fx-text-fill: -color-success-fg;");
                
                fifoInfo.getChildren().addAll(lblAntigo, lblNovo);
                
                // Adiciona um badge visual de "Multi-Lote"
                Label badge = new Label("MULTI-LOTE");
                badge.getStyleClass().addAll("badge", Styles.SMALL, Styles.WARNING);
                content.getChildren().add(0, badge);
            }
        } catch (Exception ignore) {}
        
        content.getChildren().addAll(name, price, stock, fifoInfo);
        card.setGraphic(content);
        card.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        
        card.setOnAction(e -> {
            lastSelectedProduct = p;
            controller.adicionarProdutoAoCarrinho(p, 1);
        });
        
        return card;
    }

    private String formatStock(Produto p) {
        if (p == null) return "0";
        Integer s = p.getStock();
        if (s == null) s = 0;
        
        ao.allon.kubata.faturacao.enums.UnidadeMedida u = p.getUnidadeMedida();
        if (u == null) return String.valueOf(s);
        
        return switch (u) {
            case KILOGRAMA, LITRO, METRO -> {
                BigDecimal val = new BigDecimal(s).divide(new BigDecimal("1000"), 3, RoundingMode.HALF_UP).stripTrailingZeros();
                String unit = switch (u) {
                    case KILOGRAMA -> "kg";
                    case LITRO -> "L";
                    case METRO -> "m";
                    default -> "";
                };
                yield val.toPlainString().replace(".", ",") + " " + unit;
            }
            case HORA, SERVICO -> {
                BigDecimal val = new BigDecimal(s).divide(new BigDecimal("60"), 2, RoundingMode.HALF_UP).stripTrailingZeros();
                yield val.toPlainString().replace(".", ",") + " h";
            }
            case CAIXA -> s + " cx";
            default -> String.valueOf(s);
        };
    }

    private void updateTotals() {
        lblTotalValue.setText(currencyFormat.format(controller.getTotalCarrinho()));
    }
    
    private void updateSearchCount() {
        int count = controller.getProdutosDisponiveis().size();
        lblSearchCount.setText("Total encontrado: " + count + " registros");
    }

    private void installAccelerators() {
        sceneProperty().addListener((obs, ov, nv) -> {
            if (nv == null) return;
            nv.getAccelerators().put(new KeyCodeCombination(KeyCode.F2), () -> controller.abrirCaixa());
            nv.getAccelerators().put(new KeyCodeCombination(KeyCode.F3), () -> txtSearch.requestFocus());
            nv.getAccelerators().put(new KeyCodeCombination(KeyCode.F4), () -> controller.finalizarVenda());
            nv.getAccelerators().put(new KeyCodeCombination(KeyCode.F5), () -> controller.carregarProdutos());
            nv.getAccelerators().put(new KeyCodeCombination(KeyCode.F11), () -> toggleFullscreen(null));            
            nv.getAccelerators().put(new KeyCodeCombination(KeyCode.F7), () -> {
                if (!controller.canUseScale()) {
                    AlertUtils.showWarningAlert("Permissão", "Sem permissão para usar a balança.");
                    return;
                }
                showScaleDialog(null);
            });
        });
    }

    private void installResponsiveness() {
        getStyleClass().add("pdv-view");
    }

    private void applyResponsiveClasses(double w, double h) {
        boolean small = (w < 1024 || h < 768);
        if (small) {
            if (!getStyleClass().contains("small-screen")) getStyleClass().add("small-screen");
            productsGrid.setPrefColumns(2);
            splitPaneRef.setDividerPositions(0.5);
        } else {
            getStyleClass().remove("small-screen");
            productsGrid.setPrefColumns(3);
            splitPaneRef.setDividerPositions(0.35);
        }
    }

    private void refreshMetrics() {
        lblVendasDia.setText(currencyFormat.format(controller.getVendasDiaAtual()));
        lblTicketMedio.setText(currencyFormat.format(controller.getTicketMedioDiaAtual()));
    }

    private void toggleFullscreen(ToggleButton source) {
        if (getScene() == null || getScene().getWindow() == null) return;
        javafx.stage.Stage stage = (javafx.stage.Stage) getScene().getWindow();
        boolean now = !stage.isFullScreen();
        stage.setFullScreen(now);
        if (source != null) {
            source.setGraphic(IconUtils.icon(now ? Feather.ZOOM_OUT : Feather.ZOOM_IN, IconUtils.SIZE_SMALL));
            source.setSelected(now);
        }
    }
    private void showNovoClienteDialog() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        
        TextField txtNome = new TextField();
        TextField txtNif = new TextField();
        
        grid.addRow(0, new Label("Nome:"), txtNome);
        grid.addRow(1, new Label("NIF:"), txtNif);
        
        controller.getModalService().create()
            .title("Novo Cliente")
            .content(grid)
            .autoSize()
            .withConfirmButton("Criar", () -> {
                controller.criarClienteRapido(txtNome.getText(), txtNif.getText());
                cbCliente.setItems(javafx.collections.FXCollections.observableArrayList(controller.getClientesDisponiveis()));
                cbCliente.setValue(controller.getCliente());
                return true;
            })
            .withCancelButton()
            .buildAndShow();
    }

    private void showImportarDocumentoDialog() {
        List<Fatura> documentos = controller.buscarDocumentosImportaveis();
        
        if (documentos.isEmpty()) {
            controller.getModalService().create()
                .title("Importar Documento")
                .content(new Label("Nenhum documento disponível para importação.\n\nVerifique se existem Orçamentos ou Encomendas pendentes (Rascunho) nos últimos 30 dias."))
                .autoSize()
                .withCloseButton()
                .buildAndShow();
            return;
        }

        // Create table for documents
        TableView<Fatura> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.setPrefHeight(300);
        
        TableColumn<Fatura, String> colTipo = new TableColumn<>("Tipo");
        colTipo.setCellValueFactory(cell -> new SimpleStringProperty(
            cell.getValue().getTipoDocumento() != null ? cell.getValue().getTipoDocumento().getDescricao() : "-"));
        colTipo.setPrefWidth(100);
        
        TableColumn<Fatura, String> colNumero = new TableColumn<>("Número");
        colNumero.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getNumero()));
        colNumero.setPrefWidth(120);
        
        TableColumn<Fatura, String> colData = new TableColumn<>("Data");
        colData.setCellValueFactory(cell -> new SimpleStringProperty(
            cell.getValue().getDataEmissao() != null ? cell.getValue().getDataEmissao().toString() : "-"));
        colData.setPrefWidth(100);
        
        TableColumn<Fatura, String> colCliente = new TableColumn<>("Cliente");
        colCliente.setCellValueFactory(cell -> new SimpleStringProperty(
            cell.getValue().getCliente() != null ? cell.getValue().getCliente().getNome() : "Consumidor Final"));
        colCliente.setPrefWidth(150);
        
        TableColumn<Fatura, String> colTotal = new TableColumn<>("Total");
        colTotal.setCellValueFactory(cell -> new SimpleStringProperty(
            currencyFormat.format(cell.getValue().getTotal())));
        colTotal.setStyle("-fx-alignment: CENTER-RIGHT;");
        colTotal.setPrefWidth(100);
        
        TableColumn<Fatura, String> colItens = new TableColumn<>("Itens");
        colItens.setCellValueFactory(cell -> new SimpleStringProperty(
            String.valueOf(cell.getValue().getItens() != null ? cell.getValue().getItens().size() : 0)));
        colItens.setPrefWidth(60);
        
        table.getColumns().addAll(colTipo, colNumero, colData, colCliente, colTotal, colItens);
        table.setItems(javafx.collections.FXCollections.observableArrayList(documentos));
        
        // Selection listener to enable/disable import button
        final Fatura[] selecionado = {null};
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            selecionado[0] = newVal;
        });
        
        // Double-click to import
        table.setRowFactory(tv -> {
            TableRow<Fatura> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    importarDocumento(row.getItem());
                }
            });
            return row;
        });
        
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        
        Label lblInfo = new Label("Selecione um Orçamento ou Encomenda para importar:");
        lblInfo.getStyleClass().add(Styles.TEXT_SMALL);
        
        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);
        
        Button btnImportar = new Button("Importar Selecionado", IconUtils.icon(Feather.DOWNLOAD, IconUtils.SIZE_SMALL));
        btnImportar.getStyleClass().addAll(Styles.BUTTON_OUTLINED, Styles.ACCENT);
        btnImportar.setDisable(true);
        btnImportar.setOnAction(e -> {
            if (selecionado[0] != null) {
                importarDocumento(selecionado[0]);
            }
        });
        
        // Enable button when selection changes
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            btnImportar.setDisable(newVal == null);
        });
        
        actions.getChildren().add(btnImportar);
        
        content.getChildren().addAll(lblInfo, table, actions);
        VBox.setVgrow(table, Priority.ALWAYS);
        
        controller.getModalService().create()
            .title("Importar Documento para PDV (" + documentos.size() + " disponíveis)")
            .content(content)
            .autoSize()
            .withCancelButton("Fechar")
            .buildAndShow();
    }
    
    private void importarDocumento(Fatura documento) {
        if (documento == null) return;
        
        // Confirm before importing if cart has items
        if (!controller.getCarrinho().isEmpty()) {
            controller.getModalService().create()
                .title("Confirmar Importação")
                .content(new Label("O carrinho atual será limpo e substituído pelos itens do documento.\n\n" +
                    "Documento: " + documento.getNumero() + "\n" +
                    "Cliente: " + (documento.getCliente() != null ? documento.getCliente().getNome() : "Consumidor Final") + "\n" +
                    "Itens: " + (documento.getItens() != null ? documento.getItens().size() : 0) + "\n" +
                    "Total: " + currencyFormat.format(documento.getTotal()) + "\n\n" +
                    "Deseja continuar?"))
                .autoSize()
                .withConfirmButton("Importar", () -> {
                    controller.importarDocumentoParaCarrinho(documento.getId());
                    // Refresh client dropdown
                    if (controller.getCliente() != null) {
                        cbCliente.setValue(controller.getCliente());
                    }
                    return true;
                })
                .withCancelButton("Cancelar")
                .buildAndShow();
        } else {
            controller.importarDocumentoParaCarrinho(documento.getId());
            // Refresh client dropdown
            if (controller.getCliente() != null) {
                cbCliente.setValue(controller.getCliente());
            }
        }
    }

    private void showScaleDialog(Produto initialTarget) {
        VBox content = new VBox(12);
        content.setPadding(new Insets(12));
        ComboBox<String> cbPorts = new ComboBox<>();
        TextField tfBaud = new TextField("9600");
        TextArea taRaw = new TextArea();
        taRaw.setPrefRowCount(8);
        Label lblWeight = new Label("Peso: -");
        HBox row1 = new HBox(10, new Label("Porta:"), cbPorts, new Label("Baud:"), tfBaud);
        row1.setAlignment(Pos.CENTER_LEFT);
        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);
        Button btnConnect = new Button("Conectar");
        Button btnCapture = new Button("Capturar");
        Button btnUse = new Button("Pesar e adicionar");
        Button btnDisconnect = new Button("Desconectar");
        actions.getChildren().addAll(btnConnect, btnCapture, btnUse, btnDisconnect);
        content.getChildren().addAll(row1, lblWeight, taRaw, actions);

        ScaleSerialService service = new ScaleSerialService();
        cbPorts.getItems().setAll(service.listPorts());
        if (!cbPorts.getItems().isEmpty()) cbPorts.setValue(cbPorts.getItems().get(0));

        final BigDecimal[] lastW = {null};
        btnConnect.setOnAction(e -> {
            try {
                int baud = Integer.parseInt(tfBaud.getText().trim());
                boolean ok = service.connect(cbPorts.getValue(), baud);
                if (!ok) AlertUtils.showWarningAlert("Balança", "Falha ao abrir porta.");
            } catch (Exception ex) {
                AlertUtils.showWarningAlert("Balança", "Configuração inválida.");
            }
        });
        btnCapture.setOnAction(e -> {
            service.startCapture(s -> javafx.application.Platform.runLater(() -> {
                taRaw.appendText(s + System.lineSeparator());
            }), w -> javafx.application.Platform.runLater(() -> {
                lastW[0] = w;
                lblWeight.setText("Peso: " + w.stripTrailingZeros().toPlainString() + " kg");
            }));
        });
        btnDisconnect.setOnAction(e -> service.disconnect());
        btnUse.setOnAction(e -> {
            BigDecimal w = lastW[0];
            if (w == null || w.compareTo(BigDecimal.ZERO) <= 0) {
                AlertUtils.showWarningAlert("Balança", "Peso inválido.");
                return;
            }
            Produto target = initialTarget != null ? initialTarget : (lastSelectedProduct != null ? lastSelectedProduct : null);
            if (target == null) {
                if (!controller.getProdutosDisponiveis().isEmpty()) target = controller.getProdutosDisponiveis().get(0);
            }
            if (target == null) {
                AlertUtils.showWarningAlert("Balança", "Selecione um produto.");
                return;
            }
            int qtd = toUnidadesMinimas(target, w);
            if (qtd <= 0) qtd = 1;
            controller.adicionarProdutoAoCarrinhoBalanca(target, qtd, w);
        });

        controller.getModalService().create()
            .title("Balança")
            .content(content)
            .autoSize()
            .withCancelButton("Fechar")
            .buildAndShow();
    }

    private int toUnidadesMinimas(Produto p, BigDecimal qtdKg) {
        if (p == null || qtdKg == null) return 0;
        ao.allon.kubata.faturacao.enums.UnidadeMedida u = p.getUnidadeMedida();
        if (u == null) return qtdKg.setScale(0, RoundingMode.HALF_UP).intValue();
        return switch (u) {
            case KILOGRAMA, LITRO, METRO -> qtdKg.multiply(new BigDecimal("1000")).setScale(0, RoundingMode.HALF_UP).intValue();
            case HORA, SERVICO -> qtdKg.multiply(new BigDecimal("60")).setScale(0, RoundingMode.HALF_UP).intValue();
            default -> qtdKg.setScale(0, RoundingMode.HALF_UP).intValue();
        };
    }
}

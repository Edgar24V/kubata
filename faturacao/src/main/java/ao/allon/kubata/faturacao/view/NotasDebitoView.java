package ao.allon.kubata.faturacao.view;

import ao.allon.kubata.core.ui.table.AdvancedTableView;
import ao.allon.kubata.core.ui.table.TableUtils;
import ao.allon.kubata.faturacao.domain.Cliente;
import ao.allon.kubata.faturacao.domain.Fatura;
import ao.allon.kubata.faturacao.domain.ItemFatura;
import ao.allon.kubata.faturacao.domain.enums.StatusFatura;
import ao.allon.kubata.faturacao.domain.enums.TipoDocumento;
import ao.allon.kubata.faturacao.service.FaturaService;
import ao.allon.kubata.faturacao.service.JasperReportService;
import ao.allon.kubata.faturacao.ui.modal.ModalService;
import ao.allon.kubata.faturacao.ui.util.AlertUtils;
import ao.allon.kubata.faturacao.ui.util.IconUtils;
import atlantafx.base.controls.Card;
import atlantafx.base.controls.CustomTextField;
import atlantafx.base.theme.Styles;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import org.kordamp.ikonli.feather.Feather;
import net.sf.jasperreports.engine.JasperPrint;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class NotasDebitoView extends BorderPane {

    private final FaturaService faturaService;
    private final JasperReportService jasperReportService;
    private final ModalService modalService;

    private final AdvancedTableView<Fatura> table = new AdvancedTableView<>();
    private final ObservableList<Fatura> data = FXCollections.observableArrayList();
    private final CustomTextField txtPesquisa = new CustomTextField();
    private final ComboBox<String> cbEstado = new ComboBox<>();

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", new Locale("pt", "AO"));
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("pt", "AO"));

    private StackPane root;
    private VBox headerBox;
    private VBox body;
    private StackPane loadingOverlay;
    private VBox emptyState;

    private Label lblTotal;
    private Label lblValorTotal;
    private Label lblEmitidas;
    private Label lblCanceladas;

    public NotasDebitoView(FaturaService faturaService, JasperReportService jasperReportService, ModalService modalService) {
        this.faturaService = faturaService;
        this.jasperReportService = jasperReportService;
        this.modalService = modalService;
        setPadding(new Insets(0));
        initializeUI();
        refreshAsync();
    }

    private void initializeUI() {
        root = new StackPane();
        headerBox = new VBox(10);
        headerBox.setPadding(new Insets(20, 30, 10, 30));
        headerBox.setStyle("-fx-background-color: -color-bg-default; -fx-border-color: -color-border-default; -fx-border-width: 0 0 1 0;");

        body = new VBox(20);
        body.setPadding(new Insets(20, 30, 30, 30));
        body.setFillWidth(true);

        headerBox.getChildren().add(buildToolbar());
        body.getChildren().add(createKpis());
        body.getChildren().add(buildContent());

        VBox layout = new VBox(headerBox, body);
        VBox.setVgrow(body, Priority.ALWAYS);

        loadingOverlay = createLoadingOverlay();
        emptyState = createEmptyState();
        emptyState.setVisible(false);
        emptyState.setManaged(false);

        root.getChildren().addAll(layout, emptyState, loadingOverlay);
        setCenter(root);
    }

    private Node buildToolbar() {
        VBox root = new VBox(10);
        root.setPadding(Insets.EMPTY);
        Label title = new Label("Notas de Débito");
        title.getStyleClass().addAll(Styles.TITLE_2);
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
        Label subtitle = new Label("Emissão, visualização e gestão de Notas de Débito");
        subtitle.getStyleClass().add(Styles.TEXT_MUTED);
        VBox titleBox = new VBox(5, title, subtitle);

        txtPesquisa.setPromptText("Pesquisar por número, NIF, cliente");
        txtPesquisa.setLeft(IconUtils.icon(Feather.SEARCH, IconUtils.SIZE_SMALL));
        txtPesquisa.textProperty().addListener((o, ov, nv) -> applyFilter());

        cbEstado.setPromptText("Estado");
        cbEstado.setItems(FXCollections.observableArrayList("Todos", "Rascunho", "Emitida", "Cancelada"));
        cbEstado.getSelectionModel().selectFirst();
        cbEstado.valueProperty().addListener((o, ov, nv) -> applyFilter());

        Button btnNovo = new Button("Nova", IconUtils.icon(Feather.FILE_PLUS, IconUtils.SIZE_SMALL));
        btnNovo.getStyleClass().addAll(Styles.SUCCESS);
        btnNovo.setOnAction(e -> showNovaNotaDebito());

        Button btnImprimir = new Button("Imprimir", IconUtils.icon(Feather.PRINTER, IconUtils.SIZE_SMALL));
        btnImprimir.setOnAction(e -> handleImprimir());

        Button btnCancelar = new Button("Cancelar", IconUtils.icon(Feather.X_CIRCLE, IconUtils.SIZE_SMALL));
        btnCancelar.getStyleClass().addAll(Styles.DANGER);
        btnCancelar.setOnAction(e -> handleCancelar());

        Button btnAtualizar = new Button("Atualizar", IconUtils.icon(Feather.REFRESH_CW, IconUtils.SIZE_SMALL));
        btnAtualizar.getStyleClass().addAll(Styles.BUTTON_OUTLINED);
        btnAtualizar.setOnAction(e -> refreshAsync());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox row = new HBox(10, txtPesquisa, cbEstado, spacer, btnNovo, btnImprimir, btnCancelar, btnAtualizar);
        row.setAlignment(Pos.CENTER_LEFT);

        root.getChildren().addAll(titleBox, row);
        return root;
    }

    private Node buildContent() {
        TableUtils.standardize(table);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        TableColumn<Fatura, String> colNumero = new TableColumn<>("Número");
        colNumero.setCellValueFactory(new PropertyValueFactory<>("numero"));
        TableColumn<Fatura, String> colCliente = new TableColumn<>("Cliente");
        colCliente.setCellValueFactory(c -> javafx.beans.binding.Bindings.createStringBinding(() -> {
            Cliente cl = c.getValue().getCliente();
            return cl != null ? cl.getNome() : "";
        }));
        TableColumn<Fatura, String> colNif = new TableColumn<>("NIF");
        colNif.setCellValueFactory(c -> javafx.beans.binding.Bindings.createStringBinding(() -> {
            Cliente cl = c.getValue().getCliente();
            return cl != null && cl.getNif() != null ? cl.getNif() : "";
        }));
        TableColumn<Fatura, LocalDate> colData = new TableColumn<>("Data");
        colData.setCellValueFactory(new PropertyValueFactory<>("dataEmissao"));
        colData.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.format(dateFormatter));
            }
        });

        TableColumn<Fatura, BigDecimal> colTotal = new TableColumn<>("Total");
        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        colTotal.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : currencyFormat.format(item));
                if (!empty && item != null) setAlignment(Pos.CENTER_RIGHT);
            }
        });
        TableColumn<Fatura, StatusFatura> colStatus = new TableColumn<>("Estado");
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        TableColumn<Fatura, Void> colAcoes = new TableColumn<>("");
        colAcoes.setMinWidth(44);
        colAcoes.setMaxWidth(64);
        colAcoes.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("", IconUtils.icon(Feather.MORE_VERTICAL, IconUtils.SIZE_SMALL));
            {
                btn.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.FLAT);
                btn.setOnAction(e -> {
                    Fatura f = getTableRow() != null ? getTableRow().getItem() : null;
                    if (f == null) return;
                    ContextMenu cm = buildRowMenu(f);
                    cm.show(btn, javafx.geometry.Side.BOTTOM, 0, 0);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
                setText(null);
                setAlignment(Pos.CENTER);
            }
        });

        table.getColumns().setAll(colNumero, colCliente, colNif, colData, colTotal, colStatus, colAcoes);
        table.setItems(data);

        table.setRowFactory(tv -> {
            TableRow<Fatura> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (row.isEmpty()) return;
                if (event.getButton() == javafx.scene.input.MouseButton.SECONDARY) {
                    ContextMenu cm = buildRowMenu(row.getItem());
                    cm.show(row, event.getScreenX(), event.getScreenY());
                }
            });
            return row;
        });

        return table;
    }

    private void refreshAsync() {
        showLoading(true);
        Thread t = new Thread(() -> {
            try {
                List<Fatura> all = faturaService.findAll();
                List<Fatura> nd = all.stream()
                        .filter(f -> f.getTipoDocumento() == TipoDocumento.NOTA_DEBITO)
                        .collect(Collectors.toList());
                Platform.runLater(() -> {
                    data.setAll(nd);
                    table.setData(data);
                    applyFilter();
                    refreshKpis();
                });
            } catch (Exception ex) {
                Platform.runLater(() -> AlertUtils.showExceptionAlert("Erro", "Falha ao carregar Notas de Débito.", ex));
            } finally {
                Platform.runLater(() -> showLoading(false));
            }
        }, "notas-debito-reload");
        t.setDaemon(true);
        t.start();
    }

    private void applyFilter() {
        String q = txtPesquisa.getText() != null ? txtPesquisa.getText().toLowerCase() : "";
        String est = cbEstado.getValue();
        
        table.setFilter(f -> {
            boolean t = q.isEmpty()
                    || (f.getNumero() != null && f.getNumero().toLowerCase().contains(q))
                    || (f.getCliente() != null && f.getCliente().getNome() != null && f.getCliente().getNome().toLowerCase().contains(q))
                    || (f.getCliente() != null && f.getCliente().getNif() != null && f.getCliente().getNif().toLowerCase().contains(q));
            boolean s = "Todos".equals(est) || (f.getStatus() != null && f.getStatus().name().equalsIgnoreCase(est));
            return t && s;
        });
        
        updateEmptyState();
    }

    private HBox createKpis() {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);

        lblTotal = new Label("0");
        lblValorTotal = new Label(currencyFormat.format(0));
        lblEmitidas = new Label("0");
        lblCanceladas = new Label("0");

        row.getChildren().addAll(
            createKpiCard("Total", lblTotal, Feather.FILE_TEXT, Styles.ACCENT),
            createKpiCard("Valor Total", lblValorTotal, Feather.DOLLAR_SIGN, Styles.SUCCESS),
            createKpiCard("Emitidas", lblEmitidas, Feather.CHECK_CIRCLE, Styles.SUCCESS),
            createKpiCard("Canceladas", lblCanceladas, Feather.X_CIRCLE, Styles.DANGER)
        );

        return row;
    }

    private Card createKpiCard(String titulo, Label valorLabel, Feather icon, String colorStyle) {
        Card card = new Card();
        card.getStyleClass().addAll(Styles.ELEVATED_1);
        card.setMinWidth(170);
        card.setMaxWidth(220);

        VBox box = new VBox(8);
        box.setPadding(new Insets(14));

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        org.kordamp.ikonli.javafx.FontIcon fi = new org.kordamp.ikonli.javafx.FontIcon(icon);
        fi.setIconSize(22);
        fi.getStyleClass().add(colorStyle);
        Label lbl = new Label(titulo);
        lbl.getStyleClass().addAll(Styles.TEXT_MUTED);
        header.getChildren().addAll(fi, lbl);

        valorLabel.getStyleClass().addAll(Styles.TITLE_3, colorStyle);
        valorLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        box.getChildren().addAll(header, valorLabel);
        card.setBody(box);
        return card;
    }

    private void refreshKpis() {
        List<Fatura> list = data.stream().collect(Collectors.toList());
        long total = list.size();
        long emit = list.stream().filter(f -> f.getStatus() == StatusFatura.EMITIDA).count();
        long canc = list.stream().filter(f -> f.getStatus() == StatusFatura.CANCELADA).count();
        BigDecimal soma = list.stream().map(Fatura::getTotal).filter(v -> v != null).reduce(BigDecimal.ZERO, BigDecimal::add);

        if (lblTotal != null) lblTotal.setText(String.valueOf(total));
        if (lblEmitidas != null) lblEmitidas.setText(String.valueOf(emit));
        if (lblCanceladas != null) lblCanceladas.setText(String.valueOf(canc));
        if (lblValorTotal != null) lblValorTotal.setText(currencyFormat.format(soma));
    }

    private ContextMenu buildRowMenu(Fatura f) {
        MenuItem miNova = new MenuItem("Nova Nota de Débito", IconUtils.icon(Feather.FILE_PLUS, IconUtils.SIZE_SMALL));
        miNova.setOnAction(e -> showNovaNotaDebito());
        MenuItem miImprimir = new MenuItem("Imprimir", IconUtils.icon(Feather.PRINTER, IconUtils.SIZE_SMALL));
        miImprimir.setOnAction(e -> handleImprimir());
        MenuItem miCancelar = new MenuItem("Cancelar", IconUtils.icon(Feather.X_CIRCLE, IconUtils.SIZE_SMALL));
        miCancelar.setOnAction(e -> handleCancelar());
        MenuItem miAtualizar = new MenuItem("Atualizar", IconUtils.icon(Feather.REFRESH_CW, IconUtils.SIZE_SMALL));
        miAtualizar.setOnAction(e -> refreshAsync());
        return new ContextMenu(miNova, new SeparatorMenuItem(), miImprimir, miCancelar, new SeparatorMenuItem(), miAtualizar);
    }

    private StackPane createLoadingOverlay() {
        ProgressIndicator pi = new ProgressIndicator();
        pi.setMaxSize(72, 72);
        Label lbl = new Label("A carregar...");
        lbl.getStyleClass().addAll(Styles.TEXT_MUTED);
        VBox box = new VBox(10, pi, lbl);
        box.setAlignment(Pos.CENTER);

        StackPane overlay = new StackPane(box);
        overlay.setVisible(false);
        overlay.setManaged(false);
        overlay.setPickOnBounds(true);
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.08);");
        return overlay;
    }

    private VBox createEmptyState() {
        Label title = new Label("Sem resultados");
        title.getStyleClass().addAll(Styles.TITLE_3);
        Label sub = new Label("Nenhuma Nota de Débito encontrada com os filtros atuais.");
        sub.getStyleClass().addAll(Styles.TEXT_MUTED);
        Button btn = new Button("Criar Nota de Débito", IconUtils.icon(Feather.FILE_PLUS, IconUtils.SIZE_SMALL));
        btn.getStyleClass().addAll(Styles.SUCCESS);
        btn.setOnAction(e -> showNovaNotaDebito());

        VBox box = new VBox(8, title, sub, btn);
        box.setAlignment(Pos.CENTER);
        box.setMaxWidth(420);
        box.setStyle("-fx-background-color: -color-bg-subtle; -fx-background-radius: 10; -fx-padding: 18; -fx-border-color: -color-border-default; -fx-border-radius: 10;");

        VBox wrap = new VBox(box);
        wrap.setAlignment(Pos.CENTER);
        wrap.setPickOnBounds(false);
        return wrap;
    }

    private void updateEmptyState() {
        if (emptyState == null) return;
        boolean show = table != null && table.getItems() != null && table.getItems().isEmpty();
        emptyState.setVisible(show);
        emptyState.setManaged(show);
    }

    private void showLoading(boolean show) {
        if (loadingOverlay == null) return;
        loadingOverlay.setVisible(show);
        loadingOverlay.setManaged(show);
    }

    private Fatura getSelected() {
        return table.getSelectionModel().getSelectedItem();
    }

    private void showNovaNotaDebito() {
        VBox root = new VBox(10);
        root.setPadding(new Insets(10));
        Label titulo = new Label("Nova Nota de Débito");
        titulo.getStyleClass().add(Styles.TITLE_4);

        ComboBox<Fatura> cbFaturaRef = new ComboBox<>();
        cbFaturaRef.setPromptText("Fatura de referência");
        cbFaturaRef.setMaxWidth(Double.MAX_VALUE);
        List<Fatura> base = faturaService.findAll().stream()
                .filter(f -> f.getTipoDocumento() == TipoDocumento.FATURA || f.getTipoDocumento() == TipoDocumento.FATURA_RECIBO)
                .filter(f -> f.getStatus() == StatusFatura.EMITIDA || f.getStatus() == StatusFatura.PAGA)
                .collect(Collectors.toList());
        cbFaturaRef.setItems(FXCollections.observableArrayList(base));
        cbFaturaRef.setCellFactory(list -> new ListCell<>() {
            @Override protected void updateItem(Fatura f, boolean empty) {
                super.updateItem(f, empty);
                setText(empty || f == null ? "" : f.getNumero() + " • " + (f.getCliente() != null ? f.getCliente().getNome() : ""));
            }
        });
        cbFaturaRef.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Fatura f, boolean empty) {
                super.updateItem(f, empty);
                setText(empty || f == null ? "" : f.getNumero() + " • " + (f.getCliente() != null ? f.getCliente().getNome() : ""));
            }
        });

        TextArea txtMotivo = new TextArea();
        txtMotivo.setPromptText("Motivo do débito");
        txtMotivo.setPrefRowCount(3);

        TableView<ItemFatura> tvItens = new TableView<>();
        ObservableList<ItemFatura> itens = FXCollections.observableArrayList();
        tvItens.setItems(itens);
        tvItens.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        TableColumn<ItemFatura, String> cDesc = new TableColumn<>("Descrição");
        cDesc.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("descricao"));
        TableColumn<ItemFatura, Integer> cQtd = new TableColumn<>("Qtd");
        cQtd.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("quantidade"));
        TableColumn<ItemFatura, BigDecimal> cPU = new TableColumn<>("Preço Unit.");
        cPU.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("precoUnitario"));
        TableColumn<ItemFatura, BigDecimal> cIva = new TableColumn<>("IVA (%)");
        cIva.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("taxaIva"));
        TableColumn<ItemFatura, BigDecimal> cTot = new TableColumn<>("Total");
        cTot.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("total"));
        tvItens.getColumns().setAll(cDesc, cQtd, cPU, cIva, cTot);

        Button btnAdd = new Button("Adicionar Item", IconUtils.icon(Feather.PLUS, IconUtils.SIZE_SMALL));
        btnAdd.setOnAction(e -> {
            ItemFatura it = new ItemFatura();
            it.setDescricao("Descrição do débito");
            it.setQuantidade(1);
            it.setPrecoUnitario(new BigDecimal("0.00"));
            it.setTaxaIva(new BigDecimal("14.00"));
            itens.add(it);
        });
        Button btnRem = new Button("Remover Selecionado", IconUtils.icon(Feather.MINUS, IconUtils.SIZE_SMALL));
        btnRem.setOnAction(e -> {
            ItemFatura sel = tvItens.getSelectionModel().getSelectedItem();
            if (sel != null) itens.remove(sel);
        });
        HBox actions = new HBox(10, btnAdd, btnRem);
        actions.setAlignment(Pos.CENTER_LEFT);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.addRow(0, new Label("Fatura de referência"), cbFaturaRef);
        grid.addRow(1, new Label("Motivo"), txtMotivo);

        VBox content = new VBox(10, titulo, grid, tvItens, actions);
        content.setPadding(new Insets(8));
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);

        modalService.create()
                .title("Nova Nota de Débito")
                .content(scroll)
                .dynamicSize()
                .withConfirmButton("Emitir", () -> {
                    try {
                        Fatura ref = cbFaturaRef.getValue();
                        if (ref == null) return false;
                        if (itens.isEmpty()) return false;
                        String motivo = txtMotivo.getText();
                        if (motivo == null || motivo.isBlank()) return false;
                        List<ItemFatura> copy = new ArrayList<>();
                        for (ItemFatura src : itens) {
                            if (src.getDescricao() == null || src.getDescricao().isBlank()) return false;
                            if (src.getQuantidade() == null || src.getQuantidade() <= 0) return false;
                            if (src.getPrecoUnitario() == null || src.getPrecoUnitario().compareTo(BigDecimal.ZERO) < 0) return false;
                            ItemFatura it = new ItemFatura();
                            it.setDescricao(src.getDescricao());
                            it.setQuantidade(src.getQuantidade());
                            it.setPrecoUnitario(src.getPrecoUnitario());
                            it.setTaxaIva(src.getTaxaIva() != null ? src.getTaxaIva() : new BigDecimal("14.00"));
                            copy.add(it);
                        }
                        Fatura nd = faturaService.emitirNotaDebito(ref.getId(), motivo, copy);
                        AlertUtils.showInfoAlert("Sucesso", "Nota de Débito emitida: " + nd.getNumero());
                        refreshAsync();
                        return true;
                    } catch (Exception ex) {
                        AlertUtils.showExceptionAlert("Erro", "Falha ao emitir Nota de Débito.", ex);
                        return false;
                    }
                })
                .withCancelButton("Cancelar")
                .buildAndShow();
    }

    private void handleImprimir() {
        Fatura f = getSelected();
        if (f == null) {
            AlertUtils.showWarningAlert("Aviso", "Selecione uma Nota de Débito.");
            return;
        }
        try {
            Fatura faturaCompleta = faturaService.findFaturaParaImpressao(f.getId())
                    .orElseThrow(() -> new IllegalStateException("Nota de Débito não encontrada para impressão. ID: " + f.getId()));
            JasperPrint jp = jasperReportService.prepararFatura(faturaCompleta);
            jasperReportService.showReport(jp);
        } catch (Exception ex) {
            AlertUtils.showExceptionAlert("Erro", "Falha ao preparar relatório.", ex);
        }
    }

    private void handleCancelar() {
        Fatura f = getSelected();
        if (f == null) {
            AlertUtils.showWarningAlert("Aviso", "Selecione uma Nota de Débito.");
            return;
        }
        TextField motivo = new TextField();
        VBox box = new VBox(8, new Label("Motivo do cancelamento"), motivo);
        box.setPadding(new Insets(8));
        modalService.create()
                .title("Cancelar Nota de Débito " + (f.getNumero() != null ? f.getNumero() : ""))
                .content(box)
                .autoSize()
                .withConfirmButton("Cancelar ND", () -> {
                    try {
                        String m = motivo.getText();
                        if (m == null || m.isBlank()) return false;
                        faturaService.cancelarFatura(f.getId(), m);
                        refreshAsync();
                        return true;
                    } catch (Exception ex) {
                        AlertUtils.showExceptionAlert("Erro", "Falha ao cancelar Nota de Débito.", ex);
                        return false;
                    }
                })
                .withCancelButton("Fechar")
                .buildAndShow();
    }
}

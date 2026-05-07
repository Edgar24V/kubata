package ao.allon.kubata.faturacao.view;

import ao.allon.kubata.core.ui.table.AdvancedTableView;
import ao.allon.kubata.core.ui.table.TableUtils;
import ao.allon.kubata.faturacao.domain.Devolucao;
import ao.allon.kubata.faturacao.domain.enums.StatusDevolucao;
import ao.allon.kubata.faturacao.service.DevolucaoService;
import ao.allon.kubata.faturacao.service.FaturaService;
import ao.allon.kubata.faturacao.service.JasperReportService;
import ao.allon.kubata.faturacao.service.ProdutoService;
import ao.allon.kubata.faturacao.service.ClienteService;
import ao.allon.kubata.faturacao.ui.modal.ModalService;
import ao.allon.kubata.faturacao.ui.util.AlertUtils;
import ao.allon.kubata.faturacao.ui.util.IconUtils;
import atlantafx.base.controls.Card;
import atlantafx.base.controls.CustomTextField;
import atlantafx.base.theme.Styles;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import org.kordamp.ikonli.feather.Feather;

import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;
import java.util.List;

public class DevolucoesView extends VBox {

    private final DevolucaoService devolucaoService;
    private final FaturaService faturaService;
    private final ProdutoService produtoService;
    private final ClienteService clienteService;
    private final ModalService modalService;
    private final JasperReportService jasperReportService;

    private AdvancedTableView<Devolucao> table;

    private CustomTextField txtSearch;

    private StackPane root;
    private VBox headerBox;
    private VBox body;
    private StackPane loadingOverlay;
    private VBox emptyState;

    private Label lblTotal;
    private Label lblPendentes;
    private Label lblAprovadas;
    private Label lblConcluidas;
    private Label lblRejeitadas;

    private List<Devolucao> baseData;

    public DevolucoesView(DevolucaoService devolucaoService, FaturaService faturaService, ProdutoService produtoService, ClienteService clienteService, ModalService modalService, JasperReportService jasperReportService) {
        this.devolucaoService = devolucaoService;
        this.faturaService = faturaService;
        this.produtoService = produtoService;
        this.clienteService = clienteService;
        this.modalService = modalService;
        this.jasperReportService = jasperReportService;

        setPadding(new Insets(0));
        setSpacing(0);

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

        headerBox.getChildren().add(createHeader());
        body.getChildren().add(createKpis());
        body.getChildren().add(createFiltersAndTable());

        VBox layout = new VBox(headerBox, body);
        VBox.setVgrow(body, Priority.ALWAYS);

        loadingOverlay = createLoadingOverlay();
        emptyState = createEmptyState();
        emptyState.setVisible(false);
        emptyState.setManaged(false);

        root.getChildren().addAll(layout, emptyState, loadingOverlay);
        getChildren().add(root);
    }

    private HBox createHeader() {
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(5);
        Label title = new Label("Devoluções");
        title.getStyleClass().addAll(Styles.TITLE_2);
        title.setFont(Font.font("System", 24));
        Label subtitle = new Label("Solicitação, análise e processamento de devoluções (geração de NC)");
        subtitle.getStyleClass().addAll(Styles.TEXT_MUTED);
        titleBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnNova = new Button("Nova", IconUtils.icon(Feather.PLUS_CIRCLE, IconUtils.SIZE_SMALL));
        btnNova.getStyleClass().addAll(Styles.SUCCESS);
        btnNova.setOnAction(e -> handleNovaSolicitacao());

        Button btnAtualizar = new Button("Atualizar", IconUtils.icon(Feather.REFRESH_CW, IconUtils.SIZE_SMALL));
        btnAtualizar.getStyleClass().addAll(Styles.BUTTON_OUTLINED);
        btnAtualizar.setOnAction(e -> refreshAsync());

        header.getChildren().addAll(titleBox, spacer, btnNova, btnAtualizar);
        return header;
    }

    private HBox createKpis() {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);

        lblTotal = new Label("0");
        lblPendentes = new Label("0");
        lblAprovadas = new Label("0");
        lblConcluidas = new Label("0");
        lblRejeitadas = new Label("0");

        row.getChildren().addAll(
            createKpiCard("Total", lblTotal, Feather.FILE_TEXT, Styles.ACCENT),
            createKpiCard("Pendentes", lblPendentes, Feather.ALERT_CIRCLE, Styles.WARNING),
            createKpiCard("Aprovadas", lblAprovadas, Feather.CHECK_CIRCLE, Styles.SUCCESS),
            createKpiCard("Concluídas", lblConcluidas, Feather.CHECK, Styles.SUCCESS),
            createKpiCard("Rejeitadas", lblRejeitadas, Feather.X_CIRCLE, Styles.DANGER)
        );

        return row;
    }

    private Card createKpiCard(String titulo, Label valorLabel, Feather icon, String colorStyle) {
        Card card = new Card();
        card.getStyleClass().addAll(Styles.ELEVATED_1);
        card.setMinWidth(160);
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

    private VBox createFiltersAndTable() {
        HBox filters = new HBox(12);
        filters.setAlignment(Pos.CENTER_LEFT);

        txtSearch = new CustomTextField();
        txtSearch.setPromptText("Pesquisar por número, cliente, fatura...");
        txtSearch.setLeft(IconUtils.icon(Feather.SEARCH, IconUtils.SIZE_SMALL));
        txtSearch.setPrefWidth(280);
        txtSearch.textProperty().addListener((obs, ov, nv) -> applyFilter());

        Button btnLimpar = new Button("Limpar", IconUtils.icon(Feather.X, IconUtils.SIZE_SMALL));
        btnLimpar.getStyleClass().addAll(Styles.BUTTON_OUTLINED);
        btnLimpar.setOnAction(e -> {
            if (txtSearch != null) txtSearch.clear();
            applyFilter();
        });

        filters.getChildren().addAll(txtSearch, btnLimpar);

        HBox toolbar = new HBox(10);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        Button analisar = new Button("Analisar", IconUtils.icon(Feather.EYE, IconUtils.SIZE_SMALL));
        analisar.setOnAction(e -> handleAnalisar());

        Button processar = new Button("Processar (Gerar NC)", IconUtils.icon(Feather.CHECK_CIRCLE, IconUtils.SIZE_SMALL));
        processar.getStyleClass().addAll(Styles.SUCCESS);
        processar.setOnAction(e -> handleProcessar());

        toolbar.getChildren().addAll(analisar, new Separator(Orientation.VERTICAL), processar);

        table = buildTable();
        VBox.setVgrow(table, Priority.ALWAYS);

        return new VBox(8, filters, toolbar, table);
    }

    private AdvancedTableView<Devolucao> buildTable() {
        table = new AdvancedTableView<>();
        TableUtils.standardize(table);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        TableColumn<Devolucao, String> colNumero = new TableColumn<>("Número");
        colNumero.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getNumero()));

        TableColumn<Devolucao, String> colCliente = new TableColumn<>("Cliente");
        colCliente.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getCliente().getNome()));

        TableColumn<Devolucao, String> colData = new TableColumn<>("Data Solicitação");
        colData.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(
                cell.getValue().getDataSolicitacao().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))));

        TableColumn<Devolucao, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getStatus().getDescricao()));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.contains("Pendente")) setStyle("-fx-text-fill: orange; -fx-font-weight: bold;");
                    else if (item.contains("Concluída")) setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                    else if (item.contains("Rejeitada")) setStyle("-fx-text-fill: red;");
                    else setStyle("-fx-text-fill: -color-fg-default;");
                }
            }
        });

        TableColumn<Devolucao, String> colOrigem = new TableColumn<>("Fatura Origem");
        colOrigem.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(
                cell.getValue().getFaturaOrigem() != null ? cell.getValue().getFaturaOrigem().getNumero() : "-"));

        table.getColumns().addAll(colNumero, colCliente, colData, colOrigem, colStatus);

        TableColumn<Devolucao, Void> colAcoes = new TableColumn<>("");
        colAcoes.setMinWidth(44);
        colAcoes.setMaxWidth(64);
        colAcoes.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("", IconUtils.icon(Feather.MORE_VERTICAL, IconUtils.SIZE_SMALL));
            {
                btn.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.FLAT);
                btn.setOnAction(e -> {
                    Devolucao d = getTableRow() != null ? getTableRow().getItem() : null;
                    if (d == null) return;
                    ContextMenu cm = buildRowMenu(d);
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

        table.getColumns().add(colAcoes);

        table.setRowFactory(tv -> {
            TableRow<Devolucao> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (row.isEmpty()) return;
                if (event.getButton() == javafx.scene.input.MouseButton.SECONDARY) {
                    ContextMenu cm = buildRowMenu(row.getItem());
                    cm.show(row, event.getScreenX(), event.getScreenY());
                }
            });
            return row;
        });

        VBox.setVgrow(table, Priority.ALWAYS);
        return table;
    }

    private void refreshAsync() {
        showLoading(true);
        Thread t = new Thread(() -> {
            try {
                List<Devolucao> list = devolucaoService.findAll();
                Platform.runLater(() -> {
                    baseData = list;
                    table.setData(FXCollections.observableArrayList(baseData));
                    applyFilter();
                    refreshKpis();
                });
            } catch (Exception ex) {
                Platform.runLater(() -> AlertUtils.showExceptionAlert("Erro", "Falha ao carregar Devoluções.", ex));
            } finally {
                Platform.runLater(() -> showLoading(false));
            }
        }, "devolucoes-reload");
        t.setDaemon(true);
        t.start();
    }

    private void applyFilter() {
        if (baseData == null) return;
        String text = txtSearch != null && txtSearch.getText() != null ? txtSearch.getText().trim() : "";
        
        table.setFilter(d -> {
            if (text.isEmpty()) return true;
            String lower = text.toLowerCase();
            return (d.getNumero() != null && d.getNumero().toLowerCase().contains(lower)) ||
                   (d.getCliente() != null && d.getCliente().getNome() != null && d.getCliente().getNome().toLowerCase().contains(lower)) ||
                   (d.getFaturaOrigem() != null && d.getFaturaOrigem().getNumero() != null && d.getFaturaOrigem().getNumero().toLowerCase().contains(lower));
        });
        
        updateEmptyState();
    }

    private void refreshKpis() {
        List<Devolucao> list = baseData != null ? baseData : java.util.Collections.emptyList();
        long total = list.size();
        long pend = list.stream().filter(d -> d.getStatus() == StatusDevolucao.PENDENTE).count();
        long apr = list.stream().filter(d -> d.getStatus() == StatusDevolucao.APROVADA).count();
        long conc = list.stream().filter(d -> d.getStatus() == StatusDevolucao.CONCLUIDA).count();
        long rej = list.stream().filter(d -> d.getStatus() == StatusDevolucao.REJEITADA).count();
        if (lblTotal != null) lblTotal.setText(String.valueOf(total));
        if (lblPendentes != null) lblPendentes.setText(String.valueOf(pend));
        if (lblAprovadas != null) lblAprovadas.setText(String.valueOf(apr));
        if (lblConcluidas != null) lblConcluidas.setText(String.valueOf(conc));
        if (lblRejeitadas != null) lblRejeitadas.setText(String.valueOf(rej));
    }

    private ContextMenu buildRowMenu(Devolucao d) {
        MenuItem miAnalisar = new MenuItem("Analisar", IconUtils.icon(Feather.EYE, IconUtils.SIZE_SMALL));
        miAnalisar.setOnAction(e -> handleAnalisar());
        MenuItem miProcessar = new MenuItem("Processar (Gerar NC)", IconUtils.icon(Feather.CHECK_CIRCLE, IconUtils.SIZE_SMALL));
        miProcessar.setOnAction(e -> handleProcessar());
        MenuItem miAtualizar = new MenuItem("Atualizar", IconUtils.icon(Feather.REFRESH_CW, IconUtils.SIZE_SMALL));
        miAtualizar.setOnAction(e -> refreshAsync());
        return new ContextMenu(miAnalisar, miProcessar, new SeparatorMenuItem(), miAtualizar);
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
        Label sub = new Label("Nenhuma devolução encontrada com os filtros atuais.");
        sub.getStyleClass().addAll(Styles.TEXT_MUTED);
        Button btn = new Button("Criar Devolução", IconUtils.icon(Feather.PLUS_CIRCLE, IconUtils.SIZE_SMALL));
        btn.getStyleClass().addAll(Styles.SUCCESS);
        btn.setOnAction(e -> handleNovaSolicitacao());

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

    private void handleNovaSolicitacao() {
        NovaDevolucaoView view = new NovaDevolucaoView(devolucaoService, faturaService, clienteService, produtoService, modalService);
        modalService.create()
                .title("Nova Solicitação de Devolução")
                .content(view)
                .dynamicSize()
                .buildAndShow();
    }

    private void handleAnalisar() {
        Devolucao selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtils.showWarningAlert("Seleção", "Selecione uma devolução para analisar.");
            return;
        }
        if (selected.getStatus() != StatusDevolucao.PENDENTE) {
            AlertUtils.showWarningAlert("Aviso", "Apenas devoluções pendentes podem ser analisadas.");
            return;
        }

        TextArea txtParecer = new TextArea();
        txtParecer.setPromptText("Insira o parecer técnico...");
        
        VBox content = new VBox(10, new Label("Parecer da Análise:"), txtParecer);
        
        modalService.create()
                .title("Análise de Devolução " + selected.getNumero())
                .content(content)
                .autoSize()
                .withCustomButton("Aprovar", () -> {
                    devolucaoService.analisarSolicitacao(selected.getId(), true, txtParecer.getText());
                    refreshAsync();
                    AlertUtils.showInfoAlert("Sucesso", "Devolução aprovada.");
                }, Styles.SUCCESS)
                .withCustomButton("Rejeitar", () -> {
                    devolucaoService.analisarSolicitacao(selected.getId(), false, txtParecer.getText());
                    refreshAsync();
                    AlertUtils.showInfoAlert("Sucesso", "Devolução rejeitada.");
                }, Styles.DANGER)
                .withCancelButton("Cancelar")
                .buildAndShow();
    }

    private void handleProcessar() {
        Devolucao selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtils.showWarningAlert("Seleção", "Selecione uma devolução para processar.");
            return;
        }
        if (selected.getStatus() != StatusDevolucao.APROVADA) {
            AlertUtils.showWarningAlert("Aviso", "Apenas devoluções aprovadas podem ser processadas.");
            return;
        }

        modalService.create()
                .title("Processar Devolução")
                .content(new Label("Confirma o processamento? Isso irá gerar uma Nota de Crédito e atualizar o estoque."))
                .autoSize()
                .withConfirmButton("Confirmar", () -> {
                    try {
                        devolucaoService.processarDevolucao(selected.getId());
                        refreshAsync();
                        AlertUtils.showInfoAlert("Sucesso", "Devolução processada e Nota de Crédito gerada.");
                        return true;
                    } catch (Exception ex) {
                        AlertUtils.showExceptionAlert("Erro", "Falha ao processar devolução.", ex);
                        return false;
                    }
                })
                .withCancelButton("Cancelar")
                .buildAndShow();
    }
}

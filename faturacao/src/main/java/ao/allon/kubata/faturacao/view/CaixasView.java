package ao.allon.kubata.faturacao.view;

import ao.allon.kubata.faturacao.domain.Caixa;
import ao.allon.kubata.faturacao.domain.MovimentoCaixa;
import ao.allon.kubata.faturacao.domain.enums.StatusCaixa;
import ao.allon.kubata.faturacao.service.CaixaService;
import ao.allon.kubata.faturacao.service.SessionManager;
import ao.allon.kubata.faturacao.ui.modal.ModalService;
import ao.allon.kubata.faturacao.ui.util.AlertUtils;
import ao.allon.kubata.faturacao.ui.util.IconUtils;
import atlantafx.base.controls.Card;
import atlantafx.base.controls.CustomTextField;
import atlantafx.base.theme.Styles;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import org.kordamp.ikonli.feather.Feather;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * View para gestão de caixas (abertura, fechamento e movimentos).
 */
public class CaixasView extends VBox {

    private final CaixaService caixaService;
    private final SessionManager sessionManager;
    private final ModalService modalService;

    private final TableView<Caixa> table = new TableView<>();
    private final ObservableList<Caixa> masterData = FXCollections.observableArrayList();
    private FilteredList<Caixa> filtered;
    private SortedList<Caixa> sorted;
    private final DateTimeFormatter df = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private Label lblCaixaAberto;
    private Label lblSaldoInicial;
    private Label lblSaldoAtual;
    private Label lblTotalMovimentos;

    private CustomTextField txtSearch;
    private ComboBox<StatusCaixa> cbStatus;

    private Card buildKpiCard(String title, Label value, Feather icon, String accentStyle) {
        Label t = new Label(title);
        t.getStyleClass().add(Styles.TEXT_MUTED);

        HBox head = new HBox(8, IconUtils.icon(icon, IconUtils.SIZE_SMALL), t);
        head.setAlignment(Pos.CENTER_LEFT);

        VBox body = new VBox(6, head, value);
        body.setAlignment(Pos.CENTER_LEFT);

        Card card = new Card();
        card.getStyleClass().add(Styles.ELEVATED_1);
        if (accentStyle != null && !accentStyle.isBlank()) {
            card.getStyleClass().add(accentStyle);
        }
        card.setBody(body);
        return card;
    }

    private String fmtKz(BigDecimal value) {
        if (value == null) {
            return "Kz 0,00";
        }
        return String.format(Locale.getDefault(), "Kz %,.2f", value);
    }

    private BigDecimal computeSaldoAtual(Caixa caixa) {
        if (caixa == null || caixa.getId() == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = BigDecimal.ZERO;
        var movimentos = caixaService.getMovimentos(caixa.getId());
        for (var m : movimentos) {
            if (m.getTipo() == ao.allon.kubata.faturacao.domain.enums.TipoMovimento.VENDA ||
                m.getTipo() == ao.allon.kubata.faturacao.domain.enums.TipoMovimento.ABERTURA ||
                m.getTipo() == ao.allon.kubata.faturacao.domain.enums.TipoMovimento.SUPRIMENTO) {
                total = total.add(m.getValor());
            } else if (m.getTipo() == ao.allon.kubata.faturacao.domain.enums.TipoMovimento.SANGRIA ||
                       m.getTipo() == ao.allon.kubata.faturacao.domain.enums.TipoMovimento.ESTORNO) {
                total = total.subtract(m.getValor());
            }
        }
        return total;
    }

    public CaixasView(CaixaService caixaService,
                      SessionManager sessionManager,
                      ModalService modalService) {
        this.caixaService = caixaService;
        this.sessionManager = sessionManager;
        this.modalService = modalService;

        setSpacing(20);
        setPadding(new Insets(20));

        setupHeader();
        setupKpis();
        setupTable();
        setupMovimentosTable();
        loadData();
    }

    private void setupKpis() {
        lblCaixaAberto = new Label("-");
        lblSaldoInicial = new Label("Kz 0,00");
        lblSaldoAtual = new Label("Kz 0,00");
        lblTotalMovimentos = new Label("0");

        lblCaixaAberto.getStyleClass().add(Styles.TITLE_4);
        lblSaldoInicial.getStyleClass().add(Styles.TITLE_4);
        lblSaldoAtual.getStyleClass().add(Styles.TITLE_4);
        lblTotalMovimentos.getStyleClass().add(Styles.TITLE_4);

        GridPane grid = new GridPane();
        grid.setHgap(12);

        ColumnConstraints cc = new ColumnConstraints();
        cc.setPercentWidth(25);
        grid.getColumnConstraints().addAll(cc, cc, cc, cc);

        grid.add(buildKpiCard("Caixa aberto", lblCaixaAberto, Feather.UNLOCK, Styles.SUCCESS), 0, 0);
        grid.add(buildKpiCard("Saldo inicial", lblSaldoInicial, Feather.PLAY, Styles.ACCENT), 1, 0);
        grid.add(buildKpiCard("Saldo atual", lblSaldoAtual, Feather.DOLLAR_SIGN, Styles.WARNING), 2, 0);
        grid.add(buildKpiCard("Movimentos", lblTotalMovimentos, Feather.LIST, Styles.TEXT_MUTED), 3, 0);

        getChildren().add(grid);
    }

    private void setupHeader() {
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 10, 0));

        VBox titleBox = new VBox(5);
        Label title = new Label("Gestão de Caixas");
        title.getStyleClass().add(Styles.TITLE_3);
        Label subtitle = new Label("Abertura, fechamento e movimentos de caixa");
        subtitle.getStyleClass().add(Styles.TEXT_MUTED);
        titleBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        txtSearch = new CustomTextField();
        txtSearch.setPromptText("Pesquisar (ID/Usuário)...");
        txtSearch.setLeft(IconUtils.icon(Feather.SEARCH, IconUtils.SIZE_SMALL));
        txtSearch.textProperty().addListener((obs, o, n) -> applyFilters());

        cbStatus = new ComboBox<>();
        cbStatus.getItems().add(null);
        cbStatus.getItems().addAll(StatusCaixa.ABERTO, StatusCaixa.FECHADO);
        cbStatus.setValue(null);
        cbStatus.setPromptText("Status");
        cbStatus.valueProperty().addListener((obs, o, n) -> applyFilters());

        Button btnLimpar = new Button("Limpar", IconUtils.icon(Feather.X, IconUtils.SIZE_SMALL));
        btnLimpar.getStyleClass().addAll(Styles.BUTTON_OUTLINED);
        btnLimpar.setOnAction(e -> {
            txtSearch.clear();
            cbStatus.setValue(null);
            applyFilters();
        });

        Button btnAbrir = new Button("Abrir Caixa", IconUtils.icon(Feather.UNLOCK, IconUtils.SIZE_SMALL));
        btnAbrir.getStyleClass().addAll(Styles.BUTTON_OUTLINED, Styles.SUCCESS);
        btnAbrir.setOnAction(e -> abrirCaixa());

        Button btnAtualizar = new Button("Atualizar", IconUtils.icon(Feather.REFRESH_CW, IconUtils.SIZE_SMALL));
        btnAtualizar.getStyleClass().add(Styles.BUTTON_OUTLINED);
        btnAtualizar.setOnAction(e -> loadData());

        boolean canEdit = sessionManager.hasAccess("CAIXA", "Editar");
        btnAbrir.setDisable(!canEdit);

        header.getChildren().addAll(titleBox, spacer, txtSearch, cbStatus, btnLimpar, btnAtualizar, btnAbrir);
        getChildren().add(header);
    }

    private void setupTable() {
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        TableColumn<Caixa, Long> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("id"));
        colId.setMinWidth(60);

        TableColumn<Caixa, String> colUsuario = new TableColumn<>("Usuário");
        colUsuario.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("usuario"));
        colUsuario.setMinWidth(120);

        TableColumn<Caixa, String> colAbertura = new TableColumn<>("Data Abertura");
        colAbertura.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                } else {
                    Caixa c = getTableRow().getItem();
                    setText(c.getDataAbertura() != null ? c.getDataAbertura().format(df) : "");
                }
            }
        });
        colAbertura.setMinWidth(140);

        TableColumn<Caixa, String> colFecho = new TableColumn<>("Data Fecho");
        colFecho.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                } else {
                    Caixa c = getTableRow().getItem();
                    setText(c.getDataFecho() != null ? c.getDataFecho().format(df) : "-");
                }
            }
        });
        colFecho.setMinWidth(140);

        TableColumn<Caixa, String> colSaldoInicial = new TableColumn<>("Saldo Inicial");
        colSaldoInicial.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                } else {
                    Caixa c = getTableRow().getItem();
                    setText(c.getSaldoInicial() != null ? String.format("Kz %.2f", c.getSaldoInicial()) : "-");
                }
            }
        });
        colSaldoInicial.setMinWidth(120);

        TableColumn<Caixa, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                    setStyle("");
                } else {
                    Caixa c = getTableRow().getItem();
                    if (c.getStatus() == StatusCaixa.ABERTO) {
                        setText("ABERTO");
                        setTextFill(Color.GREEN);
                        setStyle("-fx-font-weight: bold;");
                    } else {
                        setText("FECHADO");
                        setTextFill(Color.GRAY);
                    }
                }
            }
        });
        colStatus.setMinWidth(100);

        TableColumn<Caixa, Void> colAcoes = new TableColumn<>("Ações");
        colAcoes.setMinWidth(200);
        colAcoes.setCellFactory(col -> new TableCell<>() {
            private final Button btnFechar = new Button("Fechar", IconUtils.icon(Feather.LOCK, IconUtils.SIZE_SMALL));
            private final Button btnMovimentos = new Button("Movimentos", IconUtils.icon(Feather.LIST, IconUtils.SIZE_SMALL));
            private final HBox box = new HBox(5, btnMovimentos, btnFechar);

            {
                btnFechar.getStyleClass().addAll(Styles.BUTTON_OUTLINED, Styles.DANGER, Styles.SMALL);
                btnMovimentos.getStyleClass().addAll(Styles.BUTTON_OUTLINED, Styles.SMALL);

                boolean canEdit = sessionManager.hasAccess("CAIXA", "Editar");
                btnFechar.setDisable(!canEdit);

                btnFechar.setOnAction(e -> {
                    Caixa c = getTableView().getItems().get(getIndex());
                    fecharCaixa(c);
                });

                btnMovimentos.setOnAction(e -> {
                    Caixa c = getTableView().getItems().get(getIndex());
                    mostrarMovimentos(c);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    Caixa c = getTableRow().getItem();
                    btnFechar.setDisable(c.getStatus() == StatusCaixa.FECHADO || !sessionManager.hasAccess("CAIXA", "Editar"));
                    setGraphic(box);
                }
            }
        });

        table.getColumns().addAll(colId, colUsuario, colAbertura, colFecho, colSaldoInicial, colStatus, colAcoes);

        table.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n != null) {
                mostrarMovimentos(n);
            } else {
                movimentosTable.setItems(FXCollections.observableArrayList());
            }
            updateKpis(n);
        });

        VBox.setVgrow(table, Priority.ALWAYS);
        getChildren().add(table);
    }

    private TableView<MovimentoCaixa> movimentosTable;

    private void setupMovimentosTable() {
        TitledPane pane = new TitledPane("Movimentos do Caixa Selecionado", new VBox());
        pane.setExpanded(false);

        movimentosTable = new TableView<>();
        movimentosTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        movimentosTable.setPrefHeight(200);

        TableColumn<MovimentoCaixa, String> colData = new TableColumn<>("Data/Hora");
        colData.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                } else {
                    MovimentoCaixa m = getTableRow().getItem();
                    setText(m.getDataHora() != null ? m.getDataHora().format(df) : "");
                }
            }
        });
        colData.setMinWidth(140);

        TableColumn<MovimentoCaixa, String> colTipo = new TableColumn<>("Tipo");
        colTipo.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("tipo"));
        colTipo.setMinWidth(100);

        TableColumn<MovimentoCaixa, String> colDescricao = new TableColumn<>("Descrição");
        colDescricao.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("descricao"));
        colDescricao.setMinWidth(200);

        TableColumn<MovimentoCaixa, String> colMetodo = new TableColumn<>("Método");
        colMetodo.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("metodoPagamento"));
        colMetodo.setMinWidth(100);

        TableColumn<MovimentoCaixa, String> colValor = new TableColumn<>("Valor");
        colValor.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                } else {
                    MovimentoCaixa m = getTableRow().getItem();
                    setText(m.getValor() != null ? String.format("Kz %.2f", m.getValor()) : "");
                }
            }
        });
        colValor.setMinWidth(120);

        movimentosTable.getColumns().addAll(colData, colTipo, colDescricao, colMetodo, colValor);

        pane.setContent(movimentosTable);
        getChildren().add(pane);
    }

    private void loadData() {
        masterData.setAll(caixaService.findAll());

        if (filtered == null) {
            filtered = new FilteredList<>(masterData, c -> true);
            sorted = new SortedList<>(filtered);
            sorted.comparatorProperty().bind(table.comparatorProperty());
            table.setItems(sorted);
        }

        applyFilters();

        Caixa selected = table.getSelectionModel().getSelectedItem();
        if (selected == null && !table.getItems().isEmpty()) {
            table.getSelectionModel().select(0);
            selected = table.getSelectionModel().getSelectedItem();
        }
        updateKpis(selected);
    }

    private void abrirCaixa() {
        TextField txtSaldo = new TextField("0.00");
        txtSaldo.setPromptText("Saldo inicial");

        VBox content = new VBox(10,
            new Label("Saldo Inicial:"),
            txtSaldo,
            new Label("Informe o valor em caixa no momento da abertura.")
        );
        content.setPadding(new Insets(10));

        modalService.create()
            .title("Abrir Caixa")
            .content(content)
            .autoSize()
            .withConfirmButton("Abrir", () -> {
                try {
                    BigDecimal saldo = new BigDecimal(txtSaldo.getText().replace(",", "."));
                    caixaService.abrirCaixa(saldo);
                    loadData();
                    return true;
                } catch (Exception ex) {
                    AlertUtils.showExceptionAlert("Erro", "Não foi possível abrir o caixa.", ex);
                    return false;
                }
            })
            .withCancelButton("Cancelar")
            .buildAndShow();
    }

    private void fecharCaixa(Caixa caixa) {
        if (caixa.getStatus() == StatusCaixa.FECHADO) {
            AlertUtils.showWarningAlert("Aviso", "Este caixa já está fechado.");
            return;
        }

        // Calcular saldo esperado
        java.math.BigDecimal totalDinheiro = java.math.BigDecimal.ZERO;
        var movimentos = caixaService.getMovimentos(caixa.getId());
        for (var m : movimentos) {
            if (m.getTipo() == ao.allon.kubata.faturacao.domain.enums.TipoMovimento.VENDA ||
                m.getTipo() == ao.allon.kubata.faturacao.domain.enums.TipoMovimento.ABERTURA ||
                m.getTipo() == ao.allon.kubata.faturacao.domain.enums.TipoMovimento.SUPRIMENTO) {
                totalDinheiro = totalDinheiro.add(m.getValor());
            } else if (m.getTipo() == ao.allon.kubata.faturacao.domain.enums.TipoMovimento.SANGRIA ||
                       m.getTipo() == ao.allon.kubata.faturacao.domain.enums.TipoMovimento.ESTORNO) {
                totalDinheiro = totalDinheiro.subtract(m.getValor());
            }
        }

        TextField txtSaldo = new TextField(totalDinheiro.toString());
        txtSaldo.setPromptText("Saldo informado");

        TextArea txtObs = new TextArea();
        txtObs.setPromptText("Observações (opcional)");
        txtObs.setPrefRowCount(2);

        VBox content = new VBox(10,
            new Label("Saldo Esperado: Kz " + String.format("%.2f", totalDinheiro)),
            new Label("Saldo Informado:"),
            txtSaldo,
            new Label("Observações:"),
            txtObs
        );
        content.setPadding(new Insets(10));

        modalService.create()
            .title("Fechar Caixa")
            .content(content)
            .autoSize()
            .withConfirmButton("Fechar", () -> {
                try {
                    BigDecimal saldo = new BigDecimal(txtSaldo.getText().replace(",", "."));
                    caixaService.fecharCaixa(caixa.getId(), saldo, txtObs.getText());
                    loadData();
                    return true;
                } catch (Exception ex) {
                    AlertUtils.showExceptionAlert("Erro", "Não foi possível fechar o caixa.", ex);
                    return false;
                }
            })
            .withCancelButton("Cancelar")
            .buildAndShow();
    }

    private void mostrarMovimentos(Caixa caixa) {
        var movimentos = caixaService.getMovimentos(caixa.getId());
        movimentosTable.setItems(FXCollections.observableArrayList(movimentos));
    }

    private void applyFilters() {
        if (filtered == null) {
            return;
        }

        String query = txtSearch != null && txtSearch.getText() != null
            ? txtSearch.getText().trim().toLowerCase()
            : "";

        StatusCaixa status = cbStatus != null ? cbStatus.getValue() : null;

        filtered.setPredicate(c -> {
            if (c == null) {
                return false;
            }

            if (status != null && c.getStatus() != status) {
                return false;
            }

            if (query.isBlank()) {
                return true;
            }

            String id = c.getId() != null ? String.valueOf(c.getId()) : "";
            String usuario = c.getUsuario() != null ? c.getUsuario().toLowerCase() : "";

            return id.contains(query) || usuario.contains(query);
        });
    }

    private void updateKpis(Caixa caixa) {
        if (lblCaixaAberto == null) {
            return;
        }

        if (caixa == null) {
            lblCaixaAberto.setText("-");
            lblSaldoInicial.setText("Kz 0,00");
            lblSaldoAtual.setText("Kz 0,00");
            lblTotalMovimentos.setText("0");
            return;
        }

        lblCaixaAberto.setText(caixa.getStatus() == StatusCaixa.ABERTO ? "ABERTO" : "FECHADO");
        lblSaldoInicial.setText(fmtKz(caixa.getSaldoInicial()));

        BigDecimal saldoAtual = computeSaldoAtual(caixa);
        lblSaldoAtual.setText(fmtKz(saldoAtual));

        var movimentos = caixaService.getMovimentos(caixa.getId());
        lblTotalMovimentos.setText(movimentos != null ? String.valueOf(movimentos.size()) : "0");
    }
}

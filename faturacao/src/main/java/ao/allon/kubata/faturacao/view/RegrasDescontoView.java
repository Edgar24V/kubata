package ao.allon.kubata.faturacao.view;

import ao.allon.kubata.faturacao.domain.*;
import ao.allon.kubata.faturacao.service.*;
import atlantafx.base.theme.Styles;
import ao.allon.kubata.faturacao.ui.modal.ModalService;
import ao.allon.kubata.faturacao.ui.util.AlertUtils;
import ao.allon.kubata.faturacao.ui.util.IconUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.util.StringConverter;
import org.kordamp.ikonli.feather.Feather;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class RegrasDescontoView extends BorderPane {

    private final RegraDescontoService regraService;
    private final CategoriaService categoriaService;
    private final ProdutoService produtoService;
    private final ImpostoService impostoService;
    private final ModalService modalService;
    private final AuditLogService auditLogService;

    private final TableView<RegraDesconto> tabela = new TableView<>();
    private final ObservableList<RegraDesconto> dados = FXCollections.observableArrayList();
    private FilteredList<RegraDesconto> filtrado;
    private SortedList<RegraDesconto> ordenado;

    private final ComboBox<EscopoRegra> filtroEscopo = new ComboBox<>();
    private final ComboBox<RegraStatus> filtroStatus = new ComboBox<>();
    private final TextField txtBuscar = new TextField();

    public RegrasDescontoView(RegraDescontoService regraService,
                              CategoriaService categoriaService,
                              ProdutoService produtoService,
                              ImpostoService impostoService,
                              ModalService modalService,
                              AuditLogService auditLogService) {
        this.regraService = regraService;
        this.categoriaService = categoriaService;
        this.produtoService = produtoService;
        this.impostoService = impostoService;
        this.modalService = modalService;
        this.auditLogService = auditLogService;

        setPadding(new Insets(12));
        setStyle("-fx-background-color: -color-bg-default;");

        Label title = new Label("Regras de Desconto");
        title.getStyleClass().add(Styles.TITLE_3);
        Label subtitle = new Label("Defina tetos de desconto por escopo, com vigência, prioridade e aprovação.");
        subtitle.getStyleClass().addAll(Styles.TEXT_SMALL, Styles.TEXT_MUTED);

        HBox toolbar = criarToolbar();
        VBox top = new VBox(6, title, subtitle, toolbar, new Separator());
        top.setPadding(new Insets(0, 0, 6, 0));
        setTop(top);

        configurarTabela();
        setCenter(tabela);

        HBox acoes = new HBox(10);
        acoes.setAlignment(Pos.CENTER_LEFT);
        Button btnNovo = new Button("Nova Regra", IconUtils.icon(Feather.FILE_PLUS, IconUtils.SIZE_SMALL));
        btnNovo.getStyleClass().add(Styles.SUCCESS);
        btnNovo.setOnAction(e -> abrirEditor(null));
        Button btnEditar = new Button("Editar", IconUtils.icon(Feather.EDIT_2, IconUtils.SIZE_SMALL));
        btnEditar.setOnAction(e -> abrirEditor(tabela.getSelectionModel().getSelectedItem()));
        Button btnAprovar = new Button("Aprovar", IconUtils.icon(Feather.CHECK, IconUtils.SIZE_SMALL));
        btnAprovar.getStyleClass().add(Styles.ACCENT);
        btnAprovar.setOnAction(e -> aprovarSelecionada());
        Button btnApagar = new Button("Apagar", IconUtils.icon(Feather.TRASH_2, IconUtils.SIZE_SMALL));
        btnApagar.getStyleClass().add(Styles.DANGER);
        btnApagar.setOnAction(e -> apagarSelecionado());
        Button btnRefresh = new Button("", IconUtils.icon(Feather.REFRESH_CW, IconUtils.SIZE_SMALL));
        btnRefresh.getStyleClass().add(Styles.BUTTON_ICON);
        btnRefresh.setOnAction(e -> refreshData());
        acoes.getChildren().addAll(btnNovo, btnEditar, btnAprovar, btnApagar, btnRefresh);
        setBottom(new VBox(new Separator(), new HBox(acoes)));

        tabela.getStyleClass().add(Styles.DENSE);
        tabela.setPlaceholder(new Label("Sem regras encontradas"));

        filtrado = new FilteredList<>(dados, r -> true);
        ordenado = new SortedList<>(filtrado);
        ordenado.comparatorProperty().bind(tabela.comparatorProperty());
        tabela.setItems(ordenado);

        refreshData();
        aplicarFiltros();
    }

    private HBox criarToolbar() {
        HBox toolbar = new HBox(8);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        filtroEscopo.getItems().add(null);
        filtroEscopo.getItems().addAll(EscopoRegra.values());
        filtroEscopo.setPromptText("Escopo");
        filtroEscopo.setMaxWidth(150);
        filtroEscopo.valueProperty().addListener((o, ov, nv) -> aplicarFiltros());

        filtroStatus.getItems().add(null);
        filtroStatus.getItems().addAll(RegraStatus.values());
        filtroStatus.setPromptText("Status");
        filtroStatus.setMaxWidth(150);
        filtroStatus.valueProperty().addListener((o, ov, nv) -> aplicarFiltros());

        txtBuscar.setPromptText("Pesquisar por motivo, produto, categoria, imposto…");
        txtBuscar.setPrefWidth(360);
        txtBuscar.textProperty().addListener((o, ov, nv) -> aplicarFiltros());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button limpar = new Button("Limpar Filtros", IconUtils.icon(Feather.X_CIRCLE, IconUtils.SIZE_SMALL));
        limpar.getStyleClass().add(Styles.BUTTON_OUTLINED);
        limpar.setOnAction(e -> {
            filtroEscopo.setValue(null);
            filtroStatus.setValue(null);
            txtBuscar.clear();
        });
        toolbar.getChildren().addAll(
                new Label(null, IconUtils.icon(Feather.FILTER, IconUtils.SIZE_SMALL)),
                filtroEscopo, filtroStatus, txtBuscar, spacer, limpar
        );
        return toolbar;
    }

    public void refreshData() {
        dados.setAll(regraService.findAll());
        aplicarFiltros();
    }

    private void configurarTabela() {
        tabela.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        TableColumn<RegraDesconto, EscopoRegra> colEscopo = new TableColumn<>("Escopo");
        colEscopo.setCellValueFactory(new PropertyValueFactory<>("escopo"));

        TableColumn<RegraDesconto, BigDecimal> colMax = new TableColumn<>("Máx. %");
        colMax.setCellValueFactory(new PropertyValueFactory<>("maxPercent"));
        colMax.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.setScale(2, java.math.RoundingMode.HALF_UP) + " %");
            }
        });

        TableColumn<RegraDesconto, Integer> colPrio = new TableColumn<>("Prioridade");
        colPrio.setCellValueFactory(new PropertyValueFactory<>("prioridade"));

        TableColumn<RegraDesconto, LocalDate> colIni = new TableColumn<>("Início");
        colIni.setCellValueFactory(new PropertyValueFactory<>("inicio"));
        colIni.setCellFactory(tc -> new TableCell<>() {
            final DateTimeFormatter f = DateTimeFormatter.ofPattern("dd/MM/yyyy", new Locale("pt", "AO"));
            @Override protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : f.format(item));
            }
        });

        TableColumn<RegraDesconto, LocalDate> colFim = new TableColumn<>("Fim");
        colFim.setCellValueFactory(new PropertyValueFactory<>("fim"));
        colFim.setCellFactory(tc -> new TableCell<>() {
            final DateTimeFormatter f = DateTimeFormatter.ofPattern("dd/MM/yyyy", new Locale("pt", "AO"));
            @Override protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : f.format(item));
            }
        });

        TableColumn<RegraDesconto, Boolean> colApr = new TableColumn<>("Requer Aprovação");
        colApr.setCellValueFactory(new PropertyValueFactory<>("requerAprovacao"));
        colApr.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : (item ? "Sim" : "Não"));
            }
        });

        TableColumn<RegraDesconto, String> colAlvo = new TableColumn<>("Alvo");
        colAlvo.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(formatAlvo(cd.getValue())));

        TableColumn<RegraDesconto, RegraStatus> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colStatus.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(RegraStatus item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label chip = new Label(item.name().replace('_', ' '));
                    chip.getStyleClass().clear();
                    chip.getStyleClass().addAll(Styles.TEXT_SMALL);
                    chip.setStyle("-fx-padding: 2 8; -fx-background-radius: 999;");
                    switch (item) {
                        case ATIVA -> chip.setStyle(chip.getStyle() + " -fx-background-color: -color-success-emphasis; -fx-text-fill: -color-fg-default;");
                        case PENDENTE_APROVACAO -> chip.setStyle(chip.getStyle() + " -fx-background-color: -color-warning-emphasis; -fx-text-fill: -color-fg-default;");
                        case EXPIRADA -> chip.setStyle(chip.getStyle() + " -fx-background-color: -color-danger-emphasis; -fx-text-fill: -color-fg-default;");
                        default -> chip.setStyle(chip.getStyle() + " -fx-background-color: -color-accent-emphasis; -fx-text-fill: -color-fg-default;");
                    }
                    setGraphic(chip);
                    setText(null);
                }
            }
        });

        TableColumn<RegraDesconto, String> colMotivo = new TableColumn<>("Motivo");
        colMotivo.setCellValueFactory(new PropertyValueFactory<>("motivo"));

        tabela.getColumns().addAll(colEscopo, colAlvo, colMax, colPrio, colIni, colFim, colStatus, colApr, colMotivo);

        tabela.setRowFactory(tv -> {
            TableRow<RegraDesconto> row = new TableRow<>();
            ContextMenu menu = new ContextMenu();
            MenuItem editar = new MenuItem("Editar", IconUtils.icon(Feather.EDIT_2, IconUtils.SIZE_SMALL));
            editar.setOnAction(e -> abrirEditor(row.getItem()));
            MenuItem aprovar = new MenuItem("Aprovar", IconUtils.icon(Feather.CHECK, IconUtils.SIZE_SMALL));
            aprovar.setOnAction(e -> {
                tabela.getSelectionModel().select(row.getItem());
                aprovarSelecionada();
            });
            MenuItem apagar = new MenuItem("Apagar", IconUtils.icon(Feather.TRASH_2, IconUtils.SIZE_SMALL));
            apagar.setOnAction(e -> {
                tabela.getSelectionModel().select(row.getItem());
                apagarSelecionado();
            });
            menu.getItems().addAll(editar, aprovar, apagar);
            row.contextMenuProperty().bind(javafx.beans.binding.Bindings
                    .when(row.emptyProperty()).then((ContextMenu) null).otherwise(menu));
            return row;
        });
    }

    private void abrirEditor(RegraDesconto regra) {
        boolean novo = regra == null;
        RegraDesconto regraRef = novo ? new RegraDesconto() : regra;

        ComboBox<EscopoRegra> cbEscopo = new ComboBox<>(FXCollections.observableArrayList(EscopoRegra.values()));
        cbEscopo.setMaxWidth(Double.MAX_VALUE);

        TextField txtMax = new TextField();
        txtMax.setPromptText("0.00");
        txtMax.setTextFormatter(new TextFormatter<>(c -> c.getControlNewText().matches("\\d{0,3}([\\.,]\\d{0,2})?") ? c : null));

        TextField txtPrio = new TextField("0");
        txtPrio.setTextFormatter(new TextFormatter<>(c -> c.getControlNewText().matches("\\d{0,3}") ? c : null));
        DatePicker dpIni = new DatePicker();
        DatePicker dpFim = new DatePicker();
        CheckBox chkApr = new CheckBox("Requer Aprovação");
        ComboBox<RegraStatus> cbStatus = new ComboBox<>(FXCollections.observableArrayList(RegraStatus.values()));
        cbStatus.setPromptText("Status");
        TextArea txtMotivo = new TextArea();
        txtMotivo.setPromptText("Motivo / Observações");
        txtMotivo.setPrefRowCount(2);

        ComboBox<Categoria> cbCat = new ComboBox<>(FXCollections.observableArrayList(categoriaService.findAll()));
        cbCat.setConverter(new StringConverter<>() {
            @Override public String toString(Categoria c) { return c == null ? "" : c.getNome(); }
            @Override public Categoria fromString(String s) { return null; }
        });
        ComboBox<Produto> cbProd = new ComboBox<>(FXCollections.observableArrayList(produtoService.findAll()));
        cbProd.setConverter(new StringConverter<>() {
            @Override public String toString(Produto p) { return p == null ? "" : p.getNome(); }
            @Override public Produto fromString(String s) { return null; }
        });
        ComboBox<Imposto> cbImp = new ComboBox<>(FXCollections.observableArrayList(impostoService.findAll()));
        cbImp.setConverter(new StringConverter<>() {
            @Override public String toString(Imposto i) { return i == null ? "" : i.getCodigo() + " (" + i.getPercentual() + "%)"; }
            @Override public Imposto fromString(String s) { return null; }
        });

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(10); grid.setPadding(new Insets(10));
        ColumnConstraints c1 = new ColumnConstraints(); c1.setPercentWidth(30);
        ColumnConstraints c2 = new ColumnConstraints(); c2.setPercentWidth(70);
        grid.getColumnConstraints().addAll(c1, c2);

        grid.addRow(0, new Label("Escopo*"), cbEscopo);
        grid.addRow(1, new Label("Máximo (%) *"), txtMax);
        grid.addRow(2, new Label("Prioridade"), txtPrio);
        grid.addRow(3, new Label("Início"), dpIni);
        grid.addRow(4, new Label("Fim"), dpFim);
        grid.addRow(5, new Label("Categoria"), cbCat);
        grid.addRow(6, new Label("Produto"), cbProd);
        grid.addRow(7, new Label("Imposto"), cbImp);
        grid.addRow(8, new Label("Status"), cbStatus);
        grid.addRow(9, chkApr, txtMotivo);

        if (!novo) {
            cbEscopo.setValue(regraRef.getEscopo());
            txtMax.setText(regraRef.getMaxPercent() != null ? regraRef.getMaxPercent().toString() : "");
            txtPrio.setText(regraRef.getPrioridade() != null ? regraRef.getPrioridade().toString() : "0");
            dpIni.setValue(regraRef.getInicio());
            dpFim.setValue(regraRef.getFim());
            chkApr.setSelected(Boolean.TRUE.equals(regraRef.getRequerAprovacao()));
            cbStatus.setValue(regraRef.getStatus());
            txtMotivo.setText(regraRef.getMotivo() != null ? regraRef.getMotivo() : "");
            cbCat.setValue(regraRef.getCategoria());
            cbProd.setValue(regraRef.getProduto());
            cbImp.setValue(regraRef.getImposto());
        }

        cbEscopo.valueProperty().addListener((o, ov, nv) -> {
            boolean cat = nv == EscopoRegra.CATEGORIA;
            boolean prod = nv == EscopoRegra.PRODUTO;
            boolean imp = nv == EscopoRegra.IMPOSTO;
            cbCat.setDisable(!cat);
            cbProd.setDisable(!prod);
            cbImp.setDisable(!imp);
        });
        cbEscopo.getSelectionModel().select(regraRef.getEscopo());

        modalService.create()
                .title(novo ? "Nova Regra de Desconto" : "Editar Regra de Desconto")
                .content(grid)
                .dynamicSize()
                .withConfirmButton("Guardar", () -> {
                    try {
                        if (cbEscopo.getValue() == null) {
                            ao.allon.kubata.faturacao.ui.util.AlertUtils.showWarningAlert("Validação", "Selecione o escopo.");
                            return false;
                        }
                        // Validações por escopo
                        switch (cbEscopo.getValue()) {
                            case PRODUTO -> {
                                if (cbProd.getValue() == null) {
                                    AlertUtils.showWarningAlert("Validação", "Selecione o produto para o escopo PRODUTO.");
                                    return false;
                                }
                            }
                            case CATEGORIA -> {
                                if (cbCat.getValue() == null) {
                                    AlertUtils.showWarningAlert("Validação", "Selecione a categoria para o escopo CATEGORIA.");
                                    return false;
                                }
                            }
                            case IMPOSTO -> {
                                if (cbImp.getValue() == null) {
                                    AlertUtils.showWarningAlert("Validação", "Selecione o imposto para o escopo IMPOSTO.");
                                    return false;
                                }
                            }
                            default -> { /* GLOBAL e SERVICO não exigem alvo explícito */ }
                        }
                        BigDecimal max = new BigDecimal(txtMax.getText().replace(",", "."));
                        regraRef.setEscopo(cbEscopo.getValue());
                        regraRef.setMaxPercent(max);
                        regraRef.setPrioridade(Integer.parseInt(txtPrio.getText()));
                        regraRef.setInicio(dpIni.getValue());
                        regraRef.setFim(dpFim.getValue());
                        regraRef.setRequerAprovacao(chkApr.isSelected());
                        // Fluxo de status com aprovação
                        RegraStatus statusEscolhido = cbStatus.getValue();
                        if (chkApr.isSelected()) {
                            if (statusEscolhido == RegraStatus.ATIVA) {
                                AlertUtils.showWarningAlert("Aprovação", "Regras que requerem aprovação não podem ser ATIVAS. Marcada como PENDENTE_APROVACAO.");
                                statusEscolhido = RegraStatus.PENDENTE_APROVACAO;
                            }
                            if (statusEscolhido == null) statusEscolhido = RegraStatus.PENDENTE_APROVACAO;
                        } else {
                            if (statusEscolhido == null || statusEscolhido == RegraStatus.PENDENTE_APROVACAO) {
                                statusEscolhido = RegraStatus.ATIVA;
                            }
                        }
                        regraRef.setStatus(statusEscolhido);
                        regraRef.setMotivo(txtMotivo.getText());
                        regraRef.setCategoria(cbCat.getValue());
                        regraRef.setProduto(cbProd.getValue());
                        regraRef.setImposto(cbImp.getValue());

                        regraService.save(regraRef);
                        auditLogService.log("DISCOUNT_RULE_SAVE", regraRef.getEscopo() + " max=" + regraRef.getMaxPercent() + " prio=" + regraRef.getPrioridade());
                        refreshData();
                        return true;
                    } catch (Exception ex) {
                        ao.allon.kubata.faturacao.ui.util.AlertUtils.showExceptionAlert("Erro", "Falha ao guardar a regra.", ex);
                        return false;
                    }
                })
                .withCancelButton("Cancelar")
                .buildAndShow();
    }

    private void apagarSelecionado() {
        RegraDesconto r = tabela.getSelectionModel().getSelectedItem();
        if (r == null) return;
        if (!AlertUtils.showConfirmationAlert("Apagar Regra", "Deseja mesmo remover esta regra?")) return;
        try {
            regraService.delete(r.getId());
            auditLogService.log("DISCOUNT_RULE_DELETE", "id=" + r.getId());
            refreshData();
        } catch (Exception ex) {
            ao.allon.kubata.faturacao.ui.util.AlertUtils.showExceptionAlert("Erro", "Falha ao apagar a regra.", ex);
        }
    }

    private void aprovarSelecionada() {
        RegraDesconto r = tabela.getSelectionModel().getSelectedItem();
        if (r == null) return;
        if (!Boolean.TRUE.equals(r.getRequerAprovacao())) {
            AlertUtils.showWarningAlert("Aprovação", "Esta regra não requer aprovação.");
            return;
        }
        if (r.getStatus() != RegraStatus.PENDENTE_APROVACAO) {
            AlertUtils.showWarningAlert("Aprovação", "Apenas regras pendentes podem ser aprovadas.");
            return;
        }
        if (!AlertUtils.showConfirmationAlert("Aprovar Regra", "Confirma a aprovação desta regra?")) return;
        try {
            r.setStatus(RegraStatus.ATIVA);
            regraService.save(r);
            auditLogService.log("DISCOUNT_RULE_APPROVE", "id=" + r.getId());
            refreshData();
        } catch (Exception ex) {
            AlertUtils.showExceptionAlert("Erro", "Falha ao aprovar a regra.", ex);
        }
    }

    private void aplicarFiltros() {
        filtrado.setPredicate(r -> {
            if (r == null) return false;
            if (filtroEscopo.getValue() != null && r.getEscopo() != filtroEscopo.getValue()) return false;
            if (filtroStatus.getValue() != null && r.getStatus() != filtroStatus.getValue()) return false;
            String q = txtBuscar.getText();
            if (q != null && !q.isBlank()) {
                String s = q.toLowerCase(Locale.ROOT);
                String alvo = formatAlvo(r).toLowerCase(Locale.ROOT);
                String motivo = r.getMotivo() != null ? r.getMotivo().toLowerCase(Locale.ROOT) : "";
                return alvo.contains(s) || motivo.contains(s);
            }
            return true;
        });
    }

    private String formatAlvo(RegraDesconto r) {
        if (r == null) return "";
        return switch (r.getEscopo()) {
            case PRODUTO -> r.getProduto() != null ? r.getProduto().getNome() : "Produto";
            case CATEGORIA -> r.getCategoria() != null ? r.getCategoria().getNome() : "Categoria";
            case IMPOSTO -> r.getImposto() != null ? r.getImposto().getCodigo() + " (" + r.getImposto().getPercentual() + "%)" : "Imposto";
            case SERVICO -> "Serviço";
            default -> "Global";
        };
    }
}

package ao.allon.kubata.faturacao.view;

import ao.allon.kubata.faturacao.domain.RetencaoFonte;
import ao.allon.kubata.faturacao.service.RetencaoFonteService;
import ao.allon.kubata.faturacao.ui.util.AlertUtils;
import ao.allon.kubata.faturacao.ui.util.IconUtils;
import atlantafx.base.controls.Card;
import atlantafx.base.controls.CustomTextField;
import atlantafx.base.theme.Styles;
import com.dlsc.gemsfx.FilterView;
import com.dlsc.gemsfx.SearchTextField;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.stream.Collectors;

/**
 * View moderna para gestão de Retenção na Fonte com AtlantaFX e GemsFX.
 * Inclui KPIs, filtros avançados, busca inteligente e interface responsiva.
 */
public class RetencaoFonteView extends BorderPane {

    private final RetencaoFonteService service;
    private final ObservableList<RetencaoFonte> itens = FXCollections.observableArrayList();
    private final ObservableList<RetencaoFonte> itensFiltrados = FXCollections.observableArrayList();

    // KPI Labels
    private Label lblTotalRetencoes;
    private Label lblTaxaMaxima;
    private Label lblTaxaMedia;
    private Label lblTotalRendimentos;

    // Components
    private TableView<RetencaoFonte> tabela;
    private SearchTextField txtBusca;
    private CustomTextField txtCodigo;
    private CustomTextField txtDescricao;
    private CustomTextField txtTaxa;
    private ComboBox<String> cmbTipo;
    private Button btnSalvar;
    private Button btnExcluir;
    private Button btnNovo;
    private Button btnLimpar;

    private RetencaoFonte selecionado;

    public RetencaoFonteView(RetencaoFonteService service) {
        this.service = service;
        setPadding(new Insets(20));
        setStyle("-fx-background-color: -color-bg-default;");
        buildUI();
        loadData();
    }

    private void buildUI() {
        setTop(createHeader());
        setCenter(createMainContent());
    }

    private VBox createHeader() {
        VBox header = new VBox(15);
        header.setPadding(new Insets(0, 0, 15, 0));

        // Title section
        HBox titleBox = new HBox(15);
        titleBox.setAlignment(Pos.CENTER_LEFT);

        FontIcon icon = IconUtils.icon(Feather.DOLLAR_SIGN, 32);
        icon.setStyle("-fx-icon-color: -color-accent-emphasis;");

        VBox titles = new VBox(5);
        Label title = new Label("Retenção na Fonte");
        title.getStyleClass().add(Styles.TITLE_2);
        Label subtitle = new Label("Gerencie as taxas de retenção para diferentes tipos de rendimento");
        subtitle.getStyleClass().add(Styles.TEXT_MUTED);
        titles.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        btnNovo = new Button("Nova Retenção", IconUtils.icon(Feather.PLUS, IconUtils.SIZE_SMALL));
        btnNovo.getStyleClass().addAll(Styles.BUTTON_OUTLINED, Styles.ACCENT);
        btnNovo.setOnAction(e -> novoRegistro());

        titleBox.getChildren().addAll(icon, titles, spacer, btnNovo);

        // KPI Cards
        HBox kpiBox = createKPICards();

        header.getChildren().addAll(titleBox, kpiBox);
        return header;
    }

    private HBox createKPICards() {
        HBox box = new HBox(15);
        box.setPadding(new Insets(10, 0, 0, 0));

        // Card 1: Total Retenções
        VBox cardTotal = createKpiCard(Feather.LAYERS, "Total Retenções", "0", Styles.ACCENT);
        lblTotalRetencoes = (Label) cardTotal.getChildren().get(1);

        // Card 2: Taxa Máxima
        VBox cardMax = createKpiCard(Feather.TRENDING_UP, "Taxa Máxima", "0%", Styles.DANGER);
        lblTaxaMaxima = (Label) cardMax.getChildren().get(1);

        // Card 3: Taxa Média
        VBox cardMedia = createKpiCard(Feather.ACTIVITY, "Taxa Média", "0%", Styles.WARNING);
        lblTaxaMedia = (Label) cardMedia.getChildren().get(1);

        // Card 4: Total Tipos
        VBox cardTipos = createKpiCard(Feather.GRID, "Tipos Ativos", "0", Styles.SUCCESS);
        lblTotalRendimentos = (Label) cardTipos.getChildren().get(1);

        box.getChildren().addAll(cardTotal, cardMax, cardMedia, cardTipos);
        HBox.setHgrow(cardTotal, Priority.ALWAYS);
        HBox.setHgrow(cardMax, Priority.ALWAYS);
        HBox.setHgrow(cardMedia, Priority.ALWAYS);
        HBox.setHgrow(cardTipos, Priority.ALWAYS);

        return box;
    }

    private VBox createKpiCard(Feather icon, String titulo, String valor, String style) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: -color-bg-overlay; -fx-background-radius: 8; " +
                      "-fx-border-color: -color-border-muted; -fx-border-radius: 8;");

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        FontIcon ic = IconUtils.icon(icon, 20);
        ic.getStyleClass().add(style);

        Label lblTitulo = new Label(titulo);
        lblTitulo.getStyleClass().addAll(Styles.TEXT_SMALL, Styles.TEXT_MUTED);

        header.getChildren().addAll(ic, lblTitulo);

        Label lblValor = new Label(valor);
        lblValor.getStyleClass().add(Styles.TITLE_3);
        lblValor.setStyle("-fx-text-fill: -color-fg-default;");

        card.getChildren().addAll(header, lblValor);
        return card;
    }

    private VBox createMainContent() {
        VBox content = new VBox(15);
        VBox.setVgrow(content, Priority.ALWAYS);

        // Toolbar com busca e filtros
        HBox toolbar = createToolbar();
        
        // Split pane com formulário e tabela
        SplitPane splitPane = new SplitPane();
        splitPane.setDividerPositions(0.4);
        splitPane.getItems().addAll(createFormPanel(), createTablePanel());
        VBox.setVgrow(splitPane, Priority.ALWAYS);

        content.getChildren().addAll(toolbar, splitPane);
        return content;
    }

    private HBox createToolbar() {
        HBox toolbar = new HBox(10);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(5, 0, 5, 0));

        txtBusca = new SearchTextField();
        txtBusca.setPromptText("Buscar por código, descrição ou tipo...");
        txtBusca.setPrefWidth(350);
        txtBusca.textProperty().addListener((obs, old, newVal) -> applyFilters());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Filter buttons
        Button badgeTodos = new Button("Todos");
        badgeTodos.getStyleClass().addAll(Styles.BUTTON_OUTLINED, Styles.ACCENT);
        badgeTodos.setOnAction(e -> applyTipoFilter(null));
        
        Button badgeTrabalho = new Button("Trabalho");
        badgeTrabalho.getStyleClass().addAll(Styles.BUTTON_OUTLINED, Styles.SUCCESS);
        badgeTrabalho.setOnAction(e -> applyTipoFilter("Trabalho"));
        
        Button badgeServicos = new Button("Serviços");
        badgeServicos.getStyleClass().addAll(Styles.BUTTON_OUTLINED, Styles.WARNING);
        badgeServicos.setOnAction(e -> applyTipoFilter("Serviços"));
        
        Button badgeCapital = new Button("Capitais");
        badgeCapital.getStyleClass().addAll(Styles.BUTTON_OUTLINED, Styles.DANGER);
        badgeCapital.setOnAction(e -> applyTipoFilter("Capitais"));

        toolbar.getChildren().addAll(txtBusca, spacer, badgeTodos, badgeTrabalho, badgeServicos, badgeCapital);
        return toolbar;
    }

    private VBox createFormPanel() {
        Card card = new Card();
        
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));

        // Form fields
        txtCodigo = new CustomTextField();
        txtCodigo.setPromptText("Código único da retenção");
        txtCodigo.setLeft(IconUtils.icon(Feather.HASH, IconUtils.SIZE_SMALL));

        txtDescricao = new CustomTextField();
        txtDescricao.setPromptText("Descrição da retenção");
        txtDescricao.setLeft(IconUtils.icon(Feather.FILE_TEXT, IconUtils.SIZE_SMALL));

        txtTaxa = new CustomTextField();
        txtTaxa.setPromptText("Taxa percentual");
        txtTaxa.setLeft(IconUtils.icon(Feather.PERCENT, IconUtils.SIZE_SMALL));

        cmbTipo = new ComboBox<>();
        cmbTipo.setPromptText("Selecione o tipo de rendimento");
        cmbTipo.getItems().setAll(
                "Trabalho Dependente",
                "Trabalho Independente", 
                "Prestação de Serviços",
                "Royalties",
                "Capitais",
                "Predial"
        );

        // Masks and validation
        txtTaxa.textProperty().addListener((obs, o, n) -> {
            if (!n.matches("\\d*(\\.\\d*)?")) {
                txtTaxa.setText(n.replaceAll("[^\\d.]", ""));
            }
        });

        // Layout
        GridPane form = new GridPane();
        form.setHgap(15);
        form.setVgap(15);
        
        form.addRow(0, createFieldBox("Código *", txtCodigo));
        form.addRow(1, createFieldBox("Descrição *", txtDescricao));
        form.addRow(2, createFieldBox("Taxa (%) *", txtTaxa));
        form.addRow(3, createFieldBox("Tipo de Rendimento *", cmbTipo));

        // Actions
        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);
        
        btnLimpar = new Button("Limpar", IconUtils.icon(Feather.X, IconUtils.SIZE_SMALL));
        btnLimpar.getStyleClass().addAll(Styles.BUTTON_OUTLINED);
        btnLimpar.setOnAction(e -> limparFormulario());
        
        btnExcluir = new Button("Excluir", IconUtils.icon(Feather.TRASH_2, IconUtils.SIZE_SMALL));
        btnExcluir.getStyleClass().addAll(Styles.BUTTON_OUTLINED, Styles.DANGER);
        btnExcluir.setDisable(true);
        btnExcluir.setOnAction(e -> excluir());
        
        btnSalvar = new Button("Salvar", IconUtils.icon(Feather.SAVE, IconUtils.SIZE_SMALL));
        btnSalvar.getStyleClass().addAll(Styles.BUTTON_OUTLINED, Styles.SUCCESS);
        btnSalvar.setDefaultButton(true);
        btnSalvar.setOnAction(e -> salvar());
        
        actions.getChildren().addAll(btnLimpar, btnExcluir, btnSalvar);

        content.getChildren().addAll(form, actions);
        card.setBody(content);
        
        return new VBox(card);
    }

    private VBox createFieldBox(String label, Control field) {
        VBox box = new VBox(5);
        Label lbl = new Label(label);
        lbl.getStyleClass().addAll(Styles.TEXT_SMALL, Styles.TEXT_MUTED);
        box.getChildren().addAll(lbl, field);
        return box;
    }

    private VBox createTablePanel() {
        Card card = new Card();
        
        tabela = createTable();
        VBox.setVgrow(tabela, Priority.ALWAYS);
        
        card.setBody(tabela);
        return new VBox(card);
    }

    private void loadData() {
        itens.setAll(service.findActive());
        itensFiltrados.setAll(itens);
        atualizarKPIs();
        limparFormulario();
    }

    private void salvar() {
        if (!validar()) return;
        
        try {
            RetencaoFonte r = selecionado != null ? selecionado : new RetencaoFonte();
            r.setCodigo(txtCodigo.getText().trim());
            r.setDescricao(txtDescricao.getText().trim());
            r.setTaxa(new BigDecimal(txtTaxa.getText().trim()));
            r.setTipoRendimento(cmbTipo.getValue());
            
            service.save(r);
            AlertUtils.showInfoAlert("Sucesso", "Retenção salva com sucesso!");
            loadData();
        } catch (Exception e) {
            AlertUtils.showError("Erro ao salvar", e.getMessage());
        }
    }

    private void excluir() {
        if (selecionado == null) return;
        
        boolean confirmado = AlertUtils.showConfirmation("Confirmar Exclusão",
            "Deseja realmente excluir a retenção '" + selecionado.getCodigo() + "'?\n\nEsta ação não pode ser desfeita.");
        
        if (confirmado) {
            try {
                service.softDelete(selecionado.getId());
                AlertUtils.showInfoAlert("Sucesso", "Retenção excluída com sucesso!");
                loadData();
            } catch (Exception e) {
                AlertUtils.showError("Erro ao excluir", e.getMessage());
            }
        }
    }

    private boolean validar() {
        if (txtCodigo.getText().trim().isEmpty() || 
            txtDescricao.getText().trim().isEmpty() ||
            txtTaxa.getText().trim().isEmpty() || 
            cmbTipo.getValue() == null) {
            AlertUtils.showWarning("Campos obrigatórios", "Por favor, preencha todos os campos obrigatórios.");
            return false;
        }
        
        try {
            BigDecimal taxa = new BigDecimal(txtTaxa.getText().trim());
            if (taxa.compareTo(BigDecimal.ZERO) < 0 || taxa.compareTo(BigDecimal.valueOf(100)) > 0) {
                AlertUtils.showWarning("Taxa inválida", "A taxa deve estar entre 0 e 100%.");
                return false;
            }
        } catch (NumberFormatException e) {
            AlertUtils.showWarning("Taxa inválida", "Por favor, informe um valor numérico válido para a taxa.");
            return false;
        }
        
        return true;
    }

    private void preencher(RetencaoFonte r) {
        selecionado = r;
        txtCodigo.setText(r.getCodigo());
        txtDescricao.setText(r.getDescricao());
        txtTaxa.setText(r.getTaxa() != null ? r.getTaxa().toString() : "");
        cmbTipo.setValue(r.getTipoRendimento());
        btnExcluir.setDisable(false);
        btnSalvar.setText("Atualizar");
    }

    private void limpar() {
        selecionado = null;
        txtCodigo.clear();
        txtDescricao.clear();
        txtTaxa.clear();
        cmbTipo.getSelectionModel().clearSelection();
        tabela.getSelectionModel().clearSelection();
        btnExcluir.setDisable(true);
        btnSalvar.setText("Salvar");
        txtCodigo.requestFocus();
    }

    private void novoRegistro() {
        limpar();
        txtCodigo.requestFocus();
    }

    private void limparFormulario() {
        limpar();
    }

    private void preencherFormulario(RetencaoFonte r) {
        preencher(r);
    }

    private void confirmarExclusao(RetencaoFonte r) {
        selecionado = r;
        excluir();
    }

    private void applyFilters() {
        String filtro = txtBusca.getText().toLowerCase();
        itensFiltrados.setAll(
            itens.stream()
                 .filter(r ->
                     r.getCodigo().toLowerCase().contains(filtro) ||
                     r.getDescricao().toLowerCase().contains(filtro) ||
                     r.getTipoRendimento().toLowerCase().contains(filtro))
                 .collect(Collectors.toList())
        );
        atualizarKPIs();
    }

    private void applyTipoFilter(String tipo) {
        if (tipo == null) {
            itensFiltrados.setAll(itens);
        } else {
            itensFiltrados.setAll(
                itens.stream()
                     .filter(r -> r.getTipoRendimento().contains(tipo))
                     .collect(Collectors.toList())
            );
        }
        atualizarKPIs();
    }

    private void atualizarKPIs() {
        lblTotalRetencoes.setText(String.valueOf(itensFiltrados.size()));
        
        BigDecimal max = itensFiltrados.stream()
            .map(RetencaoFonte::getTaxa)
            .max(Comparator.naturalOrder())
            .orElse(BigDecimal.ZERO);
        lblTaxaMaxima.setText(max + "%");

        BigDecimal avg = itensFiltrados.stream()
            .map(RetencaoFonte::getTaxa)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(Math.max(itensFiltrados.size(), 1)), 2, BigDecimal.ROUND_HALF_UP);
        lblTaxaMedia.setText(avg + "%");

        long tipos = itensFiltrados.stream()
            .map(RetencaoFonte::getTipoRendimento)
            .distinct()
            .count();
        lblTotalRendimentos.setText(String.valueOf(tipos));
    }

    private TableView<RetencaoFonte> createTable() {
        TableView<RetencaoFonte> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_NEXT_COLUMN);
        table.setItems(itensFiltrados);
        table.setRowFactory(tv -> {
            TableRow<RetencaoFonte> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    preencherFormulario(row.getItem());
                }
            });
            return row;
        });

        TableColumn<RetencaoFonte, String> colCodigo = new TableColumn<>("Código");
        colCodigo.setCellValueFactory(new PropertyValueFactory<>("codigo"));
        colCodigo.setPrefWidth(100);

        TableColumn<RetencaoFonte, String> colDescricao = new TableColumn<>("Descrição");
        colDescricao.setCellValueFactory(new PropertyValueFactory<>("descricao"));
        colDescricao.setPrefWidth(200);

        TableColumn<RetencaoFonte, BigDecimal> colTaxa = new TableColumn<>("Taxa (%)");
        colTaxa.setCellValueFactory(new PropertyValueFactory<>("taxa"));
        colTaxa.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item + "%");
                    getStyleClass().removeAll(Styles.SUCCESS, Styles.WARNING, Styles.DANGER);
                    if (item.compareTo(BigDecimal.valueOf(15)) >= 0) {
                        getStyleClass().add(Styles.DANGER);
                    } else if (item.compareTo(BigDecimal.valueOf(10)) >= 0) {
                        getStyleClass().add(Styles.WARNING);
                    } else {
                        getStyleClass().add(Styles.SUCCESS);
                    }
                }
            }
        });
        colTaxa.setPrefWidth(100);

        TableColumn<RetencaoFonte, String> colTipo = new TableColumn<>("Tipo Rendimento");
        colTipo.setCellValueFactory(new PropertyValueFactory<>("tipoRendimento"));
        colTipo.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    Label badge = new Label(item);
                    badge.getStyleClass().addAll(Styles.BUTTON_OUTLINED, "badge-label");
                    badge.setStyle("-fx-padding: 4 12; -fx-background-radius: 4;");
                    
                    if (item.contains("Trabalho")) {
                        badge.getStyleClass().add(Styles.ACCENT);
                    } else if (item.contains("Serviços")) {
                        badge.getStyleClass().add(Styles.SUCCESS);
                    } else if (item.contains("Capital") || item.contains("Royalties")) {
                        badge.getStyleClass().add(Styles.WARNING);
                    } else {
                        badge.getStyleClass().add(Styles.DANGER);
                    }
                    setGraphic(badge);
                }
            }
        });
        colTipo.setPrefWidth(150);

        TableColumn<RetencaoFonte, Void> colActions = new TableColumn<>("Ações");
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit = new Button("", IconUtils.icon(Feather.EDIT, IconUtils.SIZE_SMALL));
            private final Button btnDelete = new Button("", IconUtils.icon(Feather.TRASH_2, IconUtils.SIZE_SMALL));
            {
                btnEdit.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.FLAT);
                btnEdit.setTooltip(new Tooltip("Editar"));
                btnEdit.setOnAction(e -> preencherFormulario(getTableView().getItems().get(getIndex())));
                btnDelete.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.FLAT, Styles.DANGER);
                btnDelete.setTooltip(new Tooltip("Excluir"));
                btnDelete.setOnAction(e -> confirmarExclusao(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox box = new HBox(5, btnEdit, btnDelete);
                    box.setAlignment(Pos.CENTER);
                    setGraphic(box);
                }
            }
        });
        colActions.setPrefWidth(80);

        table.getColumns().addAll(colCodigo, colDescricao, colTaxa, colTipo, colActions);
        return table;
    }

}
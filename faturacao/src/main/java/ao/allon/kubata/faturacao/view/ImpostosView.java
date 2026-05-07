package ao.allon.kubata.faturacao.view;

import ao.allon.kubata.core.ui.table.AdvancedTableView;
import ao.allon.kubata.core.ui.table.TableUtils;
import ao.allon.kubata.faturacao.domain.Imposto;
import ao.allon.kubata.faturacao.domain.MotivoIsencao;
import ao.allon.kubata.faturacao.service.ImpostoService;
import ao.allon.kubata.faturacao.service.MotivoIsencaoService;
import ao.allon.kubata.faturacao.ui.modal.ModalService;
import ao.allon.kubata.faturacao.ui.util.AlertUtils;
import ao.allon.kubata.faturacao.ui.util.IconUtils;
import ao.allon.kubata.faturacao.ui.util.TileFactory;
import atlantafx.base.theme.Styles;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.stream.Collectors;

/**
 * View moderna para gestão de Impostos (IVA, IS, II) com AtlantaFX e GemsFX.
 * Inclui KPIs, filtros avançados, busca e formulário em modal.
 */
public class ImpostosView extends BorderPane {

    private final ImpostoService impostoService;
    private final MotivoIsencaoService motivoService;
    private final ModalService modalService;

    private final ObservableList<Imposto> listaImpostos = FXCollections.observableArrayList();
    private final ObservableList<MotivoIsencao> listaMotivos = FXCollections.observableArrayList();

    // KPI Labels
    private Label lblTotalImpostos;
    private Label lblIvaAtivo;
    private Label lblIsencoes;
    private Label lblTaxaMaxima;

    // Table and Filter
    private AdvancedTableView<Imposto> tabela;
    private TextField txtBusca;

    public ImpostosView(ImpostoService impostoService, MotivoIsencaoService motivoService, ModalService modalService) {
        this.impostoService = impostoService;
        this.motivoService = motivoService;
        this.modalService = modalService;

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

        FontIcon icon = IconUtils.icon(Feather.PERCENT, 32);
        icon.setStyle("-fx-icon-color: -color-accent-emphasis;");

        VBox titles = new VBox(5);
        Label title = new Label("Configuração de Impostos");
        title.getStyleClass().add(Styles.TITLE_2);
        Label subtitle = new Label("Gerencie as taxas de IVA, IS, II e motivos de isenção fiscal");
        subtitle.getStyleClass().add(Styles.TEXT_MUTED);
        titles.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnNovo = new Button("Novo Imposto", IconUtils.icon(Feather.PLUS, IconUtils.SIZE_SMALL));
        btnNovo.getStyleClass().addAll(Styles.BUTTON_OUTLINED, Styles.ACCENT);
        btnNovo.setOnAction(e -> showImpostoDialog(null));

        titleBox.getChildren().addAll(icon, titles, spacer, btnNovo);

        // KPI Cards
        HBox kpiBox = createKPICards();

        header.getChildren().addAll(titleBox, kpiBox);
        return header;
    }

    private HBox createKPICards() {
        HBox box = new HBox(15);
        box.setPadding(new Insets(10, 0, 0, 0));

        // Card 1: Total Impostos
        VBox cardTotal = createKpiCard(Feather.LAYERS, "Total de Impostos", "0", Styles.ACCENT);
        lblTotalImpostos = (Label) cardTotal.getChildren().get(1);

        // Card 2: IVA Ativo
        VBox cardIva = createKpiCard(Feather.CHECK_CIRCLE, "IVA Ativo", "0", Styles.SUCCESS);
        lblIvaAtivo = (Label) cardIva.getChildren().get(1);

        // Card 3: Isenções
        VBox cardIsencoes = createKpiCard(Feather.INFO, "Com Isenção", "0", Styles.WARNING);
        lblIsencoes = (Label) cardIsencoes.getChildren().get(1);

        // Card 4: Taxa Máxima
        VBox cardMax = createKpiCard(Feather.TRENDING_UP, "Taxa Máxima", "0%", Styles.DANGER);
        lblTaxaMaxima = (Label) cardMax.getChildren().get(1);

        box.getChildren().addAll(cardTotal, cardIva, cardIsencoes, cardMax);
        HBox.setHgrow(cardTotal, Priority.ALWAYS);
        HBox.setHgrow(cardIva, Priority.ALWAYS);
        HBox.setHgrow(cardIsencoes, Priority.ALWAYS);
        HBox.setHgrow(cardMax, Priority.ALWAYS);

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

        HBox toolbar = createToolbar();
        tabela = createTable();
        VBox.setVgrow(tabela, Priority.ALWAYS);

        content.getChildren().addAll(toolbar, tabela);
        return content;
    }

    private HBox createToolbar() {
        HBox toolbar = new HBox(10);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(5, 0, 5, 0));

        txtBusca = new TextField();
        txtBusca.setPromptText("Buscar por código ou descrição...");
        txtBusca.setPrefWidth(300);
        txtBusca.textProperty().addListener((obs, old, newVal) -> applyFilters());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Filter buttons
        Button badgeTodos = new Button("Todos");
        badgeTodos.getStyleClass().addAll(Styles.BUTTON_OUTLINED, Styles.ACCENT);
        badgeTodos.setOnAction(e -> applyTipoFilter(null));
        
        Button badgeIva = new Button("IVA");
        badgeIva.getStyleClass().addAll(Styles.BUTTON_OUTLINED, Styles.ACCENT);
        badgeIva.setOnAction(e -> applyTipoFilter("IVA"));
        
        Button badgeIs = new Button("IS");
        badgeIs.getStyleClass().addAll(Styles.BUTTON_OUTLINED, Styles.SUCCESS);
        badgeIs.setOnAction(e -> applyTipoFilter("IS"));
        
        Button badgeIi = new Button("II");
        badgeIi.getStyleClass().addAll(Styles.BUTTON_OUTLINED, Styles.DANGER);
        badgeIi.setOnAction(e -> applyTipoFilter("II"));

        toolbar.getChildren().addAll(txtBusca, spacer, badgeTodos, badgeIva, badgeIs, badgeIi);
        return toolbar;
    }

    private AdvancedTableView<Imposto> createTable() {
        AdvancedTableView<Imposto> table = new AdvancedTableView<>();
        TableUtils.standardize(table);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_NEXT_COLUMN);
        table.setData(listaImpostos);
        table.setRowFactory(tv -> {
            TableRow<Imposto> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    showImpostoDialog(row.getItem());
                }
            });
            return row;
        });

        TableColumn<Imposto, String> colCodigo = new TableColumn<>("Código");
        colCodigo.setCellValueFactory(new PropertyValueFactory<>("codigo"));
        colCodigo.setPrefWidth(100);

        TableColumn<Imposto, String> colDescricao = new TableColumn<>("Descrição");
        colDescricao.setCellValueFactory(new PropertyValueFactory<>("descricao"));
        colDescricao.setPrefWidth(200);

        TableColumn<Imposto, String> colTipo = new TableColumn<>("Tipo");
        colTipo.setCellValueFactory(new PropertyValueFactory<>("tipo"));
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
                    switch (item) {
                        case "IVA" -> badge.getStyleClass().add(Styles.ACCENT);
                        case "IS" -> badge.getStyleClass().add(Styles.SUCCESS);
                        case "II" -> badge.getStyleClass().add(Styles.DANGER);
                    }
                    setGraphic(badge);
                }
            }
        });
        colTipo.setPrefWidth(80);

        TableColumn<Imposto, BigDecimal> colPercentual = new TableColumn<>("Taxa %");
        colPercentual.setCellValueFactory(new PropertyValueFactory<>("percentual"));
        colPercentual.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item + "%");
                    if (item.compareTo(BigDecimal.ZERO) == 0) {
                        getStyleClass().add(Styles.WARNING);
                    }
                }
            }
        });
        colPercentual.setPrefWidth(80);

        TableColumn<Imposto, String> colMotivo = new TableColumn<>("Motivo Isenção");
        colMotivo.setCellValueFactory(cd -> {
            MotivoIsencao m = cd.getValue().getMotivoIsencao();
            return new SimpleStringProperty(m != null ? m.getCodigo() + " - " + m.getDescricao() : "-");
        });
        colMotivo.setPrefWidth(250);

        TableColumn<Imposto, Void> colActions = new TableColumn<>("Ações");
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit = new Button("", IconUtils.icon(Feather.EDIT, IconUtils.SIZE_SMALL));
            private final Button btnDelete = new Button("", IconUtils.icon(Feather.TRASH_2, IconUtils.SIZE_SMALL));
            {
                btnEdit.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.FLAT);
                btnEdit.setTooltip(new Tooltip("Editar"));
                btnEdit.setOnAction(e -> showImpostoDialog(getTableView().getItems().get(getIndex())));
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
        colActions.setPrefWidth(100);

        table.getColumns().addAll(colCodigo, colDescricao, colTipo, colPercentual, colMotivo, colActions);
        return table;
    }

    private void loadData() {
        listaImpostos.setAll(impostoService.findActive());
        listaMotivos.setAll(motivoService.findActive());
        tabela.setData(listaImpostos);
        updateKPIs();
    }

    private void updateKPIs() {
        lblTotalImpostos.setText(String.valueOf(listaImpostos.size()));
        long ivaAtivo = listaImpostos.stream()
                .filter(i -> "IVA".equals(i.getTipo()))
                .filter(i -> i.getPercentual().compareTo(BigDecimal.ZERO) > 0)
                .count();
        lblIvaAtivo.setText(String.valueOf(ivaAtivo));
        long isencoes = listaImpostos.stream()
                .filter(i -> i.getPercentual().compareTo(BigDecimal.ZERO) == 0)
                .count();
        lblIsencoes.setText(String.valueOf(isencoes));
        BigDecimal taxaMax = listaImpostos.stream()
                .map(Imposto::getPercentual)
                .max(Comparator.naturalOrder())
                .orElse(BigDecimal.ZERO);
        lblTaxaMaxima.setText(taxaMax + "%");
    }

    private void applyFilters() {
        String busca = txtBusca.getText().toLowerCase();
        tabela.setFilter(i -> busca.isEmpty() ||
                        i.getCodigo().toLowerCase().contains(busca) ||
                        i.getDescricao().toLowerCase().contains(busca));
    }

    private void applyTipoFilter(String tipo) {
        if (tipo == null) {
            tabela.setFilter(i -> true);
        } else {
            tabela.setFilter(i -> tipo.equals(i.getTipo()));
        }
        updateKPIs();
    }

    private void showImpostoDialog(Imposto imposto) {
        boolean isEdit = imposto != null;
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setPrefWidth(450);
        
        TextField txtCodigo = new TextField();
        txtCodigo.setPromptText("Ex: ISE, IVA14, IS5");
        
        TextField txtDescricao = new TextField();
        txtDescricao.setPromptText("Descrição do imposto");
        
        ComboBox<String> cmbTipo = new ComboBox<>();
        cmbTipo.getItems().setAll("IVA", "IS", "II");
        cmbTipo.setMaxWidth(Double.MAX_VALUE);
        
        TextField txtPercentual = new TextField();
        txtPercentual.setPromptText("Ex: 14.00");
        
        ComboBox<MotivoIsencao> cmbMotivo = new ComboBox<>();
        cmbMotivo.setItems(listaMotivos);
        cmbMotivo.setMaxWidth(Double.MAX_VALUE);
        cmbMotivo.setDisable(true);
        
        txtPercentual.textProperty().addListener((obs, o, n) -> {
            try {
                BigDecimal val = new BigDecimal(n.isEmpty() ? "0" : n);
                cmbMotivo.setDisable(val.compareTo(BigDecimal.ZERO) != 0);
                if (val.compareTo(BigDecimal.ZERO) != 0) cmbMotivo.setValue(null);
            } catch (NumberFormatException ignored) {}
        });
        
        if (isEdit) {
            txtCodigo.setText(imposto.getCodigo());
            txtDescricao.setText(imposto.getDescricao());
            cmbTipo.setValue(imposto.getTipo());
            txtPercentual.setText(imposto.getPercentual().toString());
            cmbMotivo.setValue(imposto.getMotivoIsencao());
        } else {
            cmbTipo.getSelectionModel().selectFirst();
        }
        
        // Form container with modern styling
        VBox formContainer = new VBox(20);
        formContainer.setStyle("-fx-background-color: -color-bg-overlay; -fx-background-radius: 12; -fx-padding: 25;");
        
        // Header section with icon
        HBox formHeader = new HBox(12);
        formHeader.setAlignment(Pos.CENTER_LEFT);
        FontIcon formIcon = IconUtils.icon(Feather.FILE_TEXT, 24);
        formIcon.setStyle("-fx-icon-color: -color-accent-emphasis;");
        Label formTitle = new Label("Dados do Imposto");
        formTitle.getStyleClass().add(Styles.TITLE_4);
        formHeader.getChildren().addAll(formIcon, formTitle);
        
        // Form fields in a grid layout
        GridPane formGrid = new GridPane();
        formGrid.setHgap(15);
        formGrid.setVgap(18);
        formGrid.setAlignment(Pos.TOP_LEFT);
        
        // Row 0: Código and Tipo (side by side)
        VBox codigoBox = createFormFieldBox("Código", "Identificador único (ex: IVA14)", Feather.HASH, txtCodigo);
        GridPane.setConstraints(codigoBox, 0, 0);
        
        VBox tipoBox = createFormFieldBox("Tipo", "Tipo de imposto fiscal", Feather.TAG, cmbTipo);
        GridPane.setConstraints(tipoBox, 1, 0);
        
        // Row 1: Descrição (full width)
        VBox descricaoBox = createFormFieldBox("Descrição", "Nome descritivo do imposto", Feather.TYPE, txtDescricao);
        GridPane.setConstraints(descricaoBox, 0, 1);
        GridPane.setColumnSpan(descricaoBox, 2);
        
        // Row 2: Percentual and Motivo Isenção
        VBox percentualBox = createFormFieldBox("Taxa Percentual (%)", "Use 0 para isenção fiscal", Feather.PERCENT, txtPercentual);
        GridPane.setConstraints(percentualBox, 0, 2);
        
        VBox motivoBox = createFormFieldBox("Motivo de Isenção", "Obrigatório quando taxa = 0%", Feather.INFO, cmbMotivo);
        GridPane.setConstraints(motivoBox, 1, 2);
        
        // Configure column constraints for equal width
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(50);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(50);
        formGrid.getColumnConstraints().addAll(col1, col2);
        
        formGrid.getChildren().addAll(codigoBox, tipoBox, descricaoBox, percentualBox, motivoBox);
        
        // Info card for exemption
        HBox infoBox = new HBox(10);
        infoBox.setAlignment(Pos.CENTER_LEFT);
        infoBox.setPadding(new Insets(12, 15, 12, 15));
        infoBox.setStyle("-fx-background-color: -color-warning-subtle; -fx-background-radius: 8;");
        FontIcon infoIcon = IconUtils.icon(Feather.ALERT_CIRCLE, 18);
        infoIcon.setStyle("-fx-icon-color: -color-warning-emphasis;");
        Label infoLabel = new Label("Para isenções fiscais (0%), selecione um motivo de isenção válido.");
        infoLabel.getStyleClass().add(Styles.TEXT_SMALL);
        infoLabel.setWrapText(true);
        infoBox.getChildren().addAll(infoIcon, infoLabel);
        
        formContainer.getChildren().addAll(formHeader, formGrid, infoBox);
        content.getChildren().add(formContainer);

        modalService.create()
                .title(isEdit ? "Editar Imposto" : "Novo Imposto")
                .content(content)
                .autoSize()
                .withConfirmButton(isEdit ? "Atualizar" : "Salvar", () -> {
                    try {
                        if (txtCodigo.getText().trim().isEmpty()) {
                            throw new IllegalArgumentException("Código é obrigatório");
                        }
                        if (txtDescricao.getText().trim().isEmpty()) {
                            throw new IllegalArgumentException("Descrição é obrigatória");
                        }
                        if (cmbTipo.getValue() == null) {
                            throw new IllegalArgumentException("Tipo é obrigatório");
                        }
                        if (txtPercentual.getText().trim().isEmpty()) {
                            throw new IllegalArgumentException("Percentual é obrigatório");
                        }

                        BigDecimal percentual = new BigDecimal(txtPercentual.getText());

                        if (percentual.compareTo(BigDecimal.ZERO) == 0 && cmbMotivo.getValue() == null) {
                            throw new IllegalArgumentException("Motivo de isenção é obrigatório para taxa 0%");
                        }

                        Imposto i = isEdit ? imposto : new Imposto();
                        i.setCodigo(txtCodigo.getText().trim().toUpperCase());
                        i.setDescricao(txtDescricao.getText().trim());
                        i.setTipo(cmbTipo.getValue());
                        i.setPercentual(percentual);
                        i.setMotivoIsencao(cmbMotivo.getValue());

                        impostoService.save(i);
                        loadData();
                        AlertUtils.showInfoAlert("Sucesso", isEdit ? "Imposto atualizado com sucesso!" : "Imposto criado com sucesso!");
                        return Boolean.TRUE;
                    } catch (IllegalArgumentException e) {
                        AlertUtils.showWarningAlert("Validação", e.getMessage());
                        return Boolean.FALSE;
                    } catch (Exception e) {
                        AlertUtils.showExceptionAlert("Erro", "Falha ao salvar imposto", e);
                        return Boolean.FALSE;
                    }
                })
                .withCancelButton("Cancelar")
                .buildAndShow();
    }

    private VBox createFormFieldBox(String label, String hint, Feather icon, Node inputNode) {
        VBox box = new VBox(6);
        box.setAlignment(Pos.TOP_LEFT);
        
        // Label with icon
        HBox labelBox = new HBox(8);
        labelBox.setAlignment(Pos.CENTER_LEFT);
        
        FontIcon fieldIcon = IconUtils.icon(icon, 14);
        fieldIcon.setStyle("-fx-icon-color: -color-fg-muted;");
        
        Label lblField = new Label(label);
        lblField.getStyleClass().addAll(Styles.TEXT_SMALL, Styles.TEXT_BOLD);
        lblField.setStyle("-fx-text-fill: -color-fg-default;");
        
        labelBox.getChildren().addAll(fieldIcon, lblField);
        
        // Style the input node
        inputNode.setStyle(inputNode.getStyle() + "-fx-min-height: 36; -fx-background-radius: 6;");
        if (inputNode instanceof TextField) {
            ((TextField) inputNode).setPrefHeight(36);
        } else if (inputNode instanceof ComboBox) {
            ((ComboBox<?>) inputNode).setPrefHeight(36);
        }
        VBox.setVgrow(inputNode, Priority.NEVER);
        
        // Hint label
        Label lblHint = new Label(hint);
        lblHint.getStyleClass().addAll(Styles.TEXT_SMALL, Styles.TEXT_MUTED);
        
        box.getChildren().addAll(labelBox, inputNode, lblHint);
        return box;
    }

    private void confirmarExclusao(Imposto imposto) {
        modalService.create()
                .title("Confirmar Exclusão")
                .content(new Label("Deseja realmente excluir o imposto '" + imposto.getCodigo() + " - " + imposto.getDescricao() + "'?"))
                .autoSize()
                .withCustomButton("Excluir", () -> {
                    try {
                        impostoService.softDelete(imposto.getId());
                        loadData();
                        AlertUtils.showInfoAlert("Sucesso", "Imposto excluído com sucesso!");
                    } catch (Exception e) {
                        AlertUtils.showExceptionAlert("Erro", "Falha ao excluir imposto", e);
                    }
                }, Styles.DANGER)
                .withCancelButton("Cancelar")
                .buildAndShow();
    }
}

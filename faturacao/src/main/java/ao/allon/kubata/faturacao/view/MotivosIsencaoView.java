package ao.allon.kubata.faturacao.view;

import ao.allon.kubata.faturacao.domain.MotivoIsencao;
import ao.allon.kubata.faturacao.service.MotivoIsencaoService;
import ao.allon.kubata.faturacao.ui.modal.ModalService;
import ao.allon.kubata.faturacao.ui.util.AlertUtils;
import ao.allon.kubata.faturacao.ui.util.IconUtils;
import ao.allon.kubata.faturacao.ui.util.TileFactory;
import atlantafx.base.theme.Styles;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.stream.Collectors;

/**
 * View moderna para gestão de Motivos de Isenção com AtlantaFX.
 * Inclui KPIs, busca e formulário em modal.
 */
public class MotivosIsencaoView extends BorderPane {

    private final MotivoIsencaoService service;
    private final ModalService modalService;

    private final ObservableList<MotivoIsencao> itens = FXCollections.observableArrayList();

    private Label lblTotalMotivos;
    private Label lblComLegislacao;
    private TableView<MotivoIsencao> tabela;
    private TextField txtBusca;

    public MotivosIsencaoView(MotivoIsencaoService service, ModalService modalService) {
        this.service = service;
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

        HBox titleBox = new HBox(15);
        titleBox.setAlignment(Pos.CENTER_LEFT);

        FontIcon icon = IconUtils.icon(Feather.INFO, 32);
        icon.setStyle("-fx-icon-color: -color-accent-emphasis;");

        VBox titles = new VBox(5);
        Label title = new Label("Motivos de Isenção");
        title.getStyleClass().add(Styles.TITLE_2);
        Label subtitle = new Label("Códigos de isenção fiscal conforme legislação angolana (M01-M99)");
        subtitle.getStyleClass().add(Styles.TEXT_MUTED);
        titles.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnNovo = new Button("Novo Motivo", IconUtils.icon(Feather.PLUS, IconUtils.SIZE_SMALL));
        btnNovo.getStyleClass().addAll(Styles.BUTTON_OUTLINED, Styles.ACCENT);
        btnNovo.setOnAction(e -> showMotivoDialog(null));

        titleBox.getChildren().addAll(icon, titles, spacer, btnNovo);

        HBox kpiBox = createKPICards();
        header.getChildren().addAll(titleBox, kpiBox);
        return header;
    }

    private HBox createKPICards() {
        HBox box = new HBox(15);
        box.setPadding(new Insets(10, 0, 0, 0));

        VBox cardTotal = createKpiCard(Feather.LAYERS, "Total Motivos", "0", Styles.ACCENT);
        lblTotalMotivos = (Label) cardTotal.getChildren().get(1);

        VBox cardLeg = createKpiCard(Feather.FILE_TEXT, "Com Legislação", "0", Styles.SUCCESS);
        lblComLegislacao = (Label) cardLeg.getChildren().get(1);

        box.getChildren().addAll(cardTotal, cardLeg);
        HBox.setHgrow(cardTotal, Priority.ALWAYS);
        HBox.setHgrow(cardLeg, Priority.ALWAYS);

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
        toolbar.getChildren().addAll(txtBusca, spacer);
        return toolbar;
    }

    private TableView<MotivoIsencao> createTable() {
        TableView<MotivoIsencao> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_NEXT_COLUMN);
        table.setItems(itens);
        table.setRowFactory(tv -> {
            TableRow<MotivoIsencao> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    showMotivoDialog(row.getItem());
                }
            });
            return row;
        });

        TableColumn<MotivoIsencao, String> colCodigo = new TableColumn<>("Código");
        colCodigo.setCellValueFactory(new PropertyValueFactory<>("codigo"));
        colCodigo.setPrefWidth(100);

        TableColumn<MotivoIsencao, String> colDescricao = new TableColumn<>("Descrição");
        colDescricao.setCellValueFactory(new PropertyValueFactory<>("descricao"));
        colDescricao.setPrefWidth(350);

        TableColumn<MotivoIsencao, String> colLegislacao = new TableColumn<>("Legislação");
        colLegislacao.setCellValueFactory(new PropertyValueFactory<>("legislacao"));
        colLegislacao.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label(item.isEmpty() ? "Não informada" : "OK");
                    badge.getStyleClass().addAll(Styles.TEXT_SMALL, Styles.BUTTON_OUTLINED);
                    if (item.isEmpty()) {
                        badge.getStyleClass().add(Styles.WARNING);
                    } else {
                        badge.getStyleClass().add(Styles.SUCCESS);
                    }
                    setGraphic(badge);
                }
            }
        });
        colLegislacao.setPrefWidth(150);

        TableColumn<MotivoIsencao, Void> colActions = new TableColumn<>("Ações");
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit = new Button("", IconUtils.icon(Feather.EDIT, IconUtils.SIZE_SMALL));
            private final Button btnDelete = new Button("", IconUtils.icon(Feather.TRASH_2, IconUtils.SIZE_SMALL));
            {
                btnEdit.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.FLAT);
                btnEdit.setTooltip(new Tooltip("Editar"));
                btnEdit.setOnAction(e -> showMotivoDialog(getTableView().getItems().get(getIndex())));
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

        table.getColumns().addAll(colCodigo, colDescricao, colLegislacao, colActions);
        return table;
    }

    private void loadData() {
        itens.setAll(service.findAll());
        updateKPIs();
    }

    private void updateKPIs() {
        lblTotalMotivos.setText(String.valueOf(itens.size()));
        long comLeg = itens.stream()
                .filter(m -> m.getLegislacao() != null && !m.getLegislacao().isEmpty())
                .count();
        lblComLegislacao.setText(String.valueOf(comLeg));
    }

    private void applyFilters() {
        String busca = txtBusca.getText().toLowerCase();
        var filtered = service.findAll().stream()
                .filter(m -> busca.isEmpty() ||
                        m.getCodigo().toLowerCase().contains(busca) ||
                        m.getDescricao().toLowerCase().contains(busca))
                .collect(Collectors.toList());
        itens.setAll(filtered);
    }

    private void showMotivoDialog(MotivoIsencao motivo) {
        boolean isEdit = motivo != null;
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setPrefWidth(450);

        TextField txtCodigo = new TextField();
        txtCodigo.setPromptText("Ex: M01, M02, ISE");

        TextField txtDescricao = new TextField();
        txtDescricao.setPromptText("Descrição do motivo de isenção");

        TextField txtLegislacao = new TextField();
        txtLegislacao.setPromptText("Artigo/Lei de referência");

        if (isEdit) {
            txtCodigo.setText(motivo.getCodigo());
            txtDescricao.setText(motivo.getDescricao());
            txtLegislacao.setText(motivo.getLegislacao());
        }

        content.getChildren().addAll(
            TileFactory.createFormField("Código", "Código único (ex: M01)", txtCodigo),
            TileFactory.createFormField("Descrição", "Descrição do motivo de isenção", txtDescricao),
            TileFactory.createFormField("Legislação", "Artigo ou lei de referência", txtLegislacao)
        );

        modalService.create()
                .title(isEdit ? "Editar Motivo" : "Novo Motivo")
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

                        MotivoIsencao m = isEdit ? motivo : new MotivoIsencao();
                        m.setCodigo(txtCodigo.getText().trim().toUpperCase());
                        m.setDescricao(txtDescricao.getText().trim());
                        m.setLegislacao(txtLegislacao.getText().trim());

                        service.save(m);
                        loadData();
                        AlertUtils.showInfoAlert("Sucesso", isEdit ? "Motivo atualizado!" : "Motivo criado!");
                        return Boolean.TRUE;
                    } catch (IllegalArgumentException e) {
                        AlertUtils.showWarningAlert("Validação", e.getMessage());
                        return Boolean.FALSE;
                    } catch (Exception e) {
                        AlertUtils.showExceptionAlert("Erro", "Falha ao salvar", e);
                        return Boolean.FALSE;
                    }
                })
                .withCancelButton("Cancelar")
                .buildAndShow();
    }

    private void confirmarExclusao(MotivoIsencao motivo) {
        modalService.create()
                .title("Confirmar Exclusão")
                .content(new Label("Deseja excluir '" + motivo.getCodigo() + " - " + motivo.getDescricao() + "'?"))
                .autoSize()
                .withCustomButton("Excluir", () -> {
                    try {
                        service.delete(motivo.getId());
                        loadData();
                        AlertUtils.showInfoAlert("Sucesso", "Motivo excluído!");
                    } catch (Exception e) {
                        AlertUtils.showExceptionAlert("Erro", "Falha ao excluir", e);
                    }
                }, Styles.DANGER)
                .withCancelButton("Cancelar")
                .buildAndShow();
    }
}

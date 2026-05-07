package ao.allon.kubata.faturacao.view;

import ao.allon.kubata.core.ui.table.TableContextMenuHelper;
import ao.allon.kubata.core.ui.table.EditableTableManager;
import ao.allon.kubata.faturacao.domain.Categoria;
import ao.allon.kubata.faturacao.service.CategoriaService;
import ao.allon.kubata.faturacao.service.SessionManager;
import ao.allon.kubata.faturacao.ui.modal.ModalService;
import ao.allon.kubata.faturacao.ui.util.AlertUtils;
import ao.allon.kubata.faturacao.ui.util.IconUtils;
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
import org.kordamp.ikonli.feather.Feather;

/**
 * View para gestão de categorias de produtos.
 */
public class CategoriasView extends VBox {

    private final CategoriaService categoriaService;
    private final SessionManager sessionManager;
    private final ModalService modalService;

    private final TableView<Categoria> table = new TableView<>();
    private final ObservableList<Categoria> masterData = FXCollections.observableArrayList();
    private FilteredList<Categoria> filtered;
    private SortedList<Categoria> sorted;

    public CategoriasView(CategoriaService categoriaService,
                          SessionManager sessionManager,
                          ModalService modalService) {
        this.categoriaService = categoriaService;
        this.sessionManager = sessionManager;
        this.modalService = modalService;

        setSpacing(20);
        setPadding(new Insets(20));

        setupHeader();
        setupTable();
        loadData();
    }

    private void setupHeader() {
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 10, 0));

        VBox titleBox = new VBox(5);
        Label title = new Label("Gestão de Categorias");
        title.getStyleClass().add(Styles.TITLE_3);
        Label subtitle = new Label("Cadastre e organize as categorias de produtos");
        subtitle.getStyleClass().add(Styles.TEXT_MUTED);
        titleBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        CustomTextField search = new CustomTextField();
        search.setPromptText("Pesquisar categoria...");
        search.setLeft(IconUtils.icon(Feather.SEARCH, IconUtils.SIZE_SMALL));
        search.setPrefWidth(250);
        search.textProperty().addListener((obs, ov, nv) -> applyFilter(nv));

        Button btnNovo = new Button("Nova Categoria", IconUtils.icon(Feather.PLUS, IconUtils.SIZE_SMALL));
        btnNovo.getStyleClass().addAll(Styles.BUTTON_OUTLINED, Styles.SUCCESS);
        btnNovo.setOnAction(e -> showCategoriaForm(null));

        boolean canEdit = sessionManager.hasAccess("PRODUTOS", "Editar");
        btnNovo.setDisable(!canEdit);

        header.getChildren().addAll(titleBox, spacer, search, btnNovo);
        getChildren().add(header);
    }

    private void setupTable() {
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setEditable(true);

        // Criar manager para colunas editáveis
        EditableTableManager<Categoria> editManager = new EditableTableManager<>(table);

        // Coluna Nome - Editável
        TableColumn<Categoria, String> colNome = editManager.createTextColumn("Nome",
            cdf -> new javafx.beans.property.SimpleStringProperty(cdf.getValue().getNome()),
            (categoria, novoNome) -> {
                categoria.setNome(novoNome);
                categoriaService.save(categoria);
            });
        colNome.setMinWidth(200);

        // Coluna Descrição - Editável
        TableColumn<Categoria, String> colDescricao = editManager.createTextColumn("Descrição",
            cdf -> new javafx.beans.property.SimpleStringProperty(cdf.getValue().getDescricao()),
            (categoria, novaDescricao) -> {
                categoria.setDescricao(novaDescricao);
                categoriaService.save(categoria);
            });
        colDescricao.setMinWidth(300);

        TableColumn<Categoria, Void> colAcoes = new TableColumn<>("Ações");
        colAcoes.setMinWidth(150);
        colAcoes.setCellFactory(col -> new TableCell<>() {
            private final Button btnEditar = new Button(null, IconUtils.icon(Feather.EDIT_2, IconUtils.SIZE_SMALL));
            private final Button btnExcluir = new Button(null, IconUtils.icon(Feather.TRASH_2, IconUtils.SIZE_SMALL));
            private final HBox box = new HBox(5, btnEditar, btnExcluir);

            {
                btnEditar.getStyleClass().addAll(Styles.BUTTON_OUTLINED, Styles.SMALL);
                btnExcluir.getStyleClass().addAll(Styles.BUTTON_OUTLINED, Styles.DANGER, Styles.SMALL);

                boolean canEdit = sessionManager.hasAccess("PRODUTOS", "Editar");
                btnEditar.setDisable(!canEdit);
                btnExcluir.setDisable(!canEdit);

                btnEditar.setOnAction(e -> {
                    Categoria c = getTableView().getItems().get(getIndex());
                    showCategoriaForm(c);
                });

                btnExcluir.setOnAction(e -> {
                    Categoria c = getTableView().getItems().get(getIndex());
                    confirmarExclusao(c);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        table.getColumns().addAll(colNome, colDescricao, colAcoes);

        // Configurar menu de contexto
        TableContextMenuHelper.createCrudMenu(table, "Categoria",
                categoria -> showCategoriaForm(categoria),
                categoria -> confirmarExclusao(categoria),
                () -> loadData())
            .apply();

        VBox.setVgrow(table, Priority.ALWAYS);
        getChildren().add(table);
    }

    private void loadData() {
        masterData.setAll(categoriaService.findAll());
        filtered = new FilteredList<>(masterData, p -> true);
        sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sorted);
    }

    private void applyFilter(String text) {
        if (text == null || text.isEmpty()) {
            filtered.setPredicate(p -> true);
        } else {
            String lower = text.toLowerCase();
            filtered.setPredicate(c ->
                c.getNome().toLowerCase().contains(lower) ||
                (c.getDescricao() != null && c.getDescricao().toLowerCase().contains(lower))
            );
        }
    }

    private void showCategoriaForm(Categoria categoria) {
        boolean isEdit = categoria != null;

        TextField txtNome = new TextField(isEdit ? categoria.getNome() : "");
        txtNome.setPromptText("Nome da categoria");

        TextArea txtDescricao = new TextArea(isEdit ? categoria.getDescricao() : "");
        txtDescricao.setPromptText("Descrição (opcional)");
        txtDescricao.setPrefRowCount(3);
        txtDescricao.setWrapText(true);

        VBox content = new VBox(10,
            new Label("Nome:"), txtNome,
            new Label("Descrição:"), txtDescricao
        );
        content.setPadding(new Insets(10));

        modalService.create()
            .title(isEdit ? "Editar Categoria" : "Nova Categoria")
            .content(content)
            .autoSize()
            .withConfirmButton(isEdit ? "Salvar" : "Criar", () -> {
                String nome = txtNome.getText().trim();
                if (nome.isEmpty()) {
                    AlertUtils.showWarningAlert("Campo Obrigatório", "O nome da categoria é obrigatório.");
                    return false;
                }

                try {
                    Categoria nova = isEdit ? categoria : new Categoria();
                    nova.setNome(nome);
                    nova.setDescricao(txtDescricao.getText().trim());
                    categoriaService.save(nova);
                    loadData();
                    return true;
                } catch (Exception ex) {
                    AlertUtils.showExceptionAlert("Erro", "Não foi possível salvar a categoria.", ex);
                    return false;
                }
            })
            .withCancelButton("Cancelar")
            .buildAndShow();
    }

    private void confirmarExclusao(Categoria categoria) {
        modalService.create()
            .title("Confirmar Exclusão")
            .content(new Label("Deseja realmente excluir a categoria '" + categoria.getNome() + "'?"))
            .autoSize()
            .withConfirmButton("Excluir", () -> {
                try {
                    categoriaService.delete(categoria.getId());
                    loadData();
                    return true;
                } catch (Exception ex) {
                    AlertUtils.showExceptionAlert("Erro", "Não foi possível excluir a categoria. Verifique se há produtos associados.", ex);
                    return false;
                }
            })
            .withCancelButton("Cancelar")
            .buildAndShow();
    }
}

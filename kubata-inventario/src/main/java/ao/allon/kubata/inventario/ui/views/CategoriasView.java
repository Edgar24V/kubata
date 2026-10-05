package ao.allon.kubata.inventario.ui.views;

import ao.allon.kubata.inventario.domain.Categoria;
import ao.allon.kubata.inventario.service.CategoriaService;
import ao.allon.kubata.inventario.ui.modal.ModalManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.springframework.stereotype.Component;

@Component
public class CategoriasView extends BorderPane {
    private final CategoriaService service;
    private final ModalManager modalManager;
    private final ObservableList<Categoria> data = FXCollections.observableArrayList();
    private final TableView<Categoria> table = new TableView<>(data);

    public CategoriasView(CategoriaService service, ModalManager modalManager) {
        this.service = service;
        this.modalManager = modalManager;
        build();
        refreshData();
    }

    private void build() {
        getStyleClass().add("inventario-page");
        setPadding(new Insets(22));
        Label title = new Label("Categorias");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Estrutura de classificação de artigos");
        subtitle.getStyleClass().add("page-subtitle");

        Button novo = new Button("Nova categoria");
        novo.getStyleClass().add("primary-action");
        novo.setOnAction(e -> novo());
        Button refresh = new Button("Atualizar");
        refresh.setOnAction(e -> refreshData());

        HBox actions = new HBox(10, novo, refresh);
        VBox header = new VBox(4, title, subtitle, actions);
        header.setSpacing(10);

        TableColumn<Categoria,String> nome = new TableColumn<>("Nome");
        nome.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNome()));
        TableColumn<Categoria,String> desc = new TableColumn<>("Descrição");
        desc.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDescricao()));
        table.getColumns().addAll(nome, desc);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("Nenhuma categoria criada."));

        setCenter(new VBox(16, header, table));
        VBox.setVgrow(table, Priority.ALWAYS);
    }

    public void refreshData() {
        data.setAll(service.findAll());
    }

    private void novo() {
        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(8));
        TextField nome = new TextField();
        TextArea desc = new TextArea();
        desc.setPrefRowCount(4);
        grid.addRow(0, new Label("Nome:*"), nome);
        grid.addRow(1, new Label("Descrição:"), desc);

        Dialog<ButtonType> dialog = modalManager.form(this, "Nova categoria", grid);
        if (dialog.showAndWait().filter(ButtonType.OK::equals).isEmpty()) return;

        try {
            Categoria c = new Categoria();
            c.setNome(nome.getText().trim());
            c.setDescricao(desc.getText().trim());
            service.save(c);
            refreshData();
        } catch (Exception ex) {
            modalManager.error(this, "Categoria", "Não foi possível gravar: " + ex.getMessage());
        }
    }
}

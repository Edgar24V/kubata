package ao.allon.kubata.inventario.ui;

import ao.allon.kubata.core.ui.table.AdvancedTableView;
import ao.allon.kubata.inventario.domain.Categoria;
import ao.allon.kubata.inventario.repository.CategoriaRepository;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.springframework.stereotype.Component;

@Component
public class CategoriasView extends BorderPane {
    private final CategoriaRepository repository;
    private final AdvancedTableView<Categoria> table = new AdvancedTableView<>();

    public CategoriasView(CategoriaRepository repository) {
        this.repository = repository;
        build();
    }

    private void build() {
        InventarioUI.installCss(this);
        setPadding(new Insets(18));
        VBox root = new VBox(14);

        Label title = new Label("Categorias de Artigos");
        title.getStyleClass().add("inventario-page-title");
        Label subtitle = new Label("Classificação do catálogo para filtros, políticas de stock e análise.");
        subtitle.getStyleClass().add("inventario-page-subtitle");

        Button novo = InventarioUI.primaryButton("Nova categoria");
        novo.setOnAction(e -> editar(null));
        Button editar = InventarioUI.secondaryButton("Editar");
        editar.setOnAction(e -> editar(table.getSelectionModel().getSelectedItem()));
        Button refresh = InventarioUI.secondaryButton("Atualizar");
        refresh.setOnAction(e -> load());

        table.setData(FXCollections.observableArrayList());
        TableColumn<Categoria,String> nome = new TableColumn<>("Nome");
        nome.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getNome()));
        TableColumn<Categoria,String> descricao = new TableColumn<>("Descrição");
        descricao.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getDescricao()));
        TableColumn<Categoria,String> estado = new TableColumn<>("Estado");
        estado.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                Boolean.TRUE.equals(c.getValue().getActive()) ? "ATIVA" : "INATIVA"));
        table.getColumns().addAll(nome, descricao, estado);
        InventarioUI.styleTable(table);
        VBox.setVgrow(table, Priority.ALWAYS);

        root.getChildren().addAll(new VBox(4, title, subtitle), new HBox(8, novo, editar, refresh), table);
        setCenter(root);
        load();
    }

    private void load() {
        table.setData(FXCollections.observableArrayList(repository.findAll()));
    }

    private void editar(Categoria atual) {
        Dialog<ButtonType> d = new Dialog<>();
        d.setTitle(atual == null ? "Nova Categoria" : "Editar Categoria");
        d.getDialogPane().getStyleClass().add("inventario-dialog");
        d.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

        TextField nome = InventarioUI.field("Nome");
        TextField descricao = InventarioUI.field("Descrição");
        if (atual != null) {
            nome.setText(atual.getNome());
            descricao.setText(atual.getDescricao());
        }

        GridPane g = new GridPane();
        g.setHgap(10); g.setVgap(9);
        g.addRow(0, new Label("Nome"), nome);
        g.addRow(1, new Label("Descrição"), descricao);
        d.getDialogPane().setContent(g);

        Button ok = (Button) d.getDialogPane().lookupButton(ButtonType.OK);
        ok.addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
            try {
                if (nome.getText().isBlank()) throw new IllegalArgumentException("O nome é obrigatório.");
                Categoria c = atual == null ? new Categoria() : atual;
                c.setNome(nome.getText().trim());
                c.setDescricao(descricao.getText().trim());
                repository.save(c);
                load();
            } catch (Exception ex) {
                e.consume();
                InventarioUI.error("Categoria", ex.getMessage());
            }
        });
        d.showAndWait();
    }
}

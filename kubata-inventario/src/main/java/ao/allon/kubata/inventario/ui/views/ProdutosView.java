package ao.allon.kubata.inventario.ui.views;

import ao.allon.kubata.inventario.domain.Categoria;
import ao.allon.kubata.inventario.domain.Produto;
import ao.allon.kubata.inventario.enums.UnidadeMedida;
import ao.allon.kubata.inventario.repository.CategoriaRepository;
import ao.allon.kubata.inventario.service.ProdutoService;
import ao.allon.kubata.inventario.ui.modal.ModalManager;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class ProdutosView extends BorderPane {
    private final ProdutoService service;
    private final CategoriaRepository categoriaRepository;
    private final ModalManager modalManager;
    private final TableView<Produto> table = new TableView<>();
    private final ObservableList<Produto> data = FXCollections.observableArrayList();
    private final TextField search = new TextField();

    public ProdutosView(ProdutoService service, CategoriaRepository categoriaRepository, ModalManager modalManager) {
        this.service = service;
        this.categoriaRepository = categoriaRepository;
        this.modalManager = modalManager;
        build();
        refresh();
    }

    private void build() {
        getStyleClass().add("inventario-page");
        setPadding(new Insets(22));

        Label title = new Label("Artigos");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Cadastro central de produtos e serviços");
        subtitle.getStyleClass().add("page-subtitle");

        HBox head = new HBox(12, title, subtitle);
        head.setAlignment(javafx.geometry.Pos.BASELINE_LEFT);
        head.setPadding(new Insets(0, 0, 14, 0));

        Button novo = new Button("Novo artigo");
        novo.getStyleClass().add("primary-action");
        novo.setOnAction(e -> novo());

        Button refresh = new Button("Atualizar");
        refresh.setOnAction(e -> refresh());

        search.setPromptText("Pesquisar por código ou descrição...");
        search.setPrefWidth(320);
        search.textProperty().addListener((o, a, b) -> filter(b));

        HBox actions = new HBox(10, novo, refresh, new Region(), search);
        HBox.setHgrow(actions.getChildren().get(2), Priority.ALWAYS);
        actions.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        table.setItems(data);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("Não existem artigos para apresentar."));

        TableColumn<Produto,String> codigo = new TableColumn<>("Código");
        codigo.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCodigoBarra()));
        TableColumn<Produto,String> nome = new TableColumn<>("Descrição");
        nome.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNome()));
        TableColumn<Produto,String> categoria = new TableColumn<>("Categoria");
        categoria.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getCategoria() == null ? "" : c.getValue().getCategoria().getNome()));
        TableColumn<Produto,BigDecimal> preco = new TableColumn<>("PVP");
        preco.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getPrecoUnitario()));
        TableColumn<Produto,Integer> stock = new TableColumn<>("Stock");
        stock.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getStock()));
        TableColumn<Produto,String> unidade = new TableColumn<>("Un.");
        unidade.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getUnidadeMedida() == null ? "" : c.getValue().getUnidadeMedida().name()));

        stock.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Integer value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? null : String.valueOf(value));
                if (!empty && value != null) {
                    getStyleClass().removeAll("stock-low", "stock-ok");
                    getStyleClass().add(value <= 0 ? "stock-low" : "stock-ok");
                }
            }
        });

        table.getColumns().addAll(codigo, nome, categoria, preco, stock, unidade);
        VBox content = new VBox(14, head, actions, table);
        VBox.setVgrow(table, Priority.ALWAYS);

        Label bar = new Label();
        bar.getStyleClass().add("status-line");
        content.getChildren().add(bar);
        data.addListener((javafx.collections.ListChangeListener<Produto>) c ->
                bar.setText(data.size() + " artigo(s) ativo(s)"));

        setCenter(content);
        setMargin(content, new Insets(0));
    }

    private void filter(String query) {
        List<Produto> all = service.findAll();
        if (query == null || query.isBlank()) {
            data.setAll(all);
            return;
        }
        String q = query.trim().toLowerCase();
        data.setAll(all.stream()
                .filter(p -> (p.getNome() != null && p.getNome().toLowerCase().contains(q))
                        || (p.getCodigoBarra() != null && p.getCodigoBarra().toLowerCase().contains(q)))
                .toList());
    }

    private void refresh() {
        filter(search.getText());
    }

    private void novo() {
        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(8));

        TextField nome = new TextField();
        TextField codigo = new TextField();
        TextField preco = new TextField();
        TextField stock = new TextField("0");
        ComboBox<UnidadeMedida> unidade = new ComboBox<>(FXCollections.observableArrayList(UnidadeMedida.values()));
        unidade.setValue(UnidadeMedida.UNIDADE);
        ComboBox<Categoria> categoria = new ComboBox<>(FXCollections.observableArrayList(categoriaRepository.findAll()));
        if (!categoria.getItems().isEmpty()) categoria.setValue(categoria.getItems().get(0));

        grid.addRow(0, new Label("Descrição:*"), nome);
        grid.addRow(1, new Label("Código:*"), codigo);
        grid.addRow(2, new Label("PVP:*"), preco);
        grid.addRow(3, new Label("Stock inicial:"), stock);
        grid.addRow(4, new Label("Unidade:"), unidade);
        grid.addRow(5, new Label("Categoria:"), categoria);

        Dialog<ButtonType> dialog = modalManager.form(this, "Novo artigo", grid);
        dialog.setOnShown(e -> nome.requestFocus());

        if (dialog.showAndWait().filter(ButtonType.OK::equals).isEmpty()) return;

        try {
            Produto p = new Produto();
            p.setNome(nome.getText().trim());
            p.setCodigoBarra(codigo.getText().trim());
            p.setPrecoUnitario(new BigDecimal(preco.getText().trim().replace(",", ".")));
            p.setStock(Integer.parseInt(stock.getText().trim()));
            p.setUnidadeMedida(unidade.getValue());
            p.setCategoria(categoria.getValue());
            service.save(p);
            refresh();
        } catch (Exception ex) {
            modalManager.error(this, "Artigo", "Não foi possível gravar: " + ex.getMessage());
        }
    }
}

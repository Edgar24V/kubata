package ao.allon.kubata.inventario.ui;

import ao.allon.kubata.core.ui.table.AdvancedTableView;
import ao.allon.kubata.inventario.domain.Categoria;
import ao.allon.kubata.inventario.domain.Produto;
import ao.allon.kubata.inventario.enums.UnidadeMedida;
import ao.allon.kubata.inventario.service.ProdutoService;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

@Component
public class ProdutosView extends BorderPane {
    private final ProdutoService service;
    private final ao.allon.kubata.inventario.repository.CategoriaRepository categoriaRepository;
    private final AdvancedTableView<Produto> table = new AdvancedTableView<>();
    private final TextField search = new TextField();

    public ProdutosView(ProdutoService service,
                        ao.allon.kubata.inventario.repository.CategoriaRepository categoriaRepository) {
        this.service = service;
        this.categoriaRepository = categoriaRepository;
        build();
    }

    private void build() {
        InventarioUI.installCss(this);
        setPadding(new Insets(18));
        VBox root = new VBox(14);
        Label title = new Label("Artigos / Produtos");
        title.getStyleClass().add("inventario-page-title");
        Label sub = new Label("Cadastro mestre, preços, unidades, mínimos e classificação.");
        sub.getStyleClass().add("inventario-page-subtitle");

        search.setPromptText("Pesquisar por nome ou código de barras...");
        search.getStyleClass().add("inventario-field");
        search.textProperty().addListener((o,a,b) -> filter(b));

        Button novo = InventarioUI.primaryButton("Novo artigo");
        novo.setOnAction(e -> editar(null));
        Button editar = InventarioUI.secondaryButton("Editar");
        editar.setOnAction(e -> editar(table.getSelectionModel().getSelectedItem()));
        Button atualizar = InventarioUI.secondaryButton("Atualizar");
        atualizar.setOnAction(e -> refresh());

        HBox toolbar = new HBox(8, search, novo, editar, atualizar);
        HBox.setHgrow(search, Priority.ALWAYS);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        table.setData(FXCollections.observableArrayList());
        table.setEntityName("Artigo");
        table.setOnViewDetails(this::editar);
        TableColumn<Produto,String> codigo = new TableColumn<>("Código");
        codigo.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getCodigoBarra()));
        TableColumn<Produto,String> nome = new TableColumn<>("Descrição");
        nome.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getNome()));
        TableColumn<Produto,String> unidade = new TableColumn<>("Unidade");
        unidade.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                c.getValue().getUnidadeMedida() == null ? "" : c.getValue().getUnidadeMedida().name()));
        TableColumn<Produto,String> stock = new TableColumn<>("Stock");
        stock.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(String.valueOf(
                c.getValue().getStock() == null ? 0 : c.getValue().getStock())));
        TableColumn<Produto,String> custo = new TableColumn<>("Custo");
        custo.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                InventarioUI.money(c.getValue().getPrecoCompra())));
        table.getColumns().addAll(codigo,nome,unidade,stock,custo);
        InventarioUI.styleTable(table);
        VBox.setVgrow(table, Priority.ALWAYS);

        root.getChildren().addAll(new VBox(4,title,sub), toolbar, table);
        setCenter(root);
        refresh();
    }

    private void filter(String text) {
        String q = text == null ? "" : text.trim().toLowerCase();
        table.setFilter(p -> q.isBlank()
                || p.getNome().toLowerCase().contains(q)
                || p.getCodigoBarra().toLowerCase().contains(q));
    }

    private void refresh() {
        table.setData(FXCollections.observableArrayList(service.findAll()));
        if (!search.getText().isBlank()) filter(search.getText());
    }

    private void editar(Produto atual) {
        Dialog<ButtonType> d = new Dialog<>();
        d.setTitle(atual == null ? "Novo Artigo" : "Editar Artigo");
        d.getDialogPane().getStyleClass().add("inventario-dialog");
        d.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

        TextField codigo = InventarioUI.field("Código / código de barras");
        TextField nome = InventarioUI.field("Descrição do artigo");
        TextField preco = InventarioUI.field("Preço de venda");
        TextField custo = InventarioUI.field("Preço de custo");
        TextField iva = InventarioUI.field("IVA (%)");
        TextField minimo = InventarioUI.field("Stock mínimo");
        TextField maximo = InventarioUI.field("Stock máximo");
        ComboBox<UnidadeMedida> unidade = new ComboBox<>(FXCollections.observableArrayList(UnidadeMedida.values()));
        unidade.getStyleClass().add("inventario-field");
        ComboBox<Categoria> categoria = new ComboBox<>(FXCollections.observableArrayList(categoriaRepository.findAll()));
        categoria.getStyleClass().add("inventario-field");

        if (atual != null) {
            codigo.setText(atual.getCodigoBarra());
            nome.setText(atual.getNome());
            preco.setText(String.valueOf(atual.getPrecoUnitario()));
            custo.setText(String.valueOf(atual.getPrecoCompra()));
            iva.setText(String.valueOf(atual.getPercentualIva()));
            minimo.setText(String.valueOf(atual.getStockMinimo()));
            maximo.setText(atual.getStockMaximo() == null ? "" : String.valueOf(atual.getStockMaximo()));
            unidade.setValue(atual.getUnidadeMedida());
            categoria.setValue(atual.getCategoria());
        } else {
            unidade.setValue(UnidadeMedida.UNIDADE);
            iva.setText("14.00");
            minimo.setText("5");
        }

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(9);
        grid.addRow(0,new Label("Código"),codigo,new Label("Nome"),nome);
        grid.addRow(1,new Label("Preço venda"),preco,new Label("Preço custo"),custo);
        grid.addRow(2,new Label("IVA"),iva,new Label("Unidade"),unidade);
        grid.addRow(3,new Label("Stock mínimo"),minimo,new Label("Stock máximo"),maximo);
        grid.addRow(4,new Label("Categoria"),categoria);
        VBox box = new VBox(8, new Label("Dados mestres"), grid);
        box.setPadding(new Insets(8));
        d.getDialogPane().setContent(box);

        Button ok=(Button)d.getDialogPane().lookupButton(ButtonType.OK);
        ok.addEventFilter(javafx.event.ActionEvent.ACTION,e -> {
            try {
                if (codigo.getText().isBlank() || nome.getText().isBlank()) throw new IllegalArgumentException("Código e nome são obrigatórios.");
                Produto p = atual == null ? new Produto() : atual;
                p.setCodigoBarra(codigo.getText().trim());
                p.setNome(nome.getText().trim());
                p.setPrecoUnitario(new BigDecimal(preco.getText().replace(',','.')));
                p.setPrecoCompra(new BigDecimal(custo.getText().replace(',','.')));
                p.setPercentualIva(new BigDecimal(iva.getText().replace(',','.')));
                p.setStockMinimo(Integer.parseInt(minimo.getText().trim()));
                p.setStockMaximo(maximo.getText().isBlank()?null:Integer.parseInt(maximo.getText().trim()));
                p.setUnidadeMedida(unidade.getValue());
                p.setUnidadeCompra(unidade.getValue());
                p.setCategoria(categoria.getValue());
                service.save(p);
                refresh();
            } catch (Exception ex) {
                e.consume();
                InventarioUI.error("Dados inválidos", ex.getMessage());
            }
        });
        d.showAndWait();
    }
}

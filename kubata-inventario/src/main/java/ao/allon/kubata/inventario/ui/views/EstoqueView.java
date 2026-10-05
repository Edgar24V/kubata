package ao.allon.kubata.inventario.ui.views;

import ao.allon.kubata.inventario.domain.Estoque;
import ao.allon.kubata.inventario.service.EstoqueService;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.springframework.stereotype.Component;

@Component
public class EstoqueView extends BorderPane {
    private final EstoqueService service;
    private final ObservableList<Estoque> data = FXCollections.observableArrayList();
    private final TableView<Estoque> table = new TableView<>(data);

    public EstoqueView(EstoqueService service) {
        this.service = service;
        build();
        refreshData();
    }

    private void build() {
        getStyleClass().add("inventario-page");
        setPadding(new Insets(22));

        Label title = new Label("Gestão de Stock");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Existências por artigo, armazém, lote e validade");
        subtitle.getStyleClass().add("page-subtitle");

        Button refresh = new Button("Atualizar");
        refresh.setOnAction(e -> refreshData());

        Button vencidos = new Button("Ver vencidos");
        vencidos.setOnAction(e -> data.setAll(service.findVencidos()));

        HBox actions = new HBox(10, refresh, vencidos);
        VBox header = new VBox(4, title, subtitle, actions);
        header.setSpacing(10);

        TableColumn<Estoque,String> produto = new TableColumn<>("Artigo");
        produto.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getProduto() == null ? "" : c.getValue().getProduto().getNome()));
        TableColumn<Estoque,String> armazem = new TableColumn<>("Armazém");
        armazem.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getArmazem() == null ? "" : c.getValue().getArmazem().getNome()));
        TableColumn<Estoque,String> lote = new TableColumn<>("Lote");
        lote.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getLote()));
        TableColumn<Estoque,Integer> quantidade = new TableColumn<>("Quantidade");
        quantidade.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getQuantidade()));
        TableColumn<Estoque,String> validade = new TableColumn<>("Validade");
        validade.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getValidade() == null ? "" : c.getValue().getValidade().toString()));
        table.getColumns().addAll(produto, armazem, lote, quantidade, validade);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("Nenhuma existência registada."));

        setCenter(new VBox(16, header, table));
        VBox.setVgrow(table, Priority.ALWAYS);
    }

    public void refreshData() {
        data.setAll(service.findAll());
    }
}

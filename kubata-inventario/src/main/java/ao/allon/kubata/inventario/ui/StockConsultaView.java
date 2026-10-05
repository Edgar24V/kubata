package ao.allon.kubata.inventario.ui;

import ao.allon.kubata.core.ui.table.AdvancedTableView;
import ao.allon.kubata.inventario.domain.Estoque;
import ao.allon.kubata.inventario.repository.EstoqueRepository;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.springframework.stereotype.Component;

@Component
public class StockConsultaView extends BorderPane {
    private final EstoqueRepository repository;
    private final AdvancedTableView<Estoque> table=new AdvancedTableView<>();

    public StockConsultaView(EstoqueRepository repository){this.repository=repository;build();}
    private void build(){
        InventarioUI.installCss(this);setPadding(new Insets(18));
        VBox root=new VBox(14);
        Label title=new Label("Consulta de Stock");title.getStyleClass().add("inventario-page-title");
        Label sub=new Label("Existências por artigo, armazém e lote. A consulta representa o stock físico registado.");sub.getStyleClass().add("inventario-page-subtitle");
        Button refresh=InventarioUI.primaryButton("Atualizar");refresh.setOnAction(e->load());
        HBox bar=new HBox(refresh);bar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        table.setData(FXCollections.observableArrayList());
        TableColumn<Estoque,String> p=new TableColumn<>("Artigo");p.setCellValueFactory(c->new javafx.beans.property.SimpleStringProperty(c.getValue().getProduto()==null?"":c.getValue().getProduto().getNome()));
        TableColumn<Estoque,String> a=new TableColumn<>("Armazém");a.setCellValueFactory(c->new javafx.beans.property.SimpleStringProperty(c.getValue().getArmazem()==null?"":c.getValue().getArmazem().getNome()));
        TableColumn<Estoque,String> lote=new TableColumn<>("Lote");lote.setCellValueFactory(c->new javafx.beans.property.SimpleStringProperty(c.getValue().getLote()==null?"—":c.getValue().getLote()));
        TableColumn<Estoque,String> q=new TableColumn<>("Quantidade");q.setCellValueFactory(c->new javafx.beans.property.SimpleStringProperty(String.valueOf(c.getValue().getQuantidade()==null?0:c.getValue().getQuantidade())));
        TableColumn<Estoque,String> custo=new TableColumn<>("Custo");custo.setCellValueFactory(c->new javafx.beans.property.SimpleStringProperty(InventarioUI.money(c.getValue().getPrecoCompra())));
        TableColumn<Estoque,String> valor=new TableColumn<>("Valor");valor.setCellValueFactory(c->new javafx.beans.property.SimpleStringProperty(InventarioUI.money(
                java.util.Optional.ofNullable(c.getValue().getQuantidade()).orElse(0) == 0 ? java.math.BigDecimal.ZERO :
                (c.getValue().getPrecoCompra()==null?java.math.BigDecimal.ZERO:c.getValue().getPrecoCompra()).multiply(java.math.BigDecimal.valueOf(c.getValue().getQuantidade())))));
        table.getColumns().addAll(p,a,lote,q,custo,valor);InventarioUI.styleTable(table);VBox.setVgrow(table,Priority.ALWAYS);
        root.getChildren().addAll(new VBox(4,title,sub),bar,table);setCenter(root);load();
    }
    private void load(){table.setData(FXCollections.observableArrayList(repository.findAll()));}
}

package ao.allon.kubata.inventario.ui;

import ao.allon.kubata.core.ui.table.AdvancedTableView;
import ao.allon.kubata.inventario.domain.Estoque;
import ao.allon.kubata.inventario.repository.EstoqueRepository;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.List;

@Component
public class ValorizacaoStockView extends BorderPane {
    private final EstoqueRepository repository; private final AdvancedTableView<LinhaValor> table=new AdvancedTableView<>();
    public ValorizacaoStockView(EstoqueRepository repository){this.repository=repository;build();}
    private void build(){
        InventarioUI.installCss(this);setPadding(new Insets(18));
        VBox root=new VBox(14);Label title=new Label("Valorização de Stock");title.getStyleClass().add("inventario-page-title");
        Label sub=new Label("Valor económico do stock pelo custo registado, com detalhe por artigo e armazém.");sub.getStyleClass().add("inventario-page-subtitle");
        Button refresh=InventarioUI.primaryButton("Atualizar");refresh.setOnAction(e->load());
        table.setData(FXCollections.observableArrayList());
        TableColumn<LinhaValor,String> p=new TableColumn<>("Artigo");p.setCellValueFactory(c->new javafx.beans.property.SimpleStringProperty(c.getValue().produto()));
        TableColumn<LinhaValor,String> a=new TableColumn<>("Armazém");a.setCellValueFactory(c->new javafx.beans.property.SimpleStringProperty(c.getValue().armazem()));
        TableColumn<LinhaValor,String> q=new TableColumn<>("Quantidade");q.setCellValueFactory(c->new javafx.beans.property.SimpleStringProperty(String.valueOf(c.getValue().quantidade())));
        TableColumn<LinhaValor,String> c=new TableColumn<>("Custo");c.setCellValueFactory(x->new javafx.beans.property.SimpleStringProperty(InventarioUI.money(x.getValue().custo())));
        TableColumn<LinhaValor,String> v=new TableColumn<>("Valor");v.setCellValueFactory(x->new javafx.beans.property.SimpleStringProperty(InventarioUI.money(x.getValue().valor())));
        table.getColumns().addAll(p,a,q,c,v);InventarioUI.styleTable(table);VBox.setVgrow(table,Priority.ALWAYS);
        root.getChildren().addAll(new VBox(4,title,sub),refresh,table);setCenter(root);load();
    }
    private void load(){List<LinhaValor> rows=repository.findAll().stream().map(e->{BigDecimal custo=e.getPrecoCompra()==null?BigDecimal.ZERO:e.getPrecoCompra();int q=e.getQuantidade()==null?0:e.getQuantidade();return new LinhaValor(e.getProduto().getNome(),e.getArmazem().getNome(),q,custo,custo.multiply(BigDecimal.valueOf(q)));}).toList();table.setData(FXCollections.observableArrayList(rows));}
    private record LinhaValor(String produto,String armazem,int quantidade,BigDecimal custo,BigDecimal valor){}
}

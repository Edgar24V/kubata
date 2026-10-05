package ao.allon.kubata.inventario.ui;

import ao.allon.kubata.core.ui.table.AdvancedTableView;
import ao.allon.kubata.inventario.domain.MovimentoStock;
import ao.allon.kubata.inventario.repository.MovimentoStockRepository;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.springframework.stereotype.Component;

@Component
public class MovimentosStockView extends BorderPane {
    private final MovimentoStockRepository repository;
    private final AdvancedTableView<MovimentoStock> table=new AdvancedTableView<>();
    public MovimentosStockView(MovimentoStockRepository repository){this.repository=repository;build();}
    private void build(){
        InventarioUI.installCss(this);setPadding(new Insets(18));
        VBox root=new VBox(14);
        Label title=new Label("Movimentos de Stock");title.getStyleClass().add("inventario-page-title");
        Label sub=new Label("Livro cronológico de entradas, saídas, transferências e regularizações.");sub.getStyleClass().add("inventario-page-subtitle");
        Button refresh=InventarioUI.primaryButton("Atualizar");refresh.setOnAction(e->load());
        table.setData(FXCollections.observableArrayList());
        TableColumn<MovimentoStock,String> data=new TableColumn<>("Data");data.setCellValueFactory(c->new javafx.beans.property.SimpleStringProperty(InventarioUI.dateTime(c.getValue().getDataMovimento())));
        TableColumn<MovimentoStock,String> tipo=new TableColumn<>("Movimento");tipo.setCellValueFactory(c->new javafx.beans.property.SimpleStringProperty(c.getValue().getTipoMovimento()==null?"":c.getValue().getTipoMovimento().name()));
        TableColumn<MovimentoStock,String> prod=new TableColumn<>("Artigo");prod.setCellValueFactory(c->new javafx.beans.property.SimpleStringProperty(c.getValue().getProduto()==null?"":c.getValue().getProduto().getNome()));
        TableColumn<MovimentoStock,String> arm=new TableColumn<>("Armazém");arm.setCellValueFactory(c->new javafx.beans.property.SimpleStringProperty(c.getValue().getArmazem()==null?"":c.getValue().getArmazem().getNome()));
        TableColumn<MovimentoStock,String> q=new TableColumn<>("Qtd.");q.setCellValueFactory(c->new javafx.beans.property.SimpleStringProperty(String.valueOf(c.getValue().getQuantidade())));
        TableColumn<MovimentoStock,String> saldo=new TableColumn<>("Saldo");saldo.setCellValueFactory(c->new javafx.beans.property.SimpleStringProperty(String.valueOf(c.getValue().getSaldoAtual())));
        TableColumn<MovimentoStock,String> obs=new TableColumn<>("Observação");obs.setCellValueFactory(c->new javafx.beans.property.SimpleStringProperty(c.getValue().getObservacao()));
        table.getColumns().addAll(data,tipo,prod,arm,q,saldo,obs);InventarioUI.styleTable(table);VBox.setVgrow(table,Priority.ALWAYS);
        HBox bar=new HBox(refresh);root.getChildren().addAll(new VBox(4,title,sub),bar,table);setCenter(root);load();
    }
    private void load(){table.setData(FXCollections.observableArrayList(repository.findTop250ByOrderByDataMovimentoDesc()));}
}

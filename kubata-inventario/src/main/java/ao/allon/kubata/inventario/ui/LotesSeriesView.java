package ao.allon.kubata.inventario.ui;

import ao.allon.kubata.core.ui.table.AdvancedTableView;
import ao.allon.kubata.inventario.domain.*;
import ao.allon.kubata.inventario.repository.*;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.springframework.stereotype.Component;

@Component
public class LotesSeriesView extends BorderPane {
    private final LoteStockRepository lotes;
    private final NumeroSerieStockRepository series;
    public LotesSeriesView(LoteStockRepository lotes,NumeroSerieStockRepository series){this.lotes=lotes;this.series=series;build();}
    private void build(){
        InventarioUI.installCss(this);setPadding(new Insets(18));
        Label title=new Label("Lotes & Números de Série");title.getStyleClass().add("inventario-page-title");
        Label sub=new Label("Rastreabilidade dos artigos por lote, validade e identificador individual.");sub.getStyleClass().add("inventario-page-subtitle");
        AdvancedTableView<LoteStock> lt=new AdvancedTableView<>();lt.setData(FXCollections.observableArrayList(lotes.findAll()));
        TableColumn<LoteStock,String> lp=new TableColumn<>("Artigo");lp.setCellValueFactory(c->new javafx.beans.property.SimpleStringProperty(c.getValue().getProduto().getNome()));
        TableColumn<LoteStock,String> lc=new TableColumn<>("Lote");lc.setCellValueFactory(c->new javafx.beans.property.SimpleStringProperty(c.getValue().getCodigo()));
        TableColumn<LoteStock,String> lv=new TableColumn<>("Validade");lv.setCellValueFactory(c->new javafx.beans.property.SimpleStringProperty(c.getValue().getDataValidade()==null?"":c.getValue().getDataValidade().toString()));
        lt.getColumns().addAll(lp,lc,lv);InventarioUI.styleTable(lt);
        AdvancedTableView<NumeroSerieStock> st=new AdvancedTableView<>();st.setData(FXCollections.observableArrayList(series.findAll()));
        TableColumn<NumeroSerieStock,String> sp=new TableColumn<>("Artigo");sp.setCellValueFactory(c->new javafx.beans.property.SimpleStringProperty(c.getValue().getProduto().getNome()));
        TableColumn<NumeroSerieStock,String> sn=new TableColumn<>("Nº Série");sn.setCellValueFactory(c->new javafx.beans.property.SimpleStringProperty(c.getValue().getNumero()));
        TableColumn<NumeroSerieStock,String> se=new TableColumn<>("Estado");se.setCellValueFactory(c->new javafx.beans.property.SimpleStringProperty(c.getValue().getEstadoSerie()));
        st.getColumns().addAll(sp,sn,se);InventarioUI.styleTable(st);
        TabPane tabs=new TabPane(new Tab("Lotes",lt),new Tab("Números de Série",st));tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        setCenter(new VBox(4,title,sub,tabs));VBox.setVgrow(tabs,Priority.ALWAYS);
    }
}

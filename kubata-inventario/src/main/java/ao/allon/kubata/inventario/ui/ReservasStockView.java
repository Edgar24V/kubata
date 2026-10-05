package ao.allon.kubata.inventario.ui;

import ao.allon.kubata.core.ui.table.AdvancedTableView;
import ao.allon.kubata.inventario.domain.*;
import ao.allon.kubata.inventario.enums.TipoReserva;
import ao.allon.kubata.inventario.repository.*;
import ao.allon.kubata.inventario.service.StockService;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ReservasStockView extends BorderPane {
    private final ReservaStockRepository repository;
    private final StockService stockService;
    private final ProdutoRepository produtoRepository;
    private final ArmazemRepository armazemRepository;
    private final AdvancedTableView<ReservaStock> table=new AdvancedTableView<>();

    public ReservasStockView(ReservaStockRepository repository, StockService stockService,
                             ProdutoRepository produtoRepository, ArmazemRepository armazemRepository){
        this.repository=repository;this.stockService=stockService;this.produtoRepository=produtoRepository;this.armazemRepository=armazemRepository;build();
    }
    private void build(){
        InventarioUI.installCss(this);setPadding(new Insets(18));
        VBox root=new VBox(14);
        Label title=new Label("Reservas de Stock");title.getStyleClass().add("inventario-page-title");
        Label sub=new Label("Stock comprometido por encomendas, produção ou necessidades internas.");sub.getStyleClass().add("inventario-page-subtitle");
        Button nova=InventarioUI.primaryButton("Nova reserva");nova.setOnAction(e->nova());
        Button libertar=InventarioUI.secondaryButton("Libertar");libertar.setOnAction(e->{var x=table.getSelectionModel().getSelectedItem();if(x!=null){try{stockService.libertarReserva(x.getId());load();}catch(Exception ex){InventarioUI.error("Reserva",ex.getMessage());}}});
        Button refresh=InventarioUI.secondaryButton("Atualizar");refresh.setOnAction(e->load());
        table.setData(FXCollections.observableArrayList());
        TableColumn<ReservaStock,String> data=new TableColumn<>("Data");data.setCellValueFactory(c->new javafx.beans.property.SimpleStringProperty(InventarioUI.dateTime(c.getValue().getDataReserva())));
        TableColumn<ReservaStock,String> prod=new TableColumn<>("Artigo");prod.setCellValueFactory(c->new javafx.beans.property.SimpleStringProperty(c.getValue().getProduto()==null?"":c.getValue().getProduto().getNome()));
        TableColumn<ReservaStock,String> arm=new TableColumn<>("Armazém");arm.setCellValueFactory(c->new javafx.beans.property.SimpleStringProperty(c.getValue().getArmazem()==null?"":c.getValue().getArmazem().getNome()));
        TableColumn<ReservaStock,String> tipo=new TableColumn<>("Tipo");tipo.setCellValueFactory(c->new javafx.beans.property.SimpleStringProperty(c.getValue().getTipo().name()));
        TableColumn<ReservaStock,String> q=new TableColumn<>("Quantidade");q.setCellValueFactory(c->new javafx.beans.property.SimpleStringProperty(String.valueOf(c.getValue().getQuantidade())));
        TableColumn<ReservaStock,String> estado=new TableColumn<>("Estado");estado.setCellValueFactory(c->new javafx.beans.property.SimpleStringProperty(c.getValue().getEstado().name()));
        table.getColumns().addAll(data,prod,arm,tipo,q,estado);InventarioUI.styleTable(table);VBox.setVgrow(table,Priority.ALWAYS);
        root.getChildren().addAll(new VBox(4,title,sub),new HBox(8,nova,libertar,refresh),table);setCenter(root);load();
    }
    private void load(){table.setData(FXCollections.observableArrayList(repository.findAll(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC,"dataReserva"))));}
    private void nova(){
        Dialog<ButtonType>d=new Dialog<>();d.setTitle("Nova reserva de stock");d.getDialogPane().getStyleClass().add("inventario-dialog");d.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL,ButtonType.OK);
        ComboBox<Produto> produto=new ComboBox<>(FXCollections.observableArrayList(produtoRepository.findAll()));
        ComboBox<Armazem> arm=new ComboBox<>(FXCollections.observableArrayList(armazemRepository.findAll()));
        ComboBox<TipoReserva> tipo=new ComboBox<>(FXCollections.observableArrayList(TipoReserva.values()));tipo.setValue(TipoReserva.VENDA);
        TextField qtd=InventarioUI.field("Quantidade");
        GridPane g=new GridPane();g.setHgap(10);g.setVgap(9);g.addRow(0,new Label("Artigo"),produto);g.addRow(1,new Label("Armazém"),arm);g.addRow(2,new Label("Tipo"),tipo);g.addRow(3,new Label("Quantidade"),qtd);d.getDialogPane().setContent(g);
        Button ok=(Button)d.getDialogPane().lookupButton(ButtonType.OK);ok.addEventFilter(javafx.event.ActionEvent.ACTION,e->{try{if(produto.getValue()==null||arm.getValue()==null)throw new IllegalArgumentException("Selecione artigo e armazém.");stockService.reservar(produto.getValue().getId(),arm.getValue().getId(),new BigDecimal(qtd.getText().replace(',','.')),tipo.getValue(),null,null,"Reserva criada no módulo de Inventário.");load();}catch(Exception ex){e.consume();InventarioUI.error("Reserva",ex.getMessage());}});d.showAndWait();
    }
}

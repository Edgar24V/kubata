package ao.allon.kubata.inventario.ui;

import ao.allon.kubata.core.ui.table.AdvancedTableView;
import ao.allon.kubata.inventario.domain.*;
import ao.allon.kubata.inventario.enums.EstadoTransferencia;
import ao.allon.kubata.inventario.repository.*;
import ao.allon.kubata.inventario.service.StockService;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TransferenciasStockView extends BorderPane {
    private final TransferenciaStockRepository repository;
    private final StockService stockService;
    private final ProdutoRepository produtoRepository;
    private final ArmazemRepository armazemRepository;
    private final AdvancedTableView<TransferenciaStock> table=new AdvancedTableView<>();

    public TransferenciasStockView(TransferenciaStockRepository repository, StockService stockService,
                                   ProdutoRepository produtoRepository, ArmazemRepository armazemRepository){
        this.repository=repository;this.stockService=stockService;this.produtoRepository=produtoRepository;this.armazemRepository=armazemRepository;build();
    }
    private void build(){
        InventarioUI.installCss(this);setPadding(new Insets(18));VBox root=new VBox(14);
        Label title=new Label("Transferências");title.getStyleClass().add("inventario-page-title");
        Label sub=new Label("Movimentação controlada entre armazéns, com histórico documental.");sub.getStyleClass().add("inventario-page-subtitle");
        Button nova=InventarioUI.primaryButton("Nova transferência");nova.setOnAction(e->nova());
        Button refresh=InventarioUI.secondaryButton("Atualizar");refresh.setOnAction(e->load());
        table.setData(FXCollections.observableArrayList());
        TableColumn<TransferenciaStock,String> num=new TableColumn<>("Nº");num.setCellValueFactory(c->new javafx.beans.property.SimpleStringProperty(c.getValue().getNumero()));
        TableColumn<TransferenciaStock,String> o=new TableColumn<>("Origem");o.setCellValueFactory(c->new javafx.beans.property.SimpleStringProperty(c.getValue().getArmazemOrigem().getNome()));
        TableColumn<TransferenciaStock,String> d=new TableColumn<>("Destino");d.setCellValueFactory(c->new javafx.beans.property.SimpleStringProperty(c.getValue().getArmazemDestino().getNome()));
        TableColumn<TransferenciaStock,String> estado=new TableColumn<>("Estado");estado.setCellValueFactory(c->new javafx.beans.property.SimpleStringProperty(c.getValue().getEstado().name()));
        TableColumn<TransferenciaStock,String> data=new TableColumn<>("Criada");data.setCellValueFactory(c->new javafx.beans.property.SimpleStringProperty(InventarioUI.dateTime(c.getValue().getCreatedAt())));
        table.getColumns().addAll(num,o,d,estado,data);InventarioUI.styleTable(table);VBox.setVgrow(table,Priority.ALWAYS);
        root.getChildren().addAll(new VBox(4,title,sub),new HBox(8,nova,refresh),table);setCenter(root);load();
    }
    private void load(){table.setData(FXCollections.observableArrayList(repository.findTop100ByOrderByCreatedAtDesc()));}
    private void nova(){
        Dialog<ButtonType>x=new Dialog<>();x.setTitle("Nova transferência");x.getDialogPane().getStyleClass().add("inventario-dialog");x.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL,ButtonType.OK);
        ComboBox<Produto> produto=new ComboBox<>(FXCollections.observableArrayList(produtoRepository.findAll()));
        ComboBox<Armazem> origem=new ComboBox<>(FXCollections.observableArrayList(armazemRepository.findAll()));
        ComboBox<Armazem> destino=new ComboBox<>(FXCollections.observableArrayList(armazemRepository.findAll()));
        TextField qtd=InventarioUI.field("Quantidade");TextField lote=InventarioUI.field("Lote (opcional)");TextField obs=InventarioUI.field("Observação");
        GridPane g=new GridPane();g.setHgap(10);g.setVgap(9);g.addRow(0,new Label("Artigo"),produto);g.addRow(1,new Label("Origem"),origem);g.addRow(2,new Label("Destino"),destino);g.addRow(3,new Label("Quantidade"),qtd);g.addRow(4,new Label("Lote"),lote);g.addRow(5,new Label("Observação"),obs);x.getDialogPane().setContent(g);
        Button ok=(Button)x.getDialogPane().lookupButton(ButtonType.OK);ok.addEventFilter(javafx.event.ActionEvent.ACTION,e->{try{
            if(produto.getValue()==null||origem.getValue()==null||destino.getValue()==null)throw new IllegalArgumentException("Selecione artigo, origem e destino.");
            var result=stockService.transferir(produto.getValue().getId(),origem.getValue().getId(),destino.getValue().getId(),Integer.parseInt(qtd.getText().trim()),lote.getText().isBlank()?null:lote.getText().trim(),obs.getText());
            TransferenciaStock t=new TransferenciaStock();t.setNumero("TRF-"+System.currentTimeMillis());t.setArmazemOrigem(origem.getValue());t.setArmazemDestino(destino.getValue());t.setEstado(EstadoTransferencia.RECEBIDA);t.setDataExpedicao(java.time.LocalDateTime.now());t.setDataRececao(java.time.LocalDateTime.now());t.setObservacoes(obs.getText());
            TransferenciaStockLinha l=new TransferenciaStockLinha();l.setProduto(produto.getValue());l.setQuantidade(java.math.BigDecimal.valueOf(Integer.parseInt(qtd.getText().trim())));l.setLote(result.saida().getLote());t.addLinha(l);repository.save(t);load();
        }catch(Exception ex){e.consume();InventarioUI.error("Transferência",ex.getMessage());}});x.showAndWait();
    }
}

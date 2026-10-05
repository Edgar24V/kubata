package ao.allon.kubata.inventario.ui;

import ao.allon.kubata.core.ui.table.AdvancedTableView;
import ao.allon.kubata.inventario.domain.*;
import ao.allon.kubata.inventario.enums.TipoInventario;
import ao.allon.kubata.inventario.repository.*;
import ao.allon.kubata.inventario.service.InventarioFisicoService;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class InventariosFisicosView extends BorderPane {
    private final InventarioFisicoService service;
    private final ArmazemRepository armazemRepository;
    private final InventarioFisicoRepository repository;
    private final AdvancedTableView<InventarioFisico> table=new AdvancedTableView<>();
    private final AdvancedTableView<InventarioFisicoLinha> linhas=new AdvancedTableView<>();

    public InventariosFisicosView(InventarioFisicoService service,ArmazemRepository armazemRepository,InventarioFisicoRepository repository){
        this.service=service;this.armazemRepository=armazemRepository;this.repository=repository;build();
    }
    private void build(){
        InventarioUI.installCss(this);setPadding(new Insets(18));
        VBox root=new VBox(14);
        Label title=new Label("Inventários físicos");title.getStyleClass().add("inventario-page-title");
        Label sub=new Label("Preparação, contagem, apuramento de diferenças e regularização do stock.");sub.getStyleClass().add("inventario-page-subtitle");
        Button iniciar=InventarioUI.primaryButton("Iniciar inventário");iniciar.setOnAction(e->iniciar());
        Button contar=InventarioUI.secondaryButton("Registar contagem");contar.setOnAction(e->contar());
        Button fechar=InventarioUI.secondaryButton("Fechar inventário");fechar.setOnAction(e->fechar());
        Button refresh=InventarioUI.secondaryButton("Atualizar");refresh.setOnAction(e->load());
        table.setData(FXCollections.observableArrayList());
        TableColumn<InventarioFisico,String> num=new TableColumn<>("Nº");num.setCellValueFactory(c->new javafx.beans.property.SimpleStringProperty(c.getValue().getNumero()));
        TableColumn<InventarioFisico,String> tipo=new TableColumn<>("Tipo");tipo.setCellValueFactory(c->new javafx.beans.property.SimpleStringProperty(c.getValue().getTipo().name()));
        TableColumn<InventarioFisico,String> estado=new TableColumn<>("Estado");estado.setCellValueFactory(c->new javafx.beans.property.SimpleStringProperty(c.getValue().getEstado().name()));
        TableColumn<InventarioFisico,String> data=new TableColumn<>("Data");data.setCellValueFactory(c->new javafx.beans.property.SimpleStringProperty(c.getValue().getDataInventario().toString()));
        table.getColumns().addAll(num,tipo,estado,data);InventarioUI.styleTable(table);table.getSelectionModel().selectedItemProperty().addListener((o,a,b)->loadLinhas());
        linhas.setData(FXCollections.observableArrayList());
        TableColumn<InventarioFisicoLinha,String> lp=new TableColumn<>("Artigo");lp.setCellValueFactory(c->new javafx.beans.property.SimpleStringProperty(c.getValue().getProduto().getNome()));
        TableColumn<InventarioFisicoLinha,String> ls=new TableColumn<>("Sistema");ls.setCellValueFactory(c->new javafx.beans.property.SimpleStringProperty(c.getValue().getStockSistema().toString()));
        TableColumn<InventarioFisicoLinha,String> lc=new TableColumn<>("Contado");lc.setCellValueFactory(c->new javafx.beans.property.SimpleStringProperty(c.getValue().getQuantidadeContada()==null?"":c.getValue().getQuantidadeContada().toString()));
        TableColumn<InventarioFisicoLinha,String> ld=new TableColumn<>("Diferença");ld.setCellValueFactory(c->new javafx.beans.property.SimpleStringProperty(c.getValue().getDiferenca().toString()));
        linhas.getColumns().addAll(lp,ls,lc,ld);InventarioUI.styleTable(linhas);
        VBox.setVgrow(table,Priority.ALWAYS);VBox.setVgrow(linhas,Priority.ALWAYS);
        SplitPane split=new SplitPane(table,linhas);split.setDividerPositions(.42);
        root.getChildren().addAll(new VBox(4,title,sub),new HBox(8,iniciar,contar,fechar,refresh),split);VBox.setVgrow(split,Priority.ALWAYS);setCenter(root);load();
    }
    private void load(){table.setData(FXCollections.observableArrayList(service.listar()));loadLinhas();}
    private void loadLinhas(){var i=table.getSelectionModel().getSelectedItem();linhas.setData(FXCollections.observableArrayList(i==null?java.util.List.of():service.linhas(i.getId())));}
    private void iniciar(){
        Dialog<ButtonType>d=new Dialog<>();d.setTitle("Preparar inventário físico");d.getDialogPane().getStyleClass().add("inventario-dialog");d.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL,ButtonType.OK);
        ComboBox<TipoInventario>tipo=new ComboBox<>(FXCollections.observableArrayList(TipoInventario.values()));tipo.setValue(TipoInventario.TOTAL);
        ComboBox<Armazem>arm=new ComboBox<>(FXCollections.observableArrayList(armazemRepository.findAll()));arm.getItems().add(0,null);
        GridPane g=new GridPane();g.setHgap(10);g.setVgap(9);g.addRow(0,new Label("Tipo"),tipo);g.addRow(1,new Label("Armazém"),arm);d.getDialogPane().setContent(g);
        Button ok=(Button)d.getDialogPane().lookupButton(ButtonType.OK);ok.addEventFilter(javafx.event.ActionEvent.ACTION,e->{try{service.iniciar(tipo.getValue(),arm.getValue()==null?null:arm.getValue().getId());load();}catch(Exception ex){e.consume();InventarioUI.error("Inventário",ex.getMessage());}});d.showAndWait();
    }
    private void contar(){
        var l=linhas.getSelectionModel().getSelectedItem();if(l==null){InventarioUI.error("Contagem","Selecione uma linha.");return;}
        TextInputDialog d=new TextInputDialog(l.getQuantidadeContada()==null?"":l.getQuantidadeContada().toString());d.setTitle("Registar contagem");d.setHeaderText(l.getProduto().getNome());d.setContentText("Quantidade contada:");
        d.showAndWait().ifPresent(v->{try{service.registarContagem(l.getId(),new BigDecimal(v.replace(',','.')));loadLinhas();}catch(Exception ex){InventarioUI.error("Contagem",ex.getMessage());}});
    }
    private void fechar(){var i=table.getSelectionModel().getSelectedItem();if(i==null){InventarioUI.error("Inventário","Selecione um inventário.");return;}try{service.fechar(i.getId());load();InventarioUI.info("Inventário","Inventário fechado e diferenças regularizadas.");}catch(Exception ex){InventarioUI.error("Inventário",ex.getMessage());}}
}

package ao.allon.kubata.inventario.ui;

import ao.allon.kubata.core.ui.table.AdvancedTableView;
import ao.allon.kubata.inventario.domain.Armazem;
import ao.allon.kubata.inventario.enums.Provincia;
import ao.allon.kubata.inventario.repository.ArmazemRepository;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.springframework.stereotype.Component;

@Component
public class ArmazensView extends BorderPane {
    private final ArmazemRepository repository;
    private final AdvancedTableView<Armazem> table = new AdvancedTableView<>();

    public ArmazensView(ArmazemRepository repository) {
        this.repository=repository; build();
    }

    private void build() {
        InventarioUI.installCss(this); setPadding(new Insets(18));
        VBox root=new VBox(14);
        Label title=new Label("Armazéns"); title.getStyleClass().add("inventario-page-title");
        Label sub=new Label("Estrutura física e lógica de armazenamento, com regras operacionais."); sub.getStyleClass().add("inventario-page-subtitle");
        Button novo=InventarioUI.primaryButton("Novo armazém"); novo.setOnAction(e->editar(null));
        Button editar=InventarioUI.secondaryButton("Editar"); editar.setOnAction(e->editar(table.getSelectionModel().getSelectedItem()));
        Button refresh=InventarioUI.secondaryButton("Atualizar"); refresh.setOnAction(e->load());
        HBox bar=new HBox(8,novo,editar,refresh); bar.setAlignment(Pos.CENTER_LEFT);

        table.setData(FXCollections.observableArrayList());
        TableColumn<Armazem,String> nome=new TableColumn<>("Armazém"); nome.setCellValueFactory(c->new javafx.beans.property.SimpleStringProperty(c.getValue().getNome()));
        TableColumn<Armazem,String> prov=new TableColumn<>("Província"); prov.setCellValueFactory(c->new javafx.beans.property.SimpleStringProperty(c.getValue().getProvincia()==null?"":c.getValue().getProvincia().name()));
        TableColumn<Armazem,String> mun=new TableColumn<>("Município"); mun.setCellValueFactory(c->new javafx.beans.property.SimpleStringProperty(c.getValue().getMunicipio()));
        TableColumn<Armazem,String> principal=new TableColumn<>("Principal"); principal.setCellValueFactory(c->new javafx.beans.property.SimpleStringProperty(Boolean.TRUE.equals(c.getValue().getIsPrincipal())?"SIM":"NÃO"));
        table.getColumns().addAll(nome,prov,mun,principal); InventarioUI.styleTable(table); VBox.setVgrow(table,Priority.ALWAYS);
        root.getChildren().addAll(new VBox(4,title,sub),bar,table); setCenter(root); load();
    }

    private void load(){table.setData(FXCollections.observableArrayList(repository.findAll()));}

    private void editar(Armazem a){
        Dialog<ButtonType>d=new Dialog<>(); d.setTitle(a==null?"Novo Armazém":"Editar Armazém"); d.getDialogPane().getStyleClass().add("inventario-dialog");
        d.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL,ButtonType.OK);
        TextField nome=InventarioUI.field("Nome do armazém"), municipio=InventarioUI.field("Município"), endereco=InventarioUI.field("Endereço"), resp=InventarioUI.field("Responsável"), tel=InventarioUI.field("Telefone");
        ComboBox<Provincia> provincia=new ComboBox<>(FXCollections.observableArrayList(Provincia.values())); provincia.getStyleClass().add("inventario-field");
        CheckBox principal=new CheckBox("Definir como armazém principal");
        if(a!=null){nome.setText(a.getNome());municipio.setText(a.getMunicipio());endereco.setText(a.getEndereco());resp.setText(a.getResponsavel());tel.setText(a.getTelefone());provincia.setValue(a.getProvincia());principal.setSelected(Boolean.TRUE.equals(a.getIsPrincipal()));}
        GridPane g=new GridPane();g.setHgap(10);g.setVgap(9);
        g.addRow(0,new Label("Nome"),nome);g.addRow(1,new Label("Província"),provincia);g.addRow(2,new Label("Município"),municipio);g.addRow(3,new Label("Endereço"),endereco);g.addRow(4,new Label("Responsável"),resp);g.addRow(5,new Label("Telefone"),tel);g.add(principal,1,6);
        d.getDialogPane().setContent(g);
        Button ok=(Button)d.getDialogPane().lookupButton(ButtonType.OK);
        ok.addEventFilter(javafx.event.ActionEvent.ACTION,e->{try{
            if(nome.getText().isBlank())throw new IllegalArgumentException("O nome é obrigatório.");
            Armazem x=a==null?new Armazem():a;x.setNome(nome.getText().trim());x.setProvincia(provincia.getValue());x.setMunicipio(municipio.getText().trim());x.setEndereco(endereco.getText().trim());x.setResponsavel(resp.getText().trim());x.setTelefone(tel.getText().trim());x.setIsPrincipal(principal.isSelected());
            repository.save(x);load();
        }catch(Exception ex){e.consume();InventarioUI.error("Dados inválidos",ex.getMessage());}});
        d.showAndWait();
    }
}

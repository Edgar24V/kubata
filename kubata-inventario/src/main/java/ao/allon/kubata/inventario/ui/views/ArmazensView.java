package ao.allon.kubata.inventario.ui.views;

import ao.allon.kubata.inventario.domain.Armazem;
import ao.allon.kubata.inventario.enums.Provincia;
import ao.allon.kubata.inventario.service.ArmazemService;
import ao.allon.kubata.inventario.ui.modal.ModalManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.springframework.stereotype.Component;

@Component
public class ArmazensView extends BorderPane {
    private final ArmazemService service;
    private final ModalManager modalManager;
    private final ObservableList<Armazem> data = FXCollections.observableArrayList();
    private final TableView<Armazem> table = new TableView<>(data);

    public ArmazensView(ArmazemService service, ModalManager modalManager) {
        this.service = service;
        this.modalManager = modalManager;
        build();
        refreshData();
    }

    private void build() {
        getStyleClass().add("inventario-page");
        setPadding(new Insets(22));

        Label title = new Label("Armazéns");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Organização física e logística de stock");
        subtitle.getStyleClass().add("page-subtitle");

        Button novo = new Button("Novo armazém");
        novo.getStyleClass().add("primary-action");
        novo.setOnAction(e -> novo());
        Button refresh = new Button("Atualizar");
        refresh.setOnAction(e -> refreshData());

        HBox actions = new HBox(10, novo, refresh);
        VBox header = new VBox(4, title, subtitle, actions);
        header.setSpacing(10);

        TableColumn<Armazem,String> nome = new TableColumn<>("Armazém");
        nome.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNome()));
        TableColumn<Armazem,String> provincia = new TableColumn<>("Província");
        provincia.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getProvincia() == null ? "" : c.getValue().getProvincia().name()));
        TableColumn<Armazem,String> municipio = new TableColumn<>("Município");
        municipio.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getMunicipio()));
        TableColumn<Armazem,String> principal = new TableColumn<>("Principal");
        principal.setCellValueFactory(c -> new SimpleStringProperty(
                Boolean.TRUE.equals(c.getValue().getIsPrincipal()) ? "Sim" : "Não"));
        table.getColumns().addAll(nome, provincia, municipio, principal);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("Nenhum armazém registado."));

        setCenter(new VBox(16, header, table));
        VBox.setVgrow(table, Priority.ALWAYS);
    }

    public void refreshData() {
        data.setAll(service.findAll());
    }

    private void novo() {
        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(8));
        TextField nome = new TextField();
        ComboBox<Provincia> provincia = new ComboBox<>(FXCollections.observableArrayList(Provincia.values()));
        TextField municipio = new TextField();
        TextField endereco = new TextField();
        CheckBox principal = new CheckBox("Definir como armazém principal");

        grid.addRow(0, new Label("Nome:*"), nome);
        grid.addRow(1, new Label("Província:"), provincia);
        grid.addRow(2, new Label("Município:"), municipio);
        grid.addRow(3, new Label("Endereço:"), endereco);
        grid.addRow(4, new Label("Estado:"), principal);

        Dialog<ButtonType> dialog = modalManager.form(this, "Novo armazém", grid);
        if (dialog.showAndWait().filter(ButtonType.OK::equals).isEmpty()) return;

        try {
            Armazem a = new Armazem();
            a.setNome(nome.getText().trim());
            a.setProvincia(provincia.getValue());
            a.setMunicipio(municipio.getText().trim());
            a.setEndereco(endereco.getText().trim());
            a.setIsPrincipal(principal.isSelected());
            service.save(a);
            refreshData();
        } catch (Exception ex) {
            modalManager.error(this, "Armazém", "Não foi possível gravar: " + ex.getMessage());
        }
    }
}

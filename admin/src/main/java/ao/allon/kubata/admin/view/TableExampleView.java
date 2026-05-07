package ao.allon.kubata.admin.view;

import ao.allon.kubata.core.ui.table.AdvancedTableView;
import ao.allon.kubata.core.ui.table.TableUtils;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.layout.VBox;
import javafx.util.converter.DefaultStringConverter;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class TableExampleView extends VBox {

    public TableExampleView() {
        setSpacing(20);
        setPadding(new Insets(20));
        getStyleClass().add("content-area");

        Label title = new Label("Exemplo de Tabela Avançada");
        title.getStyleClass().add("label-title");

        // Dados de exemplo
        ObservableList<Person> data = FXCollections.observableArrayList(
            new Person("João Silva", "Admin", true, LocalDate.now()),
            new Person("Maria Santos", "User", false, LocalDate.now().minusDays(5)),
            new Person("Pedro Costa", "Guest", true, LocalDate.now().minusMonths(1))
        );

        // Tabela Avançada
        AdvancedTableView<Person> table = new AdvancedTableView<>(data);
        table.setEditable(true);
        TableUtils.standardize(table);

        // Colunas
        TableColumn<Person, String> colNome = TableUtils.createTextColumn("Nome", col -> new SimpleStringProperty(col.getValue().getName()));
        colNome.setOnEditCommit(event -> {
            Person p = event.getRowValue();
            p.setName(event.getNewValue());
            System.out.println("Nome alterado para: " + p.getName());
        });

        TableColumn<Person, String> colRole = TableUtils.createComboColumn("Perfil", 
                col -> new SimpleObjectProperty<>(col.getValue().getRole()), 
                FXCollections.observableArrayList("Admin", "User", "Guest"), 
                new DefaultStringConverter());
        colRole.setOnEditCommit(event -> {
            Person p = event.getRowValue();
            p.setRole(event.getNewValue());
            System.out.println("Perfil alterado para: " + p.getRole());
        });

        TableColumn<Person, Boolean> colAtivo = TableUtils.createCheckColumn("Ativo", col -> new SimpleBooleanProperty(col.getValue().isActive()));
        // CheckBox columns update data automatically if using PropertyValueFactory with SimpleBooleanProperty, 
        // but here we are using custom factory. TableUtils.createCheckColumn usually handles this.

        TableColumn<Person, LocalDate> colData = TableUtils.createDateColumn("Data Cadastro", col -> new SimpleObjectProperty<>(col.getValue().getDate()));
        colData.setOnEditCommit(event -> {
            Person p = event.getRowValue();
            p.setDate(event.getNewValue());
            System.out.println("Data alterada para: " + p.getDate());
        });

        table.getColumns().addAll(colNome, colRole, colAtivo, colData);

        getChildren().addAll(title, table);
    }

    // Classe POJO simples para o exemplo
    public static class Person {
        private String name;
        private String role;
        private boolean active;
        private LocalDate date;

        public Person(String name, String role, boolean active, LocalDate date) {
            this.name = name;
            this.role = role;
            this.active = active;
            this.date = date;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
        public LocalDate getDate() { return date; }
        public void setDate(LocalDate date) { this.date = date; }

        @Override
        public String toString() {
            return name + " (" + role + ")";
        }
    }
}

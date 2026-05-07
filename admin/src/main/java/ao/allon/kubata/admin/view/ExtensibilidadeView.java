package ao.allon.kubata.admin.view;

import ao.allon.kubata.admin.extensibilidade.AdministradorExtensibilidadeRegistry;
import ao.allon.kubata.admin.extensibilidade.AplicacaoAdministrador;
import ao.allon.kubata.admin.extensibilidade.MockAplicacao;
import ao.allon.kubata.admin.ui.modal.ModalManager;
import ao.allon.kubata.admin.ui.util.IconUtils;
import ao.allon.kubata.core.ui.table.AdvancedTableView;
import ao.allon.kubata.core.ui.table.TableUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Vista para gestão de Aplicações Externas (Extensibilidade do Administrador).
 */
@Component
public class ExtensibilidadeView extends VBox {

    private final AdministradorExtensibilidadeRegistry registry;
    private final ModalManager modalManager;

    private AdvancedTableView<AplicacaoAdministrador> table;
    private ObservableList<AplicacaoAdministrador> apps;

    public ExtensibilidadeView(AdministradorExtensibilidadeRegistry registry, ModalManager modalManager) {
        this.registry = registry;
        this.modalManager = modalManager;
        this.apps = FXCollections.observableArrayList();
        
        buildUI();
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        if (apps.isEmpty() && getScene() != null) {
            refreshList();
        }
    }

    private void buildUI() {
        setSpacing(0);
        
        HBox toolbar = new HBox(10);
        toolbar.getStyleClass().add("header-box");
        toolbar.setPadding(new Insets(10, 15, 10, 15));
        toolbar.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Extensibilidade do Administrador");
        title.getStyleClass().add("h3");

        Button btnRegistar = new Button("Registar Demo", IconUtils.icon(Feather.PLUS, IconUtils.SIZE_SMALL));
        btnRegistar.getStyleClass().add("button-primary");
        btnRegistar.setOnAction(e -> {
            try {
                registry.registarAplicacao(new MockAplicacao());
                refreshList();
                modalManager.alert("Sucesso", "Aplicação de demonstração registada com sucesso.", "info", null);
            } catch (Exception ex) {
                modalManager.alert("Erro", ex.getMessage(), "error", null);
            }
        });

        Button btnRefresh = new Button(null, IconUtils.icon(Feather.REFRESH_CW, IconUtils.SIZE_SMALL));
        btnRefresh.getStyleClass().add("button-outlined");
        btnRefresh.setOnAction(e -> refreshList());

        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        toolbar.getChildren().addAll(title, spacer, btnRegistar, btnRefresh);

        table = new AdvancedTableView<>();
        TableUtils.standardize(table);
        
        TableColumn<AplicacaoAdministrador, String> colAbrev = new TableColumn<>("Abrev.");
        colAbrev.setCellValueFactory(new PropertyValueFactory<>("abreviatura"));
        colAbrev.setPrefWidth(100);

        TableColumn<AplicacaoAdministrador, String> colNome = new TableColumn<>("Nome da Aplicação");
        colNome.setCellValueFactory(new PropertyValueFactory<>("nome"));
        colNome.setPrefWidth(300);

        TableColumn<AplicacaoAdministrador, String> colAudit = new TableColumn<>("Audit");
        colAudit.setCellValueFactory(cell -> {
            boolean has = cell.getValue().getAudit() != null;
            return new javafx.beans.property.SimpleStringProperty(has ? "Disponível" : "N/D");
        });
        colAudit.setPrefWidth(120);

        TableColumn<AplicacaoAdministrador, String> colServicos = new TableColumn<>("Serviços");
        colServicos.setCellValueFactory(cell -> {
            boolean has = cell.getValue().getServicos() != null;
            return new javafx.beans.property.SimpleStringProperty(has ? "Ativos" : "N/D");
        });
        colServicos.setPrefWidth(120);

        table.getColumns().addAll(colAbrev, colNome, colAudit, colServicos);
        table.setData(apps);

        getChildren().addAll(toolbar, table);
        VBox.setVgrow(table, Priority.ALWAYS);
    }

    private void refreshList() {
        apps.setAll(registry.getAplicacoesRegistadas());
    }
}

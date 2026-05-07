package ao.allon.kubata.faturacao.profile.view;

import ao.allon.kubata.faturacao.domain.UserProfileEntity;
import ao.allon.kubata.faturacao.profile.controller.ProfileManagerController;
import ao.allon.kubata.faturacao.profile.model.UserType;
import ao.allon.kubata.faturacao.profile.util.UiUtil;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.text.Font;

public class ProfileView extends SplitPane {

    private final ProfileManagerController controller;
    private TreeView<String> treeTypes;
    private TextField quickFilter;
    private TableView<UserProfileEntity> table;
    private Pagination pagination;
    private VBox details;

    public ProfileView(ProfileManagerController controller) {
        this.controller = controller;
        setOrientation(Orientation.HORIZONTAL);
        getItems().addAll(buildLeftPanel(), buildCenterPanel(), buildRightPanel());
        setDividerPositions(0.2, 0.75, 0.95);
    }

    private Node buildLeftPanel() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(15));
        Label title = new Label("Tipos de Usuário");
        title.setFont(Font.font(20));
        quickFilter = new TextField();
        quickFilter.setPromptText("Filtro rápido...");
        treeTypes = new TreeView<>();
        TreeItem<String> rootItem = new TreeItem<>("Tipos");
        for (UserType ut : UserType.values()) {
            int count = controller.countByType(ut);
            rootItem.getChildren().add(new TreeItem<>(ut.name() + " (" + count + ")"));
        }
        rootItem.setExpanded(true);
        treeTypes.setRoot(rootItem);
        treeTypes.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            controller.filterByType(n != null ? n.getValue() : null);
        });
        quickFilter.textProperty().addListener((obs, ov, nv) -> controller.quickFilter(nv));
        box.getChildren().addAll(title, quickFilter, treeTypes);
        return box;
    }

    private Node buildCenterPanel() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(15));
        Label header = new Label("Perfis");
        header.setFont(Font.font(20));
        table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        TableColumn<UserProfileEntity, Long> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        TableColumn<UserProfileEntity, String> colNome = new TableColumn<>("Nome");
        colNome.setCellValueFactory(new PropertyValueFactory<>("nome"));
        TableColumn<UserProfileEntity, String> colEmail = new TableColumn<>("Email");
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        TableColumn<UserProfileEntity, UserType> colTipo = new TableColumn<>("Tipo");
        colTipo.setCellValueFactory(new PropertyValueFactory<>("tipo"));
        TableColumn<UserProfileEntity, Boolean> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(new PropertyValueFactory<>("ativo"));
        colStatus.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(Boolean ativo, boolean empty) {
                super.updateItem(ativo, empty);
                setGraphic(empty ? null : UiUtil.statusBadge(Boolean.TRUE.equals(ativo)));
            }
        });
        TableColumn<UserProfileEntity, String> colCriado = new TableColumn<>("Criado em");
        colCriado.setCellValueFactory(c -> javafx.beans.binding.Bindings.createStringBinding(
                () -> c.getValue().getCreatedAt() == null ? "" :
                        c.getValue().getCreatedAt().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                new javafx.beans.Observable[]{}
        ));
        TableColumn<UserProfileEntity, Void> colAcoes = new TableColumn<>("Ações");
        colAcoes.setCellFactory(tc -> controller.actionCellFactory());
        table.getColumns().addAll(colId, colNome, colEmail, colTipo, colStatus, colCriado, colAcoes);
        table.setItems(controller.filteredProfiles());
        table.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> controller.setSelected(n));
        controller.installContextMenu(table);
        pagination = new Pagination();
        pagination.setPageFactory(pageIndex -> {
            controller.applyPage(pageIndex);
            return table;
        });
        box.getChildren().addAll(header, table, pagination);
        VBox.setVgrow(table, Priority.ALWAYS);
        return box;
    }

    private Node buildRightPanel() {
        details = new VBox(10);
        details.setPadding(new Insets(15));
        controller.bindDetails(details);
        return details;
    }
}

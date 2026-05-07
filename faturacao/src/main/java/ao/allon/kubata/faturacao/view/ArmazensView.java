package ao.allon.kubata.faturacao.view;

import ao.allon.kubata.core.ui.table.TableContextMenuHelper;
import ao.allon.kubata.core.ui.table.EditableTableManager;
import ao.allon.kubata.faturacao.domain.Armazem;
import ao.allon.kubata.faturacao.domain.enums.Provincia;
import ao.allon.kubata.faturacao.service.ArmazemService;
import ao.allon.kubata.faturacao.ui.modal.ModalService;
import ao.allon.kubata.faturacao.ui.util.IconUtils;
import ao.allon.kubata.faturacao.ui.util.AlertUtils;
import ao.allon.kubata.faturacao.ui.util.TileFactory;
import atlantafx.base.controls.Card;
import atlantafx.base.controls.CustomTextField;
import atlantafx.base.controls.Tile;
import atlantafx.base.theme.Styles;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2OutlinedMZ;

public class ArmazensView extends BorderPane {

    private final ArmazemService armazemService;
    private final ModalService modalService;
    private final TableView<Armazem> table = new TableView<>();

    public ArmazensView(ArmazemService armazemService, ModalService modalService) {
        this.armazemService = armazemService;
        this.modalService = modalService;
        
        getStyleClass().add("armazens-view");
        setPadding(new Insets(20));
        
        setTop(createHeader());
        setCenter(createContent());
        
        refreshData();
    }

    private Node createHeader() {
        VBox header = new VBox(10);
        
        HBox topBar = new HBox(10);
        topBar.setAlignment(Pos.CENTER_LEFT);
        
        Label title = new Label("Gestão de Armazéns");
        title.getStyleClass().add(Styles.TITLE_2);
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Button btnNovo = new Button("Novo Armazém", IconUtils.icon(Feather.PLUS, IconUtils.SIZE_SMALL));
        btnNovo.getStyleClass().add(Styles.ACCENT);
        btnNovo.setOnAction(e -> showForm(null));
        
        topBar.getChildren().addAll(title, spacer, btnNovo);
        
        header.getChildren().add(topBar);
        return header;
    }

    private Node createContent() {
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.getStyleClass().add(Styles.STRIPED);
        table.setEditable(true);
        
        // Manager para colunas editáveis
        EditableTableManager<Armazem> editManager = new EditableTableManager<>(table);
        
        // Coluna Nome - Editável
        TableColumn<Armazem, String> colNome = editManager.createTextColumn("Nome",
            cell -> new SimpleStringProperty(cell.getValue().getNome()),
            (armazem, novoNome) -> {
                armazem.setNome(novoNome);
                armazemService.save(armazem);
            });
        
        // Coluna Província - Não editável (é um enum)
        TableColumn<Armazem, String> colProvincia = new TableColumn<>("Província");
        colProvincia.setCellValueFactory(cell -> new SimpleStringProperty(
            cell.getValue().getProvincia() != null ? cell.getValue().getProvincia().getNome() : "-"
        ));
        
        // Coluna Município - Editável
        TableColumn<Armazem, String> colMunicipio = editManager.createTextColumn("Município",
            cell -> new SimpleStringProperty(cell.getValue().getMunicipio() != null ? cell.getValue().getMunicipio() : ""),
            (armazem, novoMunicipio) -> {
                armazem.setMunicipio(novoMunicipio);
                armazemService.save(armazem);
            });
        
        // Coluna Responsável - Editável
        TableColumn<Armazem, String> colResponsavel = editManager.createTextColumn("Responsável",
            cell -> new SimpleStringProperty(cell.getValue().getResponsavel() != null ? cell.getValue().getResponsavel() : ""),
            (armazem, novoResponsavel) -> {
                armazem.setResponsavel(novoResponsavel);
                armazemService.save(armazem);
            });

        // Coluna Telefone - Editável
        TableColumn<Armazem, String> colTelefone = editManager.createTextColumn("Telefone",
            cell -> new SimpleStringProperty(cell.getValue().getTelefone() != null ? cell.getValue().getTelefone() : ""),
            (armazem, novoTelefone) -> {
                armazem.setTelefone(novoTelefone);
                armazemService.save(armazem);
            });
        
        TableColumn<Armazem, Void> colStatus = new TableColumn<>("Status");
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Armazem a = getTableView().getItems().get(getIndex());
                    if (Boolean.TRUE.equals(a.getIsPrincipal())) {
                        Label lbl = new Label("Principal");
                        lbl.getStyleClass().addAll(Styles.SUCCESS, Styles.TEXT_SMALL, Styles.TEXT_BOLD);
                        lbl.setStyle("-fx-background-color: -color-success-muted; -fx-padding: 2 6; -fx-background-radius: 4;");
                        setGraphic(lbl);
                    } else {
                        setGraphic(null);
                    }
                }
                setAlignment(Pos.CENTER);
            }
        });

        TableColumn<Armazem, Void> colActions = new TableColumn<>("Ações");
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit = new Button("", IconUtils.icon(Feather.EDIT, IconUtils.SIZE_SMALL));
            private final Button btnDelete = new Button("", IconUtils.icon(Feather.TRASH, IconUtils.SIZE_SMALL));
            private final Button btnStar = new Button("", IconUtils.icon(Feather.STAR, IconUtils.SIZE_SMALL));
            
            {
                btnEdit.getStyleClass().addAll(Styles.BUTTON_CIRCLE, Styles.FLAT);
                btnEdit.setTooltip(new Tooltip("Editar"));
                btnEdit.setOnAction(e -> showForm(getTableView().getItems().get(getIndex())));
                
                btnDelete.getStyleClass().addAll(Styles.BUTTON_CIRCLE, Styles.FLAT, Styles.DANGER);
                btnDelete.setTooltip(new Tooltip("Excluir"));
                btnDelete.setOnAction(e -> delete(getTableView().getItems().get(getIndex())));
                
                btnStar.getStyleClass().addAll(Styles.BUTTON_CIRCLE, Styles.FLAT, Styles.WARNING);
                btnStar.setTooltip(new Tooltip("Definir como Principal"));
                btnStar.setOnAction(e -> setPrincipal(getTableView().getItems().get(getIndex())));
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Armazem a = getTableView().getItems().get(getIndex());
                    HBox box = new HBox(5, btnEdit);
                    if (!Boolean.TRUE.equals(a.getIsPrincipal())) {
                        box.getChildren().add(0, btnStar);
                        box.getChildren().add(btnDelete);
                    }
                    box.setAlignment(Pos.CENTER);
                    setGraphic(box);
                }
            }
        });
        
        table.getColumns().addAll(colNome, colProvincia, colMunicipio, colResponsavel, colTelefone, colStatus, colActions);
        
        // Configurar menu de contexto
        TableContextMenuHelper.createCrudMenu(table, "Armazém",
                armazem -> showForm(armazem),
                armazem -> delete(armazem),
                () -> refreshData())
            .apply();
        
        VBox container = new VBox(10, table);
        VBox.setVgrow(table, Priority.ALWAYS);
        
        return container;
    }

    public void refreshData() {
        table.setItems(FXCollections.observableArrayList(armazemService.findAll()));
    }

    private void showForm(Armazem armazem) {
        VBox form = new VBox(20);
        form.setPadding(new Insets(20));
        form.setPrefWidth(500);

        // Fields
        TextField txtNome = new TextField(armazem != null ? armazem.getNome() : "");
        txtNome.setPromptText("Ex: Armazém Central");
        
        ComboBox<Provincia> cbProvincia = new ComboBox<>(FXCollections.observableArrayList(Provincia.values()));
        cbProvincia.setPromptText("Selecione");
        cbProvincia.setMaxWidth(Double.MAX_VALUE);
        if (armazem != null) cbProvincia.setValue(armazem.getProvincia());
        
        TextField txtMunicipio = new TextField(armazem != null ? armazem.getMunicipio() : "");
        txtMunicipio.setPromptText("Ex: Viana");
        
        TextField txtEndereco = new TextField(armazem != null ? armazem.getEndereco() : "");
        txtEndereco.setPromptText("Rua, Bairro, Ponto de Referência");
        
        TextField txtResponsavel = new TextField(armazem != null ? armazem.getResponsavel() : "");
        txtResponsavel.setPromptText("Nome do gerente");
        
        var txtTelefone = new CustomTextField();
        txtTelefone.setText(armazem != null ? armazem.getTelefone() : "");
        txtTelefone.setPromptText("(999) 999 999 999");
        txtTelefone.setLeft(new FontIcon(Material2OutlinedMZ.PHONE));
        
        TextArea txtDesc = new TextArea(armazem != null ? armazem.getDescricao() : "");
        txtDesc.setPrefRowCount(3);
        txtDesc.setWrapText(true);

        // Layout using GridPane for structure
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        
        // Column constraints for 50/50 split
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(50);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(50);
        grid.getColumnConstraints().addAll(col1, col2);
        
        addFormField(grid, "Nome do Armazém", txtNome, 0, 0, 2);
        addFormField(grid, "Província", cbProvincia, 0, 1, 1);
        addFormField(grid, "Município", txtMunicipio, 1, 1, 1);
        addFormField(grid, "Endereço", txtEndereco, 0, 2, 2);
        addFormField(grid, "Responsável", txtResponsavel, 0, 3, 1);
        addFormField(grid, "Telefone", txtTelefone, 1, 3, 1);
        addFormField(grid, "Observações", txtDesc, 0, 4, 2);

        form.getChildren().addAll(grid);

        modalService.create()
            .title(armazem == null ? "Novo Armazém" : "Editar Armazém")
            .content(form)
            .autoSize()
            .withConfirmButton("Salvar Armazém", () -> {
                try {
                    if (txtNome.getText().isEmpty()) throw new IllegalArgumentException("O nome é obrigatório");
                    if (cbProvincia.getValue() == null) throw new IllegalArgumentException("A província é obrigatória");

                    Armazem a = armazem != null ? armazem : new Armazem();
                    a.setNome(txtNome.getText());
                    a.setProvincia(cbProvincia.getValue());
                    a.setMunicipio(txtMunicipio.getText());
                    a.setEndereco(txtEndereco.getText());
                    a.setResponsavel(txtResponsavel.getText());
                    a.setTelefone(txtTelefone.getText());
                    a.setDescricao(txtDesc.getText());

                    armazemService.save(a);
                    AlertUtils.showInfoAlert("Sucesso", "Armazém salvo com sucesso!");
                    refreshData();
                    return true;
                } catch (Exception ex) {
                    AlertUtils.showErrorAlert("Erro", ex.getMessage());
                    return false;
                }
            })
            .withCancelButton("Cancelar")
            .buildAndShow();
    }

    private void addFormField(GridPane grid, String labelText, Node field, int col, int row, int colSpan) {
        Label label = new Label(labelText);
        label.getStyleClass().add(Styles.TEXT_BOLD);
        
        VBox container = new VBox(5, label, field);
        grid.add(container, col, row, colSpan, 1);
    }

    private void delete(Armazem armazem) {
        boolean confirm = AlertUtils.showConfirmationAlert("Excluir Armazém", 
            "Tem certeza que deseja excluir '" + armazem.getNome() + "'?");
        
        if (confirm) {
            try {
                armazemService.delete(armazem.getId());
                refreshData();
                AlertUtils.showInfoAlert("Sucesso", "Armazém excluído.");
            } catch (Exception e) {
                AlertUtils.showErrorAlert("Erro", e.getMessage());
            }
        }
    }

    private void setPrincipal(Armazem armazem) {
        boolean confirm = AlertUtils.showConfirmationAlert("Definir Principal", 
            "Definir '" + armazem.getNome() + "' como armazém principal?");
            
        if (confirm) {
            try {
                armazemService.setPrincipal(armazem);
                refreshData();
                AlertUtils.showInfoAlert("Sucesso", "Armazém principal atualizado.");
            } catch (Exception e) {
                AlertUtils.showErrorAlert("Erro", e.getMessage());
            }
        }
    }
}

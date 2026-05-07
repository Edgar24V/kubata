package ao.allon.kubata.rh.ui.views;

import ao.allon.kubata.core.ui.table.AdvancedTableView;
import ao.allon.kubata.core.ui.table.TableUtils;
import ao.allon.kubata.rh.domain.Cargo;
import ao.allon.kubata.rh.service.CargoService;
import ao.allon.kubata.rh.ui.modal.ModalManager;
import net.synedra.validatorfx.Validator;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.feather.Feather;
import javax.annotation.PostConstruct;
import java.util.List;

@Component
public class CargoView extends VBox {

    @Autowired
    private CargoService cargoService;

    @Autowired
    private ModalManager modalManager;

    private AdvancedTableView<Cargo> tableView;
    private TableColumn<Cargo, String> colNome;
    private TableColumn<Cargo, String> colNivel;
    private TableColumn<Cargo, String> colDescricao;
    private TableColumn<Cargo, String> colAtivo;
    private TableColumn<Cargo, Void> colAcoes;

    private TextField txtNome;
    private TextField txtNivel;
    private TextArea txtDescricao;
    private CheckBox chkAtivo;

    private Button btnNovo;

    private ObservableList<Cargo> cargos;
    private Cargo cargoSelecionado;
    private Validator validator;

    public CargoView() {
        setSpacing(10);
        setPadding(new Insets(10));
    }

    @PostConstruct
    public void init() {
        Platform.runLater(() -> {
            buildUI();
            configurarTabela();
            carregarDados();
        });
    }

    private void buildUI() {
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setSpacing(10);

        Label title = new Label("Gestão de Cargos");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        
        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        btnNovo = new Button("Novo Cargo");
        btnNovo.getStyleClass().add("accent");
        btnNovo.setOnAction(e -> abrirFormulario(null));

        header.getChildren().addAll(title, spacer, btnNovo);

        tableView = new AdvancedTableView<>();
        VBox.setVgrow(tableView, Priority.ALWAYS);

        colNome = new TableColumn<>("Nome");
        colNivel = new TableColumn<>("Nível");
        colDescricao = new TableColumn<>("Descrição");
        colAtivo = new TableColumn<>("Ativo");
        colAcoes = new TableColumn<>("Ações");

        tableView.getColumns().addAll(colNome, colNivel, colDescricao, colAtivo, colAcoes);

        getChildren().addAll(header, tableView);
    }

    private void configurarTabela() {
        TableUtils.standardize(tableView);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Configura colunas
        colNome.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getNome()));
        colNivel.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getNivel() != null ? data.getValue().getNivel() : ""));
        colDescricao.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDescricao() != null ? data.getValue().getDescricao() : ""));
        colAtivo.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getActive() ? "Sim" : "Não"));

        // Configura coluna de ações
        configurarColunaAcoes();

        // Configura Menu de Contexto (Botão Direito)
        configurarMenuContexto();

        cargos = FXCollections.observableArrayList();
        tableView.setData(cargos);
    }

    private void configurarMenuContexto() {
        ContextMenu contextMenu = new ContextMenu();

        MenuItem itemNovo = new MenuItem("Novo Cargo");
        itemNovo.setOnAction(e -> abrirFormulario(null));

        MenuItem itemEditar = new MenuItem("Editar Selecionado");
        itemEditar.setOnAction(e -> {
            Cargo selecionado = tableView.getSelectionModel().getSelectedItem();
            if (selecionado != null) {
                abrirFormulario(selecionado);
            }
        });

        MenuItem itemToggleAtivo = new MenuItem("Alternar Status (Ativo/Inativo)");
        itemToggleAtivo.setOnAction(e -> {
            Cargo selecionado = tableView.getSelectionModel().getSelectedItem();
            if (selecionado != null) {
                toggleAtivo(selecionado);
            }
        });

        SeparatorMenuItem separator = new SeparatorMenuItem();

        MenuItem itemAtualizar = new MenuItem("Atualizar Tabela");
        itemAtualizar.setOnAction(e -> carregarDados());

        contextMenu.getItems().addAll(itemNovo, itemEditar, itemToggleAtivo, separator, itemAtualizar);
        tableView.setContextMenu(contextMenu);
    }

    private void configurarColunaAcoes() {
        colAcoes.setCellFactory(param -> new TableCell<>() {
            private final Button btnEditar = new Button();
            private final Button btnAtivar = new Button();
            private final HBox hbox = new HBox(8, btnEditar, btnAtivar);

            {
                btnEditar.setGraphic(new FontIcon(Feather.EDIT_2));
                btnEditar.setTooltip(new Tooltip("Editar"));
                btnEditar.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
                
                btnAtivar.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
                
                btnEditar.setOnAction(e -> abrirFormulario(getTableView().getItems().get(getIndex())));
                btnAtivar.setOnAction(e -> toggleAtivo(getTableView().getItems().get(getIndex())));
                
                hbox.setAlignment(Pos.CENTER);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Cargo cargo = getTableView().getItems().get(getIndex());
                    FontIcon iconStatus = new FontIcon(cargo.getActive() ? Feather.CHECK_CIRCLE : Feather.X_CIRCLE);
                    iconStatus.setIconColor(javafx.scene.paint.Color.valueOf(cargo.getActive() ? "#28a745" : "#dc3545"));
                    btnAtivar.setGraphic(iconStatus);
                    btnAtivar.setTooltip(new Tooltip(cargo.getActive() ? "Inativar" : "Ativar"));
                    
                    setGraphic(hbox);
                }
            }
        });
    }

    private void carregarDados() {
        try {
            List<Cargo> lista = cargoService.findAll();
            cargos.clear();
            cargos.addAll(lista);
        } catch (Exception e) {
            modalManager.alert("Erro", "Não foi possível carregar os dados: " + e.getMessage(), "error", e);
        }
    }

    private void abrirFormulario(Cargo cargo) {
        this.cargoSelecionado = cargo;
        this.validator = new Validator();
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));

        grid.add(new Label("Nome:"), 0, 0);
        txtNome = new TextField();
        if (cargo != null) txtNome.setText(cargo.getNome());
        grid.add(txtNome, 1, 0);

        validator.createCheck()
            .dependsOn("nome", txtNome.textProperty())
            .withMethod(c -> {
                String nome = c.get("nome");
                if (nome == null || nome.trim().isEmpty()) {
                    c.error("O nome do cargo é obrigatório");
                }
            })
            .decorates(txtNome)
            .immediate();

        grid.add(new Label("Nível:"), 0, 1);
        txtNivel = new TextField();
        if (cargo != null) txtNivel.setText(cargo.getNivel() != null ? cargo.getNivel() : "");
        grid.add(txtNivel, 1, 1);

        grid.add(new Label("Descrição:"), 0, 2);
        txtDescricao = new TextArea();
        txtDescricao.setPrefRowCount(3);
        if (cargo != null) txtDescricao.setText(cargo.getDescricao() != null ? cargo.getDescricao() : "");
        grid.add(txtDescricao, 1, 2);

        chkAtivo = new CheckBox("Ativo");
        chkAtivo.setSelected(cargo == null || cargo.getActive());
        grid.add(chkAtivo, 1, 3);

        modalManager.showModal(grid, new ModalManager.ModalConfig()
            .title(cargo == null ? "Novo Cargo" : "Editar Cargo")
            .withConfirmButtons("Salvar", "Cancelar")
            .onConfirm(() -> {
                if (validator.validate()) {
                    salvarCargo();
                } else {
                    String errors = String.join("\n", validator.createStringBinding().get().split(","));
                    modalManager.alert("Aviso", "Por favor, corrija os erros no formulário:\n" + errors, "warning", null);
                }
            }));
        
        Platform.runLater(() -> txtNome.requestFocus());
    }

    private void salvarCargo() {
        try {
            if (txtNome.getText().trim().isEmpty()) {
                modalManager.alert("Aviso", "Nome do cargo é obrigatório", "warning", null);
                return;
            }

            Cargo cargo = cargoSelecionado != null ? cargoSelecionado : new Cargo();
            cargo.setNome(txtNome.getText().trim());
            cargo.setNivel(txtNivel.getText().trim().isEmpty() ? null : txtNivel.getText().trim());
            cargo.setDescricao(txtDescricao.getText().trim().isEmpty() ? null : txtDescricao.getText().trim());
            cargo.setActive(chkAtivo.isSelected());

            cargoService.save(cargo);
            carregarDados();
            
        } catch (Exception e) {
            modalManager.alert("Erro ao Salvar", "Não foi possível salvar o cargo.", "error", e);
        }
    }

    private void toggleAtivo(Cargo cargo) {
        try {
            if (cargo.getActive()) {
                cargoService.deactivate(cargo.getId());
            } else {
                cargoService.activate(cargo.getId());
            }
            carregarDados();
        } catch (Exception e) {
            modalManager.alert("Erro de Status", "Não foi possível alterar o status do cargo.", "error", e);
        }
    }


}

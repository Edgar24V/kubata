package ao.allon.kubata.rh.ui.views;

import ao.allon.kubata.core.ui.table.AdvancedTableView;
import ao.allon.kubata.core.ui.table.TableUtils;
import ao.allon.kubata.rh.domain.Departamento;
import ao.allon.kubata.rh.service.DepartamentoService;
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
public class DepartamentoView extends VBox {

    @Autowired
    private DepartamentoService departamentoService;

    @Autowired
    private ModalManager modalManager;

    private AdvancedTableView<Departamento> tableView;
    private TableColumn<Departamento, String> colNome;
    private TableColumn<Departamento, String> colSigla;
    private TableColumn<Departamento, String> colDescricao;
    private TableColumn<Departamento, String> colAtivo;
    private TableColumn<Departamento, Void> colAcoes;

    private TextField txtNome;
    private TextField txtSigla;
    private TextArea txtDescricao;
    private CheckBox chkAtivo;

    private Button btnNovo;

    private ObservableList<Departamento> departamentos;
    private Departamento departamentoSelecionado;
    private Validator validator;

    public DepartamentoView() {
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

        Label title = new Label("Gestão de Departamentos");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        
        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        btnNovo = new Button("Novo Departamento");
        btnNovo.getStyleClass().add("accent");
        btnNovo.setOnAction(e -> abrirFormulario(null));

        header.getChildren().addAll(title, spacer, btnNovo);

        // Tabela
        tableView = new AdvancedTableView<>();
        VBox.setVgrow(tableView, Priority.ALWAYS);
        
        colNome = new TableColumn<>("Nome");
        colSigla = new TableColumn<>("Sigla");
        colDescricao = new TableColumn<>("Descrição");
        colAtivo = new TableColumn<>("Ativo");
        colAcoes = new TableColumn<>("Ações");
        
        tableView.getColumns().addAll(colNome, colSigla, colDescricao, colAtivo, colAcoes);
        
        getChildren().addAll(header, tableView);
    }

    private void configurarTabela() {
        TableUtils.standardize(tableView);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Configura colunas
        colNome.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getNome()));
        colSigla.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getSigla() != null ? data.getValue().getSigla() : ""));
        colDescricao.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDescricao() != null ? data.getValue().getDescricao() : ""));
        colAtivo.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getActive() ? "Sim" : "Não"));

        // Configura coluna de ações
        configurarColunaAcoes();

        // Configura Menu de Contexto (Botão Direito)
        configurarMenuContexto();

        departamentos = FXCollections.observableArrayList();
        tableView.setData(departamentos);
    }

    private void configurarMenuContexto() {
        ContextMenu contextMenu = new ContextMenu();

        MenuItem itemNovo = new MenuItem("Novo Departamento");
        itemNovo.setOnAction(e -> abrirFormulario(null));

        MenuItem itemEditar = new MenuItem("Editar Selecionado");
        itemEditar.setOnAction(e -> {
            Departamento selecionado = tableView.getSelectionModel().getSelectedItem();
            if (selecionado != null) {
                abrirFormulario(selecionado);
            }
        });

        MenuItem itemToggleAtivo = new MenuItem("Alternar Status (Ativo/Inativo)");
        itemToggleAtivo.setOnAction(e -> {
            Departamento selecionado = tableView.getSelectionModel().getSelectedItem();
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
                    Departamento dep = getTableView().getItems().get(getIndex());
                    FontIcon iconStatus = new FontIcon(dep.getActive() ? Feather.CHECK_CIRCLE : Feather.X_CIRCLE);
                    iconStatus.setIconColor(javafx.scene.paint.Color.valueOf(dep.getActive() ? "#28a745" : "#dc3545"));
                    btnAtivar.setGraphic(iconStatus);
                    btnAtivar.setTooltip(new Tooltip(dep.getActive() ? "Inativar" : "Ativar"));
                    
                    setGraphic(hbox);
                }
            }
        });
    }

    private void carregarDados() {
        try {
            List<Departamento> lista = departamentoService.findAll();
            departamentos.clear();
            departamentos.addAll(lista);
        } catch (Exception e) {
            modalManager.alert("Erro", "Não foi possível carregar os dados: " + e.getMessage(), "error", e);
        }
    }

    private void abrirFormulario(Departamento departamento) {
        this.departamentoSelecionado = departamento;
        this.validator = new Validator();

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));

        grid.add(new Label("Nome:"), 0, 0);
        txtNome = new TextField();
        if (departamento != null) txtNome.setText(departamento.getNome());
        grid.add(txtNome, 1, 0);

        validator.createCheck()
            .dependsOn("nome", txtNome.textProperty())
            .withMethod(c -> {
                String nome = c.get("nome");
                if (nome == null || nome.trim().isEmpty()) {
                    c.error("O nome do departamento é obrigatório");
                } else if (nome.length() < 3) {
                    c.error("O nome deve ter pelo menos 3 caracteres");
                }
            })
            .decorates(txtNome)
            .immediate();

        grid.add(new Label("Sigla:"), 0, 1);
        txtSigla = new TextField();
        if (departamento != null) txtSigla.setText(departamento.getSigla() != null ? departamento.getSigla() : "");
        grid.add(txtSigla, 1, 1);

        validator.createCheck()
            .dependsOn("sigla", txtSigla.textProperty())
            .withMethod(c -> {
                String sigla = c.get("sigla");
                if (sigla != null && !sigla.trim().isEmpty() && sigla.length() > 10) {
                    c.error("A sigla não deve exceder 10 caracteres");
                }
            })
            .decorates(txtSigla)
            .immediate();

        grid.add(new Label("Descrição:"), 0, 2);
        txtDescricao = new TextArea();
        txtDescricao.setPrefRowCount(3);
        if (departamento != null) txtDescricao.setText(departamento.getDescricao() != null ? departamento.getDescricao() : "");
        grid.add(txtDescricao, 1, 2);

        chkAtivo = new CheckBox("Ativo");
        chkAtivo.setSelected(departamento == null || departamento.getActive());
        grid.add(chkAtivo, 1, 3);

        modalManager.showModal(grid, new ModalManager.ModalConfig()
            .title(departamento == null ? "Novo Departamento" : "Editar Departamento")
            .withConfirmButtons("Salvar", "Cancelar")
            .onConfirm(() -> {
                if (validator.validate()) {
                    salvarDepartamento();
                } else {
                    String errors = String.join("\n", validator.createStringBinding().get().split(","));
                    modalManager.alert("Aviso", "Por favor, corrija os erros no formulário:\n" + errors, "warning", null);
                }
            }));

        Platform.runLater(() -> txtNome.requestFocus());
    }

    private void salvarDepartamento() {
        try {
            if (txtNome.getText().trim().isEmpty()) {
                modalManager.alert("Aviso", "Nome do departamento é obrigatório", "warning", null);
                return;
            }

            Departamento departamento = departamentoSelecionado != null ? departamentoSelecionado : new Departamento();
            departamento.setNome(txtNome.getText().trim());
            departamento.setSigla(txtSigla.getText().trim().isEmpty() ? null : txtSigla.getText().trim());
            departamento.setDescricao(txtDescricao.getText().trim().isEmpty() ? null : txtDescricao.getText().trim());
            departamento.setActive(chkAtivo.isSelected());

            departamentoService.save(departamento);
            carregarDados();

        } catch (Exception e) {
            modalManager.alert("Erro ao Salvar", "Não foi possível salvar o departamento.", "error", e);
        }
    }

    private void toggleAtivo(Departamento departamento) {
        try {
            if (departamento.getActive()) {
                departamentoService.deactivate(departamento.getId());
            } else {
                departamentoService.activate(departamento.getId());
            }
            carregarDados();
        } catch (Exception e) {
            modalManager.alert("Erro de Status", "Não foi possível alterar o status do departamento.", "error", e);
        }
    }

}

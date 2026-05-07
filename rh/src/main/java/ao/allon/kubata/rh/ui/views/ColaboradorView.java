package ao.allon.kubata.rh.ui.views;

import ao.allon.kubata.core.ui.table.AdvancedTableView;
import ao.allon.kubata.core.ui.table.TableUtils;
import ao.allon.kubata.rh.domain.Colaborador;
import ao.allon.kubata.rh.domain.Departamento;
import ao.allon.kubata.rh.domain.Cargo;
import ao.allon.kubata.rh.service.ColaboradorService;
import ao.allon.kubata.rh.service.DepartamentoService;
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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.feather.Feather;
import javax.annotation.PostConstruct;

@Component
public class ColaboradorView extends VBox {

    @Autowired
    private ColaboradorService colaboradorService;
    
    @Autowired
    private DepartamentoService departamentoService;
    
    @Autowired
    private CargoService cargoService;

    @Autowired
    private ModalManager modalManager;

    private AdvancedTableView<Colaborador> tableView;
    private TableColumn<Colaborador, String> colNome;
    private TableColumn<Colaborador, String> colBI;
    private TableColumn<Colaborador, String> colNIF;
    private TableColumn<Colaborador, String> colDepartamento;
    private TableColumn<Colaborador, String> colCargo;
    private TableColumn<Colaborador, String> colDataAdmissao;
    private TableColumn<Colaborador, String> colEstado;
    private TableColumn<Colaborador, Void> colAcoes;

    private TextField txtNome;
    private TextField txtBI;
    private TextField txtNIF;
    private TextField txtTelefone;
    private TextField txtEmail;
    private ComboBox<Departamento> cmbDepartamento;
    private ComboBox<Cargo> cmbCargo;
    private DatePicker dpDataAdmissao;
    private ComboBox<Colaborador.EstadoColaborador> cmbEstado;

    private Button btnNovo;

    private ObservableList<Colaborador> colaboradores;
    private ObservableList<Departamento> departamentos;
    private ObservableList<Cargo> cargos;
    private Colaborador colaboradorSelecionado;
    private Validator validator;

    public ColaboradorView() {
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

        Label title = new Label("Gestão de Colaboradores");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        
        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        btnNovo = new Button("Novo Colaborador");
        btnNovo.getStyleClass().add("accent");
        btnNovo.setOnAction(e -> abrirFormulario(null));

        header.getChildren().addAll(title, spacer, btnNovo);

        tableView = new AdvancedTableView<>();
        VBox.setVgrow(tableView, Priority.ALWAYS);

        colNome = new TableColumn<>("Nome");
        colBI = new TableColumn<>("BI");
        colNIF = new TableColumn<>("NIF");
        colDepartamento = new TableColumn<>("Departamento");
        colCargo = new TableColumn<>("Cargo");
        colDataAdmissao = new TableColumn<>("Admissão");
        colEstado = new TableColumn<>("Estado");
        colAcoes = new TableColumn<>("Ações");

        tableView.getColumns().addAll(colNome, colBI, colNIF, colDepartamento, colCargo, colDataAdmissao, colEstado, colAcoes);

        getChildren().addAll(header, tableView);
    }

    private void configurarTabela() {
        TableUtils.standardize(tableView);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Configura colunas
        colNome.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getNomeCompleto()));
        colBI.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getBi() != null ? data.getValue().getBi() : ""));
        colNIF.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getNif() != null ? data.getValue().getNif() : ""));
        colDepartamento.setCellValueFactory(data -> new SimpleStringProperty(
            data.getValue().getDepartamento() != null ? data.getValue().getDepartamento().getNome() : ""));
        colCargo.setCellValueFactory(data -> new SimpleStringProperty(
            data.getValue().getCargo() != null ? data.getValue().getCargo().getNome() : ""));
        colDataAdmissao.setCellValueFactory(data -> new SimpleStringProperty(
            data.getValue().getDataAdmissao() != null ? data.getValue().getDataAdmissao().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : ""));
        colEstado.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getEstado().toString()));

        // Configura coluna de ações
        configurarColunaAcoes();

        // Configura Menu de Contexto (Botão Direito)
        configurarMenuContexto();

        colaboradores = FXCollections.observableArrayList();
        tableView.setData(colaboradores);
    }

    private void configurarMenuContexto() {
        ContextMenu contextMenu = new ContextMenu();

        MenuItem itemNovo = new MenuItem("Novo Colaborador");
        itemNovo.setOnAction(e -> abrirFormulario(null));

        MenuItem itemEditar = new MenuItem("Editar Selecionado");
        itemEditar.setOnAction(e -> {
            Colaborador selecionado = tableView.getSelectionModel().getSelectedItem();
            if (selecionado != null) {
                abrirFormulario(selecionado);
            }
        });

        MenuItem itemToggleStatus = new MenuItem("Alterar Estado (Ativo/Desligado)");
        itemToggleStatus.setOnAction(e -> {
            Colaborador selecionado = tableView.getSelectionModel().getSelectedItem();
            if (selecionado != null) {
                toggleStatus(selecionado);
            }
        });

        SeparatorMenuItem separator = new SeparatorMenuItem();

        MenuItem itemAtualizar = new MenuItem("Atualizar Tabela");
        itemAtualizar.setOnAction(e -> carregarDados());

        contextMenu.getItems().addAll(itemNovo, itemEditar, itemToggleStatus, separator, itemAtualizar);
        tableView.setContextMenu(contextMenu);
    }

    private void configurarColunaAcoes() {
        colAcoes.setCellFactory(param -> new TableCell<>() {
            private final Button btnEditar = new Button();
            private final Button btnDesligar = new Button();
            private final HBox hbox = new HBox(8, btnEditar, btnDesligar);

            {
                btnEditar.setGraphic(new FontIcon(Feather.EDIT_2));
                btnEditar.setTooltip(new Tooltip("Editar"));
                btnEditar.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
                
                btnDesligar.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
                
                btnEditar.setOnAction(e -> abrirFormulario(getTableView().getItems().get(getIndex())));
                btnDesligar.setOnAction(e -> toggleStatus(getTableView().getItems().get(getIndex())));
                
                hbox.setAlignment(Pos.CENTER);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Colaborador colab = getTableView().getItems().get(getIndex());
                    boolean isAtivo = colab.getEstado() == Colaborador.EstadoColaborador.ATIVO;
                    FontIcon iconStatus = new FontIcon(isAtivo ? Feather.USER_CHECK : Feather.USER_X);
                    iconStatus.setIconColor(javafx.scene.paint.Color.valueOf(isAtivo ? "#28a745" : "#dc3545"));
                    btnDesligar.setGraphic(iconStatus);
                    btnDesligar.setTooltip(new Tooltip(isAtivo ? "Desligar" : "Ativar"));
                    
                    setGraphic(hbox);
                }
            }
        });
    }

    private void carregarDados() {
        try {
            // Carrega colaboradores
            List<Colaborador> lista = colaboradorService.findAll();
            colaboradores.clear();
            colaboradores.addAll(lista);

            // Carrega departamentos
            List<Departamento> deptos = departamentoService.findAtivos();
            departamentos = FXCollections.observableArrayList(deptos);

            // Carrega cargos
            List<Cargo> listaCargos = cargoService.findAtivos();
            cargos = FXCollections.observableArrayList(listaCargos);

        } catch (Exception e) {
            modalManager.alert("Erro", "Não foi possível carregar os dados: " + e.getMessage(), "error", e);
        }
    }

    private void abrirFormulario(Colaborador colaborador) {
        this.colaboradorSelecionado = colaborador;
        this.validator = new Validator();

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));

        grid.add(new Label("Nome Completo:"), 0, 0);
        txtNome = new TextField();
        if (colaborador != null) txtNome.setText(colaborador.getNomeCompleto());
        grid.add(txtNome, 1, 0, 3, 1);

        validator.createCheck()
            .dependsOn("nome", txtNome.textProperty())
            .withMethod(c -> {
                String nome = c.get("nome");
                if (nome == null || nome.trim().isEmpty()) {
                    c.error("O nome completo é obrigatório");
                }
            })
            .decorates(txtNome)
            .immediate();

        grid.add(new Label("BI:"), 0, 1);
        txtBI = new TextField();
        if (colaborador != null) txtBI.setText(colaborador.getBi() != null ? colaborador.getBi() : "");
        grid.add(txtBI, 1, 1);

        validator.createCheck()
            .dependsOn("bi", txtBI.textProperty())
            .withMethod(c -> {
                String bi = c.get("bi");
                if (bi != null && !bi.trim().isEmpty()) {
                    // Padrão BI Angolano: 9 números + 2 letras + 3 números
                    if (!bi.matches("\\d{9}[A-Z]{2}\\d{3}")) {
                        c.error("BI inválido (Formato: 000000000XX000)");
                    }
                }
            })
            .decorates(txtBI)
            .immediate();

        grid.add(new Label("NIF:"), 2, 1);
        txtNIF = new TextField();
        if (colaborador != null) txtNIF.setText(colaborador.getNif() != null ? colaborador.getNif() : "");
        grid.add(txtNIF, 3, 1);

        validator.createCheck()
            .dependsOn("nif", txtNIF.textProperty())
            .withMethod(c -> {
                String nif = c.get("nif");
                if (nif != null && !nif.trim().isEmpty()) {
                    if (nif.length() < 10 || nif.length() > 14) {
                        c.error("NIF deve ter entre 10 e 14 caracteres");
                    }
                }
            })
            .decorates(txtNIF)
            .immediate();

        grid.add(new Label("Telefone:"), 0, 2);
        txtTelefone = new TextField();
        if (colaborador != null) txtTelefone.setText(colaborador.getTelefone() != null ? colaborador.getTelefone() : "");
        grid.add(txtTelefone, 1, 2);

        grid.add(new Label("Email:"), 2, 2);
        txtEmail = new TextField();
        if (colaborador != null) txtEmail.setText(colaborador.getEmail() != null ? colaborador.getEmail() : "");
        grid.add(txtEmail, 3, 2);

        validator.createCheck()
            .dependsOn("email", txtEmail.textProperty())
            .withMethod(c -> {
                String email = c.get("email");
                if (email != null && !email.trim().isEmpty()) {
                    if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                        c.error("Formato de email inválido");
                    }
                }
            })
            .decorates(txtEmail)
            .immediate();

        grid.add(new Label("Departamento:"), 0, 3);
        cmbDepartamento = new ComboBox<>(departamentos);
        cmbDepartamento.setMaxWidth(Double.MAX_VALUE);
        if (colaborador != null && colaborador.getDepartamento() != null) cmbDepartamento.getSelectionModel().select(colaborador.getDepartamento());
        grid.add(cmbDepartamento, 1, 3);

        grid.add(new Label("Cargo:"), 2, 3);
        cmbCargo = new ComboBox<>(cargos);
        cmbCargo.setMaxWidth(Double.MAX_VALUE);
        if (colaborador != null && colaborador.getCargo() != null) cmbCargo.getSelectionModel().select(colaborador.getCargo());
        grid.add(cmbCargo, 3, 3);

        grid.add(new Label("Data Admissão:"), 0, 4);
        dpDataAdmissao = new DatePicker();
        dpDataAdmissao.setMaxWidth(Double.MAX_VALUE);
        if (colaborador != null) dpDataAdmissao.setValue(colaborador.getDataAdmissao());
        grid.add(dpDataAdmissao, 1, 4);

        grid.add(new Label("Estado:"), 2, 4);
        cmbEstado = new ComboBox<>(FXCollections.observableArrayList(Colaborador.EstadoColaborador.values()));
        cmbEstado.setMaxWidth(Double.MAX_VALUE);
        if (colaborador != null) cmbEstado.getSelectionModel().select(colaborador.getEstado());
        else cmbEstado.getSelectionModel().select(Colaborador.EstadoColaborador.ATIVO);
        grid.add(cmbEstado, 3, 4);

        modalManager.showModal(grid, new ModalManager.ModalConfig()
            .title(colaborador == null ? "Novo Colaborador" : "Editar Colaborador")
            .withConfirmButtons("Salvar", "Cancelar")
            .onConfirm(() -> {
                if (validator.validate()) {
                    salvarColaborador();
                } else {
                    String errors = String.join("\n", validator.createStringBinding().get().split(","));
                    modalManager.alert("Aviso", "Por favor, corrija os erros no formulário:\n" + errors, "warning", null);
                }
            }));

        Platform.runLater(() -> txtNome.requestFocus());
    }

    private void salvarColaborador() {
        try {
            if (txtNome.getText().trim().isEmpty()) {
                modalManager.alert("Aviso", "Nome completo é obrigatório", "warning", null);
                return;
            }

            Colaborador colaborador = colaboradorSelecionado != null ? colaboradorSelecionado : new Colaborador();
            colaborador.setNomeCompleto(txtNome.getText().trim());
            colaborador.setBi(txtBI.getText().trim().isEmpty() ? null : txtBI.getText().trim());
            colaborador.setNif(txtNIF.getText().trim().isEmpty() ? null : txtNIF.getText().trim());
            colaborador.setTelefone(txtTelefone.getText().trim().isEmpty() ? null : txtTelefone.getText().trim());
            colaborador.setEmail(txtEmail.getText().trim().isEmpty() ? null : txtEmail.getText().trim());
            colaborador.setDepartamento(cmbDepartamento.getValue());
            colaborador.setCargo(cmbCargo.getValue());
            colaborador.setDataAdmissao(dpDataAdmissao.getValue());
            colaborador.setEstado(cmbEstado.getValue());

            colaboradorService.save(colaborador);
            carregarDados();
            
        } catch (Exception e) {
            modalManager.alert("Erro ao Salvar", "Não foi possível salvar os dados do colaborador.", "error", e);
        }
    }

    private void toggleStatus(Colaborador colaborador) {
        try {
            if (colaborador.getEstado() == Colaborador.EstadoColaborador.ATIVO) {
                colaboradorService.deactivate(colaborador.getId());
            } else {
                colaboradorService.activate(colaborador.getId());
            }
            carregarDados();
        } catch (Exception e) {
            modalManager.alert("Erro de Estado", "Não foi possível alterar o estado do colaborador.", "error", e);
        }
    }
}

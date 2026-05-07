package ao.allon.kubata.admin.view;

import ao.allon.kubata.core.ui.table.AdvancedTableView;
import ao.allon.kubata.core.ui.table.TableUtils;
import ao.allon.kubata.core.ui.table.TextTableCell;
import ao.allon.kubata.admin.service.PersistenceService;
import ao.allon.kubata.admin.service.SessionManager;
import ao.allon.kubata.admin.ui.modal.ModalManager;
import ao.allon.kubata.admin.ui.util.IconUtils;
import ao.allon.kubata.admin.ui.util.ThemeManager;
import ao.allon.kubata.core.domain.Empresa;
import ao.allon.kubata.core.domain.PerfilAcesso;
import ao.allon.kubata.core.domain.Role;
import ao.allon.kubata.core.domain.User;
import ao.allon.kubata.core.repository.EmpresaRepository;
import ao.allon.kubata.core.repository.PerfilAcessoRepository;
import ao.allon.kubata.core.repository.UserRepository;
import ao.allon.kubata.core.service.AcessoService;
import ao.allon.kubata.core.service.SecurityService;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.context.ApplicationContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class UtilizadoresView extends VBox {

    private final UserRepository userRepository;
    private final PerfilAcessoRepository perfilRepository;
    private final EmpresaRepository empresaRepository;
    private final AcessoService acessoService;
    private final SecurityService securityService;
    private final PasswordEncoder passwordEncoder;
    private final SessionManager sessionManager;
    private final ModalManager modalManager;
    private final PersistenceService persistenceService;

    private AdvancedTableView<User> table;
    private ObservableList<User> users;
    private TextField searchField;
    private Button btnNovo, btnEditar, btnClonar, btnRemover, btnResetPassword;

    public UtilizadoresView(UserRepository userRepository, 
                            PerfilAcessoRepository perfilRepository,
                            EmpresaRepository empresaRepository,
                            AcessoService acessoService, 
                            SecurityService securityService,
                            PasswordEncoder passwordEncoder, 
                            SessionManager sessionManager, 
                            ModalManager modalManager,
                            PersistenceService persistenceService) {
        this.userRepository = userRepository;
        this.perfilRepository = perfilRepository;
        this.empresaRepository = empresaRepository;
        this.acessoService = acessoService;
        this.securityService = securityService;
        this.passwordEncoder = passwordEncoder;
        this.sessionManager = sessionManager;
        this.modalManager = modalManager;
        this.persistenceService = persistenceService;

        users = FXCollections.observableArrayList();
        buildUI();
    }

    private boolean dataLoaded = false;

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        // Lazy load data when view is first shown
        if (!dataLoaded && getScene() != null) {
            dataLoaded = true;
            loadUsers();
        }
    }

    private void buildUI() {
        setSpacing(0);
        setPadding(Insets.EMPTY);

        HBox toolbar = buildToolbar();
        table = buildTable();

        getChildren().addAll(toolbar, table);
        VBox.setVgrow(table, Priority.ALWAYS);
    }

    private HBox buildToolbar() {
        HBox box = new HBox(10);
        box.getStyleClass().add("header-box");
        box.setPadding(new Insets(10, 15, 10, 15));
        box.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Gestão de Utilizadores");
        title.getStyleClass().add("h3");

        searchField = new TextField();
        searchField.setPromptText("Pesquisar por nome ou email...");
        searchField.setPrefWidth(280);
        searchField.textProperty().addListener((obs, oldVal, newVal) -> filterUsers(newVal));

        btnNovo = new Button("Novo", IconUtils.icon(Feather.PLUS, IconUtils.SIZE_SMALL));
        btnNovo.getStyleClass().add("button-primary");
        btnNovo.setDisable(!securityService.hasPermission(sessionManager.getUser(), "UTILIZADORES", "CRIAR"));
        btnNovo.setOnAction(e -> showUserDialog(null));

        btnEditar = new Button("Editar", IconUtils.icon(Feather.EDIT_2, IconUtils.SIZE_SMALL));
        btnEditar.getStyleClass().add("button-outlined");
        btnEditar.setDisable(true);
        btnEditar.setOnAction(e -> {
            User selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) showUserDialog(selected);
        });

        btnClonar = new Button("Clonar", IconUtils.icon(Feather.COPY, IconUtils.SIZE_SMALL));
        btnClonar.getStyleClass().add("button-outlined");
        btnClonar.setDisable(true);
        btnClonar.setOnAction(e -> cloneUser());

        btnRemover = new Button("Remover", IconUtils.icon(Feather.TRASH_2, IconUtils.SIZE_SMALL));
        btnRemover.getStyleClass().add("button-danger");
        btnRemover.setDisable(true);
        btnRemover.setOnAction(e -> removeUser());

        btnResetPassword = new Button("Reset Password", IconUtils.icon(Feather.KEY, IconUtils.SIZE_SMALL));
        btnResetPassword.getStyleClass().add("button-outlined");
        btnResetPassword.setDisable(true);
        btnResetPassword.setOnAction(e -> resetPassword());

        Button btnRefresh = new Button(null, IconUtils.icon(Feather.REFRESH_CW, IconUtils.SIZE_SMALL));
        btnRefresh.getStyleClass().add("button-outlined");
        btnRefresh.setOnAction(e -> {
            loadUsers();
            modalManager.alert("Atualização", "Lista de utilizadores atualizada com sucesso.", "info", null);
        });

        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        box.getChildren().addAll(title, spacer, searchField, btnNovo, btnEditar, btnClonar, btnRemover, btnResetPassword, btnRefresh);
        return box;
    }

    private AdvancedTableView<User> buildTable() {
        AdvancedTableView<User> tv = new AdvancedTableView<>();
        tv.setData(users);
        tv.setEditable(true);
        
        TableUtils.standardize(tv);
        
        TableColumn<User, String> colNome = new TableColumn<>("Nome");
        colNome.setCellValueFactory(new PropertyValueFactory<>("nome"));
        colNome.setCellFactory(tc -> TextTableCell.create());
        colNome.setOnEditCommit(event -> {
            User u = event.getRowValue();
            u.setNome(event.getNewValue());
            saveUserInline(u);
        });
        colNome.setPrefWidth(250);

        TableColumn<User, String> colEmail = new TableColumn<>("Email");
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colEmail.setCellFactory(tc -> TextTableCell.create());
        colEmail.setOnEditCommit(event -> {
            User u = event.getRowValue();
            u.setEmail(event.getNewValue());
            saveUserInline(u);
        });
        colEmail.setPrefWidth(250);

        TableColumn<User, Role> colRole = new TableColumn<>("Perfil (Role)");
        colRole.setCellValueFactory(col -> new SimpleObjectProperty<>(col.getValue().getRole()));
        colRole.setPrefWidth(150);

        TableColumn<User, String> colNif = new TableColumn<>("NIF");
        colNif.setCellValueFactory(new PropertyValueFactory<>("nif"));
        colNif.setCellFactory(tc -> TextTableCell.create());
        colNif.setOnEditCommit(event -> {
            User u = event.getRowValue();
            u.setNif(event.getNewValue());
            saveUserInline(u);
        });
        colNif.setPrefWidth(130);

        TableColumn<User, String> colTelefone = new TableColumn<>("Telefone");
        colTelefone.setCellValueFactory(new PropertyValueFactory<>("telefone"));
        colTelefone.setCellFactory(tc -> TextTableCell.create());
        colTelefone.setOnEditCommit(event -> {
            User u = event.getRowValue();
            u.setTelefone(event.getNewValue());
            saveUserInline(u);
        });
        colTelefone.setPrefWidth(150);

        TableColumn<User, Boolean> colAtivo = TableUtils.createCheckColumn("Ativo", col -> new SimpleBooleanProperty(col.getValue().getActive()));
        colAtivo.setPrefWidth(80);
        colAtivo.setEditable(true);

        TableColumn<User, Boolean> colMfa = TableUtils.createCheckColumn("MFA", col -> new SimpleBooleanProperty(col.getValue().isMfaEnabled()));
        colMfa.setPrefWidth(80);
        colMfa.setEditable(true);

        TableColumn<User, Integer> colFailed = new TableColumn<>("Falhas Login");
        colFailed.setCellValueFactory(col -> new javafx.beans.property.SimpleIntegerProperty(col.getValue().getFailedAttempts()).asObject());
        colFailed.setPrefWidth(100);

        TableColumn<User, LocalDateTime> colUltimoAcesso = new TableColumn<>("Último Acesso");
        colUltimoAcesso.setCellValueFactory(col -> new SimpleObjectProperty<>(col.getValue().getUltimoAcesso()));
        colUltimoAcesso.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                setText(empty || item == null ? "-" : item.toString().replace("T", " "));
            }
        });
        colUltimoAcesso.setPrefWidth(180);

        TableColumn<User, LocalDateTime> colBloqueio = new TableColumn<>("Bloqueio");
        colBloqueio.setCellValueFactory(col -> new SimpleObjectProperty<>(col.getValue().getLockoutEnd()));
        colBloqueio.setPrefWidth(180);

        tv.getColumns().addAll(colNome, colEmail, colRole, colNif, colTelefone, colAtivo, colMfa, colFailed, colUltimoAcesso, colBloqueio);
        
        // Listener para seleção
        tv.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            boolean hasSelection = newSelection != null;
            User currentUser = sessionManager.getUser();
            
            btnEditar.setDisable(!hasSelection || !securityService.hasPermission(currentUser, "UTILIZADORES", "EDITAR"));
            btnClonar.setDisable(!hasSelection || !securityService.hasPermission(currentUser, "UTILIZADORES", "CRIAR"));
            btnRemover.setDisable(!hasSelection || !securityService.hasPermission(currentUser, "UTILIZADORES", "REMOVER"));
            btnResetPassword.setDisable(!hasSelection || !securityService.hasPermission(currentUser, "UTILIZADORES", "EDITAR"));
        });

        return tv;
    }

    private void saveUserInline(User u) {
        persistenceService.saveAsync(userRepository, u, "UTILIZADOR", 
                "Atualização inline do utilizador: " + u.getEmail(), null);
    }

    private void loadUsers() {
        table.setLoading(true);
        Platform.runLater(() -> {
            try {
                List<User> all = userRepository.findAll();
                users.setAll(all);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                table.setLoading(false);
            }
        });
    }

    private void filterUsers(String query) {
        if (query == null || query.isBlank()) {
            table.setFilter(u -> true);
            return;
        }
        String lower = query.toLowerCase();
        table.setFilter(u -> u.getNome().toLowerCase().contains(lower) ||
                             u.getEmail().toLowerCase().contains(lower));
    }

    public void showUserDialog(User user) {
        boolean isNew = (user == null);
        
        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // Tab 1: Dados Básicos
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);
        grid.setPadding(new Insets(20));
        
        // Configuração das colunas: label (fixa) e campo (expansível)
        ColumnConstraints colLabel = new ColumnConstraints();
        colLabel.setMinWidth(80);
        colLabel.setPrefWidth(100);
        
        ColumnConstraints colField = new ColumnConstraints();
        colField.setMinWidth(250);
        colField.setPrefWidth(350);
        colField.setHgrow(Priority.ALWAYS);
        
        grid.getColumnConstraints().addAll(colLabel, colField);

        TextField txtNome = new TextField(user != null ? user.getNome() : "");
        txtNome.setPromptText("Nome completo");
        txtNome.setPrefWidth(350);
        txtNome.setMinWidth(250);
        txtNome.setMaxWidth(Double.MAX_VALUE);

        TextField txtEmail = new TextField(user != null ? user.getEmail() : "");
        txtEmail.setPromptText("Email corporativo");
        txtEmail.setPrefWidth(350);
        txtEmail.setMinWidth(250);
        txtEmail.setMaxWidth(Double.MAX_VALUE);

        PasswordField txtSenha = new PasswordField();
        txtSenha.setPromptText(isNew ? "Senha inicial" : "Nova senha (opcional)");
        txtSenha.setPrefWidth(350);
        txtSenha.setMinWidth(250);
        txtSenha.setMaxWidth(Double.MAX_VALUE);
        if (isNew) txtSenha.setText(generateRandomPassword());

        ComboBox<ao.allon.kubata.core.domain.Role> cmbRole = new ComboBox<>(FXCollections.observableArrayList(ao.allon.kubata.core.domain.Role.values()));
        cmbRole.setValue(user != null ? user.getRole() : ao.allon.kubata.core.domain.Role.USER);
        cmbRole.setPrefWidth(350);
        cmbRole.setMinWidth(250);
        cmbRole.setMaxWidth(Double.MAX_VALUE);

        ComboBox<Empresa> cbEmpresa = new ComboBox<>();
        cbEmpresa.setItems(FXCollections.observableArrayList(empresaRepository.findAll()));
        cbEmpresa.setValue(user != null ? user.getEmpresa() : null);
        cbEmpresa.setPromptText("Selecione a Empresa principal");
        cbEmpresa.setPrefWidth(350);
        cbEmpresa.setMinWidth(250);
        cbEmpresa.setMaxWidth(Double.MAX_VALUE);

        TextField txtNif = new TextField(user != null && user.getNif() != null ? user.getNif() : "");
        txtNif.setPrefWidth(350);
        txtNif.setMinWidth(250);
        txtNif.setMaxWidth(Double.MAX_VALUE);
        
        TextField txtTelefone = new TextField(user != null && user.getTelefone() != null ? user.getTelefone() : "");
        txtTelefone.setPrefWidth(350);
        txtTelefone.setMinWidth(250);
        txtTelefone.setMaxWidth(Double.MAX_VALUE);
        
        CheckBox chkAtivo = new CheckBox("Utilizador Ativo");
        chkAtivo.setSelected(user == null || user.getActive());

        grid.add(new Label("Nome:*"), 0, 0);
        grid.add(txtNome, 1, 0);
        GridPane.setHgrow(txtNome, Priority.ALWAYS);
        
        grid.add(new Label("Email:*"), 0, 1);
        grid.add(txtEmail, 1, 1);
        GridPane.setHgrow(txtEmail, Priority.ALWAYS);
        
        grid.add(new Label("Empresa:*"), 0, 2);
        grid.add(cbEmpresa, 1, 2);
        GridPane.setHgrow(cbEmpresa, Priority.ALWAYS);
        
        grid.add(new Label("Senha:*"), 0, 3);
        grid.add(txtSenha, 1, 3);
        GridPane.setHgrow(txtSenha, Priority.ALWAYS);
        
        grid.add(new Label("Role (Base):"), 0, 4);
        grid.add(cmbRole, 1, 4);
        GridPane.setHgrow(cmbRole, Priority.ALWAYS);
        
        grid.add(new Label("NIF:"), 0, 5);
        grid.add(txtNif, 1, 5);
        GridPane.setHgrow(txtNif, Priority.ALWAYS);
        
        grid.add(new Label("Telefone:"), 0, 6);
        grid.add(txtTelefone, 1, 6);
        GridPane.setHgrow(txtTelefone, Priority.ALWAYS);
        
        grid.add(chkAtivo, 1, 7);

        Tab tabDados = new Tab("Dados Básicos", grid);

        // Tab 2: Perfis de Acesso
        VBox perfisBox = new VBox(10);
        perfisBox.setPadding(new Insets(20));
        perfisBox.getChildren().add(new Label("Selecione os Perfis de Acesso para este utilizador:"));

        ListView<PerfilAcesso> listPerfis = new ListView<>();
        listPerfis.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        
        // Carrega perfis baseados na empresa selecionada + globais
        cbEmpresa.valueProperty().addListener((obs, old, newVal) -> {
            List<PerfilAcesso> available = new ArrayList<>();
            available.addAll(perfilRepository.findByEmpresaIsNull()); // Globais
            if (newVal != null) {
                available.addAll(perfilRepository.findByEmpresa(newVal));
            }
            listPerfis.setItems(FXCollections.observableArrayList(available));
        });
        
        // Trigger inicial
        cbEmpresa.setValue(cbEmpresa.getValue());

        if (user != null && user.getPerfis() != null) {
            Platform.runLater(() -> {
                for (PerfilAcesso p : user.getPerfis()) {
                    listPerfis.getSelectionModel().select(p);
                }
            });
        }

        perfisBox.getChildren().add(listPerfis);
        VBox.setVgrow(listPerfis, Priority.ALWAYS);
        Tab tabPerfis = new Tab("Perfis e Segurança", perfisBox);

        tabs.getTabs().addAll(tabDados, tabPerfis);

        // Configuração com tamanho fixo para o modal
        ModalManager.ModalConfig config = new ModalManager.ModalConfig()
                .size(550, 500)
                .minSize(500, 450);
        
        modalManager.showConfirmModal(tabs, isNew ? "Novo Utilizador" : "Editar Utilizador: " + user.getNome(), () -> {
            if (txtNome.getText().isBlank() || txtEmail.getText().isBlank() || cbEmpresa.getValue() == null) {
                modalManager.alert("Aviso", "Nome, Email e Empresa são obrigatórios.", "warning", null);
                return;
            }

            User u = (isNew) ? new User() : user;
            u.setNome(txtNome.getText());
            u.setEmail(txtEmail.getText());
            u.setEmpresa(cbEmpresa.getValue());
            u.setRole(cmbRole.getValue());
            u.setNif(txtNif.getText());
            u.setTelefone(txtTelefone.getText());
            u.setActive(chkAtivo.isSelected());
            
            Set<PerfilAcesso> selectedPerfis = new HashSet<>(listPerfis.getSelectionModel().getSelectedItems());
            u.setPerfis(selectedPerfis);

            if (!txtSenha.getText().isBlank()) {
                u.setPassword(passwordEncoder.encode(txtSenha.getText()));
            }

            persistenceService.saveAsync(userRepository, u, "UTILIZADOR", 
                    (isNew ? "Criado" : "Atualizado") + " utilizador: " + u.getEmail(),
                    saved -> loadUsers());
        }, null, config);
    }

    private void removeUser() {
        User selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            modalManager.alert("Aviso", "Selecione um utilizador para remover.", "warning", null);
            return;
        }

        if (selected.getEmail().equals(sessionManager.getUser().getEmail())) {
            modalManager.alert("Aviso", "Não pode remover o seu próprio utilizador.", "warning", null);
            return;
        }

        modalManager.showConfirmModal(new Label("Tem certeza que deseja remover o utilizador: " + selected.getNome() + "?"),
                "Remover Utilizador", () -> {
            persistenceService.deleteAsync(userRepository, selected, null, "UTILIZADOR", 
                    "Removido utilizador: " + selected.getEmail(), this::loadUsers);
        }, null);
    }

    private void cloneUser() {
        User selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            modalManager.alert("Aviso", "Selecione um utilizador para clonar.", "warning", null);
            return;
        }

        User clone = new User();
        clone.setNome(selected.getNome() + " (Cópia)");
        clone.setEmail("copy_" + System.currentTimeMillis() + "_" + selected.getEmail());
        clone.setRole(selected.getRole());
        clone.setNif(selected.getNif());
        clone.setTelefone(selected.getTelefone());
        clone.setActive(true);
        clone.setPassword(selected.getPassword());

        persistenceService.saveAsync(userRepository, clone, "UTILIZADOR", 
                "Clonado utilizador: " + selected.getEmail() + " para " + clone.getEmail(),
                saved -> loadUsers());
    }

    private void resetPassword() {
        User selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            modalManager.alert("Aviso", "Selecione um utilizador para reset password.", "warning", null);
            return;
        }

        String newPassword = generateRandomPassword();
        selected.setPassword(passwordEncoder.encode(newPassword));
        
        persistenceService.saveAsync(userRepository, selected, "UTILIZADOR", 
                "Reset de password para utilizador: " + selected.getEmail(),
                saved -> {
                    modalManager.alert("Password Redefinida", "A nova password para " + selected.getEmail() + " é: " + newPassword, "info", null);
                    loadUsers();
                });
    }

    private String generateRandomPassword() {
        return "Admin@" + (1000 + new Random().nextInt(9000));
    }
}

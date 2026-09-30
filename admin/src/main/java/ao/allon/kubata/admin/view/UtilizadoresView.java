package ao.allon.kubata.admin.view;

import ao.allon.kubata.admin.service.PersistenceService;
import ao.allon.kubata.admin.service.SessionManager;
import ao.allon.kubata.admin.ui.modal.ModalManager;
import ao.allon.kubata.admin.ui.util.IconUtils;
import ao.allon.kubata.core.domain.Empresa;
import ao.allon.kubata.core.domain.PerfilAcesso;
import ao.allon.kubata.core.domain.PermissaoPerfil;
import ao.allon.kubata.core.domain.Role;
import ao.allon.kubata.core.domain.User;
import ao.allon.kubata.core.repository.EmpresaRepository;
import ao.allon.kubata.core.repository.PerfilAcessoRepository;
import ao.allon.kubata.core.repository.UserRepository;
import ao.allon.kubata.core.service.AcessoService;
import ao.allon.kubata.core.service.PasswordResetService;
import ao.allon.kubata.core.service.SecurityService;
import ao.allon.kubata.core.ui.table.AdvancedTableView;
import ao.allon.kubata.core.ui.table.TableUtils;
import ao.allon.kubata.core.ui.table.TextTableCell;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class UtilizadoresView extends VBox {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final UserRepository userRepository;
    private final PerfilAcessoRepository perfilRepository;
    private final EmpresaRepository empresaRepository;
    private final AcessoService acessoService;
    private final PasswordResetService passwordResetService;
    private final SecurityService securityService;
    private final PasswordEncoder passwordEncoder;
    private final SessionManager sessionManager;
    private final ModalManager modalManager;
    private final PersistenceService persistenceService;

    private final ObservableList<User> users = FXCollections.observableArrayList();

    private AdvancedTableView<User> table;
    private TextField searchField;
    private ComboBox<Empresa> empresaFilter;
    private ComboBox<Role> roleFilter;
    private ComboBox<String> statusFilter;
    private ComboBox<String> mfaFilter;

    private Label totalValue;
    private Label activeValue;
    private Label blockedValue;
    private Label mfaValue;
    private Label statusLabel;

    private VBox detailsPane;
    private Label detailName;
    private Label detailEmail;
    private Label detailStatus;
    private Label detailEmpresa;
    private Label detailRole;
    private Label detailDepartamento;
    private Label detailCargo;
    private Label detailUltimoAcesso;
    private Label detailIp;
    private Label detailMfa;
    private Label detailPassword;
    private Label detailFalhas;

    private Button btnNovo;
    private Button btnEditar;
    private Button btnClonar;
    private Button btnStatus;
    private Button btnDesbloquear;
    private Button btnResetPassword;

    private boolean dataLoaded;

    public UtilizadoresView(UserRepository userRepository,
                            PerfilAcessoRepository perfilRepository,
                            EmpresaRepository empresaRepository,
                            AcessoService acessoService,
                            PasswordResetService passwordResetService,
                            SecurityService securityService,
                            PasswordEncoder passwordEncoder,
                            SessionManager sessionManager,
                            ModalManager modalManager,
                            PersistenceService persistenceService) {
        this.userRepository = userRepository;
        this.perfilRepository = perfilRepository;
        this.empresaRepository = empresaRepository;
        this.acessoService = acessoService;
        this.passwordResetService = passwordResetService;
        this.securityService = securityService;
        this.passwordEncoder = passwordEncoder;
        this.sessionManager = sessionManager;
        this.modalManager = modalManager;
        this.persistenceService = persistenceService;

        buildUI();
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        if (!dataLoaded && getScene() != null) {
            dataLoaded = true;
            refreshActionPermissions();
            loadUsers();
        }
    }

    private void buildUI() {
        setSpacing(0);
        setPadding(Insets.EMPTY);
        getStyleClass().add("kubata-users-page");

        VBox header = buildPageHeader();
        Node workspace = buildWorkspace();
        HBox footer = buildStatusBar();

        getChildren().addAll(header, workspace, footer);
        VBox.setVgrow(workspace, Priority.ALWAYS);
    }

    private VBox buildPageHeader() {
        VBox header = new VBox(12);
        header.getStyleClass().add("kubata-users-header");
        header.setPadding(new Insets(18, 22, 14, 22));

        HBox titleLine = new HBox(12);
        titleLine.setAlignment(Pos.CENTER_LEFT);

        Label icon = new Label("", IconUtils.icon(Feather.USERS, 24));
        icon.getStyleClass().add("kubata-users-title-icon");

        VBox titleBox = new VBox(2);
        Label title = new Label("Utilizadores");
        title.getStyleClass().add("kubata-users-title");

        Label subtitle = new Label(
                "Administração central de contas, acessos, segurança e contexto empresarial."
        );
        subtitle.getStyleClass().add("kubata-users-subtitle");

        titleBox.getChildren().addAll(title, subtitle);
        titleLine.getChildren().addAll(icon, titleBox);

        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        btnNovo = new Button(
                "Novo Utilizador",
                IconUtils.icon(Feather.USER_PLUS, IconUtils.SIZE_SMALL)
        );
        btnNovo.getStyleClass().add("button-primary");
        btnNovo.setOnAction(e -> showUserDialog(null));
        btnNovo.setDisable(true);

        Button btnRefresh = new Button(
                "Actualizar",
                IconUtils.icon(Feather.REFRESH_CW, IconUtils.SIZE_SMALL)
        );
        btnRefresh.getStyleClass().add("button-outlined");
        btnRefresh.setOnAction(e -> loadUsers());

        titleLine.getChildren().addAll(spacer, btnRefresh, btnNovo);

        totalValue = new Label("0");
        activeValue = new Label("0");
        blockedValue = new Label("0");
        mfaValue = new Label("0");

        HBox kpis = new HBox(10,
                createKpi("UTILIZADORES", Feather.USERS, totalValue),
                createKpi("ACTIVOS", Feather.CHECK_CIRCLE, activeValue),
                createKpi("BLOQUEADOS", Feather.LOCK, blockedValue),
                createKpi("MFA", Feather.SHIELD, mfaValue)
        );
        kpis.setFillHeight(true);

        header.getChildren().addAll(titleLine, kpis);
        return header;
    }

    private VBox createKpi(String title, Feather icon, Label value) {
        VBox card = new VBox(2);
        card.getStyleClass().add("kubata-users-kpi");
        card.setPadding(new Insets(10, 14, 10, 14));
        card.setMinWidth(170);

        HBox top = new HBox(7);
        top.setAlignment(Pos.CENTER_LEFT);

        Label iconLabel = new Label("", IconUtils.icon(icon, 14));
        iconLabel.getStyleClass().add("kubata-users-kpi-icon");

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("kubata-users-kpi-title");
        top.getChildren().addAll(iconLabel, titleLabel);

        value.getStyleClass().add("kubata-users-kpi-value");

        card.getChildren().addAll(top, value);
        return card;
    }

    private BorderPane buildWorkspace() {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("kubata-users-workspace");

        root.setTop(buildFilterBar());
        root.setCenter(buildMainArea());
        return root;
    }

    private HBox buildFilterBar() {
        HBox bar = new HBox(8);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(10, 14, 10, 14));
        bar.getStyleClass().add("kubata-users-filter-bar");

        searchField = new TextField();
        searchField.setPromptText("Pesquisar por nome, email, NIF, telefone, departamento ou cargo...");
        searchField.setPrefWidth(360);
        searchField.textProperty().addListener((obs, old, value) -> applyFilters());

        empresaFilter = new ComboBox<>();
        empresaFilter.getItems().add(null);
        empresaFilter.getItems().addAll(empresaRepository.findAll());
        empresaFilter.setPromptText("Empresa");
        empresaFilter.setConverter(new StringConverter<>() {
            @Override
            public String toString(Empresa object) {
                return object == null ? "Todas as empresas" : object.getNome();
            }

            @Override
            public Empresa fromString(String string) {
                return null;
            }
        });
        empresaFilter.setPrefWidth(190);
        empresaFilter.valueProperty().addListener((obs, old, value) -> applyFilters());

        roleFilter = new ComboBox<>();
        roleFilter.getItems().add(null);
        roleFilter.getItems().addAll(Role.values());
        roleFilter.setPromptText("Role");
        roleFilter.setConverter(new StringConverter<>() {
            @Override
            public String toString(Role object) {
                return object == null ? "Todas as roles" : roleLabel(object);
            }

            @Override
            public Role fromString(String string) {
                return null;
            }
        });
        roleFilter.setPrefWidth(170);
        roleFilter.valueProperty().addListener((obs, old, value) -> applyFilters());

        statusFilter = new ComboBox<>(FXCollections.observableArrayList(
                "Todos",
                "Activos",
                "Inactivos",
                "Bloqueados",
                "Senha expirada"
        ));
        statusFilter.setValue("Todos");
        statusFilter.setPrefWidth(150);
        statusFilter.valueProperty().addListener((obs, old, value) -> applyFilters());

        mfaFilter = new ComboBox<>(FXCollections.observableArrayList(
                "MFA: Todos",
                "MFA: Activo",
                "MFA: Inactivo"
        ));
        mfaFilter.setValue("MFA: Todos");
        mfaFilter.setPrefWidth(145);
        mfaFilter.valueProperty().addListener((obs, old, value) -> applyFilters());

        Button limpar = new Button(
                "Limpar",
                IconUtils.icon(Feather.X, 13)
        );
        limpar.getStyleClass().add("button-outlined");
        limpar.setOnAction(e -> {
            searchField.clear();
            empresaFilter.setValue(null);
            roleFilter.setValue(null);
            statusFilter.setValue("Todos");
            mfaFilter.setValue("MFA: Todos");
        });

        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label hint = new Label("Ctrl+F para pesquisar");
        hint.getStyleClass().add("kubata-users-filter-hint");

        bar.getChildren().addAll(
                searchField,
                empresaFilter,
                roleFilter,
                statusFilter,
                mfaFilter,
                limpar,
                spacer,
                hint
        );
        return bar;
    }

    private SplitPane buildMainArea() {
        table = buildTable();

        detailsPane = buildDetailsPane();

        SplitPane split = new SplitPane(table, new ScrollPane(detailsPane));
        split.setDividerPositions(0.72);
        split.setStyle("-fx-background-color: transparent;");
        split.getItems().get(1).getStyleClass().add("kubata-users-details-wrapper");
        return split;
    }

    private AdvancedTableView<User> buildTable() {
        AdvancedTableView<User> tv = new AdvancedTableView<>();
        tv.setData(users);
        tv.setEditable(true);
        tv.setEntityName("Utilizador");
        tv.setPlaceholder(new Label("Nenhum utilizador corresponde aos filtros activos."));

        TableUtils.standardize(tv);

        TableColumn<User, String> colNome = new TableColumn<>("Nome");
        colNome.setCellValueFactory(new PropertyValueFactory<>("nome"));
        colNome.setCellFactory(tc -> TextTableCell.create());
        colNome.setPrefWidth(210);

        TableColumn<User, String> colEmail = new TableColumn<>("Email");
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colEmail.setCellFactory(tc -> TextTableCell.create());
        colEmail.setPrefWidth(235);

        TableColumn<User, String> colEmpresa = new TableColumn<>("Empresa");
        colEmpresa.setCellValueFactory(cell ->
                new SimpleStringPropertySafe(companyName(cell.getValue().getEmpresa())));
        colEmpresa.setPrefWidth(180);

        TableColumn<User, Role> colRole = new TableColumn<>("Função");
        colRole.setCellValueFactory(cell ->
                new SimpleObjectProperty<>(cell.getValue().getRole()));
        colRole.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Role item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "-" : roleLabel(item));
            }
        });
        colRole.setPrefWidth(160);

        TableColumn<User, Boolean> colAtivo = TableUtils.createCheckColumn(
                "Activo",
                cell -> new SimpleBooleanProperty(Boolean.TRUE.equals(cell.getValue().getActive()))
        );
        colAtivo.setPrefWidth(82);
        colAtivo.setEditable(false);

        TableColumn<User, String> colEstado = new TableColumn<>("Estado");
        colEstado.setCellValueFactory(cell ->
                new SimpleStringPropertySafe(userStatus(cell.getValue())));
        colEstado.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                getStyleClass().removeAll(
                        "kubata-user-status-active",
                        "kubata-user-status-inactive",
                        "kubata-user-status-locked",
                        "kubata-user-status-expired"
                );
                if (!empty && item != null) {
                    switch (item) {
                        case "Activo" -> getStyleClass().add("kubata-user-status-active");
                        case "Inactivo" -> getStyleClass().add("kubata-user-status-inactive");
                        case "Bloqueado" -> getStyleClass().add("kubata-user-status-locked");
                        case "Senha expirada" -> getStyleClass().add("kubata-user-status-expired");
                        default -> { }
                    }
                }
            }
        });
        colEstado.setPrefWidth(125);

        TableColumn<User, Boolean> colMfa = TableUtils.createCheckColumn(
                "MFA",
                cell -> new SimpleBooleanProperty(cell.getValue().isMfaEnabled())
        );
        colMfa.setPrefWidth(70);
        colMfa.setEditable(false);

        TableColumn<User, LocalDateTime> colUltimoAcesso =
                new TableColumn<>("Último acesso");
        colUltimoAcesso.setCellValueFactory(cell ->
                new SimpleObjectProperty<>(cell.getValue().getUltimoAcesso()));
        colUltimoAcesso.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "-" : DATE_TIME_FORMAT.format(item));
            }
        });
        colUltimoAcesso.setPrefWidth(150);

        tv.getColumns().addAll(
                colNome, colEmail, colEmpresa, colRole,
                colAtivo, colEstado, colMfa, colUltimoAcesso
        );

        tv.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> updateSelection(selected));

        tv.setOnEdit(this::showUserDialog);
        tv.setOnDelete(this::confirmDelete);
        tv.setOnViewDetails(this::showDetailsModal);
        tv.setOnRefresh(this::loadUsers);

        return tv;
    }

    private VBox buildDetailsPane() {
        VBox pane = new VBox(14);
        pane.setPadding(new Insets(18));
        pane.setMinWidth(280);
        pane.setPrefWidth(330);
        pane.getStyleClass().add("kubata-users-details");

        HBox title = new HBox(10);
        title.setAlignment(Pos.CENTER_LEFT);

        StackPane avatar = new StackPane();
        avatar.getStyleClass().add("kubata-users-avatar");
        Label avatarText = new Label("U");
        avatarText.getStyleClass().add("kubata-users-avatar-text");
        avatar.getChildren().add(avatarText);

        VBox identity = new VBox(2);
        detailName = new Label("Nenhum utilizador seleccionado");
        detailName.getStyleClass().add("kubata-users-detail-name");
        detailEmail = new Label("Seleccione uma linha para consultar os detalhes.");
        detailEmail.getStyleClass().add("kubata-users-detail-email");
        identity.getChildren().addAll(detailName, detailEmail);

        title.getChildren().addAll(avatar, identity);

        Separator separator = new Separator();

        detailStatus = detailValue("Estado", "-");
        detailEmpresa = detailValue("Empresa", "-");
        detailRole = detailValue("Função", "-");
        detailDepartamento = detailValue("Departamento", "-");
        detailCargo = detailValue("Cargo", "-");
        detailUltimoAcesso = detailValue("Último acesso", "-");
        detailIp = detailValue("IP de acesso", "-");
        detailMfa = detailValue("MFA", "-");
        detailPassword = detailValue("Credencial", "-");
        detailFalhas = detailValue("Tentativas", "-");

        pane.getChildren().addAll(
                title,
                separator,
                detailStatus, detailEmpresa, detailRole,
                detailDepartamento, detailCargo,
                detailUltimoAcesso, detailIp,
                detailMfa, detailPassword, detailFalhas
        );

        Separator actionsSeparator = new Separator();
        Label actionsTitle = new Label("Acções do utilizador");
        actionsTitle.getStyleClass().add("kubata-users-section-title");

        btnEditar = detailButton("Editar", Feather.EDIT_2, "button-outlined");
        btnClonar = detailButton("Clonar", Feather.COPY, "button-outlined");
        btnStatus = detailButton("Desactivar", Feather.POWER, "button-outlined");
        btnDesbloquear = detailButton("Desbloquear", Feather.UNLOCK, "button-outlined");
        btnResetPassword = detailButton("Redefinir senha", Feather.KEY, "button-outlined");

        btnEditar.setOnAction(e -> selectedUser().ifPresent(this::showUserDialog));
        btnClonar.setOnAction(e -> cloneUser());
        btnStatus.setOnAction(e -> toggleSelectedStatus());
        btnDesbloquear.setOnAction(e -> unlockSelectedUser());
        btnResetPassword.setOnAction(e -> resetPassword());

        GridPane actions = new GridPane();
        actions.setHgap(7);
        actions.setVgap(7);
        actions.add(btnEditar, 0, 0);
        actions.add(btnClonar, 1, 0);
        actions.add(btnStatus, 0, 1);
        actions.add(btnDesbloquear, 1, 1);
        actions.add(btnResetPassword, 0, 2, 2, 1);

        GridPane.setHgrow(btnEditar, Priority.ALWAYS);
        GridPane.setHgrow(btnClonar, Priority.ALWAYS);
        GridPane.setHgrow(btnStatus, Priority.ALWAYS);
        GridPane.setHgrow(btnDesbloquear, Priority.ALWAYS);
        GridPane.setHgrow(btnResetPassword, Priority.ALWAYS);

        pane.getChildren().addAll(actionsSeparator, actionsTitle, actions);
        pane.setDisable(false);
        updateSelection(null);
        return pane;
    }

    private Label detailValue(String label, String value) {
        Label valueLabel = new Label(value);
        valueLabel.setWrapText(true);
        valueLabel.getStyleClass().add("kubata-users-detail-value");

        VBox box = new VBox(2);
        box.getStyleClass().add("kubata-users-detail-row");
        Label title = new Label(label.toUpperCase());
        title.getStyleClass().add("kubata-users-detail-label");
        box.getChildren().addAll(title, valueLabel);

        return valueLabel;
    }

    private Button detailButton(String text, Feather icon, String styleClass) {
        Button button = new Button(text, IconUtils.icon(icon, 12));
        button.setMaxWidth(Double.MAX_VALUE);
        button.getStyleClass().add(styleClass);
        return button;
    }

    private HBox buildStatusBar() {
        HBox bar = new HBox(10);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(7, 14, 7, 14));
        bar.getStyleClass().add("kubata-users-statusbar");

        statusLabel = new Label("A carregar...");
        statusLabel.getStyleClass().add("kubata-users-statusbar-text");

        Label hint = new Label("Duplo clique: detalhes  •  Botão direito: operações");
        hint.getStyleClass().add("kubata-users-statusbar-hint");

        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        bar.getChildren().addAll(statusLabel, spacer, hint);
        return bar;
    }

    private void updateSelection(User selected) {
        boolean hasSelection = selected != null;
        User current = sessionManager.getUser();

        detailName.setText(hasSelection ? safe(selected.getNome(), "Utilizador") : "Nenhum utilizador seleccionado");
        detailEmail.setText(hasSelection
                ? safe(selected.getEmail(), "Sem email")
                : "Seleccione uma linha para consultar os detalhes.");

        if (!hasSelection) {
            setDetails("-", "-", "-", "-", "-", "-", "-", "-", "-", "-");
        } else {
            setDetails(
                    userStatus(selected),
                    companyName(selected.getEmpresa()),
                    roleLabel(selected.getRole()),
                    safe(selected.getDepartamento(), "-"),
                    safe(selected.getCargo(), "-"),
                    formatDateTime(selected.getUltimoAcesso()),
                    safe(selected.getUltimoIpLogin(), "-"),
                    selected.isMfaEnabled() ? "Activo" : "Inactivo",
                    credentialStatus(selected),
                    String.valueOf(selected.getFailedAttempts())
            );
        }

        btnEditar.setDisable(!hasSelection || !can("EDITAR"));
        btnClonar.setDisable(!hasSelection || !can("CRIAR"));
        btnResetPassword.setDisable(!hasSelection || !can("EDITAR"));
        btnDesbloquear.setDisable(!hasSelection || !can("EDITAR") || !isBlocked(selected));
        btnStatus.setDisable(!hasSelection || !can("EDITAR") ||
                (current != null && selected != null && current.getId() != null && current.getId().equals(selected.getId())));

        if (selected != null && Boolean.TRUE.equals(selected.getActive())) {
            btnStatus.setText("Desactivar");
            btnStatus.setGraphic(IconUtils.icon(Feather.POWER, 12));
        } else {
            btnStatus.setText("Activar");
            btnStatus.setGraphic(IconUtils.icon(Feather.CHECK_CIRCLE, 12));
        }
    }

    private void setDetails(String status, String empresa, String role, String departamento,
                            String cargo, String ultimoAcesso, String ip, String mfa,
                            String password, String falhas) {
        detailStatus.setText(status);
        detailEmpresa.setText(empresa);
        detailRole.setText(role);
        detailDepartamento.setText(departamento);
        detailCargo.setText(cargo);
        detailUltimoAcesso.setText(ultimoAcesso);
        detailIp.setText(ip);
        detailMfa.setText(mfa);
        detailPassword.setText(password);
        detailFalhas.setText(falhas);
    }

    private void loadUsers() {
        refreshActionPermissions();

        if (table != null) {
            table.setLoading(true);
        }

        Platform.runLater(() -> {
            try {
                users.setAll(userRepository.findAll());
                refreshFilters();
                applyFilters();
                updateSummary();
            } catch (Exception e) {
                e.printStackTrace();
                modalManager.showErrorModal(
                        "Erro ao carregar utilizadores",
                        "Não foi possível carregar a lista de utilizadores.",
                        e
                );
            } finally {
                if (table != null) {
                    table.setLoading(false);
                }
            }
        });
    }

    private void refreshFilters() {
        Empresa selectedEmpresa = empresaFilter == null ? null : empresaFilter.getValue();
        if (empresaFilter != null) {
            empresaFilter.getItems().setAll();
            empresaFilter.getItems().add(null);
            empresaFilter.getItems().addAll(empresaRepository.findAll());
            empresaFilter.setValue(selectedEmpresa);
        }
    }

    private void applyFilters() {
        if (table == null) {
            return;
        }

        String query = searchField == null ? "" : searchField.getText().trim().toLowerCase();
        Empresa empresa = empresaFilter == null ? null : empresaFilter.getValue();
        Role role = roleFilter == null ? null : roleFilter.getValue();
        String status = statusFilter == null ? "Todos" : statusFilter.getValue();
        String mfa = mfaFilter == null ? "MFA: Todos" : mfaFilter.getValue();

        table.setFilter(user -> {
            if (user == null) {
                return false;
            }

            boolean textMatch = query.isBlank()
                    || contains(user.getNome(), query)
                    || contains(user.getEmail(), query)
                    || contains(user.getNif(), query)
                    || contains(user.getTelefone(), query)
                    || contains(user.getDepartamento(), query)
                    || contains(user.getCargo(), query);

            boolean empresaMatch = empresa == null || sameId(user.getEmpresa(), empresa);
            boolean roleMatch = role == null || user.getRole() == role;

            boolean statusMatch = switch (status) {
                case "Activos" -> Boolean.TRUE.equals(user.getActive()) && !isBlocked(user)
                        && !"Senha expirada".equals(credentialStatus(user));
                case "Inactivos" -> !Boolean.TRUE.equals(user.getActive());
                case "Bloqueados" -> isBlocked(user);
                case "Senha expirada" -> "Senha expirada".equals(credentialStatus(user));
                default -> true;
            };

            boolean mfaMatch = switch (mfa) {
                case "MFA: Activo" -> user.isMfaEnabled();
                case "MFA: Inactivo" -> !user.isMfaEnabled();
                default -> true;
            };

            return textMatch && empresaMatch && roleMatch && statusMatch && mfaMatch;
        });

        updateFilteredCount();
    }

    private void updateSummary() {
        long total = users.size();
        long active = users.stream()
                .filter(u -> Boolean.TRUE.equals(u.getActive()))
                .count();
        long blocked = users.stream()
                .filter(this::isBlocked)
                .count();
        long mfa = users.stream()
                .filter(User::isMfaEnabled)
                .count();

        totalValue.setText(String.valueOf(total));
        activeValue.setText(String.valueOf(active));
        blockedValue.setText(String.valueOf(blocked));
        mfaValue.setText(String.valueOf(mfa));
        updateFilteredCount();
    }

    private void updateFilteredCount() {
        if (statusLabel == null || table == null) {
            return;
        }
        long visible = table.getItems().size();
        statusLabel.setText(visible + " de " + users.size() + " utilizador(es)");
    }

    public void showUserDialog(User user) {
        boolean isNew = user == null;

        if (!isNew && user.getId() != null) {
            user = userRepository.findByIdWithPerfis(user.getId()).orElse(user);
        }

        final User formUser = user;
        final boolean isSelf = !isNew
                && sessionManager.getUser() != null
                && sessionManager.getUser().getId() != null
                && sessionManager.getUser().getId().equals(formUser.getId());

        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getStyleClass().add("kubata-users-form-tabs");

        // ================================================================
        // GERAL — identidade + organização
        // ================================================================
        VBox generalPage = new VBox(14);
        generalPage.setPadding(new Insets(16));
        generalPage.getStyleClass().add("kubata-users-form-page");

        HBox identityCard = new HBox(14);
        identityCard.getStyleClass().add("kubata-users-identity-card");
        identityCard.setPadding(new Insets(14));
        identityCard.setAlignment(Pos.CENTER_LEFT);

        StackPane avatar = new StackPane();
        avatar.getStyleClass().add("kubata-users-form-avatar");
        Label avatarText = new Label(initials(isNew ? "Utilizador" : formUser.getNome()));
        avatarText.getStyleClass().add("kubata-users-form-avatar-text");
        avatar.getChildren().add(avatarText);

        final byte[][] avatarBytes = {
                isNew ? null : formUser.getAvatar()
        };
        applyAvatar(avatar, avatarText, avatarBytes[0]);

        VBox identityInfo = new VBox(3);
        Label identityName = new Label(
                safe(isNew ? "" : formUser.getNome(), "Novo utilizador")
        );
        identityName.getStyleClass().add("kubata-users-form-identity-name");

        Label identityMeta = new Label(
                isNew
                        ? "Preencha os dados para criar uma nova conta."
                        : "Ficha de utilizador · " + safe(formUser.getEmail(), "sem email")
        );
        identityMeta.getStyleClass().add("kubata-users-form-identity-meta");

        Label identityStatus = new Label(
                isNew
                        ? "NOVA CONTA"
                        : userStatus(formUser).toUpperCase()
        );
        identityStatus.getStyleClass().add("kubata-users-form-identity-status");

        identityInfo.getChildren().addAll(identityName, identityMeta, identityStatus);

        VBox avatarActions = new VBox(5);
        Button chooseAvatar = new Button(
                "Alterar foto",
                IconUtils.icon(Feather.CAMERA, 12)
        );
        chooseAvatar.getStyleClass().add("button-outlined");

        Button removeAvatar = new Button(
                "Remover foto",
                IconUtils.icon(Feather.X, 12)
        );
        removeAvatar.getStyleClass().add("button-outlined");
        removeAvatar.setDisable(avatarBytes[0] == null || avatarBytes[0].length == 0);

        chooseAvatar.setOnAction(e -> {
            if (tabs.getScene() == null || tabs.getScene().getWindow() == null) {
                return;
            }

            FileChooser chooser = new FileChooser();
            chooser.setTitle("Seleccionar fotografia do utilizador");
            chooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter(
                            "Imagens",
                            "*.png", "*.jpg", "*.jpeg", "*.webp"
                    )
            );

            var file = chooser.showOpenDialog(tabs.getScene().getWindow());
            if (file == null) {
                return;
            }

            try {
                avatarBytes[0] = Files.readAllBytes(file.toPath());
                applyAvatar(avatar, avatarText, avatarBytes[0]);
                removeAvatar.setDisable(false);
            } catch (Exception ex) {
                modalManager.showErrorModal(
                        "Fotografia",
                        "Não foi possível carregar a fotografia seleccionada.",
                        ex
                );
            }
        });

        removeAvatar.setOnAction(e -> {
            avatarBytes[0] = null;
            applyAvatar(avatar, avatarText, null);
            removeAvatar.setDisable(true);
        });

        avatarActions.getChildren().addAll(chooseAvatar, removeAvatar);

        identityCard.getChildren().addAll(
                avatar,
                identityInfo,
                avatarActions
        );

        generalPage.getChildren().add(identityCard);

        VBox identitySection = sectionCard(
                "Dados pessoais",
                "Informação principal utilizada para identificação e contacto."
        );

        GridPane personalGrid = formGrid();

        TextField txtNome = field(
                "Nome completo",
                isNew ? "" : formUser.getNome(),
                "Nome e apelido"
        );
        TextField txtEmail = field(
                "Email",
                isNew ? "" : formUser.getEmail(),
                "Email corporativo"
        );
        TextField txtNif = field(
                "NIF",
                isNew ? "" : safe(formUser.getNif(), ""),
                "Número de identificação fiscal"
        );
        TextField txtTelefone = field(
                "Telefone",
                isNew ? "" : safe(formUser.getTelefone(), ""),
                "Contacto telefónico"
        );

        addFormPair(personalGrid, 0, "Nome:*", txtNome, "Email:*", txtEmail);
        addFormPair(personalGrid, 1, "NIF:", txtNif, "Telefone:", txtTelefone);

        identitySection.getChildren().add(personalGrid);

        VBox organizationSection = sectionCard(
                "Organização e função",
                "Associe o utilizador à empresa e defina o contexto funcional."
        );

        GridPane organizationGrid = formGrid();

        ComboBox<Empresa> cbEmpresa = new ComboBox<>();
        cbEmpresa.getItems().addAll(empresaRepository.findAll());
        cbEmpresa.setValue(isNew ? null : formUser.getEmpresa());
        cbEmpresa.setPromptText("Seleccionar empresa");
        cbEmpresa.setMaxWidth(Double.MAX_VALUE);
        cbEmpresa.setConverter(new StringConverter<>() {
            @Override
            public String toString(Empresa object) {
                return object == null ? "" : companyName(object);
            }

            @Override
            public Empresa fromString(String string) {
                return null;
            }
        });

        ComboBox<Role> cmbRole = new ComboBox<>(
                FXCollections.observableArrayList(Role.values())
        );
        cmbRole.setValue(isNew ? Role.USER : formUser.getRole());
        cmbRole.setMaxWidth(Double.MAX_VALUE);
        cmbRole.setConverter(new StringConverter<>() {
            @Override
            public String toString(Role object) {
                return object == null ? "" : roleLabel(object);
            }

            @Override
            public Role fromString(String string) {
                return null;
            }
        });

        TextField txtDepartamento = field(
                "Departamento",
                isNew ? "" : safe(formUser.getDepartamento(), ""),
                "Ex.: Financeiro"
        );
        TextField txtCargo = field(
                "Cargo",
                isNew ? "" : safe(formUser.getCargo(), ""),
                "Ex.: Operador de facturação"
        );

        addFormPair(organizationGrid, 0, "Empresa:*", cbEmpresa, "Função:", cmbRole);
        addFormPair(organizationGrid, 1, "Departamento:", txtDepartamento, "Cargo:", txtCargo);

        organizationSection.getChildren().add(organizationGrid);
        generalPage.getChildren().addAll(identitySection, organizationSection);

        Runnable refreshIdentityPreview = () -> {
            String name = txtNome.getText().trim();
            identityName.setText(name.isBlank() ? "Novo utilizador" : name);
            identityMeta.setText(
                    txtEmail.getText().isBlank()
                            ? "Preencha os dados para criar a conta."
                            : txtEmail.getText().trim()
            );
            if (avatarBytes[0] == null || avatarBytes[0].length == 0) {
                avatarText.setText(initials(name.isBlank() ? "Utilizador" : name));
            }
            identityStatus.setText(isNew
                    ? "NOVA CONTA"
                    : userStatus(formUser).toUpperCase());
        };

        txtNome.textProperty().addListener((obs, old, value) -> refreshIdentityPreview.run());
        txtEmail.textProperty().addListener((obs, old, value) -> refreshIdentityPreview.run());

        Tab tabGeral = new Tab("Geral", generalPage);
        tabGeral.setGraphic(IconUtils.icon(Feather.USER, 13));

        // ================================================================
        // SEGURANÇA — credenciais + controlos
        // ================================================================
        VBox securityPage = new VBox(14);
        securityPage.setPadding(new Insets(16));
        securityPage.getStyleClass().add("kubata-users-form-page");

        VBox passwordCard = sectionCard(
                "Credenciais",
                isNew
                        ? "Defina a credencial inicial desta conta."
                        : "Actualize a credencial apenas quando necessário."
        );

        GridPane passwordGrid = formGrid();

        PasswordField txtSenha = new PasswordField();
        txtSenha.setPromptText(
                isNew ? "Senha inicial" : "Nova senha (opcional)"
        );
        txtSenha.setMaxWidth(Double.MAX_VALUE);

        PasswordField txtConfirmarSenha = new PasswordField();
        txtConfirmarSenha.setPromptText(
                isNew ? "Confirmar senha" : "Confirmar nova senha"
        );
        txtConfirmarSenha.setMaxWidth(Double.MAX_VALUE);

        if (isNew) {
            txtSenha.setText(generateRandomPassword());
            txtConfirmarSenha.setText(txtSenha.getText());
        }

        ProgressBar passwordStrengthBar = new ProgressBar();
        passwordStrengthBar.setProgress(0);
        passwordStrengthBar.setMaxWidth(Double.MAX_VALUE);
        passwordStrengthBar.getStyleClass().add("kubata-users-password-strength");

        Label passwordStrengthLabel = new Label("Força da senha");
        passwordStrengthLabel.getStyleClass().add("kubata-users-password-strength-label");

        Button generatePassword = new Button(
                "Gerar senha segura",
                IconUtils.icon(Feather.REFRESH_CW, 12)
        );
        generatePassword.getStyleClass().add("button-outlined");
        generatePassword.setOnAction(e -> {
            String password = generateRandomPassword();
            txtSenha.setText(password);
            txtConfirmarSenha.setText(password);
        });

        Button copyPassword = new Button(
                "Copiar",
                IconUtils.icon(Feather.COPY, 12)
        );
        copyPassword.getStyleClass().add("button-outlined");
        copyPassword.setDisable(txtSenha.getText().isBlank());
        copyPassword.setOnAction(e -> {
            javafx.scene.input.ClipboardContent clipboard =
                    new javafx.scene.input.ClipboardContent();
            clipboard.putString(txtSenha.getText());
            javafx.scene.input.Clipboard.getSystemClipboard().setContent(clipboard);
        });

        txtSenha.textProperty().addListener((obs, old, value) -> {
            updatePasswordStrength(value, passwordStrengthBar, passwordStrengthLabel);
            copyPassword.setDisable(value == null || value.isBlank());
        });
        updatePasswordStrength(
                txtSenha.getText(),
                passwordStrengthBar,
                passwordStrengthLabel
        );

        VBox passwordControls = new VBox(6);
        HBox passwordButtons = new HBox(7, generatePassword, copyPassword);
        passwordButtons.setAlignment(Pos.CENTER_LEFT);
        passwordControls.getChildren().addAll(
                passwordStrengthLabel,
                passwordStrengthBar,
                passwordButtons
        );

        addFormPair(
                passwordGrid,
                0,
                "Senha" + (isNew ? ":*" : ":"),
                txtSenha,
                "Confirmar:",
                txtConfirmarSenha
        );

        passwordCard.getChildren().add(passwordGrid);
        passwordCard.getChildren().add(passwordControls);

        VBox accountCard = sectionCard(
                "Políticas da conta",
                "Controlos de acesso e ciclo de vida das credenciais."
        );

        DatePicker dataExpiracao = new DatePicker();
        dataExpiracao.setMaxWidth(Double.MAX_VALUE);
        dataExpiracao.setValue(
                isNew
                        ? LocalDate.now().plusDays(90)
                        : formUser.getDataExpiracaoPassword()
        );

        CheckBox chkProvisoria = new CheckBox(
                "Exigir troca de senha no próximo acesso"
        );
        chkProvisoria.setSelected(
                isNew || formUser.isPasswordProvisoria()
        );

        CheckBox chkMfa = new CheckBox(
                "Autenticação multifactor (MFA)"
        );
        chkMfa.setSelected(
                !isNew && formUser.isMfaEnabled()
        );

        CheckBox chkAtivo = new CheckBox("Conta activa");
        chkAtivo.setSelected(
                isNew || Boolean.TRUE.equals(formUser.getActive())
        );
        chkAtivo.setDisable(isSelf);

        CheckBox chkSuperadmin = new CheckBox("Superadministrador");
        chkSuperadmin.setSelected(
                !isNew && formUser.isSuperadmin()
        );

        boolean canManageSuperadmin =
                sessionManager.getUser() != null
                        && sessionManager.getUser().isSuperadmin();
        chkSuperadmin.setDisable(!canManageSuperadmin);

        GridPane accountGrid = formGrid();
        Label expirationLabel = new Label("Expiração:");
        expirationLabel.getStyleClass().add("kubata-users-form-label");
        accountGrid.add(expirationLabel, 0, 0);
        accountGrid.add(dataExpiracao, 1, 0);
        GridPane.setHgrow(dataExpiracao, Priority.ALWAYS);

        HBox policyLine = new HBox(18, chkProvisoria, chkMfa);
        policyLine.setAlignment(Pos.CENTER_LEFT);

        HBox stateLine = new HBox(18, chkAtivo, chkSuperadmin);
        stateLine.setAlignment(Pos.CENTER_LEFT);

        accountCard.getChildren().addAll(
                accountGrid,
                policyLine,
                stateLine,
                hint(
                        "MFA protege a conta com um segundo factor. "
                                + "Senhas provisórias podem ser expiradas conforme a política definida."
                )
        );

        securityPage.getChildren().addAll(passwordCard, accountCard);

        Tab tabSeguranca = new Tab("Segurança", securityPage);
        tabSeguranca.setGraphic(IconUtils.icon(Feather.SHIELD, 13));

        // ================================================================
        // PERFIS — atribuição produtiva
        // ================================================================
        VBox profilesPage = new VBox(12);
        profilesPage.setPadding(new Insets(16));
        profilesPage.getStyleClass().add("kubata-users-form-page");

        VBox profileHeader = new VBox(4);
        Label profileTitle = new Label("Perfis de acesso");
        profileTitle.getStyleClass().add("kubata-users-section-title");

        Label profileHint = new Label(
                "Os perfis determinam as permissões funcionais disponíveis para este utilizador."
        );
        profileHint.setWrapText(true);
        profileHint.getStyleClass().add("kubata-users-form-hint");

        profileHeader.getChildren().addAll(profileTitle, profileHint);

        HBox profileTools = new HBox(8);
        profileTools.setAlignment(Pos.CENTER_LEFT);

        TextField profileSearch = new TextField();
        profileSearch.setPromptText("Pesquisar perfil...");
        profileSearch.setPrefWidth(260);

        Label selectedProfiles = new Label("0 seleccionados");
        selectedProfiles.getStyleClass().add("kubata-users-profile-count");

        Button selectAllProfiles = new Button(
                "Seleccionar todos",
                IconUtils.icon(Feather.CHECK_SQUARE, 12)
        );
        selectAllProfiles.getStyleClass().add("button-outlined");

        Button clearProfiles = new Button(
                "Limpar",
                IconUtils.icon(Feather.X, 12)
        );
        clearProfiles.getStyleClass().add("button-outlined");

        profileTools.getChildren().addAll(
                profileSearch,
                selectAllProfiles,
                clearProfiles,
                new Pane(),
                selectedProfiles
        );
        HBox.setHgrow(profileTools.getChildren().get(3), Priority.ALWAYS);

        ListView<PerfilAcesso> listPerfis = new ListView<>();
        listPerfis.getSelectionModel().setSelectionMode(
                SelectionMode.MULTIPLE
        );
        listPerfis.setPrefHeight(330);
        listPerfis.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(PerfilAcesso item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                VBox box = new VBox(2);
                Label code = new Label(
                        safe(item.getCodigo(), "PERFIL")
                );
                code.getStyleClass().add("kubata-users-profile-code");

                Label description = new Label(
                        safe(item.getDescricao(), "Sem descrição")
                );
                description.getStyleClass().add("kubata-users-profile-description");

                box.getChildren().addAll(code, description);
                setText(null);
                setGraphic(box);
            }
        });

        final List<PerfilAcesso> availableProfiles = new ArrayList<>();

        Runnable refreshProfiles = () -> {
            availableProfiles.clear();
            availableProfiles.addAll(
                    perfilRepository.findByEmpresaIsNull()
            );

            if (cbEmpresa.getValue() != null) {
                availableProfiles.addAll(
                        perfilRepository.findByEmpresa(cbEmpresa.getValue())
                );
            }

            String query = profileSearch.getText() == null
                    ? ""
                    : profileSearch.getText().trim().toLowerCase();

            List<PerfilAcesso> visible = availableProfiles.stream()
                    .filter(p -> query.isBlank()
                            || contains(p.getCodigo(), query)
                            || contains(p.getDescricao(), query))
                    .collect(Collectors.toList());

            listPerfis.setItems(
                    FXCollections.observableArrayList(visible)
            );

            if (formUser != null && formUser.getPerfis() != null) {
                for (PerfilAcesso p : formUser.getPerfis()) {
                    if (visible.contains(p)) {
                        listPerfis.getSelectionModel().select(p);
                    }
                }
            }

            selectedProfiles.setText(
                    listPerfis.getSelectionModel().getSelectedItems().size()
                            + " seleccionados"
            );
        };

        profileSearch.textProperty().addListener(
                (obs, old, value) -> refreshProfiles.run()
        );
        cbEmpresa.valueProperty().addListener(
                (obs, old, value) -> refreshProfiles.run()
        );

        listPerfis.getSelectionModel().getSelectedItems().addListener(
                (javafx.collections.ListChangeListener<PerfilAcesso>) change ->
                        selectedProfiles.setText(
                                listPerfis.getSelectionModel()
                                        .getSelectedItems().size()
                                        + " seleccionados"
                        )
        );

        selectAllProfiles.setOnAction(e ->
                listPerfis.getSelectionModel().selectAll()
        );

        clearProfiles.setOnAction(e ->
                listPerfis.getSelectionModel().clearSelection()
        );

        refreshProfiles.run();

        VBox profileCard = new VBox(10);
        profileCard.getStyleClass().add("kubata-users-profile-card");
        profileCard.getChildren().addAll(
                profileTools,
                listPerfis,
                hint(
                        "Os perfis globais ficam disponíveis para todas as empresas. "
                                + "Os perfis da empresa são apresentados quando uma empresa é seleccionada."
                )
        );
        VBox.setVgrow(listPerfis, Priority.ALWAYS);

        profilesPage.getChildren().addAll(profileHeader, profileCard);

        Tab tabPerfis = new Tab(
                "Perfis e permissões",
                profilesPage
        );
        tabPerfis.setGraphic(IconUtils.icon(Feather.KEY, 13));

        // ================================================================
        // PREFERÊNCIAS — produtividade
        // ================================================================
        VBox preferencesPage = new VBox(14);
        preferencesPage.setPadding(new Insets(16));
        preferencesPage.getStyleClass().add("kubata-users-form-page");

        VBox appearanceCard = sectionCard(
                "Experiência de utilização",
                "Personalize idioma, tema e densidade de informação."
        );

        ComboBox<String> cmbIdioma = new ComboBox<>(
                FXCollections.observableArrayList(
                        "pt-AO", "pt-PT", "pt-BR", "en"
                )
        );
        cmbIdioma.setValue(
                isNew ? "pt-AO" : safe(formUser.getIdioma(), "pt-AO")
        );
        cmbIdioma.setMaxWidth(Double.MAX_VALUE);

        ComboBox<String> cmbTema = new ComboBox<>(
                FXCollections.observableArrayList(
                        "VERDE_ADMIN", "CLARO", "ESCURO"
                )
        );
        cmbTema.setValue(
                isNew ? "VERDE_ADMIN" : safe(formUser.getTema(), "VERDE_ADMIN")
        );
        cmbTema.setMaxWidth(Double.MAX_VALUE);

        int currentRows = isNew
                ? 50
                : Math.max(10, Math.min(500, formUser.getLinhasPorPagina()));
        Spinner<Integer> linhas = new Spinner<>(
                10, 500, currentRows, 10
        );
        linhas.setMaxWidth(Double.MAX_VALUE);

        GridPane preferencesGrid = formGrid();
        addFormPair(
                preferencesGrid,
                0,
                "Idioma:",
                cmbIdioma,
                "Tema:",
                cmbTema
        );
        preferencesGrid.add(
                new Label("Linhas por página:"),
                0,
                1
        );
        preferencesGrid.add(linhas, 1, 1);
        GridPane.setHgrow(linhas, Priority.ALWAYS);

        appearanceCard.getChildren().addAll(
                preferencesGrid,
                hint(
                        "As preferências são guardadas no perfil e aplicadas "
                                + "à experiência do utilizador."
                )
        );

        VBox preferencesInfo = sectionCard(
                "Atalhos e produtividade",
                "Configurações práticas para utilização diária do Administrator."
        );
        preferencesInfo.getChildren().addAll(
                shortcutRow("Ctrl + F", "Focar rapidamente a pesquisa"),
                shortcutRow("Duplo clique", "Abrir os detalhes do registo"),
                shortcutRow("Botão direito", "Operações e exportação da grelha")
        );

        preferencesPage.getChildren().addAll(
                appearanceCard,
                preferencesInfo
        );

        Tab tabPreferencias = new Tab(
                "Preferências",
                preferencesPage
        );
        tabPreferencias.setGraphic(
                IconUtils.icon(Feather.SETTINGS, 13)
        );

        tabs.getTabs().addAll(
                tabGeral,
                tabSeguranca,
                tabPerfis,
                tabPreferencias
        );

        ModalManager.ModalConfig config = new ModalManager.ModalConfig()
                .size(980, 720)
                .minSize(800, 600)
                .maxSize(1300, 900)
                .maximizable(true)
                .minimizable(true)
                .windowControls(true)
                .scrollable(false);

        modalManager.showConfirmModal(
                tabs,
                isNew
                        ? "Novo utilizador"
                        : "Editar utilizador — " + safe(formUser.getNome(), ""),
                () -> {
                    if (!can(isNew ? "CRIAR" : "EDITAR")) {
                        modalManager.alert(
                                "Acesso negado",
                                "Não possui permissão para esta operação.",
                                "warning",
                                null
                        );
                        return;
                    }

                    if (txtNome.getText().isBlank()
                            || txtEmail.getText().isBlank()
                            || cbEmpresa.getValue() == null) {
                        modalManager.alert(
                                "Dados incompletos",
                                "Nome, email e empresa são obrigatórios.",
                                "warning",
                                null
                        );
                        return;
                    }

                    if (!txtEmail.getText().contains("@")) {
                        modalManager.alert(
                                "Email inválido",
                                "Introduza um endereço de email válido.",
                                "warning",
                                null
                        );
                        return;
                    }

                    String password = txtSenha.getText();
                    if (isNew && password.isBlank()) {
                        modalManager.alert(
                                "Senha obrigatória",
                                "Defina uma senha inicial para o novo utilizador.",
                                "warning",
                                null
                        );
                        return;
                    }

                    if (!password.isBlank()
                            && !password.equals(txtConfirmarSenha.getText())) {
                        modalManager.alert(
                                "Confirmação inválida",
                                "A confirmação da senha não coincide.",
                                "warning",
                                null
                        );
                        return;
                    }

                    if (!password.isBlank()
                            && password.length() < 8) {
                        modalManager.alert(
                                "Senha fraca",
                                "A senha deve ter pelo menos 8 caracteres.",
                                "warning",
                                null
                        );
                        return;
                    }

                    if (isSelf && !chkAtivo.isSelected()) {
                        modalManager.alert(
                                "Operação não permitida",
                                "A sua própria conta não pode ser desactivada nesta área.",
                                "warning",
                                null
                        );
                        return;
                    }

                    User target = isNew ? new User() : formUser;

                    target.setNome(txtNome.getText().trim());
                    target.setEmail(txtEmail.getText().trim());
                    target.setEmpresa(cbEmpresa.getValue());
                    target.setRole(cmbRole.getValue());
                    target.setNif(blankToNull(txtNif.getText()));
                    target.setTelefone(blankToNull(txtTelefone.getText()));
                    target.setDepartamento(blankToNull(txtDepartamento.getText()));
                    target.setCargo(blankToNull(txtCargo.getText()));
                    target.setAvatar(avatarBytes[0]);
                    target.setActive(chkAtivo.isSelected());
                    target.setMfaEnabled(chkMfa.isSelected());
                    target.setSuperadmin(chkSuperadmin.isSelected());
                    target.setPasswordProvisoria(chkProvisoria.isSelected());
                    target.setDataExpiracaoPassword(
                            dataExpiracao.getValue()
                    );
                    target.setIdioma(cmbIdioma.getValue());
                    target.setTema(cmbTema.getValue());
                    target.setLinhasPorPagina(linhas.getValue());

                    Set<PerfilAcesso> selectedPerfis =
                            new HashSet<>(
                                    listPerfis.getSelectionModel()
                                            .getSelectedItems()
                            );
                    target.setPerfis(selectedPerfis);

                    if (!password.isBlank()) {
                        target.setPassword(
                                passwordEncoder.encode(password)
                        );
                        target.setPasswordChangedAt(LocalDateTime.now());
                    }

                    if (isNew) {
                        target.setPasswordProvisoria(true);
                        target.setFailedAttempts(0);
                        target.setLockoutEnd(null);
                    }

                    persistenceService.saveAsync(
                            userRepository,
                            target,
                            "UTILIZADOR",
                            (isNew ? "Criado" : "Actualizado")
                                    + " utilizador: " + target.getEmail(),
                            saved -> loadUsers()
                    );
                },
                null,
                config
        );
    }

    private GridPane formGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(9);

        ColumnConstraints labelLeft = new ColumnConstraints(92);
        ColumnConstraints fieldLeft = new ColumnConstraints();
        fieldLeft.setHgrow(Priority.ALWAYS);
        fieldLeft.setFillWidth(true);

        ColumnConstraints labelRight = new ColumnConstraints(92);
        ColumnConstraints fieldRight = new ColumnConstraints();
        fieldRight.setHgrow(Priority.ALWAYS);
        fieldRight.setFillWidth(true);

        grid.getColumnConstraints().addAll(
                labelLeft, fieldLeft, labelRight, fieldRight
        );
        return grid;
    }

    private void addFormPair(
            GridPane grid,
            int row,
            String leftLabel,
            Control leftControl,
            String rightLabel,
            Control rightControl
    ) {
        Label leftTitle = new Label(leftLabel);
        leftTitle.getStyleClass().add("kubata-users-form-label");
        grid.add(leftTitle, 0, row);
        grid.add(leftControl, 1, row);
        GridPane.setHgrow(leftControl, Priority.ALWAYS);

        if (rightLabel != null && !rightLabel.isBlank()) {
            Label rightTitle = new Label(rightLabel);
            rightTitle.getStyleClass().add("kubata-users-form-label");
            grid.add(rightTitle, 2, row);
            grid.add(rightControl, 3, row);
            GridPane.setHgrow(rightControl, Priority.ALWAYS);
        }
    }

    private VBox sectionCard(String title, String description) {
        VBox card = new VBox(9);
        card.getStyleClass().add("kubata-users-form-card");

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("kubata-users-form-card-title");

        Label descriptionLabel = new Label(description);
        descriptionLabel.setWrapText(true);
        descriptionLabel.getStyleClass().add("kubata-users-form-hint");

        card.getChildren().addAll(titleLabel, descriptionLabel);
        return card;
    }

    private HBox shortcutRow(String shortcut, String description) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);

        Label key = new Label(shortcut);
        key.getStyleClass().add("kubata-users-shortcut-key");

        Label text = new Label(description);
        text.getStyleClass().add("kubata-users-form-hint");

        row.getChildren().addAll(key, text);
        return row;
    }

    private void applyAvatar(StackPane avatar, Label fallback, byte[] bytes) {
        avatar.getChildren().clear();

        if (bytes != null && bytes.length > 0) {
            try (ByteArrayInputStream input = new ByteArrayInputStream(bytes)) {
                Image image = new Image(input, 52, 52, true, true);
                if (!image.isError()) {
                    ImageView imageView = new ImageView(image);
                    imageView.setFitWidth(52);
                    imageView.setFitHeight(52);
                    imageView.setPreserveRatio(true);
                    imageView.setSmooth(true);
                    avatar.getChildren().add(imageView);
                    return;
                }
            } catch (Exception ignored) {
                // Fallback para as iniciais.
            }
        }

        avatar.getChildren().add(fallback);
    }

    private String initials(String value) {
        String text = safe(value, "U").trim();
        if (text.isBlank()) {
            return "U";
        }

        String[] parts = text.split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, 1).toUpperCase();
        }

        return (
                parts[0].substring(0, 1)
                        + parts[parts.length - 1].substring(0, 1)
        ).toUpperCase();
    }

    private void updatePasswordStrength(
            String password,
            ProgressBar bar,
            Label label
    ) {
        String value = password == null ? "" : password;

        int score = 0;
        if (value.length() >= 8) score++;
        if (value.length() >= 12) score++;
        if (value.matches(".*[A-Z].*")) score++;
        if (value.matches(".*[a-z].*")) score++;
        if (value.matches(".*\\d.*")) score++;
        if (value.matches(".*[^A-Za-z0-9].*")) score++;

        double progress = Math.min(1.0, score / 6.0);
        bar.setProgress(progress);

        bar.getStyleClass().removeAll(
                "password-weak",
                "password-medium",
                "password-strong"
        );
        label.getStyleClass().removeAll(
                "password-weak",
                "password-medium",
                "password-strong"
        );

        if (value.isBlank()) {
            label.setText("Força da senha");
            return;
        }

        if (score <= 2) {
            label.setText("Senha fraca");
            bar.getStyleClass().add("password-weak");
            label.getStyleClass().add("password-weak");
        } else if (score <= 4) {
            label.setText("Senha média");
            bar.getStyleClass().add("password-medium");
            label.getStyleClass().add("password-medium");
        } else {
            label.setText("Senha forte");
            bar.getStyleClass().add("password-strong");
            label.getStyleClass().add("password-strong");
        }
    }

    private TextField field(String label, String value, String prompt) {
        TextField field = new TextField(value);
        field.setPromptText(prompt);
        field.setMaxWidth(Double.MAX_VALUE);
        return field;
    }

    private void addFormRow(GridPane grid, int row, String label, Control control) {
        Label title = new Label(label);
        title.getStyleClass().add("kubata-users-form-label");
        grid.add(title, 0, row);
        grid.add(control, 1, row);
        GridPane.setHgrow(control, Priority.ALWAYS);
    }

    private VBox labelled(String label, Control control) {
        Label title = new Label(label);
        title.getStyleClass().add("kubata-users-form-label");

        VBox box = new VBox(5, title, control);
        box.setMaxWidth(Double.MAX_VALUE);
        return box;
    }

    private Label hint(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.getStyleClass().add("kubata-users-form-hint");
        return label;
    }

    private void confirmDelete(User selected) {
        if (selected == null) {
            return;
        }
        User current = sessionManager.getUser();

        if (current != null && current.getId() != null && current.getId().equals(selected.getId())) {
            modalManager.alert(
                    "Operação não permitida",
                    "Não pode remover a própria conta.",
                    "warning",
                    null
            );
            return;
        }

        if (!can("REMOVER")) {
            modalManager.alert("Acesso negado", "Não possui permissão para remover utilizadores.", "warning", null);
            return;
        }

        modalManager.showConfirmModal(
                confirmationContent(
                        "Remover utilizador",
                        "A conta seleccionada será removida do sistema."
                ),
                "Remover utilizador — " + safe(selected.getNome(), selected.getEmail()),
                () -> persistenceService.deleteAsync(
                        userRepository,
                        selected,
                        null,
                        "UTILIZADOR",
                        "Removido utilizador: " + selected.getEmail(),
                        this::loadUsers
                ),
                null
        );
    }

    private VBox confirmationContent(String title, String message) {
        VBox box = new VBox(8);
        box.setPadding(new Insets(4));
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("kubata-users-section-title");
        Label messageLabel = new Label(message);
        messageLabel.setWrapText(true);
        messageLabel.getStyleClass().add("kubata-users-form-hint");
        box.getChildren().addAll(titleLabel, messageLabel);
        return box;
    }

    private void cloneUser() {
        User selected = selectedUser().orElse(null);
        if (selected == null) {
            return;
        }

        if (!can("CRIAR")) {
            modalManager.alert("Acesso negado", "Não possui permissão para criar utilizadores.", "warning", null);
            return;
        }

        User clone = new User();
        clone.setNome(safe(selected.getNome(), "Utilizador") + " (Cópia)");
        clone.setEmail("copy_" + System.currentTimeMillis() + "_" + safe(selected.getEmail(), "utilizador@kubata.local"));
        clone.setRole(selected.getRole());
        clone.setEmpresa(selected.getEmpresa());
        clone.setNif(selected.getNif());
        clone.setTelefone(selected.getTelefone());
        clone.setDepartamento(selected.getDepartamento());
        clone.setCargo(selected.getCargo());
        clone.setActive(true);
        clone.setMfaEnabled(false);
        clone.setPassword(selected.getPassword());
        clone.setPasswordProvisoria(true);
        clone.setDataExpiracaoPassword(LocalDate.now().plusDays(90));
        clone.setPerfis(selected.getPerfis() == null
                ? new HashSet<>()
                : new HashSet<>(selected.getPerfis()));

        persistenceService.saveAsync(
                userRepository,
                clone,
                "UTILIZADOR",
                "Clonado utilizador: " + selected.getEmail()
                        + " para " + clone.getEmail(),
                saved -> {
                    loadUsers();
                    Platform.runLater(() -> showUserDialog(saved));
                }
        );
    }

    private void toggleSelectedStatus() {
        User selected = selectedUser().orElse(null);
        if (selected == null || !can("EDITAR")) {
            return;
        }

        User current = sessionManager.getUser();
        if (current != null && current.getId() != null && current.getId().equals(selected.getId())) {
            modalManager.alert(
                    "Operação não permitida",
                    "Não pode desactivar a própria conta.",
                    "warning",
                    null
            );
            return;
        }

        boolean next = !Boolean.TRUE.equals(selected.getActive());

        modalManager.showConfirm(
                next ? "Activar utilizador" : "Desactivar utilizador",
                "Confirma a alteração do estado de " + safe(selected.getNome(), selected.getEmail()) + "?",
                () -> {
                    selected.setActive(next);
                    persistenceService.saveAsync(
                            userRepository,
                            selected,
                            "UTILIZADOR",
                            (next ? "Activado" : "Desactivado")
                                    + " utilizador: " + selected.getEmail(),
                            saved -> loadUsers()
                    );
                }
        );
    }

    private void unlockSelectedUser() {
        User selected = selectedUser().orElse(null);
        if (selected == null || !can("EDITAR") || !isBlocked(selected)) {
            return;
        }

        modalManager.showConfirm(
                "Desbloquear utilizador",
                "Os bloqueios e tentativas falhadas de " + safe(selected.getNome(), selected.getEmail())
                        + " serão limpos.",
                () -> {
                    selected.setFailedAttempts(0);
                    selected.setLockoutEnd(null);
                    persistenceService.saveAsync(
                            userRepository,
                            selected,
                            "UTILIZADOR",
                            "Desbloqueado utilizador: " + selected.getEmail(),
                            saved -> loadUsers()
                    );
                }
        );
    }

    private void resetPassword() {
        User selected = selectedUser().orElse(null);
        User current = sessionManager.getUser();

        if (selected == null || !can("EDITAR")) {
            return;
        }

        if (current != null
                && current.getId() != null
                && selected.getId() != null
                && current.getId().equals(selected.getId())) {
            modalManager.alert(
                    "Operação não permitida",
                    "A redefinição administrativa não pode ser usada na própria conta. "
                            + "Para a sua conta, altere a palavra-passe no seu perfil.",
                    "warning",
                    null
            );
            return;
        }

        VBox box = new VBox(12);
        box.setPadding(new Insets(4));

        Label warning = new Label(
                "Será criada uma palavra-passe temporária e a credencial actual será invalidada. "
                        + "Todas as sessões registadas deste utilizador serão terminadas."
        );
        warning.setWrapText(true);
        warning.getStyleClass().add("kubata-users-form-hint");

        Label target = new Label(
                "Utilizador: " + safe(selected.getNome(), selected.getEmail())
                        + "\nEmail: " + safe(selected.getEmail(), "-")
        );
        target.setWrapText(true);
        target.getStyleClass().add("kubata-users-section-title");

        TextArea reason = new TextArea();
        reason.setPromptText("Motivo obrigatório da redefinição (mínimo 5 caracteres)");
        reason.setWrapText(true);
        reason.setPrefRowCount(4);
        reason.setMaxWidth(Double.MAX_VALUE);

        Label privacy = new Label(
                "A palavra-passe temporária não será gravada na auditoria nem nos logs."
        );
        privacy.setWrapText(true);
        privacy.getStyleClass().add("kubata-users-form-hint");

        box.getChildren().addAll(target, warning, reason, privacy);

        modalManager.showConfirmModal(
                box,
                "Redefinir palavra-passe — "
                        + safe(selected.getNome(), selected.getEmail()),
                () -> {
                    String normalizedReason = reason.getText() == null
                            ? ""
                            : reason.getText().trim();

                    if (normalizedReason.length() < 5) {
                        modalManager.alert(
                                "Motivo obrigatório",
                                "Indique um motivo com pelo menos 5 caracteres.",
                                "warning",
                                null
                        );
                        return;
                    }

                    javafx.concurrent.Task<PasswordResetService.ResetResult> task =
                            new javafx.concurrent.Task<>() {
                                @Override
                                protected PasswordResetService.ResetResult call() {
                                    return passwordResetService.resetByAdministrator(
                                            current,
                                            selected.getId(),
                                            "127.0.0.1",
                                            normalizedReason
                                    );
                                }
                            };

                    task.setOnSucceeded(event -> {
                        PasswordResetService.ResetResult result = task.getValue();

                        VBox resultBox = new VBox(12);
                        resultBox.setPadding(new Insets(4));

                        Label success = new Label(
                                "A palavra-passe foi redefinida com sucesso. "
                                        + "Entregue a credencial temporária ao utilizador através de um canal seguro."
                        );
                        success.setWrapText(true);

                        TextField temporary = new TextField(result.temporaryPassword());
                        temporary.setEditable(false);
                        temporary.setMaxWidth(Double.MAX_VALUE);

                        Button copy = new Button(
                                "Copiar palavra-passe",
                                IconUtils.icon(Feather.COPY, 12)
                        );
                        copy.getStyleClass().add("button-outlined");
                        copy.setOnAction(e -> {
                            javafx.scene.input.ClipboardContent clipboard =
                                    new javafx.scene.input.ClipboardContent();
                            clipboard.putString(result.temporaryPassword());
                            javafx.scene.input.Clipboard.getSystemClipboard().setContent(clipboard);
                        });

                        VBox meta = new VBox(4,
                                new Label(
                                        "Expira em: "
                                                + result.expiresOn().format(DATE_FORMAT)
                                ),
                                new Label(
                                        "Sessões terminadas: "
                                                + result.revokedSessions()
                                ),
                                new Label(
                                        "A conta ficou marcada para troca da palavra-passe temporária."
                                )
                        );
                        meta.getStyleClass().add("kubata-users-form-hint");

                        resultBox.getChildren().addAll(
                                success,
                                new Label("Palavra-passe temporária"),
                                temporary,
                                copy,
                                meta
                        );

                        modalManager.showModal(
                                resultBox,
                                new ModalManager.ModalConfig()
                                        .title("Palavra-passe redefinida")
                                        .icon(Feather.CHECK_CIRCLE)
                                        .tone(ModalManager.ModalTone.SUCCESS)
                                        .singleButton("Concluir")
                                        .size(540, 390)
                                        .minSize(480, 340)
                                        .maximizable(false)
                                        .minimizable(false)
                                        .closeOnOverlayClick(false)
                        );

                        loadUsers();
                    });

                    task.setOnFailed(event -> {
                        Throwable error = task.getException();

                        modalManager.showErrorModal(
                                "Falha na redefinição",
                                error == null || error.getMessage() == null
                                        ? "Não foi possível redefinir a palavra-passe."
                                        : error.getMessage(),
                                error
                        );
                    });

                    Thread worker = new Thread(task, "kubata-password-reset");
                    worker.setDaemon(true);
                    worker.start();
                },
                null,
                new ModalManager.ModalConfig()
                        .size(560, 420)
                        .minSize(500, 360)
                        .maximizable(false)
                        .minimizable(false)
                        .icon(Feather.KEY)
                        .tone(ModalManager.ModalTone.WARNING)
                        .footerHint("A operação fica registada na auditoria do sistema.")
        );
    }

    private void showDetailsModal(User user) {
        if (user == null) {
            return;
        }

        VBox content = new VBox(14);
        content.setPadding(new Insets(4));

        content.getChildren().addAll(
                detailCard("Identidade", List.of(
                        "Nome: " + safe(user.getNome(), "-"),
                        "Email: " + safe(user.getEmail(), "-"),
                        "NIF: " + safe(user.getNif(), "-"),
                        "Telefone: " + safe(user.getTelefone(), "-")
                )),
                detailCard("Organização", List.of(
                        "Empresa: " + companyName(user.getEmpresa()),
                        "Função: " + roleLabel(user.getRole()),
                        "Departamento: " + safe(user.getDepartamento(), "-"),
                        "Cargo: " + safe(user.getCargo(), "-")
                )),
                detailCard("Segurança", List.of(
                        "Estado: " + userStatus(user),
                        "MFA: " + (user.isMfaEnabled() ? "Activo" : "Inactivo"),
                        "Credencial: " + credentialStatus(user),
                        "Tentativas falhadas: " + user.getFailedAttempts(),
                        "Bloqueio: " + formatDateTime(user.getLockoutEnd())
                )),
                detailCard("Actividade", List.of(
                        "Último acesso: " + formatDateTime(user.getUltimoAcesso()),
                        "Último IP: " + safe(user.getUltimoIpLogin(), "-"),
                        "Criado em: " + formatDateTime(user.getCreatedAt()),
                        "Actualizado em: " + formatDateTime(user.getUpdatedAt())
                ))
        );

        ModalManager.ModalConfig config = new ModalManager.ModalConfig()
                .size(680, 620)
                .minSize(560, 480)
                .maximizable(true)
                .minimizable(true);

        modalManager.showModal(content, config.title("Detalhes do utilizador").icon(Feather.USER));
    }

    private VBox detailCard(String title, List<String> lines) {
        VBox card = new VBox(5);
        card.getStyleClass().add("kubata-users-modal-card");

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("kubata-users-section-title");

        card.getChildren().add(titleLabel);
        for (String line : lines) {
            Label row = new Label(line);
            row.setWrapText(true);
            row.getStyleClass().add("kubata-users-modal-row");
            card.getChildren().add(row);
        }
        return card;
    }

    private void refreshActionPermissions() {
        if (btnNovo != null) {
            btnNovo.setDisable(!can("CRIAR"));
        }

        User current = sessionManager.getUser();
        if (btnEditar != null && table != null) {
            User selected = table.getSelectionModel().getSelectedItem();
            boolean hasSelection = selected != null;
            btnEditar.setDisable(!hasSelection || !can("EDITAR"));
            btnClonar.setDisable(!hasSelection || !can("CRIAR"));
            btnResetPassword.setDisable(!hasSelection || !can("EDITAR"));
            btnDesbloquear.setDisable(!hasSelection || !can("EDITAR") || !isBlocked(selected));
            btnStatus.setDisable(!hasSelection || !can("EDITAR")
                    || (current != null && selected != null
                    && current.getId() != null && current.getId().equals(selected.getId())));
        }
    }

    private boolean can(String operation) {
        User currentUser = sessionManager.getUser();
        if (currentUser == null) {
            return false;
        }

        if (currentUser.isSuperadmin() || currentUser.getRole() == Role.ADMIN) {
            return true;
        }

        String normalized = "REMOVER".equalsIgnoreCase(operation)
                ? "APAGAR"
                : operation;

        try {
            return securityService.hasPermission(
                    currentUser,
                    "ADMINISTRATOR",
                    "UTILIZADORES",
                    PermissaoPerfil.Operacao.valueOf(normalized.toUpperCase())
            );
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private java.util.Optional<User> selectedUser() {
        return table.getSelectionModel().getSelectedItem() == null
                ? java.util.Optional.empty()
                : java.util.Optional.of(table.getSelectionModel().getSelectedItem());
    }

    private boolean isBlocked(User user) {
        return user != null
                && user.getLockoutEnd() != null
                && user.getLockoutEnd().isAfter(LocalDateTime.now());
    }

    private String userStatus(User user) {
        if (user == null) {
            return "-";
        }
        if (isBlocked(user)) {
            return "Bloqueado";
        }
        if ("Senha expirada".equals(credentialStatus(user))) {
            return "Senha expirada";
        }
        return Boolean.TRUE.equals(user.getActive()) ? "Activo" : "Inactivo";
    }

    private String credentialStatus(User user) {
        if (user == null) {
            return "-";
        }
        LocalDate expiration = user.getDataExpiracaoPassword();
        if (expiration != null && expiration.isBefore(LocalDate.now())) {
            return "Senha expirada";
        }
        if (user.isPasswordProvisoria()) {
            return "Senha provisória";
        }
        return "Normal";
    }

    private String roleLabel(Role role) {
        if (role == null) {
            return "-";
        }
        return switch (role) {
            case ADMIN -> "Administrador";
            case USER -> "Utilizador";
            case OPERATOR -> "Operador";
            case DIRETOR -> "Director";
            case GERENTE_FINANCEIRO -> "Gerente Financeiro";
            case CONTABILISTA -> "Contabilista";
            case OPERADOR_FATURACAO -> "Operador de Facturação";
            case CAIXA -> "Caixa";
            case SUPERVISOR_VENDAS -> "Supervisor de Vendas";
            case VENDEDOR -> "Vendedor";
            case ESTOQUE -> "Gestor de Stock";
            case COMPRAS -> "Compras";
            case LOGISTICA -> "Logística";
            case AUDITOR -> "Auditor";
            case SUPORTE_TI -> "Suporte TI";
            case RESPONSAVEL_FISCAL_AO -> "Responsável Fiscal AO";
            case VISITANTE -> "Visitante";
        };
    }

    private String companyName(Empresa empresa) {
        return empresa == null ? "Sem empresa" : safe(empresa.getNome(), "Empresa");
    }

    private boolean sameId(Empresa a, Empresa b) {
        if (a == null || b == null) {
            return a == b;
        }
        return a.getId() != null && a.getId().equals(b.getId());
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase().contains(query);
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? "-" : DATE_TIME_FORMAT.format(value);
    }

    private String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static class SimpleStringPropertySafe extends javafx.beans.property.SimpleStringProperty {
        SimpleStringPropertySafe(String value) {
            super(value == null ? "" : value);
        }
    }

    private String generateRandomPassword() {
        SecureRandom random = new SecureRandom();
        final String upper = "ABCDEFGHJKLMNPQRSTUVWXYZ";
        final String lower = "abcdefghijkmnopqrstuvwxyz";
        final String digits = "23456789";
        final String symbols = "@#$%&*!";

        StringBuilder password = new StringBuilder(12);
        password.append(upper.charAt(random.nextInt(upper.length())));
        password.append(lower.charAt(random.nextInt(lower.length())));
        password.append(digits.charAt(random.nextInt(digits.length())));
        password.append(symbols.charAt(random.nextInt(symbols.length())));

        String alphabet = upper + lower + digits + symbols;
        while (password.length() < 12) {
            password.append(alphabet.charAt(random.nextInt(alphabet.length())));
        }

        char[] chars = password.toString().toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char tmp = chars[i];
            chars[i] = chars[j];
            chars[j] = tmp;
        }
        return new String(chars);
    }
}

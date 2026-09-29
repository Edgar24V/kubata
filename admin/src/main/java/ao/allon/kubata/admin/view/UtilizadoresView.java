package ao.allon.kubata.admin.view;

import ao.allon.kubata.admin.service.PersistenceService;
import ao.allon.kubata.admin.service.SessionManager;
import ao.allon.kubata.admin.ui.modal.ModalManager;
import ao.allon.kubata.admin.ui.util.IconUtils;
import ao.allon.kubata.core.domain.Empresa;
import ao.allon.kubata.core.domain.PerfilAcesso;
import ao.allon.kubata.core.domain.Role;
import ao.allon.kubata.core.domain.User;
import ao.allon.kubata.core.repository.EmpresaRepository;
import ao.allon.kubata.core.repository.PerfilAcessoRepository;
import ao.allon.kubata.core.repository.UserRepository;
import ao.allon.kubata.core.service.AcessoService;
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
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.util.StringConverter;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

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

    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final UserRepository userRepository;
    private final PerfilAcessoRepository perfilRepository;
    private final EmpresaRepository empresaRepository;
    private final AcessoService acessoService;
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

        buildUI();
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        if (!dataLoaded && getScene() != null) {
            dataLoaded = true;
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

        Button btnNovo = new Button(
                "Novo Utilizador",
                IconUtils.icon(Feather.USER_PLUS, IconUtils.SIZE_SMALL)
        );
        btnNovo.getStyleClass().add("button-primary");
        btnNovo.setOnAction(e -> showUserDialog(null));
        btnNovo.setDisable(!can("CRIAR"));

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
        boolean isSelf = !isNew
                && sessionManager.getUser() != null
                && sessionManager.getUser().getId() != null
                && sessionManager.getUser().getId().equals(user.getId());

        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        GridPane general = new GridPane();
        general.setHgap(14);
        general.setVgap(11);
        general.setPadding(new Insets(18));
        general.getStyleClass().add("kubata-users-form");

        ColumnConstraints labelCol = new ColumnConstraints(115);
        ColumnConstraints fieldCol = new ColumnConstraints();
        fieldCol.setHgrow(Priority.ALWAYS);
        fieldCol.setFillWidth(true);
        general.getColumnConstraints().addAll(labelCol, fieldCol);

        TextField txtNome = field("Nome completo", user == null ? "" : user.getNome(), "Nome e apelido");
        TextField txtEmail = field("Email", user == null ? "" : user.getEmail(), "Email corporativo");
        TextField txtNif = field("NIF", user == null ? "" : safe(user.getNif(), ""), "NIF");
        TextField txtTelefone = field("Telefone", user == null ? "" : safe(user.getTelefone(), ""), "Contacto telefónico");
        TextField txtDepartamento = field("Departamento", user == null ? "" : safe(user.getDepartamento(), ""), "Ex.: Financeiro");
        TextField txtCargo = field("Cargo", user == null ? "" : safe(user.getCargo(), ""), "Ex.: Operador");

        ComboBox<Empresa> cbEmpresa = new ComboBox<>();
        cbEmpresa.getItems().addAll(empresaRepository.findAll());
        cbEmpresa.setValue(user == null ? null : user.getEmpresa());
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

        ComboBox<Role> cmbRole = new ComboBox<>(FXCollections.observableArrayList(Role.values()));
        cmbRole.setValue(user == null ? Role.USER : user.getRole());
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

        addFormRow(general, 0, "Nome:*", txtNome);
        addFormRow(general, 1, "Email:*", txtEmail);
        addFormRow(general, 2, "Empresa:*", cbEmpresa);
        addFormRow(general, 3, "Função:", cmbRole);
        addFormRow(general, 4, "NIF:", txtNif);
        addFormRow(general, 5, "Telefone:", txtTelefone);
        addFormRow(general, 6, "Departamento:", txtDepartamento);
        addFormRow(general, 7, "Cargo:", txtCargo);

        Tab tabGeral = new Tab("Geral", general);

        VBox security = new VBox(12);
        security.setPadding(new Insets(18));

        PasswordField txtSenha = new PasswordField();
        txtSenha.setPromptText(isNew ? "Senha inicial" : "Nova senha (deixe vazio para manter)");
        txtSenha.setMaxWidth(Double.MAX_VALUE);

        if (isNew) {
            txtSenha.setText(generateRandomPassword());
        } else if (user.isPasswordProvisoria()) {
            txtSenha.setPromptText("A senha actual é provisória");
        }

        DatePicker dataExpiracao = new DatePicker();
        dataExpiracao.setMaxWidth(Double.MAX_VALUE);
        dataExpiracao.setValue(user == null ? LocalDate.now().plusDays(90) : user.getDataExpiracaoPassword());

        CheckBox chkProvisoria = new CheckBox("Exigir troca de senha no próximo acesso");
        chkProvisoria.setSelected(isNew || user.isPasswordProvisoria());

        CheckBox chkMfa = new CheckBox("Autenticação multifactor (MFA)");
        chkMfa.setSelected(!isNew && user.isMfaEnabled());

        CheckBox chkAtivo = new CheckBox("Conta activa");
        chkAtivo.setSelected(user == null || Boolean.TRUE.equals(user.getActive()));
        chkAtivo.setDisable(isSelf);

        CheckBox chkSuperadmin = new CheckBox("Superadministrador");
        chkSuperadmin.setSelected(!isNew && user.isSuperadmin());
        boolean canManageSuperadmin = sessionManager.getUser() != null
                && sessionManager.getUser().isSuperadmin();
        chkSuperadmin.setDisable(!canManageSuperadmin);

        VBox passwordCard = new VBox(8);
        passwordCard.getStyleClass().add("kubata-users-security-card");
        passwordCard.getChildren().addAll(
                labelled("Senha", txtSenha),
                hint("Ao criar ou redefinir uma senha, é possível obrigar a troca no próximo acesso."),
                labelled("Expiração da senha", dataExpiracao),
                chkProvisoria,
                chkMfa,
                chkAtivo,
                chkSuperadmin
        );

        security.getChildren().add(passwordCard);

        Tab tabSeguranca = new Tab("Segurança", security);

        VBox perfisBox = new VBox(10);
        perfisBox.setPadding(new Insets(18));

        Label perfisHint = new Label(
                "Associe os perfis globais ou específicos da empresa. As permissões são herdadas dos perfis."
        );
        perfisHint.setWrapText(true);
        perfisHint.getStyleClass().add("kubata-users-form-hint");

        ListView<PerfilAcesso> listPerfis = new ListView<>();
        listPerfis.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        Runnable refreshProfiles = () -> {
            List<PerfilAcesso> available = new ArrayList<>();
            available.addAll(perfilRepository.findByEmpresaIsNull());
            if (cbEmpresa.getValue() != null) {
                available.addAll(perfilRepository.findByEmpresa(cbEmpresa.getValue()));
            }
            listPerfis.setItems(FXCollections.observableArrayList(available));

            if (user != null && user.getPerfis() != null) {
                for (PerfilAcesso p : user.getPerfis()) {
                    listPerfis.getSelectionModel().select(p);
                }
            }
        };

        cbEmpresa.valueProperty().addListener((obs, old, value) -> refreshProfiles.run());
        refreshProfiles.run();

        perfisBox.getChildren().addAll(perfisHint, listPerfis);
        VBox.setVgrow(listPerfis, Priority.ALWAYS);

        Tab tabPerfis = new Tab("Perfis e permissões", perfisBox);

        VBox preferencias = new VBox(12);
        preferencias.setPadding(new Insets(18));

        ComboBox<String> cmbIdioma = new ComboBox<>(
                FXCollections.observableArrayList("pt-AO", "pt-PT", "pt-BR", "en")
        );
        cmbIdioma.setValue(user == null ? "pt-AO" : safe(user.getIdioma(), "pt-AO"));
        cmbIdioma.setMaxWidth(Double.MAX_VALUE);

        ComboBox<String> cmbTema = new ComboBox<>(
                FXCollections.observableArrayList("VERDE_ADMIN", "CLARO", "ESCURO")
        );
        cmbTema.setValue(user == null ? "VERDE_ADMIN" : safe(user.getTema(), "VERDE_ADMIN"));
        cmbTema.setMaxWidth(Double.MAX_VALUE);

        Spinner<Integer> linhas = new Spinner<>(10, 500, user == null ? 50 : user.getLinhasPorPagina(), 10);
        linhas.setMaxWidth(Double.MAX_VALUE);

        preferencias.getChildren().addAll(
                labelled("Idioma", cmbIdioma),
                labelled("Tema", cmbTema),
                labelled("Linhas por página", linhas),
                hint("As preferências são guardadas no perfil do utilizador.")
        );

        Tab tabPreferencias = new Tab("Preferências", preferencias);

        tabs.getTabs().addAll(tabGeral, tabSeguranca, tabPerfis, tabPreferencias);

        ModalManager.ModalConfig config = new ModalManager.ModalConfig()
                .size(860, 650)
                .minSize(720, 520)
                .maxSize(1200, 860)
                .maximizable(true)
                .minimizable(true)
                .windowControls(true)
                .scrollable(false);

        modalManager.showConfirmModal(
                tabs,
                isNew ? "Novo utilizador" : "Editar utilizador — " + safe(user.getNome(), ""),
                () -> {
                    if (!can(isNew ? "CRIAR" : "EDITAR")) {
                        modalManager.alert("Acesso negado", "Não possui permissão para esta operação.", "warning", null);
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

                    if (isSelf && !chkAtivo.isSelected()) {
                        modalManager.alert(
                                "Operação não permitida",
                                "A sua própria conta não pode ser desactivada nesta área.",
                                "warning",
                                null
                        );
                        return;
                    }

                    User target = isNew ? new User() : user;
                    target.setNome(txtNome.getText().trim());
                    target.setEmail(txtEmail.getText().trim());
                    target.setEmpresa(cbEmpresa.getValue());
                    target.setRole(cmbRole.getValue());
                    target.setNif(blankToNull(txtNif.getText()));
                    target.setTelefone(blankToNull(txtTelefone.getText()));
                    target.setDepartamento(blankToNull(txtDepartamento.getText()));
                    target.setCargo(blankToNull(txtCargo.getText()));
                    target.setActive(chkAtivo.isSelected());
                    target.setMfaEnabled(chkMfa.isSelected());
                    target.setSuperadmin(chkSuperadmin.isSelected());
                    target.setPasswordProvisoria(chkProvisoria.isSelected());
                    target.setDataExpiracaoPassword(dataExpiracao.getValue());
                    target.setIdioma(cmbIdioma.getValue());
                    target.setTema(cmbTema.getValue());
                    target.setLinhasPorPagina(linhas.getValue());

                    Set<PerfilAcesso> selectedPerfis =
                            new HashSet<>(listPerfis.getSelectionModel().getSelectedItems());
                    target.setPerfis(selectedPerfis);

                    if (!txtSenha.getText().isBlank()) {
                        target.setPassword(passwordEncoder.encode(txtSenha.getText()));
                        target.setPasswordChangedAt(LocalDateTime.now());
                    } else if (isNew) {
                        modalManager.alert(
                                "Senha obrigatória",
                                "Defina uma senha inicial para o novo utilizador.",
                                "warning",
                                null
                        );
                        return;
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
        if (selected == null || !can("EDITAR")) {
            return;
        }

        String newPassword = generateRandomPassword();

        VBox box = new VBox(10);
        box.setPadding(new Insets(4));

        Label message = new Label(
                "Foi gerada uma nova senha temporária. Guarde-a e entregue-a ao utilizador por um canal seguro."
        );
        message.setWrapText(true);
        message.getStyleClass().add("kubata-users-form-hint");

        TextField generated = new TextField(newPassword);
        generated.setEditable(false);
        generated.setMaxWidth(Double.MAX_VALUE);

        Button copy = new Button(
                "Copiar senha",
                IconUtils.icon(Feather.COPY, 12)
        );
        copy.getStyleClass().add("button-outlined");
        copy.setOnAction(e -> {
            javafx.scene.input.ClipboardContent clipboard =
                    new javafx.scene.input.ClipboardContent();
            clipboard.putString(newPassword);
            javafx.scene.input.Clipboard.getSystemClipboard().setContent(clipboard);
        });

        box.getChildren().addAll(message, generated, copy);

        modalManager.showConfirmModal(
                box,
                "Redefinir senha — " + safe(selected.getNome(), selected.getEmail()),
                () -> {
                    selected.setPassword(passwordEncoder.encode(newPassword));
                    selected.setPasswordChangedAt(LocalDateTime.now());
                    selected.setPasswordProvisoria(true);
                    selected.setDataExpiracaoPassword(LocalDate.now().plusDays(90));
                    selected.setFailedAttempts(0);
                    selected.setLockoutEnd(null);

                    persistenceService.saveAsync(
                            userRepository,
                            selected,
                            "UTILIZADOR",
                            "Redefinição de senha: " + selected.getEmail(),
                            saved -> loadUsers()
                    );
                },
                null,
                new ModalManager.ModalConfig()
                        .size(520, 330)
                        .minSize(450, 300)
                        .maximizable(false)
                        .minimizable(false)
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

    private boolean can(String operation) {
        return securityService.hasPermission(
                sessionManager.getUser(),
                "UTILIZADORES",
                operation
        );
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

    private TextField field(String label, String value, String prompt) {
        TextField field = new TextField(value);
        field.setPromptText(prompt);
        field.setMaxWidth(Double.MAX_VALUE);
        return field;
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

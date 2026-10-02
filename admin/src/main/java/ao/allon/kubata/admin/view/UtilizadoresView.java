package ao.allon.kubata.admin.view;

import ao.allon.kubata.admin.service.SessionManager;
import ao.allon.kubata.admin.ui.modal.ModalManager;
import ao.allon.kubata.admin.ui.util.IconUtils;
import ao.allon.kubata.core.domain.Empresa;
import ao.allon.kubata.core.domain.Filial;
import ao.allon.kubata.core.domain.PerfilAcesso;
import ao.allon.kubata.core.domain.PermissaoPerfil;
import ao.allon.kubata.core.domain.Role;
import ao.allon.kubata.core.domain.TipoConta;
import ao.allon.kubata.core.domain.UserDevice;
import ao.allon.kubata.core.domain.UserSession;
import ao.allon.kubata.core.domain.User;
import ao.allon.kubata.core.repository.EmpresaRepository;
import ao.allon.kubata.core.repository.PerfilAcessoRepository;
import ao.allon.kubata.core.repository.UserRepository;
import ao.allon.kubata.core.service.AcessoService;
import ao.allon.kubata.core.service.PasswordResetService;
import ao.allon.kubata.core.service.MfaService;
import ao.allon.kubata.core.service.SecurityService;
import ao.allon.kubata.core.service.UserAdministrationService;
import ao.allon.kubata.core.service.UserDeviceService;
import ao.allon.kubata.core.ui.table.AdvancedTableView;
import ao.allon.kubata.core.ui.table.TableUtils;
import ao.allon.kubata.core.ui.table.TextTableCell;
import javafx.application.Platform;
import javafx.concurrent.Task;
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
import javafx.scene.paint.Color;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;
import org.kordamp.ikonli.feather.Feather;
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
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

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
    private final MfaService mfaService;
    private final SecurityService securityService;
    private final SessionManager sessionManager;
    private final ModalManager modalManager;
    private final UserAdministrationService userAdministrationService;
    private final UserDeviceService userDeviceService;
    private final PerfilSegurancaUtilizadorView perfilSegurancaUtilizadorView;

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
    private Label detailCodigo;
    private Label detailFilial;
    private Label detailTipoConta;

    private Button btnNovo;
    private Button btnEditar;
    private Button btnClonar;
    private Button btnStatus;
    private Button btnDesbloquear;
    private Button btnResetPassword;
    private Button btnMfa;
    private Button btnDispositivos;
    private Button btnSessoes;
    private Button btnPerfilSeguranca;

    private boolean dataLoaded;

    public UtilizadoresView(UserRepository userRepository,
                            PerfilAcessoRepository perfilRepository,
                            EmpresaRepository empresaRepository,
                            AcessoService acessoService,
                            PasswordResetService passwordResetService,
                            MfaService mfaService,
                            SecurityService securityService,
                            SessionManager sessionManager,
                            ModalManager modalManager,
                            UserAdministrationService userAdministrationService,
                            UserDeviceService userDeviceService,
                            PerfilSegurancaUtilizadorView perfilSegurancaUtilizadorView) {
        this.userRepository = userRepository;
        this.perfilRepository = perfilRepository;
        this.empresaRepository = empresaRepository;
        this.acessoService = acessoService;
        this.passwordResetService = passwordResetService;
        this.mfaService = mfaService;
        this.securityService = securityService;
        this.sessionManager = sessionManager;
        this.modalManager = modalManager;
        this.userAdministrationService = userAdministrationService;
        this.userDeviceService = userDeviceService;
        this.perfilSegurancaUtilizadorView = perfilSegurancaUtilizadorView;

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

        TableColumn<User, String> colCodigo = new TableColumn<>("Código");
        colCodigo.setCellValueFactory(new PropertyValueFactory<>("codigo"));
        colCodigo.setCellFactory(tc -> TextTableCell.create());
        colCodigo.setPrefWidth(125);

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

        TableColumn<User, String> colFilial = new TableColumn<>("Filial");
        colFilial.setCellValueFactory(cell ->
                new SimpleStringPropertySafe(
                        cell.getValue().getFilial() == null
                                ? "-"
                                : safe(cell.getValue().getFilial().getNome(), "-")
                ));
        colFilial.setPrefWidth(150);

        TableColumn<User, String> colTipoConta = new TableColumn<>("Tipo de conta");
        colTipoConta.setCellValueFactory(cell ->
                new SimpleStringPropertySafe(tipoContaLabel(cell.getValue().getTipoConta())));
        colTipoConta.setPrefWidth(150);

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
                colCodigo, colNome, colEmail, colEmpresa, colFilial,
                colTipoConta, colRole,
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

        detailCodigo = detailValue("Código", "-");
        detailStatus = detailValue("Estado", "-");
        detailEmpresa = detailValue("Empresa", "-");
        detailFilial = detailValue("Filial", "-");
        detailTipoConta = detailValue("Tipo de conta", "-");
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
                detailCodigo, detailStatus, detailEmpresa, detailFilial,
                detailTipoConta, detailRole,
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
        btnMfa = detailButton("Gerir MFA", Feather.SHIELD, "button-outlined");
        btnDispositivos = detailButton("Dispositivos", Feather.CPU, "button-outlined");
        btnSessoes = detailButton("Sessões", Feather.ACTIVITY, "button-outlined");
        btnPerfilSeguranca = detailButton("Perfil de segurança", Feather.SHIELD, "button-outlined");

        btnEditar.setOnAction(e -> selectedUser().ifPresent(this::showUserDialog));
        btnClonar.setOnAction(e -> cloneUser());
        btnStatus.setOnAction(e -> toggleSelectedStatus());
        btnDesbloquear.setOnAction(e -> unlockSelectedUser());
        btnResetPassword.setOnAction(e -> resetPassword());
        btnMfa.setOnAction(e -> selectedUser().ifPresent(this::showMfaManagementModal));
        btnDispositivos.setOnAction(e -> selectedUser().ifPresent(this::showDevicesModal));
        btnSessoes.setOnAction(e -> selectedUser().ifPresent(this::showSessionsModal));
        btnPerfilSeguranca.setOnAction(e -> selectedUser().ifPresent(perfilSegurancaUtilizadorView::open));

        GridPane actions = new GridPane();
        actions.setHgap(7);
        actions.setVgap(7);
        actions.add(btnEditar, 0, 0);
        actions.add(btnClonar, 1, 0);
        actions.add(btnStatus, 0, 1);
        actions.add(btnDesbloquear, 1, 1);
        actions.add(btnMfa, 0, 2);
        actions.add(btnResetPassword, 1, 2);
        actions.add(btnDispositivos, 0, 3);
        actions.add(btnSessoes, 1, 3);
        actions.add(btnPerfilSeguranca, 0, 4, 2, 1);

        GridPane.setHgrow(btnMfa, Priority.ALWAYS);

        GridPane.setHgrow(btnEditar, Priority.ALWAYS);
        GridPane.setHgrow(btnClonar, Priority.ALWAYS);
        GridPane.setHgrow(btnStatus, Priority.ALWAYS);
        GridPane.setHgrow(btnDesbloquear, Priority.ALWAYS);
        GridPane.setHgrow(btnResetPassword, Priority.ALWAYS);
        GridPane.setHgrow(btnDispositivos, Priority.ALWAYS);
        GridPane.setHgrow(btnSessoes, Priority.ALWAYS);
        GridPane.setHgrow(btnPerfilSeguranca, Priority.ALWAYS);

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
            detailCodigo.setText("-");
            detailFilial.setText("-");
            detailTipoConta.setText("-");
            setDetails("-", "-", "-", "-", "-", "-", "-", "-", "-", "-");
        } else {
            detailCodigo.setText(safe(selected.getCodigo(), "-"));
            detailFilial.setText(
                    selected.getFilial() == null
                            ? "Sem filial"
                            : safe(selected.getFilial().getNome(), "Filial")
            );
            detailTipoConta.setText(tipoContaLabel(selected.getTipoConta()));
            setDetails(
                    userStatus(selected),
                    companyName(selected.getEmpresa()),
                    roleLabel(selected.getRole()),
                    safe(selected.getDepartamento(), "-"),
                    safe(selected.getCargo(), "-"),
                    formatDateTime(selected.getUltimoAcesso()),
                    safe(selected.getUltimoIpLogin(), "-"),
                    selected.isMfaEnabled()
                            ? "Activo · " + mfaService.countRecoveryCodes(selected)
                            + " códigos de recuperação"
                            : "Inactivo",
                    credentialStatus(selected),
                    String.valueOf(selected.getFailedAttempts())
            );
        }

        btnEditar.setDisable(!hasSelection || !can("EDITAR"));
        btnClonar.setDisable(!hasSelection || !can("CRIAR"));
        btnResetPassword.setDisable(!hasSelection || !can("EDITAR"));
        btnMfa.setDisable(!hasSelection || !can("EDITAR"));
        btnDispositivos.setDisable(!hasSelection || !can("EDITAR"));
        btnSessoes.setDisable(!hasSelection || !can("EDITAR"));
        btnPerfilSeguranca.setDisable(!hasSelection || !can("EDITAR"));
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

        runUserTask(
                "Carregar utilizadores",
                () -> userAdministrationService.listar(sessionManager.getUser()),
                loaded -> {
                    users.setAll(loaded);
                    refreshFilters();
                    applyFilters();
                    updateSummary();
                    if (table != null) {
                        table.setLoading(false);
                    }
                }
        );
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
                    || contains(user.getCargo(), query)
                    || contains(user.getCodigo(), query)
                    || (user.getFilial() != null && contains(user.getFilial().getNome(), query));

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
        boolean isNew = user == null || user.getId() == null;

        if (!isNew && user.getId() != null) {
            user = userAdministrationService.carregarParaEdicao(
                    sessionManager.getUser(),
                    user.getId()
            );
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

        TextField txtCodigo = field(
                "Código",
                isNew ? "" : safe(formUser.getCodigo(), ""),
                "Gerado automaticamente"
        );
        txtCodigo.setEditable(false);

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

        addFormPair(personalGrid, 0, "Código:", txtCodigo, "Nome:*", txtNome);
        addFormPair(personalGrid, 1, "Email:*", txtEmail, "NIF:", txtNif);
        addFormPair(personalGrid, 2, "Telefone:", txtTelefone, null, new Label());

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

        ComboBox<Filial> cbFilial = new ComboBox<>();
        cbFilial.setMaxWidth(Double.MAX_VALUE);
        cbFilial.setPromptText("Seleccionar filial");
        cbFilial.setConverter(new StringConverter<>() {
            @Override
            public String toString(Filial object) {
                return object == null ? "" : object.toString();
            }

            @Override
            public Filial fromString(String string) {
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

        ComboBox<TipoConta> cmbTipoConta = new ComboBox<>(
                FXCollections.observableArrayList(TipoConta.values())
        );
        cmbTipoConta.setValue(
                isNew ? TipoConta.PESSOAL
                        : (formUser.getTipoConta() == null ? TipoConta.PESSOAL : formUser.getTipoConta())
        );
        cmbTipoConta.setMaxWidth(Double.MAX_VALUE);
        cmbTipoConta.setConverter(new StringConverter<>() {
            @Override
            public String toString(TipoConta object) {
                return object == null ? "" : tipoContaLabel(object);
            }

            @Override
            public TipoConta fromString(String string) {
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

        Runnable refreshFiliais = () -> {
            Empresa selectedEmpresa = cbEmpresa.getValue();
            List<Filial> options = selectedEmpresa == null
                    ? List.of()
                    : userAdministrationService.filiais(
                            sessionManager.getUser(),
                            selectedEmpresa
                    );
            Filial currentFilial = cbFilial.getValue();
            cbFilial.getItems().setAll(options);
            if (currentFilial != null && options.stream()
                    .anyMatch(f -> f.getId() != null && f.getId().equals(currentFilial.getId()))) {
                cbFilial.setValue(currentFilial);
            } else if (!isNew && formUser.getFilial() != null && options.stream()
                    .anyMatch(f -> f.getId() != null && f.getId().equals(formUser.getFilial().getId()))) {
                cbFilial.setValue(formUser.getFilial());
            } else {
                cbFilial.setValue(null);
            }
        };

        cbEmpresa.setOnAction(e -> refreshFiliais.run());
        if (!isNew && formUser.getEmpresa() != null) {
            cbEmpresa.setValue(formUser.getEmpresa());
            refreshFiliais.run();
        }

        addFormPair(organizationGrid, 0, "Empresa:*", cbEmpresa, "Filial:", cbFilial);
        addFormPair(organizationGrid, 1, "Função:", cmbRole, "Tipo de conta:", cmbTipoConta);
        addFormPair(organizationGrid, 2, "Departamento:", txtDepartamento, "Cargo:", txtCargo);

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

        final User mfaDraft = isNew ? new User() : formUser;
        if (isNew) {
            mfaDraft.setEmail(txtEmail.getText().trim());
        }

        final boolean[] mfaConfirmed = {
                !isNew && formUser.isMfaEnabled()
        };

        Label mfaStatus = new Label(
                chkMfa.isSelected()
                        ? "MFA activo"
                        : "MFA não configurado"
        );
        mfaStatus.getStyleClass().add(
                chkMfa.isSelected()
                        ? "kubata-users-security-ok"
                        : "kubata-users-form-hint"
        );

        Button manageMfa = new Button(
                chkMfa.isSelected()
                        ? "Gerir MFA"
                        : "Configurar MFA",
                IconUtils.icon(Feather.SHIELD, 12)
        );
        manageMfa.getStyleClass().add("button-outlined");

        manageMfa.setOnAction(e -> {
            mfaDraft.setEmail(txtEmail.getText().trim());
            if (mfaDraft.isMfaEnabled()) {
                showMfaManagementModal(mfaDraft, chkMfa, mfaConfirmed, mfaStatus, manageMfa);
            } else {
                showMfaActivationModal(mfaDraft, chkMfa, mfaConfirmed, mfaStatus, manageMfa);
            }
        });

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

        HBox mfaLine = new HBox(10, chkMfa, manageMfa, mfaStatus);
        mfaLine.setAlignment(Pos.CENTER_LEFT);

        chkMfa.setOnAction(e -> {
            if (chkMfa.isSelected() && !mfaConfirmed[0]) {
                mfaStatus.setText("Configure o MFA antes de guardar");
                manageMfa.setText("Configurar MFA");
                manageMfa.setGraphic(IconUtils.icon(Feather.SHIELD, 12));
            } else if (!chkMfa.isSelected()) {
                mfaConfirmed[0] = false;
                mfaStatus.setText("MFA será desactivado ao guardar");
                manageMfa.setText("Configurar MFA");
                manageMfa.setGraphic(IconUtils.icon(Feather.SHIELD, 12));
            }
        });

        HBox policyLine = new HBox(18, chkProvisoria);
        policyLine.setAlignment(Pos.CENTER_LEFT);

        HBox stateLine = new HBox(18, chkAtivo, chkSuperadmin);
        stateLine.setAlignment(Pos.CENTER_LEFT);

        accountCard.getChildren().addAll(
                accountGrid,
                policyLine,
                mfaLine,
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
                    setDisable(false);
                    getStyleClass().removeAll(
                            "kubata-users-profile-incompatible",
                            "kubata-users-profile-inactive"
                    );
                    return;
                }

                boolean active = Boolean.TRUE.equals(item.getActivo());
                boolean compatible = isProfileCompatible(item, cbEmpresa.getValue());

                VBox box = new VBox(2);

                HBox title = new HBox(7);
                title.setAlignment(Pos.CENTER_LEFT);

                Label code = new Label(
                        safe(item.getCodigo(), "PERFIL")
                );
                code.getStyleClass().add("kubata-users-profile-code");

                Label scope = new Label(
                        item.getEmpresa() == null
                                ? "GLOBAL"
                                : "EMPRESA · " + companyName(item.getEmpresa())
                );
                scope.getStyleClass().add(
                        item.getEmpresa() == null
                                ? "kubata-users-profile-scope-global"
                                : "kubata-users-profile-scope-company"
                );

                title.getChildren().addAll(code, scope);

                Label description = new Label(
                        safe(item.getDescricao(), "Sem descrição")
                );
                description.getStyleClass().add("kubata-users-profile-description");

                Label state = new Label();
                state.getStyleClass().add("kubata-users-profile-state");

                if (!active) {
                    state.setText("INACTIVO");
                    state.getStyleClass().add("kubata-users-profile-state-inactive");
                } else if (!compatible) {
                    state.setText(
                            cbEmpresa.getValue() == null
                                    ? "SELECCIONE A EMPRESA"
                                    : "OUTRA EMPRESA"
                    );
                    state.getStyleClass().add("kubata-users-profile-state-incompatible");
                } else {
                    state.setText("DISPONÍVEL");
                    state.getStyleClass().add("kubata-users-profile-state-active");
                }

                title.getChildren().add(state);
                box.getChildren().addAll(title, description);

                setText(null);
                setGraphic(box);
                setDisable(!active || !compatible);

                getStyleClass().removeAll(
                        "kubata-users-profile-incompatible",
                        "kubata-users-profile-inactive"
                );
                if (!active) {
                    getStyleClass().add("kubata-users-profile-inactive");
                } else if (!compatible) {
                    getStyleClass().add("kubata-users-profile-incompatible");
                }
            }
        });

        final List<PerfilAcesso> availableProfiles = new ArrayList<>();

        Runnable refreshProfiles = () -> {
            Set<Long> selectedIds = new HashSet<>(
                    listPerfis.getSelectionModel().getSelectedItems().stream()
                            .filter(p -> p != null && p.getId() != null)
                            .map(PerfilAcesso::getId)
                            .collect(Collectors.toSet())
            );

            if (formUser != null && formUser.getPerfis() != null) {
                formUser.getPerfis().stream()
                        .filter(p -> p != null && p.getId() != null)
                        .map(PerfilAcesso::getId)
                        .forEach(selectedIds::add);
            }

            availableProfiles.clear();

            // Carrega a matriz completa. O filtro de empresa passa a ser de
            // elegibilidade, não de visibilidade: nenhum perfil fica escondido.
            availableProfiles.addAll(perfilRepository.findAllWithEmpresa());

            availableProfiles.sort(
                    Comparator
                            .comparing((PerfilAcesso p) -> !Boolean.TRUE.equals(p.getActivo()))
                            .thenComparing((PerfilAcesso p) -> p.getEmpresa() == null ? "" : companyName(p.getEmpresa()))
                            .thenComparing(p -> safe(p.getCodigo(), ""))
            );

            String query = profileSearch.getText() == null
                    ? ""
                    : profileSearch.getText().trim().toLowerCase();

            List<PerfilAcesso> visible = availableProfiles.stream()
                    .filter(p -> query.isBlank()
                            || contains(p.getCodigo(), query)
                            || contains(p.getDescricao(), query)
                            || (p.getEmpresa() != null
                            && contains(p.getEmpresa().getNome(), query)))
                    .collect(Collectors.toList());

            listPerfis.setItems(
                    FXCollections.observableArrayList(visible)
            );
            listPerfis.getSelectionModel().clearSelection();

            for (PerfilAcesso p : visible) {
                if (p.getId() != null
                        && selectedIds.contains(p.getId())
                        && isProfileCompatible(p, cbEmpresa.getValue())
                        && Boolean.TRUE.equals(p.getActivo())) {
                    listPerfis.getSelectionModel().select(p);
                }
            }

            selectedProfiles.setText(
                    listPerfis.getSelectionModel().getSelectedItems().size()
                            + " seleccionados · "
                            + visible.size()
                            + " apresentados"
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
                                        + " seleccionados · "
                                        + listPerfis.getItems().size()
                                        + " apresentados"
                        )
        );

        selectAllProfiles.setOnAction(e ->
                listPerfis.getItems().stream()
                        .filter(p -> Boolean.TRUE.equals(p.getActivo()))
                        .filter(p -> isProfileCompatible(p, cbEmpresa.getValue()))
                        .forEach(p -> listPerfis.getSelectionModel().select(p))
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
                        "Todos os perfis activos são apresentados. GLOBAL pode ser atribuído a qualquer empresa; "
                                + "perfil de EMPRESA só pode ser atribuído à empresa seleccionada. "
                                + "Perfis incompatíveis ou inactivos ficam visíveis para facilitar a administração."
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

                    target.setCodigo(txtCodigo.getText().trim());
                    target.setNome(txtNome.getText().trim());
                    target.setEmail(txtEmail.getText().trim());
                    target.setEmpresa(cbEmpresa.getValue());
                    target.setFilial(cbFilial.getValue());
                    target.setRole(cmbRole.getValue());
                    target.setTipoConta(cmbTipoConta.getValue());
                    target.setNif(blankToNull(txtNif.getText()));
                    target.setTelefone(blankToNull(txtTelefone.getText()));
                    target.setDepartamento(blankToNull(txtDepartamento.getText()));
                    target.setCargo(blankToNull(txtCargo.getText()));
                    target.setAvatar(avatarBytes[0]);
                    target.setActive(chkAtivo.isSelected());

                    if (chkMfa.isSelected()) {
                        if (!mfaConfirmed[0]
                                || mfaDraft.getMfaSecret() == null
                                || !mfaDraft.isMfaEnabled()) {
                            modalManager.alert(
                                    "MFA não confirmado",
                                    "Configure e confirme o MFA com um código de 6 dígitos antes de guardar.",
                                    "warning",
                                    null
                            );
                            return;
                        }

                        target.setMfaEnabled(true);
                        target.setMfaSecret(mfaDraft.getMfaSecret());
                        target.setMfaRecoveryCodes(mfaDraft.getMfaRecoveryCodes());
                    } else {
                        target.setMfaEnabled(false);
                        target.setMfaSecret(null);
                        target.setMfaRecoveryCodes(null);
                    }

                    target.setSuperadmin(chkSuperadmin.isSelected());
                    target.setPasswordProvisoria(chkProvisoria.isSelected());
                    target.setDataExpiracaoPassword(dataExpiracao.getValue());
                    target.setIdioma(cmbIdioma.getValue());
                    target.setTema(cmbTema.getValue());
                    target.setLinhasPorPagina(linhas.getValue());

                    Set<PerfilAcesso> selectedPerfis =
                            new HashSet<>(
                                    listPerfis.getSelectionModel()
                                            .getSelectedItems()
                            );

                    runUserTask(
                            isNew ? "Criar utilizador" : "Actualizar utilizador",
                            () -> userAdministrationService.salvar(
                                    sessionManager.getUser(),
                                    target,
                                    password,
                                    selectedPerfis,
                                    cbEmpresa.getValue(),
                                    cbFilial.getValue(),
                                    "127.0.0.1"
                            ),
                            saved -> loadUsers()
                    );
                },
                null,
                config
        );
    }

    private <T> void runUserTask(String title, Callable<T> operation, Consumer<T> onSuccess) {
        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return operation.call();
            }
        };

        task.setOnSucceeded(event -> {
            if (onSuccess != null) {
                onSuccess.accept(task.getValue());
            }
        });
        task.setOnFailed(event -> {
            if (table != null) {
                table.setLoading(false);
            }
            Throwable error = task.getException();
            modalManager.showErrorModal(
                    title,
                    error == null || error.getMessage() == null
                            ? "Não foi possível concluir a operação."
                            : error.getMessage(),
                    error
            );
        });

        Thread worker = new Thread(task, "kubata-user-admin");
        worker.setDaemon(true);
        worker.start();
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
                    "Não pode arquivar a própria conta.",
                    "warning",
                    null
            );
            return;
        }

        if (!can("REMOVER")) {
            modalManager.alert("Acesso negado", "Não possui permissão para arquivar utilizadores.", "warning", null);
            return;
        }

        modalManager.showConfirmModal(
                confirmationContent(
                        "Arquivar utilizador",
                        "A conta será desactivada e as sessões serão terminadas. "
                                + "Os dados permanecem para garantir rastreabilidade."
                ),
                "Arquivar utilizador — " + safe(selected.getNome(), selected.getEmail()),
                () -> runUserTask(
                        "Arquivar utilizador",
                        () -> userAdministrationService.arquivar(
                                current,
                                selected.getId(),
                                "127.0.0.1"
                        ),
                        saved -> loadUsers()
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

    private void showDevicesModal(User user) {
        if (user == null || !can("VER")) {
            return;
        }

        TableView<UserDevice> deviceTable = new TableView<>();
        deviceTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        deviceTable.setPlaceholder(new Label("Nenhum dispositivo registado."));

        TableColumn<UserDevice, String> name = deviceTextColumn("Dispositivo", d -> safe(d.getDeviceName(), "Dispositivo"));
        TableColumn<UserDevice, String> type = deviceTextColumn("Tipo", d -> safe(d.getDeviceType(), "—"));
        TableColumn<UserDevice, String> platform = deviceTextColumn("Plataforma", d -> safe(d.getPlatform(), "—"));
        TableColumn<UserDevice, String> ip = deviceTextColumn("Último IP", d -> safe(d.getLastIp(), "—"));
        TableColumn<UserDevice, String> trusted = deviceTextColumn("Confiança", d -> d.isTrusted() ? "Confiável" : "Normal");
        TableColumn<UserDevice, String> lastSeen = deviceTextColumn(
                "Última actividade",
                d -> formatDateTime(d.getLastSeen())
        );
        TableColumn<UserDevice, String> state = deviceTextColumn(
                "Estado",
                d -> Boolean.TRUE.equals(d.getActive()) ? "Activo" : "Revogado"
        );
        deviceTable.getColumns().setAll(name, type, platform, ip, trusted, lastSeen, state);

        Button trust = new Button("Alterar confiança", IconUtils.icon(Feather.SHIELD, 12));
        Button revoke = new Button("Revogar", IconUtils.icon(Feather.X_CIRCLE, 12));
        Button refresh = new Button("Actualizar", IconUtils.icon(Feather.REFRESH_CW, 12));
        Button close = new Button("Fechar", IconUtils.icon(Feather.X, 12));
        trust.getStyleClass().add("button-outlined");
        revoke.getStyleClass().add("button-outlined");
        refresh.getStyleClass().add("button-outlined");
        close.getStyleClass().add("button-primary");

        Runnable load = () -> runUserTask(
                "Dispositivos",
                () -> userDeviceService.listar(sessionManager.getUser(), user.getId()),
                list -> deviceTable.getItems().setAll(list)
        );
        load.run();

        deviceTable.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            trust.setDisable(selected == null);
            revoke.setDisable(selected == null || !Boolean.TRUE.equals(selected.getActive()));
        });
        trust.setDisable(true);
        revoke.setDisable(true);

        trust.setOnAction(e -> {
            UserDevice selected = deviceTable.getSelectionModel().getSelectedItem();
            if (selected == null) return;
            boolean next = !selected.isTrusted();
            modalManager.showConfirm(
                    next ? "Marcar dispositivo como confiável" : "Retirar confiança",
                    "Alterar o nível de confiança de \"" 
                            + safe(selected.getDeviceName(), "Dispositivo") 
                            + "\"?",
                    () -> runUserTask(
                            "Confiança do dispositivo",
                            () -> userDeviceService.setTrusted(
                                    sessionManager.getUser(),
                                    selected.getId(),
                                    next,
                                    "127.0.0.1"
                            ),
                            saved -> load.run()
                    )
            );
        });

        revoke.setOnAction(e -> {
            UserDevice selected = deviceTable.getSelectionModel().getSelectedItem();
            if (selected == null) return;

            TextArea reason = new TextArea();
            reason.setPromptText("Motivo da revogação");
            reason.setPrefRowCount(3);
            reason.setWrapText(true);

            modalManager.showConfirmModal(
                    new VBox(10, new Label(
                            "O dispositivo deixará de ser utilizável pela conta até ser registado novamente."
                    ), reason),
                    "Revogar dispositivo",
                    () -> {
                        String value = reason.getText() == null ? "" : reason.getText().trim();
                        if (value.length() < 3) {
                            modalManager.alert(
                                    "Motivo obrigatório",
                                    "Indique um motivo com pelo menos 3 caracteres.",
                                    "warning",
                                    null
                            );
                            return;
                        }
                        runUserTask(
                                "Revogar dispositivo",
                                () -> userDeviceService.revogar(
                                        sessionManager.getUser(),
                                        selected.getId(),
                                        "127.0.0.1",
                                        value
                                ),
                                saved -> load.run()
                        );
                    },
                    null
            );
        });

        refresh.setOnAction(e -> load.run());
        close.setOnAction(e -> modalManager.hideModal());

        HBox actions = new HBox(8, trust, revoke, new Pane(), refresh, close);
        actions.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(actions.getChildren().get(2), Priority.ALWAYS);

        VBox content = new VBox(
                12,
                new Label("Dispositivos reconhecidos para " + safe(user.getEmail(), user.getNome())),
                deviceTable,
                actions
        );
        content.setPadding(new Insets(4));
        VBox.setVgrow(deviceTable, Priority.ALWAYS);

        modalManager.showModal(
                content,
                new ModalManager.ModalConfig()
                        .title("Dispositivos")
                        .subtitle("Identidade · confiança · revogação")
                        .icon(Feather.CPU)
                        .size(900, 520)
                        .minSize(720, 440)
                        .maximizable(true)
                        .minimizable(false)
        );
    }

    private void showSessionsModal(User user) {
        if (user == null || !can("VER")) {
            return;
        }

        TableView<UserSession> sessionTable = new TableView<>();
        sessionTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        sessionTable.setPlaceholder(new Label("Nenhuma sessão registada."));

        TableColumn<UserSession, String> workstation = sessionTextColumn("Posto", UserSession::getWorkstation);
        TableColumn<UserSession, String> ip = sessionTextColumn("IP", UserSession::getIpAddress);
        TableColumn<UserSession, String> context = sessionTextColumn("Contexto", UserSession::getContext);
        TableColumn<UserSession, String> login = sessionTextColumn(
                "Login",
                s -> formatDateTime(s.getLoginTime())
        );
        TableColumn<UserSession, String> memory = sessionTextColumn(
                "Memória",
                s -> safe(s.getMemoryUsage(), "—")
        );
        sessionTable.getColumns().setAll(workstation, ip, context, login, memory);

        Button terminate = new Button(
                "Terminar todas as sessões",
                IconUtils.icon(Feather.LOG_OUT, 12)
        );
        terminate.getStyleClass().add("button-outlined");
        Button refresh = new Button("Actualizar", IconUtils.icon(Feather.REFRESH_CW, 12));
        refresh.getStyleClass().add("button-outlined");
        Button close = new Button("Fechar", IconUtils.icon(Feather.X, 12));
        close.getStyleClass().add("button-primary");

        Runnable load = () -> runUserTask(
                "Sessões",
                () -> userAdministrationService.sessoes(
                        sessionManager.getUser(),
                        user.getId()
                ),
                list -> sessionTable.getItems().setAll(list)
        );
        load.run();

        terminate.setOnAction(e -> modalManager.showConfirm(
                "Terminar sessões",
                "Todas as sessões registadas de " + safe(user.getNome(), user.getEmail()) + " serão terminadas.",
                () -> runUserTask(
                        "Terminar sessões",
                        () -> userAdministrationService.terminarSessoes(
                                sessionManager.getUser(),
                                user.getId(),
                                "127.0.0.1"
                        ),
                        count -> {
                            load.run();
                            modalManager.alert(
                                    "Sessões terminadas",
                                    count + " sessão(ões) terminada(s).",
                                    "info",
                                    null
                            );
                        }
                )
        ));

        refresh.setOnAction(e -> load.run());
        close.setOnAction(e -> modalManager.hideModal());

        HBox actions = new HBox(8, terminate, new Pane(), refresh, close);
        actions.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(actions.getChildren().get(1), Priority.ALWAYS);

        VBox content = new VBox(
                12,
                new Label("Sessões registadas de " + safe(user.getEmail(), user.getNome())),
                sessionTable,
                actions
        );
        content.setPadding(new Insets(4));
        VBox.setVgrow(sessionTable, Priority.ALWAYS);

        modalManager.showModal(
                content,
                new ModalManager.ModalConfig()
                        .title("Sessões do utilizador")
                        .subtitle("Administração de sessões e revogação de acesso")
                        .icon(Feather.ACTIVITY)
                        .size(860, 500)
                        .minSize(700, 420)
                        .maximizable(true)
                        .minimizable(false)
        );
    }

    private TableColumn<UserDevice, String> deviceTextColumn(
            String title,
            java.util.function.Function<UserDevice, String> mapper) {
        TableColumn<UserDevice, String> column = new TableColumn<>(title);
        column.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                safe(mapper.apply(data.getValue()), "—")
        ));
        return column;
    }

    private TableColumn<UserSession, String> sessionTextColumn(
            String title,
            java.util.function.Function<UserSession, String> mapper) {
        TableColumn<UserSession, String> column = new TableColumn<>(title);
        column.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                safe(mapper.apply(data.getValue()), "—")
        ));
        return column;
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
        clone.setTipoConta(selected.getTipoConta());
        clone.setEmpresa(selected.getEmpresa());
        clone.setFilial(selected.getFilial());
        clone.setNif(selected.getNif());
        clone.setTelefone(selected.getTelefone());
        clone.setDepartamento(selected.getDepartamento());
        clone.setCargo(selected.getCargo());
        clone.setActive(true);
        clone.setMfaEnabled(false);
        clone.setPasswordProvisoria(true);
        clone.setDataExpiracaoPassword(LocalDate.now().plusDays(90));
        clone.setPerfis(selected.getPerfis() == null
                ? new HashSet<>()
                : new HashSet<>(selected.getPerfis()));

        showUserDialog(clone);
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
                    runUserTask(
                            next ? "Activar utilizador" : "Desactivar utilizador",
                            () -> userAdministrationService.alterarEstado(
                                    current,
                                    selected.getId(),
                                    next,
                                    "127.0.0.1"
                            ),
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
                    runUserTask(
                            "Desbloquear utilizador",
                            () -> userAdministrationService.desbloquear(
                                    sessionManager.getUser(),
                                    selected.getId(),
                                    "127.0.0.1"
                            ),
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

    private void showMfaActivationModal(
            User user,
            CheckBox mfaCheckBox,
            boolean[] mfaConfirmed,
            Label mfaStatus,
            Button manageButton
    ) {
        if (user == null) {
            return;
        }

        if (user.getEmail() == null || user.getEmail().isBlank()) {
            modalManager.alert(
                    "Email necessário",
                    "Preencha o email do utilizador antes de configurar o MFA.",
                    "warning",
                    null
            );
            return;
        }

        final String secret;
        try {
            secret = mfaService.prepareActivation(user);
        } catch (Exception ex) {
            modalManager.showErrorModal(
                    "MFA",
                    "Não foi possível iniciar a configuração do MFA.",
                    ex
            );
            return;
        }

        final String provisioningUri = mfaService.buildProvisioningUri(user);

        ImageView qrView = new ImageView();
        qrView.setFitWidth(220);
        qrView.setFitHeight(220);
        qrView.setPreserveRatio(true);

        try {
            qrView.setImage(createQrImage(provisioningUri, 220));
        } catch (Exception ex) {
            modalManager.showErrorModal(
                    "QR Code",
                    "Não foi possível gerar o QR Code do MFA.",
                    ex
            );
            return;
        }

        Label instructions = new Label(
                "1. Abra a aplicação autenticadora no telemóvel.\n"
                        + "2. Leia o QR Code abaixo.\n"
                        + "3. Introduza o código de 6 dígitos apresentado para confirmar."
        );
        instructions.setWrapText(true);
        instructions.getStyleClass().add("kubata-users-form-hint");

        TextField secretField = new TextField(secret);
        secretField.setEditable(false);
        secretField.setMaxWidth(Double.MAX_VALUE);
        secretField.setTooltip(
                new Tooltip("Chave secreta para configuração manual")
        );

        TextField codeField = new TextField();
        codeField.setPromptText("Código MFA de 6 dígitos");
        codeField.setPrefHeight(44);
        codeField.setTextFormatter(
                new TextFormatter<String>(change ->
                        change.getControlNewText().matches("\\d{0,6}")
                                ? change
                                : null
                )
        );

        Label status = new Label();
        status.setWrapText(true);
        status.setManaged(false);
        status.setVisible(false);

        Button cancel = new Button(
                "Cancelar",
                IconUtils.icon(Feather.X, 12)
        );
        cancel.getStyleClass().add("button-outlined");

        Button confirm = new Button(
                "Confirmar MFA",
                IconUtils.icon(Feather.SHIELD, 12)
        );
        confirm.getStyleClass().add("button-primary");

        HBox actions = new HBox(8, cancel, confirm);
        actions.setAlignment(Pos.CENTER_RIGHT);

        VBox qrBox = new VBox(8, qrView);
        qrBox.setAlignment(Pos.CENTER);
        qrBox.setMinWidth(250);

        VBox manualBox = new VBox(
                8,
                new Label("Chave manual"),
                secretField,
                new Label("Código de confirmação"),
                codeField,
                status
        );
        manualBox.setPrefWidth(360);

        HBox contentTop = new HBox(18, qrBox, manualBox);
        contentTop.setAlignment(Pos.TOP_LEFT);

        VBox content = new VBox(
                14,
                instructions,
                contentTop,
                new Separator(),
                new Label(
                        "Guarde os códigos de recuperação apresentados após a confirmação."
                                + " Eles são de uso único."
                ),
                actions
        );
        content.setPadding(new Insets(4));
        content.setPrefWidth(690);

        cancel.setOnAction(e -> modalManager.hideModal());

        confirm.setOnAction(e -> {
            String code = codeField.getText() == null
                    ? ""
                    : codeField.getText().trim();

            if (code.length() != 6) {
                status.setText("Introduza o código actual de 6 dígitos.");
                status.setStyle("-fx-text-fill: #b91c1c; -fx-font-size: 11px;");
                status.setManaged(true);
                status.setVisible(true);
                codeField.requestFocus();
                return;
            }

            confirm.setDisable(true);
            cancel.setDisable(true);
            confirm.setText("A validar...");

            try {
                MfaService.ActivationResult result =
                        user.getId() != null
                                ? mfaService.adminConfirmActivation(
                                        sessionManager.getUser(),
                                        user.getId(),
                                        code
                                )
                                : mfaService.confirmActivation(user, code);

                if (user.getId() != null) {
                    User refreshed = userAdministrationService.carregarParaEdicao(sessionManager.getUser(), user.getId());
                    user.setMfaEnabled(refreshed.isMfaEnabled());
                    user.setMfaSecret(refreshed.getMfaSecret());
                    user.setMfaRecoveryCodes(refreshed.getMfaRecoveryCodes());
                }

                mfaCheckBox.setSelected(true);
                mfaConfirmed[0] = true;
                mfaStatus.setText(
                        "MFA activo · "
                                + result.recoveryCodes().size()
                                + " códigos de recuperação"
                );
                mfaStatus.setStyle(
                        "-fx-text-fill: #166534; -fx-font-size: 11px; -fx-font-weight: 700;"
                );
                manageButton.setText("Gerir MFA");
                manageButton.setGraphic(IconUtils.icon(Feather.SHIELD, 12));

                modalManager.hideModal();
                loadUsers();

                showRecoveryCodesModal(
                        user,
                        result.recoveryCodes(),
                        "MFA activado"
                );
            } catch (Exception ex) {
                confirm.setDisable(false);
                cancel.setDisable(false);
                confirm.setText("Confirmar MFA");
                status.setText(
                        ex.getMessage() == null
                                ? "O código MFA é inválido ou expirou."
                                : ex.getMessage()
                );
                status.setStyle("-fx-text-fill: #b91c1c; -fx-font-size: 11px;");
                status.setManaged(true);
                status.setVisible(true);
                codeField.requestFocus();
                codeField.selectAll();
            }
        });

        modalManager.showModal(
                content,
                new ModalManager.ModalConfig()
                        .title("Configurar autenticação multifactor")
                        .subtitle(
                                "Associe esta conta a uma aplicação autenticadora"
                        )
                        .icon(Feather.SHIELD)
                        .tone(ModalManager.ModalTone.INFO)
                        .size(780, 540)
                        .minSize(700, 500)
                        .maximizable(false)
                        .minimizable(false)
                        .closeOnOverlayClick(false)
                        .closeOnEscape(false)
        );

        Platform.runLater(() -> codeField.requestFocus());
    }

    /**
     * Abre a gestão de MFA a partir do painel de detalhes da lista.
     * O fluxo completo (QR, confirmação, recuperação e desactivação)
     * reutiliza o mesmo modal usado pelo formulário de utilizador.
     */
    private void showMfaManagementModal(User user) {
        if (user == null) {
            return;
        }

        CheckBox mfaCheckBox = new CheckBox();
        mfaCheckBox.setSelected(user.isMfaEnabled());

        boolean[] mfaConfirmed = {
                user.isMfaEnabled()
        };

        Label mfaStatus = new Label(
                user.isMfaEnabled()
                        ? "MFA activo"
                        : "MFA não configurado"
        );

        Button manageButton = new Button("Gerir MFA");

        showMfaManagementModal(
                user,
                mfaCheckBox,
                mfaConfirmed,
                mfaStatus,
                manageButton
        );
    }

    private void showMfaManagementModal(
            User user,
            CheckBox mfaCheckBox,
            boolean[] mfaConfirmed,
            Label mfaStatus,
            Button manageButton
    ) {
        if (user == null) {
            return;
        }

        if (!user.isMfaEnabled()) {
            showMfaActivationModal(
                    user,
                    mfaCheckBox,
                    mfaConfirmed,
                    mfaStatus,
                    manageButton
            );
            return;
        }

        int remaining = mfaService.countRecoveryCodes(user);

        Label status = new Label(
                "MFA activo. Existem " + remaining
                        + " código(s) de recuperação disponível(eis)."
        );
        status.setWrapText(true);
        status.setStyle(
                "-fx-background-color: #f0fdf4;"
                        + "-fx-background-radius: 10;"
                        + "-fx-border-color: #bbf7d0;"
                        + "-fx-border-radius: 10;"
                        + "-fx-padding: 10;"
                        + "-fx-text-fill: #166534;"
                        + "-fx-font-size: 12px;"
        );

        TextField totpCode = new TextField();
        totpCode.setPromptText(
                "Código MFA actual para gerar novos códigos"
        );
        totpCode.setPrefHeight(44);
        totpCode.setTextFormatter(
                new TextFormatter<String>(change ->
                        change.getControlNewText().matches("\\d{0,6}")
                                ? change
                                : null
                )
        );

        Label hint = new Label(
                "Gerar novos códigos invalida imediatamente todos os códigos de recuperação anteriores."
        );
        hint.setWrapText(true);
        hint.getStyleClass().add("kubata-users-form-hint");

        Label error = new Label();
        error.setWrapText(true);
        error.setManaged(false);
        error.setVisible(false);

        Button regenerate = new Button(
                "Gerar novos códigos",
                IconUtils.icon(Feather.REFRESH_CW, 12)
        );
        regenerate.getStyleClass().add("button-outlined");

        Button disable = new Button(
                "Desactivar MFA",
                IconUtils.icon(Feather.SHIELD_OFF, 12)
        );
        disable.getStyleClass().add("button-outlined");

        Button close = new Button(
                "Fechar",
                IconUtils.icon(Feather.X, 12)
        );
        close.getStyleClass().add("button-outlined");

        HBox actions = new HBox(
                8,
                regenerate,
                disable,
                close
        );
        actions.setAlignment(Pos.CENTER_RIGHT);

        VBox content = new VBox(
                14,
                status,
                new Label("Código MFA actual"),
                totpCode,
                hint,
                error,
                actions
        );
        content.setPadding(new Insets(4));
        content.setPrefWidth(580);

        close.setOnAction(e -> modalManager.hideModal());

        regenerate.setOnAction(e -> {
            String code = totpCode.getText() == null
                    ? ""
                    : totpCode.getText().trim();

            if (code.length() != 6) {
                error.setText("Introduza o código MFA actual de 6 dígitos.");
                error.setStyle("-fx-text-fill: #b91c1c; -fx-font-size: 11px;");
                error.setManaged(true);
                error.setVisible(true);
                return;
            }

            regenerate.setDisable(true);
            disable.setDisable(true);
            close.setDisable(true);
            error.setManaged(false);
            error.setVisible(false);

            javafx.concurrent.Task<List<String>> task =
                    new javafx.concurrent.Task<>() {
                        @Override
                        protected List<String> call() {
                            return user.getId() != null
                                    ? mfaService.adminRegenerateRecoveryCodes(
                                            sessionManager.getUser(),
                                            user.getId(),
                                            code,
                                            "127.0.0.1"
                                    )
                                    : mfaService.regenerateRecoveryCodes(user, code);
                        }
                    };

            task.setOnSucceeded(event -> {
                modalManager.hideModal();
                showRecoveryCodesModal(
                        user,
                        task.getValue(),
                        "Novos códigos de recuperação"
                );
                loadUsers();
            });

            task.setOnFailed(event -> {
                regenerate.setDisable(false);
                disable.setDisable(false);
                close.setDisable(false);
                Throwable failure = task.getException();
                error.setText(
                        failure == null || failure.getMessage() == null
                                ? "Não foi possível gerar novos códigos."
                                : failure.getMessage()
                );
                error.setStyle("-fx-text-fill: #b91c1c; -fx-font-size: 11px;");
                error.setManaged(true);
                error.setVisible(true);
            });

            Thread worker = new Thread(task, "kubata-mfa-recovery");
            worker.setDaemon(true);
            worker.start();
        });

        disable.setOnAction(e -> modalManager.showConfirmModal(
                new VBox(
                        10,
                        new Label(
                                "O MFA será desactivado e a chave TOTP e todos os "
                                        + "códigos de recuperação serão removidos desta conta."
                        ),
                        new Label(
                                "A conta ficará imediatamente sem o segundo factor."
                        )
                ),
                "Desactivar MFA",
                () -> {
                    try {
                        if (user.getId() != null) {
                            mfaService.adminDisableMfa(
                                    sessionManager.getUser(),
                                    user.getId(),
                                    "127.0.0.1"
                            );
                        } else {
                            mfaService.disableMfa(user);
                        }
                        mfaCheckBox.setSelected(false);
                        mfaConfirmed[0] = false;
                        mfaStatus.setText("MFA será desactivado ao guardar");
                        mfaStatus.setStyle(
                                "-fx-text-fill: #92400e; -fx-font-size: 11px; -fx-font-weight: 700;"
                        );
                        manageButton.setText("Configurar MFA");
                        manageButton.setGraphic(
                                IconUtils.icon(Feather.SHIELD, 12)
                        );
                        loadUsers();
                        modalManager.hideModal();
                    } catch (Exception ex) {
                        modalManager.showErrorModal(
                                "MFA",
                                "Não foi possível desactivar o MFA.",
                                ex
                        );
                    }
                },
                null,
                new ModalManager.ModalConfig()
                        .size(500, 280)
                        .minSize(440, 240)
                        .maximizable(false)
                        .minimizable(false)
                        .icon(Feather.SHIELD_OFF)
                        .tone(ModalManager.ModalTone.WARNING)
        ));

        modalManager.showModal(
                content,
                new ModalManager.ModalConfig()
                        .title("Gestão do MFA")
                        .subtitle(
                                safe(user.getNome(), user.getEmail())
                        )
                        .icon(Feather.SHIELD)
                        .tone(ModalManager.ModalTone.INFO)
                        .size(640, 390)
                        .minSize(560, 340)
                        .maximizable(false)
                        .minimizable(false)
        );
    }

    private void showRecoveryCodesModal(
            User user,
            List<String> codes,
            String title
    ) {
        TextArea codesArea = new TextArea(
                String.join(System.lineSeparator(), codes)
        );
        codesArea.setEditable(false);
        codesArea.setWrapText(false);
        codesArea.setPrefRowCount(10);
        codesArea.setStyle(
                "-fx-font-family: 'Consolas';"
                        + "-fx-font-size: 15px;"
                        + "-fx-font-weight: 700;"
                        + "-fx-letter-spacing: 1px;"
        );

        Label warning = new Label(
                "IMPORTANTE: estes códigos só serão apresentados agora. "
                        + "Guarde-os num local seguro. Cada código pode ser usado uma única vez."
        );
        warning.setWrapText(true);
        warning.setStyle(
                "-fx-background-color: #fff8eb;"
                        + "-fx-background-radius: 10;"
                        + "-fx-border-color: #f5ddb0;"
                        + "-fx-border-radius: 10;"
                        + "-fx-padding: 10;"
                        + "-fx-text-fill: #8a5a00;"
                        + "-fx-font-size: 11px;"
        );

        Button copy = new Button(
                "Copiar códigos",
                IconUtils.icon(Feather.COPY, 12)
        );
        copy.getStyleClass().add("button-outlined");

        Button close = new Button(
                "Fechar",
                IconUtils.icon(Feather.CHECK, 12)
        );
        close.getStyleClass().add("button-primary");

        HBox actions = new HBox(8, copy, close);
        actions.setAlignment(Pos.CENTER_RIGHT);

        VBox content = new VBox(
                12,
                new Label(
                        "Conta: "
                                + safe(user.getEmail(), user.getNome())
                ),
                warning,
                codesArea,
                actions
        );
        content.setPadding(new Insets(4));
        content.setPrefWidth(600);

        copy.setOnAction(e -> {
            javafx.scene.input.ClipboardContent clipboard =
                    new javafx.scene.input.ClipboardContent();
            clipboard.putString(String.join(
                    System.lineSeparator(),
                    codes
            ));
            javafx.scene.input.Clipboard.getSystemClipboard()
                    .setContent(clipboard);
        });

        close.setOnAction(e -> modalManager.hideModal());

        modalManager.showModal(
                content,
                new ModalManager.ModalConfig()
                        .title(title)
                        .subtitle("Códigos de recuperação de uso único")
                        .icon(Feather.KEY)
                        .tone(ModalManager.ModalTone.WARNING)
                        .singleButton("Concluir")
                        .size(680, 500)
                        .minSize(580, 440)
                        .maximizable(false)
                        .minimizable(false)
                        .closeOnOverlayClick(false)
        );
    }

    private Image createQrImage(String text, int size) throws Exception {
        BitMatrix matrix = new QRCodeWriter().encode(
                text,
                BarcodeFormat.QR_CODE,
                size,
                size
        );

        WritableImage image = new WritableImage(size, size);
        javafx.scene.image.PixelWriter writer =
                image.getPixelWriter();

        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                writer.setColor(
                        x,
                        y,
                        matrix.get(x, y)
                                ? Color.BLACK
                                : Color.WHITE
                );
            }
        }

        return image;
    }

    private void showDetailsModal(User user) {
        if (user == null) {
            return;
        }

        VBox content = new VBox(14);
        content.setPadding(new Insets(4));

        content.getChildren().addAll(
                detailCard("Identidade", List.of(
                        "Código: " + safe(user.getCodigo(), "-"),
                        "Nome: " + safe(user.getNome(), "-"),
                        "Email: " + safe(user.getEmail(), "-"),
                        "NIF: " + safe(user.getNif(), "-"),
                        "Telefone: " + safe(user.getTelefone(), "-")
                )),
                detailCard("Organização", List.of(
                        "Empresa: " + companyName(user.getEmpresa()),
                        "Filial: " + (user.getFilial() == null ? "Sem filial" : safe(user.getFilial().getNome(), "-")),
                        "Tipo de conta: " + tipoContaLabel(user.getTipoConta()),
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
            btnMfa.setDisable(!hasSelection || !can("EDITAR"));
            btnDispositivos.setDisable(!hasSelection || !can("VER"));
            btnSessoes.setDisable(!hasSelection || !can("VER"));
            btnPerfilSeguranca.setDisable(!hasSelection || !can("EDITAR"));
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

    private String tipoContaLabel(TipoConta tipoConta) {
        if (tipoConta == null) {
            return "Pessoal";
        }
        return switch (tipoConta) {
            case PESSOAL -> "Pessoal";
            case ADMINISTRATIVA -> "Administrativa";
            case SERVICO -> "Serviço";
            case API -> "API";
            case TECNICA -> "Técnica";
            case TEMPORARIA -> "Temporária";
        };
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

    private boolean isProfileCompatible(PerfilAcesso profile, Empresa selectedEmpresa) {
        if (profile == null || !Boolean.TRUE.equals(profile.getActivo())) {
            return false;
        }

        if (profile.getEmpresa() == null) {
            return true;
        }

        return selectedEmpresa != null
                && profile.getEmpresa().getId() != null
                && selectedEmpresa.getId() != null
                && profile.getEmpresa().getId().equals(selectedEmpresa.getId());
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

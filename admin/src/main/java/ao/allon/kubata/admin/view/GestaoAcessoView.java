package ao.allon.kubata.admin.view;

import ao.allon.kubata.admin.service.SessionManager;
import ao.allon.kubata.admin.ui.modal.ModalManager;
import ao.allon.kubata.admin.ui.util.IconUtils;
import ao.allon.kubata.core.domain.Empresa;
import ao.allon.kubata.core.domain.PerfilAcesso;
import ao.allon.kubata.core.domain.Role;
import ao.allon.kubata.core.domain.User;
import ao.allon.kubata.core.repository.PerfilAcessoRepository;
import ao.allon.kubata.core.service.UserAdministrationService;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.StringConverter;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class GestaoAcessoView extends BorderPane {

    private final UserAdministrationService userAdministrationService;
    private final PerfilAcessoRepository perfilRepository;
    private final SessionManager sessionManager;
    private final ModalManager modalManager;
    private final UtilizadoresView utilizadoresView;
    private final PerfilSegurancaUtilizadorView perfilSegurancaUtilizadorView;

    private final ObservableList<User> users = FXCollections.observableArrayList();
    private final ObservableList<User> filteredUsers = FXCollections.observableArrayList();
    private final ObservableList<PerfilAcesso> perfis = FXCollections.observableArrayList();

    private TextField searchField;
    private ComboBox<Role> roleFilter;
    private ComboBox<String> statusFilter;
    private ComboBox<Empresa> empresaFilter;
    private TableView<User> table;

    private Label totalValue;
    private Label activeValue;
    private Label noProfileValue;
    private Label mfaValue;
    private Label statusLabel;

    private Label detailName;
    private Label detailEmail;
    private Label detailRole;
    private Label detailEmpresa;
    private Label detailEstado;
    private Label detailPerfis;
    private VBox detailProfilesBox;
    private Button btnEditAccess;
    private Button btnSecurityProfile;

    private boolean loaded;

    public GestaoAcessoView(
            UserAdministrationService userAdministrationService,
            PerfilAcessoRepository perfilRepository,
            SessionManager sessionManager,
            ModalManager modalManager,
            UtilizadoresView utilizadoresView,
            PerfilSegurancaUtilizadorView perfilSegurancaUtilizadorView) {
        this.userAdministrationService = userAdministrationService;
        this.perfilRepository = perfilRepository;
        this.sessionManager = sessionManager;
        this.modalManager = modalManager;
        this.utilizadoresView = utilizadoresView;
        this.perfilSegurancaUtilizadorView = perfilSegurancaUtilizadorView;

        buildUI();
        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null && !loaded) {
                loaded = true;
                loadData();
            }
        });
    }

    private void buildUI() {
        setPadding(Insets.EMPTY);
        getStyleClass().add("kubata-access-page");

        setTop(buildHeader());
        setCenter(buildWorkspace());
        setBottom(buildStatusBar());
    }

    private Node buildHeader() {
        VBox header = new VBox(12);
        header.setPadding(new Insets(18, 22, 14, 22));
        header.getStyleClass().add("kubata-access-header");

        HBox titleLine = new HBox(12);
        titleLine.setAlignment(Pos.CENTER_LEFT);

        Label icon = new Label("", IconUtils.icon(Feather.LOCK, 24));
        icon.getStyleClass().add("kubata-access-title-icon");

        VBox titleBox = new VBox(2);
        Label title = new Label("Gestão de Acesso");
        title.getStyleClass().add("kubata-access-title");

        Label subtitle = new Label(
                "Centro de controlo para atribuição de perfis, permissões, políticas individuais e acesso por utilizador."
        );
        subtitle.getStyleClass().add("kubata-access-subtitle");
        subtitle.setWrapText(true);
        titleBox.getChildren().addAll(title, subtitle);

        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button refresh = new Button(
                "Actualizar",
                IconUtils.icon(Feather.REFRESH_CW, 13)
        );
        refresh.getStyleClass().add("button-outlined");
        refresh.setOnAction(e -> loadData());

        Button newUser = new Button(
                "Novo Utilizador",
                IconUtils.icon(Feather.USER_PLUS, 13)
        );
        newUser.getStyleClass().add("button-primary");
        newUser.setOnAction(e -> utilizadoresView.showUserDialog(null));

        titleLine.getChildren().addAll(icon, titleBox, spacer, refresh, newUser);

        totalValue = new Label("0");
        activeValue = new Label("0");
        noProfileValue = new Label("0");
        mfaValue = new Label("0");

        HBox kpis = new HBox(10,
                kpi("UTILIZADORES", Feather.USERS, totalValue),
                kpi("ACTIVOS", Feather.CHECK_CIRCLE, activeValue),
                kpi("SEM PERFIL", Feather.USER_X, noProfileValue),
                kpi("MFA", Feather.SHIELD, mfaValue)
        );

        header.getChildren().addAll(titleLine, kpis);
        return header;
    }

    private VBox kpi(String title, Feather icon, Label value) {
        VBox card = new VBox(2);
        card.getStyleClass().add("kubata-access-kpi");
        card.setPadding(new Insets(9, 13, 9, 13));
        card.setMinWidth(155);

        HBox top = new HBox(7);
        top.setAlignment(Pos.CENTER_LEFT);

        Label iconLabel = new Label("", IconUtils.icon(icon, 14));
        iconLabel.getStyleClass().add("kubata-access-kpi-icon");

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("kubata-access-kpi-title");
        top.getChildren().addAll(iconLabel, titleLabel);

        value.getStyleClass().add("kubata-access-kpi-value");
        card.getChildren().addAll(top, value);
        return card;
    }

    private BorderPane buildWorkspace() {
        BorderPane workspace = new BorderPane();
        workspace.getStyleClass().add("kubata-access-workspace");
        workspace.setTop(buildFilters());
        workspace.setCenter(buildMainArea());
        return workspace;
    }

    private HBox buildFilters() {
        HBox bar = new HBox(8);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(9, 14, 9, 14));
        bar.getStyleClass().add("kubata-access-filter-bar");

        searchField = new TextField();
        searchField.setPromptText("Pesquisar nome, email ou código...");
        searchField.setPrefWidth(310);
        searchField.textProperty().addListener((obs, old, value) -> applyFilters());

        empresaFilter = new ComboBox<>();
        empresaFilter.setPromptText("Empresa");
        empresaFilter.setPrefWidth(190);
        empresaFilter.setConverter(new StringConverter<>() {
            @Override
            public String toString(Empresa object) {
                return object == null ? "Todas as empresas" : safe(object.getNome(), "Empresa");
            }

            @Override
            public Empresa fromString(String string) {
                return null;
            }
        });
        empresaFilter.valueProperty().addListener((obs, old, value) -> applyFilters());

        roleFilter = new ComboBox<>();
        roleFilter.setPromptText("Função");
        roleFilter.setPrefWidth(165);
        roleFilter.setConverter(new StringConverter<>() {
            @Override
            public String toString(Role object) {
                return object == null ? "Todas as funções" : roleLabel(object);
            }

            @Override
            public Role fromString(String string) {
                return null;
            }
        });
        roleFilter.valueProperty().addListener((obs, old, value) -> applyFilters());

        statusFilter = new ComboBox<>(FXCollections.observableArrayList(
                "Todos",
                "Activos",
                "Inactivos",
                "Sem perfil",
                "Com perfil",
                "MFA activo",
                "MFA inactivo"
        ));
        statusFilter.setValue("Todos");
        statusFilter.setPrefWidth(150);
        statusFilter.valueProperty().addListener((obs, old, value) -> applyFilters());

        Button clear = new Button(
                "Limpar filtros",
                IconUtils.icon(Feather.X, 12)
        );
        clear.getStyleClass().add("button-outlined");
        clear.setOnAction(e -> {
            searchField.clear();
            empresaFilter.setValue(null);
            roleFilter.setValue(null);
            statusFilter.setValue("Todos");
        });

        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label resultCount = new Label();
        resultCount.getStyleClass().add("kubata-access-result-count");
        filteredUsers.addListener((javafx.collections.ListChangeListener<User>) c ->
                resultCount.setText(filteredUsers.size() + " registo(s)"));

        bar.getChildren().addAll(
                searchField, empresaFilter, roleFilter, statusFilter, clear, spacer, resultCount
        );

        return bar;
    }

    private SplitPane buildMainArea() {
        SplitPane split = new SplitPane();
        split.setDividerPositions(0.72);
        split.getStyleClass().add("kubata-access-split");

        VBox tableBox = new VBox(0);
        tableBox.getStyleClass().add("kubata-access-table-shell");

        table = new TableView<>(filteredUsers);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("Nenhum utilizador corresponde aos filtros."));
        table.getStyleClass().add("kubata-access-table");
        table.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, selected) -> updateSelection(selected));

        table.getColumns().addAll(
                columnNome(),
                columnEmail(),
                columnPerfil(),
                columnRole(),
                columnEmpresa(),
                columnMfa(),
                columnEstado(),
                columnActions()
        );

        VBox.setVgrow(table, Priority.ALWAYS);
        tableBox.getChildren().add(table);

        VBox detail = buildDetailPane();

        split.getItems().addAll(tableBox, detail);
        return split;
    }

    private TableColumn<User, String> columnNome() {
        TableColumn<User, String> col = new TableColumn<>("Utilizador");
        col.setCellValueFactory(data -> new SimpleStringProperty(
                safe(data.getValue().getNome(), data.getValue().getEmail())
        ));
        col.setPrefWidth(170);
        return col;
    }

    private TableColumn<User, String> columnEmail() {
        TableColumn<User, String> col = new TableColumn<>("Email");
        col.setCellValueFactory(data -> new SimpleStringProperty(
                safe(data.getValue().getEmail(), "—")
        ));
        col.setPrefWidth(190);
        return col;
    }

    private TableColumn<User, String> columnPerfil() {
        TableColumn<User, String> col = new TableColumn<>("Perfil(s)");
        col.setCellValueFactory(data -> new SimpleStringProperty(profileText(data.getValue())));
        col.setPrefWidth(175);
        return col;
    }

    private TableColumn<User, String> columnRole() {
        TableColumn<User, String> col = new TableColumn<>("Função");
        col.setCellValueFactory(data -> new SimpleStringProperty(roleLabel(data.getValue().getRole())));
        col.setPrefWidth(120);
        return col;
    }

    private TableColumn<User, String> columnEmpresa() {
        TableColumn<User, String> col = new TableColumn<>("Empresa");
        col.setCellValueFactory(data -> new SimpleStringProperty(companyName(data.getValue())));
        col.setPrefWidth(145);
        return col;
    }

    private TableColumn<User, Boolean> columnMfa() {
        TableColumn<User, Boolean> col = new TableColumn<>("MFA");
        col.setCellValueFactory(data -> new SimpleBooleanProperty(data.getValue().isMfaEnabled()));
        col.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                if (empty) return;
                Label badge = new Label(Boolean.TRUE.equals(item) ? "ACTIVO" : "—");
                badge.getStyleClass().add(
                        Boolean.TRUE.equals(item)
                                ? "kubata-access-badge-success"
                                : "kubata-access-badge-muted"
                );
                setGraphic(badge);
            }
        });
        col.setPrefWidth(70);
        return col;
    }

    private TableColumn<User, String> columnEstado() {
        TableColumn<User, String> col = new TableColumn<>("Estado");
        col.setCellValueFactory(data -> new SimpleStringProperty(statusText(data.getValue())));
        col.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                if (empty) return;
                Label badge = new Label(item);
                badge.getStyleClass().add(
                        item.startsWith("ACTIVO")
                                ? "kubata-access-badge-success"
                                : item.startsWith("BLOQUEADO")
                                ? "kubata-access-badge-warning"
                                : "kubata-access-badge-muted"
                );
                setGraphic(badge);
            }
        });
        col.setPrefWidth(105);
        return col;
    }

    private TableColumn<User, Void> columnActions() {
        TableColumn<User, Void> col = new TableColumn<>("Ações");
        col.setPrefWidth(150);
        col.setCellFactory(tc -> new TableCell<>() {
            private final Button access = new Button("", IconUtils.icon(Feather.USER_CHECK, 13));
            private final Button policy = new Button("", IconUtils.icon(Feather.SHIELD, 13));
            private final Button edit = new Button("", IconUtils.icon(Feather.EDIT_2, 13));
            private final HBox box = new HBox(4, access, policy, edit);

            {
                access.getStyleClass().add("button-outlined");
                policy.getStyleClass().add("button-outlined");
                edit.getStyleClass().add("button-outlined");

                access.setTooltip(new Tooltip("Atribuir perfis de acesso"));
                policy.setTooltip(new Tooltip("Abrir política individual de segurança"));
                edit.setTooltip(new Tooltip("Editar utilizador"));

                access.setOnAction(e -> {
                    User row = getTableView().getItems().get(getIndex());
                    openProfileAssignment(row);
                });
                policy.setOnAction(e -> {
                    User row = getTableView().getItems().get(getIndex());
                    perfilSegurancaUtilizadorView.open(row);
                });
                edit.setOnAction(e -> {
                    User row = getTableView().getItems().get(getIndex());
                    utilizadoresView.showUserDialog(row);
                });

                box.setAlignment(Pos.CENTER);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                    return;
                }
                setGraphic(box);
            }
        });
        return col;
    }

    private VBox buildDetailPane() {
        VBox pane = new VBox(11);
        pane.setPadding(new Insets(16));
        pane.getStyleClass().add("kubata-access-detail");

        HBox identity = new HBox(10);
        identity.setAlignment(Pos.CENTER_LEFT);

        Label icon = new Label("", IconUtils.icon(Feather.USER, 20));
        icon.getStyleClass().add("kubata-access-detail-icon");

        VBox title = new VBox(2);
        detailName = new Label("Nenhum utilizador seleccionado");
        detailName.getStyleClass().add("kubata-access-detail-name");
        detailEmail = new Label("Seleccione uma linha para gerir o acesso.");
        detailEmail.getStyleClass().add("kubata-access-detail-email");
        title.getChildren().addAll(detailName, detailEmail);
        identity.getChildren().addAll(icon, title);

        Separator sep = new Separator();

        detailRole = detailValue("Função", "—");
        detailEmpresa = detailValue("Empresa", "—");
        detailEstado = detailValue("Estado", "—");
        detailPerfis = detailValue("Resumo", "—");

        Label profilesTitle = new Label("Perfis atribuídos");
        profilesTitle.getStyleClass().add("kubata-access-section-title");

        detailProfilesBox = new VBox(5);
        detailProfilesBox.getStyleClass().add("kubata-access-profile-list");

        VBox profileScrollContent = new VBox(5, detailProfilesBox);
        ScrollPane profileScroll = new ScrollPane(profileScrollContent);
        profileScroll.setFitToWidth(true);
        profileScroll.setPrefViewportHeight(170);
        profileScroll.setMaxHeight(210);
        profileScroll.getStyleClass().add("kubata-access-scroll");

        Label hint = new Label(
                "O RBAC define o acesso funcional. A política individual adiciona restrições de MFA, horário, IP, módulos e limites."
        );
        hint.setWrapText(true);
        hint.getStyleClass().add("kubata-access-hint");

        btnEditAccess = detailButton(
                "Atribuir perfis",
                Feather.USER_CHECK,
                "button-primary"
        );
        btnSecurityProfile = detailButton(
                "Política individual",
                Feather.SHIELD,
                "button-outlined"
        );

        btnEditAccess.setOnAction(e -> {
            User selected = selectedUser();
            if (selected != null) {
                openProfileAssignment(selected);
            }
        });
        btnSecurityProfile.setOnAction(e -> {
            User selected = selectedUser();
            if (selected != null) {
                perfilSegurancaUtilizadorView.open(selected);
            }
        });

        HBox actions = new HBox(7, btnEditAccess, btnSecurityProfile);
        actions.setAlignment(Pos.CENTER_LEFT);

        pane.getChildren().addAll(
                identity,
                sep,
                detailRole,
                detailEmpresa,
                detailEstado,
                detailPerfis,
                profilesTitle,
                profileScroll,
                hint,
                actions
        );

        updateSelection(null);
        return pane;
    }

    private Label detailValue(String label, String initial) {
        Label value = new Label(initial);
        value.setWrapText(true);
        value.getStyleClass().add("kubata-access-detail-value");

        Label caption = new Label(label.toUpperCase(Locale.ROOT));
        caption.getStyleClass().add("kubata-access-detail-label");

        VBox box = new VBox(2, caption, value);
        box.getStyleClass().add("kubata-access-detail-row");
        return value;
    }

    private Button detailButton(String text, Feather icon, String styleClass) {
        Button button = new Button(text, IconUtils.icon(icon, 12));
        button.getStyleClass().add(styleClass);
        return button;
    }

    private HBox buildStatusBar() {
        HBox bar = new HBox(10);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(7, 14, 7, 14));
        bar.getStyleClass().add("kubata-access-statusbar");

        statusLabel = new Label("A carregar...");
        statusLabel.getStyleClass().add("kubata-access-status-text");

        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label helper = new Label(
                "Gestão central de acessos · perfis RBAC + política individual"
        );
        helper.getStyleClass().add("kubata-access-status-hint");

        bar.getChildren().addAll(statusLabel, spacer, helper);
        return bar;
    }

    private void loadData() {
        if (table != null) {
            table.setDisable(true);
        }
        statusLabel.setText("A carregar utilizadores e perfis...");

        Task<DataSet> task = new Task<>() {
            @Override
            protected DataSet call() {
                User actor = sessionManager.getUser();
                List<User> loadedUsers = userAdministrationService.listar(actor);
                List<PerfilAcesso> loadedProfiles = perfilRepository.findAllWithEmpresa();
                return new DataSet(loadedUsers, loadedProfiles);
            }
        };

        task.setOnSucceeded(e -> {
            DataSet result = task.getValue();
            users.setAll(result.users());
            perfis.setAll(result.profiles());
            refreshFilters(result.users());
            applyFilters();
            updateKpis();
            table.setDisable(false);
            statusLabel.setText(
                    users.size() + " utilizador(es) · "
                            + perfis.size() + " perfil(is) de acesso carregado(s)."
            );
        });

        task.setOnFailed(e -> {
            table.setDisable(false);
            Throwable ex = task.getException();
            statusLabel.setText(
                    "Não foi possível carregar a gestão de acesso: "
                            + safe(ex == null ? null : ex.getMessage(), "erro desconhecido")
            );
            modalManager.showErrorModal(
                    "Gestão de Acesso",
                    "Não foi possível carregar os dados de acesso.",
                    ex instanceof Exception ? (Exception) ex : new RuntimeException(ex)
            );
        });

        Thread thread = new Thread(task, "kubata-admin-access");
        thread.setDaemon(true);
        thread.start();
    }

    private void refreshFilters(List<User> currentUsers) {
        Empresa selectedEmpresa = empresaFilter.getValue();
        roleFilter.getItems().setAll(Role.values());

        List<Empresa> empresas = currentUsers.stream()
                .map(User::getEmpresa)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(Empresa::getId, e -> e, (a, b) -> a),
                        map -> map.values().stream()
                                .sorted(Comparator.comparing(e -> safe(e.getNome(), "")))
                                .toList()
                ));

        empresaFilter.getItems().setAll(empresas);
        empresaFilter.setValue(
                selectedEmpresa != null && empresas.stream()
                        .anyMatch(e -> e.getId().equals(selectedEmpresa.getId()))
                        ? selectedEmpresa
                        : null
        );
    }

    private void applyFilters() {
        String query = normalize(searchField == null ? null : searchField.getText());
        Role role = roleFilter == null ? null : roleFilter.getValue();
        String status = statusFilter == null ? "Todos" : statusFilter.getValue();
        Empresa empresa = empresaFilter == null ? null : empresaFilter.getValue();

        List<User> result = users.stream()
                .filter(user -> query.isBlank()
                        || contains(user.getNome(), query)
                        || contains(user.getEmail(), query)
                        || contains(user.getCodigo(), query))
                .filter(user -> role == null || user.getRole() == role)
                .filter(user -> empresa == null
                        || (user.getEmpresa() != null && user.getEmpresa().getId().equals(empresa.getId())))
                .filter(user -> matchesStatus(user, status))
                .toList();

        filteredUsers.setAll(result);

        if (table != null) {
            table.getSelectionModel().clearSelection();
        }
    }

    private boolean matchesStatus(User user, String status) {
        return switch (status == null ? "Todos" : status) {
            case "Activos" -> Boolean.TRUE.equals(user.getActive());
            case "Inactivos" -> !Boolean.TRUE.equals(user.getActive());
            case "Sem perfil" -> user.getPerfis() == null || user.getPerfis().isEmpty();
            case "Com perfil" -> user.getPerfis() != null && !user.getPerfis().isEmpty();
            case "MFA activo" -> user.isMfaEnabled();
            case "MFA inactivo" -> !user.isMfaEnabled();
            default -> true;
        };
    }

    private void updateKpis() {
        int active = (int) users.stream().filter(u -> Boolean.TRUE.equals(u.getActive())).count();
        int withoutProfile = (int) users.stream()
                .filter(u -> u.getPerfis() == null || u.getPerfis().isEmpty())
                .count();
        int mfa = (int) users.stream().filter(User::isMfaEnabled).count();

        totalValue.setText(Integer.toString(users.size()));
        activeValue.setText(Integer.toString(active));
        noProfileValue.setText(Integer.toString(withoutProfile));
        mfaValue.setText(Integer.toString(mfa));
    }

    private void updateSelection(User selected) {
        boolean has = selected != null;

        detailName.setText(has ? safe(selected.getNome(), "Utilizador") : "Nenhum utilizador seleccionado");
        detailEmail.setText(has
                ? safe(selected.getEmail(), "Sem email")
                : "Seleccione uma linha para gerir o acesso.");

        detailRole.setText(has ? roleLabel(selected.getRole()) : "—");
        detailEmpresa.setText(has ? companyName(selected) : "—");
        detailEstado.setText(has ? statusText(selected) : "—");

        List<PerfilAcesso> assigned = has && selected.getPerfis() != null
                ? selected.getPerfis().stream()
                    .sorted(Comparator.comparing(p -> safe(p.getCodigo(), "")))
                    .toList()
                : List.of();

        detailPerfis.setText(has
                ? assigned.size() + " perfil(is) atribuído(s)"
                : "—");

        detailProfilesBox.getChildren().clear();
        if (assigned.isEmpty()) {
            Label none = new Label("Nenhum perfil atribuído.");
            none.getStyleClass().add("kubata-access-profile-empty");
            detailProfilesBox.getChildren().add(none);
        } else {
            for (PerfilAcesso perfil : assigned) {
                Label item = new Label(
                        safe(perfil.getCodigo(), "PERFIL")
                                + " · "
                                + safe(perfil.getDescricao(), "Sem descrição")
                                + (perfil.getEmpresa() == null
                                ? " · GLOBAL"
                                : " · " + companyName(perfil.getEmpresa()))
                );
                item.setWrapText(true);
                item.getStyleClass().add("kubata-access-profile-item");
                detailProfilesBox.getChildren().add(item);
            }
        }

        btnEditAccess.setDisable(!has);
        btnSecurityProfile.setDisable(!has);
    }

    private void openProfileAssignment(User selected) {
        if (selected == null) return;

        try {
            User current = sessionManager.getUser();
            User managed = userAdministrationService.carregarParaEdicao(current, selected.getId());

            if (managed.isSuperadmin() && (current == null || !current.isSuperadmin())) {
                throw new SecurityException(
                        "Só um Superadministrador pode alterar o acesso de outro Superadministrador."
                );
            }

            List<PerfilAcesso> available = perfis.stream()
                    .filter(p -> Boolean.TRUE.equals(p.getActivo()))
                    .filter(p -> p.getEmpresa() == null
                            || (managed.getEmpresa() != null
                            && p.getEmpresa().getId().equals(managed.getEmpresa().getId())))
                    .sorted(Comparator
                            .comparing((PerfilAcesso p) -> p.getEmpresa() != null)
                            .thenComparing(p -> safe(p.getCodigo(), "")))
                    .toList();

            ListView<PerfilAcesso> list = new ListView<>();
            list.getItems().setAll(available);
            list.setPrefHeight(390);
            list.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
            list.setCellFactory(view -> new ListCell<>() {
                @Override
                protected void updateItem(PerfilAcesso item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        return;
                    }
                    setText(
                            safe(item.getCodigo(), "PERFIL")
                                    + " — " + safe(item.getDescricao(), "Sem descrição")
                                    + (item.getEmpresa() == null
                                    ? " · GLOBAL"
                                    : " · EMPRESA")
                    );
                }
            });

            Set<Long> assignedIds = managed.getPerfis() == null
                    ? Set.of()
                    : managed.getPerfis().stream()
                        .filter(p -> p.getId() != null)
                        .map(PerfilAcesso::getId)
                        .collect(Collectors.toSet());

            for (int i = 0; i < available.size(); i++) {
                PerfilAcesso perfil = available.get(i);
                if (perfil.getId() != null && assignedIds.contains(perfil.getId())) {
                    list.getSelectionModel().select(i);
                }
            }

            Label heading = new Label(
                    "Utilizador: " + safe(managed.getNome(), managed.getEmail())
                            + "\nEmpresa: " + companyName(managed)
            );
            heading.setWrapText(true);
            heading.getStyleClass().add("kubata-access-modal-heading");

            Label hint = new Label(
                    "Seleccione os perfis que devem ser atribuídos. Perfis inactivos ou de outra empresa não aparecem."
            );
            hint.setWrapText(true);
            hint.getStyleClass().add("kubata-access-hint");

            Label count = new Label();
            count.getStyleClass().add("kubata-access-result-count");
            Runnable updateCount = () -> count.setText(
                    list.getSelectionModel().getSelectedItems().size() + " perfil(is) seleccionado(s)"
            );
            list.getSelectionModel().getSelectedItems().addListener(
                    (javafx.collections.ListChangeListener<PerfilAcesso>) c -> updateCount.run()
            );
            updateCount.run();

            Button cancel = new Button("Cancelar", IconUtils.icon(Feather.X, 12));
            cancel.getStyleClass().add("button-outlined");

            Button save = new Button(
                    "Guardar acesso",
                    IconUtils.icon(Feather.SAVE, 12)
            );
            save.getStyleClass().add("button-primary");

            HBox actions = new HBox(7, new Pane(), cancel, save);
            HBox.setHgrow(actions.getChildren().get(0), Priority.ALWAYS);
            actions.setAlignment(Pos.CENTER_RIGHT);

            cancel.setOnAction(e -> modalManager.hideModal());
            save.setOnAction(e -> {
                try {
                    Set<PerfilAcesso> selectedProfiles = new HashSet<>(
                            list.getSelectionModel().getSelectedItems()
                    );

                    userAdministrationService.actualizarPerfisAcesso(
                            current,
                            managed.getId(),
                            selectedProfiles,
                            "127.0.0.1"
                    );

                    modalManager.hideModal();
                    loadData();
                    statusLabel.setText("Acesso actualizado para " + safe(managed.getNome(), managed.getEmail()) + ".");
                } catch (Exception ex) {
                    modalManager.showErrorModal(
                            "Gestão de Acesso",
                            "Não foi possível guardar os perfis atribuídos.",
                            ex
                    );
                }
            });

            VBox content = new VBox(10, heading, hint, list, count, new Separator(), actions);
            content.setPadding(new Insets(4));

            modalManager.showModal(
                    content,
                    new ModalManager.ModalConfig()
                            .title("Atribuição de perfis")
                            .subtitle("Acesso funcional do utilizador")
                            .icon(Feather.USER_CHECK)
                            .tone(ModalManager.ModalTone.INFO)
                            .size(650, 600)
                            .minSize(560, 520)
                            .maximizable(true)
            );
        } catch (Exception ex) {
            modalManager.showErrorModal(
                    "Gestão de Acesso",
                    "Não foi possível abrir a atribuição de perfis.",
                    ex
            );
        }
    }

    private String profileText(User user) {
        if (user == null || user.getPerfis() == null || user.getPerfis().isEmpty()) {
            return "—";
        }
        return user.getPerfis().stream()
                .sorted(Comparator.comparing(p -> safe(p.getCodigo(), "")))
                .map(p -> safe(p.getCodigo(), "PERFIL"))
                .collect(Collectors.joining(", "));
    }

    private String statusText(User user) {
        if (user == null) return "—";
        if (!Boolean.TRUE.equals(user.getActive())) return "INACTIVO";
        if (user.getLockoutEnd() != null) return "BLOQUEADO";
        return "ACTIVO";
    }

    private String roleLabel(Role role) {
        if (role == null) return "—";
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
            case ESTOQUE -> "Responsável de Estoque";
            case COMPRAS -> "Compras";
            case LOGISTICA -> "Logística";
            case AUDITOR -> "Auditor";
            case SUPORTE_TI -> "Suporte TI";
            case RESPONSAVEL_FISCAL_AO -> "Responsável Fiscal AO";
            case VISITANTE -> "Visitante";
        };
    }

    private String companyName(User user) {
        return user == null ? "—" : companyName(user.getEmpresa());
    }

    private String companyName(Empresa empresa) {
        return empresa == null
                ? "—"
                : safe(empresa.getNome(), "Empresa");
    }

    private boolean contains(String value, String query) {
        return normalize(value).contains(query);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private User selectedUser() {
        return table == null ? null : table.getSelectionModel().getSelectedItem();
    }

    private record DataSet(List<User> users, List<PerfilAcesso> profiles) {
    }
}

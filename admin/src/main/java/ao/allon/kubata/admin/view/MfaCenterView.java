package ao.allon.kubata.admin.view;

import ao.allon.kubata.admin.service.SessionManager;
import ao.allon.kubata.admin.ui.modal.ModalManager;
import ao.allon.kubata.admin.ui.util.IconUtils;
import ao.allon.kubata.core.domain.MfaPolicy;
import ao.allon.kubata.core.domain.User;
import ao.allon.kubata.core.repository.MfaPolicyRepository;
import ao.allon.kubata.core.repository.UserRepository;
import ao.allon.kubata.core.service.MfaService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.stereotype.Component;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.util.List;
import java.util.Locale;

@Component
public class MfaCenterView extends VBox {

    private final UserRepository userRepository;
    private final MfaPolicyRepository policyRepository;
    private final MfaService mfaService;
    private final SessionManager sessionManager;
    private final ModalManager modalManager;

    private final ObservableList<User> users = FXCollections.observableArrayList();

    private TableView<User> table;
    private TextField searchField;
    private ComboBox<String> stateFilter;
    private Label statusLabel;

    private Label totalValue;
    private Label activeValue;
    private Label enabledValue;
    private Label requiredValue;
    private Label pendingValue;
    private Label lowRecoveryValue;

    private Label detailUser;
    private Label detailEmail;
    private Label detailState;
    private Label detailRecovery;
    private Label detailRequired;

    private CheckBox globalRequired;
    private CheckBox globalAllowDisable;
    private CheckBox globalAllowRecovery;
    private Spinner<Integer> globalRecoveryCount;
    private Spinner<Integer> globalGraceDays;
    private TextField globalIssuer;

    public MfaCenterView(
            UserRepository userRepository,
            MfaPolicyRepository policyRepository,
            MfaService mfaService,
            SessionManager sessionManager,
            ModalManager modalManager) {
        this.userRepository = userRepository;
        this.policyRepository = policyRepository;
        this.mfaService = mfaService;
        this.sessionManager = sessionManager;
        this.modalManager = modalManager;

        buildUI();
        load();
    }

    private void buildUI() {
        setSpacing(0);
        setPadding(Insets.EMPTY);
        getStyleClass().add("kubata-mfa-center-page");

        getChildren().addAll(
                buildHeader(),
                buildToolbar(),
                buildWorkspace(),
                buildStatusBar()
        );

        VBox.setVgrow(getChildren().get(2), Priority.ALWAYS);
    }

    private Node buildHeader() {
        VBox header = new VBox(12);
        header.setPadding(new Insets(18, 22, 14, 22));
        header.getStyleClass().add("kubata-mfa-header");

        HBox titleLine = new HBox(12);
        titleLine.setAlignment(Pos.CENTER_LEFT);

        Label icon = new Label("", IconUtils.icon(Feather.SHIELD, 24));
        icon.getStyleClass().add("kubata-mfa-title-icon");

        VBox titles = new VBox(2);
        Label title = new Label("MFA Center");
        title.getStyleClass().add("kubata-mfa-title");

        Label subtitle = new Label(
                "Centro de administração TOTP, recovery codes, forcing, política e conformidade de autenticação."
        );
        subtitle.setWrapText(true);
        subtitle.getStyleClass().add("kubata-mfa-subtitle");

        titles.getChildren().addAll(title, subtitle);

        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button refresh = new Button(
                "Actualizar",
                IconUtils.icon(Feather.REFRESH_CW, 13)
        );
        refresh.getStyleClass().add("button-outlined");
        refresh.setOnAction(e -> load());

        titleLine.getChildren().addAll(icon, titles, spacer, refresh);

        totalValue = new Label("0");
        activeValue = new Label("0");
        enabledValue = new Label("0");
        requiredValue = new Label("0");
        pendingValue = new Label("0");
        lowRecoveryValue = new Label("0");

        HBox kpis = new HBox(9,
                kpi("UTILIZADORES", Feather.USERS, totalValue),
                kpi("ACTIVOS", Feather.CHECK_CIRCLE, activeValue),
                kpi("MFA ACTIVO", Feather.SHIELD, enabledValue),
                kpi("MFA OBRIGATÓRIO", Feather.LOCK, requiredValue),
                kpi("PENDENTES", Feather.ALERT_TRIANGLE, pendingValue),
                kpi("RECOVERY BAIXO", Feather.KEY, lowRecoveryValue)
        );

        header.getChildren().addAll(titleLine, kpis);
        return header;
    }

    private VBox kpi(String title, Feather icon, Label value) {
        VBox card = new VBox(2);
        card.setPadding(new Insets(9, 12, 9, 12));
        card.setMinWidth(150);
        card.getStyleClass().add("kubata-mfa-kpi");

        HBox top = new HBox(7);
        top.setAlignment(Pos.CENTER_LEFT);

        Label iconLabel = new Label("", IconUtils.icon(icon, 13));
        iconLabel.getStyleClass().add("kubata-mfa-kpi-icon");

        Label caption = new Label(title);
        caption.getStyleClass().add("kubata-mfa-kpi-title");

        top.getChildren().addAll(iconLabel, caption);

        value.getStyleClass().add("kubata-mfa-kpi-value");
        card.getChildren().addAll(top, value);
        return card;
    }

    private HBox buildToolbar() {
        HBox bar = new HBox(8);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(9, 14, 9, 14));
        bar.getStyleClass().add("kubata-mfa-toolbar");

        searchField = new TextField();
        searchField.setPromptText("Pesquisar nome, email ou código...");
        searchField.setPrefWidth(330);

        stateFilter = new ComboBox<>(FXCollections.observableArrayList(
                "Todos",
                "MFA activo",
                "MFA inactivo",
                "MFA obrigatório",
                "MFA obrigatório pendente"
        ));
        stateFilter.setValue("Todos");
        stateFilter.setPrefWidth(190);

        Button clear = new Button("Limpar", IconUtils.icon(Feather.X, 12));
        clear.getStyleClass().add("button-outlined");
        clear.setOnAction(e -> {
            searchField.clear();
            stateFilter.setValue("Todos");
            applyFilters();
        });

        Button globalPolicy = new Button(
                "Política Global",
                IconUtils.icon(Feather.SETTINGS, 12)
        );
        globalPolicy.getStyleClass().add("button-outlined");
        globalPolicy.setOnAction(e -> showGlobalPolicyModal());

        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label hint = new Label("Operações sensíveis ficam auditadas.");
        hint.getStyleClass().add("kubata-mfa-toolbar-hint");

        searchField.textProperty().addListener((obs, old, value) -> applyFilters());
        stateFilter.valueProperty().addListener((obs, old, value) -> applyFilters());

        bar.getChildren().addAll(
                searchField,
                stateFilter,
                clear,
                globalPolicy,
                spacer,
                hint
        );
        return bar;
    }

    private SplitPane buildWorkspace() {
        table = new TableView<>(users);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("Nenhum utilizador corresponde aos filtros."));
        table.getStyleClass().add("kubata-mfa-table");
        table.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, selected) -> updateDetails(selected));

        TableColumn<User, String> code = new TableColumn<>("Código");
        code.setCellValueFactory(new PropertyValueFactory<>("codigo"));
        code.setPrefWidth(100);

        TableColumn<User, String> name = new TableColumn<>("Utilizador");
        name.setCellValueFactory(new PropertyValueFactory<>("nome"));
        name.setPrefWidth(180);

        TableColumn<User, String> email = new TableColumn<>("Email");
        email.setCellValueFactory(new PropertyValueFactory<>("email"));
        email.setPrefWidth(210);

        TableColumn<User, String> mfa = new TableColumn<>("MFA");
        mfa.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(
                cell.getValue().isMfaEnabled() ? "ACTIVO" : "INACTIVO"
        ));
        mfa.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                if (empty) {
                    setGraphic(null);
                    return;
                }
                Label badge = new Label(item);
                badge.getStyleClass().add(
                        "ACTIVO".equals(item)
                                ? "kubata-mfa-badge-success"
                                : "kubata-mfa-badge-muted"
                );
                setGraphic(badge);
            }
        });
        mfa.setPrefWidth(90);

        TableColumn<User, String> required = new TableColumn<>("Política");
        required.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(
                mfaService.isMfaRequired(cell.getValue()) ? "OBRIGATÓRIO" : "OPCIONAL"
        ));
        required.setPrefWidth(110);

        TableColumn<User, String> recovery = new TableColumn<>("Recovery");
        recovery.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(
                String.valueOf(mfaService.countRecoveryCodes(cell.getValue()))
        ));
        recovery.setPrefWidth(80);

        TableColumn<User, String> state = new TableColumn<>("Estado");
        state.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(
                rowState(cell.getValue())
        ));
        state.setPrefWidth(140);

        TableColumn<User, Void> actions = new TableColumn<>("Acções");
        actions.setPrefWidth(215);
        actions.setCellFactory(tc -> new TableCell<>() {
            private final Button activate = iconButton(Feather.SHIELD, "Activar MFA");
            private final Button disable = iconButton(Feather.SHIELD_OFF, "Desactivar MFA");
            private final Button recovery = iconButton(Feather.KEY, "Regenerar recovery codes");
            private final Button force = iconButton(Feather.LOCK, "Forçar MFA");
            private final HBox box = new HBox(4, activate, disable, recovery, force);

            {
                activate.setOnAction(e -> {
                    User u = getTableView().getItems().get(getIndex());
                    beginActivation(u);
                });
                disable.setOnAction(e -> {
                    User u = getTableView().getItems().get(getIndex());
                    disableMfa(u);
                });
                recovery.setOnAction(e -> {
                    User u = getTableView().getItems().get(getIndex());
                    regenerateRecovery(u);
                });
                force.setOnAction(e -> {
                    User u = getTableView().getItems().get(getIndex());
                    forceMfa(u);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                    return;
                }
                User u = getTableView().getItems().get(getIndex());
                activate.setDisable(u.isMfaEnabled());
                disable.setDisable(!u.isMfaEnabled());
                recovery.setDisable(!u.isMfaEnabled() || mfaService.countRecoveryCodes(u) == 0);
                force.setDisable(mfaService.isMfaRequired(u));
                setGraphic(box);
            }
        });

        table.getColumns().addAll(code, name, email, mfa, required, recovery, state, actions);

        VBox tableShell = new VBox(table);
        tableShell.getStyleClass().add("kubata-mfa-table-shell");
        VBox.setVgrow(table, Priority.ALWAYS);

        VBox details = buildDetails();
        SplitPane split = new SplitPane(tableShell, details);
        split.setDividerPositions(0.70);
        split.getStyleClass().add("kubata-mfa-split");

        return split;
    }

    private Button iconButton(Feather icon, String tooltip) {
        Button button = new Button("", IconUtils.icon(icon, 12));
        button.getStyleClass().add("button-outlined");
        button.setTooltip(new Tooltip(tooltip));
        return button;
    }

    private VBox buildDetails() {
        VBox pane = new VBox(12);
        pane.setPadding(new Insets(16));
        pane.getStyleClass().add("kubata-mfa-details");

        HBox title = new HBox(9);
        title.setAlignment(Pos.CENTER_LEFT);

        Label icon = new Label("", IconUtils.icon(Feather.SHIELD, 19));
        icon.getStyleClass().add("kubata-mfa-details-icon");

        VBox titleBox = new VBox(2);
        detailUser = new Label("Nenhum utilizador seleccionado");
        detailUser.getStyleClass().add("kubata-mfa-details-title");

        detailEmail = new Label("Seleccione uma conta para gerir o MFA.");
        detailEmail.getStyleClass().add("kubata-mfa-details-subtitle");

        titleBox.getChildren().addAll(detailUser, detailEmail);
        title.getChildren().addAll(icon, titleBox);

        detailState = detailValue("Estado", "—");
        detailRequired = detailValue("Política", "—");
        detailRecovery = detailValue("Recovery codes", "—");

        Button activate = new Button(
                "Activar / configurar",
                IconUtils.icon(Feather.SHIELD, 12)
        );
        activate.getStyleClass().add("button-primary");
        activate.setOnAction(e -> {
            User selected = selectedUser();
            if (selected != null) beginActivation(selected);
        });

        Button disable = new Button(
                "Desactivar",
                IconUtils.icon(Feather.SHIELD_OFF, 12)
        );
        disable.getStyleClass().add("button-outlined");
        disable.setOnAction(e -> {
            User selected = selectedUser();
            if (selected != null) disableMfa(selected);
        });

        Button recovery = new Button(
                "Regenerar recovery",
                IconUtils.icon(Feather.KEY, 12)
        );
        recovery.getStyleClass().add("button-outlined");
        recovery.setOnAction(e -> {
            User selected = selectedUser();
            if (selected != null) regenerateRecovery(selected);
        });

        Button force = new Button(
                "Forçar MFA",
                IconUtils.icon(Feather.LOCK, 12)
        );
        force.getStyleClass().add("button-outlined");
        force.setOnAction(e -> {
            User selected = selectedUser();
            if (selected != null) forceMfa(selected);
        });

        VBox note = new VBox(6);
        note.getStyleClass().add("kubata-mfa-security-card");

        Label noteTitle = new Label("Controlo de segurança");
        noteTitle.getStyleClass().add("kubata-mfa-security-title");

        Label noteText = new Label(
                "O forcing bloqueia o acesso normal enquanto o segundo factor não estiver activo. "
                        + "Recovery codes são de utilização única."
        );
        noteText.setWrapText(true);
        noteText.getStyleClass().add("kubata-mfa-security-text");

        note.getChildren().addAll(noteTitle, noteText);

        HBox row1 = new HBox(7, activate, disable);
        HBox row2 = new HBox(7, recovery, force);

        pane.getChildren().addAll(
                title,
                new Separator(),
                detailState,
                detailRequired,
                detailRecovery,
                new Label("Acções"),
                row1,
                row2,
                note
        );

        updateDetails(null);
        return pane;
    }

    private Label detailValue(String title, String value) {
        Label caption = new Label(title.toUpperCase(Locale.ROOT));
        caption.getStyleClass().add("kubata-mfa-detail-label");

        Label data = new Label(value);
        data.setWrapText(true);
        data.getStyleClass().add("kubata-mfa-detail-value");

        VBox box = new VBox(2, caption, data);
        box.getStyleClass().add("kubata-mfa-detail-row");
        return data;
    }

    private HBox buildStatusBar() {
        HBox bar = new HBox(10);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(7, 14, 7, 14));
        bar.getStyleClass().add("kubata-mfa-statusbar");

        statusLabel = new Label("A carregar...");
        statusLabel.getStyleClass().add("kubata-mfa-status-text");

        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label helper = new Label(
                "MFA Center · TOTP + recovery codes + política central"
        );
        helper.getStyleClass().add("kubata-mfa-status-hint");

        bar.getChildren().addAll(statusLabel, spacer, helper);
        return bar;
    }

    private void load() {
        try {
            users.setAll(userRepository.findAll());
            updateIndicators();
            applyFilters();
            loadGlobalPolicy();
            statusLabel.setText(users.size() + " utilizador(es) · centro MFA actualizado");
        } catch (Exception ex) {
            statusLabel.setText("Falha ao carregar o MFA Center.");
            modalManager.showErrorModal(
                    "MFA Center",
                    "Não foi possível carregar os dados do centro MFA.",
                    ex
            );
        }
    }

    private void updateIndicators() {
        MfaService.MfaIndicators indicators =
                mfaService.getIndicators(sessionManager.getUser());

        totalValue.setText(String.valueOf(indicators.totalUsers()));
        activeValue.setText(String.valueOf(indicators.activeUsers()));
        enabledValue.setText(String.valueOf(indicators.mfaEnabledUsers()));
        requiredValue.setText(String.valueOf(indicators.requiredUsers()));
        pendingValue.setText(String.valueOf(indicators.requiredPendingUsers()));
        lowRecoveryValue.setText(String.valueOf(indicators.lowRecoveryUsers()));
    }

    private void applyFilters() {
        if (table == null) return;

        String search = searchField == null || searchField.getText() == null
                ? ""
                : searchField.getText().trim().toLowerCase(Locale.ROOT);

        String filter = stateFilter == null ? "Todos" : stateFilter.getValue();

        ObservableList<User> filtered = FXCollections.observableArrayList();

        for (User user : users) {
            String haystack = safe(user.getCodigo()) + " "
                    + safe(user.getNome()) + " "
                    + safe(user.getEmail());

            if (!search.isBlank() && !haystack.toLowerCase(Locale.ROOT).contains(search)) {
                continue;
            }

            boolean required = mfaService.isMfaRequired(user);
            boolean enabled = user.isMfaEnabled();

            if ("MFA activo".equals(filter) && !enabled) continue;
            if ("MFA inactivo".equals(filter) && enabled) continue;
            if ("MFA obrigatório".equals(filter) && !required) continue;
            if ("MFA obrigatório pendente".equals(filter) && (!required || enabled)) continue;

            filtered.add(user);
        }

        table.setItems(filtered);
        updateDetails(table.getSelectionModel().getSelectedItem());
    }

    private void updateDetails(User user) {
        if (user == null) {
            detailUser.setText("Nenhum utilizador seleccionado");
            detailEmail.setText("Seleccione uma conta para gerir o MFA.");
            detailState.setText("—");
            detailRequired.setText("—");
            detailRecovery.setText("—");
            return;
        }

        detailUser.setText(safe(user.getNome(), "Utilizador"));
        detailEmail.setText(safe(user.getEmail(), "—"));
        detailState.setText(rowState(user));
        detailRequired.setText(
                mfaService.isMfaRequired(user) ? "MFA obrigatório" : "MFA opcional"
        );
        detailRecovery.setText(
                mfaService.countRecoveryCodes(user) + " código(s) disponível(eis)"
        );
    }

    private User selectedUser() {
        return table == null ? null : table.getSelectionModel().getSelectedItem();
    }

    private String rowState(User user) {
        if (mfaService.isMfaRequired(user) && !user.isMfaEnabled()) {
            return "PENDENTE DE ACTIVAÇÃO";
        }
        if (user.isMfaEnabled() && mfaService.countRecoveryCodes(user) <= 2) {
            return "RECOVERY BAIXO";
        }
        if (user.isMfaEnabled()) {
            return "PROTEGIDO";
        }
        return "SEM MFA";
    }

    private void beginActivation(User user) {
        if (user == null || user.getId() == null) return;

        try {
            String secret = mfaService.adminPrepareActivation(
                    sessionManager.getUser(),
                    user.getId(),
                    sourceIp()
            );

            String uri = mfaService.buildProvisioningUri(user);

            ImageView qr = new ImageView(createQrImage(uri, 210));
            qr.setFitWidth(210);
            qr.setFitHeight(210);
            qr.setPreserveRatio(true);

            Label instructions = new Label(
                    "1. Abra o autenticador.\n"
                            + "2. Leia o QR Code.\n"
                            + "3. Introduza o código TOTP de 6 dígitos."
            );
            instructions.setWrapText(true);
            instructions.getStyleClass().add("kubata-mfa-security-text");

            TextField secretField = new TextField(secret);
            secretField.setEditable(false);

            TextField codeField = new TextField();
            codeField.setPromptText("Código TOTP de 6 dígitos");
            codeField.setTextFormatter(new TextFormatter<String>(change ->
                    change.getControlNewText().matches("\\d{0,6}")
                            ? change
                            : null
            ));

            Label error = new Label();
            error.setManaged(false);
            error.setVisible(false);

            Button cancel = new Button(
                    "Cancelar",
                    IconUtils.icon(Feather.X, 12)
            );
            cancel.getStyleClass().add("button-outlined");
            cancel.setOnAction(e -> modalManager.hideModal());

            Button confirm = new Button(
                    "Confirmar MFA",
                    IconUtils.icon(Feather.CHECK, 12)
            );
            confirm.getStyleClass().add("button-primary");
            confirm.setOnAction(e -> {
                if (codeField.getText() == null || codeField.getText().length() != 6) {
                    error.setText("Introduza um código TOTP válido de 6 dígitos.");
                    error.setManaged(true);
                    error.setVisible(true);
                    return;
                }

                try {
                    MfaService.ActivationResult result =
                            mfaService.adminConfirmActivation(
                                    sessionManager.getUser(),
                                    user.getId(),
                                    codeField.getText(),
                                    sourceIp()
                            );

                    modalManager.hideModal();
                    showRecoveryCodes(result.recoveryCodes(), "MFA activado");
                    load();
                } catch (Exception ex) {
                    error.setText(ex.getMessage() == null
                            ? "Não foi possível confirmar o MFA."
                            : ex.getMessage());
                    error.setManaged(true);
                    error.setVisible(true);
                }
            });

            VBox manual = new VBox(
                    8,
                    new Label("Chave manual"),
                    secretField,
                    new Label("Confirmação"),
                    codeField,
                    error
            );
            manual.setPrefWidth(340);

            HBox body = new HBox(
                    18,
                    new VBox(8, qr, instructions),
                    manual
            );
            body.setAlignment(Pos.TOP_LEFT);

            VBox content = new VBox(12, body, new Separator(), new HBox(8, cancel, confirm));
            content.setPadding(new Insets(4));

            modalManager.showModal(
                    content,
                    new ModalManager.ModalConfig()
                            .title("Activar MFA")
                            .subtitle(safe(user.getNome(), user.getEmail()))
                            .icon(Feather.SHIELD)
                            .tone(ModalManager.ModalTone.INFO)
                            .size(700, 470)
                            .minSize(620, 420)
                            .maximizable(false)
                            .minimizable(false)
            );
        } catch (Exception ex) {
            modalManager.showErrorModal(
                    "MFA Center",
                    "Não foi possível iniciar a activação MFA.",
                    ex
            );
        }
    }

    private void disableMfa(User user) {
        if (user == null || user.getId() == null) return;

        Label warning = new Label(
                "A operação remove a chave TOTP e todos os recovery codes desta conta."
        );
        warning.setWrapText(true);
        warning.getStyleClass().add("kubata-mfa-security-text");

        Button cancel = new Button("Cancelar", IconUtils.icon(Feather.X, 12));
        cancel.getStyleClass().add("button-outlined");
        cancel.setOnAction(e -> modalManager.hideModal());

        Button confirm = new Button(
                "Desactivar MFA",
                IconUtils.icon(Feather.SHIELD_OFF, 12)
        );
        confirm.getStyleClass().add("button-primary");
        confirm.setOnAction(e -> {
            try {
                mfaService.adminDisableMfa(
                        sessionManager.getUser(),
                        user.getId(),
                        sourceIp()
                );
                modalManager.hideModal();
                load();
            } catch (Exception ex) {
                modalManager.showErrorModal(
                        "MFA Center",
                        "Não foi possível desactivar o MFA.",
                        ex
                );
            }
        });

        VBox content = new VBox(
                12,
                new Label("Confirmar desactivação"),
                warning,
                new HBox(8, cancel, confirm)
        );
        content.setPadding(new Insets(4));

        modalManager.showModal(
                content,
                new ModalManager.ModalConfig()
                        .title("Desactivar MFA")
                        .subtitle(safe(user.getNome(), user.getEmail()))
                        .icon(Feather.SHIELD_OFF)
                        .tone(ModalManager.ModalTone.WARNING)
                        .size(500, 260)
                        .minSize(440, 230)
                        .maximizable(false)
                        .minimizable(false)
        );
    }

    private void regenerateRecovery(User user) {
        if (user == null || user.getId() == null) return;

        TextField code = new TextField();
        code.setPromptText("Código TOTP actual");
        code.setTextFormatter(new TextFormatter<String>(change ->
                change.getControlNewText().matches("\\d{0,6}")
                        ? change
                        : null
        ));

        Button cancel = new Button("Cancelar", IconUtils.icon(Feather.X, 12));
        cancel.getStyleClass().add("button-outlined");
        cancel.setOnAction(e -> modalManager.hideModal());

        Button regenerate = new Button(
                "Regenerar",
                IconUtils.icon(Feather.KEY, 12)
        );
        regenerate.getStyleClass().add("button-primary");
        regenerate.setOnAction(e -> {
            try {
                List<String> codes = mfaService.adminRegenerateRecoveryCodes(
                        sessionManager.getUser(),
                        user.getId(),
                        code.getText(),
                        sourceIp()
                );
                modalManager.hideModal();
                showRecoveryCodes(codes, "Recovery codes regenerados");
                load();
            } catch (Exception ex) {
                modalManager.showErrorModal(
                        "MFA Center",
                        "Não foi possível regenerar os recovery codes.",
                        ex
                );
            }
        });

        VBox content = new VBox(
                10,
                new Label("Introduza o TOTP actual para confirmar a regeneração."),
                code,
                new HBox(8, cancel, regenerate)
        );
        content.setPadding(new Insets(4));

        modalManager.showModal(
                content,
                new ModalManager.ModalConfig()
                        .title("Regenerar recovery codes")
                        .subtitle(safe(user.getNome(), user.getEmail()))
                        .icon(Feather.KEY)
                        .size(500, 250)
                        .minSize(440, 230)
                        .maximizable(false)
                        .minimizable(false)
        );
    }

    private void forceMfa(User user) {
        if (user == null || user.getId() == null) return;

        VBox content = new VBox(
                10,
                new Label(
                        "O forcing exige que esta conta tenha MFA activo para concluir o acesso."
                ),
                new Label(
                        "A desactivação voluntária ficará bloqueada até a política deixar de exigir MFA."
                )
        );
        content.setPadding(new Insets(4));

        Button cancel = new Button("Cancelar", IconUtils.icon(Feather.X, 12));
        cancel.getStyleClass().add("button-outlined");
        cancel.setOnAction(e -> modalManager.hideModal());

        Button confirm = new Button(
                "Forçar MFA",
                IconUtils.icon(Feather.LOCK, 12)
        );
        confirm.getStyleClass().add("button-primary");
        confirm.setOnAction(e -> {
            try {
                mfaService.adminForceMfa(
                        sessionManager.getUser(),
                        user.getId(),
                        sourceIp()
                );
                modalManager.hideModal();
                load();
            } catch (Exception ex) {
                modalManager.showErrorModal(
                        "MFA Center",
                        "Não foi possível definir MFA obrigatório.",
                        ex
                );
            }
        });

        content.getChildren().add(new HBox(8, cancel, confirm));

        modalManager.showModal(
                content,
                new ModalManager.ModalConfig()
                        .title("Forçar MFA")
                        .subtitle(safe(user.getNome(), user.getEmail()))
                        .icon(Feather.LOCK)
                        .tone(ModalManager.ModalTone.WARNING)
                        .size(520, 300)
                        .minSize(450, 260)
                        .maximizable(false)
                        .minimizable(false)
        );
    }

    private void showRecoveryCodes(List<String> codes, String title) {
        TextArea area = new TextArea(String.join(System.lineSeparator(), codes));
        area.setEditable(false);
        area.setPrefRowCount(Math.max(6, Math.min(12, codes.size())));

        Label warning = new Label(
                "Estes códigos são apresentados apenas agora. Cada código só pode ser utilizado uma vez."
        );
        warning.setWrapText(true);
        warning.getStyleClass().add("kubata-mfa-security-text");

        Button copy = new Button("Copiar", IconUtils.icon(Feather.COPY, 12));
        copy.getStyleClass().add("button-outlined");
        copy.setOnAction(e -> {
            javafx.scene.input.ClipboardContent cc =
                    new javafx.scene.input.ClipboardContent();
            cc.putString(area.getText());
            javafx.scene.input.Clipboard.getSystemClipboard().setContent(cc);
        });

        Button close = new Button("Fechar", IconUtils.icon(Feather.CHECK, 12));
        close.getStyleClass().add("button-primary");
        close.setOnAction(e -> modalManager.hideModal());

        VBox content = new VBox(
                10,
                warning,
                area,
                new HBox(8, copy, close)
        );
        content.setPadding(new Insets(4));

        modalManager.showModal(
                content,
                new ModalManager.ModalConfig()
                        .title(title)
                        .subtitle("Guardar os códigos num local seguro.")
                        .icon(Feather.KEY)
                        .tone(ModalManager.ModalTone.WARNING)
                        .size(580, 430)
                        .minSize(520, 360)
                        .maximizable(true)
                        .minimizable(false)
        );
    }

    private void showGlobalPolicyModal() {
        MfaPolicy policy = policyRepository
                .findByScopeTypeAndActiveTrue(MfaPolicy.ScopeType.GLOBAL)
                .orElseGet(() -> defaultGlobalPolicy());

        globalRequired = new CheckBox(
                "Exigir MFA para contas abrangidas pela política global"
        );
        globalRequired.setSelected(policy.isRequired());

        globalAllowDisable = new CheckBox("Permitir desactivação voluntária");
        globalAllowDisable.setSelected(policy.isAllowUserDisable());

        globalAllowRecovery = new CheckBox("Permitir recovery codes");
        globalAllowRecovery.setSelected(policy.isAllowRecoveryCodes());

        globalRecoveryCount = spinner(
                policy.getRecoveryCodeCount(),
                0,
                20,
                1
        );

        globalGraceDays = spinner(
                policy.getGracePeriodDays(),
                0,
                365,
                1
        );

        globalIssuer = new TextField(policy.getIssuer());
        globalIssuer.setPromptText("Kubata");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(9);

        grid.add(globalRequired, 0, 0, 2, 1);
        grid.add(globalAllowDisable, 0, 1, 2, 1);
        grid.add(globalAllowRecovery, 0, 2, 2, 1);
        grid.add(new Label("Recovery codes"), 0, 3);
        grid.add(globalRecoveryCount, 1, 3);
        grid.add(new Label("Grace period (dias)"), 0, 4);
        grid.add(globalGraceDays, 1, 4);
        grid.add(new Label("Issuer"), 0, 5);
        grid.add(globalIssuer, 1, 5);

        Label hint = new Label(
                "A política global é a última camada da resolução: utilizador → perfil → empresa → global."
        );
        hint.setWrapText(true);
        hint.getStyleClass().add("kubata-mfa-security-text");

        Button cancel = new Button("Cancelar", IconUtils.icon(Feather.X, 12));
        cancel.getStyleClass().add("button-outlined");
        cancel.setOnAction(e -> modalManager.hideModal());

        Button save = new Button(
                "Guardar política",
                IconUtils.icon(Feather.SAVE, 12)
        );
        save.getStyleClass().add("button-primary");
        save.setOnAction(e -> {
            try {
                policy.setScopeType(MfaPolicy.ScopeType.GLOBAL);
                policy.setScopeId(null);
                policy.setScopeKey("GLOBAL");
                policy.setNome("Política MFA Global");
                policy.setRequired(globalRequired.isSelected());
                policy.setAllowUserDisable(globalAllowDisable.isSelected());
                policy.setAllowRecoveryCodes(globalAllowRecovery.isSelected());
                policy.setRecoveryCodeCount(globalRecoveryCount.getValue());
                policy.setGracePeriodDays(globalGraceDays.getValue());
                policy.setIssuer(globalIssuer.getText());

                mfaService.savePolicy(
                        sessionManager.getUser(),
                        policy,
                        sourceIp()
                );

                modalManager.hideModal();
                load();
            } catch (Exception ex) {
                modalManager.showErrorModal(
                        "MFA Center",
                        "Não foi possível guardar a política global de MFA.",
                        ex
                );
            }
        });

        VBox content = new VBox(
                12,
                grid,
                new Separator(),
                hint,
                new HBox(8, cancel, save)
        );
        content.setPadding(new Insets(4));

        modalManager.showModal(
                content,
                new ModalManager.ModalConfig()
                        .title("Política Global de MFA")
                        .subtitle("Configuração central do segundo factor")
                        .icon(Feather.SETTINGS)
                        .tone(ModalManager.ModalTone.INFO)
                        .size(620, 430)
                        .minSize(540, 380)
                        .maximizable(false)
                        .minimizable(false)
        );
    }

    private Spinner<Integer> spinner(int value, int min, int max, int step) {
        SpinnerValueFactory.IntegerSpinnerValueFactory factory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(
                        min, max, Math.max(min, Math.min(max, value)), step
                );
        Spinner<Integer> spinner = new Spinner<>(factory);
        spinner.setEditable(true);
        spinner.setPrefWidth(110);
        return spinner;
    }

    private MfaPolicy defaultGlobalPolicy() {
        MfaPolicy policy = new MfaPolicy();
        policy.setScopeType(MfaPolicy.ScopeType.GLOBAL);
        policy.setScopeKey("GLOBAL");
        policy.setNome("Política MFA Global");
        policy.setRequired(false);
        policy.setAllowUserDisable(true);
        policy.setAllowRecoveryCodes(true);
        policy.setRecoveryCodeCount(10);
        policy.setIssuer("Kubata");
        policy.setGracePeriodDays(0);
        policy.setActive(true);
        return policy;
    }

    private void loadGlobalPolicy() {
        // Os campos são carregados quando o modal é aberto; este hook mantém o método explícito
        // para a barra de estado e futuras extensões do centro.
    }

    private Image createQrImage(String text, int size) {
        try {
            BitMatrix matrix = new QRCodeWriter().encode(
                    text,
                    BarcodeFormat.QR_CODE,
                    size,
                    size
            );

            WritableImage image = new WritableImage(size, size);
            javafx.scene.image.PixelWriter writer = image.getPixelWriter();

            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    writer.setColor(
                            x,
                            y,
                            matrix.get(x, y) ? Color.BLACK : Color.WHITE
                    );
                }
            }

            return image;
        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Não foi possível gerar o QR Code do MFA.",
                    ex
            );
        }
    }

    private String sourceIp() {
        User actor = sessionManager.getUser();
        return actor == null || actor.getUltimoIpLogin() == null || actor.getUltimoIpLogin().isBlank()
                ? "127.0.0.1"
                : actor.getUltimoIpLogin();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String safe(String first, String second) {
        return first == null || first.isBlank()
                ? (second == null ? "" : second)
                : first;
    }
}

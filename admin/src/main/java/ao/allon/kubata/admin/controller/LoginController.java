package ao.allon.kubata.admin.controller;

import ao.allon.kubata.admin.service.MaintenanceModeService;
import ao.allon.kubata.admin.ui.event.LoginSuccessEvent;
import ao.allon.kubata.admin.ui.modal.ModalManager;
import ao.allon.kubata.core.domain.Role;
import ao.allon.kubata.core.domain.User;
import ao.allon.kubata.core.exception.AuthenticationException;
import ao.allon.kubata.core.exception.PasswordChangeRequiredException;
import ao.allon.kubata.core.service.AuthService;
import ao.allon.kubata.core.service.PasswordChangeService;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;

/**
 * Tela principal de autenticação do Kubata Administrator.
 *
 * <p>Responsabilidades:
 * validação do formulário, MFA, autenticação assíncrona,
 * alteração obrigatória de credenciais provisórias e recuperação de acesso.</p>
 */
@Component
public class LoginController {

    private static final Logger log = LoggerFactory.getLogger(LoginController.class);

    private static final String PREF_NODE = "ao.allon.kubata.admin.login";
    private static final String PREF_EMAIL = "rememberedEmail";

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@([A-Za-z0-9-]+\\.)+[A-Za-z]{2,}$");

    private final AuthService authService;
    private final ApplicationEventPublisher eventPublisher;
    private final MaintenanceModeService maintenanceModeService;
    private final PasswordChangeService passwordChangeService;
    private final ModalManager modalManager;
    private final Preferences preferences =
            Preferences.userRoot().node(PREF_NODE);

    private final ExecutorService executor =
            Executors.newCachedThreadPool(r -> {
                Thread thread = new Thread(r, "kubata-login");
                thread.setDaemon(true);
                return thread;
            });

    private TextField emailField;
    private PasswordField passwordField;
    private TextField passwordVisibleField;
    private TextField mfaCodeField;

    private Label messageLabel;
    private Label emailStateLabel;
    private Label mfaHintLabel;

    private Button loginButton;
    private ProgressIndicator loading;
    private ToggleButton showPasswordBtn;
    private CheckBox rememberEmail;
    private Hyperlink forgotPasswordLink;
    private Button helpButton;
    private Hyperlink recoveryModeLink;
    private boolean recoveryMode;
    private Button maximizeButton;
    private Rectangle windowClip;

    private double normalStageX;
    private double normalStageY;
    private double normalStageWidth;
    private double normalStageHeight;
    private boolean windowMaximized;

    private double xOffset;
    private double yOffset;

    public LoginController(AuthService authService,
                           ApplicationEventPublisher eventPublisher,
                           MaintenanceModeService maintenanceModeService,
                           PasswordChangeService passwordChangeService,
                           ModalManager modalManager) {
        this.authService = authService;
        this.eventPublisher = eventPublisher;
        this.maintenanceModeService = maintenanceModeService;
        this.passwordChangeService = passwordChangeService;
        this.modalManager = modalManager;
    }

    public Parent createView(Stage stage) {
        StackPane root = new StackPane();
        root.setPrefSize(1120, 720);
        root.getStyleClass().add("login-container");

        modalManager.setRoot(root);

        Region background = new Region();
        background.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #eef5f1, #f8fafc);"
        );

        Region ambient = new Region();
        ambient.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, "
                        + "rgba(26,92,53,0.95), rgba(33,115,70,0.88), rgba(46,204,113,0.72));"
        );

        root.getChildren().addAll(background, ambient);

        installRoundedWindow(root);
        installWindowDragging(root, stage);

        VBox shell = createLoginShell(stage);
        StackPane.setAlignment(shell, Pos.CENTER);
        root.getChildren().add(shell);

        root.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER
                    && !loginButton.isDisabled()
                    && !modalManager.hasOpenModal()) {
                handleLogin();
            }
        });

        animateEntrance(shell);
        Platform.runLater(() -> emailField.requestFocus());

        return root;
    }

    private void installRoundedWindow(StackPane root) {
        windowClip = new Rectangle();
        windowClip.widthProperty().bind(root.widthProperty());
        windowClip.heightProperty().bind(root.heightProperty());
        windowClip.setArcWidth(28);
        windowClip.setArcHeight(28);
        root.setClip(windowClip);
    }

    private HBox createWindowBar(Stage stage) {
        HBox bar = new HBox(2);
        bar.setAlignment(Pos.CENTER_RIGHT);
        bar.setPickOnBounds(false);
        bar.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2
                    && event.getTarget() == bar) {
                toggleWindowMaximize(stage);
                event.consume();
            }
        });

        Button minimizeButton = createWindowControl(
                Feather.MINUS,
                "Minimizar",
                "kubata-modal-window-control",
                () -> stage.setIconified(true)
        );

        maximizeButton = createWindowControl(
                Feather.MAXIMIZE_2,
                "Maximizar",
                "kubata-modal-window-control",
                () -> toggleWindowMaximize(stage)
        );

        Button closeButton = createWindowControl(
                Feather.X,
                "Fechar",
                "kubata-modal-window-control-danger",
                () -> {
                    modalManager.hideAllModals();
                    stage.close();
                }
        );

        bar.getChildren().addAll(
                minimizeButton,
                maximizeButton,
                closeButton
        );

        return bar;
    }

    private Button createWindowControl(
            Feather icon,
            String accessibleText,
            String styleClass,
            Runnable action
    ) {
        Button button = new Button("", new FontIcon(icon));
        button.setAccessibleText(accessibleText);
        button.setFocusTraversable(false);
        button.getStyleClass().addAll(
                "button-icon",
                "flat",
                styleClass
        );
        button.setCursor(Cursor.HAND);
        button.setOnAction(event -> {
            action.run();
            event.consume();
        });
        return button;
    }

    private void toggleWindowMaximize(Stage stage) {
        if (windowMaximized) {
            restoreLoginWindow(stage);
        } else {
            maximizeLoginWindow(stage);
        }
    }

    private void maximizeLoginWindow(Stage stage) {
        normalStageX = stage.getX();
        normalStageY = stage.getY();
        normalStageWidth = stage.getWidth();
        normalStageHeight = stage.getHeight();

        javafx.geometry.Rectangle2D bounds =
                Screen.getScreensForRectangle(
                        stage.getX(),
                        stage.getY(),
                        Math.max(stage.getWidth(), 1),
                        Math.max(stage.getHeight(), 1)
                ).stream()
                        .findFirst()
                        .orElse(Screen.getPrimary())
                        .getVisualBounds();

        stage.setX(bounds.getMinX());
        stage.setY(bounds.getMinY());
        stage.setWidth(bounds.getWidth());
        stage.setHeight(bounds.getHeight());

        windowMaximized = true;
        updateWindowMaximizeButton();

        if (windowClip != null) {
            windowClip.setArcWidth(0);
            windowClip.setArcHeight(0);
        }
    }

    private void restoreLoginWindow(Stage stage) {
        stage.setX(normalStageX);
        stage.setY(normalStageY);
        stage.setWidth(normalStageWidth);
        stage.setHeight(normalStageHeight);

        windowMaximized = false;
        updateWindowMaximizeButton();

        if (windowClip != null) {
            windowClip.setArcWidth(28);
            windowClip.setArcHeight(28);
        }

    }

    private void updateWindowMaximizeButton() {
        if (maximizeButton == null) {
            return;
        }

        maximizeButton.setGraphic(
                new FontIcon(
                        windowMaximized
                                ? Feather.MINIMIZE_2
                                : Feather.MAXIMIZE_2
                )
        );
        maximizeButton.setAccessibleText(
                windowMaximized
                        ? "Restaurar tamanho"
                        : "Maximizar"
        );
        maximizeButton.setTooltip(
                new Tooltip(
                        windowMaximized
                                ? "Restaurar tamanho"
                                : "Maximizar"
                )
        );
    }

    private void installWindowDragging(StackPane root, Stage stage) {
        root.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });

        root.setOnMouseDragged(event -> {
            if (windowMaximized) {
                return;
            }
            if (event.getTarget() instanceof Control
                    && !(event.getTarget() instanceof Label)) {
                return;
            }
            stage.setX(event.getScreenX() - xOffset);
            stage.setY(event.getScreenY() - yOffset);
        });

    }

    private VBox createLoginShell(Stage stage) {
        VBox shell = new VBox();
        shell.setPrefSize(1000, 610);
        shell.setMaxSize(1000, 610);
        shell.setMinSize(920, 560);
        shell.getStyleClass().add("login-card");
        shell.setStyle(
                "-fx-background-color: #ffffff;"
                        + "-fx-background-radius: 22;"
                        + "-fx-border-color: rgba(33,115,70,0.12);"
                        + "-fx-border-width: 1;"
                        + "-fx-border-radius: 22;"
        );
        shell.setEffect(new DropShadow(
                35,
                Color.rgb(15, 23, 42, 0.24)
        ));

        HBox header = createLoginHeader(stage);
        HBox body = new HBox();
        HBox.setHgrow(body, Priority.ALWAYS);
        body.setMinHeight(0);

        Node branding = createBrandingPanel();
        Node authentication = createAuthenticationPanel();

        HBox.setHgrow(branding, Priority.ALWAYS);
        HBox.setHgrow(authentication, Priority.ALWAYS);

        body.getChildren().addAll(
                branding,
                authentication
        );

        shell.getChildren().addAll(
                header,
                body
        );

        return shell;
    }

    private HBox createLoginHeader(Stage stage) {
        HBox header = new HBox(10);
        header.getStyleClass().add("login-window-header");
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(9, 12, 9, 18));
        header.setPrefHeight(50);
        header.setMinHeight(50);
        header.setMaxHeight(50);

        FontIcon icon = new FontIcon(Feather.SHIELD);
        icon.setIconSize(16);
        icon.setIconColor(Color.web("#217346"));

        VBox titleBox = new VBox(1);
        titleBox.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("KUBATA Administrator");
        title.getStyleClass().add("login-window-title");

        Label subtitle = new Label("Acesso seguro");
        subtitle.getStyleClass().add("login-window-subtitle");

        titleBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox controls = createWindowBar(stage);
        header.getChildren().addAll(
                icon,
                titleBox,
                spacer,
                controls
        );

        header.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2
                    && (event.getTarget() == header
                    || event.getTarget() == titleBox
                    || event.getTarget() == title
                    || event.getTarget() == subtitle
                    || event.getTarget() == icon)) {
                toggleWindowMaximize(stage);
                event.consume();
            }
        });

        return header;
    }

    private VBox createBrandingPanel() {
        VBox panel = new VBox(24);
        panel.setPrefWidth(455);
        panel.setMinWidth(400);
        panel.setAlignment(Pos.CENTER_LEFT);
        panel.setPadding(new Insets(50));
        panel.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, "
                        + "#124d2f, #1f7045);"
                        + "-fx-background-radius: 0 0 0 20;"
        );

        StackPane emblem = new StackPane();
        emblem.setPrefSize(96, 96);
        emblem.setMaxSize(96, 96);
        emblem.setStyle(
                "-fx-background-color: rgba(255,255,255,0.14);"
                        + "-fx-background-radius: 28;"
                        + "-fx-border-color: rgba(255,255,255,0.22);"
                        + "-fx-border-radius: 28;"
                        + "-fx-border-width: 1;"
        );

        FontIcon shield = new FontIcon(Feather.SHIELD);
        shield.setIconSize(54);
        shield.setIconColor(Color.WHITE);
        emblem.getChildren().add(shield);

        Label title = new Label("KUBATA");
        title.setStyle(
                "-fx-font-size: 44px;"
                        + "-fx-font-weight: 900;"
                        + "-fx-text-fill: white;"
                        + "-fx-letter-spacing: 4px;"
        );

        Label subtitle = new Label(
                "Administrator"
        );
        subtitle.setStyle(
                "-fx-font-size: 19px;"
                        + "-fx-font-weight: 600;"
                        + "-fx-text-fill: rgba(255,255,255,0.93);"
        );

        Label description = new Label(
                "Gestão centralizada de utilizadores, segurança, "
                        + "configuração e operação empresarial."
        );
        description.setWrapText(true);
        description.setMaxWidth(320);
        description.setStyle(
                "-fx-font-size: 14px;"
                        + "-fx-line-spacing: 4;"
                        + "-fx-text-fill: rgba(255,255,255,0.76);"
        );

        VBox securityCard = new VBox(10);
        securityCard.setPadding(new Insets(16));
        securityCard.setMaxWidth(335);
        securityCard.setStyle(
                "-fx-background-color: rgba(255,255,255,0.10);"
                        + "-fx-background-radius: 14;"
                        + "-fx-border-color: rgba(255,255,255,0.16);"
                        + "-fx-border-radius: 14;"
                        + "-fx-border-width: 1;"
        );

        HBox securityTitle = new HBox(8);
        securityTitle.setAlignment(Pos.CENTER_LEFT);

        FontIcon securityIcon = new FontIcon(Feather.LOCK);
        securityIcon.setIconSize(16);
        securityIcon.setIconColor(Color.WHITE);

        Label securityLabel = new Label("Camada de segurança");
        securityLabel.setStyle(
                "-fx-font-size: 13px;"
                        + "-fx-font-weight: 700;"
                        + "-fx-text-fill: white;"
        );

        securityTitle.getChildren().addAll(
                securityIcon,
                securityLabel
        );

        Label securityText = new Label(
                "Bloqueio automático, expiração de credenciais, MFA "
                        + "e auditoria administrativa."
        );
        securityText.setWrapText(true);
        securityText.setStyle(
                "-fx-font-size: 12px;"
                        + "-fx-text-fill: rgba(255,255,255,0.72);"
        );

        securityCard.getChildren().addAll(
                securityTitle,
                securityText
        );

        VBox.setVgrow(description, Priority.NEVER);
        panel.getChildren().addAll(
                emblem,
                title,
                subtitle,
                description,
                securityCard
        );

        Label buildLabel = new Label("KUBATA Administrator • v2.0.0");
        buildLabel.setStyle(
                "-fx-font-size: 11px;"
                        + "-fx-text-fill: rgba(255,255,255,0.48);"
        );
        panel.getChildren().add(buildLabel);

        return panel;
    }

    private VBox createAuthenticationPanel() {
        VBox panel = new VBox(18);
        panel.setPrefWidth(545);
        panel.setPadding(new Insets(48, 54, 42, 54));
        panel.setStyle(
                "-fx-background-color: white;"
                        + "-fx-background-radius: 0 0 20 0;"
        );
        panel.setAlignment(Pos.TOP_LEFT);

        HBox topLine = new HBox();
        topLine.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Iniciar sessão");
        title.setStyle(
                "-fx-font-size: 30px;"
                        + "-fx-font-weight: 800;"
                        + "-fx-text-fill: #163725;"
        );

        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);

        helpButton = new Button(
                "Ajuda",
                new FontIcon(Feather.HELP_CIRCLE)
        );
        helpButton.getStyleClass().add("button-outlined");
        helpButton.setCursor(Cursor.HAND);
        helpButton.setOnAction(event -> showSecurityHelp());

        topLine.getChildren().addAll(
                title,
                topSpacer,
                helpButton
        );

        Label subtitle = new Label(
                "Aceda à consola administrativa do Kubata."
        );
        subtitle.setStyle(
                "-fx-font-size: 14px;"
                        + "-fx-text-fill: #708090;"
        );

        VBox form = createForm();

        Label footer = new Label(
                "Nunca partilhe a sua palavra-passe ou código MFA."
        );
        footer.setWrapText(true);
        footer.setStyle(
                "-fx-font-size: 11px;"
                        + "-fx-text-fill: #94a3b8;"
        );

        panel.getChildren().addAll(
                topLine,
                subtitle,
                form,
                footer
        );

        return panel;
    }

    private VBox createForm() {
        VBox form = new VBox(13);

        VBox emailBox = new VBox(6);
        HBox emailHeader = new HBox(6);
        emailHeader.setAlignment(Pos.CENTER_LEFT);

        Label emailLabel = createFieldLabel("Email");
        emailStateLabel = new Label();
        emailStateLabel.setStyle(
                "-fx-font-size: 11px;"
                        + "-fx-text-fill: #16a34a;"
        );

        emailHeader.getChildren().addAll(emailLabel, emailStateLabel);

        emailField = new TextField();
        emailField.setPromptText("nome@empresa.ao");
        emailField.setPrefHeight(46);
        emailField.getStyleClass().add("modern-text-field");

        String rememberedEmail = preferences.get(PREF_EMAIL, "");
        if (!rememberedEmail.isBlank()) {
            emailField.setText(rememberedEmail);
            emailStateLabel.setText("guardado neste computador");
        }

        ChangeListener<String> emailListener =
                (obs, oldValue, value) -> updateEmailState(value);
        emailField.textProperty().addListener(emailListener);

        emailBox.getChildren().addAll(
                emailHeader,
                emailField
        );

        VBox passwordBox = createPasswordBox();

        VBox mfaBox = new VBox(6);
        HBox mfaHeader = new HBox(6);
        mfaHeader.setAlignment(Pos.CENTER_LEFT);

        Label mfaLabel = createFieldLabel("Código MFA");
        mfaHintLabel = new Label("6 dígitos quando o MFA estiver activo");
        mfaHintLabel.setStyle(
                "-fx-font-size: 11px;"
                        + "-fx-text-fill: #94a3b8;"
        );

        Region mfaSpacer = new Region();
        HBox.setHgrow(mfaSpacer, Priority.ALWAYS);

        recoveryModeLink = new Hyperlink("Usar código de recuperação");
        recoveryModeLink.setStyle(
                "-fx-font-size: 11px;"
                        + "-fx-font-weight: 700;"
                        + "-fx-text-fill: #217346;"
        );
        recoveryModeLink.setOnAction(event -> setRecoveryMode(!recoveryMode));

        mfaHeader.getChildren().addAll(
                mfaLabel,
                mfaHintLabel,
                mfaSpacer,
                recoveryModeLink
        );

        mfaCodeField = new TextField();
        mfaCodeField.setPromptText("Código de 6 dígitos");
        mfaCodeField.setPrefHeight(46);
        mfaCodeField.getStyleClass().add("modern-text-field");

        setRecoveryMode(false);

        mfaCodeField.setOnAction(event -> handleLogin());

        mfaBox.getChildren().addAll(
                mfaHeader,
                mfaCodeField
        );

        HBox actions = new HBox();
        actions.setAlignment(Pos.CENTER_LEFT);

        rememberEmail = new CheckBox("Lembrar email");
        rememberEmail.setSelected(!rememberedEmail.isBlank());
        rememberEmail.setStyle(
                "-fx-font-size: 12px;"
                        + "-fx-text-fill: #64748b;"
        );

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        forgotPasswordLink = new Hyperlink("Recuperar acesso");
        forgotPasswordLink.setStyle(
                "-fx-font-size: 12px;"
                        + "-fx-font-weight: 700;"
                        + "-fx-text-fill: #217346;"
        );
        forgotPasswordLink.setOnAction(event -> showRecoveryInfo());

        actions.getChildren().addAll(
                rememberEmail,
                spacer,
                forgotPasswordLink
        );

        messageLabel = new Label();
        messageLabel.setWrapText(true);
        messageLabel.setManaged(false);
        messageLabel.setVisible(false);
        messageLabel.setMaxWidth(Double.MAX_VALUE);

        loginButton = new Button(
                "ENTRAR NO KUBATA",
                new FontIcon(Feather.LOG_IN)
        );
        loginButton.getStyleClass().add("button-primary");
        loginButton.setPrefHeight(54);
        loginButton.setMaxWidth(Double.MAX_VALUE);
        loginButton.setCursor(Cursor.HAND);
        loginButton.setDefaultButton(true);
        loginButton.setOnAction(event -> handleLogin());

        loading = new ProgressIndicator();
        loading.setMaxSize(26, 26);
        loading.setVisible(false);
        loading.setManaged(false);

        StackPane loginStack = new StackPane(
                loginButton,
                loading
        );

        HBox status = createSecurityStatus();

        form.getChildren().addAll(
                emailBox,
                passwordBox,
                mfaBox,
                actions,
                messageLabel,
                loginStack,
                status
        );

        return form;
    }

    private void setRecoveryMode(boolean enabled) {
        recoveryMode = enabled;
        mfaCodeField.clear();

        if (recoveryMode) {
            mfaCodeField.setPromptText("ABCD-EFGH-JKLM");
            mfaHintLabel.setText("Código de recuperação de uso único");
            recoveryModeLink.setText("Usar código MFA");
            mfaCodeField.setTextFormatter(
                    new TextFormatter<String>(change ->
                            change.getControlNewText()
                                    .toUpperCase()
                                    .matches("[A-HJ-NP-Z2-9-]{0,14}")
                                    ? change
                                    : null
                    )
            );
        } else {
            mfaCodeField.setPromptText("Código de 6 dígitos");
            mfaHintLabel.setText("6 dígitos quando o MFA estiver activo");
            recoveryModeLink.setText("Usar código de recuperação");
            mfaCodeField.setTextFormatter(
                    new TextFormatter<String>(change ->
                            change.getControlNewText().matches("\\d{0,6}")
                                    ? change
                                    : null
                    )
            );
        }
    }

    private VBox createPasswordBox() {
        VBox passwordBox = new VBox(6);

        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);

        Label label = createFieldLabel("Palavra-passe");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        showPasswordBtn = new ToggleButton(
                "Mostrar",
                new FontIcon(Feather.EYE)
        );
        showPasswordBtn.getStyleClass().add("button-icon-small");
        showPasswordBtn.setCursor(Cursor.HAND);
        showPasswordBtn.setTooltip(
                new Tooltip("Mostrar ou ocultar a palavra-passe")
        );

        header.getChildren().addAll(
                label,
                spacer,
                showPasswordBtn
        );

        passwordField = new PasswordField();
        passwordField.setPromptText("Introduza a sua palavra-passe");
        passwordField.setPrefHeight(46);
        passwordField.getStyleClass().add("modern-text-field");

        passwordVisibleField = new TextField();
        passwordVisibleField.setPromptText("Introduza a sua palavra-passe");
        passwordVisibleField.setPrefHeight(46);
        passwordVisibleField.getStyleClass().add("modern-text-field");
        passwordVisibleField.setVisible(false);
        passwordVisibleField.setManaged(false);

        passwordVisibleField.textProperty()
                .bindBidirectional(passwordField.textProperty());

        showPasswordBtn.selectedProperty().addListener(
                (obs, wasSelected, selected) -> {
                    showPasswordBtn.setText(
                            selected ? "Ocultar" : "Mostrar"
                    );
                    showPasswordBtn.setGraphic(
                            new FontIcon(
                                    selected
                                            ? Feather.EYE_OFF
                                            : Feather.EYE
                            )
                    );

                    passwordField.setVisible(!selected);
                    passwordField.setManaged(!selected);

                    passwordVisibleField.setVisible(selected);
                    passwordVisibleField.setManaged(selected);

                    if (selected) {
                        passwordVisibleField.requestFocus();
                    } else {
                        passwordField.requestFocus();
                    }
                }
        );

        passwordField.setOnAction(event -> handleLogin());

        passwordBox.getChildren().addAll(
                header,
                passwordField,
                passwordVisibleField
        );

        return passwordBox;
    }

    private HBox createSecurityStatus() {
        HBox status = new HBox(9);
        status.setAlignment(Pos.CENTER_LEFT);
        status.setPadding(new Insets(10, 12, 10, 12));
        status.setStyle(
                "-fx-background-color: #f1f8f4;"
                        + "-fx-background-radius: 10;"
                        + "-fx-border-color: #d8eee0;"
                        + "-fx-border-radius: 10;"
                        + "-fx-border-width: 1;"
        );

        FontIcon icon = new FontIcon(Feather.SHIELD);
        icon.setIconSize(14);
        icon.setIconColor(Color.web("#1f7a46"));

        Label text = new Label(
                "Autenticação protegida • MFA disponível • "
                        + "credenciais temporárias exigem troca"
        );
        text.setWrapText(true);
        text.setStyle(
                "-fx-font-size: 11px;"
                        + "-fx-text-fill: #47735a;"
        );

        status.getChildren().addAll(icon, text);
        return status;
    }

    private Label createFieldLabel(String text) {
        Label label = new Label(text);
        label.setStyle(
                "-fx-font-size: 13px;"
                        + "-fx-font-weight: 700;"
                        + "-fx-text-fill: #334155;"
        );
        return label;
    }

    private void updateEmailState(String email) {
        if (email == null || email.isBlank()) {
            emailStateLabel.setText("");
            emailField.setStyle("");
            return;
        }

        if (EMAIL_PATTERN.matcher(email.trim()).matches()) {
            emailStateLabel.setText("formato válido");
            emailField.setStyle(
                    "-fx-border-color: #86efac;"
                            + "-fx-border-width: 1;"
            );
        } else {
            emailStateLabel.setText("");
            emailField.setStyle(
                    "-fx-border-color: #fca5a5;"
                            + "-fx-border-width: 1;"
            );
        }
    }

    private void handleLogin() {
        String email = emailField.getText() == null
                ? ""
                : emailField.getText().trim();

        String password = passwordField.getText() == null
                ? ""
                : passwordField.getText();

        String mfaRaw = mfaCodeField.getText() == null
                ? ""
                : mfaCodeField.getText().trim();

        if (email.isBlank()) {
            showMessage("Introduza o email da conta.", true);
            emailField.requestFocus();
            return;
        }

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            showMessage(
                    "Introduza um endereço de email válido.",
                    true
            );
            emailField.requestFocus();
            return;
        }

        if (password.isBlank()) {
            showMessage(
                    "Introduza a palavra-passe.",
                    true
            );
            passwordField.requestFocus();
            return;
        }

        Integer mfaCode = null;
        String recoveryCode = null;

        if (!mfaRaw.isBlank()) {
            if (recoveryMode) {
                String normalizedRecovery =
                        mfaRaw.replace("-", "").trim().toUpperCase();

                if (!(normalizedRecovery.length() == 12
                        || normalizedRecovery.length() == 14)) {
                    showMessage(
                            "O código de recuperação deve ter 12 caracteres, "
                                    + "com ou sem hífens.",
                            true
                    );
                    mfaCodeField.requestFocus();
                    return;
                }

                recoveryCode = mfaRaw;
            } else {
                if (mfaRaw.length() != 6) {
                    showMessage(
                            "O código MFA deve ter 6 dígitos.",
                            true
                    );
                    mfaCodeField.requestFocus();
                    return;
                }
                mfaCode = Integer.valueOf(mfaRaw);
            }
        }

        setLoading(true);
        showMessage("", false);

        final Integer finalMfaCode = mfaCode;
        final String finalRecoveryCode = recoveryCode;
        final String finalPassword = password;
        final String sourceIp = resolveSourceIp();

        Task<User> task = new Task<>() {
            @Override
            protected User call() {
                return authService.authenticate(
                        email,
                        password,
                        finalMfaCode,
                        finalRecoveryCode,
                        sourceIp
                );
            }
        };

        task.setOnSucceeded(event -> {
            setLoading(false);

            User user = task.getValue();
            boolean elevated =
                    user != null
                            && (user.isSuperadmin()
                            || user.getRole() == Role.ADMIN);

            if (maintenanceModeService.isEnabled() && !elevated) {
                authService.logout(user, sourceIp);
                showMessage(
                        "O sistema está temporariamente em manutenção. "
                                + maintenanceModeService.getReason(),
                        true
                );
                return;
            }

            persistRememberedEmail(email);

            modalManager.hideAllModals();
            playExitAnimation(() ->
                    Platform.runLater(() ->
                            eventPublisher.publishEvent(
                                    new LoginSuccessEvent(this, user)
                            )
                    )
            );
        });

        task.setOnFailed(event -> {
            setLoading(false);

            Throwable error = task.getException();

            if (error instanceof PasswordChangeRequiredException required) {
                showMandatoryPasswordChange(
                        required.getUser(),
                        password
                );
                return;
            }

            if (error instanceof AuthenticationException authenticationException) {
                log.warn(
                        "Falha de autenticação para {}: {}",
                        email,
                        authenticationException.getMessage()
                );
                showAuthenticationErrorModal(
                        email,
                        finalPassword,
                        sourceIp,
                        authenticationException.getMessage()
                );
                shakeNode(loginButton);
                return;
            }

            if (error != null) {
                log.warn(
                        "Falha de autenticação para {}: {}",
                        email,
                        error.getMessage(),
                        error
                );
            }

            showMessage(
                    "Não foi possível iniciar a sessão. "
                            + "Consulte os detalhes da falha para saber como resolver.",
                    true
            );
            shakeNode(loginButton);
        });

        executor.submit(task);
    }

    private void showAuthenticationErrorModal(
            String email,
            String password,
            String sourceIp,
            String reason
    ) {
        AuthenticationErrorDetails details = describeAuthenticationError(reason);

        VBox content = new VBox(14);
        content.setPadding(new Insets(6));
        content.setPrefWidth(570);

        HBox summary = new HBox(12);
        summary.setAlignment(Pos.TOP_LEFT);
        summary.setPadding(new Insets(14));
        summary.setStyle(
                "-fx-background-color: #f8fafc;"
                        + "-fx-background-radius: 12;"
                        + "-fx-border-color: #e2e8f0;"
                        + "-fx-border-radius: 12;"
                        + "-fx-border-width: 1;"
        );

        FontIcon summaryIcon = new FontIcon(details.icon());
        summaryIcon.setIconSize(24);
        summaryIcon.setIconColor(details.toneColor());

        VBox summaryText = new VBox(4);
        Label summaryTitle = new Label(details.title());
        summaryTitle.setStyle(
                "-fx-font-size: 16px;"
                        + "-fx-font-weight: 800;"
                        + "-fx-text-fill: #1e293b;"
        );

        Label accountLabel = new Label(
                "Conta: " + (email == null || email.isBlank() ? "-" : email)
        );
        accountLabel.setStyle(
                "-fx-font-size: 11px;"
                        + "-fx-text-fill: #64748b;"
        );

        summaryText.getChildren().addAll(summaryTitle, accountLabel);
        summary.getChildren().addAll(summaryIcon, summaryText);

        VBox explanation = createAuthenticationErrorSection(
                Feather.INFO,
                "O que aconteceu",
                details.explanation(),
                "kubata-auth-modal-info"
        );

        VBox resolution = createAuthenticationErrorSection(
                Feather.TOOL,
                "Como resolver",
                details.resolution(),
                "kubata-auth-modal-resolution"
        );

        VBox sessionAction = null;
        if (isConcurrentSessionLimitError(reason)) {
            sessionAction = buildTerminateOldestSessionAction(
                    email,
                    password,
                    sourceIp
            );
        }

        Label securityNote = new Label(
                "Por segurança, o Kubata não apresenta senhas, tokens MFA "
                        + "ou outros segredos neste diagnóstico."
        );
        securityNote.setWrapText(true);
        securityNote.setStyle(
                "-fx-font-size: 11px;"
                        + "-fx-text-fill: #64748b;"
        );

        if (sessionAction != null) {
            content.getChildren().addAll(
                    summary,
                    explanation,
                    resolution,
                    sessionAction,
                    securityNote
            );
        } else {
            content.getChildren().addAll(
                    summary,
                    explanation,
                    resolution,
                    securityNote
            );
        }

        Runnable focusAfterClose = () -> Platform.runLater(() -> {
            if (details.focusMfa()) {
                mfaCodeField.requestFocus();
                mfaCodeField.selectAll();
            } else if (details.focusPassword()) {
                passwordField.requestFocus();
                passwordField.selectAll();
            } else if (details.focusEmail()) {
                emailField.requestFocus();
                emailField.selectAll();
            }
        });

        modalManager.showModal(
                content,
                new ModalManager.ModalConfig()
                        .title("Não foi possível iniciar a sessão")
                        .subtitle("Diagnóstico da autenticação")
                        .icon(details.icon())
                        .tone(details.tone())
                        .singleButton("Entendido")
                        .size(650, 480)
                        .minSize(570, 430)
                        .maxSize(760, 620)
                        .maximizable(false)
                        .minimizable(false)
                        .closeOnOverlayClick(false)
                        .closeOnEscape(true)
                        .footerHint("Corrija a causa indicada e tente iniciar a sessão novamente.")
                        .onConfirm(focusAfterClose)
        );
    }

    private boolean isConcurrentSessionLimitError(String reason) {
        return reason != null
                && reason.toLowerCase(Locale.ROOT)
                .contains("limite de sessões simultâneas");
    }

    private VBox buildTerminateOldestSessionAction(
            String email,
            String password,
            String sourceIp
    ) {
        VBox box = new VBox(9);
        box.setPadding(new Insets(13));
        box.setStyle(
                "-fx-background-color: #f0fdf4;"
                        + "-fx-background-radius: 12;"
                        + "-fx-border-color: #bbf7d0;"
                        + "-fx-border-radius: 12;"
                        + "-fx-border-width: 1;"
        );

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        FontIcon icon = new FontIcon(Feather.LOG_OUT);
        icon.setIconSize(15);
        icon.setIconColor(Color.web("#217346"));

        Label title = new Label("Libertar espaço para este login");
        title.setStyle(
                "-fx-font-size: 12px;"
                        + "-fx-font-weight: 800;"
                        + "-fx-text-fill: #166534;"
        );
        header.getChildren().addAll(icon, title);

        Label description = new Label(
                "Pode terminar a sessão mais antiga desta conta sem precisar entrar na aplicação. "
                        + "A operação exige novamente a palavra-passe da conta e afecta apenas uma sessão."
        );
        description.setWrapText(true);
        description.setStyle(
                "-fx-font-size: 11px;"
                        + "-fx-text-fill: #47735a;"
        );

        Button terminate = new Button(
                "Terminar sessão mais antiga",
                new FontIcon(Feather.LOG_OUT)
        );
        terminate.getStyleClass().add("button-outlined");
        terminate.setCursor(Cursor.HAND);
        terminate.setAccessibleText("Terminar a sessão mais antiga desta conta");
        terminate.setTooltip(new Tooltip("Terminar apenas a sessão mais antiga"));
        terminate.setOnAction(event -> showTerminateOldestSessionConfirmation(
                email,
                password,
                sourceIp
        ));

        HBox actionRow = new HBox(terminate);
        actionRow.setAlignment(Pos.CENTER_RIGHT);
        box.getChildren().addAll(header, description, actionRow);
        return box;
    }

    private void showTerminateOldestSessionConfirmation(
            String email,
            String password,
            String sourceIp
    ) {
        VBox content = new VBox(12);
        content.setPadding(new Insets(6));

        Label warning = new Label(
                "A sessão que tiver o login mais antigo será terminada. "
                        + "As outras sessões permanecem activas. Depois da operação, volte ao ecrã de login e tente novamente."
        );
        warning.setWrapText(true);
        warning.setStyle(
                "-fx-font-size: 12px;"
                        + "-fx-text-fill: #475569;"
        );

        VBox caution = new VBox(6);
        caution.setPadding(new Insets(11));
        caution.setStyle(
                "-fx-background-color: #fff8eb;"
                        + "-fx-background-radius: 10;"
                        + "-fx-border-color: #f5ddb0;"
                        + "-fx-border-radius: 10;"
                        + "-fx-border-width: 1;"
        );
        Label cautionText = new Label(
                "Esta acção pode interromper o acesso de outro posto de trabalho que esteja a usar esta conta."
        );
        cautionText.setWrapText(true);
        cautionText.setStyle(
                "-fx-font-size: 11px;"
                        + "-fx-text-fill: #8a5a00;"
        );
        caution.getChildren().add(cautionText);

        content.getChildren().addAll(
                warning,
                caution
        );

        modalManager.showModal(
                content,
                new ModalManager.ModalConfig()
                        .title("Confirmar término da sessão")
                        .subtitle("Esta operação irá libertar uma sessão da conta")
                        .icon(Feather.LOG_OUT)
                        .tone(ModalManager.ModalTone.WARNING)
                        .withConfirmButtons("Terminar sessão", "Cancelar")
                        .confirmStyle("button-primary")
                        .size(570, 300)
                        .minSize(500, 270)
                        .maximizable(false)
                        .minimizable(false)
                        .closeOnOverlayClick(false)
                        .footerHint("Será terminada apenas uma sessão: a mais antiga.")
                        .onConfirm(() -> terminateOldestSessionFromLogin(
                                email,
                                password,
                                sourceIp
                        ))
        );
    }

    private void terminateOldestSessionFromLogin(
            String email,
            String password,
            String sourceIp
    ) {
        modalManager.showLoadingModal(
                "A terminar sessão antiga",
                "A validar a conta e a libertar uma sessão segura..."
        );

        executor.submit(() -> {
            try {
                int terminated = authService.terminateOldestSessionForLogin(
                        email,
                        password,
                        sourceIp
                );

                Platform.runLater(() -> {
                    modalManager.hideLoadingModal();

                    if (terminated > 0) {
                        showSessionTerminationSuccess();
                    } else {
                        showSessionTerminationResult(
                                "Nenhuma sessão encontrada",
                                "Já não existem sessões registadas para esta conta. Pode tentar iniciar a sessão novamente.",
                                ModalManager.ModalTone.INFO,
                                Feather.INFO
                        );
                    }
                });
            } catch (Exception ex) {
                log.warn(
                        "Falha ao terminar sessão antiga durante o login para {}: {}",
                        email,
                        ex.getMessage()
                );

                Platform.runLater(() -> {
                    modalManager.hideLoadingModal();
                    showSessionTerminationResult(
                            "Não foi possível terminar a sessão",
                            ex.getMessage() == null || ex.getMessage().isBlank()
                                    ? "A operação foi recusada. Verifique a credencial e tente novamente."
                                    : ex.getMessage(),
                            ModalManager.ModalTone.DANGER,
                            Feather.ALERT_OCTAGON
                    );
                });
            }
        });
    }

    private void showSessionTerminationSuccess() {
        modalManager.showModal(
                createSessionTerminationMessage(
                        Feather.CHECK_CIRCLE,
                        "Sessão terminada com sucesso",
                        "Uma sessão antiga foi terminada. O limite de sessões desta conta foi libertado e pode tentar iniciar a sessão novamente.",
                        "kubata-auth-modal-resolution"
                ),
                new ModalManager.ModalConfig()
                        .title("Sessão libertada")
                        .subtitle("Pode voltar a tentar o acesso")
                        .icon(Feather.CHECK_CIRCLE)
                        .tone(ModalManager.ModalTone.SUCCESS)
                        .singleButton("Voltar ao login")
                        .size(560, 270)
                        .minSize(500, 250)
                        .maximizable(false)
                        .minimizable(false)
                        .onConfirm(() -> {
                            emailField.requestFocus();
                        })
        );
    }

    private void showSessionTerminationResult(
            String title,
            String message,
            ModalManager.ModalTone tone,
            Feather icon
    ) {
        modalManager.showModal(
                createSessionTerminationMessage(
                        icon,
                        title,
                        message,
                        "kubata-auth-modal-info"
                ),
                new ModalManager.ModalConfig()
                        .title(title)
                        .icon(icon)
                        .tone(tone)
                        .singleButton("Entendido")
                        .size(560, 270)
                        .minSize(500, 250)
                        .maximizable(false)
                        .minimizable(false)
        );
    }

    private VBox createSessionTerminationMessage(
            Feather icon,
            String title,
            String message,
            String styleClass
    ) {
        VBox box = new VBox(9);
        box.setPadding(new Insets(8));

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        FontIcon itemIcon = new FontIcon(icon);
        itemIcon.setIconSize(20);
        itemIcon.setIconColor(
                icon == Feather.CHECK_CIRCLE
                        ? Color.web("#217346")
                        : Color.web("#64748b")
        );

        Label itemTitle = new Label(title);
        itemTitle.setStyle(
                "-fx-font-size: 15px;"
                        + "-fx-font-weight: 800;"
                        + "-fx-text-fill: #1e293b;"
        );
        header.getChildren().addAll(itemIcon, itemTitle);

        Label text = new Label(message);
        text.setWrapText(true);
        text.setStyle(
                "-fx-font-size: 12px;"
                        + "-fx-text-fill: #64748b;"
        );

        box.getStyleClass().add(styleClass);
        box.getChildren().addAll(header, text);
        return box;
    }

    private VBox createAuthenticationErrorSection(
            Feather icon,
            String title,
            String text,
            String styleClass
    ) {
        VBox box = new VBox(8);
        box.setPadding(new Insets(12));
        box.getStyleClass().add(styleClass);

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        FontIcon sectionIcon = new FontIcon(icon);
        sectionIcon.setIconSize(15);
        sectionIcon.setIconColor(Color.web("#217346"));

        Label label = new Label(title);
        label.setStyle(
                "-fx-font-size: 12px;"
                        + "-fx-font-weight: 800;"
                        + "-fx-text-fill: #334155;"
        );

        header.getChildren().addAll(sectionIcon, label);

        Label description = new Label(text);
        description.setWrapText(true);
        description.setMaxWidth(Double.MAX_VALUE);
        description.setStyle(
                "-fx-font-size: 12px;"
                        + "-fx-line-spacing: 2;"
                        + "-fx-text-fill: #64748b;"
        );

        box.getChildren().addAll(header, description);
        return box;
    }

    private AuthenticationErrorDetails describeAuthenticationError(String reason) {
        String normalized = reason == null
                ? ""
                : reason.toLowerCase(Locale.ROOT);

        if (normalized.contains("limite de sessões simultâneas")) {
            return new AuthenticationErrorDetails(
                    "Limite de sessões atingido",
                    "A conta já possui o número máximo de sessões simultâneas permitido pela sua política de segurança.",
                    "Feche uma sessão que esteja activa noutro computador ou dispositivo. No Kubata Administrator, abra Administração > Sessões para terminar sessões que já não estejam em uso. Se todas forem necessárias, peça a um administrador autorizado para rever o limite de sessões no Perfil de Segurança do utilizador. As sessões antigas também podem desaparecer automaticamente quando o tempo limite configurado for ultrapassado.",
                    Feather.USERS,
                    ModalManager.ModalTone.WARNING,
                    Color.web("#b45309"),
                    false,
                    false,
                    false
            );
        }

        if (normalized.contains("temporariamente bloqueada")) {
            return new AuthenticationErrorDetails(
                    "Conta temporariamente bloqueada",
                    "A política de segurança bloqueou temporariamente novas tentativas de autenticação nesta conta.",
                    "Aguarde o fim do período de bloqueio antes de tentar novamente. Se o bloqueio ocorreu por engano ou continuar depois do período previsto, peça a um administrador autorizado para verificar a política de login e o estado da conta.",
                    Feather.LOCK,
                    ModalManager.ModalTone.WARNING,
                    Color.web("#b45309"),
                    true,
                    false,
                    false
            );
        }

        if (normalized.contains("credenciais inválidas")) {
            return new AuthenticationErrorDetails(
                    "Credenciais não validadas",
                    "O email ou a palavra-passe fornecidos não correspondem às credenciais aceites pelo Kubata. Por segurança, o sistema não informa qual dos dois dados está incorrecto.",
                    "Confirme o email da conta e introduza novamente a palavra-passe. Evite copiar espaços antes ou depois do email. Se continuar a falhar, utilize a recuperação de acesso disponível ou peça a um administrador autorizado para confirmar o estado da conta e redefinir a palavra-passe, quando aplicável.",
                    Feather.KEY,
                    ModalManager.ModalTone.DANGER,
                    Color.web("#b91c1c"),
                    false,
                    true,
                    true
            );
        }

        if (normalized.contains("senha expirada") || normalized.contains("password expirou")) {
            return new AuthenticationErrorDetails(
                    "Palavra-passe expirada",
                    "A política de segurança da conta determinou que a palavra-passe actual já não pode ser usada para iniciar uma sessão normal.",
                    "Altere a palavra-passe através do procedimento de alteração disponibilizado para a conta. Caso não exista uma opção de auto-atendimento neste ambiente, peça a um administrador autorizado para efectuar a redefinição e, depois, volte a iniciar a sessão.",
                    Feather.KEY,
                    ModalManager.ModalTone.WARNING,
                    Color.web("#b45309"),
                    false,
                    true,
                    false
            );
        }

        if (normalized.contains("conta inativa")) {
            return new AuthenticationErrorDetails(
                    "Conta inactiva",
                    "A conta existe, mas está actualmente marcada como inactiva e a política de autenticação impede o acesso.",
                    "Peça a um administrador autorizado para verificar o estado da conta no módulo Utilizadores e activá-la, caso o acesso deva continuar permitido.",
                    Feather.USER_X,
                    ModalManager.ModalTone.DANGER,
                    Color.web("#b91c1c"),
                    false,
                    false,
                    false
            );
        }

        if (normalized.contains("endereço ip não está autorizado")) {
            return new AuthenticationErrorDetails(
                    "Endereço IP não autorizado",
                    "A política de segurança individual desta conta restringe os endereços de rede a partir dos quais o login é permitido.",
                    "Entre a partir de uma rede autorizada ou peça a um administrador autorizado para verificar e actualizar os IPs permitidos no Perfil de Segurança do utilizador. Não tente contornar a restrição com credenciais de outra conta.",
                    Feather.GLOBE,
                    ModalManager.ModalTone.WARNING,
                    Color.web("#b45309"),
                    false,
                    false,
                    false
            );
        }

        if (normalized.contains("não está autorizado neste horário")) {
            return new AuthenticationErrorDetails(
                    "Horário de acesso não permitido",
                    "O período actual está fora do horário definido na política de segurança desta conta.",
                    "Tente novamente dentro do horário autorizado. Se o horário estiver incorrecto para a sua função, peça a um administrador autorizado para rever a agenda definida no Perfil de Segurança.",
                    Feather.CLOCK,
                    ModalManager.ModalTone.WARNING,
                    Color.web("#b45309"),
                    false,
                    false,
                    false
            );
        }

        if (normalized.contains("não está autorizado neste dia")) {
            return new AuthenticationErrorDetails(
                    "Dia de acesso não permitido",
                    "O dia actual não está incluído nos dias da semana autorizados para esta conta.",
                    "Tente novamente num dia permitido ou peça a um administrador autorizado para rever os dias autorizados no Perfil de Segurança do utilizador.",
                    Feather.CALENDAR,
                    ModalManager.ModalTone.WARNING,
                    Color.web("#b45309"),
                    false,
                    false,
                    false
            );
        }

        if (normalized.contains("multifactor")
                || normalized.contains("código mfa")
                || normalized.contains("código de recuperação")) {
            return new AuthenticationErrorDetails(
                    "Segundo factor não validado",
                    "A conta exige um segundo factor, mas o código MFA ou o código de recuperação fornecido não foi aceite.",
                    "Confirme o código de 6 dígitos no aplicativo autenticador e tente novamente. Se estiver a usar um código de recuperação, confirme que é o código correcto e que ainda pode ser utilizado. Nunca partilhe estes códigos com outra pessoa.",
                    Feather.SHIELD,
                    ModalManager.ModalTone.WARNING,
                    Color.web("#b45309"),
                    false,
                    false,
                    false
            );
        }

        return new AuthenticationErrorDetails(
                "Autenticação rejeitada",
                reason == null || reason.isBlank()
                        ? "O servidor recusou a tentativa de autenticação por uma regra de segurança ou de acesso."
                        : "O servidor recusou a tentativa de autenticação: " + reason,
                "Verifique os dados introduzidos e tente novamente. Se o problema persistir, peça a um administrador autorizado para consultar a política de segurança da conta, o estado do utilizador e os registos de auditoria.",
                Feather.ALERT_TRIANGLE,
                ModalManager.ModalTone.DANGER,
                Color.web("#b91c1c"),
                false,
                true,
                false
        );
    }

    private record AuthenticationErrorDetails(
            String title,
            String explanation,
            String resolution,
            Feather icon,
            ModalManager.ModalTone tone,
            Color toneColor,
            boolean focusEmail,
            boolean focusPassword,
            boolean focusMfa
    ) {}

    private void showMandatoryPasswordChange(
            User user,
            String currentPassword
    ) {
        PasswordField newPassword = new PasswordField();
        newPassword.setPromptText(
                "Nova palavra-passe (mín. 8 caracteres)"
        );
        newPassword.setPrefHeight(46);

        PasswordField confirmPassword = new PasswordField();
        confirmPassword.setPromptText(
                "Confirmar nova palavra-passe"
        );
        confirmPassword.setPrefHeight(46);

        Label policy = new Label(
                "A nova palavra-passe precisa de pelo menos 8 caracteres, "
                        + "com maiúsculas, minúsculas e números."
        );
        policy.setWrapText(true);
        policy.setStyle(
                "-fx-font-size: 11px;"
                        + "-fx-text-fill: #64748b;"
        );

        Label status = new Label();
        status.setWrapText(true);
        status.setManaged(false);
        status.setVisible(false);

        Button cancel = new Button(
                "Cancelar",
                new FontIcon(Feather.X)
        );
        cancel.getStyleClass().add("button-outlined");

        Button confirm = new Button(
                "Alterar e continuar",
                new FontIcon(Feather.CHECK)
        );
        confirm.getStyleClass().add("button-primary");
        confirm.setDefaultButton(true);

        HBox actions = new HBox(8, cancel, confirm);
        actions.setAlignment(Pos.CENTER_RIGHT);

        VBox content = new VBox(
                14,
                new Label(
                        "A credencial temporária foi validada. "
                                + "Por segurança, deve definir agora uma nova palavra-passe."
                ),
                new Label(
                        "Conta: "
                                + (user == null
                                ? "-"
                                : user.getEmail())
                ),
                new Separator(),
                new Label("Nova palavra-passe"),
                newPassword,
                new Label("Confirmar nova palavra-passe"),
                confirmPassword,
                policy,
                status,
                actions
        );
        content.setPadding(new Insets(6));
        content.setPrefWidth(500);

        cancel.setOnAction(event -> {
            modalManager.hideModal();
            showMessage(
                    "A alteração da palavra-passe é obrigatória para entrar.",
                    true
            );
        });

        confirm.setOnAction(event -> {
            String newValue = newPassword.getText() == null
                    ? ""
                    : newPassword.getText();
            String confirmation = confirmPassword.getText() == null
                    ? ""
                    : confirmPassword.getText();

            Optional<String> validation =
                    validatePassword(newValue, confirmation);

            if (validation.isPresent()) {
                status.setText(validation.get());
                status.setStyle(
                        "-fx-font-size: 11px;"
                                + "-fx-text-fill: #b91c1c;"
                );
                status.setManaged(true);
                status.setVisible(true);
                return;
            }

            setMandatoryPasswordLoading(
                    confirm,
                    cancel,
                    true
            );

            executor.submit(() -> {
                try {
                    User changed = passwordChangeService.changeOwnPassword(
                            user.getId(),
                            currentPassword,
                            newValue
                    );

                    Platform.runLater(() -> {
                        setMandatoryPasswordLoading(
                                confirm,
                                cancel,
                                false
                        );

                        modalManager.hideModal();
                        playExitAnimation(() ->
                                Platform.runLater(() ->
                                        eventPublisher.publishEvent(
                                                new LoginSuccessEvent(
                                                        this,
                                                        changed
                                                )
                                        )
                                )
                        );
                    });
                } catch (Exception ex) {
                    log.warn(
                            "Falha ao concluir alteração obrigatória de palavra-passe para {}",
                            user.getEmail(),
                            ex
                    );

                    Platform.runLater(() -> {
                        setMandatoryPasswordLoading(
                                confirm,
                                cancel,
                                false
                        );
                        status.setText(
                                "Não foi possível alterar a palavra-passe. "
                                        + "Verifique os dados e tente novamente."
                        );
                        status.setStyle(
                                "-fx-font-size: 11px;"
                                        + "-fx-text-fill: #b91c1c;"
                        );
                        status.setManaged(true);
                        status.setVisible(true);
                    });
                }
            });
        });

        modalManager.showModal(
                content,
                new ModalManager.ModalConfig()
                        .title("Primeiro acesso")
                        .subtitle(
                                "Alteração obrigatória da palavra-passe"
                        )
                        .icon(Feather.KEY)
                        .tone(ModalManager.ModalTone.WARNING)
                        .size(590, 520)
                        .minSize(520, 470)
                        .maximizable(false)
                        .minimizable(false)
                        .closeOnOverlayClick(false)
                        .closeOnEscape(false)
        );

        Platform.runLater(() -> newPassword.requestFocus());
    }

    private void setMandatoryPasswordLoading(
            Button confirm,
            Button cancel,
            boolean loadingState
    ) {
        confirm.setDisable(loadingState);
        cancel.setDisable(loadingState);
        confirm.setText(
                loadingState
                        ? "A guardar..."
                        : "Alterar e continuar"
        );
    }

    private Optional<String> validatePassword(
            String password,
            String confirmation
    ) {
        if (password.isBlank()) {
            return Optional.of(
                    "Introduza a nova palavra-passe."
            );
        }

        if (password.length() < 8) {
            return Optional.of(
                    "A nova palavra-passe deve ter pelo menos 8 caracteres."
            );
        }

        boolean upper =
                password.chars().anyMatch(Character::isUpperCase);
        boolean lower =
                password.chars().anyMatch(Character::isLowerCase);
        boolean digit =
                password.chars().anyMatch(Character::isDigit);

        if (!upper || !lower || !digit) {
            return Optional.of(
                    "Use pelo menos uma maiúscula, uma minúscula e um número."
            );
        }

        if (!password.equals(confirmation)) {
            return Optional.of(
                    "A confirmação da palavra-passe não coincide."
            );
        }

        return Optional.empty();
    }

    private void showRecoveryInfo() {
        VBox content = new VBox(12);
        content.setPadding(new Insets(6));

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        FontIcon icon = new FontIcon(Feather.LIFE_BUOY);
        icon.setIconSize(22);
        icon.setIconColor(Color.web("#217346"));

        VBox textBox = new VBox(3);
        Label title = new Label("Recuperação de acesso");
        title.setStyle(
                "-fx-font-size: 16px;"
                        + "-fx-font-weight: 800;"
                        + "-fx-text-fill: #1e293b;"
        );

        Label body = new Label(
                "A recuperação automática por email não está activa "
                        + "neste ambiente. Para uma conta administrativa, "
                        + "contacte outro administrador autorizado para efectuar "
                        + "uma redefinição através do módulo Utilizadores."
        );
        body.setWrapText(true);
        body.setStyle(
                "-fx-font-size: 13px;"
                        + "-fx-text-fill: #64748b;"
        );

        textBox.getChildren().addAll(title, body);
        header.getChildren().addAll(icon, textBox);

        VBox warning = new VBox(6);
        warning.setPadding(new Insets(12));
        warning.setStyle(
                "-fx-background-color: #fff8eb;"
                        + "-fx-background-radius: 10;"
                        + "-fx-border-color: #f5ddb0;"
                        + "-fx-border-radius: 10;"
                        + "-fx-border-width: 1;"
        );

        Label warningLabel = new Label(
                "Por segurança, o Kubata não apresenta senhas, "
                        + "tokens ou códigos de recuperação nesta tela."
        );
        warningLabel.setWrapText(true);
        warningLabel.setStyle(
                "-fx-font-size: 11px;"
                        + "-fx-text-fill: #8a5a00;"
        );

        warning.getChildren().add(warningLabel);

        content.getChildren().addAll(
                header,
                warning
        );

        modalManager.showModal(
                content,
                new ModalManager.ModalConfig()
                        .title("Recuperar acesso")
                        .icon(Feather.LOCK)
                        .tone(ModalManager.ModalTone.INFO)
                        .singleButton("Fechar")
                        .size(560, 310)
                        .minSize(500, 280)
                        .maximizable(false)
                        .minimizable(false)
        );
    }

    private void showSecurityHelp() {
        VBox content = new VBox(12);
        content.setPadding(new Insets(6));

        content.getChildren().addAll(
                securityInfo(
                        Feather.KEY,
                        "Palavra-passe",
                        "Nunca partilhe a sua credencial. "
                                + "Senhas provisórias precisam de ser trocadas "
                                + "antes do acesso normal."
                ),
                securityInfo(
                        Feather.SHIELD,
                        "MFA",
                        "Quando a conta usa MFA, introduza o código "
                                + "de 6 dígitos no campo correspondente."
                ),
                securityInfo(
                        Feather.LOCK,
                        "Bloqueio",
                        "Tentativas falhadas podem activar o bloqueio "
                                + "temporário definido pelo administrador."
                ),
                securityInfo(
                        Feather.ACTIVITY,
                        "Auditoria",
                        "Operações administrativas relevantes são "
                                + "registadas para rastreabilidade."
                )
        );

        modalManager.showModal(
                content,
                new ModalManager.ModalConfig()
                        .title("Segurança do acesso")
                        .icon(Feather.SHIELD)
                        .tone(ModalManager.ModalTone.INFO)
                        .singleButton("Fechar")
                        .size(600, 470)
                        .minSize(520, 420)
                        .maximizable(false)
                        .minimizable(false)
        );
    }

    private VBox securityInfo(
            Feather icon,
            String title,
            String description
    ) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.TOP_LEFT);
        row.setPadding(new Insets(10));
        row.setStyle(
                "-fx-background-color: #f8fafc;"
                        + "-fx-background-radius: 10;"
        );

        FontIcon itemIcon = new FontIcon(icon);
        itemIcon.setIconSize(17);
        itemIcon.setIconColor(Color.web("#217346"));

        VBox texts = new VBox(3);

        Label itemTitle = new Label(title);
        itemTitle.setStyle(
                "-fx-font-size: 13px;"
                        + "-fx-font-weight: 800;"
                        + "-fx-text-fill: #334155;"
        );

        Label itemDescription = new Label(description);
        itemDescription.setWrapText(true);
        itemDescription.setStyle(
                "-fx-font-size: 11px;"
                        + "-fx-text-fill: #64748b;"
        );

        texts.getChildren().addAll(
                itemTitle,
                itemDescription
        );

        row.getChildren().addAll(
                itemIcon,
                texts
        );

        return new VBox(row);
    }

    private void persistRememberedEmail(String email) {
        if (rememberEmail.isSelected()) {
            preferences.put(PREF_EMAIL, email);
        } else {
            preferences.remove(PREF_EMAIL);
        }
    }

    private String resolveSourceIp() {
        try {
            String hostAddress =
                    InetAddress.getLocalHost().getHostAddress();

            return hostAddress == null || hostAddress.isBlank()
                    ? "127.0.0.1"
                    : hostAddress;
        } catch (Exception ex) {
            log.debug(
                    "Não foi possível resolver o IP local.",
                    ex
            );
            return "127.0.0.1";
        }
    }

    private void setLoading(boolean on) {
        loginButton.setDisable(on);
        loading.setVisible(on);
        loading.setManaged(on);

        emailField.setDisable(on);
        passwordField.setDisable(on);
        passwordVisibleField.setDisable(on);
        mfaCodeField.setDisable(on);
        showPasswordBtn.setDisable(on);
        rememberEmail.setDisable(on);
        forgotPasswordLink.setDisable(on);
        helpButton.setDisable(on);

        loginButton.setOpacity(on ? 0 : 1);
    }

    private void showMessage(String message, boolean error) {
        if (message == null || message.isBlank()) {
            messageLabel.setText("");
            messageLabel.setVisible(false);
            messageLabel.setManaged(false);
            return;
        }

        messageLabel.setText(message);
        messageLabel.setVisible(true);
        messageLabel.setManaged(true);
        messageLabel.setPadding(new Insets(10, 12, 10, 12));
        messageLabel.setStyle(
                error
                        ? "-fx-background-color: #fef2f2;"
                        + "-fx-text-fill: #b91c1c;"
                        + "-fx-border-color: #fecaca;"
                        + "-fx-border-radius: 8;"
                        + "-fx-background-radius: 8;"
                        + "-fx-font-size: 12px;"
                        : "-fx-background-color: #f0fdf4;"
                        + "-fx-text-fill: #166534;"
                        + "-fx-border-color: #bbf7d0;"
                        + "-fx-border-radius: 8;"
                        + "-fx-background-radius: 8;"
                        + "-fx-font-size: 12px;"
        );
    }

    private void animateEntrance(Node node) {
        node.setOpacity(0);
        node.setTranslateY(18);
        node.setScaleX(0.985);
        node.setScaleY(0.985);

        FadeTransition fade =
                new FadeTransition(Duration.millis(420), node);
        fade.setFromValue(0);
        fade.setToValue(1);

        TranslateTransition translate =
                new TranslateTransition(Duration.millis(420), node);
        translate.setFromY(18);
        translate.setToY(0);

        ScaleTransition scale =
                new ScaleTransition(Duration.millis(420), node);
        scale.setFromX(0.985);
        scale.setFromY(0.985);
        scale.setToX(1);
        scale.setToY(1);

        new ParallelTransition(
                fade,
                translate,
                scale
        ).play();
    }

    private void playExitAnimation(Runnable onFinished) {
        FadeTransition fade =
                new FadeTransition(Duration.millis(220), emailField.getScene().getRoot());

        fade.setFromValue(1);
        fade.setToValue(0);
        fade.setOnFinished(event -> {
            if (onFinished != null) {
                onFinished.run();
            }
        });
        fade.play();
    }

    private void shakeNode(Node node) {
        TranslateTransition shake =
                new TranslateTransition(Duration.millis(45), node);
        shake.setFromX(-6);
        shake.setToX(6);
        shake.setCycleCount(4);
        shake.setAutoReverse(true);
        shake.setOnFinished(event ->
                node.setTranslateX(0)
        );
        shake.play();
    }
}

package ao.allon.kubata.platform.login;

import ao.allon.kubata.core.domain.User;
import ao.allon.kubata.core.exception.AuthenticationException;
import ao.allon.kubata.core.exception.PasswordChangeRequiredException;
import ao.allon.kubata.core.service.AuthService;
import ao.allon.kubata.core.service.PasswordChangeService;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.InetAddress;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;

/**
 * Login central da plataforma Kubata.
 *
 * <p>Todos os clientes desktop (Administrator, RH, Faturação e futuros
 * módulos) usam o mesmo fluxo de identidade no Core. A aplicação de destino
 * é apenas um contexto de autorização; não existe uma base de credenciais
 * independente por módulo.</p>
 */
public final class CentralLoginController {

    private static final String PREF_NODE = "ao.allon.kubata.platform.login";
    private static final String PREF_EMAIL = "rememberedEmail";
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@([A-Za-z0-9-]+\\.)+[A-Za-z]{2,}$");

    private final AuthService authService;
    private final PasswordChangeService passwordChangeService;
    private final String applicationKey;
    private final String applicationName;
    private final Consumer<User> authenticatedHandler;
    private final Preferences preferences = Preferences.userRoot().node(PREF_NODE);

    private final java.util.concurrent.ExecutorService executor =
            java.util.concurrent.Executors.newCachedThreadPool(r -> {
                Thread thread = new Thread(r, "kubata-central-login");
                thread.setDaemon(true);
                return thread;
            });

    private Stage stage;
    private StackPane root;
    private StackPane contentHost;
    private TextField emailField;
    private PasswordField passwordField;
    private TextField visiblePasswordField;
    private TextField mfaField;
    private ToggleButton showPasswordButton;
    private CheckBox rememberEmail;
    private Label messageLabel;
    private Label contextLabel;
    private Button submitButton;
    private ProgressIndicator loading;
    private Hyperlink recoveryModeLink;
    private Label mfaHintLabel;
    private boolean recoveryMode;
    private Rectangle clip;
    private boolean maximized;
    private double restoreX;
    private double restoreY;
    private double restoreWidth;
    private double restoreHeight;
    private double dragX;
    private double dragY;

    private User pendingPasswordUser;
    private String pendingPassword;

    public CentralLoginController(
            AuthService authService,
            PasswordChangeService passwordChangeService,
            String applicationKey,
            String applicationName,
            Consumer<User> authenticatedHandler) {

        this.authService = authService;
        this.passwordChangeService = passwordChangeService;
        this.applicationKey = normalizeApplicationKey(applicationKey);
        this.applicationName =
                applicationName == null || applicationName.isBlank()
                        ? this.applicationKey
                        : applicationName.trim();
        this.authenticatedHandler = authenticatedHandler;
    }

    public StackPane createView(Stage stage) {
        this.stage = stage;

        if (stage == null) {
            throw new IllegalArgumentException("O Stage do Login Central não pode ser nulo.");
        }

        root = new StackPane();
        root.setPrefSize(1120, 720);
        root.setMinSize(900, 560);
        root.setStyle(
                "-fx-background-color: transparent;"
                        + "-fx-font-family: 'Segoe UI', 'Calibri', 'Arial', sans-serif;"
                        + "-fx-font-size: 13px;"
                        + "-fx-accent: #217346;"
                        + "-fx-focus-color: #66BB6A;"
                        + "-fx-faint-focus-color: rgba(33,115,70,0.12);"
        );

        clip = new Rectangle();
        clip.widthProperty().bind(root.widthProperty());
        clip.heightProperty().bind(root.heightProperty());
        clip.setArcWidth(28);
        clip.setArcHeight(28);
        root.setClip(clip);

        Region background = new Region();
        background.setStyle("-fx-background-color: #F5F5F5;");

        root.getChildren().add(background);

        installDragging();
        contentHost = new StackPane();
        contentHost.setPadding(new Insets(28));
        root.getChildren().add(contentHost);

        buildLoginPage();

        // O controlador central constrói apenas o conteúdo. A aplicação
        // consumidora é responsável por criar/atribuir a Scene ao Stage.
        // Isto permite trocar Login ↔ Main sem tentar reutilizar o mesmo
        // root em duas Scene diferentes.
        stage.setWidth(1120);
        stage.setHeight(720);
        stage.setMinWidth(920);
        stage.setMinHeight(600);
        stage.centerOnScreen();

        root.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case ENTER -> {
                    if (submitButton != null && !submitButton.isDisabled()) {
                        submit();
                    }
                }
                case ESCAPE -> {
                    if (recoveryMode) {
                        setRecoveryMode(false);
                    }
                }
            }
        });

        Platform.runLater(() -> emailField.requestFocus());
        return root;
    }

    private void buildLoginPage() {
        VBox shell = new VBox();
        shell.setMaxSize(980, 610);
        shell.setMinSize(860, 520);
        shell.setStyle(
                "-fx-background-color: #FFFFFF;"
                        + "-fx-background-radius: 6;"
                        + "-fx-border-color: #D0D7DE;"
                        + "-fx-border-width: 1;"
                        + "-fx-border-radius: 6;"
        );
        shell.setEffect(new DropShadow(22, Color.rgb(36, 41, 47, 0.18)));

        HBox titleBar = buildLoginTitleBar();
        HBox body = new HBox();
        body.setFillHeight(true);

        VBox branding = buildBranding();
        VBox form = buildAuthentication();
        HBox.setHgrow(form, Priority.ALWAYS);

        body.getChildren().addAll(branding, form);
        VBox.setVgrow(body, Priority.ALWAYS);

        shell.getChildren().addAll(titleBar, body);
        contentHost.getChildren().setAll(shell);

        shell.setOpacity(0);
        shell.setTranslateY(22);

        FadeTransition fade = new FadeTransition(Duration.millis(450), shell);
        fade.setFromValue(0);
        fade.setToValue(1);
        TranslateTransition slide = new TranslateTransition(Duration.millis(450), shell);
        slide.setFromY(22);
        slide.setToY(0);
        new ParallelTransition(fade, slide).play();
    }

    private HBox buildLoginTitleBar() {
        HBox bar = new HBox();
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setSpacing(0);
        bar.setPrefHeight(40);
        bar.setMinHeight(40);
        bar.setMaxHeight(40);
        bar.setPadding(new Insets(0, 8, 0, 0));
        bar.setStyle(
                "-fx-background-color: #1E6B3C;"
                        + "-fx-background-radius: 5 5 0 0;"
        );

        FontIcon appIcon = new FontIcon(Feather.FILE_TEXT);
        appIcon.setIconSize(18);
        appIcon.setIconColor(Color.WHITE);

        StackPane iconBox = new StackPane(appIcon);
        iconBox.setPadding(new Insets(0, 6, 0, 12));

        Label appName = new Label(applicationName);
        appName.setStyle(
                "-fx-text-fill: rgba(255,255,255,0.90);"
                        + "-fx-font-size: 13px;"
                        + "-fx-font-family: 'Segoe UI Semibold', 'Segoe UI', 'Calibri', sans-serif;"
                        + "-fx-font-weight: bold;"
                        + "-fx-padding: 0 16 0 4;"
        );

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        bar.getChildren().addAll(
                iconBox,
                appName,
                spacer,
                buildWindowButtons()
        );
        return bar;
    }

    private VBox buildBranding() {
        VBox box = new VBox(14);
        box.setPrefWidth(345);
        box.setMinWidth(320);
        box.setPadding(new Insets(34, 36, 32, 36));
        box.setAlignment(Pos.TOP_LEFT);
        box.setStyle(
                "-fx-background-color: #E8F5E9;"
                        + "-fx-background-radius: 0 0 0 5;"
                        + "-fx-border-color: transparent #A5D6A7 transparent transparent;"
                        + "-fx-border-width: 0 1 0 0;"
        );

        StackPane emblem = new StackPane();
        emblem.setPrefSize(62, 62);
        emblem.setMaxSize(62, 62);
        emblem.setStyle(
                "-fx-background-color: #FFFFFF;"
                        + "-fx-background-radius: 6;"
                        + "-fx-border-color: #A5D6A7;"
                        + "-fx-border-radius: 6;"
                        + "-fx-border-width: 1;"
        );

        FontIcon shield = new FontIcon(Feather.SHIELD);
        shield.setIconSize(32);
        shield.setIconColor(Color.web("#217346"));
        emblem.getChildren().add(shield);

        Label kubata = new Label("KUBATA");
        kubata.setStyle(
                "-fx-font-family: 'Segoe UI Semibold', 'Segoe UI', 'Calibri', sans-serif;"
                        + "-fx-font-size: 30px;"
                        + "-fx-font-weight: 800;"
                        + "-fx-text-fill: #1E6B3C;"
                        + "-fx-letter-spacing: 2px;"
        );

        Label platform = new Label("Plataforma Empresarial");
        platform.setStyle(
                "-fx-font-size: 15px;"
                        + "-fx-font-weight: 700;"
                        + "-fx-text-fill: #1A5C35;"
        );

        Region divider = new Region();
        divider.setPrefHeight(1);
        divider.setMaxWidth(Double.MAX_VALUE);
        divider.setStyle("-fx-background-color: #A5D6A7;");

        Label description = new Label(
                "Autenticação central para todas as aplicações Kubata, "
                        + "com controlo de identidade, sessão, MFA e permissões."
        );
        description.setWrapText(true);
        description.setMaxWidth(270);
        description.setStyle(
                "-fx-font-size: 12px;"
                        + "-fx-line-spacing: 2;"
                        + "-fx-text-fill: #57606A;"
        );

        VBox contextCard = new VBox(5);
        contextCard.setPadding(new Insets(12));
        contextCard.setMaxWidth(275);
        contextCard.setStyle(
                "-fx-background-color: #FFFFFF;"
                        + "-fx-background-radius: 4;"
                        + "-fx-border-color: #D0D7DE;"
                        + "-fx-border-radius: 4;"
                        + "-fx-border-width: 1;"
        );

        Label contextTitle = new Label("APLICAÇÃO");
        contextTitle.setStyle(
                "-fx-font-size: 9px;"
                        + "-fx-font-weight: 800;"
                        + "-fx-text-fill: #6E7781;"
        );

        contextLabel = new Label(applicationName);
        contextLabel.setStyle(
                "-fx-font-size: 14px;"
                        + "-fx-font-weight: 800;"
                        + "-fx-text-fill: #24292F;"
        );
        contextLabel.setWrapText(true);

        contextCard.getChildren().addAll(contextTitle, contextLabel);

        Label footer = new Label(
                "Kubata Platform • sessão segura"
        );
        footer.setStyle(
                "-fx-font-size: 10px;"
                        + "-fx-text-fill: #6E7781;"
        );

        box.getChildren().addAll(
                emblem,
                kubata,
                platform,
                divider,
                description,
                contextCard,
                footer
        );
        return box;
    }

    private VBox buildAuthentication() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(30, 42, 28, 42));
        box.setAlignment(Pos.TOP_LEFT);
        box.setStyle(
                "-fx-background-color: #FFFFFF;"
                        + "-fx-background-radius: 0 0 5 0;"
        );

        Label title = new Label("Iniciar sessão");
        title.setStyle(
                "-fx-font-family: 'Segoe UI Semibold', 'Segoe UI', 'Calibri', sans-serif;"
                        + "-fx-font-size: 24px;"
                        + "-fx-font-weight: 800;"
                        + "-fx-text-fill: #24292F;"
        );

        Label section = new Label(applicationName);
        section.setStyle(
                "-fx-font-size: 11px;"
                        + "-fx-font-weight: 700;"
                        + "-fx-text-fill: #217346;"
        );

        Label subtitle = new Label(
                "Use a sua conta Kubata para entrar na aplicação seleccionada."
        );
        subtitle.setStyle(
                "-fx-font-size: 12px;"
                        + "-fx-text-fill: #57606A;"
        );
        subtitle.setWrapText(true);

        VBox emailBox = fieldBox("Email");
        emailField = new TextField();
        emailField.setPromptText("nome@empresa.ao");
        styleField(emailField);
        emailBox.getChildren().add(emailField);

        VBox passwordBox = fieldBox("Palavra-passe");
        HBox passwordLine = new HBox(8);

        passwordField = new PasswordField();
        passwordField.setPromptText("Introduza a sua palavra-passe");
        styleField(passwordField);
        HBox.setHgrow(passwordField, Priority.ALWAYS);

        visiblePasswordField = new TextField();
        visiblePasswordField.setPromptText("Introduza a sua palavra-passe");
        styleField(visiblePasswordField);
        visiblePasswordField.setVisible(false);
        visiblePasswordField.setManaged(false);
        visiblePasswordField.textProperty()
                .bindBidirectional(passwordField.textProperty());
        HBox.setHgrow(visiblePasswordField, Priority.ALWAYS);

        showPasswordButton = new ToggleButton("Mostrar", new FontIcon(Feather.EYE));
        showPasswordButton.setTooltip(
                new Tooltip("Mostrar ou ocultar a palavra-passe")
        );
        showPasswordButton.setCursor(Cursor.HAND);
        showPasswordButton.setPrefHeight(40);
        showPasswordButton.setStyle(
                "-fx-background-color: #FFFFFF;"
                        + "-fx-border-color: #D0D7DE;"
                        + "-fx-border-width: 1;"
                        + "-fx-border-radius: 4;"
                        + "-fx-background-radius: 4;"
                        + "-fx-text-fill: #57606A;"
                        + "-fx-font-size: 11px;"
                        + "-fx-font-weight: 700;"
                        + "-fx-padding: 0 10;"
        );
        showPasswordButton.getStyleClass().add("kubata-login-small-button");
        showPasswordButton.selectedProperty().addListener((obs, oldValue, selected) -> {
            showPasswordButton.setText(selected ? "Ocultar" : "Mostrar");
            showPasswordButton.setGraphic(
                    new FontIcon(selected ? Feather.EYE_OFF : Feather.EYE)
            );
            passwordField.setVisible(!selected);
            passwordField.setManaged(!selected);
            visiblePasswordField.setVisible(selected);
            visiblePasswordField.setManaged(selected);
            (selected ? visiblePasswordField : passwordField).requestFocus();
        });

        passwordLine.getChildren().addAll(passwordField, visiblePasswordField, showPasswordButton);
        passwordBox.getChildren().add(passwordLine);

        VBox mfaBox = fieldBox("MFA");
        HBox mfaHeader = new HBox(8);
        mfaHeader.setAlignment(Pos.CENTER_LEFT);

        mfaHintLabel = new Label("Preencha apenas quando a conta exigir MFA.");
        mfaHintLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #94a3b8;");
        Region mfaSpacer = new Region();
        HBox.setHgrow(mfaSpacer, Priority.ALWAYS);

        recoveryModeLink = new Hyperlink("Usar recuperação");
        recoveryModeLink.setStyle(
                "-fx-font-size: 10px;"
                        + "-fx-font-weight: 700;"
                        + "-fx-text-fill: #217346;"
        );
        recoveryModeLink.setOnAction(e -> setRecoveryMode(!recoveryMode));

        mfaHeader.getChildren().addAll(mfaHintLabel, mfaSpacer, recoveryModeLink);

        mfaField = new TextField();
        styleField(mfaField);
        setRecoveryMode(false);

        mfaBox.getChildren().addAll(mfaHeader, mfaField);

        rememberEmail = new CheckBox("Lembrar email neste computador");
        rememberEmail.setStyle(
                "-fx-font-size: 11px;"
                        + "-fx-text-fill: #57606A;"
        );

        String remembered = preferences.get(PREF_EMAIL, "");
        if (!remembered.isBlank()) {
            emailField.setText(remembered);
            rememberEmail.setSelected(true);
        }

        messageLabel = new Label();
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(Double.MAX_VALUE);
        messageLabel.setManaged(false);
        messageLabel.setVisible(false);
        messageLabel.setStyle(
                "-fx-font-size: 11px;"
                        + "-fx-font-weight: 700;"
                        + "-fx-text-fill: #b91c1c;"
        );

        submitButton = new Button(
                "ENTRAR NO KUBATA",
                new FontIcon(Feather.LOG_IN)
        );
        submitButton.setPrefHeight(42);
        submitButton.setMaxWidth(Double.MAX_VALUE);
        submitButton.setCursor(Cursor.HAND);
        submitButton.setDefaultButton(true);
        submitButton.setStyle(
                "-fx-background-color: #217346;"
                        + "-fx-background-radius: 4;"
                        + "-fx-border-color: #217346;"
                        + "-fx-border-radius: 4;"
                        + "-fx-text-fill: white;"
                        + "-fx-font-size: 12px;"
                        + "-fx-font-weight: 800;"
                        + "-fx-padding: 0 16;"
        );
        submitButton.setOnAction(e -> submit());

        loading = new ProgressIndicator();
        loading.setMaxSize(24, 24);
        loading.setVisible(false);
        loading.setManaged(false);

        StackPane action = new StackPane(submitButton, loading);

        Label security = new Label(
                "Protegido pelo Core: password, MFA, bloqueio, sessão, empresa e permissões."
        );
        security.setWrapText(true);
        security.setStyle(
                "-fx-font-size: 10px;"
                        + "-fx-text-fill: #94a3b8;"
        );

        VBox.setVgrow(security, Priority.NEVER);

        box.getChildren().addAll(
                section,
                title,
                subtitle,
                emailBox,
                passwordBox,
                mfaBox,
                rememberEmail,
                messageLabel,
                action,
                security
        );

        return box;
    }

    private VBox fieldBox(String title) {
        VBox box = new VBox(6);
        Label label = new Label(title);
        label.setStyle(
                "-fx-font-size: 11px;"
                        + "-fx-font-weight: 700;"
                        + "-fx-text-fill: #424A53;"
        );
        box.getChildren().add(label);
        return box;
    }

    private void styleField(TextField field) {
        field.setPrefHeight(40);
        field.setStyle(
                "-fx-background-color: #FFFFFF;"
                        + "-fx-border-color: #D0D7DE;"
                        + "-fx-border-width: 1;"
                        + "-fx-border-radius: 4;"
                        + "-fx-background-radius: 4;"
                        + "-fx-padding: 0 11;"
                        + "-fx-font-size: 12px;"
        );
    }

    private HBox buildWindowButtons() {
        HBox bar = new HBox(2);
        bar.setAlignment(Pos.CENTER_RIGHT);

        Button minimize = windowButton(Feather.MINUS, "Minimizar", () -> stage.setIconified(true));
        Button maximize = windowButton(
                Feather.MAXIMIZE_2,
                "Maximizar",
                () -> toggleMaximize()
        );
        Button close = windowButton(
                Feather.X,
                "Fechar",
                () -> stage.close()
        );
        close.setOnMouseEntered(e -> close.setStyle(
                "-fx-background-color: #C42B1C;"
                        + "-fx-text-fill: #FFFFFF;"
                        + "-fx-min-width: 46px;"
                        + "-fx-pref-width: 46px;"
                        + "-fx-min-height: 40px;"
                        + "-fx-pref-height: 40px;"
                        + "-fx-background-radius: 0;"
                        + "-fx-border-color: transparent;"
                        + "-fx-padding: 0;"
        ));
        close.setOnMouseExited(e -> close.setStyle(
                "-fx-background-color: transparent;"
                        + "-fx-text-fill: rgba(255,255,255,0.82);"
                        + "-fx-min-width: 46px;"
                        + "-fx-pref-width: 46px;"
                        + "-fx-min-height: 40px;"
                        + "-fx-pref-height: 40px;"
                        + "-fx-background-radius: 0;"
                        + "-fx-border-color: transparent;"
                        + "-fx-padding: 0;"
        ));

        bar.getChildren().addAll(minimize, maximize, close);
        return bar;
    }

    private Button windowButton(Feather icon, String text, Runnable action) {
        FontIcon glyph = new FontIcon(icon);
        glyph.setIconSize(13);
        glyph.setIconColor(Color.WHITE);

        Button button = new Button("", glyph);
        button.setAccessibleText(text);
        button.setCursor(Cursor.HAND);
        button.setFocusTraversable(false);
        button.setStyle(
                "-fx-background-color: transparent;"
                        + "-fx-text-fill: rgba(255,255,255,0.82);"
                        + "-fx-min-width: 46px;"
                        + "-fx-pref-width: 46px;"
                        + "-fx-min-height: 40px;"
                        + "-fx-pref-height: 40px;"
                        + "-fx-background-radius: 0;"
                        + "-fx-border-color: transparent;"
                        + "-fx-padding: 0;"
        );
        button.setOnMouseEntered(e -> button.setStyle(
                "-fx-background-color: rgba(255,255,255,0.15);"
                        + "-fx-text-fill: #FFFFFF;"
                        + "-fx-min-width: 46px;"
                        + "-fx-pref-width: 46px;"
                        + "-fx-min-height: 40px;"
                        + "-fx-pref-height: 40px;"
                        + "-fx-background-radius: 0;"
                        + "-fx-border-color: transparent;"
                        + "-fx-padding: 0;"
        ));
        button.setOnMouseExited(e -> button.setStyle(
                "-fx-background-color: transparent;"
                        + "-fx-text-fill: rgba(255,255,255,0.82);"
                        + "-fx-min-width: 46px;"
                        + "-fx-pref-width: 46px;"
                        + "-fx-min-height: 40px;"
                        + "-fx-pref-height: 40px;"
                        + "-fx-background-radius: 0;"
                        + "-fx-border-color: transparent;"
                        + "-fx-padding: 0;"
        ));
        button.setOnAction(e -> action.run());
        return button;
    }

    private void installDragging() {
        root.setOnMousePressed(e -> {
            dragX = e.getSceneX();
            dragY = e.getSceneY();
        });

        root.setOnMouseDragged(e -> {
            if (maximized) {
                return;
            }
            if (e.getTarget() instanceof javafx.scene.control.Control) {
                return;
            }
            stage.setX(e.getScreenX() - dragX);
            stage.setY(e.getScreenY() - dragY);
        });
    }

    private void toggleMaximize() {
        if (maximized) {
            stage.setX(restoreX);
            stage.setY(restoreY);
            stage.setWidth(restoreWidth);
            stage.setHeight(restoreHeight);
            maximized = false;
            clip.setArcWidth(28);
            clip.setArcHeight(28);
            return;
        }

        restoreX = stage.getX();
        restoreY = stage.getY();
        restoreWidth = stage.getWidth();
        restoreHeight = stage.getHeight();

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
        maximized = true;
        clip.setArcWidth(0);
        clip.setArcHeight(0);
    }

    private void setRecoveryMode(boolean enabled) {
        recoveryMode = enabled;
        if (mfaField == null) {
            return;
        }

        mfaField.clear();
        if (enabled) {
            mfaField.setPromptText("ABCD-EFGH-JKLM");
            mfaHintLabel.setText("Código de recuperação de uso único.");
            recoveryModeLink.setText("Usar MFA");
            mfaField.setTextFormatter(
                    new TextFormatter<String>(change ->
                            change.getControlNewText()
                                    .toUpperCase(Locale.ROOT)
                                    .matches("[A-HJ-NP-Z2-9-]{0,14}")
                                    ? change
                                    : null
                    )
            );
        } else {
            mfaField.setPromptText("Código de 6 dígitos");
            mfaHintLabel.setText("Preencha apenas quando a conta exigir MFA.");
            recoveryModeLink.setText("Usar recuperação");
            mfaField.setTextFormatter(
                    new TextFormatter<String>(change ->
                            change.getControlNewText().matches("\\d{0,6}")
                                    ? change
                                    : null
                    )
            );
        }
    }

    private void submit() {
        String email = Optional.ofNullable(emailField.getText()).orElse("").trim();
        String password = Optional.ofNullable(passwordField.getText()).orElse("");
        String factor = Optional.ofNullable(mfaField.getText()).orElse("").trim();

        if (email.isBlank() || !EMAIL_PATTERN.matcher(email).matches()) {
            showMessage("Introduza um endereço de email válido.", true);
            emailField.requestFocus();
            return;
        }

        if (password.isBlank()) {
            showMessage("Introduza a palavra-passe.", true);
            passwordField.requestFocus();
            return;
        }

        Integer mfaCode = null;
        String recoveryCode = null;

        if (!factor.isBlank()) {
            if (recoveryMode) {
                String normalized = factor.replace("-", "").toUpperCase(Locale.ROOT);
                if (normalized.length() < 12 || normalized.length() > 14) {
                    showMessage("O código de recuperação não tem um formato válido.", true);
                    mfaField.requestFocus();
                    return;
                }
                recoveryCode = factor;
            } else {
                if (!factor.matches("\\d{6}")) {
                    showMessage("O código MFA deve ter 6 dígitos.", true);
                    mfaField.requestFocus();
                    return;
                }
                mfaCode = Integer.valueOf(factor);
            }
        }

        setLoading(true);
        showMessage("", false);

        final Integer finalMfa = mfaCode;
        final String finalRecovery = recoveryCode;
        final String finalPassword = password;

        Task<User> task = new Task<>() {
            @Override
            protected User call() {
                return authService.authenticateForApplication(
                        email,
                        finalPassword,
                        finalMfa,
                        finalRecovery,
                        resolveSourceIp(),
                        applicationKey
                );
            }
        };

        task.setOnSucceeded(event -> {
            setLoading(false);
            persistRememberedEmail(email);
            User user = task.getValue();
            if (authenticatedHandler != null) {
                Platform.runLater(() -> authenticatedHandler.accept(user));
            }
        });

        task.setOnFailed(event -> {
            setLoading(false);
            Throwable error = task.getException();

            if (error instanceof PasswordChangeRequiredException required) {
                pendingPasswordUser = required.getUser();
                pendingPassword = finalPassword;
                showPasswordChangePage();
                return;
            }

            String reason =
                    error == null || error.getMessage() == null
                            ? "Não foi possível iniciar a sessão."
                            : error.getMessage();

            if (reason.toLowerCase(Locale.ROOT).contains("mfa")) {
                mfaField.requestFocus();
            }

            showMessage(reason, true);
        });

        executor.submit(task);
    }

    private void showPasswordChangePage() {
        VBox card = new VBox(14);
        card.setMaxWidth(560);
        card.setPadding(new Insets(34));
        card.setAlignment(Pos.TOP_LEFT);
        card.setStyle(
                "-fx-background-color: white;"
                        + "-fx-background-radius: 20;"
                        + "-fx-border-color: #dbe4df;"
                        + "-fx-border-radius: 20;"
                        + "-fx-border-width: 1;"
        );
        card.setEffect(new DropShadow(26, Color.rgb(15, 23, 42, 0.15)));

        Label title = new Label("Alteração obrigatória da palavra-passe");
        title.setStyle(
                "-fx-font-size: 24px;"
                        + "-fx-font-weight: 800;"
                        + "-fx-text-fill: #163725;"
        );

        Label subtitle = new Label(
                "A conta foi autenticada, mas a palavra-passe provisória "
                        + "deve ser substituída antes de continuar."
        );
        subtitle.setWrapText(true);
        subtitle.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px;");

        PasswordField newPassword = new PasswordField();
        PasswordField confirm = new PasswordField();
        styleField(newPassword);
        styleField(confirm);
        newPassword.setPromptText("Nova palavra-passe");
        confirm.setPromptText("Confirmar nova palavra-passe");

        Label hint = new Label(
                "A validação final é realizada pelo serviço de políticas do Core."
        );
        hint.setWrapText(true);
        hint.setStyle("-fx-font-size: 10px; -fx-text-fill: #94a3b8;");

        Button change = new Button(
                "GUARDAR E CONTINUAR",
                new FontIcon(Feather.CHECK)
        );
        change.setPrefHeight(48);
        change.setMaxWidth(Double.MAX_VALUE);
        change.setCursor(Cursor.HAND);
        change.setStyle(
                "-fx-background-color: #217346;"
                        + "-fx-background-radius: 10;"
                        + "-fx-text-fill: white;"
                        + "-fx-font-weight: 800;"
        );

        Label error = new Label();
        error.setWrapText(true);
        error.setVisible(false);
        error.setManaged(false);
        error.setStyle("-fx-text-fill: #b91c1c; -fx-font-size: 11px;");

        change.setOnAction(e -> {
            String first = newPassword.getText();
            String second = confirm.getText();

            if (first == null || first.isBlank()) {
                error.setText("Introduza a nova palavra-passe.");
                error.setVisible(true);
                error.setManaged(true);
                return;
            }

            if (!first.equals(second)) {
                error.setText("A confirmação da palavra-passe não coincide.");
                error.setVisible(true);
                error.setManaged(true);
                return;
            }

            change.setDisable(true);
            ProgressIndicator progress = new ProgressIndicator();
            progress.setMaxSize(22, 22);
            card.getChildren().add(progress);

            Task<User> task = new Task<>() {
                @Override
                protected User call() {
                    return passwordChangeService.changeOwnPassword(
                            pendingPasswordUser.getId(),
                            pendingPassword,
                            first
                    );
                }
            };

            task.setOnSucceeded(done -> {
                card.getChildren().remove(progress);
                pendingPasswordUser = null;

                String newValue = first;
                pendingPassword = null;
                change.setDisable(false);

                passwordField.setText(newValue);
                showMessage("Palavra-passe alterada. A autenticação será concluída automaticamente.", false);
                passwordField.setText(newValue);

                String email = emailField.getText().trim();
                Task<User> reauth = new Task<>() {
                    @Override
                    protected User call() {
                        return authService.authenticateForApplication(
                                email,
                                newValue,
                                null,
                                null,
                                resolveSourceIp(),
                                applicationKey
                        );
                    }
                };

                reauth.setOnSucceeded(doneAgain -> {
                    User user = reauth.getValue();
                    if (authenticatedHandler != null) {
                        Platform.runLater(() -> authenticatedHandler.accept(user));
                    }
                });

                reauth.setOnFailed(doneAgain -> {
                    Throwable failure = reauth.getException();
                    buildLoginPage();
                    showMessage(
                            failure == null ? "Não foi possível concluir a sessão." : failure.getMessage(),
                            true
                    );
                });

                executor.submit(reauth);
            });

            task.setOnFailed(done -> {
                card.getChildren().remove(progress);
                change.setDisable(false);
                Throwable failure = task.getException();
                error.setText(
                        failure == null || failure.getMessage() == null
                                ? "Não foi possível alterar a palavra-passe."
                                : failure.getMessage()
                );
                error.setVisible(true);
                error.setManaged(true);
            });

            executor.submit(task);
        });

        Hyperlink back = new Hyperlink("Voltar ao login");
        back.setStyle(
                "-fx-font-size: 11px;"
                        + "-fx-font-weight: 700;"
                        + "-fx-text-fill: #217346;"
        );
        back.setOnAction(e -> {
            pendingPasswordUser = null;
            pendingPassword = null;
            buildLoginPage();
        });

        card.getChildren().addAll(
                title,
                subtitle,
                new Label("Nova palavra-passe"),
                newPassword,
                new Label("Confirmar"),
                confirm,
                hint,
                error,
                change,
                back
        );

        contentHost.getChildren().setAll(card);
        Platform.runLater(newPassword::requestFocus);
    }

    private void setLoading(boolean value) {
        if (submitButton == null || loading == null) {
            return;
        }
        submitButton.setVisible(!value);
        submitButton.setManaged(!value);
        loading.setVisible(value);
        loading.setManaged(value);
    }

    private void showMessage(String message, boolean error) {
        if (messageLabel == null) {
            return;
        }
        messageLabel.setText(message == null ? "" : message);
        messageLabel.setVisible(error && message != null && !message.isBlank());
        messageLabel.setManaged(messageLabel.isVisible());
        messageLabel.setStyle(
                "-fx-font-size: 11px;"
                        + "-fx-font-weight: 700;"
                        + "-fx-text-fill: "
                        + (error ? "#b91c1c;" : "#166534;")
        );
    }

    private void persistRememberedEmail(String email) {
        if (rememberEmail != null && rememberEmail.isSelected()) {
            preferences.put(PREF_EMAIL, email);
        } else {
            preferences.remove(PREF_EMAIL);
        }
    }

    private String resolveSourceIp() {
        try {
            String address = InetAddress.getLocalHost().getHostAddress();
            return address == null || address.isBlank() ? "127.0.0.1" : address;
        } catch (Exception ignored) {
            return "127.0.0.1";
        }
    }

    private String normalizeApplicationKey(String value) {
        String normalized =
                value == null || value.isBlank()
                        ? "KUBATA"
                        : value.trim().toUpperCase(Locale.ROOT);
        return normalized.replaceAll("[^A-Z0-9_\\-]", "_");
    }

    public void shutdown() {
        executor.shutdownNow();
    }
}

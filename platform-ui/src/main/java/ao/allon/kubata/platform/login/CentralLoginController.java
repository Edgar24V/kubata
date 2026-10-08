package ao.allon.kubata.platform.login;

import ao.allon.kubata.core.domain.User;
import ao.allon.kubata.core.exception.AuthenticationException;
import ao.allon.kubata.core.exception.SessionLimitExceededException;
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
    private StackPane modalLayer;
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
        root.setMinSize(920, 600);
        root.setStyle("-fx-background-color: transparent;");

        clip = new Rectangle();
        clip.widthProperty().bind(root.widthProperty());
        clip.heightProperty().bind(root.heightProperty());
        clip.setArcWidth(28);
        clip.setArcHeight(28);
        root.setClip(clip);

        Region background = new Region();
        background.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, "
                        + "#edf5f0, #f8fafc);"
        );

        root.getChildren().add(background);

        installDragging();
        contentHost = new StackPane();
        contentHost.setPadding(new Insets(32));
        root.getChildren().add(contentHost);

        modalLayer = new StackPane();
        modalLayer.setVisible(false);
        modalLayer.setManaged(false);
        modalLayer.setAlignment(Pos.CENTER);
        modalLayer.setStyle(
                "-fx-background-color: rgba(15, 23, 42, 0.46);"
        );
        modalLayer.setOnMouseClicked(event -> event.consume());
        root.getChildren().add(modalLayer);

        buildLoginPage();

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
        HBox shell = new HBox();
        shell.setMaxSize(1000, 600);
        shell.setMinSize(850, 520);
        shell.setStyle(
                "-fx-background-color: white;"
                        + "-fx-background-radius: 22;"
                        + "-fx-border-color: rgba(33,115,70,0.14);"
                        + "-fx-border-width: 1;"
                        + "-fx-border-radius: 22;"
        );
        shell.setEffect(new DropShadow(32, Color.rgb(15, 23, 42, 0.22)));

        VBox branding = buildBranding();
        VBox form = buildAuthentication();
        HBox.setHgrow(form, Priority.ALWAYS);

        shell.getChildren().addAll(branding, form);
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

    private VBox buildBranding() {
        VBox box = new VBox(18);
        box.setPrefWidth(400);
        box.setMinWidth(360);
        box.setPadding(new Insets(42));
        box.setAlignment(Pos.CENTER_LEFT);
        box.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #124d2f, #217346);"
                        + "-fx-background-radius: 20 0 0 20;"
        );

        StackPane emblem = new StackPane();
        emblem.setPrefSize(82, 82);
        emblem.setMaxSize(82, 82);
        emblem.setStyle(
                "-fx-background-color: rgba(255,255,255,0.13);"
                        + "-fx-background-radius: 24;"
                        + "-fx-border-color: rgba(255,255,255,0.22);"
                        + "-fx-border-radius: 24;"
                        + "-fx-border-width: 1;"
        );

        FontIcon shield = new FontIcon(Feather.SHIELD);
        shield.setIconSize(46);
        shield.setIconColor(Color.WHITE);
        emblem.getChildren().add(shield);

        Label kubata = new Label("KUBATA");
        kubata.setStyle(
                "-fx-font-size: 38px;"
                        + "-fx-font-weight: 900;"
                        + "-fx-text-fill: white;"
                        + "-fx-letter-spacing: 3px;"
        );

        Label platform = new Label("Plataforma Empresarial");
        platform.setStyle(
                "-fx-font-size: 18px;"
                        + "-fx-font-weight: 700;"
                        + "-fx-text-fill: rgba(255,255,255,0.94);"
        );

        Label description = new Label(
                "Uma única identidade para todas as aplicações Kubata."
        );
        description.setWrapText(true);
        description.setMaxWidth(300);
        description.setStyle(
                "-fx-font-size: 13px;"
                        + "-fx-line-spacing: 3;"
                        + "-fx-text-fill: rgba(255,255,255,0.72);"
        );

        VBox card = new VBox(8);
        card.setPadding(new Insets(15));
        card.setMaxWidth(310);
        card.setStyle(
                "-fx-background-color: rgba(255,255,255,0.10);"
                        + "-fx-background-radius: 14;"
                        + "-fx-border-color: rgba(255,255,255,0.16);"
                        + "-fx-border-radius: 14;"
                        + "-fx-border-width: 1;"
        );

        Label title = new Label("Contexto de acesso");
        title.setStyle(
                "-fx-font-size: 11px;"
                        + "-fx-font-weight: 800;"
                        + "-fx-text-fill: rgba(255,255,255,0.68);"
        );

        contextLabel = new Label(applicationName);
        contextLabel.setStyle(
                "-fx-font-size: 17px;"
                        + "-fx-font-weight: 800;"
                        + "-fx-text-fill: white;"
        );

        card.getChildren().addAll(title, contextLabel);

        Label footer = new Label(
                "Autenticação central • sessão segura • controlo por empresa e perfil"
        );
        footer.setWrapText(true);
        footer.setMaxWidth(310);
        footer.setStyle(
                "-fx-font-size: 10px;"
                        + "-fx-text-fill: rgba(255,255,255,0.52);"
        );

        box.getChildren().addAll(
                emblem,
                kubata,
                platform,
                description,
                card,
                footer
        );
        return box;
    }

    private VBox buildAuthentication() {
        VBox box = new VBox(14);
        box.setPadding(new Insets(42, 48, 40, 48));
        box.setAlignment(Pos.TOP_LEFT);
        box.setStyle(
                "-fx-background-color: white;"
                        + "-fx-background-radius: 0 20 20 0;"
        );

        HBox titleLine = new HBox();
        titleLine.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Iniciar sessão");
        title.setStyle(
                "-fx-font-size: 29px;"
                        + "-fx-font-weight: 800;"
                        + "-fx-text-fill: #163725;"
        );

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox windowButtons = buildWindowButtons();
        titleLine.getChildren().addAll(title, spacer, windowButtons);

        Label subtitle = new Label(
                "Use a sua conta Kubata para entrar na aplicação seleccionada."
        );
        subtitle.setStyle(
                "-fx-font-size: 13px;"
                        + "-fx-text-fill: #64748b;"
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
                        + "-fx-text-fill: #64748b;"
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
        submitButton.setPrefHeight(52);
        submitButton.setMaxWidth(Double.MAX_VALUE);
        submitButton.setCursor(Cursor.HAND);
        submitButton.setDefaultButton(true);
        submitButton.setStyle(
                "-fx-background-color: #217346;"
                        + "-fx-background-radius: 12;"
                        + "-fx-text-fill: white;"
                        + "-fx-font-size: 13px;"
                        + "-fx-font-weight: 800;"
                        + "-fx-padding: 0 18;"
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
                titleLine,
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
                "-fx-font-size: 12px;"
                        + "-fx-font-weight: 700;"
                        + "-fx-text-fill: #334155;"
        );
        box.getChildren().add(label);
        return box;
    }

    private void styleField(TextField field) {
        field.setPrefHeight(45);
        field.setStyle(
                "-fx-background-color: #f8fafc;"
                        + "-fx-border-color: #dbe4df;"
                        + "-fx-border-width: 1;"
                        + "-fx-border-radius: 10;"
                        + "-fx-background-radius: 10;"
                        + "-fx-padding: 0 12;"
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

        bar.getChildren().addAll(minimize, maximize, close);
        return bar;
    }

    private Button windowButton(Feather icon, String text, Runnable action) {
        Button button = new Button("", new FontIcon(icon));
        button.setAccessibleText(text);
        button.setCursor(Cursor.HAND);
        button.getStyleClass().add("kubata-login-window-button");
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

            if (error instanceof SessionLimitExceededException sessionLimit) {
                showSessionLimitModal(sessionLimit);
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

    private void showSessionLimitModal(SessionLimitExceededException exception) {
        VBox card = new VBox(12);
        card.setMaxWidth(480);
        card.setPrefWidth(480);
        card.setPadding(new Insets(20));
        card.setStyle(
                "-fx-background-color: white;"
                        + "-fx-background-radius: 16;"
                        + "-fx-border-color: #dbe4df;"
                        + "-fx-border-radius: 16;"
                        + "-fx-border-width: 1;"
        );
        card.setEffect(new DropShadow(30, Color.rgb(15, 23, 42, 0.28)));

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        StackPane iconBox = new StackPane();
        iconBox.setMinSize(42, 42);
        iconBox.setPrefSize(42, 42);
        iconBox.setStyle(
                "-fx-background-color: #fff4e5;"
                        + "-fx-background-radius: 12;"
                        + "-fx-border-color: #f6c98b;"
                        + "-fx-border-radius: 12;"
        );

        FontIcon icon = new FontIcon(Feather.LOCK);
        icon.setIconSize(20);
        icon.setIconColor(Color.web("#b45309"));
        iconBox.getChildren().add(icon);

        VBox heading = new VBox(4);
        Label title = new Label("Limite de sessões atingido");
        title.setStyle(
                "-fx-font-size: 18px;"
                        + "-fx-font-weight: 800;"
                        + "-fx-text-fill: #163725;"
        );

        Label subtitle = new Label(
                "A autenticação está correcta, mas esta conta já tem o número máximo de sessões simultâneas permitido."
        );
        subtitle.setWrapText(true);
        subtitle.setStyle(
                "-fx-font-size: 11px;"
                        + "-fx-text-fill: #64748b;"
        );
        heading.getChildren().addAll(title, subtitle);

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        Button close = new Button("", new FontIcon(Feather.X));
        close.setAccessibleText("Fechar explicação");
        close.setTooltip(new Tooltip("Fechar"));
        close.setStyle(
                "-fx-background-color: transparent;"
                        + "-fx-text-fill: #64748b;"
                        + "-fx-background-radius: 8;"
                        + "-fx-padding: 10;"
        );
        close.setOnAction(event -> hideSessionLimitModal());

        header.getChildren().addAll(iconBox, heading, headerSpacer, close);

        HBox status = new HBox(10);
        status.setAlignment(Pos.CENTER_LEFT);

        VBox activeBox = new VBox(3);
        activeBox.setMaxWidth(Double.MAX_VALUE);
        activeBox.setStyle(
                "-fx-background-color: #f8faf9;"
                        + "-fx-background-radius: 12;"
                        + "-fx-border-color: #e2ebe5;"
                        + "-fx-border-radius: 12;"
                        + "-fx-padding: 10;"
        );
        Label activeCaption = new Label("SESSÕES ACTIVAS");
        activeCaption.setStyle(
                "-fx-font-size: 10px;"
                        + "-fx-font-weight: 800;"
                        + "-fx-text-fill: #7b8794;"
        );
        Label activeValue = new Label(String.valueOf(exception.getActiveSessions()));
        activeValue.setStyle(
                "-fx-font-size: 20px;"
                        + "-fx-font-weight: 800;"
                        + "-fx-text-fill: #163725;"
        );
        activeBox.getChildren().addAll(activeCaption, activeValue);

        VBox limitBox = new VBox(3);
        limitBox.setMaxWidth(Double.MAX_VALUE);
        limitBox.setStyle(
                "-fx-background-color: #edf7f0;"
                        + "-fx-background-radius: 12;"
                        + "-fx-border-color: #cfe5d5;"
                        + "-fx-border-radius: 12;"
                        + "-fx-padding: 10;"
        );
        Label limitCaption = new Label("LIMITE DA CONTA");
        limitCaption.setStyle(
                "-fx-font-size: 10px;"
                        + "-fx-font-weight: 800;"
                        + "-fx-text-fill: #53705d;"
        );
        Label limitValue = new Label(String.valueOf(exception.getMaxSessions()));
        limitValue.setStyle(
                "-fx-font-size: 20px;"
                        + "-fx-font-weight: 800;"
                        + "-fx-text-fill: #217346;"
        );
        limitBox.getChildren().addAll(limitCaption, limitValue);

        HBox.setHgrow(activeBox, Priority.ALWAYS);
        HBox.setHgrow(limitBox, Priority.ALWAYS);
        status.getChildren().addAll(activeBox, limitBox);

        VBox explanation = new VBox(8);
        Label why = new Label("Porque aconteceu?");
        why.setStyle(
                "-fx-font-size: 12px;"
                        + "-fx-font-weight: 800;"
                        + "-fx-text-fill: #1f2937;"
        );
        Label whyText = new Label(
                "A política de segurança desta conta permite no máximo "
                        + exception.getMaxSessions()
                        + " sessão"
                        + (exception.getMaxSessions() == 1 ? "" : "ões")
                        + " ao mesmo tempo. O Kubata encontrou "
                        + exception.getActiveSessions()
                        + " sessão"
                        + (exception.getActiveSessions() == 1 ? "" : "ões")
                        + " activa"
                        + (exception.getActiveSessions() == 1 ? "" : "s")
                        + " e, por segurança, não criou uma nova sessão."
        );
        whyText.setWrapText(true);
        whyText.setStyle(
                "-fx-font-size: 11px;"
                        + "-fx-text-fill: #475569;"
        );
        explanation.getChildren().addAll(why, whyText);

        VBox resolution = new VBox(8);
        Label resolve = new Label("Como resolver");
        resolve.setStyle(
                "-fx-font-size: 12px;"
                        + "-fx-font-weight: 800;"
                        + "-fx-text-fill: #1f2937;"
        );

        Label steps = new Label(
                "1. Termine a sessão antiga num dos dispositivos onde a conta ainda está aberta.\n"
                        + "2. Caso não tenha acesso a esse dispositivo, peça a um Administrador autorizado para encerrar a sessão.\n"
                        + "3. Regresse a este ecrã e tente autenticar novamente.\n"
                        + "4. O limite é individual da conta e pode ser ajustado no Perfil de Segurança do utilizador."
        );
        steps.setWrapText(true);
        steps.setStyle(
                "-fx-font-size: 11px;"
                        + "-fx-text-fill: #475569;"
                        + "-fx-line-spacing: 2px;"
        );
        resolution.getChildren().addAll(resolve, steps);

        Label operationStatus = new Label();
        operationStatus.setWrapText(true);
        operationStatus.setVisible(false);
        operationStatus.setManaged(false);
        operationStatus.setStyle(
                "-fx-font-size: 10px;"
                        + "-fx-text-fill: #9a3412;"
        );

        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button retry = new Button(
                "TENTAR NOVAMENTE",
                new FontIcon(Feather.REFRESH_CW)
        );
        retry.setPrefHeight(38);
        retry.setCursor(Cursor.HAND);
        retry.setStyle(
                "-fx-background-color: #217346;"
                        + "-fx-background-radius: 8;"
                        + "-fx-text-fill: white;"
                        + "-fx-font-size: 10px;"
                        + "-fx-font-weight: 800;"
                        + "-fx-padding: 0 12px;"
        );
        retry.setOnAction(event -> {
            hideSessionLimitModal();
            submit();
        });

        Button back = new Button("FECHAR");
        back.setPrefHeight(38);
        back.setCursor(Cursor.HAND);
        back.setStyle(
                "-fx-background-color: white;"
                        + "-fx-background-radius: 8;"
                        + "-fx-border-color: #cfdad4;"
                        + "-fx-border-radius: 8;"
                        + "-fx-text-fill: #395347;"
                        + "-fx-font-size: 10px;"
                        + "-fx-font-weight: 800;"
                        + "-fx-padding: 0 12px;"
        );
        back.setOnAction(event -> hideSessionLimitModal());

        if (exception.isAdministrator()) {
            Button release = new Button(
                    "LIBERTAR SESSÃO MAIS ANTIGA",
                    new FontIcon(Feather.UNLOCK)
            );
            release.setPrefHeight(38);
            release.setCursor(Cursor.HAND);
            release.setStyle(
                    "-fx-background-color: #fff7ed;"
                            + "-fx-background-radius: 8;"
                            + "-fx-border-color: #fed7aa;"
                            + "-fx-border-radius: 8;"
                            + "-fx-text-fill: #9a3412;"
                            + "-fx-font-size: 10px;"
                            + "-fx-font-weight: 800;"
                            + "-fx-padding: 0 12px;"
            );
            release.setTooltip(
                    new Tooltip("Encerra apenas a sessão mais antiga desta conta e tenta novamente.")
            );
            release.setOnAction(event ->
                    releaseOldestSessionAndRetry(release, operationStatus)
            );
            actions.getChildren().add(release);
        }

        actions.getChildren().addAll(back, retry);
        card.getChildren().addAll(
                header,
                status,
                explanation,
                resolution,
                operationStatus,
                actions
        );

        modalLayer.getChildren().setAll(card);
        modalLayer.setManaged(true);
        modalLayer.setVisible(true);
        card.setOpacity(0);
        card.setScaleX(0.97);
        card.setScaleY(0.97);

        FadeTransition fade = new FadeTransition(Duration.millis(160), card);
        fade.setFromValue(0);
        fade.setToValue(1);

        javafx.animation.ScaleTransition scale =
                new javafx.animation.ScaleTransition(Duration.millis(160), card);
        scale.setFromX(0.97);
        scale.setFromY(0.97);
        scale.setToX(1);
        scale.setToY(1);

        new ParallelTransition(fade, scale).play();
        Platform.runLater(retry::requestFocus);
    }

    private void releaseOldestSessionAndRetry(
            Button releaseButton,
            Label operationStatus) {

        String email = emailField == null ? "" : emailField.getText().trim();
        String password = passwordField == null ? "" : passwordField.getText();

        if (email.isBlank() || password.isBlank()) {
            operationStatus.setText(
                    "A palavra-passe da conta é necessária para executar esta operação."
            );
            operationStatus.setVisible(true);
            operationStatus.setManaged(true);
            return;
        }

        releaseButton.setDisable(true);
        operationStatus.setText("A libertar a sessão mais antiga…");
        operationStatus.setVisible(true);
        operationStatus.setManaged(true);

        final String ip = resolveSourceIp();

        Task<Integer> task = new Task<>() {
            @Override
            protected Integer call() {
                return authService.terminateOldestSessionForLogin(
                        email,
                        password,
                        ip
                );
            }
        };

        task.setOnSucceeded(event -> {
            int released = task.getValue() == null ? 0 : task.getValue();

            if (released > 0) {
                hideSessionLimitModal();
                submit();
                return;
            }

            releaseButton.setDisable(false);
            operationStatus.setText(
                    "Não foi encontrada uma sessão activa para libertar. Tente novamente."
            );
        });

        task.setOnFailed(event -> {
            releaseButton.setDisable(false);
            Throwable error = task.getException();
            operationStatus.setText(
                    error == null || error.getMessage() == null
                            ? "Não foi possível libertar a sessão."
                            : error.getMessage()
            );
            operationStatus.setVisible(true);
            operationStatus.setManaged(true);
        });

        executor.submit(task);
    }

    private void hideSessionLimitModal() {
        if (modalLayer == null) {
            return;
        }
        modalLayer.getChildren().clear();
        modalLayer.setVisible(false);
        modalLayer.setManaged(false);
        submitButton.requestFocus();
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

                    if (failure instanceof SessionLimitExceededException sessionLimit) {
                        showSessionLimitModal(sessionLimit);
                        return;
                    }

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

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
import javafx.stage.Stage;
import javafx.util.Duration;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
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

        installWindowDragging(root, stage);

        HBox shell = createLoginShell();
        StackPane.setAlignment(shell, Pos.CENTER);
        root.getChildren().add(shell);

        Button closeButton = new Button(
                "",
                new FontIcon(Feather.X)
        );
        closeButton.getStyleClass().addAll(
                "button-icon-small",
                "login-close-button"
        );
        closeButton.setCursor(Cursor.HAND);
        closeButton.setTooltip(new Tooltip("Fechar"));
        closeButton.setAccessibleText("Fechar janela");
        closeButton.setOnAction(event -> {
            modalManager.hideAllModals();
            stage.close();
        });

        StackPane.setAlignment(closeButton, Pos.TOP_RIGHT);
        StackPane.setMargin(closeButton, new Insets(16));
        root.getChildren().add(closeButton);

        root.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER
                    && !loginButton.isDisabled()) {
                handleLogin();
            }
        });

        animateEntrance(shell);
        Platform.runLater(() -> emailField.requestFocus());

        return root;
    }

    private void installWindowDragging(StackPane root, Stage stage) {
        root.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });

        root.setOnMouseDragged(event -> {
            if (event.getTarget() instanceof Control
                    && !(event.getTarget() instanceof Label)) {
                return;
            }
            stage.setX(event.getScreenX() - xOffset);
            stage.setY(event.getScreenY() - yOffset);
        });
    }

    private HBox createLoginShell() {
        HBox shell = new HBox();
        shell.setPrefSize(1000, 610);
        shell.setMaxSize(1000, 610);
        shell.setMinSize(920, 560);
        shell.getStyleClass().add("login-card");
        shell.setEffect(new DropShadow(
                35,
                Color.rgb(15, 23, 42, 0.24)
        ));

        shell.getChildren().addAll(
                createBrandingPanel(),
                createAuthenticationPanel()
        );

        return shell;
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
                        + "-fx-background-radius: 22 0 0 22;"
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
                        + "-fx-background-radius: 0 22 22 0;"
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
        mfaHintLabel = new Label("Opcional quando o MFA não está activo");
        mfaHintLabel.setStyle(
                "-fx-font-size: 11px;"
                        + "-fx-text-fill: #94a3b8;"
        );

        mfaHeader.getChildren().addAll(
                mfaLabel,
                mfaHintLabel
        );

        mfaCodeField = new TextField();
        mfaCodeField.setPromptText("Código de 6 dígitos");
        mfaCodeField.setPrefHeight(46);
        mfaCodeField.getStyleClass().add("modern-text-field");

        mfaCodeField.setTextFormatter(
                new TextFormatter<String>(change ->
                        change.getControlNewText().matches("\\d{0,6}")
                                ? change
                                : null
                )
        );

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
        if (!mfaRaw.isBlank()) {
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

        setLoading(true);
        showMessage("", false);

        final Integer finalMfaCode = mfaCode;
        final String sourceIp = resolveSourceIp();

        Task<User> task = new Task<>() {
            @Override
            protected User call() {
                return authService.authenticate(
                        email,
                        password,
                        finalMfaCode,
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

            if (error instanceof AuthenticationException
                    && error.getMessage() != null
                    && error.getMessage()
                    .toLowerCase()
                    .contains("mfa")) {
                showMessage(
                        "A autenticação requer um código MFA válido.",
                        true
                );
                mfaCodeField.requestFocus();
                mfaCodeField.selectAll();
                return;
            }

            if (error != null) {
                log.warn(
                        "Falha de autenticação para {}: {}",
                        email,
                        error.getMessage()
                );
            }

            showMessage(
                    "Não foi possível iniciar a sessão. "
                            + "Verifique as credenciais e tente novamente.",
                    true
            );
            shakeNode(loginButton);
        });

        executor.submit(task);
    }

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

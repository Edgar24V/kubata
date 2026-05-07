package ao.allon.kubata.admin.controller;

import ao.allon.kubata.admin.ui.event.LoginSuccessEvent;
import ao.allon.kubata.core.domain.User;
import ao.allon.kubata.core.service.AuthService;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

@Component
public class LoginController {

    private final AuthService authService;
    private final ApplicationEventPublisher eventPublisher;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    // UI Components
    private TextField emailField;
    private PasswordField passwordField;
    private TextField passwordVisibleField;
    private Label messageLabel;
    private Button loginButton;
    private ProgressIndicator loading;
    private CheckBox rememberMe;
    private ToggleButton showPasswordBtn;

    // Window dragging offsets
    private double xOffset = 0;
    private double yOffset = 0;

    // Email validation pattern (RFC 5322 compliant simplified)
    private static final Pattern EMAIL_PATTERN = 
        Pattern.compile("^[A-Za-z0-9+_.-]+@([A-Za-z0-9-]+\\.)+[A-Za-z]{2,}$");

    public LoginController(AuthService authService, ApplicationEventPublisher eventPublisher) {
        this.authService = authService;
        this.eventPublisher = eventPublisher;
    }

    public Parent createView(Stage stage) {
        StackPane root = new StackPane();
        root.setPrefSize(960, 680);
        root.getStyleClass().add("login-container");

        // Background – Fundo sólido para evitar artefatos de transparência nativa
        Region background = new Region();
        background.setStyle("-fx-background-color: #f4f7f6; -fx-background-radius: 20px;");
        
        // Camada de gradiente Kubata
        Region gradientOverlay = new Region();
        gradientOverlay.setStyle("-fx-background-color: linear-gradient(to bottom right, #1A5C35, #217346, #2ecc71); -fx-background-radius: 20px;");
        
        root.getChildren().addAll(background, gradientOverlay);

        // Dragging logic for the whole window
        root.setOnMousePressed(e -> {
            xOffset = e.getSceneX();
            yOffset = e.getSceneY();
        });
        root.setOnMouseDragged(e -> {
            stage.setX(e.getScreenX() - xOffset);
            stage.setY(e.getScreenY() - yOffset);
        });

        // Main card
        HBox contentWrapper = createContentWrapper();
        root.getChildren().add(contentWrapper);

        // Botão de fechar posicionado de forma absoluta no canto superior direito
        Button closeBtn = new Button();
        closeBtn.setGraphic(new FontIcon(Feather.X));
        closeBtn.getStyleClass().addAll("button-icon-small", "login-close-button");
        closeBtn.setCursor(Cursor.HAND);
        closeBtn.setOnAction(e -> stage.close());
        
        StackPane.setAlignment(closeBtn, Pos.TOP_RIGHT);
        StackPane.setMargin(closeBtn, new Insets(15));
        root.getChildren().add(closeBtn);

        // Entrance animation
        animateEntrance(contentWrapper);

        // Setup default button (Enter key)
        root.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER && loginButton.isDisabled() == false) {
                handleLogin();
            }
        });

        return root;
    }

    private HBox createContentWrapper() {
        HBox wrapper = new HBox();
        wrapper.setMaxSize(960, 680);
        wrapper.getStyleClass().add("login-card");
        wrapper.setEffect(new DropShadow(30, Color.rgb(0, 0, 0, 0.2)));

        // Left side – branding
        VBox leftSide = createBrandingSide();

        // Right side – form
        VBox rightSide = createFormSide();

        wrapper.getChildren().addAll(leftSide, rightSide);
        return wrapper;
    }

    private VBox createBrandingSide() {
        VBox leftSide = new VBox(25);
        leftSide.setPrefWidth(480);
        leftSide.getStyleClass().add("login-visual-side");
        leftSide.setAlignment(Pos.CENTER);
        leftSide.setPadding(new Insets(40));
        // Degradê translúcido sobre o fundo principal
        leftSide.setStyle("-fx-background-color: linear-gradient(to bottom right, rgba(26, 92, 53, 0.9), rgba(33, 115, 70, 0.8)); " +
                         "-fx-background-radius: 20px 0px 0px 20px;");

        FontIcon logoIcon = new FontIcon(Feather.SHIELD);
        logoIcon.setIconSize(110);
        logoIcon.setIconColor(Color.WHITE);
        logoIcon.setEffect(new DropShadow(15, Color.rgb(255, 255, 255, 0.3)));

        Label brandTitle = new Label("KUBATA");
        brandTitle.setStyle("-fx-font-size: 42px; -fx-font-weight: bold; -fx-text-fill: white; -fx-letter-spacing: 3px;");

        Label brandSlogan = new Label("Enterprise Resource Planning");
        brandSlogan.setStyle("-fx-font-size: 16px; -fx-text-fill: rgba(255,255,255,0.8); -fx-font-style: italic;");

        VBox infoBox = new VBox(10);
        infoBox.setAlignment(Pos.CENTER);
        infoBox.setPadding(new Insets(30, 0, 0, 0));
        
        Label versionLabel = new Label("v2.0.0 Stable");
        versionLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.5); -fx-font-size: 11px;");
        
        leftSide.getChildren().addAll(logoIcon, brandTitle, brandSlogan, infoBox, versionLabel);
        return leftSide;
    }

    private VBox createFormSide() {
        VBox rightSide = new VBox(30);
        rightSide.setPrefWidth(480);
        rightSide.getStyleClass().add("login-form-side");
        rightSide.setPadding(new Insets(60));
        rightSide.setStyle("-fx-background-color: white; -fx-background-radius: 0 20 20 0;");
        rightSide.setAlignment(Pos.CENTER_LEFT);

        VBox headerBox = new VBox(10);
        Label welcomeLabel = new Label("KUBATA");
        welcomeLabel.getStyleClass().add("title-2");
        welcomeLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #1A5C35; -fx-font-size: 32px;");

        Label instructionLabel = new Label("Aceda à gestão centralizada do sistema");
        instructionLabel.setStyle("-fx-text-fill: #95a5a6; -fx-font-size: 15px;");
        headerBox.getChildren().addAll(welcomeLabel, instructionLabel);

        VBox formFields = createFormFields();

        rightSide.getChildren().addAll(headerBox, formFields);
        return rightSide;
    }

    private VBox createFormFields() {
        VBox formFields = new VBox(25);

        // Email field
        VBox emailBox = new VBox(8);
        Label lblEmail = new Label("Utilizador ou Email");
        lblEmail.setStyle("-fx-font-weight: 600; -fx-font-size: 14px; -fx-text-fill: #34495e;");
        emailField = new TextField();
        emailField.setPromptText("exemplo@allon.ao");
        emailField.setPrefHeight(50);
        emailField.getStyleClass().add("modern-text-field");
        emailField.textProperty().addListener((obs, old, val) -> validateEmail(val));
        emailBox.getChildren().addAll(lblEmail, emailField);

        // Password field with visibility toggle
        VBox passwordBox = createPasswordBox();

        // Remember & Forgot
        HBox extraActions = createExtraActions();

        // Message label
        messageLabel = new Label();
        messageLabel.setWrapText(true);
        messageLabel.setManaged(false);
        messageLabel.setVisible(false);
        messageLabel.getStyleClass().add("message-label");
        messageLabel.setStyle("-fx-font-size: 13px; -fx-padding: 12px; -fx-background-radius: 8px;");

        // Login button + loading indicator
        loginButton = new Button("AUTENTICAR NO SISTEMA");
        loginButton.getStyleClass().add("button-primary");
        loginButton.setMaxWidth(Double.MAX_VALUE);
        loginButton.setPrefHeight(60);
        loginButton.setCursor(Cursor.HAND);
        loginButton.setOnAction(e -> handleLogin());

        loading = new ProgressIndicator();
        loading.setMaxSize(28, 28);
        loading.setVisible(false);
        loading.setManaged(false);

        StackPane btnStack = new StackPane(loginButton, loading);

        formFields.getChildren().addAll(emailBox, passwordBox, extraActions, messageLabel, btnStack);
        return formFields;
    }

    private VBox createPasswordBox() {
        VBox passwordBox = new VBox(8);
        Label lblPass = new Label("Palavra-passe");
        lblPass.setStyle("-fx-font-weight: 600; -fx-font-size: 14px; -fx-text-fill: #34495e;");

        passwordField = new PasswordField();
        passwordField.setPromptText("••••••••");
        passwordField.setPrefHeight(50);
        passwordField.getStyleClass().add("modern-text-field");

        passwordVisibleField = new TextField();
        passwordVisibleField.setManaged(false);
        passwordVisibleField.setVisible(false);
        passwordVisibleField.setPrefHeight(50);
        passwordVisibleField.getStyleClass().add("modern-text-field");
        passwordVisibleField.textProperty().bindBidirectional(passwordField.textProperty());

        showPasswordBtn = new ToggleButton();
        showPasswordBtn.setGraphic(new FontIcon(Feather.EYE));
        showPasswordBtn.getStyleClass().add("button-icon-small");
        showPasswordBtn.setCursor(Cursor.HAND);
        showPasswordBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #95a5a6;");

        showPasswordBtn.selectedProperty().addListener((obs, old, isSelected) -> {
            if (isSelected) {
                showPasswordBtn.setGraphic(new FontIcon(Feather.EYE_OFF));
                passwordField.setManaged(false);
                passwordField.setVisible(false);
                passwordVisibleField.setManaged(true);
                passwordVisibleField.setVisible(true);
            } else {
                showPasswordBtn.setGraphic(new FontIcon(Feather.EYE));
                passwordField.setManaged(true);
                passwordField.setVisible(true);
                passwordVisibleField.setManaged(false);
                passwordVisibleField.setVisible(false);
            }
        });

        // Stack to swap password field with plain text field
        StackPane passStack = new StackPane(passwordField, passwordVisibleField);
        HBox passContainer = new HBox(passStack, showPasswordBtn);
        HBox.setHgrow(passStack, Priority.ALWAYS);
        passContainer.setAlignment(Pos.CENTER_LEFT);
        passContainer.setSpacing(5);

        passwordBox.getChildren().addAll(lblPass, passContainer);
        return passwordBox;
    }

    private HBox createExtraActions() {
        HBox extraActions = new HBox();
        extraActions.setAlignment(Pos.CENTER_LEFT);
        rememberMe = new CheckBox("Manter sessão iniciada");
        rememberMe.setStyle("-fx-font-size: 13px; -fx-text-fill: #7f8c8d; -fx-cursor: hand;");

        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Hyperlink forgotPass = new Hyperlink("Recuperar palavra-passe");
        forgotPass.setStyle("-fx-font-size: 13px; -fx-text-fill: #217346; -fx-font-weight: 600;");
        forgotPass.setOnAction(e -> handleForgotPassword());

        extraActions.getChildren().addAll(rememberMe, spacer, forgotPass);
        return extraActions;
    }



    private void validateEmail(String email) {
        if (email == null || email.isEmpty()) {
            emailField.setStyle("");
            return;
        }
        if (EMAIL_PATTERN.matcher(email).matches()) {
            emailField.setStyle("-fx-border-color: #2ecc71; -fx-border-width: 2;");
        } else {
            emailField.setStyle("-fx-border-color: #e74c3c; -fx-border-width: 2;");
        }
    }

    private void handleLogin() {
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        // Validations
        if (email.isEmpty()) {
            showMessage("Por favor, insira o email.", true);
            emailField.requestFocus();
            return;
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            showMessage("Por favor, insira um email válido (ex: nome@dominio.ao).", true);
            emailField.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            showMessage("Por favor, insira a palavra-passe.", true);
            passwordField.requestFocus();
            return;
        }

        setLoading(true);
        showMessage("", false);

        // Run authentication in background
        executor.submit(() -> {
            try {
                // Remove artificial sleep – use real authentication
                User user = authService.authenticate(email, password, null, "127.0.0.1");
                Platform.runLater(() -> {
                    setLoading(false);
                    eventPublisher.publishEvent(new LoginSuccessEvent(this, user));
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    setLoading(false);
                    String errorMsg = ex.getMessage();
                    if (errorMsg == null || errorMsg.isBlank()) {
                        errorMsg = "Falha na autenticação. Verifique as suas credenciais.";
                    }
                    showMessage(errorMsg, true);
                    shakeNode(loginButton);
                });
            }
        });
    }

    private void handleForgotPassword() {
        String email = emailField.getText().trim();
        if (email.isEmpty()) {
            showMessage("Por favor, insira o seu email para recuperar a senha.", true);
            emailField.requestFocus();
            return;
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            showMessage("Email inválido. Corrija o email antes de pedir recuperação.", true);
            return;
        }

        setLoading(true);
        executor.submit(() -> {
            try {
                // Chamar serviço de reset de senha (implementar no AuthService)
                // authService.requestPasswordReset(email);
                Thread.sleep(1000); // Simulação – remover depois
                Platform.runLater(() -> {
                    setLoading(false);
                    showMessage("Foi enviado um link de recuperação para " + email, false);
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    setLoading(false);
                    showMessage("Erro ao solicitar recuperação: " + ex.getMessage(), true);
                });
            }
        });
    }

    private void setLoading(boolean on) {
        loginButton.setDisable(on);
        loginButton.setText(on ? "" : "Iniciar Sessão");
        loading.setVisible(on);
        loading.setManaged(on);
        emailField.setDisable(on);
        passwordField.setDisable(on);
        showPasswordBtn.setDisable(on);
        // Also disable social buttons during loading
        // (they are not directly referenced, but we can traverse or store them)
    }

    private void showMessage(String msg, boolean isError) {
        if (msg == null || msg.isBlank()) {
            messageLabel.setVisible(false);
            messageLabel.setManaged(false);
            return;
        }
        messageLabel.setVisible(true);
        messageLabel.setManaged(true);
        messageLabel.setText(msg);
        if (isError) {
            messageLabel.setStyle("-fx-background-color: #fdeaea; -fx-text-fill: #cf222e; -fx-padding: 10px; -fx-background-radius: 4px; -fx-border-color: #ffcecb; -fx-border-width: 1px;");
        } else {
            messageLabel.setStyle("-fx-background-color: #e6ffed; -fx-text-fill: #1a7f37; -fx-padding: 10px; -fx-background-radius: 4px; -fx-border-color: #aceebb; -fx-border-width: 1px;");
        }
    }

    private void animateEntrance(Node node) {
        node.setOpacity(0);
        node.setScaleX(0.9);
        node.setScaleY(0.9);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(800), node);
        fadeIn.setToValue(1);
        ScaleTransition scaleUp = new ScaleTransition(Duration.millis(800), node);
        scaleUp.setToX(1);
        scaleUp.setToY(1);
        new ParallelTransition(fadeIn, scaleUp).play();
    }

    private void shakeNode(Node node) {
        ShakeTransition shake = new ShakeTransition(node);
        shake.play();
    }

    // Custom shake transition that resets translation after completion
    private static class ShakeTransition extends Transition {
        private final Node node;
        private final double originalX;

        public ShakeTransition(Node node) {
            this.node = node;
            this.originalX = node.getTranslateX();
            setCycleDuration(Duration.millis(500));
        }

        @Override
        protected void interpolate(double frac) {
            if (frac >= 1.0) {
                node.setTranslateX(originalX);
            } else {
                double delta = 10 * Math.sin(frac * 10 * Math.PI);
                node.setTranslateX(originalX + delta);
            }
        }
    }
}
package ao.allon.kubata.faturacao.controller;

import ao.allon.kubata.core.domain.User;
import ao.allon.kubata.core.service.AuthService;
import ao.allon.kubata.faturacao.ui.event.LoginSuccessEvent;
import atlantafx.base.controls.Card;
import atlantafx.base.theme.Styles;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.*;
import javafx.scene.Group;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.util.Duration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class LoginController {

    private final AuthService authService;
    private final ApplicationEventPublisher eventPublisher;
    
    private TextField emailField;
    private PasswordField passwordField;
    private Label errorLabel;
    private Button loginButton;
    private ProgressIndicator loadingIndicator;
    private StackPane mainContainer;
    private VBox loginCard;

    public LoginController(AuthService authService, ApplicationEventPublisher eventPublisher) {
        this.authService = authService;
        this.eventPublisher = eventPublisher;
    }

    public StackPane createView() {
        mainContainer = new StackPane();
        mainContainer.setPrefSize(900, 600);
        mainContainer.getStyleClass().add("login-bg");
        mainContainer.setStyle("-fx-background-color: -color-accent-8;");
        
        createBackgroundElements();
        loginCard = createLoginCard();
        StackPane.setAlignment(loginCard, Pos.CENTER);
        mainContainer.getChildren().add(loginCard);
        playEntryAnimation();
        
        return mainContainer;
    }
    
    private void createBackgroundElements() {
        for (int i = 0; i < 5; i++) {
            Circle circle = new Circle(50 + Math.random() * 100);
            circle.setFill(Color.rgb(255, 255, 255, 0.05));
            circle.setTranslateX(-300 + Math.random() * 600);
            circle.setTranslateY(-200 + Math.random() * 400);
            
            TranslateTransition anim = new TranslateTransition(Duration.seconds(4 + Math.random() * 4), circle);
            anim.setByY(-30);
            anim.setAutoReverse(true);
            anim.setCycleCount(Animation.INDEFINITE);
            anim.setDelay(Duration.seconds(i * 0.5));
            anim.play();
            
            mainContainer.getChildren().add(circle);
        }
    }
    
    private VBox createLoginCard() {
        VBox card = new VBox(20);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(40, 50, 40, 50));
        card.setMaxWidth(400);
        card.setMaxHeight(680);
        card.getStyleClass().addAll(Styles.BORDER_DEFAULT, Styles.ELEVATED_2);
        card.setStyle("-fx-background-color: -color-bg-default; -fx-background-radius: 10px;");
        
        DropShadow shadow = new DropShadow();
        shadow.setRadius(20);
        shadow.setColor(Color.rgb(0, 0, 0, 0.2));
        card.setEffect(shadow);
        
        StackPane iconContainer = createLogoContainer();
        
        ScaleTransition pulse = new ScaleTransition(Duration.seconds(2), iconContainer);
        pulse.setFromX(1);
        pulse.setToX(1.05);
        pulse.setFromY(1);
        pulse.setToY(1.05);
        pulse.setAutoReverse(true);
        pulse.setCycleCount(Animation.INDEFINITE);
        pulse.play();
        
        Text title = new Text("Bem-vindo");
        title.getStyleClass().addAll(Styles.TITLE_2, Styles.TEXT_BOLD);
        
        Text subtitle = new Text("Entre na sua conta para continuar");
        subtitle.getStyleClass().add(Styles.TEXT_MUTED);
        
        VBox formBox = createFormBox();
        
        loginButton = new Button("Entrar");
        loginButton.setDefaultButton(true);
        loginButton.setPrefWidth(300);
        loginButton.setPrefHeight(44);
        loginButton.getStyleClass().add(Styles.ACCENT);
        loginButton.setOnAction(e -> handleLogin());
        
        loadingIndicator = new ProgressIndicator();
        loadingIndicator.setVisible(false);
        loadingIndicator.setMaxSize(30, 30);
        
        StackPane buttonContainer = new StackPane(loginButton, loadingIndicator);
        
        Hyperlink forgotLink = new Hyperlink("Esqueceu a senha?");
        forgotLink.getStyleClass().add(Styles.TEXT_SMALL);
        forgotLink.setOnAction(e -> handleForgotPassword());
        
        Label versionLabel = new Label("Kubata Faturacao v1.0");
        versionLabel.getStyleClass().addAll(Styles.TEXT_SMALL, Styles.TEXT_MUTED);
        
        card.getChildren().addAll(iconContainer, title, subtitle, formBox, buttonContainer, forgotLink, versionLabel);
        return card;
    }
    
    private VBox createFormBox() {
        VBox form = new VBox(12);
        form.setAlignment(Pos.CENTER_LEFT);
        form.setMaxWidth(300);
        
        Label lblEmail = new Label("Email");
        lblEmail.getStyleClass().add(Styles.TEXT_SMALL);
        
        emailField = new TextField();
        emailField.setPromptText("seu@email.com");
        emailField.setPrefHeight(40);
        
        Label lblSenha = new Label("Senha");
        lblSenha.getStyleClass().add(Styles.TEXT_SMALL);
        
        passwordField = new PasswordField();
        passwordField.setPromptText("Sua senha");
        passwordField.setPrefHeight(40);
        passwordField.setOnAction(e -> handleLogin());
        
        errorLabel = new Label();
        errorLabel.getStyleClass().add(Styles.TEXT_SMALL);
        errorLabel.setStyle("-fx-text-fill: -color-danger-fg;");
        errorLabel.setWrapText(true);
        errorLabel.setVisible(false);
        
        form.getChildren().addAll(lblEmail, emailField, lblSenha, passwordField, errorLabel);
        return form;
    }
    
    private void playEntryAnimation() {
        loginCard.setOpacity(0);
        loginCard.setTranslateY(30);
        
        FadeTransition fade = new FadeTransition(Duration.millis(800), loginCard);
        fade.setFromValue(0);
        fade.setToValue(1);
        
        TranslateTransition slide = new TranslateTransition(Duration.millis(800), loginCard);
        slide.setFromY(30);
        slide.setToY(0);
        
        ParallelTransition parallel = new ParallelTransition(fade, slide);
        parallel.setInterpolator(Interpolator.EASE_OUT);
        parallel.play();
    }
    
    private void shakeForm() {
        TranslateTransition shake = new TranslateTransition(Duration.millis(50), loginCard);
        shake.setByX(10);
        shake.setAutoReverse(true);
        shake.setCycleCount(6);
        shake.play();
    }

    public void handleLogin() {
        if (emailField == null) return;
        
        String email = emailField.getText();
        String password = passwordField.getText();

        if (email == null || email.isEmpty() || password == null || password.isEmpty()) {
            showError("Preencha todos os campos.");
            shakeForm();
            return;
        }

        setLoading(true);
        errorLabel.setVisible(false);

        Task<User> task = new Task<>() {
            @Override
            protected User call() throws Exception {
                return authService.authenticate(email, password, null, "127.0.0.1");
            }
        };

        task.setOnSucceeded(e -> {
            setLoading(false);
            playExitAnimation(() -> {
                Platform.runLater(() -> eventPublisher.publishEvent(new LoginSuccessEvent(this, task.getValue())));
            });
        });

        task.setOnFailed(e -> {
            setLoading(false);
            showError(task.getException() != null ? task.getException().getMessage() : "Erro no login.");
            shakeForm();
        });

        new Thread(task).start();
    }
    
    private void setLoading(boolean loading) {
        loginButton.setVisible(!loading);
        loginButton.setManaged(!loading);
        loadingIndicator.setVisible(loading);
        loadingIndicator.setManaged(loading);
    }
    
    private void playExitAnimation(Runnable onComplete) {
        FadeTransition fade = new FadeTransition(Duration.millis(400), loginCard);
        fade.setFromValue(1);
        fade.setToValue(0);
        fade.setOnFinished(e -> { if (onComplete != null) onComplete.run(); });
        fade.play();
    }

    public void handleForgotPassword() {
        String email = emailField.getText();
        if (email == null || email.isEmpty()) {
            showError("Digite seu email.");
            shakeForm();
            return;
        }
        authService.recoverPassword(email);
        showInfo("Se o email existir, instrucoes foram enviadas.");
    }

    private void showError(String msg) {
        errorLabel.setStyle("-fx-text-fill: -color-danger-fg;");
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
    }
    
    private void showInfo(String msg) {
        errorLabel.setStyle("-fx-text-fill: -color-success-fg;");
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
    }
    
    private StackPane createLogoContainer() {
        StackPane container = new StackPane();
        container.setPrefSize(80, 80);
        
        LinearGradient gradient = new LinearGradient(
            0, 0, 1, 1, true, null,
            new Stop(0, Color.web("#6366f1")),
            new Stop(1, Color.web("#8b5cf6"))
        );
        
        Circle bgCircle = new Circle(40);
        bgCircle.setFill(gradient);
        bgCircle.setEffect(new DropShadow(10, Color.rgb(99, 102, 241, 0.4)));
        
        Rectangle doc = new Rectangle(36, 44);
        doc.setFill(Color.WHITE);
        doc.setArcWidth(6);
        doc.setArcHeight(6);
        doc.setTranslateX(-2);
        doc.setTranslateY(2);
        
        Text letterK = new Text("K");
        letterK.setFont(Font.font("System", FontWeight.BOLD, 22));
        letterK.setFill(Color.web("#6366f1"));
        letterK.setTranslateY(-4);
        letterK.setTranslateX(-2);
        
        Rectangle line1 = new Rectangle(24, 3);
        line1.setFill(Color.web("#c7d2fe"));
        line1.setArcWidth(1.5);
        line1.setArcHeight(1.5);
        line1.setTranslateX(-2);
        line1.setTranslateY(12);
        
        Rectangle line2 = new Rectangle(18, 3);
        line2.setFill(Color.web("#c7d2fe"));
        line2.setArcWidth(1.5);
        line2.setArcHeight(1.5);
        line2.setTranslateX(-2);
        line2.setTranslateY(18);
        
        Circle moneyCircle = new Circle(10);
        moneyCircle.setFill(Color.web("#818cf8"));
        moneyCircle.setTranslateX(18);
        moneyCircle.setTranslateY(-18);
        
        Text dollarSign = new Text("$");
        dollarSign.setFont(Font.font("System", FontWeight.BOLD, 12));
        dollarSign.setFill(Color.WHITE);
        dollarSign.setTranslateX(18);
        dollarSign.setTranslateY(-16);
        
        Group docGroup = new Group(doc, letterK, line1, line2, moneyCircle, dollarSign);
        
        container.getChildren().addAll(bgCircle, docGroup);
        return container;
    }
}

package ao.allon.kubata.faturacao.ui.loading;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Splash Screen moderno e profissional para inicialização do sistema Kubata.
 * Mostra progresso de carregamento com animações suaves.
 */
public class SplashScreen {

    private Stage splashStage;
    private Label lblStatus;
    private Label lblProgress;
    private javafx.scene.control.ProgressBar progressBar;
    private VBox contentBox;
    
    private Consumer<Void> onComplete;
    private double progress = 0;

    public SplashScreen() {
        initialize();
    }

    private void initialize() {
        Platform.runLater(() -> {
            createStage();
            createContent();
            startAnimations();
        });
    }

    private void createStage() {
        splashStage = new Stage();
        splashStage.initStyle(StageStyle.UNDECORATED);
        splashStage.setResizable(false);
        splashStage.setAlwaysOnTop(true);
        
        // Tamanho da splash screen
        double width = 600;
        double height = 400;
        
        // Criar cena com fundo gradiente
        StackPane root = new StackPane();
        root.setPrefSize(width, height);
        
        // Background com gradiente
        Rectangle bg = new Rectangle(width, height);
        LinearGradient gradient = new LinearGradient(
            0, 0, 1, 1, true, null,
            new Stop(0, Color.web("#1e1b4b")),
            new Stop(0.5, Color.web("#312e81")),
            new Stop(1, Color.web("#4338ca"))
        );
        bg.setFill(gradient);
        root.getChildren().add(bg);
        
        Scene scene = new Scene(root, width, height);
        scene.setFill(Color.TRANSPARENT);
        scene.getStylesheets().add(Objects.requireNonNull(
            getClass().getResource("/styles/splash-screen.css")).toExternalForm());
        
        splashStage.setScene(scene);
        splashStage.centerOnScreen();
    }

    private void createContent() {
        StackPane root = (StackPane) splashStage.getScene().getRoot();
        
        contentBox = new VBox(25);
        contentBox.setAlignment(Pos.CENTER);
        contentBox.setPadding(new Insets(40));
        contentBox.setMaxWidth(500);
        
        // Logo/Icon
        FontIcon icon = new FontIcon(Feather.BOX);
        icon.setIconSize(64);
        icon.setIconColor(Color.WHITE);
        
        // Título
        Label lblTitle = new Label("KUBATA");
        lblTitle.setFont(Font.font("System", FontWeight.BOLD, 36));
        lblTitle.setTextFill(Color.WHITE);
        
        Label lblSubtitle = new Label("Sistema de Faturação e Gestão Empresarial");
        lblSubtitle.setFont(Font.font("System", 16));
        lblSubtitle.setTextFill(Color.web("#c7d2fe"));
        
        // Status
        lblStatus = new Label("Inicializando sistema...");
        lblStatus.setFont(Font.font("System", 14));
        lblStatus.setTextFill(Color.web("#a5b4fc"));
        
        // Progress bar
        progressBar = new javafx.scene.control.ProgressBar(0);
        progressBar.setPrefWidth(400);
        progressBar.getStyleClass().add("splash-progress");
        
        // Progress text
        lblProgress = new Label("0%");
        lblProgress.setFont(Font.font("System", 12));
        lblProgress.setTextFill(Color.web("#818cf8"));
        
        // Versão
        Label lblVersion = new Label("Versão 1.0.0");
        lblVersion.setFont(Font.font("System", 11));
        lblVersion.setTextFill(Color.web("#6366f1"));
        
        VBox.setVgrow(lblVersion, Priority.ALWAYS);
        lblVersion.setAlignment(Pos.BOTTOM_CENTER);
        
        contentBox.getChildren().addAll(
            icon, 
            lblTitle, 
            lblSubtitle, 
            new VBox(10, lblStatus, progressBar, lblProgress),
            lblVersion
        );
        
        // Efeito de glassmorphism
        VBox glassContainer = new VBox(contentBox);
        glassContainer.setAlignment(Pos.CENTER);
        glassContainer.setStyle(
            "-fx-background-color: rgba(255, 255, 255, 0.05);" +
            "-fx-background-radius: 20;" +
            "-fx-border-radius: 20;" +
            "-fx-border-color: rgba(255, 255, 255, 0.1);" +
            "-fx-border-width: 1;"
        );
        glassContainer.setPadding(new Insets(20));
        glassContainer.setMaxWidth(520);
        glassContainer.setMaxHeight(360);
        
        // Sombra
        DropShadow shadow = new DropShadow();
        shadow.setRadius(30);
        shadow.setSpread(0.1);
        shadow.setColor(Color.rgb(0, 0, 0, 0.5));
        glassContainer.setEffect(shadow);
        
        root.getChildren().add(glassContainer);
    }

    private void startAnimations() {
        Platform.runLater(() -> {
            // Animação de entrada
            FadeTransition fadeIn = new FadeTransition(Duration.millis(800), contentBox);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            
            ScaleTransition scaleIn = new ScaleTransition(Duration.millis(800), contentBox);
            scaleIn.setFromX(0.9);
            scaleIn.setFromY(0.9);
            scaleIn.setToX(1);
            scaleIn.setToY(1);
            
            ParallelTransition parallel = new ParallelTransition(fadeIn, scaleIn);
            parallel.play();
        });
    }

    /**
     * Atualiza o status e progresso
     */
    public void updateProgress(String status, double percent) {
        Platform.runLater(() -> {
            lblStatus.setText(status);
            progress = Math.min(percent, 1.0);
            progressBar.setProgress(progress);
            lblProgress.setText(String.format("%.0f%%", progress * 100));
        });
    }

    /**
     * Atualiza apenas o status
     */
    public void updateStatus(String status) {
        Platform.runLater(() -> lblStatus.setText(status));
    }

    /**
     * Mostra a splash screen
     */
    public void show() {
        Platform.runLater(() -> {
            if (splashStage != null && !splashStage.isShowing()) {
                splashStage.show();
            }
        });
    }

    /**
     * Esconde a splash screen com animação de fade out
     */
    public void hide() {
        Platform.runLater(() -> {
            if (splashStage != null && splashStage.isShowing()) {
                FadeTransition fadeOut = new FadeTransition(Duration.millis(500), contentBox);
                fadeOut.setFromValue(1);
                fadeOut.setToValue(0);
                fadeOut.setOnFinished(e -> {
                    splashStage.hide();
                    if (onComplete != null) {
                        onComplete.accept(null);
                    }
                });
                fadeOut.play();
            }
        });
    }

    /**
     * Configura callback para quando a splash screen terminar
     */
    public void setOnComplete(Consumer<Void> callback) {
        this.onComplete = callback;
    }

    /**
     * Simula carregamento com etapas predefinidas
     */
    public void simulateLoading(java.util.List<String> steps, Runnable onComplete) {
        setOnComplete(v -> {
            if (onComplete != null) {
                onComplete.run();
            }
        });
        
        new Thread(() -> {
            int totalSteps = steps.size();
            for (int i = 0; i < totalSteps; i++) {
                final int currentStep = i;
                Platform.runLater(() -> {
                    double percent = (double) (currentStep + 1) / totalSteps;
                    updateProgress(steps.get(currentStep), percent);
                });
                
                try {
                    Thread.sleep(300 + (int)(Math.random() * 400));
                } catch (InterruptedException ignored) {}
            }
            
            // Pequena pausa antes de fechar
            try {
                Thread.sleep(500);
            } catch (InterruptedException ignored) {}
            
            hide();
        }).start();
    }

    public boolean isShowing() {
        return splashStage != null && splashStage.isShowing();
    }

    public Stage getStage() {
        return splashStage;
    }
}

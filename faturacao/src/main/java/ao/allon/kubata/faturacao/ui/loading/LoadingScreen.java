package ao.allon.kubata.faturacao.ui.loading;

import javafx.animation.FadeTransition;
import javafx.animation.RotateTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Tela de loading profissional e moderna para o sistema Kubata.
 * Suporta múltiplos estilos de animação e pode ser usada de forma modal ou não-modal.
 */
public class LoadingScreen {

    private Stage loadingStage;
    private Label lblMensagem;
    private Label lblSubMensagem;
    private StackPane rootContainer;
    private VBox contentBox;
    private Pane animationContainer;
    
    // Configurações de estilo
    private double width = 400;
    private double height = 250;
    private String title = "Processando...";
    private String mensagem = "Por favor, aguarde";
    private String subMensagem = "";
    private LoadingStyle style = LoadingStyle.SPINNER;
    private boolean modal = true;
    private boolean cancelable = false;
    private Runnable onCancel;

    public enum LoadingStyle {
        SPINNER,      // Indicador de progresso circular
        DOTS,         // Pontos animados
        PULSING,      // Círculo pulsante
        MODERN_RING   // Anel moderno com múltiplos elementos
    }

    public LoadingScreen() {
        initialize();
    }

    private void initialize() {
        Platform.runLater(() -> {
            createStage();
            createContent();
            setupAnimations();
        });
    }

    private void createStage() {
        loadingStage = new Stage();
        loadingStage.initStyle(StageStyle.TRANSPARENT);
        loadingStage.setResizable(false);
        loadingStage.setAlwaysOnTop(true);
        
        if (modal) {
            loadingStage.initModality(Modality.APPLICATION_MODAL);
        }
        
        // Criar cena com fundo transparente
        rootContainer = new StackPane();
        rootContainer.setStyle("-fx-background-color: rgba(0, 0, 0, 0.6);");
        
        Scene scene = new Scene(rootContainer, width, height);
        scene.setFill(Color.TRANSPARENT);
        scene.getStylesheets().add(Objects.requireNonNull(
            getClass().getResource("/styles/loading-screen.css")).toExternalForm());
        
        loadingStage.setScene(scene);
        
        // Centralizar na tela
        loadingStage.centerOnScreen();
    }

    private void createContent() {
        // Container principal do card
        contentBox = new VBox(20);
        contentBox.setAlignment(Pos.CENTER);
        contentBox.setPadding(new Insets(30));
        contentBox.setMaxWidth(350);
        contentBox.setMaxHeight(200);
        contentBox.getStyleClass().add("loading-card");
        
        // Efeito de sombra
        DropShadow shadow = new DropShadow();
        shadow.setRadius(20);
        shadow.setSpread(0.2);
        shadow.setColor(Color.rgb(0, 0, 0, 0.3));
        contentBox.setEffect(shadow);
        
        // Container da animação
        animationContainer = new StackPane();
        animationContainer.setPrefSize(80, 80);
        animationContainer.setMinSize(80, 80);
        
        // Labels
        lblMensagem = new Label(mensagem);
        lblMensagem.getStyleClass().addAll("loading-title", "text-bold");
        lblMensagem.setWrapText(true);
        lblMensagem.setAlignment(Pos.CENTER);
        
        lblSubMensagem = new Label(subMensagem);
        lblSubMensagem.getStyleClass().addAll("loading-subtitle", "text-muted");
        lblSubMensagem.setWrapText(true);
        lblSubMensagem.setAlignment(Pos.CENTER);
        lblSubMensagem.setVisible(!subMensagem.isEmpty());
        
        contentBox.getChildren().addAll(animationContainer, lblMensagem, lblSubMensagem);
        rootContainer.getChildren().add(contentBox);
        
        // Clicar fora fecha (se cancelable)
        rootContainer.setOnMouseClicked(e -> {
            if (cancelable && e.getTarget() == rootContainer) {
                hide();
                if (onCancel != null) onCancel.run();
            }
        });
    }

    private void setupAnimations() {
        Platform.runLater(() -> {
            animationContainer.getChildren().clear();
            
            switch (style) {
                case SPINNER -> createSpinnerAnimation();
                case DOTS -> createDotsAnimation();
                case PULSING -> createPulsingAnimation();
                case MODERN_RING -> createModernRingAnimation();
            }
        });
    }

    private void createSpinnerAnimation() {
        // Anel externo
        Arc arc1 = new Arc(40, 40, 35, 35, 0, 270);
        arc1.setType(ArcType.OPEN);
        arc1.setFill(Color.TRANSPARENT);
        arc1.setStroke(Color.web("#6366f1"));
        arc1.setStrokeWidth(4);
        arc1.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        
        // Anel interno
        Arc arc2 = new Arc(40, 40, 25, 25, 180, 180);
        arc2.setType(ArcType.OPEN);
        arc2.setFill(Color.TRANSPARENT);
        arc2.setStroke(Color.web("#8b5cf6"));
        arc2.setStrokeWidth(3);
        arc2.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        
        animationContainer.getChildren().addAll(arc1, arc2);
        
        // Animações
        RotateTransition rotate1 = new RotateTransition(Duration.seconds(2), arc1);
        rotate1.setByAngle(360);
        rotate1.setCycleCount(RotateTransition.INDEFINITE);
        rotate1.setInterpolator(javafx.animation.Interpolator.LINEAR);
        
        RotateTransition rotate2 = new RotateTransition(Duration.seconds(1.5), arc2);
        rotate2.setByAngle(-360);
        rotate2.setCycleCount(RotateTransition.INDEFINITE);
        rotate2.setInterpolator(javafx.animation.Interpolator.LINEAR);
        
        rotate1.play();
        rotate2.play();
    }

    private void createDotsAnimation() {
        HBox dotsBox = new HBox(8);
        dotsBox.setAlignment(Pos.CENTER);
        
        for (int i = 0; i < 3; i++) {
            Circle dot = new Circle(6);
            dot.setFill(Color.web("#6366f1"));
            dotsBox.getChildren().add(dot);
            
            ScaleTransition scale = new ScaleTransition(Duration.seconds(0.6), dot);
            scale.setFromX(1);
            scale.setFromY(1);
            scale.setToX(1.5);
            scale.setToY(1.5);
            scale.setAutoReverse(true);
            scale.setCycleCount(ScaleTransition.INDEFINITE);
            scale.setDelay(Duration.seconds(i * 0.2));
            scale.play();
        }
        
        animationContainer.getChildren().add(dotsBox);
    }

    private void createPulsingAnimation() {
        StackPane pulseContainer = new StackPane();
        
        for (int i = 0; i < 3; i++) {
            Circle circle = new Circle(40 - i * 10);
            circle.setFill(Color.TRANSPARENT);
            circle.setStroke(Color.web("#6366f1"));
            circle.setStrokeWidth(2);
            circle.setOpacity(0.7 - i * 0.2);
            pulseContainer.getChildren().add(circle);
            
            ScaleTransition scale = new ScaleTransition(Duration.seconds(1.5), circle);
            scale.setFromX(0.8);
            scale.setFromY(0.8);
            scale.setToX(1.2);
            scale.setToY(1.2);
            scale.setAutoReverse(true);
            scale.setCycleCount(ScaleTransition.INDEFINITE);
            scale.setDelay(Duration.seconds(i * 0.3));
            scale.play();
        }
        
        Circle center = new Circle(15);
        center.setFill(Color.web("#6366f1"));
        pulseContainer.getChildren().add(center);
        
        animationContainer.getChildren().add(pulseContainer);
    }

    private void createModernRingAnimation() {
        StackPane ringContainer = new StackPane();
        
        // Anéis concêntricos com rotação diferente
        double[] radii = {35, 28, 20};
        double[] strokeWidths = {3, 2.5, 2};
        String[] colors = {"#6366f1", "#8b5cf6", "#ec4899"};
        double[] speeds = {2, -1.5, 1};
        double[] arcs = {300, 240, 180};
        
        for (int i = 0; i < 3; i++) {
            Arc arc = new Arc(40, 40, radii[i], radii[i], 0, arcs[i]);
            arc.setType(ArcType.OPEN);
            arc.setFill(Color.TRANSPARENT);
            arc.setStroke(Color.web(colors[i]));
            arc.setStrokeWidth(strokeWidths[i]);
            arc.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
            ringContainer.getChildren().add(arc);
            
            RotateTransition rotate = new RotateTransition(Duration.seconds(speeds[i]), arc);
            rotate.setByAngle(360 * (i % 2 == 0 ? 1 : -1));
            rotate.setCycleCount(RotateTransition.INDEFINITE);
            rotate.setInterpolator(javafx.animation.Interpolator.LINEAR);
            rotate.play();
        }
        
        animationContainer.getChildren().add(ringContainer);
    }

    // ===== Builder Pattern Methods =====

    public LoadingScreen withTitle(String title) {
        this.title = title;
        if (loadingStage != null) {
            Platform.runLater(() -> loadingStage.setTitle(title));
        }
        return this;
    }

    public LoadingScreen withMessage(String mensagem) {
        this.mensagem = mensagem;
        if (lblMensagem != null) {
            Platform.runLater(() -> lblMensagem.setText(mensagem));
        }
        return this;
    }

    public LoadingScreen withSubMessage(String subMensagem) {
        this.subMensagem = subMensagem;
        if (lblSubMensagem != null) {
            Platform.runLater(() -> {
                lblSubMensagem.setText(subMensagem);
                lblSubMensagem.setVisible(!subMensagem.isEmpty());
            });
        }
        return this;
    }

    public LoadingScreen withStyle(LoadingStyle style) {
        this.style = style;
        setupAnimations();
        return this;
    }

    public LoadingScreen withSize(double width, double height) {
        this.width = width;
        this.height = height;
        if (loadingStage != null) {
            Platform.runLater(() -> {
                loadingStage.setWidth(width);
                loadingStage.setHeight(height);
            });
        }
        return this;
    }

    public LoadingScreen cancelable(Runnable onCancel) {
        this.cancelable = true;
        this.onCancel = onCancel;
        return this;
    }

    public LoadingScreen nonModal() {
        this.modal = false;
        if (loadingStage != null) {
            Platform.runLater(() -> loadingStage.initModality(Modality.NONE));
        }
        return this;
    }

    // ===== Show/Hide Methods =====

    public void show() {
        Platform.runLater(() -> {
            if (loadingStage != null && !loadingStage.isShowing()) {
                loadingStage.show();
                
                // Fade in animation
                FadeTransition fadeIn = new FadeTransition(Duration.millis(200), rootContainer);
                fadeIn.setFromValue(0);
                fadeIn.setToValue(1);
                fadeIn.play();
            }
        });
    }

    public void show(Stage owner) {
        Platform.runLater(() -> {
            if (loadingStage != null) {
                loadingStage.initOwner(owner);
                show();
            }
        });
    }

    public void hide() {
        Platform.runLater(() -> {
            if (loadingStage != null && loadingStage.isShowing()) {
                FadeTransition fadeOut = new FadeTransition(Duration.millis(150), rootContainer);
                fadeOut.setFromValue(1);
                fadeOut.setToValue(0);
                fadeOut.setOnFinished(e -> loadingStage.hide());
                fadeOut.play();
            }
        });
    }

    public void updateMessage(String message) {
        Platform.runLater(() -> {
            if (lblMensagem != null) {
                lblMensagem.setText(message);
            }
        });
    }

    public void updateSubMessage(String subMessage) {
        Platform.runLater(() -> {
            if (lblSubMensagem != null) {
                lblSubMensagem.setText(subMessage);
                lblSubMensagem.setVisible(!subMessage.isEmpty());
            }
        });
    }

    public boolean isShowing() {
        return loadingStage != null && loadingStage.isShowing();
    }

    // ===== Static Helper Methods =====

    /**
     * Executa uma operação assíncrona mostrando loading
     */
    public static <T> CompletableFuture<T> runWithLoading(Supplier<T> operation, 
                                                         String message, 
                                                         LoadingStyle style) {
        CompletableFuture<T> future = new CompletableFuture<>();
        
        Platform.runLater(() -> {
            LoadingScreen loading = new LoadingScreen()
                .withMessage(message)
                .withStyle(style)
                .nonModal();
            
            loading.show();
            
            // Executar operação em thread separada
            CompletableFuture.supplyAsync(() -> {
                try {
                    T result = operation.get();
                    future.complete(result);
                    return result;
                } catch (Exception e) {
                    future.completeExceptionally(e);
                    throw e;
                } finally {
                    Platform.runLater(loading::hide);
                }
            });
        });
        
        return future;
    }

    /**
     * Executa uma operação Runnable com loading
     */
    public static CompletableFuture<Void> runWithLoading(Runnable operation, 
                                                         String message, 
                                                         LoadingStyle style) {
        return runWithLoading(() -> {
            operation.run();
            return null;
        }, message, style);
    }

    /**
     * Mostra loading simples por um período
     */
    public static void showTemporary(String message, int milliseconds) {
        Platform.runLater(() -> {
            LoadingScreen loading = new LoadingScreen()
                .withMessage(message)
                .withStyle(LoadingStyle.SPINNER);
            loading.show();
            
            new Thread(() -> {
                try {
                    Thread.sleep(milliseconds);
                } catch (InterruptedException ignored) {}
                Platform.runLater(loading::hide);
            }).start();
        });
    }
}

package ao.allon.kubata.faturacao.ui.loading;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.util.Duration;

/**
 * Componente de loading inline que pode ser sobreposto a qualquer container.
 * Útil para mostrar loading dentro de painéis específicos sem bloquear toda a UI.
 */
public class LoadingOverlay extends StackPane {

    private final VBox contentBox;
    private final Label lblMessage;
    private final Pane animationPane;
    private final StackPane targetContainer;
    private Node originalContent;
    private boolean isShowing = false;

    public LoadingOverlay(StackPane targetContainer) {
        this.targetContainer = targetContainer;
        
        // Configurar overlay
        setStyle("-fx-background-color: rgba(255, 255, 255, 0.9);");
        setAlignment(Pos.CENTER);
        setVisible(false);
        setOpacity(0);
        
        // Container do conteúdo
        contentBox = new VBox(15);
        contentBox.setAlignment(Pos.CENTER);
        contentBox.getStyleClass().add("inline-loading");
        
        // Painel de animação
        animationPane = new StackPane();
        animationPane.setPrefSize(60, 60);
        
        // Label de mensagem
        lblMessage = new Label("Carregando...");
        lblMessage.getStyleClass().add("message");
        
        contentBox.getChildren().addAll(animationPane, lblMessage);
        getChildren().add(contentBox);
        
        // Adicionar ao container alvo
        targetContainer.getChildren().add(this);
        
        // Inicializar com spinner simples
        createSimpleSpinner();
    }

    private void createSimpleSpinner() {
        javafx.scene.shape.Arc arc = new javafx.scene.shape.Arc(30, 30, 25, 25, 0, 270);
        arc.setType(javafx.scene.shape.ArcType.OPEN);
        arc.setFill(Color.TRANSPARENT);
        arc.setStroke(Color.web("#6366f1"));
        arc.setStrokeWidth(4);
        arc.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        
        animationPane.getChildren().add(arc);
        
        javafx.animation.RotateTransition rotate = new javafx.animation.RotateTransition(
            javafx.util.Duration.seconds(1), arc);
        rotate.setByAngle(360);
        rotate.setCycleCount(javafx.animation.Animation.INDEFINITE);
        rotate.setInterpolator(javafx.animation.Interpolator.LINEAR);
        rotate.play();
    }

    /**
     * Mostra o overlay de loading
     */
    public void show() {
        if (isShowing) return;
        
        Platform.runLater(() -> {
            // Trazer para frente
            toFront();
            
            setVisible(true);
            
            FadeTransition fadeIn = new FadeTransition(Duration.millis(200), this);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            fadeIn.play();
            
            isShowing = true;
        });
    }

    /**
     * Mostra com mensagem personalizada
     */
    public void show(String message) {
        Platform.runLater(() -> lblMessage.setText(message));
        show();
    }

    /**
     * Esconde o overlay
     */
    public void hide() {
        if (!isShowing) return;
        
        Platform.runLater(() -> {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(150), this);
            fadeOut.setFromValue(1);
            fadeOut.setToValue(0);
            fadeOut.setOnFinished(e -> {
                setVisible(false);
                toBack();
            });
            fadeOut.play();
            
            isShowing = false;
        });
    }

    /**
     * Atualiza a mensagem
     */
    public void updateMessage(String message) {
        Platform.runLater(() -> lblMessage.setText(message));
    }

    public boolean isShowing() {
        return isShowing;
    }

    /**
     * Cria um overlay para um container específico
     */
    public static LoadingOverlay createFor(StackPane container) {
        return new LoadingOverlay(container);
    }

    /**
     * Executa operação com overlay temporário
     */
    public static void executeWithOverlay(StackPane container, String message, Runnable operation) {
        LoadingOverlay overlay = new LoadingOverlay(container);
        overlay.show(message);
        
        new Thread(() -> {
            try {
                operation.run();
            } finally {
                Platform.runLater(overlay::hide);
            }
        }).start();
    }
}

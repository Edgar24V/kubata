package ao.allon.kubata.faturacao.ui.modal;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Container responsivo que anima suas dimensões quando o conteúdo interno muda.
 * Implementa lógica de dimensionamento inteligente com limites e transições suaves.
 */
public class ResponsiveModalContainer extends VBox {

    private final BooleanProperty animateChanges = new SimpleBooleanProperty(true);
    private Timeline resizeTimeline;
    private boolean isResizing = false;
    
    // Limites de segurança
    private double maxContainerWidth = 0;
    private double maxContainerHeight = 0;
    private double minContainerWidth = 0; // Removido limite mínimo forçado para permitir modais compactos
    private double minContainerHeight = 0;

    public ResponsiveModalContainer() {
        super();
        // Inicializa com tamanho computado
        setPrefSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
        
        // Monitora alterações na Scene para atualizar limites máximos
        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.widthProperty().addListener((o, oldW, newW) -> updateMaxLimits());
                newScene.heightProperty().addListener((o, oldH, newH) -> updateMaxLimits());
                updateMaxLimits();
            }
        });
    }

    private void updateMaxLimits() {
        if (getScene() != null) {
            // Define limite máximo como 90% da tela ou valores fixos de segurança
            maxContainerWidth = Math.max(minContainerWidth, getScene().getWidth() * 0.9);
            maxContainerHeight = Math.max(minContainerHeight, getScene().getHeight() * 0.9);
            // Re-verifica o tamanho atual se necessário
            requestLayout();
        }
    }

    @Override
    public void requestLayout() {
        super.requestLayout();
        
        // Se já estiver redimensionando, não agende nova verificação imediatamente para evitar loops
        if (!isResizing && animateChanges.get()) {
            Platform.runLater(this::checkAndAnimateSize);
        }
    }

    private void checkAndAnimateSize() {
        if (getScene() == null || getChildren().isEmpty()) return;

        // Calcula o tamanho desejado baseado nos filhos (ignorando o tamanho atual fixo se houver)
        // O -1 instrui o layout a calcular o tamanho preferido intrínseco
        double targetWidth = super.computePrefWidth(-1);
        double targetHeight = super.computePrefHeight(-1);

        // Aplica padding e bordas
        Insets insets = getInsets();
        targetWidth += insets.getLeft() + insets.getRight();
        targetHeight += insets.getTop() + insets.getBottom();

        // Aplica restrições (Clamping)
        targetWidth = Math.max(minContainerWidth, Math.min(targetWidth, maxContainerWidth));
        targetHeight = Math.max(minContainerHeight, Math.min(targetHeight, maxContainerHeight));

        // Verifica se precisa redimensionar (com uma margem de tolerância de 1px)
        double currentWidth = getPrefWidth();
        double currentHeight = getPrefHeight();
        
        // Se estiver como USE_COMPUTED_SIZE (-1), assumimos o tamanho atual real
        if (currentWidth == Region.USE_COMPUTED_SIZE) currentWidth = getWidth();
        if (currentHeight == Region.USE_COMPUTED_SIZE) currentHeight = getHeight();

        // Se o tamanho atual for 0 (inicialização), não anima, apenas seta
        if (currentWidth == 0 || currentHeight == 0) {
            setPrefSize(targetWidth, targetHeight);
            setMaxSize(targetWidth, targetHeight); // Limita o tamanho máximo ao conteúdo
            return;
        }

        boolean widthChanged = Math.abs(targetWidth - currentWidth) > 1;
        boolean heightChanged = Math.abs(targetHeight - currentHeight) > 1;

        if (widthChanged || heightChanged) {
            animateTo(targetWidth, targetHeight);
        }
    }

    private void animateTo(double targetWidth, double targetHeight) {
        if (resizeTimeline != null && resizeTimeline.getStatus() == Timeline.Status.RUNNING) {
            resizeTimeline.stop();
        }

        isResizing = true;
        
        // Também atualiza o MaxSize para evitar expansão indesejada pelo layout pai
        setMaxSize(targetWidth, targetHeight);

        resizeTimeline = new Timeline(
            new KeyFrame(Duration.millis(250), 
                new KeyValue(prefWidthProperty(), targetWidth, Interpolator.EASE_BOTH),
                new KeyValue(prefHeightProperty(), targetHeight, Interpolator.EASE_BOTH)
            )
        );

        resizeTimeline.setOnFinished(e -> {
            isResizing = false;
            // Opcional: Voltar para COMPUTED_SIZE se quisermos que o layout nativo assuma,
            // mas manter fixo evita "pulos" se o conteúdo for instável.
            // setPrefSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
        });

        resizeTimeline.play();
    }

    // API Pública para configuração

    public void setAnimateChanges(boolean animate) {
        this.animateChanges.set(animate);
    }

    public BooleanProperty animateChangesProperty() {
        return animateChanges;
    }
    
    public void setMinDimensions(double width, double height) {
        this.minContainerWidth = width;
        this.minContainerHeight = height;
        requestLayout();
    }
    
    public void setMaxDimensions(double width, double height) {
        this.maxContainerWidth = width;
        this.maxContainerHeight = height;
        requestLayout();
    }
}

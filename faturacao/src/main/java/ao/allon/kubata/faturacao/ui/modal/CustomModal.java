package ao.allon.kubata.faturacao.ui.modal;

import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.FadeTransition;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.feather.Feather;

import javafx.scene.input.KeyCode;
import java.util.function.Consumer;

public class CustomModal extends StackPane {

    private final Region overlay;
    private final ResponsiveModalContainer modalContainer;
    private final HBox header;
    private final StackPane contentPane;
    private final ScrollPane outerScrollPane;
    private final HBox footer;
    private final Label titleLabel;
    
    // Dragging state
    private double xOffset = 0;
    private double yOffset = 0;
    
    // Callbacks
    private Consumer<Void> onCloseRequest;

    public CustomModal() {
        // 1. Overlay
        overlay = new Region();
        overlay.getStyleClass().add("custom-modal-overlay");
        // No close on overlay click here; handled by scroll container

        // 2. Modal Container
        modalContainer = new ResponsiveModalContainer();
        modalContainer.getStyleClass().add("custom-modal-container");
       
        // Accessibility: Allow focus and close on ESC
        modalContainer.setFocusTraversable(true);
        modalContainer.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                hide();
                e.consume();
            }
        });

        // 3. Header
        titleLabel = new Label("Default Title");
        titleLabel.getStyleClass().add("custom-modal-title");
        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button closeButton = new Button("", new FontIcon(Feather.X));
        closeButton.getStyleClass().addAll("custom-modal-close-button", "button-icon");
        closeButton.setOnAction(e -> {
            if (onCloseRequest != null) {
                onCloseRequest.accept(null);
            } else {
                hide();
            }
        });

        header = new HBox(titleLabel, spacer, closeButton);
        header.getStyleClass().add("custom-modal-header");
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(10, 20, 10, 20)); // Consistent padding
        
        // Enable dragging via header
        header.setCursor(Cursor.MOVE);
        header.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });
        header.setOnMouseDragged(event -> {
            modalContainer.setTranslateX(modalContainer.getTranslateX() + event.getSceneX() - xOffset);
            modalContainer.setTranslateY(modalContainer.getTranslateY() + event.getSceneY() - yOffset);
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });

        // 4. Content
        contentPane = new StackPane();
        contentPane.getStyleClass().add("custom-modal-content");
        contentPane.setPadding(new Insets(10)); // Reduced padding

        // 5. Footer
        footer = new HBox(10); // Spacing between buttons
        footer.getStyleClass().add("custom-modal-footer");
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(10)); // Reduced padding

        // Assembly of Modal Card
        // No internal ScrollPane anymore; the whole card scrolls
        modalContainer.getChildren().addAll(header, contentPane, footer);
        VBox.setVgrow(header, Priority.NEVER);
        VBox.setVgrow(contentPane, Priority.NEVER); // Changed to NEVER to avoid filling extra space
        VBox.setVgrow(footer, Priority.NEVER);

        // 6. Outer Scroll Structure
        StackPane scrollContent = new StackPane(modalContainer);
        scrollContent.setAlignment(Pos.CENTER);
        scrollContent.setPadding(new Insets(40, 20, 40, 20)); // Add top/bottom/left/right spacing
        // Ensure background is set to transparent to capture clicks
        scrollContent.setStyle("-fx-background-color: transparent;");
        
        // Close on clicking outside the modal
        scrollContent.setOnMouseClicked(e -> {
            // Check if the click target is the scrollContent itself (the backdrop area)
            // or if the click didn't occur inside the modalContainer
            if (e.getTarget() == scrollContent) {
                hide();
            }
        });

        outerScrollPane = new ScrollPane(scrollContent);
        outerScrollPane.setFitToWidth(true); // Garante que o scroll content ocupe a largura toda
        outerScrollPane.setFitToHeight(true); // Garante que o scroll content ocupe a altura toda
        
        // Ensure modal container refuses to shrink below its content size
        modalContainer.setMinWidth(Region.USE_PREF_SIZE);
        modalContainer.setMinHeight(Region.USE_PREF_SIZE);
        outerScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        outerScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        outerScrollPane.getStyleClass().add("custom-modal-scroll-pane"); // For styling transparency
        outerScrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        // Root Assembly
        getChildren().addAll(overlay, outerScrollPane);

        // Initial state
        setVisible(false);
        modalContainer.setScaleX(0.9);
        modalContainer.setScaleY(0.9);
        modalContainer.setOpacity(0);
    }

    // --- Public API for the Modal ---

    public void setContent(Node node) {
        contentPane.getChildren().setAll(node);
        // Ensure modal container adapts to content width if not manually set
        if (modalContainer.getPrefWidth() == Region.USE_COMPUTED_SIZE) {
            // Remove previous bindings if any
            if (modalContainer.maxWidthProperty().isBound()) {
                modalContainer.maxWidthProperty().unbind();
            }
            
            // Allow full width as defined by child
            modalContainer.setMaxWidth(Double.MAX_VALUE);
            modalContainer.setMinWidth(Region.USE_PREF_SIZE);
            modalContainer.setMinHeight(Region.USE_PREF_SIZE);
        }
    }

    public void setTitle(String title) {
        titleLabel.setText(title);
    }

    public void setFitToContent() {
        modalContainer.setAnimateChanges(true);
        modalContainer.setPrefWidth(Region.USE_COMPUTED_SIZE);
        modalContainer.setPrefHeight(Region.USE_COMPUTED_SIZE);
        
        if (modalContainer.maxWidthProperty().isBound()) modalContainer.maxWidthProperty().unbind();
        modalContainer.setMaxWidth(Region.USE_COMPUTED_SIZE);
        
        if (modalContainer.maxHeightProperty().isBound()) modalContainer.maxHeightProperty().unbind();
        modalContainer.setMaxHeight(Region.USE_COMPUTED_SIZE);
        
        modalContainer.setMinWidth(Region.USE_COMPUTED_SIZE);
        modalContainer.setMinHeight(Region.USE_COMPUTED_SIZE);
        modalContainer.requestLayout();
    }

    public void setModalWidth(double width) {
        if (width <= 0) {
            modalContainer.setAnimateChanges(true);
            modalContainer.setPrefWidth(Region.USE_COMPUTED_SIZE);
            if (modalContainer.maxWidthProperty().isBound()) {
                modalContainer.maxWidthProperty().unbind();
            }
            modalContainer.setMaxWidth(Double.MAX_VALUE);
        } else {
            modalContainer.setAnimateChanges(false);
            modalContainer.setPrefWidth(width);
            if (modalContainer.maxWidthProperty().isBound()) {
                modalContainer.maxWidthProperty().unbind();
            }
            modalContainer.setMaxWidth(width);
        }
    }

    public void setModalHeight(double height) {
        if (height <= 0) {
            modalContainer.setAnimateChanges(true);
            modalContainer.setPrefHeight(Region.USE_COMPUTED_SIZE);
            modalContainer.setMaxHeight(Double.MAX_VALUE);
        } else {
            modalContainer.setAnimateChanges(false);
            modalContainer.setPrefHeight(height);
            modalContainer.setMaxHeight(height);
        }
    }

    public HBox getFooter() {
        return footer;
    }
    
    public Button addPrimaryButton(String text, EventHandler<ActionEvent> action) {
        Button btn = new Button(text);
        btn.getStyleClass().add("button-primary");
        btn.setOnAction(action);
        footer.getChildren().add(btn);
        return btn;
    }

    public Button addSecondaryButton(String text, EventHandler<ActionEvent> action) {
        Button btn = new Button(text);
        btn.getStyleClass().add("button-secondary");
        btn.setOnAction(action);
        footer.getChildren().add(btn);
        return btn;
    }
    
    public void setOnCloseRequest(Consumer<Void> onCloseRequest) {
        this.onCloseRequest = onCloseRequest;
    }

    public void show() {
        if (isVisible()) return;

        setVisible(true);
        
        // Garante centralização
        outerScrollPane.setVvalue(0.5); // Centraliza verticalmente o scroll
        outerScrollPane.setHvalue(0.5); // Centraliza horizontalmente o scroll
        
        // Reset translation on show (optional, but good practice if reused)
        modalContainer.setTranslateX(0);
        modalContainer.setTranslateY(0);

        // Fluid Animation: Fade + Scale
        FadeTransition ft = new FadeTransition(Duration.millis(300), overlay);
        ft.setFromValue(0);
        ft.setToValue(0.6); // Slightly darker overlay
        ft.setInterpolator(Interpolator.EASE_OUT);

        FadeTransition ftContainer = new FadeTransition(Duration.millis(300), modalContainer);
        ftContainer.setFromValue(0);
        ftContainer.setToValue(1);
        ftContainer.setInterpolator(Interpolator.EASE_OUT);

        ScaleTransition st = new ScaleTransition(Duration.millis(300), modalContainer);
        st.setFromX(0.85); // Start slightly smaller for more pop
        st.setFromY(0.85);
        st.setToX(1.0);
        st.setToY(1.0);
        st.setInterpolator(Interpolator.EASE_OUT);

        ParallelTransition pt = new ParallelTransition(ft, ftContainer, st);
        
        pt.play();
        
        modalContainer.requestFocus();
    }

    public void hide() {
        if (!isVisible()) return;

        // Fluid Animation: Fade + Scale Out
        FadeTransition ft = new FadeTransition(Duration.millis(200), overlay);
        ft.setFromValue(overlay.getOpacity());
        ft.setToValue(0);
        ft.setInterpolator(Interpolator.EASE_IN);

        FadeTransition ftContainer = new FadeTransition(Duration.millis(200), modalContainer);
        ftContainer.setFromValue(1);
        ftContainer.setToValue(0);
        ftContainer.setInterpolator(Interpolator.EASE_IN);

        ScaleTransition st = new ScaleTransition(Duration.millis(200), modalContainer);
        st.setFromX(1.0);
        st.setFromY(1.0);
        st.setToX(0.9);
        st.setToY(0.9);
        st.setInterpolator(Interpolator.EASE_IN);

        ParallelTransition pt = new ParallelTransition(ft, ftContainer, st);
        pt.setOnFinished(e -> {
            setVisible(false);
            if (getParent() instanceof Pane p) {
                p.getChildren().remove(this);
            }
        });
        pt.play();
    }
}

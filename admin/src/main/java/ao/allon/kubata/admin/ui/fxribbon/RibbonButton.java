package ao.allon.kubata.admin.ui.fxribbon;

import javafx.beans.binding.Bindings;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.control.OverrunStyle;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;

/**
 * Botão do Ribbon.
 *
 * LARGE = comando principal com ícone acima do texto.
 * SMALL = comando secundário com ícone à esquerda.
 */
public class RibbonButton extends Button {

    private final RibbonButtonSize size;

    public RibbonButton(String id,
                        String label,
                        Node icon,
                        RibbonButtonSize size,
                        String tooltip,
                        Runnable action) {

        this.size = size;
        setId(id);

        getStyleClass().add("ribbon-button");
        setFocusTraversable(true);
        setMnemonicParsing(false);

        if (size == RibbonButtonSize.LARGE) {
            buildLargeLayout(label, icon);
        } else {
            buildSmallLayout(label, icon);
        }

        setText("");

        if (tooltip != null && !tooltip.isBlank()) {
            setTooltip(new Tooltip(tooltip));
        }

        if (action != null) {
            setOnAction(e -> action.run());
        }
    }

    private void buildLargeLayout(String label, Node icon) {
        getStyleClass().add("ribbon-button-tile");

        VBox content = new VBox(4);
        content.setAlignment(Pos.TOP_CENTER);
        content.setFillWidth(true);
        content.setMinWidth(0);
        content.setMaxWidth(Double.MAX_VALUE);

        StackPane iconWrap = new StackPane();
        iconWrap.getStyleClass().add("ribbon-tile-icon-wrap");

        if (icon != null) {
            if (!icon.getStyleClass().contains("ribbon-icon-tile")) {
                icon.getStyleClass().add("ribbon-icon-tile");
            }
            iconWrap.getChildren().add(icon);
        }

        Label text = new Label(label);
        text.getStyleClass().addAll("ribbon-button-text", "ribbon-button-text-below");
        text.setTextAlignment(TextAlignment.CENTER);
        text.setAlignment(Pos.CENTER);
        text.setWrapText(true);
        text.setTextOverrun(OverrunStyle.ELLIPSIS);
        text.setMinWidth(0);
        text.setMaxHeight(34);
        text.prefWidthProperty().bind(
                Bindings.createDoubleBinding(
                        () -> Math.max(28, getWidth() - 12),
                        widthProperty()));
        text.maxWidthProperty().bind(
                Bindings.createDoubleBinding(
                        () -> Math.max(28, getWidth() - 12),
                        widthProperty()));
        VBox.setVgrow(text, javafx.scene.layout.Priority.NEVER);

        content.getChildren().addAll(iconWrap, text);
        setGraphic(content);
    }

    private void buildSmallLayout(String label, Node icon) {
        getStyleClass().add("ribbon-button-small");

        HBox content = new HBox(6);
        content.setAlignment(Pos.CENTER_LEFT);
        content.setFillHeight(true);
        content.setMinWidth(0);
        content.setMaxWidth(Double.MAX_VALUE);

        if (icon != null) {
            if (!icon.getStyleClass().contains("ribbon-icon-small")) {
                icon.getStyleClass().add("ribbon-icon-small");
            }
            content.getChildren().add(icon);
        }

        Label text = new Label(label);
        text.getStyleClass().add("ribbon-button-text");
        text.setAlignment(Pos.CENTER_LEFT);
        text.setWrapText(true);
        text.setTextOverrun(OverrunStyle.ELLIPSIS);
        text.setMinWidth(0);
        text.setMaxHeight(28);
        text.prefWidthProperty().bind(
                Bindings.createDoubleBinding(
                        () -> Math.max(36, getWidth() - 38),
                        widthProperty()));
        text.maxWidthProperty().bind(
                Bindings.createDoubleBinding(
                        () -> Math.max(36, getWidth() - 38),
                        widthProperty()));

        HBox.setHgrow(text, javafx.scene.layout.Priority.ALWAYS);
        content.getChildren().add(text);
        setGraphic(content);
    }

    public RibbonButtonSize getRibbonSize() {
        return size;
    }

    public void setSelected(boolean selected) {
        getStyleClass().remove("ribbon-button-selected");

        if (selected) {
            getStyleClass().add("ribbon-button-selected");
        }
    }
}

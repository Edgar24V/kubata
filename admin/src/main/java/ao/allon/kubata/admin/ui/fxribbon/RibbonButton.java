package ao.allon.kubata.admin.ui.fxribbon;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;

/**
 * Botão do Ribbon.
 *
 * LARGE: ícone acima e texto abaixo.
 * SMALL: ícone à esquerda e texto ao lado.
 *
 * As dimensões são estáveis; a responsividade é tratada pelo overflow
 * de grupos para não esmagar ou cortar os textos.
 */
public class RibbonButton extends Button {

    private final RibbonButtonSize size;

    public RibbonButton(String id,
                        String label,
                        Node icon,
                        RibbonButtonSize size,
                        String tooltip,
                        Runnable action) {

        this.size = size == null ? RibbonButtonSize.SMALL : size;

        setId(id);
        getStyleClass().add("ribbon-button");
        setFocusTraversable(true);
        setMnemonicParsing(false);
        setText("");

        if (this.size == RibbonButtonSize.LARGE) {
            buildLargeLayout(label, icon);
        } else {
            buildSmallLayout(label, icon);
        }

        if (tooltip != null && !tooltip.isBlank()) {
            setTooltip(new Tooltip(tooltip));
        }

        if (action != null) {
            setOnAction(e -> action.run());
        }
    }

    private void buildLargeLayout(String label, Node icon) {
        getStyleClass().add("ribbon-button-tile");

        VBox content = new VBox(3);
        content.setAlignment(Pos.TOP_CENTER);
        content.setFillWidth(true);
        content.setMinWidth(0);
        content.setPrefWidth(70);
        content.setMaxWidth(70);

        StackPane iconWrap = new StackPane();
        iconWrap.getStyleClass().add("ribbon-tile-icon-wrap");
        iconWrap.setMinWidth(40);
        iconWrap.setPrefWidth(40);
        iconWrap.setMinHeight(36);
        iconWrap.setPrefHeight(36);

        if (icon != null) {
            if (!icon.getStyleClass().contains("ribbon-icon-tile")) {
                icon.getStyleClass().add("ribbon-icon-tile");
            }
            iconWrap.getChildren().add(icon);
        }

        Label text = new Label(label == null ? "" : label);
        text.getStyleClass().addAll("ribbon-button-text", "ribbon-button-text-below");
        text.setAlignment(Pos.CENTER);
        text.setTextAlignment(TextAlignment.CENTER);
        text.setWrapText(true);
        text.setTextOverrun(OverrunStyle.ELLIPSIS);
        text.setMinWidth(0);
        text.setPrefWidth(66);
        text.setMaxWidth(66);
        text.setMinHeight(21);
        text.setPrefHeight(30);
        text.setMaxHeight(34);

        content.getChildren().addAll(iconWrap, text);
        setGraphic(content);
    }

    private void buildSmallLayout(String label, Node icon) {
        getStyleClass().add("ribbon-button-small");

        HBox content = new HBox(5);
        content.setAlignment(Pos.CENTER_LEFT);
        content.setFillHeight(true);
        content.setMinWidth(0);
        content.setPrefWidth(104);
        content.setMaxWidth(104);

        if (icon != null) {
            if (!icon.getStyleClass().contains("ribbon-icon-small")) {
                icon.getStyleClass().add("ribbon-icon-small");
            }
            StackPane iconBox = new StackPane(icon);
            iconBox.setMinWidth(18);
            iconBox.setPrefWidth(18);
            iconBox.setMaxWidth(18);
            content.getChildren().add(iconBox);
        }

        Label text = new Label(label == null ? "" : label);
        text.getStyleClass().add("ribbon-button-text");
        text.setAlignment(Pos.CENTER_LEFT);
        text.setWrapText(true);
        text.setTextOverrun(OverrunStyle.ELLIPSIS);
        text.setMinWidth(0);
        text.setPrefWidth(78);
        text.setMaxWidth(78);
        text.setMaxHeight(28);

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

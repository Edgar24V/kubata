package ao.allon.kubata.admin.ui.fxribbon;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
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

        StackPane iconWrap = new StackPane();
        iconWrap.getStyleClass().add("ribbon-tile-icon-wrap");

        if (icon != null) {
            if (!icon.getStyleClass().contains("ribbon-icon-tile")) {
                icon.getStyleClass().add("ribbon-icon-tile");
            }
            iconWrap.getChildren().add(icon);
        }

        Text text = new Text(label);
        text.getStyleClass().addAll("ribbon-button-text", "ribbon-button-text-below");
        text.setTextAlignment(TextAlignment.CENTER);
        text.setWrappingWidth(62);

        content.getChildren().addAll(iconWrap, text);
        setGraphic(content);
    }

    private void buildSmallLayout(String label, Node icon) {
        getStyleClass().add("ribbon-button-small");

        HBox content = new HBox(6);
        content.setAlignment(Pos.CENTER_LEFT);

        if (icon != null) {
            if (!icon.getStyleClass().contains("ribbon-icon-small")) {
                icon.getStyleClass().add("ribbon-icon-small");
            }
            content.getChildren().add(icon);
        }

        Text text = new Text(label);
        text.getStyleClass().add("ribbon-button-text");
        text.setWrappingWidth(105);

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

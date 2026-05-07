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
 * Botão do Ribbon que se adapta ao tamanho:
 * <ul>
 *   <li><b>LARGE</b> – mosaico quadrado: wrapper de ícone (40×40) + texto em baixo</li>
 *   <li><b>SMALL</b> – linha horizontal: ícone pequeno + texto ao lado</li>
 * </ul>
 */
public class RibbonButton extends Button {

    private final RibbonButtonSize size;

    public RibbonButton(String id, String label, Node icon, RibbonButtonSize size, String tooltip, Runnable action) {
        this.size = size;
        setId(id);

        getStyleClass().add("ribbon-button");

        if (size == RibbonButtonSize.LARGE) {
            buildLargeLayout(label, icon);
        } else {
            buildSmallLayout(label, icon);
        }

        setText(""); // conteúdo gerido pelo graphic

        if (tooltip != null && !tooltip.isBlank()) {
            setTooltip(new Tooltip(tooltip));
        }
        if (action != null) {
            setOnAction(e -> action.run());
        }
    }

    // ── Layouts ────────────────────────────────────────────────────────────────

    private void buildLargeLayout(String label, Node icon) {
        getStyleClass().add("ribbon-button-tile");

        VBox content = new VBox(3);
        content.setAlignment(Pos.TOP_CENTER);

        // Wrapper quadrado com fundo verde subtil (estilo PRIMAVERA V10)
        StackPane iconWrap = new StackPane();
        iconWrap.getStyleClass().add("ribbon-tile-icon-wrap");
        if (icon != null) {
            icon.getStyleClass().add("ribbon-icon-tile");
            iconWrap.getChildren().add(icon);
        }

        Text text = new Text(label);
        text.getStyleClass().addAll("ribbon-button-text", "ribbon-button-text-below");
        text.setTextAlignment(TextAlignment.CENTER);
        text.setWrappingWidth(52);

        content.getChildren().addAll(iconWrap, text);
        setGraphic(content);
    }

    private void buildSmallLayout(String label, Node icon) {
        getStyleClass().add("ribbon-button-small");

        HBox content = new HBox(5);
        content.setAlignment(Pos.CENTER_LEFT);

        if (icon != null) {
            icon.getStyleClass().add("ribbon-icon-small");
            content.getChildren().add(icon);
        }

        Text text = new Text(label);
        text.getStyleClass().add("ribbon-button-text");

        content.getChildren().add(text);
        setGraphic(content);
    }

    // ── API pública ────────────────────────────────────────────────────────────

    public RibbonButtonSize getRibbonSize() {
        return size;
    }

    public void setSelected(boolean selected) {
        if (selected) {
            if (!getStyleClass().contains("ribbon-button-selected")) {
                getStyleClass().add("ribbon-button-selected");
            }
        } else {
            getStyleClass().remove("ribbon-button-selected");
        }
    }
}

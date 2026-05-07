package ao.allon.kubata.admin.ui.fxribbon;

import javafx.geometry.Insets;
import javafx.stage.Popup;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * Popup de overflow que aparece quando o Ribbon fica estreito demais
 * para mostrar todos os grupos de uma tab.
 */
public class RibbonOverflowPopup extends Popup {

    private final VBox container;

    public RibbonOverflowPopup() {
        container = new VBox(4);
        container.setPadding(new Insets(8));
        container.getStyleClass().add("ribbon-overflow-popup");

        setAutoHide(true);
        setAutoFix(true);
        setHideOnEscape(true);

        getContent().add(container);
    }

    public void setGroups(List<RibbonGroup> hiddenGroups) {
        container.getChildren().setAll(hiddenGroups);
    }
}

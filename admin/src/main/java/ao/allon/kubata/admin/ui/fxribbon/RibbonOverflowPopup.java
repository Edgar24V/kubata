package ao.allon.kubata.admin.ui.fxribbon;

import javafx.geometry.Insets;
import javafx.stage.Popup;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * Popup dos comandos que não cabem na largura disponível do Ribbon.
 *
 * Os grupos originais são temporariamente reposicionados aqui, portanto
 * continuam a executar exatamente as mesmas ações.
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
        setConsumeAutoHidingEvents(true);

        getContent().add(container);
    }

    public void setGroups(List<RibbonGroup> hiddenGroups) {
        container.getChildren().setAll(hiddenGroups == null ? List.of() : hiddenGroups);
    }

    public boolean hasGroups() {
        return !container.getChildren().isEmpty();
    }
}

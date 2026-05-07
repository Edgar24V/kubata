package ao.allon.kubata.core.module;

import javafx.scene.Node;

/**
 * Representa uma view/função dentro de um módulo.
 */
public record ModuleView(
    String viewId,
    String viewName,
    String viewDescription,
    Node viewIcon,
    String permission,
    Class<?> viewClass,
    int displayOrder
) {
    public ModuleView {
        if (viewId == null || viewId.isBlank()) {
            throw new IllegalArgumentException("viewId cannot be null or blank");
        }
        if (viewName == null || viewName.isBlank()) {
            throw new IllegalArgumentException("viewName cannot be null or blank");
        }
    }
}

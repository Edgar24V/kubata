package ao.allon.kubata.rh.ui.util;

import javafx.scene.Scene;

/**
 * Tema do Kubata RH: reutiliza a pele visual do Kubata Administrator
 * (kubata.css + ribbon do core) e acrescenta as camadas específicas do RH.
 */
public final class ThemeManager {

    /** Cor de marca partilhada pelo ecossistema Kubata. */
    public static final String KUBATA_GREEN =
            ao.allon.kubata.core.ui.theme.ThemeManager.KUBATA_GREEN;

    private ThemeManager() {
    }

    public static void applyTheme(Scene scene) {
        if (scene == null) {
            return;
        }

        ao.allon.kubata.core.ui.theme.ThemeManager.applyTheme(scene);

        addStylesheet(scene, "/ao/allon/kubata/rh/ui/styles/rh-tabpane.css");
        addStylesheet(scene, "/ao/allon/kubata/rh/ui/styles/rh-ribbon.css");
    }

    public static boolean isReducedMotion() {
        return ao.allon.kubata.core.ui.theme.ThemeManager.isReducedMotion();
    }

    private static void addStylesheet(Scene scene, String resource) {
        var url = ThemeManager.class.getResource(resource);
        if (url == null) {
            throw new IllegalStateException("Folha de estilos não encontrada: " + resource);
        }

        String css = url.toExternalForm();
        if (!scene.getStylesheets().contains(css)) {
            scene.getStylesheets().add(css);
        }
    }
}

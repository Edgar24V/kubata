package ao.allon.kubata.core.ui.theme;

import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * Tema visual único do ecossistema Kubata, extraído do Kubata Administrator.
 */
public final class ThemeManager {

    public static final String KUBATA_GREEN_DARK = "#1A5C35";
    public static final String KUBATA_GREEN = "#217346";
    public static final String KUBATA_GREEN_MEDIUM = "#217346";
    public static final String KUBATA_GREEN_LIGHT = "#66BB6A";
    public static final String KUBATA_GREEN_PALE = "#E8F5E9";
    public static final String KUBATA_GREEN_BORDER = "#A5D6A7";

    private static final String KUBATA_CSS = "/ao/allon/kubata/core/ui/styles/kubata.css";
    private static final String RIBBON_CSS = "/ao/allon/kubata/core/ui/styles/ribbon-office.css";
    private static final String RIBBON_BUTTONS_CSS = "/ao/allon/kubata/core/ui/styles/ribbon-buttons.css";

    private ThemeManager() {
    }

    public static void applyTheme(Scene scene) {
        if (scene == null) {
            return;
        }
        loadStylesheet(scene, KUBATA_CSS);
        loadStylesheet(scene, RIBBON_CSS);
        loadStylesheet(scene, RIBBON_BUTTONS_CSS);

        if (scene.getRoot() != null) {
            String current = scene.getRoot().getStyle();
            if (current == null) {
                current = "";
            }
            scene.getRoot().setStyle(current
                    + " -fx-accent: " + KUBATA_GREEN + ";"
                    + " -fx-focus-color: " + KUBATA_GREEN_LIGHT + ";"
                    + " -fx-selection-bar: " + KUBATA_GREEN + ";");
        }
    }

    public static void setupCustomTitleBar(Stage stage) {
        if (stage != null && stage.getStyle() != StageStyle.TRANSPARENT) {
            stage.initStyle(StageStyle.TRANSPARENT);
        }
    }

    public static boolean isReducedMotion() {
        return Boolean.parseBoolean(System.getProperty("kubata.reducedMotion", "false"));
    }

    private static void loadStylesheet(Scene scene, String path) {
        var resource = ThemeManager.class.getResource(path);
        if (resource == null) {
            System.err.println("[ThemeManager] CSS não encontrado: " + path);
            return;
        }
        String css = resource.toExternalForm();
        if (!scene.getStylesheets().contains(css)) {
            scene.getStylesheets().add(css);
        }
    }
}

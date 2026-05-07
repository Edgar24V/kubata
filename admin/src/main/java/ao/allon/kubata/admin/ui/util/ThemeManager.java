package ao.allon.kubata.admin.ui.util;

import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public final class ThemeManager {

    // ── Paleta Kubata Verde ──────────────────────────────
    public static final String KUBATA_GREEN_DARK   = "#1A5C35";
    public static final String KUBATA_GREEN        = "#217346";
    public static final String KUBATA_GREEN_MEDIUM = "#217346";
    public static final String KUBATA_GREEN_LIGHT  = "#66BB6A";
    public static final String KUBATA_GREEN_PALE   = "#E8F5E9";
    public static final String KUBATA_GREEN_BORDER = "#A5D6A7";

    private static final String ADMIN_CSS_PATH =
        "/ao/allon/kubata/admin/ui/styles/admin.css";

    private static final String RIBBON_CSS_PATH =
        "/ao/allon/kubata/admin/ui/styles/ribbon-modern.css";

    private static final String RIBBON_BUTTONS_CSS_PATH =
        "/ao/allon/kubata/admin/ui/styles/ribbon-buttons.css";

    private ThemeManager() {}

    /**
     * Aplica o tema completo (admin.css) à Scene.
     * Chamar uma vez após criar a Scene principal.
     */
    public static void applyTheme(Scene scene) {
        loadStylesheet(scene, ADMIN_CSS_PATH);
        loadStylesheet(scene, RIBBON_CSS_PATH);
        loadStylesheet(scene, RIBBON_BUTTONS_CSS_PATH);

        // Custom override for global green theme properties
        scene.getRoot().setStyle(scene.getRoot().getStyle() +
            " -fx-accent: " + KUBATA_GREEN + ";" +
            " -fx-focus-color: " + KUBATA_GREEN_LIGHT + ";" +
            " -fx-selection-bar: " + KUBATA_GREEN + ";");
    }

    /**
     * Configura o Stage para usar barra de título personalizada.
     * IMPORTANTE: Chamar ANTES de stage.show().
     */
    public static void setupCustomTitleBar(Stage stage) {
        stage.initStyle(StageStyle.UNDECORATED);
    }

    /**
     * Quando {@code true}, o ribbon e popups relacionados evitam animações (acessibilidade).
     * Definir com {@code -Dkubata.reducedMotion=true}.
     */
    public static boolean isReducedMotion() {
        return Boolean.parseBoolean(System.getProperty("kubata.reducedMotion", "false"));
    }

    private static void loadStylesheet(Scene scene, String path) {
        var resource = ThemeManager.class.getResource(path);
        if (resource != null) {
            scene.getStylesheets().add(resource.toExternalForm());
        } else {
            System.err.println("[ThemeManager] CSS não encontrado: " + path);
        }
    }
}

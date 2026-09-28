package ao.allon.kubata.rh.ui.util;

import com.pixelduke.transit.Style;
import com.pixelduke.transit.TransitTheme;
import javafx.scene.Scene;

public final class ThemeManager {

    private static boolean darkMode = false;

    /** Cor de marca partilhada pelo ecossistema Kubata. */
    public static final String KUBATA_GREEN = "#107C41";

    private ThemeManager() {
    }

    public static void applyTheme(Scene scene) {
        if (scene == null) {
            return;
        }

        TransitTheme transitTheme =
                new TransitTheme(darkMode ? Style.DARK : Style.LIGHT);
        transitTheme.setScene(scene);

        // Accent global consistente com o Admin.
        if (scene.getRoot() != null) {
            String currentStyle = scene.getRoot().getStyle();
            if (currentStyle == null) {
                currentStyle = "";
            }
            scene.getRoot().setStyle(currentStyle + "; -fx-accent: " + KUBATA_GREEN + ";");
        }

        addStylesheet(scene, "/ao/allon/kubata/rh/ui/styles/rh-tabpane.css");
        addStylesheet(scene, "/ao/allon/kubata/rh/ui/styles/rh-ribbon.css");
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

    public static void setDarkMode(boolean dark) {
        darkMode = dark;
    }

    public static boolean isDarkMode() {
        return darkMode;
    }

    public static void toggleTheme(Scene scene) {
        darkMode = !darkMode;
        applyTheme(scene);
    }
}

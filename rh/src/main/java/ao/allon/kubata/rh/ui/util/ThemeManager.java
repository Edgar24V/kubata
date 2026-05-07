package ao.allon.kubata.rh.ui.util;

import com.pixelduke.transit.TransitTheme;
import com.pixelduke.transit.Style;
import javafx.scene.Scene;
import java.util.Base64;
import java.nio.charset.StandardCharsets;

public class ThemeManager {
    private static boolean darkMode = false;

    // KUBATA GREEN PALETTE
    public static final String KUBATA_GREEN = "#2E7D32";

    public static void applyTheme(Scene scene) {
        if (scene != null) {
            TransitTheme transitTheme = new TransitTheme(darkMode ? Style.DARK : Style.LIGHT);
            transitTheme.setScene(scene);
            
            // Custom override for global green theme
            scene.getRoot().setStyle(scene.getRoot().getStyle() + " -fx-accent: " + KUBATA_GREEN + ";");

            // Load TabPane Office 365 styles
            scene.getStylesheets().add(ThemeManager.class.getResource("/ao/allon/kubata/rh/ui/styles/rh-tabpane.css").toExternalForm());

            // Ribbon Modern Style & Animations
            String ribbonStyles = 
                " .ribbon { -fx-background-color: -color-bg-subtle; -fx-border-color: -color-border-muted; -fx-border-width: 0 0 1 0; } " +
                " .ribbon-group { -fx-padding: 5 10 5 10; -fx-border-color: transparent -color-border-muted transparent transparent; } " +
                " .ribbon-group-title { -fx-text-fill: -color-fg-muted; -fx-font-size: 10px; } " +
                
                " .ribbon-button { " +
                "    -fx-background-color: transparent; " +
                "    -fx-background-radius: 8; " +
                "    -fx-padding: 8 5 8 5; " +
                "    -fx-cursor: hand; " +
                " } " +
                " .ribbon-button:hover { " +
                "    -fx-background-color: " + KUBATA_GREEN + "22; " +
                "    -fx-scale-x: 1.05; " +
                "    -fx-scale-y: 1.05; " +
                " } " +
                " .ribbon-button:pressed { " +
                "    -fx-background-color: " + KUBATA_GREEN + "44; " +
                "    -fx-scale-x: 0.95; " +
                "    -fx-scale-y: 0.95; " +
                " } " +
                " .ribbon-button .label { -fx-font-size: 11px; } " +
                " .ribbon-button:hover .label { -fx-text-fill: " + KUBATA_GREEN + "; -fx-font-weight: bold; } ";

            String base64Styles = Base64.getEncoder().encodeToString(ribbonStyles.getBytes(StandardCharsets.UTF_8));
            scene.getStylesheets().add("data:text/css;base64," + base64Styles);
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

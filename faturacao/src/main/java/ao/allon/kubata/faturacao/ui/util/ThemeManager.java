package ao.allon.kubata.faturacao.ui.util;

import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * Tema visual comum do Kubata. Mantém o PrimerLight como base e aplica
 * a paleta, a barra de título e o Ribbon usados pelo Administrator.
 */
public final class ThemeManager {

    private static final String[] SHEET_PATHS = {
            "/ao/allon/kubata/faturacao/ui/styles/admin.css",
            "/ao/allon/kubata/faturacao/ui/styles/ribbon-modern.css",
            "/ao/allon/kubata/faturacao/ui/styles/ribbon-buttons.css"
    };

    private ThemeManager() { }

    /** Define a base de controlos JavaFX do Kubata. */
    public static void applyTheme() {
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
    }

    /** Aplica o tema completo à cena actual. */
    public static void applyTheme(Scene scene) {
        if (scene == null) return;
        applyTheme();

        for (String path : SHEET_PATHS) {
            var resource = ThemeManager.class.getResource(path);
            if (resource != null && !scene.getStylesheets().contains(resource.toExternalForm())) {
                scene.getStylesheets().add(resource.toExternalForm());
            } else if (resource == null) {
                System.err.println("[ThemeManager] CSS não encontrado: " + path);
            }
        }

        if (scene.getRoot() != null) {
            String existing = scene.getRoot().getStyle();
            scene.getRoot().setStyle((existing == null ? "" : existing)
                    + " -fx-accent: #217346;"
                    + " -fx-focus-color: #66BB6A;"
                    + " -fx-selection-bar: #217346;");
        }
    }

    /** Deve ser chamado antes de stage.show(). */
    public static void setupCustomTitleBar(Stage stage) {
        if (stage != null) {
            stage.initStyle(StageStyle.TRANSPARENT);
        }
    }
}

package ao.allon.kubata.faturacao.ui.util;

import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public final class ThemeManager {

    private static final String KUBATA_CSS =
            "/ao/allon/kubata/faturacao/ui/styles/faturacao.css";
    private static final String RIBBON_CSS =
            "/ao/allon/kubata/faturacao/ui/styles/ribbon-modern.css";
    private static final String RIBBON_BUTTONS_CSS =
            "/ao/allon/kubata/faturacao/ui/styles/ribbon-buttons.css";

    private ThemeManager() {
    }

    /** Define a base do tema, mantendo a compatibilidade com os controlos Atlantafx existentes. */
    public static void applyTheme() {
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
    }

    /** Aplica o mesmo tema corporativo verde utilizado pelo Kubata Administrator à cena. */
    public static void applyTheme(Scene scene) {
        if (scene == null) {
            return;
        }
        loadStylesheet(scene, KUBATA_CSS);
        loadStylesheet(scene, RIBBON_CSS);
        loadStylesheet(scene, RIBBON_BUTTONS_CSS);
        scene.setFill(Color.TRANSPARENT);

        if (scene.getRoot() != null) {
            scene.getRoot().setStyle(
                    scene.getRoot().getStyle()
                            + " -fx-accent: #217346;"
                            + " -fx-focus-color: #66BB6A;"
                            + " -fx-selection-bar: #217346;"
            );
        }
    }

    /** Deve ser chamado antes de Stage.show() para permitir uma moldura personalizada. */
    public static void setupCustomTitleBar(Stage stage) {
        if (stage != null && !stage.isShowing()) {
            stage.initStyle(StageStyle.TRANSPARENT);
        }
    }

    private static void loadStylesheet(Scene scene, String path) {
        var resource = ThemeManager.class.getResource(path);
        if (resource == null) {
            System.err.println("[ThemeManager] CSS não encontrado: " + path);
            return;
        }
        String cssUrl = resource.toExternalForm();
        if (!scene.getStylesheets().contains(cssUrl)) {
            scene.getStylesheets().add(cssUrl);
        }
    }
}

package ao.allon.kubata.faturacao.ui.util;

import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;
import javafx.scene.Scene;

public final class ThemeManager {

    private ThemeManager() {
        // private constructor to prevent instantiation
    }

    public static void applyTheme() {
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
    }

    /**
     * Aplica a pele visual do Kubata Administrator (kubata.css + ribbon) por
     * cima do tema base, para que todos os módulos partilhem o mesmo estilo.
     */
    public static void applySceneTheme(Scene scene) {
        ao.allon.kubata.core.ui.theme.ThemeManager.applyTheme(scene);
    }
}

package ao.allon.kubata.faturacao.ui.util;

import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;

import java.util.Objects;

public final class ThemeManager {

    private ThemeManager() {
        // private constructor to prevent instantiation
    }

    public static void applyTheme() {
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
    }
}

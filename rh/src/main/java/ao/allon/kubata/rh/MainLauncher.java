package ao.allon.kubata.rh;

import javafx.application.Application;
import ao.allon.kubata.rh.ui.JavaFxApplication;

/**
 * Launcher de entrada simples para contornar problemas de classpath do JavaFX.
 */
public class MainLauncher {
    public static void main(String[] args) {
        // Esta chamada indireta garante que o JavaFX seja inicializado corretamente
        // mesmo que o JAR não seja modular.
        Application.launch(JavaFxApplication.class, args);
    }
}

package ao.allon.kubata.faturacao;

import javafx.application.Application;
import ao.allon.kubata.faturacao.ui.JavaFxApplication;

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

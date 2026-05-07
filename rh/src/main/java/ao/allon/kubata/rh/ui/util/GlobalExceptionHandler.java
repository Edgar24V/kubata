package ao.allon.kubata.rh.ui.util;

import java.util.logging.Level;
import java.util.logging.Logger;

public class GlobalExceptionHandler {
    private static final Logger LOGGER = Logger.getLogger(GlobalExceptionHandler.class.getName());

    public static void setup() {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            LOGGER.log(Level.SEVERE, "Uncaught exception in thread " + thread.getName(), throwable);
            
            // Mostra diálogo de erro em JavaFX se disponível
            javafx.application.Platform.runLater(() -> {
                try {
                    javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                    alert.setTitle("Erro Inesperado");
                    alert.setHeaderText("Ocorreu um erro inesperado");
                    alert.setContentText("Detalhes: " + throwable.getMessage());
                    alert.showAndWait();
                } catch (Exception e) {
                    // Se JavaFX não estiver disponível, apenas loga
                    LOGGER.log(Level.SEVERE, "Failed to show error dialog", e);
                }
            });
        });
    }
}

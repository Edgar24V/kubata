package ao.allon.kubata.admin.ui.util;

import javafx.application.Platform;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GlobalExceptionHandler {
    private static final Logger LOGGER = Logger.getLogger(GlobalExceptionHandler.class.getName());
    private static final AtomicBoolean showingDialog = new AtomicBoolean(false);

    public static void setup() {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            LOGGER.log(Level.SEVERE, "Uncaught exception in thread " + thread.getName(), throwable);

            // Evita diálogos recursivos durante falhas de inicialização
            if (showingDialog.compareAndSet(false, true)) {
                try {
                    // Só tenta mostrar diálogo se o JavaFX estiver inicializado
                    if (Platform.isFxApplicationThread()) {
                        showErrorDialog(throwable);
                    } else {
                        Platform.runLater(() -> showErrorDialog(throwable));
                    }
                } catch (IllegalStateException e) {
                    // JavaFX não inicializado ou toolkit não disponível
                    LOGGER.log(Level.SEVERE, "JavaFX not available for error dialog: " + e.getMessage());
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Failed to show error dialog", e);
                } finally {
                    showingDialog.set(false);
                }
            } else {
                LOGGER.log(Level.WARNING, "Suppressed nested error dialog during initialization failure");
            }
        });
    }

    private static void showErrorDialog(Throwable throwable) {
        try {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
            alert.setTitle("Erro Inesperado");
            alert.setHeaderText("Ocorreu um erro inesperado");
            alert.setContentText("Detalhes: " + throwable.getMessage());
            alert.showAndWait();
        } finally {
            showingDialog.set(false);
        }
    }
}

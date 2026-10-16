package ao.allon.kubata.admin.ui.util;

import ao.allon.kubata.core.ui.modal.ModalManager;
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
            ModalManager manager = ModalManager.getActiveInstance();
            if (manager != null && manager.hasRoot()) {
                manager.showErrorModal(
                        "Erro inesperado",
                        "Ocorreu um erro inesperado. Os detalhes técnicos estão disponíveis abaixo.",
                        throwable
                );
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Failed to show error modal", ex);
        } finally {
            showingDialog.set(false);
        }
    }
}
